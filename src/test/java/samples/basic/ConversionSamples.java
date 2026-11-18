package samples.basic;

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
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import org.junit.Test;
import org.w3c.dom.Document;

import com.ethlo.jsons2xsd.Config;
import com.ethlo.jsons2xsd.Jsons2Xsd;
import com.ethlo.jsons2xsd.XmlUtil;

public class ConversionSamples {
    /**
     * Convert a JSON schema to an XML schema. The input is [`arrayschema.json`](src/test/resources/schema/arrayschema.json)
     * and the result equals [`arrayschema.xsd`](src/test/resources/schema/arrayschema.xsd), both shown below.
     */
    @Test
    public void convert() throws IOException {
        try (final Reader r = reader("/schema/arrayschema.json")) {
            final Config cfg = new Config.Builder()
                    .targetNamespace("http://ethlo.com/schema/array-test-1.0.xsd")
                    .name("array")
                    .build();
            final Document doc = Jsons2Xsd.convert(r, cfg);

            final String xsd = XmlUtil.asXmlString(doc.getDocumentElement());
            assertThat(xsd).isXmlEqualTo(load("/schema/arrayschema.xsd"));
        }
    }

    private Reader reader(String path) {
        return new InputStreamReader(getClass().getResourceAsStream(path), StandardCharsets.UTF_8);
    }

    private String load(String path) {
        try (InputStream in = getClass().getResourceAsStream(path);
             Scanner scanner = new Scanner(in, StandardCharsets.UTF_8.name())) {
            return scanner.useDelimiter("\\A").next();
        } catch (IOException exc) {
            throw new UncheckedIOException(exc);
        }
    }
}
