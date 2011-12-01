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

    /**
     * The high digits lookup table.
    */
    private static final byte[] highDigits;

    /**
     * The low digits lookup table.
     */
    private static final byte[] lowDigits;

    /**
     * Initialize lookup tables.
     */
    static {
        final byte[] digits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

        int i;
        byte[] high = new byte[256];
        byte[] low = new byte[256];

        for (i = 0; i < 256; i++) {
            high[i] = digits[i >>> 4];
            low[i] = digits[i & 0x0F];
        }

        highDigits = high;
        lowDigits = low;
    }
    
    public static String dump(IoBuffer buffer) {
        StringBuilder sb = new StringBuilder();
        
        boolean isFirst = true;
        
        for (int i = 0; i < buffer.limit(); i++) {
            int byteValue = buffer.get(i) & 0xFF;
            
            if (isFirst) {
                isFirst = false;
            } else {
                sb.append(' ');
            }
            
            sb.append("0x");
            sb.append((char) highDigits[byteValue]);
            sb.append((char) lowDigits[byteValue]);
        }
            
        return sb.toString();
    }

    public static String dump(ByteBuffer buffer) {

        // is not ASCII printable ?
        boolean binaryContent = false;
        for (int i = 0; i < buffer.remaining(); i++) {
            byte b = buffer.get(i);
            if ((b < 32 || b > 126) && b != 13 && b != 10) {
                binaryContent = true;
                break;
            }
        }

        if (binaryContent) {
            int pos = buffer.position();
            int size = buffer.remaining();
            StringBuilder out = new StringBuilder(size * 3 + 24);
            out.append("ByteBuffer[len=").append(size).append(",bytes='");
            int mark = buffer.position();

            // fill the first
            int byteValue = buffer.get() & 0xFF;
            out.append((char) highDigits[byteValue]);
            out.append((char) lowDigits[byteValue]);
            size--;

            // and the others, too
            for (; size > 0; size--) {
                out.append(' ');
                byteValue = buffer.get() & 0xFF;
                out.append((char) highDigits[byteValue]);
                out.append((char) lowDigits[byteValue]);
            }
            buffer.position(pos);
            out.append("'");
            return out.toString();

        } else {
            byte[] data = new byte[buffer.remaining()];
            int pos = buffer.position();
            buffer.get(data);
            buffer.position(pos);
            StringBuilder out = new StringBuilder(buffer.remaining() + 22);
            out.append("ByteBuffer[len=").append(buffer.remaining()).append(",str='").append(new String(data))
                    .append("'");
            return out.toString();
        }
    }

}
