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
package org.apache.mina.filter.ssl;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;

/**
 * A factory that creates and configures a new {@link KeyStore} instance.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class KeyStoreFactory {
    
    private String type = "JKS";
    private String provider = null;
    private char[] password = null;
    private byte[] data = null;

    /**
     * Creates a new {@link KeyStore}. This method will be called
     * by the base class when Spring creates a bean using this FactoryBean.
     *
     * @return a new {@link KeyStore} instance.
     */
    public KeyStore newInstance() throws KeyStoreException, NoSuchProviderException, NoSuchAlgorithmException, CertificateException, IOException {
        if (data == null) {
            throw new IllegalStateException("data property is not set.");
        }

        KeyStore ks;
        if (provider == null) {
            ks = KeyStore.getInstance(type);
        } else {
            ks = KeyStore.getInstance(type, provider);
        }

        InputStream is = new ByteArrayInputStream(data);
        try {
            ks.load(is, password);
        } finally {
            try {
                is.close();
            } catch (IOException ignored) {
                // Do nothing
            }
        }

        return ks;
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
        if (type == null) {
            throw new IllegalArgumentException("type");
        }
        this.type = type;
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
     * @param provider the name of the provider, e.g. <tt>"SUN"</tt>.
     */
    public void setProvider(String provider) {
        this.provider = provider;
    }

    /**
     * Sets the data which contains the key store.
     *
     * @param data the byte array that contains the key store
     */
    public void setData(byte[] data) {
        byte[] copy = new byte[data.length];
        System.arraycopy(data, 0, copy, 0, data.length);
        this.data = copy;
    }
    
    /**
     * Sets the data which contains the key store.
     *
     * @param dataStream the {@link InputStream} that contains the key store
     */
    private void setData(InputStream dataStream) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            for (;;) {
                int data = dataStream.read();
                if (data < 0) {
                    break;
                }
                out.write(data);
            }
            setData(out.toByteArray());
        } finally {
            try {
                dataStream.close();
            } catch (IOException e) {
                // Ignore.
            }
        }
    }
    
    /**
     * Sets the data which contains the key store.
     *
     * @param dataFile the {@link File} that contains the key store
     */
    public void setDataFile(File dataFile) throws IOException {
        setData(new BufferedInputStream(new FileInputStream(dataFile)));
    }
    
    /**
     * Sets the data which contains the key store.
     *
     * @param dataUrl the {@link URL} that contains the key store.
     */
    public void setDataUrl(URL dataUrl) throws IOException {
        setData(dataUrl.openStream());
    }
}
