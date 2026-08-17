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
package org.apache.mina.example.echoserver.ssl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class TlsClient {
    private static final String CERTIFICATE = "bogus.cert";
    private static final char[] PASSWORD = new char[] { 'b', 'o', 'g', 'u', 's', 'p', 'w' };
    private static final String PROTOCOL = "TLSv1.3";
    private static final String KEY_MANAGER_FACTORY_ALGORITHM;

    static {
        String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");

        if (algorithm == null) {
            algorithm = KeyManagerFactory.getDefaultAlgorithm();
        }

        KEY_MANAGER_FACTORY_ALGORITHM = algorithm;
    }

    public static void main(String[] args) throws KeyStoreException, NoSuchAlgorithmException, CertificateException,
            IOException, UnrecoverableKeyException, KeyManagementException {

        // Create keystore with the test certificate
        KeyStore ks = KeyStore.getInstance("JKS");
        InputStream in = null;

        try {
            in = TlsClient.class.getResourceAsStream(CERTIFICATE);
            ks.load(in, PASSWORD);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
        }

        // Create a TrustManagerFactory from our test keystore
        TrustManagerFactory tmf = TrustManagerFactory
                .getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);

        // Create a SSL socket factory that uses our trust manager
        SSLContext sslContext = SSLContext.getInstance(PROTOCOL);
        sslContext.init(null, tmf.getTrustManagers(), null);
        SSLSocketFactory sslFactory = sslContext.getSocketFactory();

        try {
            // Create a socket - will not connect yet
            SSLSocket socket = (SSLSocket) sslFactory.createSocket("localhost", 8080);

            if (socket == null) {
                return;
            }

            socket.setEnabledCipherSuites(
                    new String[] { "TLS_AES_128_GCM_SHA256", "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256" });
            socket.setEnabledProtocols(new String[] { "TLSv1.3", "TLSv1.2" });

            // Handshake to create a session
            socket.startHandshake();

            // What parameters were established?
            System.out.println(String.format("Negotiated Session: %s", socket.getSession().getProtocol()));
            System.out.println(String.format("Cipher Suite: %s", socket.getSession().getCipherSuite()));

            // We're reading and writing bytes. Other streams can be used.
            BufferedOutputStream output = new BufferedOutputStream(socket.getOutputStream());
            BufferedInputStream input = new BufferedInputStream(socket.getInputStream());
            output.write(new byte[] { 2, 3, 5, 7, 11, 13, 17, 19, 23 });
            output.flush();

            // Read the server response up to a max of 64 bytes.
            byte[] serverResponse = new byte[64];
            int len = input.read(serverResponse, 0, 64);

            // Expect 9 bytes back
            if (len == 9) {
                System.out.println("\nServer result: " + Arrays.toString(Arrays.copyOfRange(serverResponse, 0, 9)));
            } else {
                System.out.println("\nServer response length: " + len);
            }
            
            // Try with a bigger buffer, 64Kb
            int bufferSize = 1_024*16*4;
            byte[] bigBuffer = new byte[bufferSize];
            
            for (int i=0; i<bufferSize;i++) {
                bigBuffer[i] = (byte)i;
            }
            output.write(bigBuffer);
            output.flush();
            
            // Loop while we have something to receive
            boolean completed = false;
            int expectedLen = bufferSize;
            int offset = 0;
            
            while (!completed) {
                serverResponse = new byte[bufferSize];
                len = input.read(serverResponse, 0, bufferSize);
                
                // Check the content
                for (int i = 0; i < len; i++) {
                    if (serverResponse[i] != bigBuffer[i+offset]) {
                        System.out.println( "ERROR!!!");
                        input.close();
                        output.close();
                        socket.close();
                        
                        return;
                    }
                }
                
                offset += len;
                
                expectedLen -= len;

                System.out.println(len + " bytes received," + expectedLen + " to be received, data checked");
            
                if (expectedLen==0) {
                    System.out.println("64K written and received");
                    completed = true;
                }
            }
            
            input.close();
            output.close();
            socket.close();
        } catch (Exception e) {
            System.err.println("Exception: " + e.toString());
        }
    }
}
