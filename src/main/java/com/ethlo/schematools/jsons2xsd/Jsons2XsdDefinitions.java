package com.ethlo.schematools.jsons2xsd;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.Map.Entry;

public class Jsons2XsdDefinitions
{
	private final static ObjectMapper mapper = new ObjectMapper();

	private static Map<String, Element> unusedElements = new HashMap<>();
	private static Set<String> neededElements = new HashSet<>();
	
	public static enum OuterWrapping
	{
		ELEMENT, TYPE;
	}
	
	private static final Map<String, String> typeMapping = new HashMap<>();
	static
	{
		// Primitive types
		typeMapping.put("string", "string");
		typeMapping.put("object", "object");
		typeMapping.put("array", "array");
		typeMapping.put("number", "decimal");
		typeMapping.put("boolean", "boolean");
		typeMapping.put("integer", "int");
		
		// Non-standard, often encountered in the wild
		typeMapping.put("int", "int");
		typeMapping.put("date-time", "dateTime");
		typeMapping.put("time", "time");
		typeMapping.put("date", "date");
		
		// TODO: Support "JSON null" 
		
		// String formats
		typeMapping.put("string|uri", "anyURI");
		typeMapping.put("string|email", "string");
		typeMapping.put("string|phone", "string");
		typeMapping.put("string|date-time", "dateTime");
		typeMapping.put("string|date", "date");
		typeMapping.put("string|time", "time");
		typeMapping.put("string|utc-millisec", "long");
		typeMapping.put("string|regex", "string");
		typeMapping.put("string|color", "string");
		typeMapping.put("string|style", "string");
	}





	public static Document convert(Reader jsonSchema, String targetNameSpaceUri, OuterWrapping wrapping, String name) throws JsonProcessingException, IOException
	{
		JsonNode rootNode = mapper.readTree(jsonSchema);

		final Document xsdDoc = XmlUtil.newDocument();
		xsdDoc.setXmlStandalone(true);

		final Element schemaRoot = createXsdElement(xsdDoc, "schema");
		schemaRoot.setAttribute("targetNamespace", targetNameSpaceUri);
		schemaRoot.setAttribute("xmlns:" + name.toLowerCase(), targetNameSpaceUri);

		schemaRoot.setAttribute("elementFormDefault", "qualified");
		schemaRoot.setAttribute("attributeFormDefault", "qualified");

		//handle external defs
		final JsonNode definitions = rootNode.path("definitions");
		Assert.notNull(definitions, "\"definitions\"  should be found in root of JSON schema\"");


		//create a fake root to for unused elements.
		final Document unsuedDoc = XmlUtil.newDocument();
		unsuedDoc.setXmlStandalone(true);

		final Element unusedRoot = createXsdElement(unsuedDoc, "root");
		doIterateDefinitions(unusedRoot, definitions);





		final String type = rootNode.path("type").textValue();
		Assert.isTrue("object".equals(type), "root should have type=\"object\"");

		final JsonNode properties = rootNode.get("properties");
		Assert.notNull(properties, "\"properties\" property should be found in root of JSON schema\"");



		Element wrapper = schemaRoot;
		if (wrapping == OuterWrapping.ELEMENT)
		{
			wrapper = createXsdElement(schemaRoot, "element");
			wrapper.setAttribute("name", name);
			wrapper.setAttribute("type", name.toLowerCase() + ":" + name);

		}

		final Element schemaComplexType = createXsdElement(schemaRoot, "complexType");

	//if (wrapping == OuterWrapping.TYPE)
		{
			schemaComplexType.setAttribute("name", name);
		}
		final Element schemaSequence = createXsdElement(schemaComplexType, "sequence");

		doIterate(schemaSequence, properties);


		//Now add the need guys
		for (String nodeName : neededElements) {
			Element newNode = unusedElements.get(nodeName);
			if (newNode != null) {

			}
		}

		return xsdDoc;
	}

