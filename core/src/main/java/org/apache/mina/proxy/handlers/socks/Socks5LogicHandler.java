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
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @since MINA 2.0.0-M3
 */
public class Socks5LogicHandler extends AbstractSocksLogicHandler {

    private final static Logger LOGGER = LoggerFactory
            .getLogger(Socks5LogicHandler.class);

    /**
     * The selected authentication method attribute key.
     */
    private final static String SELECTED_AUTH_METHOD = Socks5LogicHandler.class
            .getName()
            + ".SelectedAuthMethod";

    /**
     * The current step in the handshake attribute key.
     */
    private final static String HANDSHAKE_STEP = Socks5LogicHandler.class
            .getName()
            + ".HandshakeStep";

    /**
     * The Java GSS-API context attribute key.
     */
    private final static String GSS_CONTEXT = Socks5LogicHandler.class
            .getName()
            + ".GSSContext";

    /**
     * Last GSS token received attribute key.
     */
    private final static String GSS_TOKEN = Socks5LogicHandler.class.getName()
            + ".GSSToken";

    /**
     * {@inheritDoc}
     */
    public Socks5LogicHandler(final ProxyIoSession proxyIoSession) {
        super(proxyIoSession);
        getSession().setAttribute(HANDSHAKE_STEP,
                SocksProxyConstants.SOCKS5_GREETING_STEP);
    }

    /**
     * Performs the handshake process.
     * 
     * @param nextFilter the next filter
     */
    public synchronized void doHandshake(final NextFilter nextFilter) {
        LOGGER.debug(" doHandshake()");

        // Send request
        writeRequest(nextFilter, request, ((Integer) getSession().getAttribute(
                HANDSHAKE_STEP)).intValue());
    }

    /**
     * Encodes the initial greeting packet.
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
     * Encodes the proxy authorization request packet.
     * 
     * @param request the socks proxy request data
     * @return the encoded buffer
     * @throws UnsupportedEncodingException if request's hostname charset 
     * can't be converted to ASCII. 
     */
    private IoBuffer encodeProxyRequestPacket(final SocksProxyRequest request)
            throws UnsupportedEncodingException {
        int len = 6;
        InetSocketAddress adr = request.getEndpointAddress();
        byte addressType = 0;
        byte[] host = null;
        
        if (adr != null && !adr.isUnresolved()) {
            if (adr.getAddress() instanceof Inet6Address) {
                len += 16;
                addressType = SocksProxyConstants.IPV6_ADDRESS_TYPE;
            } else if (adr.getAddress() instanceof Inet4Address) {
                len += 4;
                addressType = SocksProxyConstants.IPV4_ADDRESS_TYPE;
            }
        } else {
            host = request.getHost() != null ? 
                    request.getHost().getBytes("ASCII") : null;

            if (host != null) {
                len += 1 + host.length;
                addressType = SocksProxyConstants.DOMAIN_NAME_ADDRESS_TYPE;
            } else {
                throw new IllegalArgumentException("SocksProxyRequest object " +
                        "has no suitable endpoint information");
            }
        }
        
        IoBuffer buf = IoBuffer.allocate(len);

        buf.put(request.getProtocolVersion());
        buf.put(request.getCommandCode());
        buf.put((byte) 0x00); // Reserved
        buf.put(addressType);

        if (host == null) {
            buf.put(request.getIpAddress());
        } else {
            buf.put((byte) host.length);
            buf.put(host);            
        }

        buf.put(request.getPort());

        return buf;
    }

