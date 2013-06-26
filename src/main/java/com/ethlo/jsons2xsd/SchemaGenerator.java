package com.ethlo.jsons2xsd;

import java.io.IOException;
import java.io.Reader;

/**
 * 
 * @author mha
 *
 */
public interface SchemaGenerator
{
	Reader getXsdSchema() throws IOException;
}
