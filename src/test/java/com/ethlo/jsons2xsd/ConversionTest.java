package com.ethlo.jsons2xsd;

/*-
 * #%L
 * jsons2xsd
 * %%
 * Copyright (C) 2014 - 2017 Morten Haraldsen (ethlo)
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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.xml.transform.TransformerException;

import org.junit.Test;
import org.w3c.dom.Document;

public class ConversionTest
{
    @Test
    public void testCustomSimpleTypemappings() throws IOException, TransformerException
    {
        try (final Reader r = reader("/schema/customformats.json"))
        {
            final Config cfg = new Config.Builder()
                    .createRootElement(false)
                    .targetNamespace("http://ethlo.com/schema/custom-test-1.0.xsd")
                    .name("custom")
                    .ignoreUnknownFormats(true)
                    .customTypeMapping(JsonSimpleType.INTEGER, "int64", XsdSimpleType.LONG)
                    .customTypeMapping(JsonSimpleType.INTEGER, "int32", XsdSimpleType.INT)
                    .customTypeMapping(JsonSimpleType.STRING, "ext-ref", XsdSimpleType.STRING)
                    .build();
            final Document doc = Jsons2Xsd.convert(r, cfg);
            assertThat(XmlUtil.asXmlString(doc.getDocumentElement())).isXmlEqualTo(load("schema/customformats.xsd"));
        }
    }

    @Test
    public void testArraySchema() throws IOException, TransformerException
    {
        try (final Reader r = reader("/schema/arrayschema.json"))
        {
            final Config cfg = new Config.Builder()
                .createRootElement(false)
                .targetNamespace("http://ethlo.com/schema/array-test-1.0.xsd")
                .name("array")
                .build();
            final Document doc = Jsons2Xsd.convert(r, cfg);
            assertThat(XmlUtil.asXmlString(doc.getDocumentElement())).isXmlEqualTo(load("schema/arrayschema.xsd"));
        }
    }
    
    @Test
    public void testArraySchema2() throws IOException, TransformerException
    {
        try (final Reader r = reader("/schema/arrayschema2.json"))
        {
            final Config cfg = new Config.Builder()
                .createRootElement(false)
                .targetNamespace("http://ethlo.com/schema/array-test-1.0.xsd")
                .name("array2")
                .build();
            final Document doc = Jsons2Xsd.convert(r, cfg);
            final String actual = XmlUtil.asXmlString(doc.getDocumentElement());
            assertThat(actual).isXmlEqualTo(load("schema/arrayschema2.xsd"));
        }
    }
    
    @Test
    public void testPetSchema() throws IOException, TransformerException
    {
        try (final Reader r = reader("/schema/petschema.json"))
        {
            final Config cfg = new Config.Builder()
                .createRootElement(false)
                .targetNamespace("http://ethlo.com/schema/pet-test-1.0.xsd")
                .name("petOfChoice")
                .build();
            final Document doc = Jsons2Xsd.convert(r, cfg);
            final String actual = XmlUtil.asXmlString(doc.getDocumentElement());
            assertThat(actual).isXmlEqualTo(load("schema/petschema.xsd"));
        }
    }
    
    @Test(expected=InvalidXsdSchema.class)
    public void testPetSchemaXsdValidationFails() throws IOException, TransformerException
    {
        try (final Reader r = reader("/schema/petschema.json"))
        {
            final Config cfg = new Config.Builder()
                .createRootElement(false)
                .targetNamespace("http://ethlo.com/schema/pet-test-1.0.xsd")
                .name("pet")
                .build();
            Jsons2Xsd.convert(r, cfg);
        }
    }
    
    @Test
    public void testJsonOrgDiskSchema() throws IOException, TransformerException
    {
        try (final Reader r = reader("/schema/json.org.2.json"))
        {
            final Config cfg = new Config.Builder()
                .createRootElement(true)
                .targetNamespace("http://json-schema.org/example2.html")
                .nsAlias("example2")
                .name("Example2")
                .validateXsdSchema(true)
                .build();
            final Document doc = Jsons2Xsd.convert(r, cfg);
            assertThat(XmlUtil.asXmlString(doc.getDocumentElement())).isXmlEqualTo(load("schema/json.org.2.xsd"));
        }
    }
    
    
	@Test
	public void testConversionAbcd() throws IOException, TransformerException
	{
	    try (final Reader r = reader("/schema/abcd.json"))
        {
	        final Config cfg = new Config.Builder()
                .createRootElement(true)
                .targetNamespace("http://ethlo.com/schema/abcd")
                .nsAlias("my")
                .nonJsonTypeMapping("date-time", XsdSimpleType.DATE_TIME)
                .nonJsonTypeMapping("int", XsdSimpleType.INT)
                .name("special")
                .build();
            final Document doc = Jsons2Xsd.convert(r, cfg);
            assertThat(XmlUtil.asXmlString(doc.getDocumentElement())).isXmlEqualTo(load("schema/abcd.xsd"));
        }
	}

	@Test
	public void testConversionCMTS() throws IOException, TransformerException
	{
		try (final Reader r = reader("/schema/account.json"))
		{
		    final Config cfg = new Config.Builder()
                .createRootElement(true)
                .targetNamespace("http://cableapi.cablelabs.com/schemas/v1/CMTS")
                .nsAlias("cmts")
                .name("CMTS")
                .attributesQualified(true)
                .build();
			final Document doc = Jsons2Xsd.convert(r, cfg);
			final String actual = XmlUtil.asXmlString(doc.getDocumentElement());
            assertThat(actual).isXmlEqualTo(load("schema/cmts.xsd"));
		}
	}
	
	@Test
    public void testConversionCMTSSeparateDefinitionsFile() throws IOException
    {
        final Reader schema = reader("/schema/account.json");
        final Reader definitions = reader("/schema/definitions.json");

        final Config cfg = new Config.Builder()
            .createRootElement(true)
            .targetNamespace("http://cableapi.cablelabs.com/schemas/v1/Account")
            .nsAlias("account")
            .name("Account")
            .includeOnlyUsedTypes(true)
            .build();
        final Document doc = Jsons2Xsd.convert(schema, definitions, cfg);
        final String actual = XmlUtil.asXmlString(doc.getDocumentElement());
            
        final String expected = load("schema/account.xsd");
        assertThat(actual).isXmlEqualTo(expected);
    }

    @Test
    public void testSimpleTypeRefs() throws IOException, TransformerException
    {
        try (final Reader r = reader("/schema/simple-type-refs.json"))
        {
            final Config cfg = new Config.Builder()
                    .createRootElement(true)
                    .targetNamespace("http://ethlo.com/schema/dog-test-1.0.xsd")
                    .name("dog")
                    .validateXsdSchema(true)
                    .build();
            final Document doc = Jsons2Xsd.convert(r, cfg);
            assertThat(XmlUtil.asXmlString(doc.getDocumentElement())).isXmlEqualTo(load("schema/simple-type-refs.xsd"));
        }
    }

    @Test
    public void testAnnotations() throws IOException, TransformerException
    {
        try (final Reader r = reader("/schema/annotations.json"))
        {
            final Config cfg = new Config.Builder()
                    .createRootElement(true)
                    .targetNamespace("http://ethlo.com/schema/dog-test-1.0.xsd")
                    .name("dog")
                    .validateXsdSchema(true)
                    .build();
            final Document doc = Jsons2Xsd.convert(r, cfg);
            assertThat(XmlUtil.asXmlString(doc.getDocumentElement())).isXmlEqualTo(load("schema/annotations.xsd"));
        }
    }

    private Reader reader(String path)
    {
        return new InputStreamReader(getClass().getResourceAsStream(path));
    }

    private String load(String path)
    {
        try
        {
            final URL url = ClassLoader.getSystemResource(path);
            Assert.notNull(url, path + " not found");
            return new String(Files.readAllBytes(Paths.get(url.toURI())));
        }
        catch (IOException | URISyntaxException exc)
        {
            throw new RuntimeException(exc.getMessage(), exc);
        }
    }
}
