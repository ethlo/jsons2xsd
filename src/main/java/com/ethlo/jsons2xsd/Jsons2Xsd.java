package com.ethlo.jsons2xsd;

/*-
 * #%L
 * jsons2xsd
 * %%
 * Copyright (C) 2014 - 2017 Morten Haraldsen (ethlo)
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

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Jsons2Xsd
{
    private Jsons2Xsd(){}
    
    private static final ObjectMapper mapper = new ObjectMapper();

    private static final Map<String, String> typeMapping = new HashMap<>();

    public static final String TYPE_STRING = "string";
    public static final String TYPE_OBJECT = "object";
    public static final String TYPE_ARRAY = "array";
    public static final String TYPE_REFERENCE = "reference";
    public static final String TYPE_BOOLEAN = "boolean";
    public static final String TYPE_DECIMAL = "decimal";
    public static final String TYPE_ENUM = "enum";
    
    public static final String FIELD_NAME = "name";
    public static final String FIELD_PROPERTIES = "properties";
    public static final String FIELD_REQUIRED = "required";
    
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
        typeMapping.put("number", TYPE_DECIMAL);
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
    

    public static Document convert(Reader jsonSchema, Config cfg) throws IOException
    {
        return convert(jsonSchema, null, cfg);
    }
    
    public static Document convert(Reader jsonSchema, Reader definitionSchema, Config cfg) throws IOException
    {
        final JsonNode rootNode = mapper.readTree(jsonSchema);
        final String nsAlias = cfg.getNsAlias();
        final Element schemaRoot = createDocument(cfg);
        
        final Set<String> neededElements = new LinkedHashSet<>();
        
        final String type = rootNode.path("type").textValue();
        switch (type)
        {
            case TYPE_OBJECT:
                handleObjectSchema(cfg, rootNode, nsAlias, schemaRoot, neededElements);
                break;

            case TYPE_ARRAY:
                final JsonNode arrItems = rootNode.path("items");
                final String arrayXsdType = determineXsdType(arrItems.path("type").textValue(), arrItems);
                handleArrayElements(neededElements, rootNode, cfg.getTargetNamespace(), arrItems, arrayXsdType, schemaRoot);
                break;

            default:
                throw new IllegalArgumentException("Unknown root type: " + type);
        }

        JsonNode definitions;
        if (definitionSchema != null)
        {
            final JsonNode definitionsRootNode = mapper.readTree(definitionSchema);
            definitions = definitionsRootNode.path("definitions");
        }
        else
        {
            definitions = rootNode.path("definitions");
        }
        
        doIterateDefinitions(neededElements, schemaRoot, definitions, nsAlias);

        return schemaRoot.getOwnerDocument();
    }

    private static void handleObjectSchema(Config cfg, final JsonNode rootNode, final String nsAlias, final Element schemaRoot, final Set<String> neededElements)
    {
        JsonNode properties;
        properties = rootNode.get(FIELD_PROPERTIES);
        Assert.notNull(properties, "\"properties\" property should be found in root of JSON schema\"");
        
        if (cfg.isCreateRootElement())
        {
            final Element wrapper = element(schemaRoot, XSD_ELEMENT);
            wrapper.setAttribute(FIELD_NAME, cfg.getName());
            wrapper.setAttribute("type", nsAlias + ":" + cfg.getName());
        }

        final Element schemaComplexType = element(schemaRoot, XSD_COMPLEXTYPE);
        schemaComplexType.setAttribute(FIELD_NAME, cfg.getName());

        final Element schemaSequence = element(schemaComplexType, XSD_SEQUENCE);
        
        doIterate(neededElements, schemaSequence, properties, getRequiredList(rootNode), nsAlias);
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

    private static void doIterateDefinitions(Set<String> neededElements, Element elem, JsonNode node, String ns)
    {
        final Iterator<Entry<String, JsonNode>> iter = node.fields();
        while (iter.hasNext())
        {
            final Entry<String, JsonNode> entry = iter.next();
            final String key = entry.getKey();
            final JsonNode val = entry.getValue();
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
                href.setAttribute("type", TYPE_STRING);

                rel.setAttribute(FIELD_NAME, "rel");
                rel.setAttribute("type", TYPE_STRING);

                title.setAttribute(FIELD_NAME, "title");
                title.setAttribute("type", TYPE_STRING);

                method.setAttribute(FIELD_NAME, "method");
                method.setAttribute("type", TYPE_STRING);

                type.setAttribute(FIELD_NAME, "type");
                type.setAttribute("type", TYPE_STRING);
            }
            else
            {
                final Element schemaComplexType = element(elem, XSD_COMPLEXTYPE);
                schemaComplexType.setAttribute(FIELD_NAME, key);

                final Element schemaSequence = element(schemaComplexType, XSD_SEQUENCE);
                final JsonNode properties = val.get(FIELD_PROPERTIES);
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
        final Element nodeElem = element(elem, XSD_ELEMENT);
        String name;
        if (!key.equals("link"))
        {
            name = key.substring(0, 1).toUpperCase() + key.substring(1);
        }
        else
        {
            name = key;
        }

        nodeElem.setAttribute(FIELD_NAME, name);

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
                
            default:
        }
    }

    private static void handleReference(Set<String> neededElements, Element nodeElem, JsonNode val, String ns)
    {
        final JsonNode refs = val.get("$ref");
        nodeElem.removeAttribute("type");
        String fixRef = refs.asText().replace("#/definitions/", ns + ":");
        String name = fixRef.substring(ns.length() + 1);
        String oldName = nodeElem.getAttribute(FIELD_NAME);

        if (oldName.length() <= 0)
        {
            nodeElem.setAttribute(FIELD_NAME, name);
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
            final Element simpleType = element(nodeElem, XSD_SIMPLETYPE);
            final Element restriction = element(simpleType, XSD_RESTRICTION);
            restriction.setAttribute("base", TYPE_STRING);

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

    private static void handleObject(Set<String> neededElements, Element nodeElem, JsonNode val, String ns)
    {
        final JsonNode properties = val.get(FIELD_PROPERTIES);
        if (properties != null)
        {
            final Element complexType = element(nodeElem, XSD_COMPLEXTYPE);
            final Element sequence = element(complexType, XSD_SEQUENCE);
            Assert.notNull(properties, "'object' type must have a 'properties' attribute");
            doIterate(neededElements, sequence, properties, getRequiredList(val), ns);
        }
    }

    private static void handleEnum(Element nodeElem, JsonNode val)
    {
        nodeElem.removeAttribute("type");
        final Element simpleType = element(nodeElem, XSD_SIMPLETYPE);
        final Element restriction = element(simpleType, XSD_RESTRICTION);
        restriction.setAttribute("base", TYPE_STRING);
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
        final Integer minimum = getIntVal(jsonNode, "minimum");
        final Integer maximum = getIntVal(jsonNode, "maximum");

        if (minimum != null || maximum != null)
        {
            nodeElem.removeAttribute("type");
            final Element simpleType = element(nodeElem, XSD_SIMPLETYPE);
            final Element restriction = element(simpleType, XSD_RESTRICTION);
            restriction.setAttribute("base", xsdType);

            if (minimum != null)
            {
                final Element min = element(restriction, "minInclusive");
                min.setAttribute(XSD_VALUE, Integer.toString(minimum));
            }

            if (maximum != null)
            {
                final Element max = element(restriction, "maxInclusive");
                max.setAttribute(XSD_VALUE, Integer.toString(maximum));
            }
        }
    }

    private static void handleArray(Set<String> neededElements, Element nodeElem, JsonNode jsonNode, String ns)
    {
        final JsonNode arrItems = jsonNode.path("items");
        final String arrayXsdType = determineXsdType(arrItems.path("type").textValue(), arrItems);
        final Element complexType = element(nodeElem, XSD_COMPLEXTYPE);
        final Element sequence = element(complexType, XSD_SEQUENCE);
        final Element arrElem = element(sequence, XSD_ELEMENT);
        handleArrayElements(neededElements, jsonNode, ns, arrItems, arrayXsdType, arrElem);
    }

    private static void handleArrayElements(Set<String> neededElements, JsonNode jsonNode, String ns, final JsonNode arrItems, final String arrayXsdType, final Element arrElem)
    {
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
            arrElem.setAttribute(FIELD_NAME, "item");
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
        final boolean isEnum = node.get(TYPE_ENUM) != null;
        final boolean isRef = node.get("$ref") != null;
        if (isRef)
        {
            return TYPE_REFERENCE;
        }
        else if (isEnum)
        {
            return TYPE_ENUM;
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
