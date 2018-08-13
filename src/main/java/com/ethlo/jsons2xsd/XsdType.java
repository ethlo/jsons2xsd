package com.ethlo.jsons2xsd;

public enum XsdType
{
    OBJECT("object"),
    ARRAY("array");

    public static final String OBJECT_VALUE = OBJECT.value();
    public static final String ARRAY_VALUE = ARRAY.value();

    XsdType(String type)
    {
        this.type = type;
    }

    private final String type;

    private String value()
    {
        return type;
    }
}
