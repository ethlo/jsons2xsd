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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class Config
{
    private final String targetNamespace;
    private final String nsAlias;
    private final boolean createRootElement;
    private final String name;
    private final boolean attributesQualified;
    private final boolean includeOnlyUsedTypes;
    private final boolean validateXsdSchema;
    private final Map<String, String> typeMapping;
    private final boolean ignoreUnknownFormats;
    private final String rootElement;
    private final Function<String,String> itemNameMapper;

    public boolean isAttributesQualified()
    {
        return attributesQualified;
    }
    
    public boolean isIncludeOnlyUsedTypes()
    {
        return includeOnlyUsedTypes;
    }

    public String getName()
    {
        return name;
    }

    public String getTargetNamespace()
    {
        return targetNamespace;
    }

    public String getNsAlias()
    {
        return nsAlias;
    }

    public boolean isCreateRootElement()
    {
        return createRootElement || rootElement != null;
    }

    public boolean isValidateXsdSchema()
    {
        return validateXsdSchema;
    }

    public Map<String, String> getTypeMapping()
    {
        return typeMapping;
    }

    public boolean isIgnoreUnknownFormats()
    {
        return ignoreUnknownFormats;
    }

    public String getRootElement() {
        return rootElement != null ? rootElement : name;
    }

    public Function<String,String> getItemNameMapper() {
        return itemNameMapper;
    }

    public String getType(String type, String format)
    {
        final String key = (type + (format != null ? ("|" + format) : "")).toLowerCase();
        return typeMapping.get(key);
    }

    public static class Builder
    {
        private String name;
        private String targetNamespace;
        private String nsAlias = "x";
        private boolean createRootElement = false;
        private boolean attributesQualified = false;
        private boolean includeOnlyUsedTypes = false;
        private boolean validateXsdSchema = true;
        private final Map<String, String> typeMapping = new HashMap<>();
        private boolean ignoreUnknownFormats;
        private String rootElement;
        private Function<String,String> itemNameMapper = Function.identity();

        public Builder targetNamespace(String targetNamespace)
        {
            this.targetNamespace = targetNamespace;
            return this;
        }

        public Builder nsAlias(String nsAlias)
        {
            this.nsAlias = nsAlias;
            return this;
        }

        public Builder createRootElement(boolean b)
        {
            this.createRootElement = b;
            return this;
        }
        
        public Builder includeOnlyUsedTypes(boolean b)
        {
            this.includeOnlyUsedTypes = b;
            return this;
        }

        public Config build()
        {
            Assert.notNull(name, "name must be set");
            Assert.notNull(targetNamespace, "targetNamespace must be set");
            
            return new Config(this);
        }

        public Builder name(String name)
        {
            this.name = name;
            return this;
        }

        public Builder attributesQualified(boolean b)
        {
            this.attributesQualified = b;
            return this;
        }
        
        public Builder validateXsdSchema(boolean b)
        {
            this.validateXsdSchema = b;
            return this;
        }

        public Builder customTypeMapping(final JsonSimpleType jsonSimpleType, final String format, final XsdSimpleType xsdSimpleType)
        {
            typeMapping.put(jsonSimpleType.value() + "|" + format, xsdSimpleType.value());
            return this;
        }

        public Builder nonJsonTypeMapping(final String nonJsonType, final XsdSimpleType xsdSimpleType)
        {
            return nonJsonTypeMapping(nonJsonType, null, xsdSimpleType);
        }

        public Builder nonJsonTypeMapping(final String nonJsonType, final String format, final XsdSimpleType xsdSimpleType)
        {
            typeMapping.put(nonJsonType + "|" + format, xsdSimpleType.value());
            return this;
        }

        public Builder ignoreUnknownFormats(final boolean b)
        {
            this.ignoreUnknownFormats = b;
            return this;
        }

        public Builder rootElement(final String name) {
            this.rootElement = name;
            return this;
        }

        public Builder mapArrayItemNames(final Function<String, String> mapper) {
            this.itemNameMapper = mapper;
            return this;
        }
    }
    
    private Config(Builder builder)
    {
        this.targetNamespace = builder.targetNamespace;
        this.nsAlias = builder.nsAlias;
        this.createRootElement = builder.createRootElement;
        this.name = builder.name;
        this.attributesQualified = builder.attributesQualified;
        this.includeOnlyUsedTypes = builder.includeOnlyUsedTypes;
        this.validateXsdSchema = builder.validateXsdSchema;
        this.typeMapping = builder.typeMapping;
        this.ignoreUnknownFormats = builder.ignoreUnknownFormats;
        this.rootElement = builder.rootElement;
        this.itemNameMapper = builder.itemNameMapper;
    }
}
