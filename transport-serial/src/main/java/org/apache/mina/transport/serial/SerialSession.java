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
import java.net.SocketAddress;
import java.util.LinkedList;
import java.util.Queue;
import java.util.TooManyListenersException;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoSessionConfig;
import org.apache.mina.common.TransportType;
import org.apache.mina.common.WriteRequest;
import org.apache.mina.common.support.BaseIoSession;
import org.apache.mina.common.support.DefaultTransportType;
import org.apache.mina.common.support.SessionIdleStatusChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link IoSession} for serial communication transport.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev: 529590 $, $Date: 2007-04-17 15:14:17 +0200 (mar., 17 avr. 2007) $
 */
public class SerialSession extends BaseIoSession implements
        SerialPortEventListener {

    // TODO make immutable fields 'final'.

    private SerialSessionConfig config;

    private IoHandler ioHandler;

    private IoFilterChain filterChain;

    private IoService service;

    private SerialAddress address;

    private final Queue<WriteRequest> writeRequestQueue;

    private InputStream inputStream;

    private OutputStream outputStream;

    private SerialPort port;

    private Logger log;

    public static final TransportType serialTransportType = new DefaultTransportType(
            "serial communication", false, SerialAddress.class,
            ByteBuffer.class, SerialSessionConfig.class);

    SerialSession(IoService service, SerialAddress address, SerialPort port) {
        this.service = service;
        this.ioHandler = service.getHandler();
        this.filterChain = new SerialFilterChain(this);
        this.writeRequestQueue = new LinkedList<WriteRequest>();
        this.port = port;
        this.address = address;

        log = LoggerFactory.getLogger(SerialSession.class);
    }

    @Override
    protected void updateTrafficMask() {
        throw new UnsupportedOperationException();
    }

    public IoSessionConfig getConfig() {
        return config;
    }

    public IoFilterChain getFilterChain() {
        return filterChain;
    }

    public IoHandler getHandler() {
        return ioHandler;
    }

    public SocketAddress getLocalAddress() {
        return null; // not applicable
    }

    public SocketAddress getRemoteAddress() {
        return address;
    }

    Queue<WriteRequest> getWriteRequestQueue() {
        return writeRequestQueue;
    }

    public int getScheduledWriteMessages() {
        synchronized (writeRequestQueue) {
            return writeRequestQueue.size();
        }
    }

    public int getScheduledWriteBytes() {
        int size = 0;
        synchronized (writeRequestQueue) {
            for (Object o : writeRequestQueue) {
                if (o instanceof ByteBuffer) {
                    size += ((ByteBuffer) o).remaining();
                }
            }
        }
        return size;
    }

    public IoService getService() {
        return service;
    }

    public TransportType getTransportType() {
        return serialTransportType;
    }

    protected void close0() {
        filterChain.fireFilterClose(this);
    }

    protected void write0(WriteRequest writeRequest) {
        filterChain.fireFilterWrite(this, writeRequest);
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
        SessionIdleStatusChecker.getInstance().addSession(this);
        ((SerialConnector) getService()).getListeners()
                .fireSessionCreated(this);
    }

    private Object writeMonitor = new Object();

    private WriteWorker writeWorker;

    private class WriteWorker extends Thread {
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
        for (;;) {
            WriteRequest req;

            synchronized (writeRequestQueue) {
                req = writeRequestQueue.peek();
            }

            if (req == null)
                break;

            ByteBuffer buf = (ByteBuffer) req.getMessage();
            if (buf.remaining() == 0) {
                synchronized (writeRequestQueue) {
                    writeRequestQueue.poll();
                }
                this.increaseWrittenMessages();

                buf.reset();

                this.getFilterChain().fireMessageSent(this, req);
                continue;
            }

            int writtenBytes = buf.remaining();
            try {
                outputStream.write(buf.array());
                buf.position(buf.position() + writtenBytes);
                this.increaseWrittenBytes(writtenBytes);
            } catch (IOException e) {
                this.getFilterChain().fireExceptionCaught(this, e);
            }
        }
    }

    void notifyWriteWorker() {
        if (writeWorker == null) {
            writeWorker = new WriteWorker();
            writeWorker.start();
        } else {
            synchronized (writeMonitor) {
                writeMonitor.notifyAll();
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
                    if (isClosing() || !isConnected())
                        break;
                    int dataSize;
                    try {
                        dataSize = inputStream.available();
                        byte[] data = new byte[dataSize];
                        int readBytes = inputStream.read(data);

                        if (readBytes > 0) {
                            increaseReadBytes(readBytes);
                            // TODO : check if it's the good allocation way
                            ByteBuffer buf = ByteBuffer.allocate(readBytes);
                            buf.put(data, 0, readBytes);
                            buf.flip();
                            getFilterChain().fireMessageReceived(
                                    SerialSession.this, buf);
                        }
                    } catch (IOException e) {
                        getFilterChain().fireExceptionCaught(
                                SerialSession.this, e);
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

    public void closeSerialPort() {
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
        notifyWriteWorker();
        synchronized (readReadyMonitor) {
            readReadyMonitor.notifyAll();
        }

        ((SerialConnector) getService()).getListeners().fireSessionDestroyed(
                this);
    }
}