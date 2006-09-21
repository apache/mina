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
package org.apache.mina.example.chat;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * (<b>Entry point</b>) Chat server which uses Spring and the serverContext.xml
 * file to set up MINA and the server handler.
 * 
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class SpringMain
{

    public static void main( String[] args ) throws Exception
    {
        if( System.getProperty( "com.sun.management.jmxremote" ) != null )
        {
            new ClassPathXmlApplicationContext( getJmxApplicationContexts() );
            System.out.println( "JMX enabled." );
        }
        else {
            new ClassPathXmlApplicationContext( getApplicationContext() );
            System.out.println( "JMX disabled. Please set the 'com.sun.management.jmxremote' system property to enable JMX." );
        }
        System.out.println( "Listening ..." );
    }

    public static String getApplicationContext()
    {
        return "org/apache/mina/example/chat/serverContext.xml";
    }

    public static String[] getJmxApplicationContexts()
    {
        return new String[] {
                "org/apache/mina/example/chat/serverContext.xml",
                "org/apache/mina/example/chat/jmxContext.xml"
        };
    }
}
