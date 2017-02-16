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
package org.apache.mina.core.buffer;

/**
 * Provides utility methods to dump an {@link IoBuffer} into a hex formatted string.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
class IoBufferHexDumper {

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

    /**
     * Dumps an {@link IoBuffer} to a hex formatted string.
     * 
     * @param in the buffer to dump
     * @param lengthLimit the limit at which hex dumping will stop
     * @return a hex formatted string representation of the <i>in</i> {@link IoBuffer}.
     */
    public static String getHexdump(IoBuffer in, int lengthLimit) {
        if (lengthLimit == 0) {
            throw new IllegalArgumentException("lengthLimit: " + lengthLimit + " (expected: 1+)");
        }

        boolean truncate = in.remaining() > lengthLimit;
        int size;
        if (truncate) {
            size = lengthLimit;
        } else {
            size = in.remaining();
        }

        if (size == 0) {
            return "empty";
        }

        StringBuilder out = new StringBuilder(size * 3 + 3);

        int mark = in.position();

        // fill the first
        int byteValue = in.get() & 0xFF;
        out.append((char) highDigits[byteValue]);
        out.append((char) lowDigits[byteValue]);
        size--;

        // and the others, too
        for (; size > 0; size--) {
            out.append(' ');
            byteValue = in.get() & 0xFF;
            out.append((char) highDigits[byteValue]);
            out.append((char) lowDigits[byteValue]);
        }

        in.position(mark);

        if (truncate) {
            out.append("...");
        }

        return out.toString();
    }
}