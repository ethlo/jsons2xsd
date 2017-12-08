package com.ethlo.jsons2xsd;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Jsons2Xsd
{
    private static final ObjectMapper mapper = new ObjectMapper();

    public enum OuterWrapping
    {
        ELEMENT, TYPE
    }

    private static final Map<String, String> typeMapping = new HashMap<>();

    public static final String TYPE_STRING = "string";
    public static final String TYPE_OBJECT = "object";
    public static final String TYPE_ARRAY = "array";
    public static final String TYPE_REFERENCE = "reference";
    public static final String TYPE_BOOLEAN = "boolean";
    
    public static final String JS_PROPERTIES = "properties";
    public static final String JS_REQUIRED = "required";
    
    public static final String XSD_ATTRIBUTE = "attribute";
    public static final String XSD_ELEMENT = "element";
    public static final String XSD_SEQUENCE = "sequence";
    public static final String XSD_COMPLEXTYPE = "complexType";
    public static final String XSD_SIMPLETYPE = "simpleType";
    public static final String XSD_RESTRICTION = "restriction";
    public static final String XSD_VALUE = "value";

    static
    {
        // Primitive types
        typeMapping.put(TYPE_STRING, TYPE_STRING);
        typeMapping.put(TYPE_OBJECT, TYPE_OBJECT);
        typeMapping.put(TYPE_ARRAY, TYPE_ARRAY);
        typeMapping.put("number", "decimal");
        typeMapping.put(TYPE_BOOLEAN, TYPE_BOOLEAN);
        typeMapping.put("integer", "int");

        // Non-standard, often encountered in the wild
        typeMapping.put("int", "int");
        typeMapping.put("date-time", "dateTime");
        typeMapping.put("time", "time");
        typeMapping.put("date", "date");

        // String formats
        typeMapping.put("string|uri", "anyURI");
        typeMapping.put("string|email", TYPE_STRING);
        typeMapping.put("string|phone", TYPE_STRING);
        typeMapping.put("string|date-time", "dateTime");
        typeMapping.put("string|date", "date");
        typeMapping.put("string|time", "time");
        typeMapping.put("string|utc-millisec", "long");
        typeMapping.put("string|regex", TYPE_STRING);
        typeMapping.put("string|color", TYPE_STRING);
        typeMapping.put("string|style", TYPE_STRING);
    }
    
    public static Document convert(Reader jsonSchema, Reader definitionSchema, String targetNameSpaceUri, OuterWrapping wrapping, String ns) throws JsonProcessingException, IOException
    {
        final Set<String> neededElements = new HashSet<>();

        JsonNode rootNode = mapper.readTree(jsonSchema);

        final Document xsdDoc = XmlUtil.newDocument();
        xsdDoc.setXmlStandalone(true);

        final Element schemaRoot = createXsdElement(xsdDoc, "schema");
        schemaRoot.setAttribute("targetNamespace", targetNameSpaceUri);
        schemaRoot.setAttribute("xmlns:" + ns.toLowerCase(), targetNameSpaceUri);

        schemaRoot.setAttribute("elementFormDefault", "qualified");
        schemaRoot.setAttribute("attributeFormDefault", "qualified");


        final String type = rootNode.path("type").textValue();
        Assert.isTrue("object".equals(type), "root should have type=\"object\"");

        final JsonNode properties = rootNode.get("properties");
        Assert.notNull(properties, "\"properties\" property should be found in root of JSON schema\"");



        Element wrapper = schemaRoot;
        if (wrapping == OuterWrapping.ELEMENT)
        {
            wrapper = createXsdElement(schemaRoot, "element");
            wrapper.setAttribute("name", ns);
            wrapper.setAttribute("type", ns.toLowerCase() + ":" + ns);

        }

        final Element schemaComplexType = createXsdElement(schemaRoot, "complexType");

    //if (wrapping == OuterWrapping.TYPE)
        {
            schemaComplexType.setAttribute("name", ns);
        }
        final Element schemaSequence = createXsdElement(schemaComplexType, "sequence");

        doIterate(neededElements, schemaSequence, properties, ns.toLowerCase());





        //find references in Defs
        JsonNode definitionsRootNode = mapper.readTree(definitionSchema);

        final JsonNode definitions = definitionsRootNode.path("definitions");
        Assert.notNull(definitions, "\"definitions\"  should be found in root of JSON schema\"");
        final Iterator<Entry<String, JsonNode>> fieldIter = definitions.fields();
        while(fieldIter.hasNext()) {
            //Create a complex type
            //get properties
            //call doiteration with properties

            final Entry<String, JsonNode> entry = fieldIter.next();
            final String key = entry.getKey();
            final JsonNode val = entry.getValue();
            if (neededElements.contains(key)) {

                final Element definitionComplexType = createXsdElement(schemaRoot, "complexType");
                definitionComplexType.setAttribute("name", key);


                final Element defSchemaSequence = createXsdElement(definitionComplexType, "sequence");
                final JsonNode defProperties = val.get("properties");
                Assert.notNull(defProperties, "\"properties\" property should be found in \"" + key + "\"");

                doIterate(neededElements, defSchemaSequence, defProperties, ns.toLowerCase());
            }

        }



        return xsdDoc;
    }
    
    private static void doIterate(Set<String> neededElements, Element elem, JsonNode node, String ns)
    {
        final Iterator<Entry<String, JsonNode>> fieldIter = node.fields();
        while(fieldIter.hasNext())
        {
            final Entry<String, JsonNode> entry = fieldIter.next();
            final String key = entry.getKey();
            final JsonNode val = entry.getValue();
            doIterateSingle(neededElements, key, val, elem, ns);
        }
    }
    
    private static void doIterateSingle(Set<String> neededElements, String key, JsonNode val, Element elem, String ns)
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
                handleArray(neededElements, nodeElem, val, ns);
                return;
                
            case "decimal":
            case "int":
                handleNumber(nodeElem, xsdType, val);
                return;
                
            case "enum":
                handleEnum(nodeElem, val);
                return;
                
            case "object":
                handleObject(neededElements, nodeElem, val, ns);
                return;
                
            case "string":
                handleString(nodeElem, val);
                return;

            case "reference":
                handleReference(neededElements, nodeElem, val, ns);
                return;
        }
    }

    public static Document convert(Reader jsonSchema, String nsUri, OuterWrapping wrapping, String nsAlias) throws IOException
    {
        final JsonNode rootNode = mapper.readTree(jsonSchema);

        final Element schemaRoot = createDocument(nsAlias, nsUri);
        
        final Set<String> neededElements = new HashSet<>();
        
        final String type = rootNode.path("type").textValue();
        JsonNode properties;
        switch (type)
        {
            case TYPE_OBJECT:
                properties = rootNode.get(JS_PROPERTIES);
                Assert.notNull(properties, "\"properties\" property should be found in root of JSON schema\"");
                
                if (wrapping == OuterWrapping.ELEMENT)
                {
                    final Element wrapper = createXsdElement(schemaRoot, XSD_ELEMENT);
                    wrapper.setAttribute("name", nsAlias);
                    wrapper.setAttribute("type", nsAlias.toLowerCase() + ":" + nsAlias);
                }

                final Element schemaComplexType = createXsdElement(schemaRoot, XSD_COMPLEXTYPE);
                schemaComplexType.setAttribute("name", nsAlias);

                final Element schemaSequence = createXsdElement(schemaComplexType, XSD_SEQUENCE);
                
                doIterate(neededElements, schemaSequence, properties, getRequiredList(rootNode), nsAlias);
                break;

            case TYPE_ARRAY:
                handleArray(neededElements, schemaRoot, rootNode, nsAlias);
                break;

            default:
                throw new IllegalArgumentException("Unknown root type: " + type);
        }

        // Handle type definitions
        final JsonNode definitions = rootNode.path("definitions");
        Assert.notNull(definitions, "\"definitions\" should be found in root of JSON schema\"");

        doIterateDefinitions(neededElements, schemaRoot, definitions, nsAlias);

        return schemaRoot.getOwnerDocument();
    }

    private static Element createDocument(String nsAlias, String nsUri)
    {
        final Document xsdDoc = XmlUtil.newDocument();
        xsdDoc.setXmlStandalone(true);

        final Element schemaRoot = createXsdElement(xsdDoc, "schema");
        schemaRoot.setAttribute("targetNamespace", nsUri);
        schemaRoot.setAttribute("xmlns:" + nsAlias.toLowerCase(), nsUri);
        schemaRoot.setAttribute("elementFormDefault", "qualified");
        return schemaRoot;
    }

    private static void doIterateDefinitions(Set<String> neededElements, Element elem, JsonNode node, String ns)
    {
        final Iterator<Entry<String, JsonNode>> fieldIter = node.fields();
        while (fieldIter.hasNext())
        {
            final Entry<String, JsonNode> entry = fieldIter.next();
            final String key = entry.getKey();
            final JsonNode val = entry.getValue();
            if (key.equals("Link"))
            {
                final Element schemaComplexType = createXsdElement(elem, XSD_COMPLEXTYPE);
                schemaComplexType.setAttribute("name", key);
                final Element href = createXsdElement(schemaComplexType, XSD_ATTRIBUTE);
                final Element rel = createXsdElement(schemaComplexType, XSD_ATTRIBUTE);
                final Element title = createXsdElement(schemaComplexType, XSD_ATTRIBUTE);
                final Element method = createXsdElement(schemaComplexType, XSD_ATTRIBUTE);
                final Element type = createXsdElement(schemaComplexType, XSD_ATTRIBUTE);

                href.setAttribute("name", "href");
                href.setAttribute("type", TYPE_STRING);

                rel.setAttribute("name", "rel");
                rel.setAttribute("type", TYPE_STRING);

                title.setAttribute("name", "title");
                title.setAttribute("type", TYPE_STRING);

                method.setAttribute("name", "method");
                method.setAttribute("type", TYPE_STRING);

                type.setAttribute("name", "type");
                type.setAttribute("type", TYPE_STRING);

            }
            else
            {

                final Element schemaComplexType = createXsdElement(elem, XSD_COMPLEXTYPE);
                schemaComplexType.setAttribute("name", key);

                final Element schemaSequence = createXsdElement(schemaComplexType, XSD_SEQUENCE);
                final JsonNode properties = val.get(JS_PROPERTIES);
                Assert.notNull(properties, "\"properties\" property should be found in \"" + key + "\"");

                doIterate(neededElements, schemaSequence, properties, getRequiredList(val), ns);
            }

        }
    }

    private static void doIterate(Set<String> neededElements, Element elem, JsonNode node, List<String> requiredList, String ns)
    {
        final Iterator<Entry<String, JsonNode>> fieldIter = node.fields();
        while (fieldIter.hasNext())
        {
            final Entry<String, JsonNode> entry = fieldIter.next();
            final String key = entry.getKey();
            final JsonNode val = entry.getValue();
            doIterateSingle(neededElements, key, val, elem, requiredList.contains(key), ns);
        }
    }

    private static void doIterateSingle(Set<String> neededElements, String key, JsonNode val, Element elem, boolean required, String ns)
    {
        final String xsdType = determineXsdType(key, val);
        final Element nodeElem = createXsdElement(elem, XSD_ELEMENT);
        String name;
        if (!key.equals("link"))
        {
            name = key.substring(0, 1).toUpperCase() + key.substring(1);
        }
        else
        {
            name = key;
        }

        nodeElem.setAttribute("name", name);

        if (!TYPE_OBJECT.equals(xsdType) && !TYPE_ARRAY.equals(xsdType))
        {
            // Simple type
            nodeElem.setAttribute("type", xsdType);
        }

        if (!required)
        {
            // Not required
            nodeElem.setAttribute("minOccurs", "0");
        }

        switch (xsdType)
        {
            case TYPE_ARRAY:
                handleArray(neededElements, nodeElem, val, ns);
                break;

            case "decimal":
            case "int":
                handleNumber(nodeElem, xsdType, val);
                break;

            case "enum":
                handleEnum(nodeElem, val);
                break;

            case TYPE_OBJECT:
                handleObject(neededElements, nodeElem, val, ns);
                break;

            case TYPE_STRING:
                handleString(nodeElem, val);
                break;

            case TYPE_REFERENCE:
                handleReference(neededElements, nodeElem, val, ns);
                break;
        }
    }

    private static void handleReference(Set<String> neededElements, Element nodeElem, JsonNode val, String ns)
    {
        final JsonNode refs = val.get("$ref");
        nodeElem.removeAttribute("type");
        String fixRef = refs.asText().replace("#/definitions/", ns + ":");
        String name = fixRef.substring(ns.length() + 1);
        String oldName = nodeElem.getAttribute("name");

        if (oldName.length() <= 0)
        {
            nodeElem.setAttribute("name", name);
        }
        nodeElem.setAttribute("type", fixRef);
        
        neededElements.add(name);
    }

    private static void handleString(Element nodeElem, JsonNode val)
    {
        final Integer minimumLength = getIntVal(val, "minLength");
        final Integer maximumLength = getIntVal(val, "maxLength");
        final String expression = val.path("pattern").textValue();

        if (minimumLength != null || maximumLength != null || expression != null)
        {
            nodeElem.removeAttribute("type");
            final Element simpleType = createXsdElement(nodeElem, XSD_SIMPLETYPE);
            final Element restriction = createXsdElement(simpleType, XSD_RESTRICTION);
            restriction.setAttribute("base", TYPE_STRING);

            if (minimumLength != null)
            {
                final Element min = createXsdElement(restriction, "minLength");
                min.setAttribute(XSD_VALUE, Integer.toString(minimumLength));
            }

            if (maximumLength != null)
            {
                final Element max = createXsdElement(restriction, "maxLength");
                max.setAttribute(XSD_VALUE, Integer.toString(maximumLength));
            }

            if (expression != null)
            {
                final Element max = createXsdElement(restriction, "pattern");
                max.setAttribute(XSD_VALUE, expression);
            }
        }
    }

    private static void handleObject(Set<String> neededElements, Element nodeElem, JsonNode val, String ns)
    {
        final JsonNode properties = val.get(JS_PROPERTIES);
        if (properties != null)
        {
            final Element complexType = createXsdElement(nodeElem, XSD_COMPLEXTYPE);
            final Element sequence = createXsdElement(complexType, XSD_SEQUENCE);
            Assert.notNull(properties, "'object' type must have a 'properties' attribute");
            doIterate(neededElements, sequence, properties, getRequiredList(val), ns);
        }
    }

    private static void handleEnum(Element nodeElem, JsonNode val)
    {
        nodeElem.removeAttribute("type");
        final Element simpleType = createXsdElement(nodeElem, XSD_SIMPLETYPE);
        final Element restriction = createXsdElement(simpleType, XSD_RESTRICTION);
        restriction.setAttribute("base", TYPE_STRING);
        final JsonNode enumNode = val.get("enum");
        for (int i = 0; i < enumNode.size(); i++)
        {
            final String enumVal = enumNode.path(i).asText();
            final Element enumElem = createXsdElement(restriction, "enumeration");
            enumElem.setAttribute(XSD_VALUE, enumVal);
        }
    }

    private static void handleNumber(Element nodeElem, String xsdType, JsonNode jsonNode)
    {
        final Integer minimum = getIntVal(jsonNode, "minimum");
        final Integer maximum = getIntVal(jsonNode, "maximum");

        if (minimum != null || maximum != null)
        {
            nodeElem.removeAttribute("type");
            final Element simpleType = createXsdElement(nodeElem, XSD_SIMPLETYPE);
            final Element restriction = createXsdElement(simpleType, XSD_RESTRICTION);
            restriction.setAttribute("base", xsdType);

            if (minimum != null)
            {
                final Element min = createXsdElement(restriction, "minInclusive");
                min.setAttribute(XSD_VALUE, Integer.toString(minimum));
            }

            if (maximum != null)
            {
                final Element max = createXsdElement(restriction, "maxInclusive");
                max.setAttribute(XSD_VALUE, Integer.toString(maximum));
            }
        }
    }

    private static void handleArray(Set<String> neededElements, Element nodeElem, JsonNode jsonNode, String ns)
    {
        final JsonNode arrItems = jsonNode.path("items");
        final String arrayXsdType = determineXsdType(arrItems.path("type").textValue(), arrItems);
        final Element complexType = createXsdElement(nodeElem, XSD_COMPLEXTYPE);
        final Element sequence = createXsdElement(complexType, XSD_SEQUENCE);
        final Element arrElem = createXsdElement(sequence, XSD_ELEMENT);
        if (arrayXsdType.equals(TYPE_REFERENCE))
        {
            handleReference(neededElements, arrElem, arrItems, ns);
        }
        else if (arrayXsdType.equals(TYPE_OBJECT))
        {
            handleObject(neededElements, arrElem, arrItems, ns);
        }
        else
        {
            arrElem.setAttribute("name", "item");
            arrElem.setAttribute("type", arrayXsdType);
        }
        
        // TODO: Set restrictions for the array type, and possibly recurse into the type if TYPE_OBJECT

        // Minimum items
        final Integer minItems = getIntVal(jsonNode, "minItems");
        arrElem.setAttribute("minOccurs", minItems != null ? Integer.toString(minItems) : "0");

        // Max Items
        final Integer maxItems = getIntVal(jsonNode, "maxItems");
        arrElem.setAttribute("maxOccurs", maxItems != null ? Integer.toString(maxItems) : "unbounded");

    }

    private static String determineXsdType(String key, JsonNode node)
    {
        final String jsonType = node.path("type").textValue();
        final String jsonFormat = node.path("format").textValue();
        final boolean isEnum = node.get("enum") != null;
        final boolean isRef = node.get("$ref") != null;
        if (isRef)
        {
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
        return typeMapping.get(key);
    }

    private static List<String> getRequiredList(JsonNode jsonNode)
    {
        if (jsonNode.path(JS_REQUIRED).isMissingNode())
        {
            return Collections.emptyList();
        }
        Assert.isTrue(jsonNode.path(JS_REQUIRED).isArray(), "'required' property must have type: array");
        List<String> requiredList = new ArrayList<>();
        for (JsonNode requiredField : jsonNode.withArray("required"))
        {
            Assert.isTrue(requiredField.isTextual(), "required must be string");
            requiredList.add(requiredField.asText());
        }
        return requiredList;
    }
}
