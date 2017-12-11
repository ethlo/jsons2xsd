package com.ethlo.jsons2xsd;

/*-
 * #%L
 * jsons2xsd
 * %%
 * Copyright (C) 2014 - 2017 Morten Haraldsen (ethlo)
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.sun.xml.xsom.parser.XSOMParser;

public class XmlUtil
{
    private XmlUtil()
    {
    }

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
            throw new IllegalStateException(e);
        }
    }

    public static Element createXsdElement(Node element, String name)
    {
        Assert.notNull(element, "element should never be null");
        final Document doc = element.getOwnerDocument() != null ? element.getOwnerDocument() : ((Document) element);
        final Element retVal = doc.createElementNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, name);
        element.appendChild(retVal);
        return retVal;
    }

    public static void validateSchema(Document doc)
    {
        final SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XSOMParser parser = new XSOMParser(factory);
        final EH errorHandler = new EH();
        parser.setErrorHandler(errorHandler);
        try
        {
            parser.parse(new StringReader(XmlUtil.asXmlString(doc)));
        }
        catch (SAXException | IOException exc)
        {
            throw new IllegalArgumentException(exc);
        }
        
        final List<SAXParseException> errors = errorHandler.getErrors();
        if (! errors.isEmpty())
        {
            final SAXParseException exc = errors.get(0);
            throw new InvalidXsdSchema(exc.getMessage(), exc);
        }
    }

    private static class EH implements ErrorHandler
    {
        private final List<SAXParseException> warnings = new LinkedList<>();
        private final List<SAXParseException> errors = new LinkedList<>();
        
        public void error(SAXParseException x) throws SAXException
        {
            errors.add(x);
        }

        public void fatalError(SAXParseException x) throws SAXException
        {
            errors.add(x);
        }

        public void warning(SAXParseException x) throws SAXException
        {
            warnings.add(x);
        }
        
        public List<SAXParseException> getErrors()
        {
            return errors;
        }
        
        public List<SAXParseException> getWarnings()
        {
            return warnings;
        }
    }
}
