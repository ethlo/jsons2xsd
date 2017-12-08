package com.ethlo.schematools.jsons2xsd;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.xml.transform.TransformerException;

import org.junit.Test;
import org.w3c.dom.Document;

import com.ethlo.jsons2xsd.Jsons2Xsd;
import com.ethlo.jsons2xsd.XmlUtil;
import com.ethlo.jsons2xsd.Jsons2Xsd.OuterWrapping;

public class ConversionTest
{
    @Test
    public void testIssue8ArraySchema() throws IOException, TransformerException
    {
        try (final Reader r = new InputStreamReader(getClass().getResourceAsStream("/schema/arrayschema.json")))
        {
            final Document doc = Jsons2Xsd.convert(r, "http://ethlo.com/schema/array-test-1.0.xsd", OuterWrapping.ELEMENT, "mySpecialElement");
            System.out.println(XmlUtil.asXmlString(doc.getDocumentElement()));
        }
    }
    
	@Test
	public void testConversionAbcd() throws IOException, TransformerException
	{
	    try (final Reader r = new InputStreamReader(getClass().getResourceAsStream("/schema/abcd.json")))
        {
            final Document doc = Jsons2Xsd.convert(r, "http://ethlo.com/schema/array-test-1.0.xsd", OuterWrapping.ELEMENT, "mySpecialElement");
            System.out.println(XmlUtil.asXmlString(doc.getDocumentElement()));
        }
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
}