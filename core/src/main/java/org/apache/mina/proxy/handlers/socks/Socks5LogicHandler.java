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
package org.apache.mina.proxy.handlers.socks;

import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetSocketAddress;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.proxy.session.ProxyIoSession;
import org.apache.mina.proxy.utils.ByteUtilities;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Socks5LogicHandler.java - SOCKS5 authentication mechanisms logic handler.
 * 
 * @author Edouard De Oliveira <a href="mailto:doe_wanted@yahoo.fr">doe_wanted@yahoo.fr</a> 
 * @version $Id: $
 */
public class Socks5LogicHandler extends AbstractSocksLogicHandler {

    private final static Logger logger = LoggerFactory
            .getLogger(Socks5LogicHandler.class);

    /**
     * The selected authentication method.
     */
    private final static String SELECTED_AUTH_METHOD = Socks5LogicHandler.class
            .getName()
            + ".SelectedAuthMethod";

    /**
     * The current step in the handshake.
     */
    private final static String HANDSHAKE_STEP = Socks5LogicHandler.class
            .getName()
            + ".HandshakeStep";

    /**
     * The Java GSS-API context.
     */
    private final static String GSS_CONTEXT = Socks5LogicHandler.class
            .getName()
            + ".GSSContext";

    /**
     * Last GSS token received.
     */
    private final static String GSS_TOKEN = Socks5LogicHandler.class.getName()
            + ".GSSToken";

    public Socks5LogicHandler(final ProxyIoSession proxyIoSession) {
        super(proxyIoSession);
        getSession().setAttribute(HANDSHAKE_STEP,
                SocksProxyConstants.SOCKS5_GREETING_STEP);
    }

    /**
     * Perform any handshaking processing.
     */
    public synchronized void doHandshake(final NextFilter nextFilter) {
        logger.debug(" doHandshake()");

        // Send request
        int step = ((Integer) getSession().getAttribute(HANDSHAKE_STEP))
                .intValue();
        writeRequest(nextFilter, request, step);
    }

    /**
     * Encode the initial greeting packet.
     * 
     * @param request the socks proxy request data
     * @return the encoded buffer
     */
    private IoBuffer encodeInitialGreetingPacket(final SocksProxyRequest request) {
        byte nbMethods = (byte) SocksProxyConstants.SUPPORTED_AUTH_METHODS.length;
        IoBuffer buf = IoBuffer.allocate(2 + nbMethods);

        buf.put(request.getProtocolVersion());
        buf.put(nbMethods);
        buf.put(SocksProxyConstants.SUPPORTED_AUTH_METHODS);

        return buf;
    }

    /**
     * Encode the proxy authorization request packet.
     * 
     * @param request the socks proxy request data
     * @return the encoded buffer
     * @throws UnsupportedEncodingException
     */
    private IoBuffer encodeProxyRequestPacket(final SocksProxyRequest request)
            throws UnsupportedEncodingException {
        int len = 6;
        byte[] host = request.getHost() != null ? request.getHost().getBytes(
                "ASCII") : null;

        InetSocketAddress adr = request.getEndpointAddress();
        byte addressType = 0;

        if (adr != null) {
            if (adr.getAddress() instanceof Inet6Address) {
                len += 16;
                addressType = SocksProxyConstants.IPV6_ADDRESS_TYPE;
            } else if (adr.getAddress() instanceof Inet4Address) {
                len += 4;
                addressType = SocksProxyConstants.IPV4_ADDRESS_TYPE;
            }
        } else {
            len += 1 + host.length;
            addressType = SocksProxyConstants.DOMAIN_NAME_ADDRESS_TYPE;
        }

        IoBuffer buf = IoBuffer.allocate(len);

        buf.put(request.getProtocolVersion());
        buf.put(request.getCommandCode());
        buf.put((byte) 0x00); // Reserved
        buf.put(addressType);

        if (addressType == SocksProxyConstants.DOMAIN_NAME_ADDRESS_TYPE) {
            buf.put((byte) host.length);
            buf.put(host);
        } else {
            buf.put(request.getIpAddress());
        }

        buf.put(request.getPort());

        return buf;
    }

