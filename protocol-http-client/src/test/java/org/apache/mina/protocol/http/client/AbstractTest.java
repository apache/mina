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
package org.apache.mina.protocol.http.client;

import java.io.File;
import java.io.FileDescriptor;
import java.net.InetAddress;
import java.security.Permission;

import junit.framework.TestCase;

import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.startup.Embedded;

public abstract class AbstractTest extends TestCase {

    protected final File BASEDIR = getBaseDir();

    protected final File CATALINAHOME = new File(BASEDIR, "src/test/catalina");

    protected final File WORK = new File(BASEDIR, "target/work");

    protected final File KEYSTORE = new File(CATALINAHOME, "conf/keystore");
    protected final File CERT = new File(CATALINAHOME, "conf/ca_bundle.crt");

    protected final File WEBAPPS = new File(CATALINAHOME, "webapps");

    protected final File ROOT = new File(WEBAPPS, "ROOT");

    protected Embedded server;

    @Override
    protected void setUp() throws Exception {
        System.out.println("BASEDIR = " + BASEDIR.getAbsolutePath());
        server = new Embedded();
        server.setCatalinaHome(CATALINAHOME.getAbsolutePath());

        Engine engine = server.createEngine();
        engine.setDefaultHost("localhost");
        
        System.setSecurityManager(new AprDisablingSecurityManager());

        Host host = server.createHost("localhost", WEBAPPS.getAbsolutePath());
        ((StandardHost) host).setWorkDir(WORK.getAbsolutePath());
        engine.addChild(host);

        Context context = server.createContext("", ROOT.getAbsolutePath());
        context.setParentClassLoader(Thread.currentThread()
                .getContextClassLoader());
        host.addChild(context);

        server.addEngine(engine);

        //Http
        Connector http = server.createConnector("localhost", 8282, false);
        server.addConnector(http);

        //Https
        Connector https = server.createConnector("localhost", 8383, true);
        https.setAttribute("keystoreFile", KEYSTORE.getAbsolutePath());
        https.setAttribute("SSLCertificateFile", CERT.getAbsolutePath());
        server.addConnector(https);
        server.start();
    }

    @Override
    protected void tearDown() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    protected final File getBaseDir() {
        File dir;

        // If ${basedir} is set, then honor it
        String tmp = System.getProperty("basedir");
        if (tmp != null) {
            dir = new File(tmp);
        } else {
            // Find the directory which this class (or really the sub-class of TestSupport) is defined in.
            String path = getClass().getProtectionDomain().getCodeSource()
                    .getLocation().getFile();

            // We expect the file to be in target/test-classes, so go up 2 dirs
            dir = new File(path).getParentFile().getParentFile();

            // Set ${basedir} which is needed by logging to initialize
            System.setProperty("basedir", dir.getPath());
        }

        return dir;
    }
    
    private static class AprDisablingSecurityManager extends SecurityManager {
        @Override
        public void checkLink(String lib) {
            if (lib != null && lib.indexOf("tcnative") >= 0) {
                throw new SecurityException("APR has been disabled.");
            }
        }

        @Override
        public void checkAccept(String host, int port) {
        }

        @Override
        public void checkAccess(Thread t) {
        }

        @Override
        public void checkAccess(ThreadGroup g) {
        }

        @Override
        public void checkConnect(String host, int port, Object context) {
        }

        @Override
        public void checkConnect(String host, int port) {
        }

        @Override
        public void checkCreateClassLoader() {
        }

        @Override
        public void checkDelete(String file) {
        }

        @Override
        public void checkExec(String cmd) {
        }

        @Override
        public void checkListen(int port) {
        }

        @Override
        public void checkMemberAccess(Class<?> clazz, int which) {
        }

        @Override
        public void checkMulticast(InetAddress maddr) {
        }

        @Override
        public void checkPackageAccess(String pkg) {
        }

        @Override
        public void checkPackageDefinition(String pkg) {
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
        }

        @Override
        public void checkPermission(Permission perm) {
        }

        @Override
        public void checkPropertiesAccess() {
        }

        @Override
        public void checkPropertyAccess(String key) {
        }

        @Override
        public void checkRead(FileDescriptor fd) {
        }

        @Override
        public void checkRead(String file, Object context) {
        }

        @Override
        public void checkRead(String file) {
        }

        @Override
        public void checkSecurityAccess(String target) {
        }

        @Override
        public void checkSetFactory() {
        }

        @Override
        public void checkWrite(FileDescriptor fd) {
        }

        @Override
        public void checkWrite(String file) {
        }
    }
}
