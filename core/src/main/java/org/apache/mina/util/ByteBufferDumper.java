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
package org.apache.mina.util;

import java.nio.ByteBuffer;

/**
 * Utility class for smart dumping {@link ByteBuffer}
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ByteBufferDumper {
    /** Hex chars */
    private static final byte[] HEX_CHAR = new byte[]
        { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
    
    /**
     * Dump the content of a IoBuffer
     * 
     * @param buffer The IoBuffer to dump
     * @return A string representing the IoBuffer content
     */
    public static String dump(IoBuffer buffer) {
        StringBuilder sb = new StringBuilder();
        
        boolean isFirst = true;
        
        for (int i = 0; i < buffer.limit(); i++) {
            byte byteValue = buffer.get(i);
            
            if (isFirst) {
                isFirst = false;
            } else {
                sb.append(' ');
            }
            
            sb.append( new String( new byte[]
                { '0', 'x', HEX_CHAR[( byteValue & 0x00F0 ) >> 4], HEX_CHAR[byteValue & 0x000F] } ));
        }
            
        return sb.toString();
    }
    
    /**
     * Dump the content of the given ByteBuffer, up to a number of bytes. If the
     * toAscii flag is set to <code>true</code>, this method will try to convert
     * the bytes to a String
     * 
     * @param buffer The buffer to dump
     * @param nbBytes The number of bytes to dump (-1 for all of them)
     * @param toAscii If we want to write the message as a String
     * @return A dump of this ByteBuffer
     */
    public static String dump(ByteBuffer buffer, int nbBytes, boolean toAscii) {
        byte[] data = buffer.array();
        int start = buffer.position();
        int size = Math.min(buffer.remaining(), nbBytes >= 0?nbBytes : Integer.MAX_VALUE);
        int length = buffer.remaining();
        
        // is not ASCII printable ?
        boolean binaryContent = false;
        
        if (toAscii) {
            for (int i = start; i < size; i++) {
                byte b = data[i];
                
                if (((b < 32) || (b > 126)) && (b != 13) && (b != 10)) {
                    binaryContent = true;
                    break;
                }
            }
        }

        if (!toAscii || binaryContent) {
            StringBuilder out = new StringBuilder(size * 3 + 30);
            out.append("ByteBuffer[len=").append(length).append(",bytes='");

            // fill the first
            int byteValue = data[start] & 0xFF;
            boolean isFirst = true;

            // and the others, too
            for (int i = start; i < size; i++) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    out.append(' ');
                }
                
                byteValue = data[i] & 0xFF;
                out.append( new String( new byte[]
                    { '0', 'x', HEX_CHAR[( byteValue & 0x00F0 ) >> 4], HEX_CHAR[byteValue & 0x000F] } ));
            }
            
            out.append("']");
            
            return out.toString();

        } else {
            StringBuilder sb = new StringBuilder(size);
            sb.append("ByteBuffer[len=")
                .append(length)
                .append(",str='")
                .append(new String(data, start, size))
                .append("']");
            
            return sb.toString();
        }
    }

    /**
     * Dumps the given buffer. If the buffer contains only ascii, it will write
     * the buffer content as a String.
     * 
     * @param buffer The buffer to dump
     * @return A string representing the buffer content
     */
    public static String dump(ByteBuffer buffer) {
        return dump(buffer, -1, true);
    }
}
