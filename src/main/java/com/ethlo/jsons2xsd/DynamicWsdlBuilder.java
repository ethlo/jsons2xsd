package com.ethlo.jsons2xsd;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import javax.xml.XMLConstants;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPathExpressionException;

import org.springframework.core.io.AbstractFileResolvingResource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * 
 * @author mha
 */
public class DynamicWsdlBuilder
{
	private URI wrapperLocation;
	private String targetNamespace;
	private SchemaGenerator schemaGenerator;
	private String wsLocationUri;
	private String wsdlPortTypeName;
	private AbstractFileResolvingResource wrapperResource;
	
	public Document buildWsdl() throws IOException, SAXException, TransformerException
	{
		final Document parentSchema = prepareParentXsdSchema();

		final CustomCommonsXsdSchemaCollection coll = new CustomCommonsXsdSchemaCollection(this.wrapperLocation, this.schemaGenerator.getXsdSchema());
		coll.setXsds(new Resource[]{new ByteArrayResource(XmlUtil.asXmlString(parentSchema).getBytes(StandardCharsets.UTF_8))});
		coll.setInline(true);
		coll.afterPropertiesSet();
		
		final DefaultWsdl11Definition wsdlDefinition = new DefaultWsdl11Definition();
		wsdlDefinition.setSchemaCollection(coll);
		wsdlDefinition.setPortTypeName(wsdlPortTypeName);
		wsdlDefinition.setLocationUri(wsLocationUri);
		
		try
		{
			wsdlDefinition.afterPropertiesSet();
		}
		catch (Exception exc)
		{
			throw new IOException(exc.getMessage(), exc);
		}
		
		return (Document)((DOMSource)wsdlDefinition.getSource()).getNode();
	}

	public DynamicWsdlBuilder withWrapper(URI wrapperLocation)
	{
		this.wrapperLocation = wrapperLocation;
		try
		{
			if ("classpath".equals(wrapperLocation.getScheme()))
			{
				final String path = wrapperLocation.getSchemeSpecificPart();
				this.wrapperResource = new ClassPathResource(path);
			}
			else
			{
				this.wrapperResource = new UrlResource(wrapperLocation);
			}
		}
		catch (MalformedURLException e)
		{
			throw new RuntimeException(e.getMessage(), e);
		}
		return this;
	}
	
	public DynamicWsdlBuilder withTargetNameSpace(String targetNamespace)
	{
		this.targetNamespace = targetNamespace;
		return this;
	}

	public DynamicWsdlBuilder withDynamicSchema(SchemaGenerator schemaGenerator)
	{
		this.schemaGenerator = schemaGenerator;
		return this;
	}

	public DynamicWsdlBuilder withWsLocationUri(String wsLocationUri)
	{
		this.wsLocationUri = wsLocationUri;
		return this;
	}

	public DynamicWsdlBuilder withWsdlPortTypeName(String wsdlPortTypeName)
	{
		this.wsdlPortTypeName = wsdlPortTypeName;
		return this;
	}

	private Document prepareParentXsdSchema() throws IOException, SAXException
	{
		final Document wrapperSchema = XmlUtil.loadDocument(new InputStreamReader(this.wrapperResource.getInputStream()));		
		final Element importElem = XmlUtil.prependXsdElement(wrapperSchema.getDocumentElement(), "import");
		importElem.setAttribute("namespace", this.targetNamespace);
		importElem.setAttribute("schemaLocation", "mem://");
		return wrapperSchema;
	}

	public Validator buildValidator() throws XPathExpressionException, IOException, SAXException, TransformerException
	{
		final Document xsdSchema = prepareParentXsdSchema();
	    final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
	    schemaFactory.setResourceResolver(new CustomResourceResolver(this.schemaGenerator.getXsdSchema()));
	    final Schema schema = schemaFactory.newSchema(new DOMSource(xsdSchema));
	    
	    final Validator retVal = schema.newValidator();
	    return retVal;
	}
}
