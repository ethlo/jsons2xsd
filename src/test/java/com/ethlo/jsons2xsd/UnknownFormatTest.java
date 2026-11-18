package com.ethlo.jsons2xsd;

/*-
 * #%L
 * jsons2xsd
 * %%
 * Copyright (C) 2014 - 2026 Morten Haraldsen (ethlo)
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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;

import org.junit.Test;

/**
 * An unknown `format` is an error, unless {@link Config.Builder#ignoreUnknownFormats(boolean)} is set. Then the
 * property gets the XSD type it would have without a format, whatever other mappings are configured.
 */
public class UnknownFormatTest {
    private static final String STRING_BAR = schema("\"type\": \"string\", \"format\": \"bar\"");
    private static final String INTEGER_WEIRD = schema("\"type\": \"integer\", \"format\": \"weird\"");

    @Test(expected = IllegalArgumentException.class)
    public void unknownFormatIsAnError() {
        convert(STRING_BAR, builder());
    }

    @Test
    public void ignoredWithoutAnyMappings() {
        assertThat(convert(STRING_BAR, builder().ignoreUnknownFormats(true))).contains("name=\"foo\" type=\"string\"");
    }

    @Test
    public void ignoredFallsBackToTheTypeNotToAnotherMapping() {
        final Config.Builder builder = builder()
                .ignoreUnknownFormats(true)
                .customTypeMapping(JsonSimpleType.INTEGER, "int64", XsdSimpleType.LONG);
        assertThat(convert(INTEGER_WEIRD, builder)).contains("name=\"foo\" type=\"int\"");
    }

    @Test
    public void knownCustomFormatStillMapped() {
        final Config.Builder builder = builder()
                .ignoreUnknownFormats(true)
                .customTypeMapping(JsonSimpleType.INTEGER, "weird", XsdSimpleType.LONG);
        assertThat(convert(INTEGER_WEIRD, builder)).contains("name=\"foo\" type=\"long\"");
    }

    @Test
    public void ignoredOnNonJsonType() {
        final Config.Builder builder = builder()
                .ignoreUnknownFormats(true)
                .nonJsonTypeMapping("int", XsdSimpleType.INT);
        assertThat(convert(schema("\"type\": \"int\", \"format\": \"weird\""), builder)).contains("name=\"foo\" type=\"int\"");
    }

    @Test(expected = IllegalArgumentException.class)
    public void unknownTypeIsStillAnError() {
        convert(schema("\"type\": \"wibble\", \"format\": \"weird\""), builder().ignoreUnknownFormats(true));
    }

    private static String schema(String fooProperty) {
        return "{\"type\": \"object\", \"properties\": {\"foo\": {" + fooProperty + "}}}";
    }

    private static Config.Builder builder() {
        return new Config.Builder().targetNamespace("urn:test").name("test");
    }

    private static String convert(String schema, Config.Builder builder) {
        try {
            return XmlUtil.asXmlString(Jsons2Xsd.convert(new StringReader(schema), builder.build()).getDocumentElement());
        } catch (IOException exc) {
            throw new UncheckedIOException(exc);
        }
    }
}
