package com.ethlo.jsons2xsd;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Jsons2Xsd
{
	private final static ObjectMapper mapper = new ObjectMapper();
	
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
	
	public static Document convert(Reader jsonSchema, String targetNameSpaceUri) throws JsonProcessingException, IOException
	{
		JsonNode rootNode = mapper.readTree(jsonSchema);
		final String type = rootNode.path("type").textValue();
		assertTrue("object".equals(type), "root should have type=\"object\"");
		
		final JsonNode properties = rootNode.get("properties");
		assertNotNull(properties, "\"properties\" property should be found in root of JSON schema\"");
		
		final Document xsdDoc = newDocument();
		final Element schemaRoot = createElement(xsdDoc, "schema");
		schemaRoot.setAttribute("targetNamespace", targetNameSpaceUri);
		final Element schemaElement = createElement(schemaRoot, "element");
		schemaElement.setAttribute("name", "schemaname");
		final Element schemaComplexType = createElement(schemaElement, "complexType");
		final Element schemaSequence = createElement(schemaComplexType, "sequence");
		
		doIterate(schemaSequence, properties);
		
		return xsdDoc;
	}
	
	private static Document newDocument()
	{
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		try
		{
			final DocumentBuilder builder = factory.newDocumentBuilder();
			return builder.newDocument();
		}
		catch (ParserConfigurationException e)
		{
			throw new RuntimeException(e);
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
		final String jsonType = val.path("type").textValue();
		assertNotNull(jsonType, "type must be specified on node '" + key + "': " + val);
		final String xsdType = getType(jsonType, val.path("format").textValue());
		final boolean required = val.path("required").booleanValue();

		final Element newElem = createElement(elem, "element");
		newElem.setAttribute("name", key);
		if (! required)
		{
			newElem.setAttribute("minOccurs", "0");
		}
		
		if (! "object".equals(xsdType) && !"array".equals(xsdType))
		{
			newElem.setAttribute("type", xsdType);
		}
		
		if ("array".equals(xsdType))
		{
			final JsonNode arrItems = val.path("items");
			final String arrayXsdType = getType(arrItems.path("type").textValue(), arrItems.path("format").textValue());
			final boolean arrRequired = arrItems.path("required").booleanValue();
			if (! arrRequired)
			{
				newElem.setAttribute("minOccurs", "0");
			}
			final Element complexType = createElement(newElem, "complexType");
			final Element sequence = createElement(complexType, "sequence");
			final Element arrElem = createElement(sequence, "element");
			arrElem.setAttribute("name", "item");
			arrElem.setAttribute("type", arrayXsdType);
			
			// TODO: Set restrictions for the array type, and possibly recurse into the type if "object"
			
			// Minimum items
			final Integer minItems = val.get("minItems") != null ? val.get("minItems").intValue() : null;
			arrElem.setAttribute("minOccurs", minItems != null ? Integer.toString(minItems) : "0");

			// Max Items
			final Integer maxItems = val.get("maxItems") != null ? val.get("maxItems").intValue() : null;
			arrElem.setAttribute("maxOccurs", maxItems != null ? Integer.toString(maxItems) : "unbounded");
		}
		else if ("object".equals(xsdType))
		{
			if (! required)
			{
				newElem.setAttribute("minOccurs", "0");
			}
			final Element complexType = createElement(newElem, "complexType");
			final Element sequence = createElement(complexType, "sequence");
			final JsonNode properties = val.get("properties");
			doIterate(sequence, properties);
		}
		else if ("decimal".equals(xsdType) || "int".equals(xsdType))
		{
			final Integer minimum = getIntVal(val, "minimum");
			final Integer maximum = getIntVal(val, "maximum");
			
			if (minimum != null || maximum != null)
			{
				newElem.removeAttribute("type");
				final Element simpleType = createElement(newElem, "simpleType");
				final Element restriction = createElement(simpleType, "restriction");
				restriction.setAttribute("base", xsdType);
				
				if (minimum != null)
				{
					final Element min = createElement(restriction, "minInclusive");
					min.setAttribute("value", Integer.toString(minimum));
				}
				
				if (maximum != null)
				{
					final Element max = createElement(restriction, "maxInclusive");
					max.setAttribute("value", Integer.toString(maximum));
				}
			}
		}
		else if ("string".equals(xsdType))
		{
			final Integer minimumLength = getIntVal(val, "minLength");
			final Integer maximumLength = getIntVal(val, "maxLength");
			final String expression = val.path("pattern").textValue();
			
			if (minimumLength  != null || maximumLength != null || expression != null)
			{
				newElem.removeAttribute("type");
				final Element simpleType = createElement(newElem, "simpleType");
				final Element restriction = createElement(simpleType, "restriction");
				restriction.setAttribute("base", xsdType);
				
				if (minimumLength != null)
				{
					final Element min = createElement(restriction, "minLength");
					min.setAttribute("value", Integer.toString(minimumLength));
				}
				
				if (maximumLength != null)
				{
					final Element max = createElement(restriction, "maxLength");
					max.setAttribute("value", Integer.toString(maximumLength));
				}
				
				if (expression != null)
				{
					final Element max = createElement(restriction, "pattern");
					max.setAttribute("value", expression);
				}
			}
		}
	}

	private static Integer getIntVal(JsonNode node, String attribute)
	{
		return node.get(attribute) != null ? node.get(attribute).intValue() : null;
	}

	private static Element createElement(Node element, String name)
	{
		assertNotNull(element, "element should never be null");
		final Document doc = element.getOwnerDocument() != null ? element.getOwnerDocument() : ((Document)element);
		final Element retVal = doc.createElementNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, name);
		element.appendChild(retVal);
		return retVal;
	}

	private static void assertTrue(boolean expression, String message)
	{
		if (! expression)
		{
			throw new IllegalArgumentException(message);
		}
	}
	
	private static void assertNotNull(Object obj, String message)
	{
		if (obj == null)
		{
			throw new IllegalArgumentException(message);
		}
	}

	private static String getType(String type, String format)
	{
		final String key = (type + (format != null ? ("|" + format) : "")).toLowerCase();
		final String retVal = typeMapping.get(key);
		return retVal;
	}
	
	public static String asXmlString(Node node) throws TransformerException 
	{
	    final Source source = new DOMSource(node);
        final StringWriter stringWriter = new StringWriter();
        final Result result = new StreamResult(stringWriter);
        final TransformerFactory factory = TransformerFactory.newInstance();
        final Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.transform(source, result);
        return stringWriter.getBuffer().toString();
	}
}
