package com.ethlo.schematools.jsons2xsd;

import com.ethlo.schematools.jsons2xsd.Jsons2XsdSingleFile.OuterWrapping;
import org.junit.Test;
import org.w3c.dom.Document;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * 
 * @author mha
 *
 */
public class ConversionTestSingleFile
{

	@Test
	public void testConversionCMTS() throws IOException, TransformerException
	{

			final Reader r = new InputStreamReader(getClass().getResourceAsStream("/schema/account.json"));
			final Reader def = new InputStreamReader(getClass().getResourceAsStream("/schema/definitions.json"));


			final Document doc = Jsons2XsdSingleFile.convert(r, def, "http://cableapi.cablelabs.com/schemas/v1/CMTS", OuterWrapping.ELEMENT, "CMTS");
			System.out.println(XmlUtil.asXmlString(doc.getDocumentElement()));

	}


}