	private static void doIterateDefinitions(Element elem, JsonNode node)
	{
		final Iterator<Entry<String, JsonNode>> fieldIter = node.fields();
		while(fieldIter.hasNext())
		{
			//Create a complex type
			//get properties
			//call doiteration with properties

			final Entry<String, JsonNode> entry = fieldIter.next();
			final String key = entry.getKey();
			final JsonNode val = entry.getValue();
			final Element schemaComplexType = createXsdElement(elem, "complexType");
			schemaComplexType.setAttribute("name", key);
			unusedElements.put(key, schemaComplexType);


			final Element schemaSequence = createXsdElement(schemaComplexType, "sequence");
			final JsonNode properties = val.get("properties");
			Assert.notNull(properties, "\"properties\" property should be found in \"" + key +  "\"");

			doIterate(schemaSequence, properties);




		}
	}

	private static void doIterate(Element elem, JsonNode node)
	{
		final Iterator<Entry<String, JsonNode>> fieldIter = node.fields();
		while(fieldIter.hasNext())
		{
			final Entry<String, JsonNode> entry = fieldIter.next();
			final String key = entry.getKey();
			final JsonNode val = entry.getValue();
			doIterateSingle(key, val, elem);
		}
	}
	
	private static void doIterateSingle(String key, JsonNode val, Element elem)
	{
		final String xsdType = determineXsdType(key, val);
		final boolean required = val.path("required").booleanValue();
		final Element nodeElem = createXsdElement(elem, "element");
		nodeElem.setAttribute("name", key);

		if (! "object".equals(xsdType) && !"array".equals(xsdType))
		{
			// Simple type
			nodeElem.setAttribute("type", xsdType);
		}
		
		if (! required)
		{
			// Not required
			nodeElem.setAttribute("minOccurs", "0");
		}
		
		switch (xsdType)
		{
			case "array":
				handleArray(nodeElem, val);
				return;
				
			case "decimal":
			case "int":
				handleNumber(nodeElem, xsdType, val);
				return;
				
			case "enum":
				handleEnum(nodeElem, val);
				return;
				
			case "object":
				handleObject(nodeElem, val);
				return;
				
			case "string":
				handleString(nodeElem, val);
				return;

			case "reference":
				handleReference(nodeElem, val);
				return;
		}
	}


	private static void handleReference(Element nodeElem, JsonNode val) {
		final JsonNode refs = val.get("$ref");
		nodeElem.removeAttribute("type");
		String fixRef = refs.asText().replace("#/definitions/", "cmts:");
		String name = fixRef.substring(5);
		String oldName = nodeElem.getAttribute("name");


		if (oldName.length() <= 0) {
			nodeElem.setAttribute("name", name);
		}
		nodeElem.setAttribute("type", fixRef);
		neededElements.add(fixRef);


	}

	private static void handleString(Element nodeElem, JsonNode val)
	{
		final Integer minimumLength = getIntVal(val, "minLength");
		final Integer maximumLength = getIntVal(val, "maxLength");
		final String expression = val.path("pattern").textValue();
		
		if (minimumLength != null || maximumLength != null || expression != null)
		{
			nodeElem.removeAttribute("type");
			final Element simpleType = createXsdElement(nodeElem, "simpleType");
			final Element restriction = createXsdElement(simpleType, "restriction");
			restriction.setAttribute("base", "string");
			
			if (minimumLength != null)
			{
				final Element min = createXsdElement(restriction, "minLength");
				min.setAttribute("value", Integer.toString(minimumLength));
			}
			
			if (maximumLength != null)
			{
				final Element max = createXsdElement(restriction, "maxLength");
				max.setAttribute("value", Integer.toString(maximumLength));
			}
			
			if (expression != null)
			{
				final Element max = createXsdElement(restriction, "pattern");
				max.setAttribute("value", expression);
			}
		}
	}

	private static void handleObject(Element nodeElem, JsonNode val)
	{
		final JsonNode properties = val.get("properties");
		if (properties != null) {
			System.out.println("In PROP");
			final Element complexType = createXsdElement(nodeElem, "complexType");
			final Element sequence = createXsdElement(complexType, "sequence");
			Assert.notNull(properties, "'object' type must have a 'properties' attribute");
			doIterate(sequence, properties);
		}
//		else {
//			final JsonNode refs = val.get("$ref");
//			Assert.notNull(refs, "No Properties and no Ref is not acceptable.");
//			nodeElem.setAttribute("ref", refs.asText().toLowerCase());
//		}
	}

