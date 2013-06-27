package com.ethlo.jsons2xsd;

import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

/**
 * 
 * @author mha
 */
public class CustomResourceResolver implements LSResourceResolver
{
	private Reader retVal;

	public CustomResourceResolver(Reader returnVal)
	{
		this.retVal = returnVal;
	}
	
    @Override
    public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI)
    {
        return new LSInput()
        {
			@Override
			public void setSystemId(String systemId){}
			
			@Override
			public void setStringData(String stringData){}
			
			@Override
			public void setPublicId(String publicId){}
			
			@Override
			public void setEncoding(String encoding){}
			
			@Override
			public void setCharacterStream(Reader characterStream){}
			
			@Override
			public void setCertifiedText(boolean certifiedText) {}
			
			@Override
			public void setByteStream(InputStream byteStream) {}
			
			@Override
			public void setBaseURI(String baseURI) {}
			
			@Override
			public String getSystemId() {return null;}
			
			@Override
			public String getStringData() {return null;}
			
			@Override
			public String getPublicId() {return null;}
			
			@Override
			public String getEncoding()
			{
				return StandardCharsets.UTF_8.displayName();
			}
			
			@Override
			public Reader getCharacterStream()
			{
				return retVal;
			}
			
			@Override
			public boolean getCertifiedText()
			{
				return true;
			}
			
			@Override
			public InputStream getByteStream()
			{
				return null;
			}
			
			@Override
			public String getBaseURI()
			{
				return null;
			}
		};
    }        
}
