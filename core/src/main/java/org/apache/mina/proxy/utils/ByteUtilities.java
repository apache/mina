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
package org.apache.mina.proxy.utils;

import java.io.UnsupportedEncodingException;

/**
 * ByteUtilities.java - Byte manipulation functions.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 * @since MINA 2.0.0-M3
 */
public class ByteUtilities {
    /**
     * Returns the integer represented by 4 bytes in network byte order.
     */
    public static int networkByteOrderToInt(byte[] buf, int start, int count) {
        if (count > 4) {
            throw new IllegalArgumentException(
                    "Cannot handle more than 4 bytes");
        }

        int result = 0;

        for (int i = 0; i < count; i++) {
            result <<= 8;
            result |= ((int) buf[start + i] & 0xff);
        }

        return result;
    }

    /**
     * Encodes an integer into 4 bytes in network byte order in the buffer
     * supplied.
     */
    public static byte[] intToNetworkByteOrder(int num, byte[] buf, int start,
            int count) {
        if (count > 4) {
            throw new IllegalArgumentException(
                    "Cannot handle more than 4 bytes");
        }

        for (int i = count - 1; i >= 0; i--) {
            buf[start + i] = (byte) (num & 0xff);
            num >>>= 8;
        }

        return buf;
    }

    /**
     * Write a 16 bit short as LITTLE_ENDIAN.
     * 
     * @param v the short to write
     */
    public final static byte[] writeShort(short v) {
        return writeShort(v, new byte[2], 0);
    }

    /**
     * Write a 16 bit short as LITTLE_ENDIAN to
     * the given array <code>b</code> at offset <code>offset</code>.
     * 
     * @param v the short to write
     */
    public final static byte[] writeShort(short v, byte[] b, int offset) {
        b[offset] = (byte) v;
        b[offset + 1] = (byte) (v >> 8);

        return b;
    }

    /**
     * Write a 32 bit int as LITTLE_ENDIAN.
     * 
     * @param v the int to write
     */
    public final static byte[] writeInt(int v) {
        return writeInt(v, new byte[4], 0);
    }

    /**
     * Write a 32 bit int as LITTLE_ENDIAN to
     * the given array <code>b</code> at offset <code>offset</code>.
     * 
     * @param v the int to write
     */
    public final static byte[] writeInt(int v, byte[] b, int offset) {
        b[offset] = (byte) v;
        b[offset + 1] = (byte) (v >> 8);
        b[offset + 2] = (byte) (v >> 16);
        b[offset + 3] = (byte) (v >> 24);

        return b;
    }

    public final static void changeWordEndianess(byte[] b, int offset,
            int length) {
        byte tmp;

        for (int i = offset; i < offset + length; i += 4) {
            tmp = b[i];
            b[i] = b[i + 3];
            b[i + 3] = tmp;
            tmp = b[i + 1];
            b[i + 1] = b[i + 2];
            b[i + 2] = tmp;
        }
    }

    public final static void changeByteEndianess(byte[] b, int offset,
            int length) {
        byte tmp;

        for (int i = offset; i < offset + length; i += 2) {
            tmp = b[i];
            b[i] = b[i + 1];
            b[i + 1] = tmp;
        }
    }

    public final static byte[] getOEMStringAsByteArray(String s)
            throws UnsupportedEncodingException {
        return s.getBytes("ASCII");
    }

    public final static byte[] getUTFStringAsByteArray(String s)
            throws UnsupportedEncodingException {
        return s.getBytes("UTF-16LE");
    }

    public final static byte[] encodeString(String s, boolean useUnicode)
            throws UnsupportedEncodingException {
        if (useUnicode) {
            return getUTFStringAsByteArray(s);
        } else {
            return getOEMStringAsByteArray(s);
        }
    }

    public static String asHex(byte[] bytes) {
        return asHex(bytes, null);
    }

    public static String asHex(byte[] bytes, String separator) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String code = Integer.toHexString(bytes[i] & 0xFF);
            if ((bytes[i] & 0xFF) < 16) {
                sb.append('0');
            }

            sb.append(code);

            if (separator != null && i < bytes.length - 1) {
                sb.append(separator);
            }
        }

        return sb.toString();
    }

    public static byte[] asByteArray(String hex) {
        byte[] bts = new byte[hex.length() / 2];
        for (int i = 0; i < bts.length; i++) {
            bts[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2),
                    16);
        }

        return bts;
    }

    public static final int makeIntFromByte4(byte[] b) {
        return makeIntFromByte4(b, 0);
    }

    public static final int makeIntFromByte4(byte[] b, int offset) {
        return b[offset] << 24 | (b[offset + 1] & 0xff) << 16
                | (b[offset + 2] & 0xff) << 8 | (b[offset + 3] & 0xff);
    }

    public static final int makeIntFromByte2(byte[] b) {
        return makeIntFromByte2(b, 0);
    }

    public static final int makeIntFromByte2(byte[] b, int offset) {
        return (b[offset] & 0xff) << 8 | (b[offset + 1] & 0xff);
    }

    /**
     * Return true if the flag <code>testFlag</code> is set in the
     * <code>flags</code> flagset.
     * 
     * @param flagset the flagset to test
     * @param testFlag the flag we search the presence of
     * @return true if testFlag is present in the flagset, false otherwise.
     */
    public final static boolean isFlagSet(int flagSet, int testFlag) {
        return (flagSet & testFlag) > 0;
    }
}