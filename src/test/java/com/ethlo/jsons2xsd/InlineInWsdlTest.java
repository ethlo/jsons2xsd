package com.ethlo.jsons2xsd;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;

import org.junit.Test;
import org.w3c.dom.Document;

import com.ethlo.jsons2xsd.Jsons2Xsd.OuterWrapping;

/**
 * 
 * @author mha
 *
 */
public class InlineInWsdlTest
{
	@Test
	public void inlinInWsdlTest() throws Exception
	{
		final String targetNs = "http://example.com/foreign-1.0.xsd";
		final Document wsdl = new DynamicWsdlBuilder()
		.withWrapper(URI.create("classpath:/schema/dynRequest.xsd"))
		.withTargetNameSpace(targetNs)
		.withDynamicSchema(new SchemaGenerator()
		{
			@Override
			public Reader getXsdSchema() throws IOException
			{
				try (final Reader r = new InputStreamReader(getClass().getResourceAsStream("/schema/medium.jsons")))
				{
					final Document doc = Jsons2Xsd.convert(r, targetNs, OuterWrapping.TYPE, "PayloadType");
					return new StringReader(XmlUtil.asXmlString(doc.getDocumentElement()));
				}
			}
		})
		.withWsLocationUri("http://localhost:8080/example-ws")
		.withWsdlPortTypeName("example")
		.buildWsdl();
		
		System.out.println(XmlUtil.asXmlString(wsdl));
	}
}
