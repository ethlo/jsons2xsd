package com.ethlo.jsons2xsd;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Validator;

import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.w3c.dom.Document;

import com.ethlo.jsons2xsd.Jsons2Xsd.OuterWrapping;

/**
 * 
 * @author mha
 */
public class ValidateDynamicContentTest
{
	@Test
	public void buildWsdlWithDynamicType() throws Exception
	{
		final Validator validator = new DynamicWsdlBuilder()
			.withWrapper(new ClassPathResource("/schema/dynRequest.xsd"))
			.withTargetNameSpace("http://example.com/foreign-1.0.xsd")
			.withTargetTypeName("PayloadType")
			.withDynamicSchema(new SchemaGenerator()
			{
				@Override
				public Reader getXsdSchema() throws IOException
				{
					return createXmlSchemaFromJsonSchema();
				}
			})
			.withWsLocationUri("http://localhost:8080/example-ws")
			.withWsdlPortTypeName("example")
			.buildValidator();
		
		final Document payload = XmlUtil.loadDocument(new InputStreamReader(getClass().getResourceAsStream("/testdata/validinput.xml")));
		validator.validate(new DOMSource(payload));
	}
	
	private Reader createXmlSchemaFromJsonSchema() throws IOException
	{
		try(final Reader r = new InputStreamReader(new ClassPathResource("/schema/small.jsons").getInputStream()))
		{
			return new StringReader(XmlUtil.asXmlString(Jsons2Xsd.convert(r, "http://example.com/foreign-1.0.xsd", OuterWrapping.TYPE, "PayloadType")));
		}
	}
}
