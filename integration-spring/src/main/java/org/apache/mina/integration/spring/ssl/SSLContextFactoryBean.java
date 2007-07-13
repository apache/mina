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

import java.security.KeyStore;
import java.security.SecureRandom;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.ManagerFactoryParameters;

import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.util.Assert;

/**
 * Spring {@link org.springframework.beans.factory.FactoryBean} implementation 
 * which makes it possible to configure {@link javax.net.ssl.SSLContext} 
 * instances using Spring.
 * <p>
 * If no properties are set the returned {@link javax.net.ssl.SSLContext} will
 * be equivalent to what the following creates:
 * <pre>
 *      SSLContext c = SSLContext.getInstance( "TLS" );
 *      c.init( null, null, null );
 * </pre>
 * </p>
 * <p>
 * Use the properties prefixed with <code>keyManagerFactory</code> to control
 * the creation of the {@link javax.net.ssl.KeyManager} to be used.
 * </p>
 * <p>
 * Use the properties prefixed with <code>trustManagerFactory</code> to control
 * the creation of the {@link javax.net.ssl.TrustManagerFactory} to be used.
 * </p>
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class SSLContextFactoryBean extends AbstractFactoryBean {
    private String protocol = "TLS";

    private String provider = null;

    private SecureRandom secureRandom = null;

    private KeyStore keyManagerFactoryKeyStore = null;

    private char[] keyManagerFactoryKeyStorePassword = null;

    private KeyManagerFactory keyManagerFactory = null;

    private String keyManagerFactoryAlgorithm = null;

    private String keyManagerFactoryProvider = null;

    private boolean keyManagerFactoryAlgorithmUseDefault = false;

    private KeyStore trustManagerFactoryKeyStore = null;

    private TrustManagerFactory trustManagerFactory = null;

    private String trustManagerFactoryAlgorithm = null;

    private String trustManagerFactoryProvider = null;

    private boolean trustManagerFactoryAlgorithmUseDefault = false;

    private ManagerFactoryParameters trustManagerFactoryParameters = null;

    protected Object createInstance() throws Exception {
        KeyManagerFactory kmf = this.keyManagerFactory;
        TrustManagerFactory tmf = this.trustManagerFactory;

        if (kmf == null) {
            String algorithm = keyManagerFactoryAlgorithm;
            if (algorithm == null && keyManagerFactoryAlgorithmUseDefault) {
                algorithm = KeyManagerFactory.getDefaultAlgorithm();
            }
            if (algorithm != null) {
                if (keyManagerFactoryProvider == null) {
                    kmf = KeyManagerFactory.getInstance(algorithm);
                } else {
                    kmf = KeyManagerFactory.getInstance(algorithm,
                            keyManagerFactoryProvider);
                }
            }
        }

        if (tmf == null) {
            String algorithm = trustManagerFactoryAlgorithm;
            if (algorithm == null && trustManagerFactoryAlgorithmUseDefault) {
                algorithm = TrustManagerFactory.getDefaultAlgorithm();
            }
            if (algorithm != null) {
                if (trustManagerFactoryProvider == null) {
                    tmf = TrustManagerFactory.getInstance(algorithm);
                } else {
                    tmf = TrustManagerFactory.getInstance(algorithm,
                            trustManagerFactoryProvider);
                }
            }
        }

        KeyManager[] keyManagers = null;
        if (kmf != null) {
            kmf.init(keyManagerFactoryKeyStore,
                    keyManagerFactoryKeyStorePassword);
            keyManagers = kmf.getKeyManagers();
        }
        TrustManager[] trustManagers = null;
        if (tmf != null) {
            if (trustManagerFactoryParameters != null) {
                tmf.init(trustManagerFactoryParameters);
            } else {
                tmf.init(trustManagerFactoryKeyStore);
            }
            trustManagers = tmf.getTrustManagers();
        }

        SSLContext context = null;
        if (provider == null) {
            context = SSLContext.getInstance(protocol);
        } else {
            context = SSLContext.getInstance(protocol, provider);
        }

        context.init(keyManagers, trustManagers, secureRandom);

        return context;
    }

    public Class getObjectType() {
        return SSLContext.class;
    }

    /**
     * Sets the protocol to use when creating the {@link SSLContext}. The
     * default is <code>TLS</code>.
     * 
     * @param protocol the name of the protocol.
     * @throws IllegalArgumentException if the specified value is 
     *         <code>null</code>.
     */
    public void setProtocol(String protocol) {
        Assert.notNull(protocol, "Property 'protocol' may not be null");
        this.protocol = protocol;
    }

    /**
     * If this is set to <code>true</code> while no {@link KeyManagerFactory}
     * has been set using {@link #setKeyManagerFactory(KeyManagerFactory)} and
     * no algorithm has been set using 
     * {@link #setKeyManagerFactoryAlgorithm(String)} the default algorithm
     * return by {@link KeyManagerFactory#getDefaultAlgorithm()} will be used.
     * 
     * @param useDefault <code>true</code> or <code>false</code>.
     */
    public void setKeyManagerFactoryAlgorithmUseDefault(boolean useDefault) {
        this.keyManagerFactoryAlgorithmUseDefault = useDefault;
    }

    /**
     * If this is set to <code>true</code> while no {@link TrustManagerFactory}
     * has been set using {@link #setTrustManagerFactory(TrustManagerFactory)} and
     * no algorithm has been set using 
     * {@link #setTrustManagerFactoryAlgorithm(String)} the default algorithm
     * return by {@link TrustManagerFactory#getDefaultAlgorithm()} will be used.
     * 
     * @param useDefault <code>true</code> or <code>false</code>.
     */
    public void setTrustManagerFactoryAlgorithmUseDefault(boolean useDefault) {
        this.trustManagerFactoryAlgorithmUseDefault = useDefault;
    }

    /**
     * Sets the {@link KeyManagerFactory} to use. If this is set the properties
     * which are used by this factory bean to create a {@link KeyManagerFactory}
     * will all be ignored.
     * 
     * @param factory the factory.
     */
    public void setKeyManagerFactory(KeyManagerFactory factory) {
        this.keyManagerFactory = factory;
    }

    /**
     * Sets the algorithm to use when creating the {@link KeyManagerFactory} 
     * using {@link KeyManagerFactory#getInstance(java.lang.String)} or
     * {@link KeyManagerFactory#getInstance(java.lang.String, java.lang.String)}.
     * <p>
     * This property will be ignored if a {@link KeyManagerFactory} has been
     * set directly using {@link #setKeyManagerFactory(KeyManagerFactory)}.
     * </p>
     * <p>
     * If this property isn't set while no {@link KeyManagerFactory} has been 
     * set using {@link #setKeyManagerFactory(KeyManagerFactory)} and 
     * {@link #setKeyManagerFactoryAlgorithmUseDefault(boolean)} has been set to 
     * <code>true</code> the value returned 
     * by {@link KeyManagerFactory#getDefaultAlgorithm()} will be used instead.
     * </p> 
     * 
     * @param algorithm the algorithm to use.
     */
    public void setKeyManagerFactoryAlgorithm(String algorithm) {
        this.keyManagerFactoryAlgorithm = algorithm;
    }

    /**
     * Sets the provider to use when creating the {@link KeyManagerFactory} 
     * using 
     * {@link KeyManagerFactory#getInstance(java.lang.String, java.lang.String)}.
     * <p>
     * This property will be ignored if a {@link KeyManagerFactory} has been
     * set directly using {@link #setKeyManagerFactory(KeyManagerFactory)}.
     * </p>
     * <p>
     * If this property isn't set and no {@link KeyManagerFactory} has been set
     * using {@link #setKeyManagerFactory(KeyManagerFactory)} 
     * {@link KeyManagerFactory#getInstance(java.lang.String)} will be used
     * to create the {@link KeyManagerFactory}.
     * </p> 
     * 
     * @param provider the name of the provider.
     */
    public void setKeyManagerFactoryProvider(String provider) {
        this.keyManagerFactoryProvider = provider;
    }

    /**
     * Sets the {@link KeyStore} which will be used in the call to 
     * {@link KeyManagerFactory#init(java.security.KeyStore, char[])} when
     * the {@link SSLContext} is created. 
     * 
     * @param keyStore the key store.
     */
    public void setKeyManagerFactoryKeyStore(KeyStore keyStore) {
        this.keyManagerFactoryKeyStore = keyStore;
    }

    /**
     * Sets the password which will be used in the call to 
     * {@link KeyManagerFactory#init(java.security.KeyStore, char[])} when
     * the {@link SSLContext} is created. 
     * 
     * @param password the password. Use <code>null</code> to disable password.
     */
    public void setKeyManagerFactoryKeyStorePassword(String password) {
        if (password != null) {
            this.keyManagerFactoryKeyStorePassword = password.toCharArray();
        } else {
            this.keyManagerFactoryKeyStorePassword = null;
        }
    }

    /**
     * Sets the {@link TrustManagerFactory} to use. If this is set the properties
     * which are used by this factory bean to create a {@link TrustManagerFactory}
     * will all be ignored.
     * 
     * @param factory the factory.
     */
    public void setTrustManagerFactory(TrustManagerFactory factory) {
        this.trustManagerFactory = factory;
    }

    /**
     * Sets the algorithm to use when creating the {@link TrustManagerFactory} 
     * using {@link TrustManagerFactory#getInstance(java.lang.String)} or
     * {@link TrustManagerFactory#getInstance(java.lang.String, java.lang.String)}.
     * <p>
     * This property will be ignored if a {@link TrustManagerFactory} has been
     * set directly using {@link #setTrustManagerFactory(TrustManagerFactory)}.
     * </p>
     * <p>
     * If this property isn't set while no {@link TrustManagerFactory} has been 
     * set using {@link #setTrustManagerFactory(TrustManagerFactory)} and 
     * {@link #setTrustManagerFactoryAlgorithmUseDefault(boolean)} has been set to 
     * <code>true</code> the value returned 
     * by {@link TrustManagerFactory#getDefaultAlgorithm()} will be used instead.
     * </p> 
     * 
     * @param algorithm the algorithm to use.
     */
    public void setTrustManagerFactoryAlgorithm(String algorithm) {
        this.trustManagerFactoryAlgorithm = algorithm;
    }

    /**
     * Sets the {@link KeyStore} which will be used in the call to 
     * {@link TrustManagerFactory#init(java.security.KeyStore)} when
     * the {@link SSLContext} is created. 
     * <p>
     * This property will be ignored if {@link ManagerFactoryParameters} has been
     * set directly using {@link #setTrustManagerFactoryParameters(ManagerFactoryParameters)}.
     * </p>
     * 
     * @param keyStore the key store.
     */
    public void setTrustManagerFactoryKeyStore(KeyStore keyStore) {
        this.trustManagerFactoryKeyStore = keyStore;
    }

    /**
     * Sets the {@link ManagerFactoryParameters} which will be used in the call to
     * {@link TrustManagerFactory#init(javax.net.ssl.ManagerFactoryParameters)} when
     * the {@link SSLContext} is created.
     *  
     * @param parameters describing provider-specific trust material.
     */
    public void setTrustManagerFactoryParameters(
            ManagerFactoryParameters parameters) {
        this.trustManagerFactoryParameters = parameters;
    }

    /**
     * Sets the provider to use when creating the {@link TrustManagerFactory} 
     * using 
     * {@link TrustManagerFactory#getInstance(java.lang.String, java.lang.String)}.
     * <p>
     * This property will be ignored if a {@link TrustManagerFactory} has been
     * set directly using {@link #setTrustManagerFactory(TrustManagerFactory)}.
     * </p>
     * <p>
     * If this property isn't set and no {@link TrustManagerFactory} has been set
     * using {@link #setTrustManagerFactory(TrustManagerFactory)} 
     * {@link TrustManagerFactory#getInstance(java.lang.String)} will be used
     * to create the {@link TrustManagerFactory}.
     * </p> 
     * 
     * @param provider the name of the provider.
     */
    public void setTrustManagerFactoryProvider(String provider) {
        this.trustManagerFactoryProvider = provider;
    }

    /**
     * Sets the {@link SecureRandom} to use when initializing the 
     * {@link SSLContext}. The JVM's default will be used if this isn't set.
     * 
     * @param secureRandom the {@link SecureRandom} or <code>null</code> if the 
     *        JVM's default should be used.
     * @see SSLContext#init(javax.net.ssl.KeyManager[], javax.net.ssl.TrustManager[], java.security.SecureRandom)
     */
    public void setSecureRandom(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }

}
