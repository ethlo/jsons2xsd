package com.ethlo.jsons2xsd;

public enum JsonSimpleType
{
    STRING, NUMBER, BOOLEAN, INTEGER;

    public static final String STRING_VALUE = "string";
    public static final String NUMBER_VALUE = "number";
    public static final String BOOLEAN_VALUE = "boolean";
    public static final String INTEGER_VALUE = "integer";

    public String value()
    {
        return this.toString().toLowerCase();
    }
}
