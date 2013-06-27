package com.ethlo.jsons2xsd;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;

import org.apache.ws.commons.schema.resolver.DefaultURIResolver;
import org.apache.ws.commons.schema.resolver.URIResolver;
import org.springframework.core.io.ClassPathResource;
import org.springframework.xml.xsd.commons.CommonsXsdSchemaCollection;
import org.xml.sax.InputSource;

/**
 * 
 * @author mha
 *
 */
public class CustomCommonsXsdSchemaCollection extends CommonsXsdSchemaCollection
{
	final URIResolver defaultResolver = new DefaultURIResolver();
	
	public CustomCommonsXsdSchemaCollection(final URI wrapperLocation, final Reader memSchema)
	{
		setUriResolver(new URIResolver()
		{
			final String path = wrapperLocation.getSchemeSpecificPart();
			final String base = new File(path).getParent();
			
			@Override
			public InputSource resolveEntity(String targetNamespace, String schemaLocation, String baseUri)
			{		
				if ("mem://".equals(schemaLocation))
				{
					return new InputSource(memSchema);
				}
				else if ("classpath".equals(wrapperLocation.getScheme()))
				{
					try
					{
						return new InputSource(new ClassPathResource(base + "/" + schemaLocation).getInputStream());
					}
					catch (IOException e)
					{
						throw new RuntimeException(e.getMessage(), e);
					}
				}
				else
				{
					return defaultResolver.resolveEntity(targetNamespace, schemaLocation, baseUri);
				}
			}
		});
	}
}
