package com.ethlo.jsons2xsd;

public enum JsonType
{
    OBJECT("object"),
    ARRAY("array");

    public static final String OBJECT_VALUE = "object";
    public static final String ARRAY_VALUE = "array";

    JsonType(String type)
    {
        this.type = type;
    }

    private final String type;

    private String value()
    {
        return type;
    }
}
