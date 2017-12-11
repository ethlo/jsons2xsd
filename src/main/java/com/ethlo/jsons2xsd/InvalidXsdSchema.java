package com.ethlo.jsons2xsd;

import org.xml.sax.SAXParseException;

public class InvalidXsdSchema extends RuntimeException
{
    private static final long serialVersionUID = -6133929781335945777L;

    public InvalidXsdSchema(String msg, SAXParseException cause)
    {
        super(msg, cause);
    }
}
