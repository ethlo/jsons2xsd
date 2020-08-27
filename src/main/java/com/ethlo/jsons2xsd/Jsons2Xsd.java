package com.ethlo.jsons2xsd;

/*-
 * #%L
 * jsons2xsd
 * %%
 * Copyright (C) 2014 - 2020 Morten Haraldsen (ethlo)
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.Map.Entry;

public class Jsons2Xsd
{
    private static final String TYPE_REFERENCE = "reference";
    private static final String TYPE_ENUM = "enum";

    private static final String FIELD_NAME = "name";
    private static final String FIELD_PROPERTIES = "properties";
    private static final String FIELD_ITEMS = "items";
    private static final String FIELD_REQUIRED = "required";

    private static final String XSD_ATTRIBUTE = "attribute";
    private static final String XSD_ELEMENT = "element";
    private static final String XSD_SEQUENCE = "sequence";
    private static final String XSD_COMPLEXTYPE = "complexType";
    private static final String XSD_SIMPLETYPE = "simpleType";
    private static final String XSD_RESTRICTION = "restriction";
    private static final String XSD_VALUE = "value";
    private static final String XSD_CHOICE = "choice";

    private static final String XSD_OBJECT = "object";
    private static final String XSD_ARRAY = "array";

    private static final String JSON_REF = "$ref";
    private static final String JSON_DEFINITIONS = "definitions";

    private static final Map<String, String> typeMapping = new HashMap<>();

    static
    {
        // Primitive types
        typeMapping.put(JsonSimpleType.STRING_VALUE, XsdSimpleType.STRING_VALUE);
        typeMapping.put(JsonComplexType.OBJECT_VALUE, XsdComplexType.OBJECT_VALUE);
        typeMapping.put(JsonComplexType.ARRAY_VALUE, XsdComplexType.ARRAY_VALUE);
        typeMapping.put(JsonSimpleType.NUMBER_VALUE, XsdSimpleType.DECIMAL_VALUE);
        typeMapping.put(JsonSimpleType.BOOLEAN_VALUE, XsdSimpleType.BOOLEAN_VALUE);
        typeMapping.put(JsonSimpleType.INTEGER_VALUE, XsdSimpleType.INT_VALUE);

        // String formats
        typeMapping.put("string|uri", "anyURI");
        typeMapping.put("string|email", XsdSimpleType.STRING_VALUE);
        typeMapping.put("string|phone", XsdSimpleType.STRING_VALUE);
        typeMapping.put("string|date-time", XsdSimpleType.DATETIME_VALUE);
        typeMapping.put("string|date", XsdSimpleType.DATE_VALUE);
        typeMapping.put("string|time", XsdSimpleType.TIME_VALUE);
        typeMapping.put("string|utc-millisec", XsdSimpleType.LONG_VALUE);
        typeMapping.put("string|regex", XsdSimpleType.STRING_VALUE);
        typeMapping.put("string|color", XsdSimpleType.STRING_VALUE);
        typeMapping.put("string|style", XsdSimpleType.STRING_VALUE);
    }

    private Jsons2Xsd()
    {
    }

    private static final ObjectMapper mapper = new ObjectMapper();

    public static Document convert(Reader jsonSchema, Config cfg) throws IOException
    {
        return convert(jsonSchema, null, cfg);
    }

    public static Document convert(Reader jsonSchema, Reader definitionSchema, Config cfg) throws IOException
    {
        final JsonNode rootNode = mapper.readTree(jsonSchema);
        final Element schemaRoot = createDocument(cfg);

        final Set<String> neededElements = new LinkedHashSet<>();

        final String type = rootNode.path("type").textValue();
        Assert.notNull(type, "type property of root node must be defined");
        switch (type)
        {
            case JsonComplexType.OBJECT_VALUE:
                handleObjectSchema(cfg, rootNode, schemaRoot, neededElements);
                break;

            case JsonComplexType.ARRAY_VALUE:
                handleArraySchema(cfg, rootNode, schemaRoot, neededElements);
                break;

            default:
                throw new IllegalArgumentException("Unknown root type: " + type);
        }

        JsonNode definitions;
        if (definitionSchema != null)
        {
            final JsonNode definitionsRootNode = mapper.readTree(definitionSchema);
            definitions = definitionsRootNode.path(JSON_DEFINITIONS);
        }
        else
        {
            definitions = rootNode.path(JSON_DEFINITIONS);
        }

        doIterateDefinitions(neededElements, schemaRoot, definitions, cfg);

        if (cfg.isValidateXsdSchema())
        {
            XmlUtil.validateSchema(schemaRoot.getOwnerDocument());
        }

        return schemaRoot.getOwnerDocument();
    }

    private static void handleArraySchema(Config cfg, JsonNode rootNode, Element schemaRoot, Set<String> neededElements)
    {
        final JsonNode items = rootNode.path(FIELD_ITEMS);
        Assert.notNull(items, "\"items\" property should be found in root of an array schema\"");

        final Element schemaSequence = createRootElementIfNeeded(cfg, schemaRoot, rootNode);

        if (!items.isArray())
        {
            // Just one type possible
            doIterate(neededElements, schemaSequence, items.get(FIELD_PROPERTIES), getRequiredList(items), cfg);
        }
        else
        {
            doIterate(neededElements, schemaSequence, items, getRequiredList(rootNode), cfg);
        }
    }

    private static Element createRootElementIfNeeded(Config cfg, Element schemaRoot, JsonNode rootNode)
    {
        if (cfg.isCreateRootElement())
        {
            final Element wrapper = element(schemaRoot, XSD_ELEMENT);
            wrapper.setAttribute(FIELD_NAME, cfg.getRootElement());
            wrapper.setAttribute("type", cfg.getNsAlias() + ":" + cfg.getName());
        }

        final Element schemaComplexType = element(schemaRoot, XSD_COMPLEXTYPE);
        schemaComplexType.setAttribute(FIELD_NAME, cfg.getName());
        addDocumentation(schemaComplexType, rootNode);

        return element(schemaComplexType, XSD_SEQUENCE);
    }

    private static void handleObjectSchema(Config cfg, final JsonNode rootNode, final Element schemaRoot, final Set<String> neededElements)
    {
        JsonNode properties;
        properties = rootNode.get(FIELD_PROPERTIES);
        Assert.notNull(properties, "\"properties\" property should be found in root of JSON schema\"");

        final Element schemaSequence = createRootElementIfNeeded(cfg, schemaRoot, rootNode);

        doIterate(neededElements, schemaSequence, properties, getRequiredList(rootNode), cfg);
    }

    private static Element createDocument(Config cfg)
    {
        final Document xsdDoc = XmlUtil.newDocument();
        xsdDoc.setXmlStandalone(true);

        final Element schemaRoot = element(xsdDoc, "schema");
        schemaRoot.setAttribute("targetNamespace", cfg.getTargetNamespace());
        schemaRoot.setAttribute("xmlns:" + cfg.getNsAlias(), cfg.getTargetNamespace());
        schemaRoot.setAttribute("elementFormDefault", "qualified");
        if (cfg.isAttributesQualified())
        {
            schemaRoot.setAttribute("attributeFormDefault", "qualified");
        }
        return schemaRoot;
    }

    private static void doIterateDefinitions(Set<String> neededElements, Element elem, JsonNode node, Config cfg)
    {
        final Iterator<Entry<String, JsonNode>> iter = node.fields();
        while (iter.hasNext())
        {
            final Entry<String, JsonNode> entry = iter.next();
            final String key = entry.getKey();
            final JsonNode val = entry.getValue();

            if (!neededElements.contains(key) && cfg.isIncludeOnlyUsedTypes())
            {
                continue;
            }

            if (key.equals("Link"))
            {
                final Element schemaComplexType = element(elem, XSD_COMPLEXTYPE);
                schemaComplexType.setAttribute(FIELD_NAME, key);
                final Element href = element(schemaComplexType, XSD_ATTRIBUTE);
                final Element rel = element(schemaComplexType, XSD_ATTRIBUTE);
                final Element title = element(schemaComplexType, XSD_ATTRIBUTE);
                final Element method = element(schemaComplexType, XSD_ATTRIBUTE);
                final Element type = element(schemaComplexType, XSD_ATTRIBUTE);

                href.setAttribute(FIELD_NAME, "href");
                href.setAttribute("type", XsdSimpleType.STRING_VALUE);

                rel.setAttribute(FIELD_NAME, "rel");
                rel.setAttribute("type", XsdSimpleType.STRING_VALUE);

                title.setAttribute(FIELD_NAME, "title");
                title.setAttribute("type", XsdSimpleType.STRING_VALUE);

                method.setAttribute(FIELD_NAME, "method");
                method.setAttribute("type", XsdSimpleType.STRING_VALUE);

                type.setAttribute(FIELD_NAME, "type");
                type.setAttribute("type", XsdSimpleType.STRING_VALUE);
            }
            else
            {
                final String xsdType = determineXsdType(cfg, key, val);
                handleContent(neededElements, val, cfg, xsdType, elem);
                ((Element)elem.getLastChild()).setAttribute(FIELD_NAME, key);
            }
        }
    }

    private static void handleObject(Set<String> neededElements, Element elem, JsonNode node, Config cfg)
    {
        final JsonNode properties = node.get(FIELD_PROPERTIES);
        if (properties != null)
        {
            final Element complexType = element(elem, XSD_COMPLEXTYPE);
            addDocumentation(complexType, node);
            final Element schemaSequence = element(complexType, XSD_SEQUENCE);

            doIterate(neededElements, schemaSequence, properties, getRequiredList(node), cfg);
        }
        else if (node.get("oneOf") != null)
        {
            final ArrayNode oneOf = (ArrayNode) node.get("oneOf");
            handleChoice(neededElements, elem, oneOf, cfg);
        }
    }

    private static void handleChoice(Set<String> neededElements, Element elem, ArrayNode oneOf, Config cfg)
    {
        final Element complexTypeElem = element(elem, XSD_COMPLEXTYPE);
        final Element choiceElem = element(complexTypeElem, XSD_CHOICE);
        for (JsonNode e : oneOf)
        {
            final Element nodeElem = element(choiceElem, XSD_ELEMENT);
            final JsonNode refs = e.get(JSON_REF);
            String fixRef = refs.asText().replace("#/definitions/", cfg.getNsAlias() + ":");
            String name = fixRef.substring(cfg.getNsAlias().length() + 1);
            nodeElem.setAttribute(FIELD_NAME, name);
            nodeElem.setAttribute("type", fixRef);

            neededElements.add(name);
        }
    }

    private static void doIterate(Set<String> neededElements, Element elem, JsonNode node, List<String> requiredList, Config cfg)
    {
        if (node.isObject())
        {
            final Iterator<Entry<String, JsonNode>> fieldIter = node.fields();
            while (fieldIter.hasNext())
            {
                final Entry<String, JsonNode> entry = fieldIter.next();
                final String key = entry.getKey();
                final JsonNode val = entry.getValue();
                doIterateSingle(neededElements, key, val, elem, requiredList.contains(key), cfg);
            }
        }
        else if (node.isArray())
        {
            int i = 0;
            for (JsonNode entry : node)
            {
                final String key = String.format("item%s", i++);
                doIterateSingle(neededElements, key, entry, elem, requiredList.contains(key), cfg);
            }
        }
    }

    private static void doIterateSingle(Set<String> neededElements, String name, JsonNode val, Element elem, boolean required, Config cfg)
    {
        final String xsdType = determineXsdType(cfg, name, val);
        final Element nodeElem = element(elem, XSD_ELEMENT);
        addDocumentation(nodeElem, val);
        nodeElem.setAttribute(FIELD_NAME, name);

        if (!XSD_OBJECT.equals(xsdType) && !XSD_ARRAY.equals(xsdType))
        {
            // Simple type
            nodeElem.setAttribute("type", xsdType);
        }

        if (!required)
        {
            // Not required
            nodeElem.setAttribute("minOccurs", "0");
        }

        handleContent(neededElements, val, cfg, xsdType, nodeElem);
    }

    private static void handleContent(Set<String> neededElements, JsonNode val, Config cfg, String xsdType, Element nodeElem) {
        switch (xsdType)
        {
            case XSD_ARRAY:
                handleArray(neededElements, nodeElem, val, cfg);
                break;

            case XsdSimpleType.DECIMAL_VALUE:
            case XsdSimpleType.INT_VALUE:
                handleNumber(nodeElem, xsdType, val);
                break;

            case "enum":
                handleEnum(nodeElem, val);
                break;

            case XSD_OBJECT:
                handleObject(neededElements, nodeElem, val, cfg);
                break;

            case XsdSimpleType.STRING_VALUE:
                handleString(nodeElem, val);
                break;

            case TYPE_REFERENCE:
                handleReference(neededElements, nodeElem, val, cfg);
                break;

            default:
                if (nodeElem.getNodeName().equals("schema")) {
                    final Element simpleType = element(nodeElem, XSD_SIMPLETYPE);
                    final Element restriction = element(simpleType, XSD_RESTRICTION);
                    restriction.setAttribute("base", xsdType);
                }
        }
    }

    private static void handleReference(Set<String> neededElements, Element nodeElem, JsonNode val, Config cfg)
    {
        final JsonNode refs = val.get(JSON_REF);
        nodeElem.removeAttribute("type");
        String fixRef = refs.asText().replace("#/definitions/", cfg.getNsAlias() + ":");
        String name = fixRef.substring(cfg.getNsAlias().length() + 1);
        String oldName = nodeElem.getAttribute(FIELD_NAME);

        if (oldName.trim().length() == 0)
        {
            nodeElem.setAttribute(FIELD_NAME, cfg.getItemNameMapper().apply(name));
        }
        nodeElem.setAttribute("type", fixRef);

        neededElements.add(name);
    }

    private static void handleString(Element nodeElem, JsonNode val)
    {
        final Integer minimumLength = getIntVal(val, "minLength");
        final Integer maximumLength = getIntVal(val, "maxLength");
        final String expression = val.path("pattern").textValue();

        if (minimumLength != null || maximumLength != null || expression != null || nodeElem.getNodeName().equals("schema"))
        {
            nodeElem.removeAttribute("type");
            final Element simpleType = element(nodeElem, XSD_SIMPLETYPE);
            addDocumentation(simpleType, val);
            final Element restriction = element(simpleType, XSD_RESTRICTION);
            restriction.setAttribute("base", XsdSimpleType.STRING_VALUE);

            if (minimumLength != null)
            {
                final Element min = element(restriction, "minLength");
                min.setAttribute(XSD_VALUE, Integer.toString(minimumLength));
            }

            if (maximumLength != null)
            {
                final Element max = element(restriction, "maxLength");
                max.setAttribute(XSD_VALUE, Integer.toString(maximumLength));
            }

            if (expression != null)
            {
                final Element max = element(restriction, "pattern");
                max.setAttribute(XSD_VALUE, expression);
            }
        }
    }

    private static void handleEnum(Element nodeElem, JsonNode val)
    {
        nodeElem.removeAttribute("type");
        final Element simpleType = element(nodeElem, XSD_SIMPLETYPE);
        addDocumentation(simpleType, val);
        final Element restriction = element(simpleType, XSD_RESTRICTION);
        restriction.setAttribute("base", XsdSimpleType.STRING_VALUE);
        final JsonNode enumNode = val.get("enum");
        for (int i = 0; i < enumNode.size(); i++)
        {
            final String enumVal = enumNode.path(i).asText();
            final Element enumElem = element(restriction, "enumeration");
            enumElem.setAttribute(XSD_VALUE, enumVal);
        }
    }

    private static void handleNumber(Element nodeElem, String xsdType, JsonNode jsonNode)
    {
        final Long minimum = getLongVal(jsonNode, "minimum");
        final Long maximum = getLongVal(jsonNode, "maximum");

        if (minimum != null || maximum != null || nodeElem.getNodeName().equals("schema"))
        {
            nodeElem.removeAttribute("type");
            final Element simpleType = element(nodeElem, XSD_SIMPLETYPE);
            addDocumentation(simpleType, jsonNode);
            final Element restriction = element(simpleType, XSD_RESTRICTION);

            boolean shouldBeLong = false;
            if (minimum != null)
            {
                if (minimum < Integer.MIN_VALUE) {
                    shouldBeLong = true;
                }
                final Element min = element(restriction, "minInclusive");
                min.setAttribute(XSD_VALUE, Long.toString(minimum));
            }

            if (maximum != null)
            {
                if (maximum > Integer.MAX_VALUE) {
                    shouldBeLong = true;
                }
                final Element max = element(restriction, "maxInclusive");
                max.setAttribute(XSD_VALUE, Long.toString(maximum));
            }

            String xsdTypeToUse = shouldBeLong ? XsdSimpleType.LONG_VALUE : xsdType;
            restriction.setAttribute("base", xsdTypeToUse);
        }
    }

    private static void handleArray(Set<String> neededElements, Element nodeElem, JsonNode jsonNode, Config cfg)
    {
        final JsonNode arrItems = jsonNode.path("items");
        final String arrayXsdType = determineXsdType(cfg, arrItems.path("type").textValue(), arrItems);
        if (cfg.isUnwrapArrays()) {
            handleArrayElements(neededElements, jsonNode, arrItems, arrayXsdType, nodeElem, cfg);
        } else {
            final Element complexType = element(nodeElem, XSD_COMPLEXTYPE);
            final Element sequence = element(complexType, XSD_SEQUENCE);
            final Element arrElem = element(sequence, XSD_ELEMENT);

            handleArrayElements(neededElements, jsonNode, arrItems, arrayXsdType, arrElem, cfg);

            final String o = arrElem.getAttribute("name");
            if (o == null || o.trim().length() == 0)
            {
                arrElem.setAttribute(FIELD_NAME, "item");
            }
        }
    }

    private static void handleArrayElements(Set<String> neededElements, JsonNode jsonNode, final JsonNode arrItems, final String arrayXsdType, final Element arrElem, Config cfg)
    {
        if (arrayXsdType.equals(TYPE_REFERENCE))
        {
            handleReference(neededElements, arrElem, arrItems, cfg);
        }
        else if (arrayXsdType.equals(JsonComplexType.OBJECT_VALUE))
        {
            handleObject(neededElements, arrElem, arrItems, cfg);
        }
        else
        {
            String oldName = arrElem.getAttribute(FIELD_NAME);
            if (oldName.trim().length() == 0)
            {
                arrElem.setAttribute(FIELD_NAME, "item");
            }
            arrElem.setAttribute("type", arrayXsdType);
        }

        // Minimum items
        final Integer minItems = getIntVal(jsonNode, "minItems");
        arrElem.setAttribute("minOccurs", minItems != null ? Integer.toString(minItems) : "0");

        // Max Items
        final Integer maxItems = getIntVal(jsonNode, "maxItems");
        arrElem.setAttribute("maxOccurs", maxItems != null ? Integer.toString(maxItems) : "unbounded");
    }

    private static String determineXsdType(final Config cfg, String key, JsonNode node)
    {
        final String jsonType = node.path("type").textValue();
        final String jsonFormat = node.path("format").textValue();
        final boolean isEnum = node.get(TYPE_ENUM) != null;
        final boolean isRef = node.get(JSON_REF) != null;
        final boolean hasProperties = node.get(FIELD_PROPERTIES) != null;
        if (isRef)
        {
            return TYPE_REFERENCE;
        }
        else if (isEnum)
        {
            return TYPE_ENUM;
        }
        else if (hasProperties || jsonType.equalsIgnoreCase(JsonComplexType.OBJECT_VALUE))
        {
            return XsdComplexType.OBJECT_VALUE;
        }
        else if (jsonType.equalsIgnoreCase(JsonComplexType.ARRAY_VALUE))
        {
            return XsdComplexType.ARRAY_VALUE;
        }

        Assert.notNull(jsonType, "type must be specified on node '" + key + "': " + node);

        // Check built-in
        String xsdType = getType(jsonType, jsonFormat);
        if (xsdType != null)
        {
            return xsdType;
        }

        // Check cusom mapping in config
        xsdType = cfg.getType(jsonType, jsonFormat);
        if (xsdType != null)
        {
            return xsdType;
        }

        // Check for non-json mappings
        final Optional<Entry<String, String>> mapping = cfg.getTypeMapping()
                .entrySet()
                .stream()
                .filter(e->e.getKey().startsWith(jsonType + "|"))
                .findFirst();
        if (mapping.isPresent() && (isFormatMatch(mapping.get().getKey(), jsonType, jsonFormat) || cfg.isIgnoreUnknownFormats()))
        {
            return mapping.get().getValue();
        }

        throw new IllegalArgumentException("Unable to determine XSD type for json type=" + jsonType + ", format=" + jsonFormat);
    }

    private static void addDocumentation(Element element, JsonNode node) {
        final JsonNode description = node.get("description");
        final boolean parentIsElement = element.getParentNode().getNodeName().equals(XSD_ELEMENT);
        if(description != null && !parentIsElement) {
            final Element annotation = element(element, "annotation");
            final Element documentation = element(annotation, "documentation");
            documentation.setTextContent(description.textValue());
        }
    }

    private static boolean isFormatMatch(final String key, final String jsonType, final String jsonFormat)
    {
        return key.equalsIgnoreCase(jsonType + "|" + jsonFormat);
    }

    private static Integer getIntVal(JsonNode node, String attribute)
    {
        return node.get(attribute) != null ? node.get(attribute).intValue() : null;
    }

    private static Long getLongVal(JsonNode node, String attribute)
    {
        return node.get(attribute) != null ? node.get(attribute).longValue() : null;
    }

    private static Element element(Node element, String name)
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
        if (jsonNode.path(FIELD_REQUIRED).isMissingNode())
        {
            return Collections.emptyList();
        }
        Assert.isTrue(jsonNode.path(FIELD_REQUIRED).isArray(), "'required' property must have type: array");
        List<String> requiredList = new ArrayList<>();
        for (JsonNode requiredField : jsonNode.withArray(FIELD_REQUIRED))
        {
            Assert.isTrue(requiredField.isTextual(), "required must be string");
            requiredList.add(requiredField.asText());
        }
        return requiredList;
    }
}
