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
import java.io.InputStream;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import org.junit.Test;

/**
 * Definitions in `$defs` (JSON Schema 2019-09 and later) work like `definitions`. Each conversion test with
 * `definitions` is repeated here with the schema rewritten to `$defs`, and must give the same XSD.
 */
public class DefsTest {
    @Test
    public void jsonOrgSchema() throws IOException {
        final Config cfg = new Config.Builder()
                .createRootElement(true)
                .targetNamespace("http://json-schema.org/example2.html")
                .nsAlias("example2")
                .name("Example2")
                .validateXsdSchema(true)
                .build();
        assertThat(convert(withDefs("/schema/json.org.2.json"), cfg)).isXmlEqualTo(load("/schema/json.org.2.xsd"));
    }

    @Test
    public void simpleTypeRefs() throws IOException {
        final Config cfg = new Config.Builder()
                .createRootElement(true)
                .targetNamespace("http://ethlo.com/schema/dog-test-1.0.xsd")
                .name("dog")
                .validateXsdSchema(true)
                .build();
        assertThat(convert(withDefs("/schema/simple-type-refs.json"), cfg)).isXmlEqualTo(load("/schema/simple-type-refs.xsd"));
    }

    @Test
    public void separateDefinitionsFile() throws IOException {
        final Config cfg = new Config.Builder()
                .createRootElement(true)
                .targetNamespace("http://cableapi.cablelabs.com/schemas/v1/Account")
                .nsAlias("account")
                .name("Account")
                .includeOnlyUsedTypes(true)
                .build();
        final String xsd = XmlUtil.asXmlString(Jsons2Xsd.convert(
                new StringReader(withDefs("/schema/account.json")),
                new StringReader(withDefs("/schema/definitions.json")), cfg).getDocumentElement());
        assertThat(xsd).isXmlEqualTo(load("/schema/account.xsd"));
    }

    @Test
    public void definitionsAndDefsTogether() throws IOException {
        final String schema = "{\"type\": \"object\", \"properties\": {"
                + "\"a\": {\"$ref\": \"#/definitions/A\"}, \"b\": {\"$ref\": \"#/$defs/B\"}},"
                + "\"definitions\": {\"A\": {\"type\": \"string\"}},"
                + "\"$defs\": {\"B\": {\"type\": \"integer\"}}}";
        final String xsd = convert(schema, config());
        assertThat(xsd).contains("name=\"a\" type=\"x:A\"");
        assertThat(xsd).contains("name=\"b\" type=\"x:B\"");
        assertThat(xsd).contains("<simpleType name=\"A\">");
        assertThat(xsd).contains("<simpleType name=\"B\">");
    }

    @Test(expected = IllegalArgumentException.class)
    public void sameNameInBothIsAnError() throws IOException {
        final String schema = "{\"type\": \"object\", \"properties\": {\"a\": {\"$ref\": \"#/$defs/A\"}},"
                + "\"definitions\": {\"A\": {\"type\": \"string\"}},"
                + "\"$defs\": {\"A\": {\"type\": \"integer\"}}}";
        convert(schema, config());
    }

    @Test(expected = IllegalArgumentException.class)
    public void unsupportedRefIsAnError() throws IOException {
        final String schema = "{\"type\": \"object\", \"properties\": {\"a\": {\"$ref\": \"#/components/schemas/A\"}}}";
        convert(schema, config());
    }

    private static Config config() {
        return new Config.Builder().targetNamespace("urn:test").nsAlias("x").name("test").build();
    }

    private static String convert(String schema, Config cfg) throws IOException {
        return XmlUtil.asXmlString(Jsons2Xsd.convert(new StringReader(schema), cfg).getDocumentElement());
    }

    private static String withDefs(String path) {
        final String schema = load(path);
        assertThat(schema).contains("\"definitions\"");
        return schema.replace("\"definitions\"", "\"$defs\"").replace("#/definitions/", "#/$defs/");
    }

    private static String load(String path) {
        try (InputStream in = DefsTest.class.getResourceAsStream(path);
             Scanner scanner = new Scanner(in, StandardCharsets.UTF_8.name())) {
            return scanner.useDelimiter("\\A").next();
        } catch (IOException exc) {
            throw new UncheckedIOException(exc);
        }
    }
}
