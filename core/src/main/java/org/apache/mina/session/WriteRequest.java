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
package org.apache.mina.session;

import org.apache.mina.api.IoFuture;

/**
 * The write request created by the {@link org.apache.mina.api.IoSession#write} method, 
 * which is transmitted through the filter chain and finish as a socket write.<br/>
 * 
 * We store the original message into this data structure, along the associated potentially
 * modified message if the original message gets encoded during the process.<br/>
 * 
 * Note that when we always ends with the message being a ByteBuffer when we reach 
 * the socket.<br/>
 * 
 * We also keep a Future into this data structure to inform the caller about the write
 * completion.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public interface WriteRequest {
    /**
     * Get the message stored for this request.
     * 
     * @return the contained message
     */
    Object getMessage();

    /**
     * Store the encoded message
     * 
     * @param The encoded message
     */
    void setMessage(Object message);

    /**
     * Gets the original message, as written by the handler, before passing through the filter chain.
     * 
     * @return The original message
     */
    Object getOriginalMessage();

    /**
     * The future to be completed on a write success
     * @return the future
     */
    IoFuture<Void> getFuture();

    /**
     * Store the future into the request
     * @param the future
     */
    void setFuture(IoFuture<Void> future);
    
    /**
     * Get the flag that tells that the underlying message is an internal one,
     * not needed to be encrypted
     * @return the internal secure flag of the message
     */
    boolean isSecureInternal();
    

    /**
     * Set the flag that tells that the underlying message is an internal one,
     * not needed to be encrypted
     * @param secureInternal the secure internal flag
     */
    void setSecureInternal(boolean secureInternal);
    
    /**
     * When this message has been set, should we send a corresponding send event
     * or not.
     * @return the send confirm flag
     */
    boolean isConfirmRequested();
}