    /**
     * Encode the authentication packet for supported auth methods.
     * 
     * @param request the socks proxy request data
     * @return the encoded buffer
     * @throws UnsupportedEncodingException
     * @throws GSSException 
     */
    private IoBuffer encodeAuthenticationPacket(final SocksProxyRequest request)
            throws UnsupportedEncodingException, GSSException {
        byte method = ((Byte) getSession().getAttribute(
                Socks5LogicHandler.SELECTED_AUTH_METHOD)).byteValue();

        if (method == SocksProxyConstants.NO_AUTH) {
            getSession().setAttribute(HANDSHAKE_STEP,
                    SocksProxyConstants.SOCKS5_REQUEST_STEP);

        } else if (method == SocksProxyConstants.GSSAPI_AUTH) {
            GSSContext ctx = (GSSContext) getSession()
                    .getAttribute(GSS_CONTEXT);
            if (ctx == null) {
                GSSManager manager = GSSManager.getInstance();
                GSSName serverName = manager.createName(request
                        .getServiceKerberosName(), null);
                Oid krb5OID = new Oid(SocksProxyConstants.KERBEROS_V5_OID);

                if (logger.isDebugEnabled()) {
                    logger.debug("Available mechs:");
                    for (Oid o : manager.getMechs()) {
                        if (o.equals(krb5OID)) {
                            logger.debug("Found Kerberos V OID available");
                        }
                        logger.debug("{} with oid = {}", manager
                                .getNamesForMech(o), o);
                    }
                }

                ctx = manager.createContext(serverName, krb5OID, null,
                        GSSContext.DEFAULT_LIFETIME);

                ctx.requestMutualAuth(true); // Mutual authentication
                ctx.requestConf(false);
                ctx.requestInteg(false);

                getSession().setAttribute(GSS_CONTEXT, ctx);
            }

            byte[] token = (byte[]) getSession().getAttribute(GSS_TOKEN);
            if (token != null) {
                logger.debug("  Received Token[{}] = {}", token.length,
                        ByteUtilities.asHex(token));
            }
            IoBuffer buf = null;

            if (!ctx.isEstablished()) {
                // token is ignored on the first call
                if (token == null) {
                    token = new byte[32];
                }

                token = ctx.initSecContext(token, 0, token.length);

                // Send a token to the server if one was generated by
                // initSecContext
                if (token != null) {
                    logger.debug("  Sending Token[{}] = {}", token.length,
                            ByteUtilities.asHex(token));

                    getSession().setAttribute(GSS_TOKEN, token);
                    buf = IoBuffer.allocate(4 + token.length);
                    buf
                            .put(new byte[] {
                                    SocksProxyConstants.GSSAPI_AUTH_SUBNEGOTIATION_VERSION,
                                    SocksProxyConstants.GSSAPI_MSG_TYPE });

                    buf.put(ByteUtilities.intToNetworkByteOrder(token.length,
                            new byte[2], 0, 2));
                    buf.put(token);
                }
            }

            return buf;

        } else if (method == SocksProxyConstants.BASIC_AUTH) {
            byte[] user = request.getUserName().getBytes("ASCII");
            byte[] pwd = request.getPassword().getBytes("ASCII");
            IoBuffer buf = IoBuffer.allocate(3 + user.length + pwd.length);

            buf.put(SocksProxyConstants.BASIC_AUTH_SUBNEGOTIATION_VERSION);
            buf.put((byte) user.length);
            buf.put(user);
            buf.put((byte) pwd.length);
            buf.put(pwd);

            return buf;
        }

        return null;
    }

    /**
     * Encode a SOCKS5 request and send it to the proxy server.
     */
    private void writeRequest(final NextFilter nextFilter,
            final SocksProxyRequest request, int step) {
        try {
            IoBuffer buf = null;

            if (step == SocksProxyConstants.SOCKS5_GREETING_STEP) {
                buf = encodeInitialGreetingPacket(request);
            } else if (step == SocksProxyConstants.SOCKS5_AUTH_STEP) {
                buf = encodeAuthenticationPacket(request);
                if (buf == null) {
                    step = SocksProxyConstants.SOCKS5_REQUEST_STEP;
                }
            }

            if (step == SocksProxyConstants.SOCKS5_REQUEST_STEP) {
                buf = encodeProxyRequestPacket(request);
            }

            buf.flip();
            writeData(nextFilter, buf);

        } catch (Exception ex) {
            closeSession("Unable to send Socks request: ", ex);
        }
    }

    /**
     * Handle incoming data during the handshake process. Should consume only the
     * handshake data from the buffer, leaving any extra data in place.
     */
    public synchronized void messageReceived(final NextFilter nextFilter,
            final IoBuffer buf) {
        try {
            int step = ((Integer) getSession().getAttribute(HANDSHAKE_STEP))
                    .intValue();

            if (step == SocksProxyConstants.SOCKS5_GREETING_STEP
                    && buf.get(0) != SocksProxyConstants.SOCKS_VERSION_5) {
                throw new IllegalStateException(
                        "Wrong socks version running on server");
            }

            if ((step == SocksProxyConstants.SOCKS5_GREETING_STEP || step == SocksProxyConstants.SOCKS5_AUTH_STEP)
                    && buf.remaining() >= 2) {
                handleResponse(nextFilter, buf, step);
            } else if (step == SocksProxyConstants.SOCKS5_REQUEST_STEP
                    && buf.remaining() >= 5) {
                handleResponse(nextFilter, buf, step);
            }
        } catch (Exception ex) {
            closeSession("Proxy handshake failed: ", ex);
        }
    }

