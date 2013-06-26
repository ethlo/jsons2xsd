package com.ethlo.jsons2xsd;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.xml.transform.TransformerException;

import org.junit.Test;
import org.w3c.dom.Document;

import com.ethlo.jsons2xsd.Jsons2Xsd.OuterWrapping;

/**
 * 
 * @author mha
 *
 */
public class ConversionTest
{
	@Test
	public void testConversionSmall() throws IOException, TransformerException
	{
		try (final Reader r = new InputStreamReader(getClass().getResourceAsStream("/schema/small.jsons")))
		{
			final Document doc = Jsons2Xsd.convert(r, "http://ethlo.com/schema/residency-1.0.xsd", OuterWrapping.ELEMENT, "mySpecialElement");
			System.out.println(XmlUtil.asXmlString(doc.getDocumentElement()));
		}
	}
	
	@Test
	public void testConversionMedium() throws IOException, TransformerException
	{
		try (final Reader r = new InputStreamReader(getClass().getResourceAsStream("/schema/medium.jsons")))
		{
			final Document doc = Jsons2Xsd.convert(r, "http://ethlo.com/schema/contacts-1.0.xsd", OuterWrapping.TYPE, "mySpecialType");
			System.out.println(XmlUtil.asXmlString(doc.getDocumentElement()));
		}
	}
}