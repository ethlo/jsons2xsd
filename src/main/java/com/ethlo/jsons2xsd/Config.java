package com.ethlo.jsons2xsd;

import com.ethlo.jsons2xsd.Jsons2Xsd.SchemaWrapping;

public class Config
{
    private boolean lowercaseElements;
    private String targetNamespace;
    private String nsAlias;
    private SchemaWrapping wrapping;
    private String name;
    private boolean attributesQualified;
    
    public boolean isAttributesQualified()
    {
        return attributesQualified;
    }

    public boolean isLowercaseElements()
    {
        return lowercaseElements;
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

    public SchemaWrapping getWrapping()
    {
        return wrapping;
    }

    public static class Builder
    {
        private boolean lowercaseElements;
        private String targetNamespace;
        private String nsAlias;
        private SchemaWrapping wrapping;
        private String name;
        private boolean attributesQualified;

        public Builder lowercaseElements(boolean lowercaseElements)
        {
            this.lowercaseElements = lowercaseElements;
            return this;
        }

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

        public Builder wrapping(SchemaWrapping wrapping)
        {
            this.wrapping = wrapping;
            return this;
        }

        public Config build()
        {
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
    }

    private Config(Builder builder)
    {
        this.lowercaseElements = builder.lowercaseElements;
        this.targetNamespace = builder.targetNamespace;
        this.nsAlias = builder.nsAlias;
        this.wrapping = builder.wrapping;
        this.name = builder.name;
        this.attributesQualified = builder.attributesQualified;
    }
}