    /**
     * Handle a SOCKS v5 response from the proxy server.
     */
    protected void handleResponse(final NextFilter nextFilter,
            final IoBuffer buf, int step) throws Exception {
        int len = 2;
        if (step == SocksProxyConstants.SOCKS5_GREETING_STEP) {
            // Send greeting message
            byte method = buf.get(1);

            if (method == SocksProxyConstants.NO_ACCEPTABLE_AUTH_METHOD) {
                throw new IllegalStateException(
                        "No acceptable authentication method to use the socks proxy server");
            }

            getSession().setAttribute(SELECTED_AUTH_METHOD, new Byte(method));

        } else if (step == SocksProxyConstants.SOCKS5_AUTH_STEP) {
            // Authentication to the SOCKS server 
            byte method = ((Byte) getSession().getAttribute(
                    Socks5LogicHandler.SELECTED_AUTH_METHOD)).byteValue();

            if (method == SocksProxyConstants.GSSAPI_AUTH) {
                int oldPos = buf.position();

                if (buf.get(0) != 0x01) {
                    throw new IllegalStateException("Authentication failed");
                }
                if (buf.get(1) == 0xFF) {
                    throw new IllegalStateException(
                            "Authentication failed: GSS API Security Context Failure");
                }

                if (buf.remaining() >= 2) {
                    byte[] size = new byte[2];
                    buf.get(size);
                    int s = ByteUtilities.makeIntFromByte2(size);
                    if (buf.remaining() >= s) {
                        byte[] token = new byte[s];
                        buf.get(token);
                        getSession().setAttribute(GSS_TOKEN, token);
                        len = 0;
                    } else {
                        //buf.position(oldPos);
                        return;
                    }
                } else {
                    buf.position(oldPos);
                    return;
                }
            } else if (buf.get(1) != SocksProxyConstants.V5_REPLY_SUCCEEDED) {
                throw new IllegalStateException("Authentication failed");
            }

        } else if (step == SocksProxyConstants.SOCKS5_REQUEST_STEP) {
            // Send the request
            byte addressType = buf.get(3);
            len = 6;
            if (addressType == SocksProxyConstants.IPV6_ADDRESS_TYPE) {
                len += 16;
            } else if (addressType == SocksProxyConstants.IPV4_ADDRESS_TYPE) {
                len += 4;
            } else if (addressType == SocksProxyConstants.DOMAIN_NAME_ADDRESS_TYPE) {
                len += 1 + ((short) buf.get(4));
            } else {
                throw new IllegalStateException("Unknwon address type");
            }

            if (buf.remaining() >= len) {
                // handle response
                byte status = buf.get(1);
                logger.debug("  response status: {}", SocksProxyConstants
                        .getReplyCodeAsString(status));

                if (status == SocksProxyConstants.V5_REPLY_SUCCEEDED) {
                    buf.position(buf.position() + len);
                    setHandshakeComplete();
                    return;
                } else
                    throw new Exception("Proxy handshake failed - Code: 0x"
                            + ByteUtilities.asHex(new byte[] { status }));
            } else
                return;
        }

        if (len > 0) {
            buf.position(buf.position() + len);
        }

        // Move to the handshaking next step if not in the middle of
        // the authentication process
        boolean isAuthenticating = false;
        if (step == SocksProxyConstants.SOCKS5_AUTH_STEP) {
            byte method = ((Byte) getSession().getAttribute(
                    Socks5LogicHandler.SELECTED_AUTH_METHOD)).byteValue();
            if (method == SocksProxyConstants.GSSAPI_AUTH) {
                GSSContext ctx = (GSSContext) getSession().getAttribute(
                        GSS_CONTEXT);
                if (ctx == null || !ctx.isEstablished()) {
                    isAuthenticating = true;
                }
            }
        }

        if (!isAuthenticating) {
            getSession().setAttribute(HANDSHAKE_STEP, ++step);
        }

        doHandshake(nextFilter);
    }

    @Override
    protected void closeSession(String message) {
        GSSContext ctx = (GSSContext) getSession().getAttribute(GSS_CONTEXT);
        if (ctx != null) {
            try {
                ctx.dispose();
            } catch (GSSException e) {
                e.printStackTrace();
                super.closeSession(message, e);
                return;
            }
        }
        super.closeSession(message);
    }
}