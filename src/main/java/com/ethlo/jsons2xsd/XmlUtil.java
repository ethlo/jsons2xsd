package com.ethlo.jsons2xsd;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.springframework.util.Assert;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * 
 * @author mha
 *
 */
public class XmlUtil
{
	public static String asXmlString(Node node) throws IOException
	{
		final Source source = new DOMSource(node);
        final StringWriter stringWriter = new StringWriter();
        final Result result = new StreamResult(stringWriter);
        final TransformerFactory factory = TransformerFactory.newInstance();
        
        try
        {
	        final Transformer transformer = factory.newTransformer();
	        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
	        transformer.transform(source, result);
	        return stringWriter.getBuffer().toString();
        }
        catch (TransformerException exc)
        {
        	throw new IOException(exc.getMessage(), exc);
        }
	}
	
	public static Document newDocument()
	{
		return getBuilder().newDocument();
	}
	
	public static DocumentBuilder getBuilder()
	{
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		try
		{
			return factory.newDocumentBuilder();
		}
		catch (ParserConfigurationException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public static Document loadDocument(Reader reader) throws SAXException, IOException
	{
		return getBuilder().parse(new InputSource(reader));
	}

	public static Node findNode(Node target, String namespaceUri, String localName) throws XPathExpressionException
	{
		final XPath xpath = getXPath(Collections.singletonMap("ns_", namespaceUri));
		final String expr = "//ns_:" + localName;
		return (Node) xpath.evaluate(expr, target, XPathConstants.NODE);
	}
	
	public static NodeList findNodeChildren(Node target, String namespaceUri, String localName) throws XPathExpressionException
	{
		final XPath xpath = getXPath(Collections.singletonMap("ns_", namespaceUri));
		final String expr = "//ns_:" + localName + "/*";
		return (NodeList) xpath.evaluate(expr, target, XPathConstants.NODESET);
	}
	
	public static XPath getXPath(Map<String, String> namespaces)
	{
		final XPathFactory xPathfactory = XPathFactory.newInstance();
		final XPath xpath = xPathfactory.newXPath();
		final NamespaceContext nsContext = new NamespaceContextMap(namespaces);
		xpath.setNamespaceContext(nsContext);
		return xpath;
	}

	public static Element createXsdElement(Node element, String name)
	{
		Assert.notNull(element, "element should never be null");
		final Document doc = element.getOwnerDocument() != null ? element.getOwnerDocument() : ((Document)element);
		final Element retVal = doc.createElementNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, name);
		element.appendChild(retVal);
		return retVal;
	}
}


