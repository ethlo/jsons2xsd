package com.ethlo.jsons2xsd;

public enum JsonSimpleType
{
    STRING, NUMBER, BOOLEAN, INTEGER;

    public static final String STRING_VALUE = STRING.value();
    public static final String NUMBER_VALUE = NUMBER.value();
    public static final String BOOLEAN_VALUE = BOOLEAN.value();
    public static final String INTEGER_VALUE = INTEGER.value();

    public static JsonSimpleType find(final String jsonType)
    {
        try
        {
            return valueOf(jsonType.toUpperCase());
        }
        catch (IllegalArgumentException exc)
        {
            return null;
        }
    }

    public String value()
    {
        return this.toString().toLowerCase();
    }
}