    /**
     * Encodes the authentication packet for supported authentication methods.
     * 
     * @param request the socks proxy request data
     * @return the encoded buffer, if null then authentication step is over 
     * and handshake process can jump immediately to the next step without waiting
     * for a server reply.
     * @throws UnsupportedEncodingException if some string charset convertion fails
     * @throws GSSException when something fails while using GSSAPI
     */
    private IoBuffer encodeAuthenticationPacket(final SocksProxyRequest request)
            throws UnsupportedEncodingException, GSSException {
        byte method = ((Byte) getSession().getAttribute(
                Socks5LogicHandler.SELECTED_AUTH_METHOD)).byteValue();

        switch (method) {
            case SocksProxyConstants.NO_AUTH:
                // In this case authentication is immediately considered as successfull
                // Next writeRequest() call will send the proxy request
                getSession().setAttribute(HANDSHAKE_STEP,
                        SocksProxyConstants.SOCKS5_REQUEST_STEP);
                break;
    
            case SocksProxyConstants.GSSAPI_AUTH:
                return encodeGSSAPIAuthenticationPacket(request);
    
            case SocksProxyConstants.BASIC_AUTH:
                // The basic auth scheme packet is sent
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
     * Encodes the authentication packet for supported authentication methods.
     * 
     * @param request the socks proxy request data
     * @return the encoded buffer
     * @throws GSSException when something fails while using GSSAPI
     */
    private IoBuffer encodeGSSAPIAuthenticationPacket(
            final SocksProxyRequest request) throws GSSException {
        GSSContext ctx = (GSSContext) getSession().getAttribute(GSS_CONTEXT);
        if (ctx == null) {
            // first step in the authentication process
            GSSManager manager = GSSManager.getInstance();
            GSSName serverName = manager.createName(request
                    .getServiceKerberosName(), null);
            Oid krb5OID = new Oid(SocksProxyConstants.KERBEROS_V5_OID);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Available mechs:");
                for (Oid o : manager.getMechs()) {
                    if (o.equals(krb5OID)) {
                        LOGGER.debug("Found Kerberos V OID available");
                    }
                    LOGGER.debug("{} with oid = {}",
                            manager.getNamesForMech(o), o);
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
            LOGGER.debug("  Received Token[{}] = {}", token.length,
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
                LOGGER.debug("  Sending Token[{}] = {}", token.length,
                        ByteUtilities.asHex(token));

                getSession().setAttribute(GSS_TOKEN, token);
                buf = IoBuffer.allocate(4 + token.length);
                buf.put(new byte[] {
                        SocksProxyConstants.GSSAPI_AUTH_SUBNEGOTIATION_VERSION,
                        SocksProxyConstants.GSSAPI_MSG_TYPE });

                buf.put(ByteUtilities.intToNetworkByteOrder(token.length, 2));
                buf.put(token);
            }
        }

        return buf;
    }

    /**
     * Encodes a SOCKS5 request and writes it to the next filter
     * so it can be sent to the proxy server.
     * 
     * @param nextFilter the next filter
     * @param request the request to send.
     * @param step the current step in the handshake process
     */
    private void writeRequest(final NextFilter nextFilter,
            final SocksProxyRequest request, int step) {
        try {
            IoBuffer buf = null;

            if (step == SocksProxyConstants.SOCKS5_GREETING_STEP) {
                buf = encodeInitialGreetingPacket(request);
            } else if (step == SocksProxyConstants.SOCKS5_AUTH_STEP) {
                // This step can happen multiple times like in GSSAPI auth for instance
                buf = encodeAuthenticationPacket(request);
                // If buf is null then go to the next step
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
     * Handles incoming data during the handshake process. Should consume only the
     * handshake data from the buffer, leaving any extra data in place.
     * 
     * @param nextFilter the next filter
     * @param buf the buffered data received 
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

            if ((step == SocksProxyConstants.SOCKS5_GREETING_STEP || 
                    step == SocksProxyConstants.SOCKS5_AUTH_STEP)
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
     * 
     * @param nextFilter the next filter
     * @param buf the buffered data received 
     * @param step the current step in the authentication process     
     */
    protected void handleResponse(final NextFilter nextFilter,
            final IoBuffer buf, int step) throws Exception {
        int len = 2;
        if (step == SocksProxyConstants.SOCKS5_GREETING_STEP) {
            // Send greeting message
            byte method = buf.get(1);

            if (method == SocksProxyConstants.NO_ACCEPTABLE_AUTH_METHOD) {
                throw new IllegalStateException(
                        "No acceptable authentication method to use with " +
                        "the socks proxy server");
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
                len += 1 + (buf.get(4));
            } else {
                throw new IllegalStateException("Unknwon address type");
            }

            if (buf.remaining() >= len) {
                // handle response
                byte status = buf.get(1);
                LOGGER.debug("  response status: {}", SocksProxyConstants
                        .getReplyCodeAsString(status));

                if (status == SocksProxyConstants.V5_REPLY_SUCCEEDED) {
                    buf.position(buf.position() + len);
                    setHandshakeComplete();
                    return;
                }

                throw new Exception("Proxy handshake failed - Code: 0x"
                            + ByteUtilities.asHex(new byte[] { status }));
            }

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

    /**
     * Closes the session. If any {@link GSSContext} is present in the session 
     * then it is closed.
     * 
     * @param message the error message
     */    
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