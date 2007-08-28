/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.mina.http.codec;


import java.net.URL;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.http.util.EncodingUtil;
import org.apache.mina.http.util.NameValuePair;


/**
 * TODO HttpRequestEncoder.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class HttpRequestEncoder extends ProtocolEncoderAdapter
{
    private static final Set<Class<HttpRequestMessage>> TYPES;
    private static final byte[] CRLF = new byte[]
        { 0x0D, 0x0A };
    private static final String POST_CONTENT_TYPE = "application/x-www-form-urlencoded";
    private URL url;

    static
    {
        Set<Class<HttpRequestMessage>> types = new HashSet<Class<HttpRequestMessage>>();
        types.add( HttpRequestMessage.class );
        TYPES = Collections.unmodifiableSet( types );
    }


    public HttpRequestEncoder( URL url )
    {
        this.url = url;
    }


    Set<Class<HttpRequestMessage>> getMessageTypes()
    {
        return TYPES;
    }


    public void encode( IoSession ioSession, Object message, ProtocolEncoderOutput out ) throws Exception
    {
        HttpRequestMessage msg = ( HttpRequestMessage ) message;

        ByteBuffer buf = ByteBuffer.allocate( 256 );

        // Enable auto-expand for easier encoding
        buf.setAutoExpand( true );

        try
        {
            //If we have content, lets create the query string
            int attrCount = msg.getParameters().size();
            String urlAttrs = "";
            if ( attrCount > 0 )
            {
                NameValuePair attrs[] = new NameValuePair[attrCount];
                Set<Map.Entry<String, String>> set = msg.getParameters().entrySet();
                int i = 0;
                for ( Map.Entry<String, String> entry : set )
                {
                    attrs[i++] = new NameValuePair( entry.getKey(), entry.getValue() );
                }
                urlAttrs = EncodingUtil.formUrlEncode( attrs, Charset.defaultCharset().toString() );
            }

            CharsetEncoder encoder = Charset.defaultCharset().newEncoder();
            buf.putString( msg.getRequestMethod(), encoder );
            buf.putString( " ", encoder );
            buf.putString( msg.getPath(), encoder );
            //If its a GET, append the attributes
            if ( msg.getRequestMethod().equals( HttpRequestMessage.REQUEST_GET ) && attrCount > 0 )
            {
                //If there is not already a ? in the query, append one, otherwise append a &
                if ( !msg.getPath().contains( "?" ) )
                {
                    buf.putString( "?", encoder );
                }
                else
                {
                    buf.putString( "&", encoder );
                }
                buf.putString( urlAttrs, encoder );
            }
            buf.putString( " HTTP/1.1", encoder );
            buf.put( CRLF );

            //This header is required for HTTP/1.1
            buf.putString( "Host: ", encoder );
            buf.putString( url.getHost(), encoder );
            buf.putString( ":", encoder );
            buf.putString( url.getPort() + "", encoder );
            buf.put( CRLF );

            //Process any headers we have
            List<NameValuePair> headers = msg.getHeaders();
            for ( NameValuePair header : headers )
            {
                String name = header.getName();
                String value = header.getValue();

                buf.putString( name, encoder );
                buf.putString( ": ", encoder );
                buf.putString( value, encoder );
                buf.put( CRLF );
            }

            //Process cookies
            //NOTE: I am just passing the name value pairs and not doing management of the expiration or path
            //As that will be left up to the user.  A possible enhancement is to make use of a CookieManager
            //to handle these issues for the request
            List<Cookie> cookies = msg.getCookies();
            if ( cookies.size() > 0 )
            {
                buf.putString( "Cookie: ", encoder );
                for ( Cookie cookie : cookies )
                {
                    String name = cookie.getName();
                    String value = cookie.getValue();

                    buf.putString( name, encoder );
                    buf.putString( "=", encoder );
                    buf.putString( value, encoder );
                    buf.putString( "; ", encoder );
                }
                buf.put( CRLF );
            }

            //If this is a POST, then we need a content length and type
            if ( msg.getRequestMethod().equals( HttpRequestMessage.REQUEST_POST ) )
            {
                byte content[] = urlAttrs.getBytes();

                //Type
                buf.putString( HttpMessage.CONTENT_TYPE, encoder );
                buf.putString( ": ", encoder );
                buf.putString( POST_CONTENT_TYPE, encoder );
                buf.put( CRLF );

                //Length
                buf.putString( HttpMessage.CONTENT_LENGTH, encoder );
                buf.putString( ": ", encoder );
                buf.putString( content.length + "", encoder );
                buf.put( CRLF );
                //Blank line
                buf.put( CRLF );
                buf.put( content );
            }
            else
            {
                //Blank line
                buf.put( CRLF );
            }

        }
        catch ( CharacterCodingException ex )
        {
            ex.printStackTrace();
        }

        buf.flip();
        out.write( buf );

    }

}
