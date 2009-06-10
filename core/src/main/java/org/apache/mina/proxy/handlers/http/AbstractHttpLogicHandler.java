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
package org.apache.mina.proxy.handlers.http;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.apache.mina.proxy.AbstractProxyLogicHandler;
import org.apache.mina.proxy.ProxyAuthException;
import org.apache.mina.proxy.session.ProxyIoSession;
import org.apache.mina.proxy.utils.IoBufferDecoder;
import org.apache.mina.proxy.utils.StringUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AbstractHttpLogicHandler.java - Base class for HTTP proxy {@link AbstractProxyLogicHandler} implementations. 
 * Provides HTTP request encoding/response decoding functionality.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @since MINA 2.0.0-M3
 */
public abstract class AbstractHttpLogicHandler extends
        AbstractProxyLogicHandler {
    private final static Logger LOGGER = LoggerFactory
            .getLogger(AbstractHttpLogicHandler.class);

    private final static String DECODER = AbstractHttpLogicHandler.class
            .getName()
            + ".Decoder";

    private final static byte[] HTTP_DELIMITER = new byte[] { '\r', '\n', '\r',
            '\n' };

    private final static byte[] CRLF_DELIMITER = new byte[] { '\r', '\n' };

    // Parsing vars

    /**
     * Temporary buffer to accumulate the HTTP response from the proxy.
     */
    private IoBuffer responseData = null;

    /**
     * The parsed http proxy response
     */
    private HttpProxyResponse parsedResponse = null;

    /**
     * The content length of the proxy response.
     */
    private int contentLength = -1;

    // HTTP/1.1 vars

    /**
     * A flag that indicates that this is a HTTP/1.1 response with chunked data.and that some chunks are missing.   
     */
    private boolean hasChunkedData;

    /**
     * A flag that indicates that some chunks of data are missing to complete the HTTP/1.1 response.   
     */
    private boolean waitingChunkedData;

    /**
     * A flag that indicates that chunked data has been read and that we're now reading the footers.   
     */
    private boolean waitingFooters;

    /**
     * Contains the position of the entity body start in the <code>responseData</code> {@link IoBuffer}.
     */
    private int entityBodyStartPosition;

    /**
     * Contains the limit of the entity body start in the <code>responseData</code> {@link IoBuffer}.
     */
    private int entityBodyLimitPosition;

    /**
     * Creates a new {@link AbstractHttpLogicHandler}.
     * 
     * @param proxyIoSession the {@link ProxyIoSession} in use.
     * @param request the requested url to negotiate with the proxy.
     */
    public AbstractHttpLogicHandler(final ProxyIoSession proxyIoSession) {
        super(proxyIoSession);
    }

    /**
     * Handles incoming data during the handshake process. Should consume only the
     * handshake data from the buffer, leaving any extra data in place.
     * 
     * @param nextFilter the next filter
     * @param buf the buffer holding received data
     */
    public synchronized void messageReceived(final NextFilter nextFilter,
            final IoBuffer buf) throws ProxyAuthException {
        LOGGER.debug(" messageReceived()");

        IoBufferDecoder decoder = (IoBufferDecoder) getSession().getAttribute(
                DECODER);
        if (decoder == null) {
            decoder = new IoBufferDecoder(HTTP_DELIMITER);
            getSession().setAttribute(DECODER, decoder);
        }

        try {
            if (parsedResponse == null) {

                responseData = decoder.decodeFully(buf);
                if (responseData == null) {
                    return;
                }

                // Handle the response                                
                String responseHeader = responseData
                        .getString(getProxyIoSession().getCharset()
                                .newDecoder());
                entityBodyStartPosition = responseData.position();

                LOGGER.debug("  response header received:\n{}", responseHeader
                        .replace("\r", "\\r").replace("\n", "\\n\n"));

                // Parse the response
                parsedResponse = decodeResponse(responseHeader);

                // Is handshake complete ?
                if (parsedResponse.getStatusCode() == 200
                        || (parsedResponse.getStatusCode() >= 300 && parsedResponse
                                .getStatusCode() <= 307)) {
                    buf.position(0);
                    setHandshakeComplete();
                    return;
                }

                String contentLengthHeader = StringUtilities
                        .getSingleValuedHeader(parsedResponse.getHeaders(),
                                "Content-Length");

                if (contentLengthHeader == null) {
                    contentLength = 0;
                } else {
                    contentLength = Integer
                            .parseInt(contentLengthHeader.trim());
                    decoder.setContentLength(contentLength, true);
                }
            }

            if (!hasChunkedData) {
                if (contentLength > 0) {
                    IoBuffer tmp = decoder.decodeFully(buf);
                    if (tmp == null) {
                        return;
                    }
                    responseData.setAutoExpand(true);
                    responseData.put(tmp);
                    contentLength = 0;
                }

                if ("chunked".equalsIgnoreCase(StringUtilities
                        .getSingleValuedHeader(parsedResponse.getHeaders(),
                                "Transfer-Encoding"))) {
                    // Handle Transfer-Encoding: Chunked
                    LOGGER.debug("Retrieving additional http response chunks");
                    hasChunkedData = true;
                    waitingChunkedData = true;
                }
            }

            if (hasChunkedData) {
                // Read chunks
                while (waitingChunkedData) {
                    if (contentLength == 0) {
                        decoder.setDelimiter(CRLF_DELIMITER, false);
                        IoBuffer tmp = decoder.decodeFully(buf);
                        if (tmp == null) {
                            return;
                        }

                        String chunkSize = tmp.getString(getProxyIoSession()
                                .getCharset().newDecoder());
                        int pos = chunkSize.indexOf(';');
                        if (pos >= 0) {
                            chunkSize = chunkSize.substring(0, pos);
                        } else {
                            chunkSize = chunkSize.substring(0, chunkSize
                                    .length() - 2);
                        }
                        contentLength = Integer.decode("0x" + chunkSize);
                        if (contentLength > 0) {
                            contentLength += 2; // also read chunk's trailing CRLF
                            decoder.setContentLength(contentLength, true);
                        }
                    }

                    if (contentLength == 0) {
                        waitingChunkedData = false;
                        waitingFooters = true;
                        entityBodyLimitPosition = responseData.position();
                        break;
                    }

                    IoBuffer tmp = decoder.decodeFully(buf);
                    if (tmp == null) {
                        return;
                    }
                    contentLength = 0;
                    responseData.put(tmp);
                    buf.position(buf.position());
                }

                // Read footers
                while (waitingFooters) {
                    decoder.setDelimiter(CRLF_DELIMITER, false);
                    IoBuffer tmp = decoder.decodeFully(buf);
                    if (tmp == null) {
                        return;
                    }

                    if (tmp.remaining() == 2) {
                        waitingFooters = false;
                        break;
                    }

                    // add footer to headers                    
                    String footer = tmp.getString(getProxyIoSession()
                            .getCharset().newDecoder());
                    String[] f = footer.split(":\\s?", 2);
                    StringUtilities.addValueToHeader(parsedResponse
                            .getHeaders(), f[0], f[1], false);
                    responseData.put(tmp);
                    responseData.put(CRLF_DELIMITER);
                }
            }

            responseData.flip();

            LOGGER.debug("  end of response received:\n{}",
                    responseData.getString(getProxyIoSession().getCharset()
                            .newDecoder()));

            // Retrieve entity body content
            responseData.position(entityBodyStartPosition);
            responseData.limit(entityBodyLimitPosition);
            parsedResponse.setBody(responseData.getString(getProxyIoSession()
                    .getCharset().newDecoder()));

            // Free the response buffer
            responseData.free();
            responseData = null;

            handleResponse(parsedResponse);

            parsedResponse = null;
            hasChunkedData = false;
            contentLength = -1;
            decoder.setDelimiter(HTTP_DELIMITER, true);

            if (!isHandshakeComplete()) {
                doHandshake(nextFilter);
            }
        } catch (Exception ex) {
            if (ex instanceof ProxyAuthException) {
                throw ((ProxyAuthException) ex);
            }

            throw new ProxyAuthException("Handshake failed", ex);
        }
    }

    /**
     * Handles a HTTP response from the proxy server.
     * 
     * @param response The response.
     */
    public abstract void handleResponse(final HttpProxyResponse response)
            throws ProxyAuthException;

    /**
     * Calls{@link #writeRequest0(NextFilter, HttpProxyRequest)} to write the request. 
     * If needed a reconnection to the proxy is done previously.
     * 
     * @param nextFilter the next filter
     * @param request the http request
     */
    public void writeRequest(final NextFilter nextFilter,
            final HttpProxyRequest request) {
        ProxyIoSession proxyIoSession = getProxyIoSession();

        if (proxyIoSession.isReconnectionNeeded()) {
            reconnect(nextFilter, request);
        } else {
            writeRequest0(nextFilter, request);
        }
    }

    /**
     * Encodes a HTTP request and sends it to the proxy server.
     * 
     * @param nextFilter the next filter
     * @param request the http request
     */
    private void writeRequest0(final NextFilter nextFilter,
            final HttpProxyRequest request) {
        try {
            String data = request.toHttpString();
            IoBuffer buf = IoBuffer.wrap(data.getBytes(getProxyIoSession()
                    .getCharsetName()));

            LOGGER.debug("   write:\n{}", data.replace("\r", "\\r").replace(
                    "\n", "\\n\n"));

            writeData(nextFilter, buf);

        } catch (UnsupportedEncodingException ex) {
            closeSession("Unable to send HTTP request: ", ex);
        }
    }

    /**
     * Method to reconnect to the proxy when it decides not to maintain the connection 
     * during handshake.
     * 
     * @param nextFilter the next filter
     * @param request the http request
     */
    private void reconnect(final NextFilter nextFilter,
            final HttpProxyRequest request) {
        LOGGER.debug("Reconnecting to proxy ...");

        final ProxyIoSession proxyIoSession = getProxyIoSession();

        // Fires reconnection
        proxyIoSession.getConnector().connect(
                new IoSessionInitializer<ConnectFuture>() {
                    public void initializeSession(final IoSession session,
                            ConnectFuture future) {
                        LOGGER.debug("Initializing new session: {}", session);
                        session.setAttribute(ProxyIoSession.PROXY_SESSION,
                                proxyIoSession);
                        proxyIoSession.setSession(session);
                        LOGGER.debug("  setting up proxyIoSession: {}", proxyIoSession);
                        future
                                .addListener(new IoFutureListener<ConnectFuture>() {
                                    public void operationComplete(
                                            ConnectFuture future) {
                                        // Reconnection is done so we send the
                                        // request to the proxy
                                        proxyIoSession
                                                .setReconnectionNeeded(false);
                                        writeRequest0(nextFilter, request);
                                    }
                                });
                    }
                });
    }

    /**
     * Parse a HTTP response from the proxy server.
     * 
     * @param response The response string.
     */
    protected HttpProxyResponse decodeResponse(final String response)
            throws Exception {
        LOGGER.debug("  parseResponse()");

        // Break response into lines
        String[] responseLines = response.split(HttpProxyConstants.CRLF);

        // Status-Line = HTTP-Version SP Status-Code SP Reason-Phrase CRLF
        // BUG FIX : Trimed to prevent failures with some proxies that add 
        // extra space chars like "Microsoft-IIS/5.0" ...
        String[] statusLine = responseLines[0].trim().split(" ", 2);

        if (statusLine.length < 2) {
            throw new Exception("Invalid response status line (" + statusLine
                    + "). Response: " + response);
        }

        // Status code is 3 digits
        if (statusLine[1].matches("^\\d\\d\\d")) {
            throw new Exception("Invalid response code (" + statusLine[1]
                    + "). Response: " + response);
        }

        Map<String, List<String>> headers = new HashMap<String, List<String>>();

        for (int i = 1; i < responseLines.length; i++) {
            String[] args = responseLines[i].split(":\\s?", 2);
            StringUtilities.addValueToHeader(headers, args[0], args[1], false);
        }

        return new HttpProxyResponse(statusLine[0], statusLine[1], headers);
    }
}
