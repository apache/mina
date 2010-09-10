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
package org.apache.mina.example.proxy;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.mina.proxy.handlers.socks.SocksProxyConstants;
import org.apache.mina.proxy.utils.ByteUtilities;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.Oid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Socks5GSSAPITestServer.java - Basic test server for SOCKS5 GSSAPI authentication.
 * 
 * NOTE: Launch this program with the following params in a pre-configured Kerberos V env.
 * Do not forget to replace < ... > vars with your own values.
 * 
 * -Djava.security.krb5.realm=<your_krb_realm> 
 * -Djavax.security.auth.useSubjectCredsOnly=false 
 * -Djava.security.krb5.kdc=<your_kdc_hostname>
 * -Djava.security.auth.login.config=${workspace_loc}\Mina2Proxy\src\bcsLogin.conf
 * -Dsun.security.krb5.debug=true 
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @since MINA 2.0.0-M3
 */
public class Socks5GSSAPITestServer {

    private final static Logger logger = LoggerFactory
            .getLogger(Socks5GSSAPITestServer.class);

    /**
     * NOTE : change this to comply with your Kerberos environment.
     */
    protected final static String SERVICE_NAME = "host/myworkstation.local.network";

    /**
     * Selected mechanism message: advertises client to use SocksV5 protocol with
     * GSSAPI authentication.
     */
    public final static byte[] SELECT_GSSAPI_AUTH_MSG = new byte[] {
            SocksProxyConstants.SOCKS_VERSION_5,
            SocksProxyConstants.GSSAPI_AUTH };

    /**
     * Simulates a Socks v5 server using only Kerberos V authentication.
     * 
     * @param localPort the local port used to bind the server
     * @throws IOException
     * @throws GSSException
     */
    private static void doHandShake(int localPort) throws IOException,
            GSSException {
        ServerSocket ss = new ServerSocket(localPort);
        GSSManager manager = GSSManager.getInstance();

        /*
         * Create a GSSContext to receive the incoming request from the client. 
         * Use null for the server credentials passed in to tell the underlying 
         * mechanism to use whatever credentials it has available that can be 
         * used to accept this connection.
         */
        GSSCredential serverCreds = manager.createCredential(manager
                .createName(SERVICE_NAME, null),
                GSSCredential.DEFAULT_LIFETIME, new Oid(
                        SocksProxyConstants.KERBEROS_V5_OID),
                GSSCredential.ACCEPT_ONLY);

        while (true) {
            logger.debug("Waiting for incoming connection on port {} ...",
                    localPort);
            GSSContext context = manager.createContext(serverCreds);
            Socket socket = ss.accept();

            try {
                DataInputStream inStream = new DataInputStream(socket
                        .getInputStream());
                DataOutputStream outStream = new DataOutputStream(socket
                        .getOutputStream());

                logger.debug("Got connection from client @ {}", socket
                        .getInetAddress());

                // Read SOCKS5 greeting packet
                byte ver = (byte) inStream.read();
                if (ver != 0x05) {
                    throw new IllegalStateException(
                            "Wrong socks version received - " + ver);
                }
                byte nbAuthMethods = (byte) inStream.read();
                byte[] methods = new byte[nbAuthMethods];
                inStream.readFully(methods);

                boolean found = false;
                for (byte b : methods) {
                    if (b == SocksProxyConstants.GSSAPI_AUTH) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    throw new IllegalStateException(
                            "Client does not support GSSAPI authentication");
                }

                // Send selected mechanism message
                outStream.write(SELECT_GSSAPI_AUTH_MSG);
                outStream.flush();

                // Do the context establishment loop
                byte[] token = null;

                while (!context.isEstablished()) {
                    byte authVersion = (byte) inStream.read();

                    if (authVersion != 0x01) {
                        throw new IllegalStateException(
                                "Wrong socks GSSAPI auth version received: "
                                        + authVersion);
                    }

                    byte mtyp = (byte) inStream.read();
                    if (mtyp != 0x01) {
                        throw new IllegalArgumentException(
                                "Message type should be equal to 1.");
                    }

                    int len = inStream.readShort();
                    token = new byte[len];
                    inStream.readFully(token);
                    logger.debug("  Received Token[{}] = {}", len,
                            ByteUtilities.asHex(token));

                    token = context.acceptSecContext(token, 0, token.length);

                    // Send a token to the peer if one was generated by acceptSecContext
                    if (token != null) {
                        logger.debug("    Sending Token[{}] = {}", token.length,
                                ByteUtilities.asHex(token));
                        outStream.writeByte(authVersion);
                        outStream.writeByte(mtyp);
                        outStream.writeShort(token.length);
                        outStream.write(token);
                        outStream.flush();
                    }
                }

                logger.debug("Context Established !");
                logger.debug("Client is {}", context.getSrcName());
                logger.debug("Server is {}", context.getTargName());

                /*
                 * If mutual authentication did not take place, then
                 * only the client was authenticated to the
                 * server. Otherwise, both client and server were
                 * authenticated to each other. 
                 */
                if (context.getMutualAuthState()) {
                    logger.debug("Mutual authentication took place !");
                }

                // We can now abort the process after a short time as auth is OK
                // and finally block will close session
                Thread.sleep(500);
            } catch (Exception ex) {
                //ex.printStackTrace();
            } finally {
                context.dispose();
                socket.close();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public static void main(String[] args) throws Exception {
        // Obtain the command-line arguments and parse the port number
        if (args.length != 1) {
            System.err
                    .println("Usage: java <options> Socks5GSSAPITestServer <localPort>");
            System.exit(-1);
        }

        doHandShake(Integer.parseInt(args[0]));
        System.exit(0);
    }
}