	private static void handleEnum(Element nodeElem, JsonNode val)
	{
		nodeElem.removeAttribute("type");
		final Element simpleType = createXsdElement(nodeElem, "simpleType");
		final Element restriction = createXsdElement(simpleType, "restriction");
		restriction.setAttribute("base", "string");
		final JsonNode enumNode = val.get("enum");
		for (int i = 0; i < enumNode.size(); i++)
		{
		    final String enumVal = enumNode.path(i).asText();
		    final Element enumElem = createXsdElement(restriction, "enumeration");
		    enumElem.setAttribute("value", enumVal);
		}
	}

	private static void handleNumber(Element nodeElem, String xsdType, JsonNode jsonNode)
	{
		final Integer minimum = getIntVal(jsonNode, "minimum");
		final Integer maximum = getIntVal(jsonNode, "maximum");
		
		if (minimum != null || maximum != null)
		{
			nodeElem.removeAttribute("type");
			final Element simpleType = createXsdElement(nodeElem, "simpleType");
			final Element restriction = createXsdElement(simpleType, "restriction");
			restriction.setAttribute("base", xsdType);
			
			if (minimum != null)
			{
				final Element min = createXsdElement(restriction, "minInclusive");
				min.setAttribute("value", Integer.toString(minimum));
			}
			
			if (maximum != null)
			{
				final Element max = createXsdElement(restriction, "maxInclusive");
				max.setAttribute("value", Integer.toString(maximum));
			}
		}
	}

	private static void handleArray(Element nodeElem, JsonNode jsonNode)
	{
		final JsonNode arrItems = jsonNode.path("items");
//		final String arrayXsdType = getType(arrItems.path("type").textValue(), arrItems.path("format").textValue());
		final String arrayXsdType = determineXsdType(arrItems.path("type").textValue(), arrItems);
		final boolean arrRequired = arrItems.path("required").booleanValue();
		if (! arrRequired)
		{
			nodeElem.setAttribute("minOccurs", "0");
		}
		final Element complexType = createXsdElement(nodeElem, "complexType");
		final Element sequence = createXsdElement(complexType, "sequence");
		final Element arrElem = createXsdElement(sequence, "element");
		if (arrayXsdType.equals("reference") ) {
			handleReference(arrElem, arrItems);
		}
		else if (arrayXsdType.equals("object")) {
			handleObject(arrElem, arrItems);
		}
		else {
			arrElem.setAttribute("name", "item");
			arrElem.setAttribute("type", arrayXsdType);
		}
		// TODO: Set restrictions for the array type, and possibly recurse into the type if "object"
		
		// Minimum items
		final Integer minItems = getIntVal(jsonNode, "minItems");
		arrElem.setAttribute("minOccurs", minItems != null ? Integer.toString(minItems) : "0");

		// Max Items
		final Integer maxItems = getIntVal(jsonNode, "maxItems");
		arrElem.setAttribute("maxOccurs", maxItems != null ? Integer.toString(maxItems) : "unbounded");

	}

	private static String determineXsdType(String key, JsonNode node)
	{
		String jsonType = node.path("type").textValue();
		final String jsonFormat = node.path("format").textValue();
		final boolean isEnum = node.get("enum") != null;
		final boolean isRef = node.get("$ref") != null;
		if (isRef) {
			return "reference";
		}
		else if (isEnum)
		{
			return "enum";
		}
		else
		{
			Assert.notNull(jsonType, "type must be specified on node '" + key + "': " + node);
			final String xsdType = getType(jsonType, jsonFormat);
			Assert.notNull(xsdType, "Unable to determine XSD type for json type=" + jsonType + ", format=" + jsonFormat);
			return xsdType;
		}

	}

	private static Integer getIntVal(JsonNode node, String attribute)
	{
		return node.get(attribute) != null ? node.get(attribute).intValue() : null;
	}

	private static Element createXsdElement(Node element, String name)
	{
		return XmlUtil.createXsdElement(element, name);
	}

	private static String getType(String type, String format)
	{
		final String key = (type + (format != null ? ("|" + format) : "")).toLowerCase();
		final String retVal = typeMapping.get(key);
		return retVal;
	}
}
