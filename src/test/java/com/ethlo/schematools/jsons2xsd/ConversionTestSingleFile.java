package com.ethlo.schematools.jsons2xsd;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.xml.transform.TransformerException;

import org.junit.Test;
import org.w3c.dom.Document;

import com.ethlo.jsons2xsd.Jsons2Xsd;
import com.ethlo.jsons2xsd.XmlUtil;
import com.ethlo.jsons2xsd.Jsons2Xsd.OuterWrapping;

public class ConversionTestSingleFile
{
    @Test
	public void testConversionCMTS() throws IOException, TransformerException, URISyntaxException
	{
		final Reader schema = new InputStreamReader(getClass().getResourceAsStream("/schema/account.json"));
		final Reader definitions = new InputStreamReader(getClass().getResourceAsStream("/schema/definitions.json"));

		final Document doc = Jsons2Xsd.convert(schema, definitions, "http://cableapi.cablelabs.com/schemas/v1/CMTS", OuterWrapping.ELEMENT, "CMTS");
		final String actual = XmlUtil.asXmlString(doc.getDocumentElement());
			
		final String expected = new String(Files.readAllBytes(Paths.get(ClassLoader.getSystemResource("schema/cmts.xsd").toURI())));
		assertThat(actual).isXmlEqualTo(expected);
	}
}