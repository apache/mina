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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;

import org.junit.Test;

/**
 * Tests {@link KeyStoreFactory}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class KeyStoreFactoryTest {
    @Test
    public void testCreateInstanceFromResource() throws Exception {
        // Test using default for now.
        KeyStoreFactory factory = new KeyStoreFactory();
        factory.setDataUrl(getClass().getResource("keystore.cert"));
        factory.setPassword("boguspw");

        KeyStore ks = factory.newInstance();

        ks.getCertificate("bogus");
        ks.getKey("bogus", "boguspw".toCharArray());
    }

    @Test
    public void testCreateInstanceFromFile() throws Exception {
        // Copy the keystore from the class path to a temporary file.
        File file = File.createTempFile("keystoretest ", null);
        file.deleteOnExit();
        InputStream in = getClass().getResourceAsStream("keystore.cert");
        OutputStream out = new FileOutputStream(file);
        int b;
        
        while ((b = in.read()) != -1) {
            out.write(b);
        }
        
        in.close();
        out.close();

        // Test using default for now.
        KeyStoreFactory factory = new KeyStoreFactory();
        factory.setDataFile(file);
        factory.setPassword("boguspw");

        KeyStore ks = factory.newInstance();

        ks.getCertificate("bogus");
        ks.getKey("bogus", "boguspw".toCharArray());
    }
}
