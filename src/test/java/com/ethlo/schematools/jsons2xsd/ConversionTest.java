package com.ethlo.schematools.jsons2xsd;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.xml.transform.TransformerException;

import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;

import com.ethlo.schematools.jsons2xsd.Jsons2Xsd;
import com.ethlo.schematools.jsons2xsd.XmlUtil;
import com.ethlo.schematools.jsons2xsd.Jsons2Xsd.OuterWrapping;

/**
 * 
 * @author mha
 *
 */
public class ConversionTest
{
	@Ignore
	@Test
	public void testConversionSmall() throws IOException, TransformerException
	{
		try (final Reader r = new InputStreamReader(getClass().getResourceAsStream("/schema/small.jsons")))
		{
			final Document doc = Jsons2Xsd.convert(r, "http://ethlo.com/schema/residency-1.0.xsd", OuterWrapping.ELEMENT, "mySpecialElement");
			System.out.println(XmlUtil.asXmlString(doc.getDocumentElement()));
		}
	}
	@Ignore

	@Test
	public void testConversionMedium() throws IOException, TransformerException
	{
		try (final Reader r = new InputStreamReader(getClass().getResourceAsStream("/schema/medium.jsons")))
		{
			final Document doc = Jsons2Xsd.convert(r, "http://cableapi.cablelabs.com/schemas/v1/CMTS", OuterWrapping.TYPE, "mySpecialType");
			System.out.println(XmlUtil.asXmlString(doc.getDocumentElement()));
		}
	}
	@Ignore

	@Test
	public void testConversionAbcd() throws IOException, TransformerException
	{
		System.out.println(XmlUtil.asXmlString(doConvert("/schema/abcd.json")));
	}

	@Test
	public void testConversionCMTS() throws IOException, TransformerException
	{
		try (final Reader r = new InputStreamReader(getClass().getResourceAsStream("/schema/account.json")))
		{
			final Document doc = Jsons2Xsd.convert(r, "http://cableapi.cablelabs.com/schemas/v1/Account", OuterWrapping.ELEMENT, "Account");
			System.out.println(XmlUtil.asXmlString(doc.getDocumentElement()));
		}
	}

	private Document doConvert(String file) throws IOException
	{
		try (final Reader r = new InputStreamReader(getClass().getResourceAsStream(file)))
		{
			return Jsons2Xsd.convert(r, "http://ethlo.com/schema/contacts-1.0.xsd", OuterWrapping.TYPE, "mySpecialType");
		}
	}
}