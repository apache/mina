/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.mina.transport.nio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.net.Socket;
import java.net.SocketException;

import org.apache.mina.api.ConfigurationException;
import org.apache.mina.api.IdleStatus;
import org.apache.mina.transport.tcp.ProxyTcpSessionConfig;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit test for {@link ProxyTcpSessionConfig}
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ProxySocketSessionConfigTest {

    private ProxyTcpSessionConfig config;

    private Socket socket;

    @Before
    public void setup() throws Exception {
        socket = Mockito.mock(Socket.class);
        config = new ProxyTcpSessionConfig(socket);
    }

    @Test
    public void idle() {
        assertEquals(-1, config.getIdleTimeInMillis(IdleStatus.READ_IDLE));
        assertEquals(-1, config.getIdleTimeInMillis(IdleStatus.WRITE_IDLE));

        config.setIdleTimeInMillis(IdleStatus.READ_IDLE, 1);
        assertEquals(1, config.getIdleTimeInMillis(IdleStatus.READ_IDLE));
        assertEquals(-1, config.getIdleTimeInMillis(IdleStatus.WRITE_IDLE));

        assertEquals(1, config.getIdleTimeInMillis(IdleStatus.READ_IDLE));
        assertEquals(-1, config.getIdleTimeInMillis(IdleStatus.WRITE_IDLE));

        config.setIdleTimeInMillis(IdleStatus.WRITE_IDLE, 3);
        assertEquals(1, config.getIdleTimeInMillis(IdleStatus.READ_IDLE));
        assertEquals(3, config.getIdleTimeInMillis(IdleStatus.WRITE_IDLE));
    }

    @Test
    public void tcpNoDelay() throws SocketException {
        when(socket.getTcpNoDelay()).thenReturn(true);
        assertTrue(config.isTcpNoDelay());
        verify(socket).getTcpNoDelay();
        verifyNoMoreInteractions(socket);

        config.setTcpNoDelay(true);
        verify(socket).setTcpNoDelay(eq(true));
        verifyNoMoreInteractions(socket);

        // handle error
        when(socket.getTcpNoDelay()).thenThrow(new SocketException("test"));
        try {
            config.isTcpNoDelay();
            fail();
        } catch (ConfigurationException e) {
            assertEquals("test", e.getCause().getMessage());
        }

        // handle error
        doThrow(new SocketException("test")).when(socket).setTcpNoDelay(eq(true));
        try {
            config.setTcpNoDelay(true);
            fail();
        } catch (ConfigurationException e) {
            assertEquals("test", e.getCause().getMessage());
        }

    }

    @Test
    public void reuseAddress() throws SocketException {
        when(socket.getReuseAddress()).thenReturn(true);
        assertTrue(config.isReuseAddress());
        verify(socket).getReuseAddress();
        verifyNoMoreInteractions(socket);

        config.setReuseAddress(true);
        verify(socket).setReuseAddress(eq(true));
        verifyNoMoreInteractions(socket);

        // handle error
        when(socket.getReuseAddress()).thenThrow(new SocketException("test"));
        try {
            config.isReuseAddress();
            fail();
        } catch (ConfigurationException e) {
            assertEquals("test", e.getCause().getMessage());
        }

        // handle error
        doThrow(new SocketException("test")).when(socket).setReuseAddress(eq(true));
        try {
            config.setReuseAddress(true);
            fail();
        } catch (ConfigurationException e) {
            assertEquals("test", e.getCause().getMessage());
        }
    }

    @Test
    public void receiveBufferSize() throws SocketException {
        when(socket.getReceiveBufferSize()).thenReturn(1234);
        assertEquals(1234, config.getReadBufferSize().intValue());
        verify(socket).getReceiveBufferSize();
        verifyNoMoreInteractions(socket);

        config.setReadBufferSize(1234);
        verify(socket).setReceiveBufferSize(eq(1234));
        verifyNoMoreInteractions(socket);

        // handle error
        when(socket.getReceiveBufferSize()).thenThrow(new SocketException("test"));
        try {
            config.getReadBufferSize();
            fail();
        } catch (ConfigurationException e) {
            assertEquals("test", e.getCause().getMessage());
        }

        // handle error
        doThrow(new SocketException("test")).when(socket).setReceiveBufferSize(eq(1234));
        try {
            config.setReadBufferSize(1234);
            fail();
        } catch (ConfigurationException e) {
            assertEquals("test", e.getCause().getMessage());
        }

    }

    @Test
    public void sendBufferSize() throws SocketException {
        when(socket.getSendBufferSize()).thenReturn(1234);
        assertEquals(1234, config.getSendBufferSize().intValue());
        verify(socket).getSendBufferSize();
        verifyNoMoreInteractions(socket);

        config.setSendBufferSize(1234);
        verify(socket).setSendBufferSize(eq(1234));
        verifyNoMoreInteractions(socket);

        // handle error
        when(socket.getSendBufferSize()).thenThrow(new SocketException("test"));
        try {
            config.getSendBufferSize();
            fail();
        } catch (ConfigurationException e) {
            assertEquals("test", e.getCause().getMessage());
        }

        // handle error
        doThrow(new SocketException("test")).when(socket).setSendBufferSize(eq(1234));
        try {
            config.setSendBufferSize(1234);
            fail();
        } catch (ConfigurationException e) {
            assertEquals("test", e.getCause().getMessage());
        }
    }

    @Test
    public void trafficClass() throws SocketException {
        when(socket.getTrafficClass()).thenReturn(1234);
        assertEquals(1234, config.getTrafficClass());
        verify(socket).getTrafficClass();
        verifyNoMoreInteractions(socket);

        config.setTrafficClass(1234);
        verify(socket).setTrafficClass(eq(1234));
        verifyNoMoreInteractions(socket);

        // handle error
        when(socket.getTrafficClass()).thenThrow(new SocketException("test"));
        try {
            config.getTrafficClass();
            fail();
        } catch (ConfigurationException e) {
            assertEquals("test", e.getCause().getMessage());
        }

        // handle error
        doThrow(new SocketException("test")).when(socket).setTrafficClass(eq(1234));
        try {
            config.setTrafficClass(1234);
            fail();
        } catch (ConfigurationException e) {
            assertEquals("test", e.getCause().getMessage());
        }
    }

    @Test
    public void keepAlive() throws SocketException {
        when(socket.getKeepAlive()).thenReturn(true);
        assertTrue(config.isKeepAlive());
        verify(socket).getKeepAlive();
        verifyNoMoreInteractions(socket);

        config.setKeepAlive(true);
        verify(socket).setKeepAlive(eq(true));
        verifyNoMoreInteractions(socket);

        // handle error
        when(socket.getKeepAlive()).thenThrow(new SocketException("test"));
        try {
            config.isKeepAlive();
            fail();
        } catch (ConfigurationException e) {
            assertEquals("test", e.getCause().getMessage());
        }

        // handle error
        doThrow(new SocketException("test")).when(socket).setKeepAlive(eq(true));
        try {
            config.setKeepAlive(true);
            fail();
        } catch (ConfigurationException e) {
            assertEquals("test", e.getCause().getMessage());
        }
    }

    @Test
    public void oobInline() throws SocketException {
        when(socket.getOOBInline()).thenReturn(true);
        assertTrue(config.isOobInline());
        verify(socket).getOOBInline();
        verifyNoMoreInteractions(socket);

        config.setOobInline(true);
        verify(socket).setOOBInline(eq(true));
        verifyNoMoreInteractions(socket);

        // handle error
        when(socket.getOOBInline()).thenThrow(new SocketException("test"));
        try {
            config.isOobInline();
            fail();
        } catch (ConfigurationException e) {
            assertEquals("test", e.getCause().getMessage());
        }

        // handle error
        doThrow(new SocketException("test")).when(socket).setOOBInline(eq(true));
        try {
            config.setOobInline(true);
            fail();
        } catch (ConfigurationException e) {
            assertEquals("test", e.getCause().getMessage());
        }
    }

    @Test
    public void soLinger() throws SocketException {
        when(socket.getSoLinger()).thenReturn(1234);
        assertEquals(1234, config.getSoLinger().intValue());
        verify(socket).getSoLinger();
        verifyNoMoreInteractions(socket);

        config.setSoLinger(1234);
        verify(socket).setSoLinger(eq(true), eq(1234));
        verifyNoMoreInteractions(socket);

        config.setSoLinger(-1234);
        verify(socket).setSoLinger(eq(false), anyInt());
        verifyNoMoreInteractions(socket);

        // handle error
        when(socket.getSoLinger()).thenThrow(new SocketException("test"));
        try {
            config.getSoLinger();
            fail();
        } catch (ConfigurationException e) {
            assertEquals("test", e.getCause().getMessage());
        }

        // handle error
        doThrow(new SocketException("test")).when(socket).setSoLinger(eq(true), eq(1234));
        try {
            config.setSoLinger(1234);
            fail();
        } catch (ConfigurationException e) {
            assertEquals("test", e.getCause().getMessage());
        }
    }

}