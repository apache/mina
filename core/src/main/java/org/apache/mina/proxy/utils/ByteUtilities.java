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
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @since MINA 2.0.0-M3
 */
public class ByteUtilities {

    /**
     * Returns the integer represented by up to 4 bytes in network byte order.
     * 
     * @param buf the buffer to read the bytes from
     * @param start
     * @param count
     * @return
     */
    public static int networkByteOrderToInt(byte[] buf, int start, int count) {
        if (count > 4) {
            throw new IllegalArgumentException(
                    "Cannot handle more than 4 bytes");
        }

        int result = 0;

        for (int i = 0; i < count; i++) {
            result <<= 8;
            result |= (buf[start + i] & 0xff);
        }

        return result;
    }

    /**
     * Encodes an integer into up to 4 bytes in network byte order.
     * 
     * @param num the int to convert to a byte array
     * @param count the number of reserved bytes for the write operation
     * @return the resulting byte array
     */
    public static byte[] intToNetworkByteOrder(int num, int count) {
        byte[] buf = new byte[count];
        intToNetworkByteOrder(num, buf, 0, count);
        
        return buf;
    }
    
    /**
     * Encodes an integer into up to 4 bytes in network byte order in the 
     * supplied buffer starting at <code>start</code> offset and writing
     * <code>count</code> bytes.
     * 
     * @param num the int to convert to a byte array
     * @param buf the buffer to write the bytes to
     * @param start the offset from beginning for the write operation
     * @param count the number of reserved bytes for the write operation
     */
    public static void intToNetworkByteOrder(int num, byte[] buf, int start,
            int count) {
        if (count > 4) {
            throw new IllegalArgumentException(
                    "Cannot handle more than 4 bytes");
        }

        for (int i = count - 1; i >= 0; i--) {
            buf[start + i] = (byte) (num & 0xff);
            num >>>= 8;
        }
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
     * @param b the byte array to write to
     * @param offset the offset at which to start writing in the array
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
     * @param b the byte array to write to
     * @param offset the offset at which to start writing in the array
     */
    public final static byte[] writeInt(int v, byte[] b, int offset) {
        b[offset] = (byte) v;
        b[offset + 1] = (byte) (v >> 8);
        b[offset + 2] = (byte) (v >> 16);
        b[offset + 3] = (byte) (v >> 24);

        return b;
    }

    /**
     * Invert the endianness of words (4 bytes) in the given byte array 
     * starting at the given offset and repeating length/4 times.
     * eg: b0b1b2b3 -> b3b2b1b0 
     * 
     * @param b the byte array 
     * @param offset the offset at which to change word start
     * @param length the number of bytes on which to operate 
     * (should be a multiple of 4)
     */
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

    /**
     * Invert two bytes in the given byte array starting at the given 
     * offset and repeating the inversion length/2 times.
     * eg: b0b1 -> b1b0
     * 
     * @param b the byte array 
     * @param offset the offset at which to change word start
     * @param length the number of bytes on which to operate 
     * (should be a multiple of 2)
     */
    public final static void changeByteEndianess(byte[] b, int offset,
            int length) {
        byte tmp;

        for (int i = offset; i < offset + length; i += 2) {
            tmp = b[i];
            b[i] = b[i + 1];
            b[i + 1] = tmp;
        }
    }

    /**
     * Converts an OEM string as defined in NTLM protocol (eg ASCII charset)
     * to a byte array.
     * 
     * @param s the string to convert
     * @return the result byte array
     * @throws UnsupportedEncodingException if the string is not an OEM string
     */
    public final static byte[] getOEMStringAsByteArray(String s)
            throws UnsupportedEncodingException {
        return s.getBytes("ASCII");
    }

    /**
     * Converts an UTF-16LE string as defined in NTLM protocol to a byte array.
     * 
     * @param s the string to convert
     * @return the result byte array
     * @throws UnsupportedEncodingException if the string is not an UTF-16LE string
     */    
    public final static byte[] getUTFStringAsByteArray(String s)
            throws UnsupportedEncodingException {
        return s.getBytes("UTF-16LE");
    }

    /**
     * Encodes the string to a byte array using UTF-16LE or the ASCII charset
     * in function of the <code>useUnicode</code> argument.
     * 
     * @param s the string to encode
     * @param useUnicode if true then string is encoded to UTF-16LE 
     * otherwise to ASCII
     * @return the encoded string as a byte array
     * @throws UnsupportedEncodingException if encoding fails
     */
    public final static byte[] encodeString(String s, boolean useUnicode)
            throws UnsupportedEncodingException {
        if (useUnicode) {
            return getUTFStringAsByteArray(s);
        }

        return getOEMStringAsByteArray(s);
    }

    /**
     * Returns a hexadecimal representation of the given byte array.
     * 
     * @param bytes the array to output to an hex string
     * @return the hex representation as a string
     */
    public static String asHex(byte[] bytes) {
        return asHex(bytes, null);
    }

    /**
     * Returns a hexadecimal representation of the given byte array.
     * 
     * @param bytes the array to output to an hex string
     * @param separator the separator to use between each byte in the output
     * string. If null no char is inserted between each byte value. 
     * @return the hex representation as a string
     */
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

    /**
     * Converts a hex string representation to a byte array.
     * 
     * @param hex the string holding the hex values
     * @return the resulting byte array
     */
    public static byte[] asByteArray(String hex) {
        byte[] bts = new byte[hex.length() / 2];
        for (int i = 0; i < bts.length; i++) {
            bts[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2),
                    16);
        }

        return bts;
    }

    /**
     * Reads an int from 4 bytes of the given array at offset 0.
     * 
     * @param b the byte array to read
     * @param offset the offset at which to start
     * @return the int value
     */
    public static final int makeIntFromByte4(byte[] b) {
        return makeIntFromByte4(b, 0);
    }

    /**
     * Reads an int from 4 bytes of the given array at the given offset.
     * 
     * @param b the byte array to read
     * @param offset the offset at which to start
     * @return the int value
     */
    public static final int makeIntFromByte4(byte[] b, int offset) {
        return b[offset] << 24 | (b[offset + 1] & 0xff) << 16
                | (b[offset + 2] & 0xff) << 8 | (b[offset + 3] & 0xff);
    }

    /**
     * Reads an int from 2 bytes of the given array at offset 0.
     * 
     * @param b the byte array to read
     * @return the int value     
     */
    public static final int makeIntFromByte2(byte[] b) {
        return makeIntFromByte2(b, 0);
    }

    /**
     * Reads an int from 2 bytes of the given array at the given offset.
     * 
     * @param b the byte array to read
     * @param offset the offset at which to start
     * @return the int value
     */
    public static final int makeIntFromByte2(byte[] b, int offset) {
        return (b[offset] & 0xff) << 8 | (b[offset + 1] & 0xff);
    }

    /**
     * Returns true if the flag <code>testFlag</code> is set in the
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