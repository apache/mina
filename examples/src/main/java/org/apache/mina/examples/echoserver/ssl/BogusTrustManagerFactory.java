/*
 *   @(#) $Id$
 *
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.mina.examples.echoserver.ssl;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactorySpi;
import javax.net.ssl.X509TrustManager;

/**
 * Bogus trust manager factory. Creates BogusX509TrustManager
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
class BogusTrustManagerFactory extends TrustManagerFactorySpi
{

    static final X509TrustManager X509 = new X509TrustManager()
    {
        public void checkClientTrusted( X509Certificate[] x509Certificates,
                                       String s ) throws CertificateException
        {
        }

        public void checkServerTrusted( X509Certificate[] x509Certificates,
                                       String s ) throws CertificateException
        {
        }

        public X509Certificate[] getAcceptedIssuers()
        {
            return new X509Certificate[ 0 ];
        }
    };

    static final TrustManager[] X509_MANAGERS = new TrustManager[] { X509 };

    public BogusTrustManagerFactory()
    {
    }

    protected TrustManager[] engineGetTrustManagers()
    {
        return X509_MANAGERS;
    }

    protected void engineInit( KeyStore keystore ) throws KeyStoreException
    {
        // noop
    }

    protected void engineInit(
                              ManagerFactoryParameters managerFactoryParameters )
            throws InvalidAlgorithmParameterException
    {
        // noop
    }
}
