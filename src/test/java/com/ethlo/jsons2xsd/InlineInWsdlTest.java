package com.ethlo.jsons2xsd;

import java.io.InputStreamReader;
import java.io.Reader;

import javax.xml.transform.dom.DOMSource;

import org.junit.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.commons.CommonsXsdSchemaCollection;
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
		byte[] schema = null;
		try (final Reader r = new InputStreamReader(getClass().getResourceAsStream("/schema/medium.jsons")))
		{
			final Document doc = Jsons2Xsd.convert(r, "http://foo.com/ns123", OuterWrapping.TYPE, "PayloadType");
			schema = XmlUtil.asXmlString(doc.getDocumentElement()).getBytes();
		}
		
		final CommonsXsdSchemaCollection coll = new CommonsXsdSchemaCollection(new Resource[]{new ByteArrayResource(schema), new ClassPathResource("/schema/dynRequest.xsd")});
		coll.setInline(true);
		coll.afterPropertiesSet();
		
		final DefaultWsdl11Definition wsdlDefinition = new DefaultWsdl11Definition();
		wsdlDefinition.setSchemaCollection(coll);
		wsdlDefinition.setPortTypeName("foo");
		wsdlDefinition.setLocationUri("https://example.com/example-ws");
		wsdlDefinition.afterPropertiesSet();
		
		System.out.println( XmlUtil.asXmlString( ((DOMSource)wsdlDefinition.getSource()).getNode() ));
	}
}
