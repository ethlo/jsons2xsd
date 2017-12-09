package com.ethlo.schematools.jsons2xsd;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.xml.transform.TransformerException;

import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;

import com.ethlo.jsons2xsd.Assert;
import com.ethlo.jsons2xsd.Config;
import com.ethlo.jsons2xsd.Jsons2Xsd;
import com.ethlo.jsons2xsd.Jsons2Xsd.SchemaWrapping;
import com.ethlo.jsons2xsd.XmlUtil;

public class ConversionTest
{
    @Ignore
    @Test
    public void testIssue8ArraySchema() throws IOException, TransformerException
    {
        try (final Reader r = new InputStreamReader(getClass().getResourceAsStream("/schema/arrayschema.json")))
        {
            final Config cfg = new Config.Builder()
                .wrapping(SchemaWrapping.ELEMENT)
                .targetNamespace("http://ethlo.com/schema/array-test-1.0.xsd")
                .nsAlias("mySpecialElement")
                .build();
            final Document doc = Jsons2Xsd.convert(r, cfg);
            assertThat(XmlUtil.asXmlString(doc.getDocumentElement())).isXmlEqualTo(load("schema/arrayschema.xsd"));
        }
    }
    
	@Test
	public void testConversionAbcd() throws IOException, TransformerException
	{
	    try (final Reader r = new InputStreamReader(getClass().getResourceAsStream("/schema/abcd.json")))
        {
	        final Config cfg = new Config.Builder()
                .wrapping(SchemaWrapping.ELEMENT)
                .targetNamespace("http://ethlo.com/schema/abcd")
                .nsAlias("my")
                .name("special")
                .build();
            final Document doc = Jsons2Xsd.convert(r, cfg);
            assertThat(XmlUtil.asXmlString(doc.getDocumentElement())).isXmlEqualTo(load("schema/abcd.xsd"));
        }
	}

	@Ignore
	@Test
	public void testConversionCMTS() throws IOException, TransformerException
	{
		try (final Reader r = new InputStreamReader(getClass().getResourceAsStream("/schema/account.json")))
		{
		    final Config cfg = new Config.Builder()
                .wrapping(SchemaWrapping.ELEMENT)
                .targetNamespace("http://cableapi.cablelabs.com/schemas/v1/CMTS")
                .nsAlias("cmts")
                .name("CMTS")
                .attributesQualified(true)
                .build();
			final Document doc = Jsons2Xsd.convert(r, cfg);
            assertThat(XmlUtil.asXmlString(doc.getDocumentElement())).isXmlEqualTo(load("schema/cmts.xsd"));
		}
	}
	
	@Test
    public void testConversionCMTSSeparateDefinitionsFile() throws IOException
    {
        final Reader schema = reader("/schema/account.json");
        final Reader definitions = reader("/schema/definitions.json");

        final Config cfg = new Config.Builder()
            .wrapping(SchemaWrapping.ELEMENT)
            .targetNamespace("http://cableapi.cablelabs.com/schemas/v1/CMTS")
            .nsAlias("cmts")
            .name("CMTS")
            .attributesQualified(true)
            .build();
        final Document doc = Jsons2Xsd.convert(schema, definitions, cfg);
        final String actual = XmlUtil.asXmlString(doc.getDocumentElement());
            
        final String expected = load("schema/cmts.xsd");
        assertThat(actual).isXmlEqualTo(expected);
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