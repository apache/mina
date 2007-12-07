/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.mina.example.httpclient;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

import org.apache.mina.filter.codec.http.DefaultHttpRequest;
import org.apache.mina.filter.codec.http.HttpResponse;
import org.apache.mina.filter.codec.http.MutableHttpRequest;
import org.apache.mina.protocol.http.client.AsyncHttpClient;
import org.apache.mina.protocol.http.client.AsyncHttpClientCallback;

/**
 * Very minimal example of how to write an HTTP Client.
 *
 * @author Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class Wget {
    /**
     * object that is used to trigger events that have happened.  Rudimentary
     * locking mechanism for this class.
     */
    protected static final Object semaphore = new Object();

    /**
     * Creates a new instance of HttpClient.  Constructor that does all
     * the work for this example.
     *
     * @param uri
     *  Example: http://www.google.com
     * @throws Exception
     *  thrown if something goes wrong.
     */
    public Wget(URI uri) throws Exception {
        if (uri.getPath() == null || uri.getPath().length() == 0) {
            uri = new URI(uri.toString() + "/index.html");
        }

        WgetCallback callback = new WgetCallback();
        MutableHttpRequest request = new DefaultHttpRequest();
        request.setRequestUri(uri);

        AsyncHttpClient ahc = new AsyncHttpClient(uri, callback);
        ahc.connect();
        ahc.sendRequest(request);
        synchronized (semaphore) {
            // 10 second timeout due to no response.
            semaphore.wait(1000 * 10);
        }

        if (callback.isException()) {
            throw new Exception(callback.getThrowable());
        }

        HttpResponse msg = callback.getMessage();
        String charset = msg.getContentType().replaceAll(".*charset\\s*=\\s*", "");
        CharsetDecoder decoder;
        try {
            decoder = Charset.forName(charset).newDecoder();
        } catch (Exception e) {
            decoder = Charset.forName("ISO-8859-1").newDecoder();
        }
        System.out.println(msg.getContent().getString(decoder));
    }

    class WgetCallback implements AsyncHttpClientCallback {

        private boolean closed = false;

        private boolean exception = false;

        private Throwable throwable = null;

        private HttpResponse message = null;

        public WgetCallback() {
            clear();
        }

        /**
         * What to do when a response has come from the server
         *
         * @see org.apache.mina.protocol.http.client.AsyncHttpClientCallback#onResponse(HttpResponse)
         */
        public void onResponse(HttpResponse message) {
            this.message = message;
            synchronized (semaphore) {
                semaphore.notify();
            }
        }

        /**
         * What to do when an exception has been thrown
         *
         * @see org.apache.mina.protocol.http.client.AsyncHttpClientCallback#onException(Throwable)
         */
        public void onException(Throwable cause) {
            throwable = cause;
            exception = true;
            synchronized (semaphore) {
                semaphore.notify();
            }
        }

        /**
         * The connection has been closed, notify the semaphore object and set
         * closed to true.
         *
         * @see org.apache.mina.protocol.http.client.AsyncHttpClientCallback#onClosed()
         */
        public void onClosed() {
            closed = true;
            synchronized (semaphore) {
                semaphore.notify();
            }
        }

        /**
         * return the Throwable that this class has thrown
         *
         * @return
         *  the Throwable that this class has thrown
         */
        public Throwable getThrowable() {
            return throwable;
        }

        /**
         * Reset all private fields in this class
         *
         */
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

        public HttpResponse getMessage() {
            return message;
        }

        public void setMessage(HttpResponse message) {
            this.message = message;
        }
    }

    /**
     * Entry point for trying out the HttpClient example application.  This
     * class will print out the page that is specified in the first parameter
     * and write it to a file specified by the seconds parameter
     *
     * @param args
     *  String array of length 1.  First parameter is the url
     * @throws Exception
     *  Thrown if something goes wrong.
     */
    public static void main(String[] args) throws Exception {
        URI theUrlRequest = new URI(args[0]);
        new Wget(theUrlRequest);

        System.exit(0);
    }
}
