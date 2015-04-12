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
package org.apache.mina.http2.api;

import java.nio.charset.Charset;

/**
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public final class Http2Constants {
    /**
     * Mask used when decoding on a 4 byte boundary, masking the reserved
     * bit
     */
    public static final int HTTP2_31BITS_MASK = 0x7FFFFFFF;
    
    /**
     * Mask used when decoding on a 4 byte boundary, retrieving
     * the exclusive bit
     */
    public static final int HTTP2_EXCLUSIVE_MASK = 0x80000000;
    
    /**
     * Length of the HTTP2 header (length, type, flags, streamID)
     */
    public static final int HTTP2_HEADER_LENGTH = 9;

    /*
     * Frame types
     */
    /**
     * DATA frame
     */
    public static final short FRAME_TYPE_DATA = 0x00;
    
    /**
     * HEADERS frame
     */
    public static final short FRAME_TYPE_HEADERS = 0x01;
    
    /**
     * PRIORITY frame
     */
    public static final short FRAME_TYPE_PRIORITY = 0x02;
    
    /**
     * RST_STREAM frame
     */
    public static final short FRAME_TYPE_RST_STREAM = 0x03;
    
    /**
     * SETTINGS stream
     */
    public static final short FRAME_TYPE_SETTINGS = 0x04;
    
    /**
     * PUSH_PROMISE frame
     */
    public static final short FRAME_TYPE_PUSH_PROMISE = 0x05;
    
    /**
     * PING frame
     */
    public static final short FRAME_TYPE_PING = 0x06;
    
    /**
     * GOAWAY frame
     */
    public static final short FRAME_TYPE_GOAWAY = 0x07;
    
    /**
     * WINDOW_UPDATE frame
     */
    public static final short FRAME_TYPE_WINDOW_UPDATE = 0x08;
    
    /**
     * CONTINUATION frame
     */
    public static final short FRAME_TYPE_CONTINUATION = 0x09;
    
    /*
     * Flags
     */
    public static final byte FLAGS_END_STREAM = 0x01;
    
    public static final byte FLAGS_ACK = 0x01;
    
    public static final byte FLAGS_END_HEADERS = 0x04;
    
    public static final byte FLAGS_PADDING = 0x08;
    
    public static final byte FLAGS_PRIORITY = 0x20;
    
    /*
     * Error codes
     */
    /**
     * The associated condition is not as a result of an error. For example, a GOAWAY might include this code to indicate graceful shutdown of a connection.
     */
    public static final int NO_ERROR = 0x0;
    
    /**
     * The endpoint detected an unspecific protocol error. This error is for use when a more specific error code is not available.
     */
    public static final int PROTOCOL_ERROR = 0x1;

    /**
     * The endpoint encountered an unexpected internal error.
     */
    public static final int INTERNAL_ERROR = 0x2;
    
    /**
     * The endpoint detected that its peer violated the flow control protocol.
     */
    public static final int FLOW_CONTROL_ERROR = 0x3;

    /**
     * The endpoint sent a SETTINGS frame, but did not receive a response in a timely manner. See Settings Synchronization (Section 6.5.3).
     */
    public static final int SETTINGS_TIMEOUT = 0x4;

    /**
     * The endpoint received a frame after a stream was half closed.
     */
    public static final int STREAM_CLOSED = 0x5;

    /**
     * The endpoint received a frame with an invalid size.
     */
    public static final int FRAME_SIZE_ERROR = 0x6;

    /**
     * The endpoint refuses the stream prior to performing any application processing, see Section 8.1.4 for details.
     */
    public static final int REFUSED_STREAM = 0x7;
     
    /**
     * Used by the endpoint to indicate that the stream is no longer needed.
     */
    public static final int CANCEL = 0x8;

    /**
     * The endpoint is unable to maintain the header compression context for the connection.
     */
    public static final int COMPRESSION_ERROR = 0x9;
    
    /**
     * The connection established in response to a CONNECT request (Section 8.3) was reset or abnormally closed.
     */
    public static final int CONNECT_ERROR = 0xa;
    
    /**
     * The endpoint detected that its peer is exhibiting a behavior that might be generating excessive load.
     */
    public static final int ENHANCE_YOUR_CALM = 0xb;

    /**
     * The underlying transport has properties that do not meet minimum security requirements (see Section 9.2).
     */
    public static final int INADEQUATE_SECURITY = 0xc;

    /**
     * The endpoint requires that HTTP/1.1 be used instead of HTTP/2.
     */
    public static final int HTTP_1_1_REQUIRED = 0xd;
        
    /*
     * Settings related stuff
     */
    public static final int SETTINGS_HEADER_TABLE_SIZE = 0x01;
    
    public static final int SETTINGS_HEADER_TABLE_SIZE_DEFAULT = 4096;
    
    public static final int SETTINGS_ENABLE_PUSH = 0x02;
    
    public static final int SETTINGS_ENABLE_PUSH_DEFAULT = 1;
    
    public static final int SETTINGS_MAX_CONCURRENT_STREAMS = 0x03;
    
    public static final int SETTINGS_MAX_CONCURRENT_STREAMS_DEFAULT = Integer.MAX_VALUE;
    
    public static final int SETTINGS_INITIAL_WINDOW_SIZE = 0x04;
    
    public static final int SETTINGS_INITIAL_WINDOW_SIZE_DEFAULT = 65535;
    
    public static final int SETTINGS_MAX_FRAME_SIZE = 0x05;
    
    public static final int SETTINGS_MAX_FRAME_SIZE_DEFAULT = 16384;
    
    public static final int SETTINGS_MAX_HEADER_LIST_SIZE = 0x06;
    
    public static final int SETTINGS_MAX_HEADER_LIST_SIZE_DEFAULT = Integer.MAX_VALUE;
    
    public static final Charset US_ASCII_CHARSET = Charset.forName("US-ASCII");
    
    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    
}
