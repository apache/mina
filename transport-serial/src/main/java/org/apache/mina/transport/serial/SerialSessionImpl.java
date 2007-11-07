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
package org.apache.mina.transport.serial;

import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TooManyListenersException;

import org.apache.mina.common.AbstractIoSession;
import org.apache.mina.common.IoBuffer;
import org.apache.mina.common.DefaultIoFilterChain;
import org.apache.mina.common.DefaultTransportMetadata;
import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.common.IdleStatusChecker;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoProcessor;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.TransportMetadata;
import org.apache.mina.common.WriteRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An imlpementation of {@link SerialSession}.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
class SerialSessionImpl extends AbstractIoSession implements
        SerialSession, SerialPortEventListener, IoProcessor {

    private SerialSessionConfig config = new DefaultSerialSessionConfig();

    private final IoHandler ioHandler;

    private final IoFilterChain filterChain;

    private final IoService service;

    private final SerialAddress address;

    private InputStream inputStream;

    private OutputStream outputStream;

    private final SerialPort port;

    private final Logger log;

    static final TransportMetadata METADATA =
            new DefaultTransportMetadata(
                    "serial", false, true, SerialAddress.class,
                    SerialSessionConfig.class, IoBuffer.class);

    SerialSessionImpl(IoService service, SerialAddress address, SerialPort port) {
        this.service = service;
        this.ioHandler = service.getHandler();
        this.filterChain = new DefaultIoFilterChain(this);
        this.port = port;
        this.address = address;

        log = LoggerFactory.getLogger(SerialSessionImpl.class);
    }

    public SerialSessionConfig getConfig() {
        return config;
    }

    public IoFilterChain getFilterChain() {
        return filterChain;
    }

    public IoHandler getHandler() {
        return ioHandler;
    }

    public TransportMetadata getTransportMetadata() {
        return METADATA;
    }

    public SerialAddress getLocalAddress() {
        return null; // not applicable
    }

    public SerialAddress getRemoteAddress() {
        return address;
    }

    @Override
    public SerialAddress getServiceAddress() {
        return (SerialAddress) super.getServiceAddress();
    }

    public IoService getService() {
        return service;
    }

    /**
     * start handling streams
     *
     * @throws IOException
     * @throws TooManyListenersException
     */
    void start() throws IOException, TooManyListenersException {
        inputStream = port.getInputStream();
        outputStream = port.getOutputStream();
        ReadWorker w = new ReadWorker();
        w.start();
        port.addEventListener(this);
        IdleStatusChecker.getInstance().addSession(this);
        ((SerialConnector) getService()).getListeners()
                .fireSessionCreated(this);
    }

    private Object writeMonitor = new Object();

    private WriteWorker writeWorker;

    private class WriteWorker extends Thread {
        @Override
        public void run() {
            while (isConnected() && !isClosing()) {
                flushWrites();

                // wait for more data
                synchronized (writeMonitor) {
                    try {
                        writeMonitor.wait();
                    } catch (InterruptedException e) {
                        log.error("InterruptedException", e);
                    }
                }
            }
        }
    }

    private void flushWrites() {
        for (; ;) {
            WriteRequest req = getCurrentWriteRequest();
            if (req == null) {
                req = getWriteRequestQueue().poll(this);
                if (req == null) {
                    break;
                }
            }

            IoBuffer buf = (IoBuffer) req.getMessage();
            if (buf.remaining() == 0) {
                setCurrentWriteRequest(null);
                buf.reset();

                this.getFilterChain().fireMessageSent(req);
                continue;
            }

            int writtenBytes = buf.remaining();
            try {
                outputStream.write(buf.array());
                buf.position(buf.position() + writtenBytes);
            } catch (IOException e) {
                this.getFilterChain().fireExceptionCaught(e);
            }
        }
    }

    private Object readReadyMonitor = new Object();

    private class ReadWorker extends Thread {
        @Override
        public void run() {
            while (isConnected() && !isClosing()) {
                synchronized (readReadyMonitor) {
                    try {
                        readReadyMonitor.wait();
                    } catch (InterruptedException e) {
                        log.error("InterruptedException", e);
                    }
                    if (isClosing() || !isConnected()) {
                        break;
                    }
                    int dataSize;
                    try {
                        dataSize = inputStream.available();
                        byte[] data = new byte[dataSize];
                        int readBytes = inputStream.read(data);

                        if (readBytes > 0) {
                            IoBuffer buf = IoBuffer
                                    .wrap(data, 0, readBytes);
                            buf.put(data, 0, readBytes);
                            buf.flip();
                            getFilterChain().fireMessageReceived(
                                    buf);
                        }
                    } catch (IOException e) {
                        getFilterChain().fireExceptionCaught(
                                e);
                    }
                }
            }
        }
    }

    public void serialEvent(SerialPortEvent evt) {
        if (evt.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
            synchronized (readReadyMonitor) {
                readReadyMonitor.notifyAll();
            }
        }
    }

    @Override
    protected IoProcessor getProcessor() {
        return this;
    }

    public void add(IoSession session) {
    }

    public void flush(IoSession session) {
        if (writeWorker == null) {
            writeWorker = new WriteWorker();
            writeWorker.start();
        } else {
            synchronized (writeMonitor) {
                writeMonitor.notifyAll();
            }
        }
    }

    public void remove(IoSession session) {
        try {
            inputStream.close();
        } catch (IOException e) {
            ExceptionMonitor.getInstance().exceptionCaught(e);
        }
        try {
            outputStream.close();
        } catch (IOException e) {
            ExceptionMonitor.getInstance().exceptionCaught(e);
        }

        port.close();
        flush(session);
        synchronized (readReadyMonitor) {
            readReadyMonitor.notifyAll();
        }

        ((SerialConnector) getService()).getListeners().fireSessionDestroyed(
                this);
    }

    public void updateTrafficMask(IoSession session) {
        throw new UnsupportedOperationException();
    }
}
