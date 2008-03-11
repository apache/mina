/**
 * 
 */
package org.apache.mina.transport.socket.nio;

import java.net.SocketException;
import java.nio.channels.DatagramChannel;

import org.apache.mina.common.RuntimeIoException;
import org.apache.mina.transport.socket.AbstractDatagramSessionConfig;
import org.apache.mina.transport.socket.DefaultDatagramSessionConfig;

class NioDatagramSessionConfig extends AbstractDatagramSessionConfig {
    private final DatagramChannel c;

    NioDatagramSessionConfig(DatagramChannel c) {
        this.c = c;
    }

    public int getReceiveBufferSize() {
        try {
            return c.socket().getReceiveBufferSize();
        } catch (SocketException e) {
            throw new RuntimeIoException(e);
        }
    }

    public void setReceiveBufferSize(int receiveBufferSize) {
        if (DefaultDatagramSessionConfig.isSetReceiveBufferSizeAvailable()) {
            try {
                c.socket().setReceiveBufferSize(receiveBufferSize);
                // Re-retrieve the effective receive buffer size.
                receiveBufferSize = c.socket().getReceiveBufferSize();
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }
    }

    public boolean isBroadcast() {
        try {
            return c.socket().getBroadcast();
        } catch (SocketException e) {
            throw new RuntimeIoException(e);
        }
    }

    public void setBroadcast(boolean broadcast) {
        try {
            c.socket().setBroadcast(broadcast);
        } catch (SocketException e) {
            throw new RuntimeIoException(e);
        }
    }

    public int getSendBufferSize() {
        try {
            return c.socket().getSendBufferSize();
        } catch (SocketException e) {
            throw new RuntimeIoException(e);
        }
    }

    public void setSendBufferSize(int sendBufferSize) {
        if (DefaultDatagramSessionConfig.isSetSendBufferSizeAvailable()) {
            try {
                c.socket().setSendBufferSize(sendBufferSize);
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }
    }

    public boolean isReuseAddress() {
        try {
            return c.socket().getReuseAddress();
        } catch (SocketException e) {
            throw new RuntimeIoException(e);
        }
    }

    public void setReuseAddress(boolean reuseAddress) {
        try {
            c.socket().setReuseAddress(reuseAddress);
        } catch (SocketException e) {
            throw new RuntimeIoException(e);
        }
    }

    public int getTrafficClass() {
        if (DefaultDatagramSessionConfig.isGetTrafficClassAvailable()) {
            try {
                return c.socket().getTrafficClass();
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        } else {
            return 0;
        }
    }

    public void setTrafficClass(int trafficClass) {
        if (DefaultDatagramSessionConfig.isSetTrafficClassAvailable()) {
            try {
                c.socket().setTrafficClass(trafficClass);
            } catch (SocketException e) {
                throw new RuntimeIoException(e);
            }
        }
    }
}