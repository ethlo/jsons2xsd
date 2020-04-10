package com.ethlo.jsons2xsd;

/*-
 * #%L
 * jsons2xsd
 * %%
 * Copyright (C) 2014 - 2020 Morten Haraldsen (ethlo)
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

    private final String type;

    XsdSimpleType(String type)
    {
        this.type = type;
    }

    public String value()
    {
        return type;
    }
}
