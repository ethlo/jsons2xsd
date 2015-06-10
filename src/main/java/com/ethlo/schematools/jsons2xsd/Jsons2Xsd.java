package com.ethlo.schematools.jsons2xsd;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.XMLConstants;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Jsons2Xsd {
    private final static ObjectMapper mapper = new ObjectMapper();

    private static Map<String, Element> unusedElements;

    private static String ns;

    public static enum OuterWrapping {
        ELEMENT, TYPE;
    }

    private static final Map<String, String> typeMapping = new HashMap<>();

    static {
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

    public static Document convert(Reader jsonSchema, String targetNameSpaceUri, OuterWrapping wrapping, String name) throws JsonProcessingException, IOException {
        JsonNode rootNode = mapper.readTree(jsonSchema);

        final Document xsdDoc = XmlUtil.newDocument();
        xsdDoc.setXmlStandalone(true);

        final Element schemaRoot = createXsdElement(xsdDoc, "schema");
        schemaRoot.setAttribute("targetNamespace", targetNameSpaceUri);
        schemaRoot.setAttribute("xmlns:" + name.toLowerCase(), targetNameSpaceUri);
        ns = name.toLowerCase();


        schemaRoot.setAttribute("elementFormDefault", "qualified");
//		schemaRoot.setAttribute("attributeFormDefault", "qualified");


        final String type = rootNode.path("type").textValue();
        Assert.isTrue("object".equals(type), "root should have type=\"object\"");

        final JsonNode properties = rootNode.get("properties");
        Assert.notNull(properties, "\"properties\" property should be found in root of JSON schema\"");


        Element wrapper = schemaRoot;
        if (wrapping == OuterWrapping.ELEMENT) {
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

        //handle external defs
        final JsonNode definitions = rootNode.path("definitions");
        Assert.notNull(definitions, "\"definitions\"  should be found in root of JSON schema\"");

        doIterateDefinitions(schemaRoot, definitions);


        return xsdDoc;
    }

    private static void doIterateDefinitions(Element elem, JsonNode node) {
        final Iterator<Entry<String, JsonNode>> fieldIter = node.fields();
        while (fieldIter.hasNext()) {



            //Create a complex type
            //get properties
            //call doiteration with properties

            final Entry<String, JsonNode> entry = fieldIter.next();
            final String key = entry.getKey();
            final JsonNode val = entry.getValue();
            if (key.equals("Link")) {
                final Element schemaComplexType = createXsdElement(elem, "complexType");
                schemaComplexType.setAttribute("name", key);
                final Element href = createXsdElement(schemaComplexType, "attribute");
                final Element rel = createXsdElement(schemaComplexType, "attribute");
                final Element title = createXsdElement(schemaComplexType, "attribute");
                final Element method = createXsdElement(schemaComplexType, "attribute");
                final Element type = createXsdElement(schemaComplexType, "attribute");

                href.setAttribute("name", "href");
                href.setAttribute("type", "string");

                rel.setAttribute("name", "rel");
                rel.setAttribute("type", "string");

                title.setAttribute("name", "title");
                title.setAttribute("type", "string");

                method.setAttribute("name", "method");
                method.setAttribute("type", "string");

                type.setAttribute("name", "type");
                type.setAttribute("type", "string");


            }
            else {


                final Element schemaComplexType = createXsdElement(elem, "complexType");
                schemaComplexType.setAttribute("name", key);

                final Element schemaSequence = createXsdElement(schemaComplexType, "sequence");
                final JsonNode properties = val.get("properties");
                Assert.notNull(properties, "\"properties\" property should be found in \"" + key + "\"");

                doIterate(schemaSequence, properties);
            }

        }
    }

    private static void doIterate(Element elem, JsonNode node) {
        final Iterator<Entry<String, JsonNode>> fieldIter = node.fields();
        while (fieldIter.hasNext()) {
            final Entry<String, JsonNode> entry = fieldIter.next();
            final String key = entry.getKey();
            final JsonNode val = entry.getValue();
            doIterateSingle(key, val, elem);
        }
    }

    private static void doIterateSingle(String key, JsonNode val, Element elem) {
        final String xsdType = determineXsdType(key, val);
        final boolean required = val.path("required").booleanValue();
        final Element nodeElem = createXsdElement(elem, "element");
        String name;
        if (!key.equals("link")) {
            name = key.substring(0, 1).toUpperCase() + key.substring(1);
        } else {
            name = key;
        }
        nodeElem.setAttribute("name", name);

        if (!"object".equals(xsdType) && !"array".equals(xsdType)) {
            // Simple type
            nodeElem.setAttribute("type", xsdType);
        }
        if (! required)
        {
            // Not required
            nodeElem.setAttribute("minOccurs", "0");
        }

        switch (xsdType) {
            case "array":
                handleArray(nodeElem, val);
                break;

            case "decimal":
            case "int":
                handleNumber(nodeElem, xsdType, val);
                break;

            case "enum":
                handleEnum(nodeElem, val);
                break;

            case "object":
                handleObject(nodeElem, val);
                break;

            case "string":
                handleString(nodeElem, val);
                break;

            case "reference":
                handleReference(nodeElem, val);
                break;
        }


    }


    private static void handleReference(Element nodeElem, JsonNode val) {
        final JsonNode refs = val.get("$ref");
        nodeElem.removeAttribute("type");
        String fixRef = refs.asText().replace("#/definitions/", ns + ":");
        String name = fixRef.substring(ns.length() + 1);
        String oldName = nodeElem.getAttribute("name");


        if (oldName.length() <= 0) {
            nodeElem.setAttribute("name", name);
        }
        nodeElem.setAttribute("type", fixRef);


    }

    private static void handleString(Element nodeElem, JsonNode val) {
        final Integer minimumLength = getIntVal(val, "minLength");
        final Integer maximumLength = getIntVal(val, "maxLength");
        final String expression = val.path("pattern").textValue();


        if (minimumLength != null || maximumLength != null || expression != null) {
            nodeElem.removeAttribute("type");
            final Element simpleType = createXsdElement(nodeElem, "simpleType");
            final Element restriction = createXsdElement(simpleType, "restriction");
            restriction.setAttribute("base", "string");

            if (minimumLength != null) {
                final Element min = createXsdElement(restriction, "minLength");
                min.setAttribute("value", Integer.toString(minimumLength));
            }

            if (maximumLength != null) {
                final Element max = createXsdElement(restriction, "maxLength");
                max.setAttribute("value", Integer.toString(maximumLength));
            }

            if (expression != null) {
                final Element max = createXsdElement(restriction, "pattern");
                max.setAttribute("value", expression);
            }
        }
    }

    private static void handleObject(Element nodeElem, JsonNode val) {
        final JsonNode properties = val.get("properties");
        if (properties != null) {
            System.out.println("In PROP");
            final Element complexType = createXsdElement(nodeElem, "complexType");
            final Element sequence = createXsdElement(complexType, "sequence");
            Assert.notNull(properties, "'object' type must have a 'properties' attribute");
            doIterate(sequence, properties);
        }

    }

    private static void handleEnum(Element nodeElem, JsonNode val) {
        nodeElem.removeAttribute("type");
        final Element simpleType = createXsdElement(nodeElem, "simpleType");
        final Element restriction = createXsdElement(simpleType, "restriction");
        restriction.setAttribute("base", "string");
        final JsonNode enumNode = val.get("enum");
        for (int i = 0; i < enumNode.size(); i++) {
            final String enumVal = enumNode.path(i).asText();
            final Element enumElem = createXsdElement(restriction, "enumeration");
            enumElem.setAttribute("value", enumVal);
        }
    }

    private static void handleNumber(Element nodeElem, String xsdType, JsonNode jsonNode) {
        final Integer minimum = getIntVal(jsonNode, "minimum");
        final Integer maximum = getIntVal(jsonNode, "maximum");

        if (minimum != null || maximum != null) {
            nodeElem.removeAttribute("type");
            final Element simpleType = createXsdElement(nodeElem, "simpleType");
            final Element restriction = createXsdElement(simpleType, "restriction");
            restriction.setAttribute("base", xsdType);

            if (minimum != null) {
                final Element min = createXsdElement(restriction, "minInclusive");
                min.setAttribute("value", Integer.toString(minimum));
            }

            if (maximum != null) {
                final Element max = createXsdElement(restriction, "maxInclusive");
                max.setAttribute("value", Integer.toString(maximum));
            }
        }
    }

    private static void handleArray(Element nodeElem, JsonNode jsonNode) {
//        //First build the outer container.
//        final Element outerComplexType = createXsdElement(nodeElem, "complexType");
//        outerComplexType.setAttribute("name", "OuterContainer");
//        final Element outerSequence = createXsdElement(outerComplexType, "sequence");
//        final Element element = createXsdElement(outerSequence, "element");
//        element.setAttribute("name", "Inner");
//        element.setAttribute("type", "InnterType");


        final JsonNode arrItems = jsonNode.path("items");
//		final String arrayXsdType = getType(arrItems.path("type").textValue(), arrItems.path("format").textValue());
        final String arrayXsdType = determineXsdType(arrItems.path("type").textValue(), arrItems);
        final boolean arrRequired = arrItems.path("required").booleanValue();
        if (!arrRequired) {
            nodeElem.setAttribute("minOccurs", "0");
        }
        final Element complexType = createXsdElement(nodeElem, "complexType");
        final Element sequence = createXsdElement(complexType, "sequence");
        final Element arrElem = createXsdElement(sequence, "element");
        if (arrayXsdType.equals("reference")) {
            handleReference(arrElem, arrItems);
        } else if (arrayXsdType.equals("object")) {
            handleObject(arrElem, arrItems);
        } else {
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

    private static String determineXsdType(String key, JsonNode node) {
        String jsonType = node.path("type").textValue();
        final String jsonFormat = node.path("format").textValue();
        final boolean isEnum = node.get("enum") != null;
        final boolean isRef = node.get("$ref") != null;
        if (isRef) {
            return "reference";
        } else if (isEnum) {
            return "enum";
        } else {
            Assert.notNull(jsonType, "type must be specified on node '" + key + "': " + node);
            final String xsdType = getType(jsonType, jsonFormat);
            Assert.notNull(xsdType, "Unable to determine XSD type for json type=" + jsonType + ", format=" + jsonFormat);
            return xsdType;
        }

    }

    private static Integer getIntVal(JsonNode node, String attribute) {
        return node.get(attribute) != null ? node.get(attribute).intValue() : null;
    }

    private static Element createXsdElement(Node element, String name) {
        return XmlUtil.createXsdElement(element, name);
    }

    private static Attr createXsdAttr(Node element, String name) {
        return XmlUtil.createXsdAttr(element, name);
    }

    private static String getType(String type, String format) {
        final String key = (type + (format != null ? ("|" + format) : "")).toLowerCase();
        final String retVal = typeMapping.get(key);
        return retVal;
    }
}
