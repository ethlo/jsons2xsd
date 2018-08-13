package com.ethlo.jsons2xsd;

public enum JsonSimpleType
{
    STRING, NUMBER, BOOLEAN, INTEGER;

    public static final String STRING_VALUE = STRING.value();
    public static final String NUMBER_VALUE = NUMBER.value();
    public static final String BOOLEAN_VALUE = BOOLEAN.value();
    public static final String INTEGER_VALUE = INTEGER.value();

    public String value()
    {
        return this.toString().toLowerCase();
    }
}
