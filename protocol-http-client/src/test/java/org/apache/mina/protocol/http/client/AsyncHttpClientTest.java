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
import java.io.FileInputStream;
import java.net.URL;
import java.util.Arrays;

import org.apache.mina.filter.codec.http.HttpRequestMessage;
import org.apache.mina.filter.codec.http.HttpResponseMessage;
import org.apache.mina.protocol.http.client.AsyncHttpClient;
import org.apache.mina.protocol.http.client.AsyncHttpClientCallback;

public class AsyncHttpClientTest extends AbstractTest {

    protected static final Object semaphore = new Object();

    public void testHtmlConnection() throws Exception {
        TestCallback callback = new TestCallback();
        doGetConnection(callback, "http://localhost:8282", "/", false);

        HttpResponseMessage msg = callback.getMessage();
        assertEquals("\nHello World!", msg.getStringContent());
    }

    public void testSSLHtmlConnection() throws Exception {
        TestCallback callback = new TestCallback();
        doGetConnection(callback, "https://localhost:8383", "/", false);

        HttpResponseMessage msg = callback.getMessage();
        assertEquals("\nHello World!", msg.getStringContent());
    }

    public void testBinaryRequest() throws Exception {

        //Get the real file
        File file = new File(ROOT, "pwrd_apache.gif");
        FileInputStream fis = new FileInputStream(file);
        byte realFile[] = new byte[(int) file.length()];
        fis.read(realFile);

        TestCallback callback = new TestCallback();
        doGetConnection(callback, "http://localhost:8282", "/pwrd_apache.gif",
                false);

        HttpResponseMessage msg = callback.getMessage();

        assertTrue(Arrays.equals(realFile, msg.getContent()));
    }

    public void testSSLBinaryRequest() throws Exception {

        //Get the real file
        File file = new File(ROOT, "pwrd_apache.gif");
        FileInputStream fis = new FileInputStream(file);
        byte realFile[] = new byte[(int) file.length()];
        fis.read(realFile);

        TestCallback callback = new TestCallback();
        doGetConnection(callback, "https://localhost:8383", "/pwrd_apache.gif",
                false);

        HttpResponseMessage msg = callback.getMessage();

        assertTrue(Arrays.equals(realFile, msg.getContent()));
    }

    public void testGetParameters() throws Exception {
        TestCallback callback = new TestCallback();
        doGetConnection(callback, "http://localhost:8282", "/params.jsp", false);

        HttpResponseMessage msg = callback.getMessage();
        assertEquals("Test One Test Two", msg.getStringContent());
    }

    public void testPostParameters() throws Exception {
        TestCallback callback = new TestCallback();
        doPostConnection(callback, "http://localhost:8282", "/params.jsp",
                false);

        HttpResponseMessage msg = callback.getMessage();
        assertEquals("Test One Test Two", msg.getStringContent());
    }

    private void doGetConnection(TestCallback callback, String url, String uri,
            boolean testForException) throws Exception {
        HttpRequestMessage request = new HttpRequestMessage(uri);
        request.setParameter("TEST1", "Test One");
        request.setParameter("TEST2", "Test Two");
        doConnection(callback, url, request, testForException);
    }

    private void doPostConnection(TestCallback callback, String url,
            String uri, boolean testForException) throws Exception {
        HttpRequestMessage request = new HttpRequestMessage(uri);
        request.setParameter("TEST1", "Test One");
        request.setParameter("TEST2", "Test Two");
        request.setRequestMethod(HttpRequestMessage.REQUEST_POST);
        doConnection(callback, url, request, testForException);
    }

    private void doConnection(TestCallback callback, String url,
            HttpRequestMessage request, boolean testForException)
            throws Exception {
        URL url_connect = new URL(url);

        AsyncHttpClient ahc = new AsyncHttpClient(url_connect, callback);
        ahc.connect();

        ahc.sendRequest(request);

        //We are done...Thread would normally end...
        //So this little wait simulates the thread going back in the pool
        synchronized (semaphore) {
            //5 second timeout due to no response
            semaphore.wait(5000);
        }

        if (!testForException) {
            if (callback.isException())
                throw new Exception(callback.getThrowable());
        }

    }

    class TestCallback implements AsyncHttpClientCallback {

        private boolean closed = false;

        private boolean exception = false;

        private Throwable throwable = null;

        private HttpResponseMessage message = null;

        public TestCallback() {
            clear();
        }

        public void onResponse(HttpResponseMessage message) {
            this.message = message;
            synchronized (semaphore) {
                semaphore.notify();
            }
        }

        public void onException(Throwable cause) {
            throwable = cause;
            exception = true;
            synchronized (semaphore) {
                semaphore.notify();
            }
        }

        public void onClosed() {
            closed = true;
            synchronized (semaphore) {
                semaphore.notify();
            }
        }

        public Throwable getThrowable() {
            return throwable;
        }

        public void clear() {
            closed = false;
            exception = false;
            message = null;
        }

        public boolean isClosed() {
            return closed;
        }

        public void setClosed(boolean closed) {
            this.closed = closed;
        }

        public boolean isException() {
            return exception;
        }

        public void setException(boolean exception) {
            this.exception = exception;
        }

        public HttpResponseMessage getMessage() {
            return message;
        }

        public void setMessage(HttpResponseMessage message) {
            this.message = message;
        }
    }
}
