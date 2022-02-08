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

import java.io.UnsupportedEncodingException;

/**
 * Provides utility methods to dump an {@link IoBuffer} into a hex formatted
 * string.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
class IoBufferHexDumper {
    private static final char hexDigit[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E',
    'F' };

    /**
     * Dumps an {@link IoBuffer} to a hex formatted string.
     * 
     * @param buf    the buffer to dump
     * @param offset the starting position to begin reading the hex dump
     * @param length the number of bytes to dump
     * @return a hex formatted string representation of the <i>in</i>
     *         {@link IoBuffer}.
     */
    public static String getHexDumpSlice(IoBuffer buf, int offset, int length) {
        if (buf == null) {
            throw new IllegalArgumentException();
        }

        if (length < 0 || offset < 0 || offset + length > buf.limit()) {
            throw new IndexOutOfBoundsException();
        }

        int pos = offset;
        int items = Math.min(offset + length, offset + buf.limit()) - pos;

        if (items <= 0) {
            return "";
        }

        int lim = pos + items;

        StringBuilder out = new StringBuilder((items * 3) + 6);

        for (;;) {
            int byteValue = buf.get(pos++) & 0xFF;
            out.append((char) hexDigit[(byteValue >> 4) & 0x0F]);
            out.append((char) hexDigit[byteValue & 0xf]);

            if (pos < lim) {
                out.append(' ');
            } else {
                break;
            }
        }

        return out.toString();
    }
    
    /**
     * Produces a verbose hex dump
     *
     * @param buf The buffer to pretty print
     * @param offset initial position which to read bytes
     * @param length number of bytes to display
     *
     * @return The formatted String representing the content between (offset) and
     *         (offset+count)
     */
    public static final String getPrettyHexDumpSlice(IoBuffer buf, int offset, int length) {
        if (buf == null) {
            throw new IllegalArgumentException();
        }

        if (length < 0 || offset < 0 || offset + length > buf.limit()) {
            throw new IndexOutOfBoundsException();
        }

        final int len = Math.min(length, buf.limit() - offset);
        final byte[] bytes = new byte[len];

        int o = offset;

        for (int i = 0; i < len; i++) {
            bytes[i] = buf.get(o++);
        }

        final StringBuilder sb = new StringBuilder();

        sb.append("Source ");
        sb.append(buf);
        sb.append(" showing index ");
        sb.append(offset);
        sb.append(" through ");
        sb.append((offset + length));
        sb.append("\n");
        sb.append(toPrettyHexDump(bytes, 0, bytes.length));

        return sb.toString();
    }

    /**
     * Generates a hex dump with line numbers, hex, volumes, and ascii
     * representation
     *
     * @param data source data to read for the hex dump
     *
     * @param pos  index position to begin reading
     *
     * @param len  number of bytes to read
     *
     * @return string hex dump
     */
    public static final String toPrettyHexDump(byte[] data, int pos, int len) {
        if (data == null) {
            throw new IllegalArgumentException();
        }

        if (len < 0 || pos < 0 || pos + len > data.length) {
            throw new IndexOutOfBoundsException();
        }

        StringBuilder buffer = new StringBuilder();

        // Process every byte in the data.
        for (int i = pos, c = 0, line = 16; i < len; i += line) {
            buffer.append(String.format("%06d", Integer.valueOf(c)) + "  ");

            buffer.append(toPrettyHexDumpLine(data, i, Math.min((pos + len) - i, line), 8, line));

            if ((i + line) < len) {
                buffer.append("\n");
            }

            c += line;
        }

        return buffer.toString();

    }

    /**
     * Generates the hex dump line with hex values, columns, and ascii
     * representation
     *
     * @param data source data to read for the hex dump
     *
     * @param pos  index position to begin reading
     *
     * @param len  number of bytes to read; this can be less than the <tt>line</tt>
     *             width
     *
     * @param col  number of bytes in a column
     *
     * @param line line width in bytes which pads the output if <tt>len</tt> is less
     *             than <tt>line</tt>
     *
     * @return string hex dump
     */
    private static final String toPrettyHexDumpLine(byte[] data, int pos, int len, int col, int line) {
        if ((line % 2) != 0) {
            throw new IllegalArgumentException("length must be multiple of 2");
        }

        StringBuilder buffer = new StringBuilder();

        for (int i = pos, t = Math.min(data.length - pos, len) + pos; i < t;) {
            for (int x = 0; (x < col) && (i < t); i++, x++) {
                buffer.append(toHex(data[i]));
                buffer.append(" ");
            }

            buffer.append(" ");
        }

        int cl = (line * 3) + (line / col);

        if (buffer.length() != cl) // check if we need to pad the output
        {
            cl -= buffer.length();

            while (cl > 0) {
                buffer.append(" ");
                cl--;
            }
        }

        try {
            String p = new String(data, pos, Math.min(data.length - pos, len), "Cp1252").replace("\r\n", "..")
                    .replace("\n", ".").replace("\\", ".");

            char[] ch = p.toCharArray();

            for (int m = 0; m < ch.length; m++) {
                if (ch[m] < 32) {
                    ch[m] = (char) 46; // add dots for whitespace chars
                }
            }

            buffer.append(ch);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return buffer.toString();
    }

    public static final String toHex(byte b) {
        // Returns hex String representation of byte

        char[] array = { hexDigit[(b >> 4) & 0x0f], hexDigit[b & 0x0f] };
        
        return new String(array);
    }
}