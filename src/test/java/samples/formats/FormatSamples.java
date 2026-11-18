package samples.formats;

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
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import com.ethlo.jsons2xsd.Config;
import com.ethlo.jsons2xsd.JsonSimpleType;
import com.ethlo.jsons2xsd.Jsons2Xsd;
import com.ethlo.jsons2xsd.XmlUtil;
import com.ethlo.jsons2xsd.XsdSimpleType;

public class FormatSamples {
    /**
     * Map a `format` of a JSON type to an XSD type. In this schema `foo` has the format `bar`, which has no mapping.
     * An unknown format is an error, but with `ignoreUnknownFormats` the property gets the type it would have without
     * a format, here `string`.
     */
    @Test
    public void customFormats() throws IOException {
        try (final Reader r = reader("/schema/customformats.json")) {
            final Config cfg = new Config.Builder()
                    .targetNamespace("http://ethlo.com/schema/custom-test-1.0.xsd")
                    .name("custom")
                    .ignoreUnknownFormats(true)
                    .customTypeMapping(JsonSimpleType.INTEGER, "int64", XsdSimpleType.LONG)
                    .customTypeMapping(JsonSimpleType.INTEGER, "int32", XsdSimpleType.INT)
                    .customTypeMapping(JsonSimpleType.STRING, "ext-ref", XsdSimpleType.STRING)
                    .build();

            final String xsd = XmlUtil.asXmlString(Jsons2Xsd.convert(r, cfg).getDocumentElement());
            assertThat(xsd).contains("<element minOccurs=\"0\" name=\"id\" type=\"long\"/>");
            assertThat(xsd).contains("<element minOccurs=\"0\" name=\"age\" type=\"int\"/>");
            assertThat(xsd).contains("<element minOccurs=\"0\" name=\"foo\" type=\"string\"/>");
        }
    }

    /**
     * Accept `type` values that are not JSON types, like `"type": "date-time"`, and map them to XSD types.
     */
    @Test
    public void nonJsonTypes() throws IOException {
        try (final Reader r = reader("/schema/abcd.json")) {
            final Config cfg = new Config.Builder()
                    .targetNamespace("http://ethlo.com/schema/abcd")
                    .name("special")
                    .nonJsonTypeMapping("date-time", XsdSimpleType.DATE_TIME)
                    .nonJsonTypeMapping("int", XsdSimpleType.INT)
                    .build();

            final String xsd = XmlUtil.asXmlString(Jsons2Xsd.convert(r, cfg).getDocumentElement());
            assertThat(xsd).contains("<element minOccurs=\"0\" name=\"EXP\" type=\"dateTime\"/>");
            assertThat(xsd).contains("<element minOccurs=\"0\" name=\"Martin\" type=\"int\"/>");
        }
    }

    private Reader reader(String path) {
        return new InputStreamReader(getClass().getResourceAsStream(path), StandardCharsets.UTF_8);
    }
}
