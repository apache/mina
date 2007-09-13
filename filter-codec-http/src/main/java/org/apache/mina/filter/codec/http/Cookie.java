/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.apache.mina.filter.codec.http;


import java.util.Date;


/**
 * TODO Cookie.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class Cookie
{

    private String comment;
    private String domain;
    private String name;
    private String value;
    private String path;
    private boolean secure;
    private int version = 0;
    private Date expires;


    public Cookie( String name, String value )
    {
        this.name = name;
        this.value = value;
    }


    public String getComment()
    {
        return comment;
    }


    public void setComment( String comment )
    {
        this.comment = comment;
    }


    public String getDomain()
    {
        return domain;
    }


    public void setDomain( String domain )
    {
        this.domain = domain;
    }


    public String getName()
    {
        return name;
    }


    public void setName( String name )
    {
        this.name = name;
    }


    public String getValue()
    {
        return value;
    }


    public void setValue( String value )
    {
        this.value = value;
    }


    public String getPath()
    {
        return path;
    }


    public void setPath( String path )
    {
        this.path = path;
    }


    public boolean isSecure()
    {
        return secure;
    }


    public void setSecure( boolean secure )
    {
        this.secure = secure;
    }


    public int getVersion()
    {
        return version;
    }


    public void setVersion( int version )
    {
        this.version = version;
    }


    public Date getExpires()
    {
        return expires;
    }


    public void setExpires( Date expires )
    {
        this.expires = expires;
    }
}
