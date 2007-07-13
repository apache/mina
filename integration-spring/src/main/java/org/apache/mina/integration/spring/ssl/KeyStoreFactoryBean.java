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
package org.apache.mina.integration.spring.ssl;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;

import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Spring {@link org.springframework.beans.factory.FactoryBean} implementation 
 * which makes it possible to configure {@link java.security.KeyStore} instances
 * using Spring.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class KeyStoreFactoryBean extends AbstractFactoryBean {
    private String type = "JKS";

    private String provider = null;

    private char[] password = null;

    private File file = null;

    private Resource resource = null;

    /**
     * Creates a new {@link KeyStore}. This method will be called
     * by the base class when Spring creates a bean using this FactoryBean.
     * 
     * @return the {@link KeyStore} instance.
     */
    protected Object createInstance() throws Exception {
        if (file == null && resource == null) {
            throw new IllegalArgumentException("Required property missing. "
                    + "Either 'file' or 'resource' have to be specified");
        }

        KeyStore ks = null;
        if (provider == null) {
            ks = KeyStore.getInstance(type);
        } else {
            ks = KeyStore.getInstance(type, provider);
        }

        InputStream is = null;
        if (file != null) {
            is = new BufferedInputStream(new FileInputStream(file));
        } else {
            is = resource.getInputStream();
        }

        try {
            ks.load(is, password);
        } finally {
            try {
                is.close();
            } catch (IOException ignored) {
            }
        }

        return ks;
    }

    public Class getObjectType() {
        return KeyStore.class;
    }

    /**
     * Sets the file which contains the key store. Either this
     * property or {@link #setProvider(String)} have to be set.
     * 
     * @param file the file to load the key store from.
     */
    public void setFile(File file) {
        this.file = file;
    }

    /**
     * Sets the key store password. If this value is <code>null</code> no
     * password will be used to check the integrity of the key store.
     * 
     * @param password the password or <code>null</code> if no password is 
     *        needed.
     */
    public void setPassword(String password) {
        if (password != null) {
            this.password = password.toCharArray();
        } else {
            this.password = null;
        }
    }

    /**
     * Sets the name of the provider to use when creating the key store. The
     * default is to use the platform default provider.
     * 
     * @param provider the name of the provider, e.g. SUN.
     */
    public void setProvider(String provider) {
        this.provider = provider;
    }

    /**
     * Sets a Spring {@link Resource} which contains the key store. Either this
     * property or {@link #setFile(File)} have to be set.
     * 
     * @param resource the resource to load the key store from.
     */
    public void setResource(Resource resource) {
        this.resource = resource;
    }

    /**
     * Sets the type of key store to create. The default is to create a 
     * JKS key store.
     * 
     * @param type the type to use when creating the key store.
     * @throws IllegalArgumentException if the specified value is 
     *         <code>null</code>.
     */
    public void setType(String type) {
        Assert.notNull(type, "Property 'type' may not be null");
        this.type = type;
    }
}
