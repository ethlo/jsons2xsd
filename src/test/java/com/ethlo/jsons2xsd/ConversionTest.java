package com.ethlo.jsons2xsd;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.xml.transform.TransformerException;

import org.junit.Test;
import org.w3c.dom.Document;

/**
 * 
 * @author mha
 *
 */
public class ConversionTest
{
	@Test
	public void testConversionMedium() throws IOException, TransformerException
	{
		try (final Reader r = new InputStreamReader(getClass().getResourceAsStream("/schema/medium.jsons")))
		{
			final Document doc = Jsons2Xsd.convert(r, "http://ethlo.com/schema/contacts-1.0.xsd");
			System.out.println(Jsons2Xsd.asXmlString(doc.getDocumentElement()));
		}
	}
}