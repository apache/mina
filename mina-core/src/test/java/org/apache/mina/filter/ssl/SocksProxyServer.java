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

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.service.SimpleIoProcessorPool;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.apache.mina.transport.socket.nio.NioProcessor;
import org.apache.mina.transport.socket.nio.NioSession;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.apache.mina.util.DaemonThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.channels.UnresolvedAddressException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * This was AI-generated.
 * <p>
 * Simple SOCKS4 / SOCKS4a / SOCKS5 proxy server.
 * <p>
 * The protocol version is detected from the first byte sent by the client.
 * <ul>
 *   <li>SOCKS5: "no authentication" method, CONNECT command, IPv4 / IPv6 / domain name addresses.</li>
 *   <li>SOCKS4: CONNECT command, IPv4 addresses. The user id is read and ignored (no identd check).</li>
 *   <li>SOCKS4a: CONNECT command, domain name addresses (the proxy resolves the name).</li>
 * </ul>
 * Not supported: BIND, UDP ASSOCIATE, username/password authentication.
 * <p>
 * Flow: client connects (acceptor session) -> handshake / CONNECT request -> proxy opens an outbound
 * connection (connector session) -> reply is sent -> bytes are relayed in both directions until either side closes.
 */
public class SocksProxyServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SocksProxyServer.class);
    private static final ThreadFactory THREAD_FACTORY = new DaemonThreadFactory();

    // Protocol constants (RFC 1928)
    private static final byte SOCKS_VERSION = 0x05;
    private static final byte SOCKS4_VERSION = 0x04;
    private static final byte AUTH_NONE = 0x00;
    private static final byte AUTH_NO_ACCEPTABLE = (byte) 0xFF;
    private static final byte CMD_CONNECT = 0x01;
    private static final byte ATYP_IPV4 = 0x01;
    private static final byte ATYP_DOMAIN = 0x03;
    private static final byte ATYP_IPV6 = 0x04;

    private static final byte REP_SUCCEEDED = 0x00;
    private static final byte REP_GENERAL_FAILURE = 0x01;
    private static final byte REP_NETWORK_UNREACHABLE = 0x03;
    private static final byte REP_HOST_UNREACHABLE = 0x04;
    private static final byte REP_CONNECTION_REFUSED = 0x05;
    private static final byte REP_TTL_EXPIRED = 0x06;
    private static final byte REP_COMMAND_NOT_SUPPORTED = 0x07;
    private static final byte REP_ADDRESS_TYPE_NOT_SUPPORTED = 0x08;

    // SOCKS4 reply codes (the SOCKS4 reply has a null version byte followed by one of these)
    private static final byte V4_REQUEST_GRANTED = 90;
    private static final byte V4_REQUEST_REJECTED = 91;

    // Session attributes
    private static final String STATE_KEY = SocksProxyServer.class.getName() + ".STATE";
    private static final String CLIENT_KEY = SocksProxyServer.class.getName() + ".CLIENT";

    // Simple back-pressure thresholds (bytes waiting to be written to a peer)
    private static final int HIGH_WATER_MARK = 1024 * 1024;
    private static final int LOW_WATER_MARK = 256 * 1024;
    private static final int MAX_HANDSHAKE_BYTES = 1024;
    private static final int CONNECT_TIMEOUT_MILLIS = 10_000;

    private int port;

    private ExecutorService executor;
    private SimpleIoProcessorPool<NioSession> processors;
    private NioSocketAcceptor acceptor;
    private NioSocketConnector connector;

    private boolean backPressureEnabled;

    public SocksProxyServer(int port) {
        this.port = port;
        this.backPressureEnabled = false;
    }

    public synchronized void start() {
        if (acceptor != null) {
            throw new IllegalStateException("SOCKS proxy server is running already");
        }

        executor = Executors.newCachedThreadPool(THREAD_FACTORY);
        processors = new SimpleIoProcessorPool<NioSession>(
                NioProcessor.class, executor, Runtime.getRuntime().availableProcessors() + 1, null);

        connector = new NioSocketConnector(processors);
        connector.setConnectTimeoutMillis(CONNECT_TIMEOUT_MILLIS);
        connector.getSessionConfig().setTcpNoDelay(true);
        connector.getSessionConfig().setReadBufferSize(16 * 1024);
        connector.setHandler(new RemoteHandler());

        acceptor = new NioSocketAcceptor(processors);
        acceptor.setReuseAddress(true);
        acceptor.getSessionConfig().setTcpNoDelay(true);
        acceptor.getSessionConfig().setReadBufferSize(16 * 1024);
        acceptor.setHandler(new ClientHandler());

        try {
            acceptor.bind(new InetSocketAddress(port));
        } catch (IOException e) {
            disposeResources();
            throw new RuntimeException("Failed to start SOCKS proxy server at port " + port, e);
        }

        // Support binding to an ephemeral port (port 0)
        InetSocketAddress local = acceptor.getLocalAddress();
        if (local != null) {
            port = local.getPort();
        }

        LOGGER.info("SOCKS proxy server started at port: {}", port);
    }

    public synchronized void stop() {
        if (acceptor == null) {
            throw new IllegalStateException("SOCKS proxy server is not running");
        }

        acceptor.unbind();
        disposeResources();

        LOGGER.info("SOCKS proxy server stopped at port {}", port);
    }

    public int getPort() {
        return port;
    }

    public InetSocketAddress getLocalAddress() {
        return acceptor.getLocalAddress();
    }

    private void disposeResources() {
        if (acceptor != null) {
            acceptor.dispose(true);
            acceptor = null;
        }
        if (connector != null) {
            connector.dispose(true);
            connector = null;
        }
        if (processors != null) {
            processors.dispose();
            processors = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Per-client state. All access is guarded by synchronized(state), because the client session and the outbound
    // session are processed by different I/O threads.
    // ------------------------------------------------------------------------------------------------------------

    private enum Phase {
        GREETING, REQUEST, CONNECTING, RELAY, CLOSED
    }

    private static final class ClientState {
        Phase phase = Phase.GREETING;
        /**
         * SOCKS protocol version (4 or 5), known once the first byte has been received.
         */
        int version = SOCKS_VERSION;
        final IoBuffer handshake = IoBuffer.allocate(512).setAutoExpand(true);
        /**
         * Client -> target bytes received before the outbound connection was established.
         */
        final Queue<IoBuffer> toRemote = new ArrayDeque<IoBuffer>();
        /**
         * Target -> client bytes received before the success reply was sent.
         */
        final Queue<IoBuffer> toClient = new ArrayDeque<IoBuffer>();
        IoSession remote;
    }

    private static ClientState stateOf(IoSession clientSession) {
        return (ClientState) clientSession.getAttribute(STATE_KEY);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Handler for sessions accepted from SOCKS clients
    // ------------------------------------------------------------------------------------------------------------

    private final class ClientHandler extends IoHandlerAdapter {

        @Override
        public void sessionCreated(IoSession session) {
            session.setAttribute(STATE_KEY, new ClientState());
        }

        @Override
        public void messageReceived(IoSession session, Object message) {
            IoBuffer in = (IoBuffer) message;
            ClientState state = stateOf(session);

            synchronized (state) {
                switch (state.phase) {
                    case GREETING:
                    case REQUEST:
                        state.handshake.put(in);
                        if (state.handshake.position() > MAX_HANDSHAKE_BYTES) {
                            closeNow(session, state);
                            return;
                        }
                        processHandshake(session, state);
                        break;
                    case CONNECTING:
                        // Client is pipelining data before we replied; hold it until the target is connected.
                        state.toRemote.add(in);
                        break;
                    case RELAY:
                        forward(session, state.remote, in, backPressureEnabled);
                        break;
                    default:
                        break; // closed: drop
                }
            }
        }

        @Override
        public void messageSent(IoSession session, Object message) {
            ClientState state = stateOf(session);
            IoSession peer;
            synchronized (state) {
                peer = state.remote;
            }
            resumePeerIfDrained(session, peer);
        }

        @Override
        public void sessionClosed(IoSession session) {
            ClientState state = stateOf(session);
            IoSession remote;
            synchronized (state) {
                state.phase = Phase.CLOSED;
                state.toRemote.clear();
                state.toClient.clear();
                remote = state.remote;
            }
            if (remote != null) {
                remote.closeOnFlush();
            }
        }

        @Override
        public void exceptionCaught(IoSession session, Throwable cause) {
            LOGGER.debug("SOCKS client session error: {}", session, cause);
            session.closeNow();
        }

        // --- handshake -----------------------------------------------------------------------------------------

        private void processHandshake(IoSession session, ClientState state) {
            boolean progress = true;
            while (progress) {
                IoBuffer buf = state.handshake;
                buf.flip();
                try {
                    if (state.phase == Phase.GREETING) {
                        progress = handleFirstBytes(session, state, buf);
                    } else if (state.phase == Phase.REQUEST) {
                        progress = handleRequest(session, state, buf);
                    } else {
                        progress = false;
                    }
                } finally {
                    buf.compact();
                }
            }
        }

        /**
         * Detects the protocol version from the first byte and dispatches.
         */
        private boolean handleFirstBytes(IoSession session, ClientState state, IoBuffer buf) {
            if (!buf.hasRemaining()) {
                return false;
            }
            byte first = buf.get(buf.position());
            if (first == SOCKS_VERSION) {
                state.version = SOCKS_VERSION;
                return handleGreeting(session, state, buf);
            }
            if (first == SOCKS4_VERSION) {
                state.version = SOCKS4_VERSION;
                return handleSocks4Request(session, state, buf);
            }
            closeNow(session, state);
            return false;
        }

        /**
         * @return true if the greeting was consumed and the next phase can be processed right away.
         */
        private boolean handleGreeting(IoSession session, ClientState state, IoBuffer buf) {
            if (buf.remaining() < 2) {
                return false;
            }
            int p = buf.position();
            if (buf.get(p) != SOCKS_VERSION) {
                closeNow(session, state);
                return false;
            }
            int methodCount = buf.get(p + 1) & 0xFF;
            if (buf.remaining() < 2 + methodCount) {
                return false;
            }

            boolean noAuthOffered = false;
            for (int i = 0; i < methodCount; i++) {
                if (buf.get(p + 2 + i) == AUTH_NONE) {
                    noAuthOffered = true;
                    break;
                }
            }
            buf.position(p + 2 + methodCount);

            if (!noAuthOffered) {
                state.phase = Phase.CLOSED;
                session.write(IoBuffer.wrap(new byte[]{SOCKS_VERSION, AUTH_NO_ACCEPTABLE}));
                session.closeOnFlush();
                return false;
            }

            session.write(IoBuffer.wrap(new byte[]{SOCKS_VERSION, AUTH_NONE}));
            state.phase = Phase.REQUEST;
            return true;
        }

        /**
         * @return always false: after a complete request the session leaves the handshake phase.
         */
        private boolean handleRequest(IoSession session, ClientState state, IoBuffer buf) {
            if (buf.remaining() < 4) {
                return false;
            }
            int p = buf.position();
            byte version = buf.get(p);
            byte command = buf.get(p + 1);
            byte addressType = buf.get(p + 3);

            if (version != SOCKS_VERSION) {
                closeNow(session, state);
                return false;
            }

            int addressLength;
            switch (addressType) {
                case ATYP_IPV4:
                    addressLength = 4;
                    break;
                case ATYP_IPV6:
                    addressLength = 16;
                    break;
                case ATYP_DOMAIN:
                    if (buf.remaining() < 5) {
                        return false;
                    }
                    addressLength = 1 + (buf.get(p + 4) & 0xFF);
                    break;
                default:
                    replyAndClose(session, state, REP_ADDRESS_TYPE_NOT_SUPPORTED);
                    return false;
            }

            int total = 4 + addressLength + 2;
            if (buf.remaining() < total) {
                return false;
            }

            final String domain;
            final byte[] ip;
            if (addressType == ATYP_DOMAIN) {
                int len = addressLength - 1;
                byte[] name = new byte[len];
                for (int i = 0; i < len; i++) {
                    name[i] = buf.get(p + 5 + i);
                }
                domain = new String(name, StandardCharsets.UTF_8);
                ip = null;
            } else {
                ip = new byte[addressLength];
                for (int i = 0; i < addressLength; i++) {
                    ip[i] = buf.get(p + 4 + i);
                }
                domain = null;
            }
            final int targetPort = ((buf.get(p + 4 + addressLength) & 0xFF) << 8)
                    | (buf.get(p + 5 + addressLength) & 0xFF);

            return finishRequest(session, state, buf, p + total, command, domain, ip, targetPort);
        }

        /**
         * SOCKS4 / SOCKS4a request: VN(1) CD(1) DSTPORT(2) DSTIP(4) USERID(variable) NUL [HOSTNAME(variable) NUL, only
         * for 4a when DSTIP is 0.0.0.x with x != 0].
         *
         * @return always false: after a complete request the session leaves the handshake phase.
         */
        private boolean handleSocks4Request(IoSession session, ClientState state, IoBuffer buf) {
            if (buf.remaining() < 9) { // VN CD PORT(2) IP(4) + at least the user id terminator
                return false;
            }
            int p = buf.position();
            int limit = buf.limit();

            byte command = buf.get(p + 1);
            int targetPort = ((buf.get(p + 2) & 0xFF) << 8) | (buf.get(p + 3) & 0xFF);
            byte[] ip = new byte[4];
            for (int i = 0; i < 4; i++) {
                ip[i] = buf.get(p + 4 + i);
            }

            // User id (ignored): wait until its NUL terminator has arrived.
            int idx = p + 8;
            while (idx < limit && buf.get(idx) != 0) {
                idx++;
            }
            if (idx >= limit) {
                return false;
            }
            int end = idx + 1;

            String domain = null;
            boolean socks4a = ip[0] == 0 && ip[1] == 0 && ip[2] == 0 && ip[3] != 0;
            if (socks4a) {
                int h = end;
                while (h < limit && buf.get(h) != 0) {
                    h++;
                }
                if (h >= limit) {
                    return false; // host name not complete yet
                }
                byte[] name = new byte[h - end];
                for (int i = 0; i < name.length; i++) {
                    name[i] = buf.get(end + i);
                }
                domain = new String(name, StandardCharsets.UTF_8);
                ip = null;
                end = h + 1;
                if (domain.isEmpty()) {
                    buf.position(end);
                    replyAndClose(session, state, REP_GENERAL_FAILURE);
                    return false;
                }
            }

            return finishRequest(session, state, buf, end, command, domain, ip, targetPort);
        }

        /**
         * Common tail of SOCKS4 and SOCKS5 request handling.
         */
        private boolean finishRequest(IoSession session, ClientState state, IoBuffer buf, int endOfRequest,
                byte command, String domain, byte[] ip, int targetPort) {
            buf.position(endOfRequest);

            if (command != CMD_CONNECT) {
                replyAndClose(session, state, REP_COMMAND_NOT_SUPPORTED);
                return false;
            }

            // Anything after the request belongs to the tunnel; keep it for later.
            if (buf.hasRemaining()) {
                byte[] rest = new byte[buf.remaining()];
                buf.get(rest);
                state.toRemote.add(IoBuffer.wrap(rest));
            }

            state.phase = Phase.CONNECTING;
            connectToTarget(session, state, domain, ip, targetPort);
            return false;
        }

        private void connectToTarget(final IoSession client, final ClientState state,
                final String domain, final byte[] ip, final int targetPort) {
            // Name resolution can block, so keep it off the I/O threads.
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    final InetSocketAddress target;
                    try {
                        target = domain != null
                                ? new InetSocketAddress(domain, targetPort)
                                : new InetSocketAddress(InetAddress.getByAddress(ip), targetPort);
                    } catch (Exception e) {
                        replyAndClose(client, state, REP_HOST_UNREACHABLE);
                        return;
                    }
                    if (target.isUnresolved()) {
                        replyAndClose(client, state, REP_HOST_UNREACHABLE);
                        return;
                    }

                    ConnectFuture future;
                    try {
                        future = connector.connect(target, new IoSessionInitializer<ConnectFuture>() {
                            @Override
                            public void initializeSession(IoSession remote, ConnectFuture f) {
                                // Set before the session starts reading so the remote handler can find its client.
                                remote.setAttribute(CLIENT_KEY, client);
                            }
                        });
                    } catch (RuntimeException e) { // e.g. connector already disposed
                        replyAndClose(client, state, REP_GENERAL_FAILURE);
                        return;
                    }

                    future.addListener(new IoFutureListener<ConnectFuture>() {
                        @Override
                        public void operationComplete(ConnectFuture f) {
                            onTargetConnected(client, state, f);
                        }
                    });
                }
            });
        }

        private void onTargetConnected(IoSession client, ClientState state, ConnectFuture future) {
            synchronized (state) {
                if (!future.isConnected()) {
                    replyAndClose(client, state, replyCodeFor(future.getException()));
                    return;
                }

                IoSession remote = future.getSession();
                if (state.phase == Phase.CLOSED) { // client went away while we were connecting
                    remote.closeNow();
                    return;
                }

                state.remote = remote;
                client.write(buildReply(state.version, REP_SUCCEEDED, remote.getLocalAddress()));
                state.phase = Phase.RELAY;

                IoBuffer b;
                while ((b = state.toRemote.poll()) != null) {
                    forward(client, remote, b, backPressureEnabled);
                }
                while ((b = state.toClient.poll()) != null) {
                    forward(remote, client, b, backPressureEnabled);
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Handler for outbound sessions (proxy -> target)
    // ------------------------------------------------------------------------------------------------------------

    private final class RemoteHandler extends IoHandlerAdapter {

        @Override
        public void messageReceived(IoSession session, Object message) {
            IoBuffer in = (IoBuffer) message;
            IoSession client = (IoSession) session.getAttribute(CLIENT_KEY);
            if (client == null) {
                session.closeNow();
                return;
            }
            ClientState state = stateOf(client);

            synchronized (state) {
                switch (state.phase) {
                    case RELAY:
                        forward(session, client, in, backPressureEnabled);
                        break;
                    case CONNECTING:
                        // Target spoke before the success reply was written: preserve ordering.
                        state.toClient.add(in);
                        break;
                    default:
                        break;
                }
            }
        }

        @Override
        public void messageSent(IoSession session, Object message) {
            resumePeerIfDrained(session, (IoSession) session.getAttribute(CLIENT_KEY));
        }

        @Override
        public void sessionClosed(IoSession session) {
            IoSession client = (IoSession) session.getAttribute(CLIENT_KEY);
            if (client != null) {
                client.closeOnFlush();
            }
        }

        @Override
        public void exceptionCaught(IoSession session, Throwable cause) {
            LOGGER.debug("SOCKS target session error: {}", session, cause);
            session.closeNow();
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------------------------------------------

    private static void forward(IoSession source, IoSession target, IoBuffer data, boolean backPressureEnabled) {
        target.write(data);

        if (backPressureEnabled && target.getScheduledWriteBytes() > HIGH_WATER_MARK) {
            source.suspendRead();
        }
    }

    private static void resumePeerIfDrained(IoSession writer, IoSession peer) {
        if (peer != null && peer.isReadSuspended() && writer.getScheduledWriteBytes() <= LOW_WATER_MARK) {
            peer.resumeRead();
        }
    }

    private static void closeNow(IoSession session, ClientState state) {
        state.phase = Phase.CLOSED;
        session.closeNow();
    }

    private static void replyAndClose(IoSession client, ClientState state, byte replyCode) {
        int version;
        synchronized (state) {
            state.phase = Phase.CLOSED;
            state.toRemote.clear();
            state.toClient.clear();
            version = state.version;
        }
        client.write(buildReply(version, replyCode, null));
        client.closeOnFlush();
    }

    private static IoBuffer buildReply(int version, byte replyCode, SocketAddress bound) {
        if (version == SOCKS4_VERSION) {
            return buildSocks4Reply(replyCode, bound);
        }
        byte[] address = new byte[4]; // 0.0.0.0
        int boundPort = 0;
        if (bound instanceof InetSocketAddress) {
            InetSocketAddress inet = (InetSocketAddress) bound;
            if (inet.getAddress() != null) {
                address = inet.getAddress().getAddress();
            }
            boundPort = inet.getPort();
        }

        IoBuffer reply = IoBuffer.allocate(6 + address.length);
        reply.put(SOCKS_VERSION);
        reply.put(replyCode);
        reply.put((byte) 0x00); // reserved
        reply.put(address.length == 4 ? ATYP_IPV4 : ATYP_IPV6);
        reply.put(address);
        reply.putShort((short) boundPort);
        reply.flip();
        return reply;
    }

    /**
     * SOCKS4 reply: VN=0, CD (90 granted / 91 rejected or failed), DSTPORT(2), DSTIP(4).
     */
    private static IoBuffer buildSocks4Reply(byte socks5ReplyCode, SocketAddress bound) {
        byte[] address = new byte[4];
        int boundPort = 0;
        if (bound instanceof InetSocketAddress) {
            InetSocketAddress inet = (InetSocketAddress) bound;
            boundPort = inet.getPort();
            if (inet.getAddress() != null && inet.getAddress().getAddress().length == 4) {
                address = inet.getAddress().getAddress();
            }
        }
        IoBuffer reply = IoBuffer.allocate(8);
        reply.put((byte) 0x00);
        reply.put(socks5ReplyCode == REP_SUCCEEDED ? V4_REQUEST_GRANTED : V4_REQUEST_REJECTED);
        reply.putShort((short) boundPort);
        reply.put(address);
        reply.flip();
        return reply;
    }

    private static byte replyCodeFor(Throwable t) {
        if (t == null) {
            return REP_GENERAL_FAILURE;
        }
        String message = String.valueOf(t.getMessage()).toLowerCase();
        if (t instanceof SocketTimeoutException || message.contains("timed out")) {
            return REP_TTL_EXPIRED;
        }
        if (t instanceof NoRouteToHostException || t instanceof UnknownHostException
                || t instanceof UnresolvedAddressException) {
            return REP_HOST_UNREACHABLE;
        }
        if (message.contains("network is unreachable")) {
            return REP_NETWORK_UNREACHABLE;
        }
        if (t instanceof ConnectException) {
            return REP_CONNECTION_REFUSED;
        }
        return REP_GENERAL_FAILURE;
    }
}