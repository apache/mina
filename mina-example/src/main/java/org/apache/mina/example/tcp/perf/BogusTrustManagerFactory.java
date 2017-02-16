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
package org.apache.mina.example.tcp.perf;

import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactorySpi;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Bogus trust manager factory. Creates BogusX509TrustManager
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
class BogusTrustManagerFactory extends TrustManagerFactorySpi {

    static final X509TrustManager X509 = new X509ExtendedTrustManager() {

        @Override
        public void checkClientTrusted( X509Certificate[] chain, String authType ) throws CertificateException {
            // Nothing to do
        }

        @Override
        public void checkServerTrusted( X509Certificate[] chain, String authType ) throws CertificateException {
            // Nothing to do
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

        @Override
        public void checkClientTrusted( X509Certificate[] chain, String authType, Socket socket )
            throws CertificateException {
            // Nothing to do
        }

        @Override
        public void checkClientTrusted( X509Certificate[] chain, String authType, SSLEngine engine )
            throws CertificateException {
            // Nothing to do
        }

        @Override
        public void checkServerTrusted( X509Certificate[] chain, String authType, Socket socket )
            throws CertificateException {
            // Nothing to do
        }

        @Override
        public void checkServerTrusted( X509Certificate[] chain, String authType, SSLEngine engine )
            throws CertificateException {
            // Nothing to do
        }
    };

    static final TrustManager[] X509_MANAGERS = new TrustManager[] { X509 };

    public BogusTrustManagerFactory() {
    }

    @Override
    protected TrustManager[] engineGetTrustManagers() {
        return X509_MANAGERS;
    }

    @Override
    protected void engineInit(KeyStore keystore) throws KeyStoreException {
        // noop
    }

    @Override
    protected void engineInit(ManagerFactoryParameters managerFactoryParameters)
            throws InvalidAlgorithmParameterException {
        // noop
    }
}
