package com.ethlo.jsons2xsd;

public enum JsonComplexType
{
    OBJECT("object"),
    ARRAY("array");

    public static final String OBJECT_VALUE = "object";
    public static final String ARRAY_VALUE = "array";

    private final String type;

    JsonComplexType(String type)
    {
        this.type = type;
    }

    private String value()
    {
        return type;
    }
}
