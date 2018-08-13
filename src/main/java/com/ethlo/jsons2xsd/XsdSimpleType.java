package com.ethlo.jsons2xsd;

public enum XsdSimpleType
{
    STRING("string"),
    INT("int"),
    LONG("long"),
    BOOLEAN("boolean"),
    DATE("date"),
    TIME("time"),
    DATE_TIME("dateTime"),
    DECIMAL("decimal");

    public static final String STRING_VALUE = "string";
    public static final String INT_VALUE = "int";
    public static final String LONG_VALUE = "long";
    public static final String BOOLEAN_VALUE = "boolean";
    public static final String DATE_VALUE = "date";
    public static final String DATETIME_VALUE = "dateTime";
    public static final String DECIMAL_VALUE = "decimal";
    public static final String TIME_VALUE = "time";

    XsdSimpleType(String type)
    {
        this.type = type;
    }

    public String value()
    {
        return type;
    }

    private final String type;
}
