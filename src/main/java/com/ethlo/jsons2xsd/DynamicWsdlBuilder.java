package com.ethlo.jsons2xsd;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import javax.xml.XMLConstants;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.commons.CommonsXsdSchemaCollection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * 
 * @author mha
 */
public class DynamicWsdlBuilder
{
	private Resource wrapper;
	private String targetNamespace;
	private String targetTypeName;
	private SchemaGenerator schemaGenerator;
	private String wsLocationUri;
	private String wsdlPortTypeName;
	
	private Document makeWsdl(Document wrapperDoc, String locationUri, String portTypeName) throws TransformerException, IOException
	{
		final CommonsXsdSchemaCollection coll = new CommonsXsdSchemaCollection();
		coll.setXsds(new Resource[]{new ByteArrayResource(XmlUtil.asXmlString(wrapperDoc).getBytes(StandardCharsets.UTF_8))});
		coll.setInline(true);
		coll.afterPropertiesSet();
		
		final DefaultWsdl11Definition wsdlDefinition = new DefaultWsdl11Definition();
		wsdlDefinition.setSchemaCollection(coll);
		wsdlDefinition.setPortTypeName(portTypeName);
		wsdlDefinition.setLocationUri(locationUri);
		
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

	private Document loadWrapperSchema(Resource wrapperSchema, String typeNamespace, String targetTypeName, String schemaLocation) throws IOException, SAXException, XPathExpressionException, TransformerException
	{
		final Document doc = XmlUtil.loadDocument(new InputStreamReader(wrapperSchema.getInputStream()));
		
		final Element importElem = findImportElementPlaceHolder(doc);
		final Element parentElem = (Element) importElem.getParentNode();
		parentElem.setAttribute("xmlns:tns", typeNamespace);
		
//		final Element typeRef = (Element) XmlUtil.getXPath(namespaces).compile("//*[@type='" + targetTypeName + "']").evaluate(doc, XPathConstants.NODE);
//		typeRef.setAttribute("type", "tns:" + targetTypeName);
		
		importElem.setAttribute("namespace", typeNamespace);
		importElem.setAttribute("schemaLocation", schemaLocation);
		
		return doc;
	}

	private Element findImportElementPlaceHolder(Node node) throws XPathExpressionException
	{
		final XPath xpath = XmlUtil.getXPath(Collections.singletonMap("xs", XMLConstants.W3C_XML_SCHEMA_NS_URI));
		final Element importElem = (Element) xpath.compile("//xs:import[@id='dynamicTypeImport']").evaluate(node, XPathConstants.NODE);
		Assert.notNull(importElem, "import element with id=\"dynamicTypeImport\" was not found in wrapper type");
		return importElem;
	}

	public DynamicWsdlBuilder withWrapper(Resource wrapper)
	{
		this.wrapper = wrapper;
		return this;
	}
	
	public DynamicWsdlBuilder withTargetNameSpace(String targetNamespace)
	{
		this.targetNamespace = targetNamespace;
		return this;
	}

	public DynamicWsdlBuilder withTargetTypeName(String typeName)
	{
		this.targetTypeName = typeName;
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

	public Document build() throws IOException, XPathExpressionException, SAXException, TransformerException
	{
		final File tmpFile = copyToFile();
		final Document wrapperDoc = loadWrapperSchema(this.wrapper, this.targetNamespace, this.targetTypeName, tmpFile.getAbsolutePath());
		return makeWsdl(wrapperDoc, wsLocationUri, wsdlPortTypeName);
	}

	private File copyToFile() throws IOException
	{
		final File tmpFile = File.createTempFile("dynxsd.", ".xsd");
		try (final Writer writer = new BufferedWriter(new FileWriter(tmpFile)))
		{
			final Reader reader = this.schemaGenerator.getXsdSchema();
			final char[] buffer = new char[8192];
		    int len;
		    while ((len = reader.read(buffer) ) != -1)
		    {
		    	writer.write(buffer, 0, len);
		    }
		}
		return tmpFile;
	}

	public Validator buildValidator() throws XPathExpressionException, IOException, SAXException, TransformerException
	{
	    final Document parentXsd = XmlUtil.loadDocument(new InputStreamReader(wrapper.getInputStream()));
	    final Element importElem = findImportElementPlaceHolder(parentXsd);
	    
	    final File tmpFile = copyToFile();
	    
	    importElem.setAttribute("schemaLocation", tmpFile.getAbsolutePath());
	    importElem.setAttribute("namespace", targetNamespace);
	    
	    System.out.println("Parent: \n" + XmlUtil.asXmlString(parentXsd));
	    
	    final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
	    final Schema schema = schemaFactory.newSchema(new DOMSource(parentXsd));
	    
	    final Validator retVal = schema.newValidator();
	    return retVal;
	}
}
