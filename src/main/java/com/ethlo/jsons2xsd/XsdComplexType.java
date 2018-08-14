package com.ethlo.jsons2xsd;

public enum XsdComplexType
{
    OBJECT("object"),
    ARRAY("array");

    public static final String OBJECT_VALUE = "object";
    public static final String ARRAY_VALUE = "array";

    private final String type;

    XsdComplexType(String type)
    {
        this.type = type;
    }

    private String value()
    {
        return type;
    }
}
