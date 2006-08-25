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
package org.apache.mina.util;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Utilities for dealing with Charsets.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$
 */
public class CharsetUtil
{
    public static final Logger log = LoggerFactory.getLogger( CharsetUtil.class );
    
    public static final String getDefaultCharsetName()
    {
        // Use reflection here to be able to compile mina with jdk 1.4.
        try
        {
            Class charsetClass = Class.forName( "java.nio.charset.Charset" );
            Object charSet = charsetClass.getMethod( "defaultCharset", null ).invoke( null, null );
            return ( String ) charsetClass.getMethod( "name", null ).invoke( charSet, null );
        }
        catch ( Exception e )
        {
        }

        // Try to use OutputStreamWriter instead.
        OutputStreamWriter writer = new OutputStreamWriter( new ByteArrayOutputStream() );
        return writer.getEncoding();
    }

    public static Charset getDefaultCharset()
    {
        return Charset.forName( getDefaultCharsetName() );
    }
}
