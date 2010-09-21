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

import java.security.DigestException;
import java.security.MessageDigestSpi;

/**
 * MD4.java - An implementation of Ron Rivest's MD4 message digest algorithm.
 * The MD4 algorithm is designed to be quite fast on 32-bit machines. In
 * addition, the MD4 algorithm does not require any large substitution
 * tables.
 *
 * @see The <a href="http://www.ietf.org/rfc/rfc1320.txt">MD4</a> Message-
 *    Digest Algorithm by R. Rivest.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @since MINA 2.0.0-M3
 */
public class MD4 extends MessageDigestSpi {

    /**
     * The MD4 algorithm message digest length is 16 bytes wide.
     */
    public static final int BYTE_DIGEST_LENGTH = 16;

    /**
     * The MD4 algorithm block length is 64 bytes wide.
     */
    public static final int BYTE_BLOCK_LENGTH = 64;

    /**
     * The initial values of the four registers. RFC gives the values 
     * in LE so we converted it as JAVA uses BE endianness.
     */
    private final static int A = 0x67452301;

    private final static int B = 0xefcdab89;

    private final static int C = 0x98badcfe;

    private final static int D = 0x10325476;

    /**
     * The four registers initialized with the above IVs.
     */
    private int a = A;

    private int b = B;

    private int c = C;

    private int d = D;

    /**
     * Counts the total length of the data being digested.
     */
    private long msgLength;

    /**
     * The internal buffer is {@link BLOCK_LENGTH} wide.
     */
    private final byte[] buffer = new byte[BYTE_BLOCK_LENGTH];

    /**
     * Default constructor.
     */
    public MD4() {
        // Do nothing
    }

    /**
     * Returns the digest length in bytes.
     *
     * @return the digest length in bytes.
     */
    protected int engineGetDigestLength() {
        return BYTE_DIGEST_LENGTH;
    }

    /**
     * {@inheritDoc}
     */
    protected void engineUpdate(byte b) {
        int pos = (int) (msgLength % BYTE_BLOCK_LENGTH);
        buffer[pos] = b;
        msgLength++;

        // If buffer contains enough data then process it.
        if (pos == (BYTE_BLOCK_LENGTH - 1)) {
            process(buffer, 0);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void engineUpdate(byte[] b, int offset, int len) {
        int pos = (int) (msgLength % BYTE_BLOCK_LENGTH);
        int nbOfCharsToFillBuf = BYTE_BLOCK_LENGTH - pos;
        int blkStart = 0;

        msgLength += len;

        // Process each full block
        if (len >= nbOfCharsToFillBuf) {
            System.arraycopy(b, offset, buffer, pos, nbOfCharsToFillBuf);
            process(buffer, 0);
            for (blkStart = nbOfCharsToFillBuf; blkStart + BYTE_BLOCK_LENGTH
                    - 1 < len; blkStart += BYTE_BLOCK_LENGTH) {
                process(b, offset + blkStart);
            }
            pos = 0;
        }

        // Fill buffer with the remaining data
        if (blkStart < len) {
            System.arraycopy(b, offset + blkStart, buffer, pos, len - blkStart);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected byte[] engineDigest() {
        byte[] p = pad();
        engineUpdate(p, 0, p.length);
        byte[] digest = { (byte) a, (byte) (a >>> 8), (byte) (a >>> 16),
                (byte) (a >>> 24), (byte) b, (byte) (b >>> 8),
                (byte) (b >>> 16), (byte) (b >>> 24), (byte) c,
                (byte) (c >>> 8), (byte) (c >>> 16), (byte) (c >>> 24),
                (byte) d, (byte) (d >>> 8), (byte) (d >>> 16),
                (byte) (d >>> 24) };

        engineReset();

        return digest;
    }

    /**
     * {@inheritDoc}
     */
    protected int engineDigest(byte[] buf, int offset, int len)
            throws DigestException {
        if (offset < 0 || offset + len >= buf.length) {
            throw new DigestException(
                    "Wrong offset or not enough space to store the digest");
        }
        int destLength = Math.min(len, BYTE_DIGEST_LENGTH);
        System.arraycopy(engineDigest(), 0, buf, offset, destLength);
        return destLength;
    }

    /**
     * {@inheritDoc}
     */
    protected void engineReset() {
        a = A;
        b = B;
        c = C;
        d = D;
        msgLength = 0;
    }

    /**
     * Pads the buffer by appending the byte 0x80, then append as many zero 
     * bytes as necessary to make the buffer length a multiple of 64 bytes.  
     * The last 8 bytes will be filled with the length of the buffer in bits.
     * If there's no room to store the length in bits in the block i.e the block 
     * is larger than 56 bytes then an additionnal 64-bytes block is appended.
     * 
     * @see sections 3.1 & 3.2 of the RFC 1320.
     * 
     * @return the pad byte array
     */
    private byte[] pad() {
        int pos = (int) (msgLength % BYTE_BLOCK_LENGTH);
        int padLength = (pos < 56) ? (64 - pos) : (128 - pos);
        byte[] pad = new byte[padLength];

        // First bit of the padding set to 1
        pad[0] = (byte) 0x80;

        long bits = msgLength << 3;
        int index = padLength - 8;
        for (int i = 0; i < 8; i++) {
            pad[index++] = (byte) (bits >>> (i << 3));
        }

        return pad;
    }

    /** 
     * Process one 64-byte block. Algorithm is constituted by three rounds.
     * Note that F, G and H functions were inlined for improved performance.
     * 
     * @param in the byte array to process
     * @param offset the offset at which the 64-byte block is stored
     */
    private void process(byte[] in, int offset) {
        // Save previous state.
        int aa = a;
        int bb = b;
        int cc = c;
        int dd = d;

        // Copy the block to process into X array
        int[] X = new int[16];
        for (int i = 0; i < 16; i++) {
            X[i] = (in[offset++] & 0xff) | (in[offset++] & 0xff) << 8
                    | (in[offset++] & 0xff) << 16 | (in[offset++] & 0xff) << 24;
        }

        // Round 1
        a += ((b & c) | (~b & d)) + X[0];
        a = a << 3 | a >>> (32 - 3);
        d += ((a & b) | (~a & c)) + X[1];
        d = d << 7 | d >>> (32 - 7);
        c += ((d & a) | (~d & b)) + X[2];
        c = c << 11 | c >>> (32 - 11);
        b += ((c & d) | (~c & a)) + X[3];
        b = b << 19 | b >>> (32 - 19);
        a += ((b & c) | (~b & d)) + X[4];
        a = a << 3 | a >>> (32 - 3);
        d += ((a & b) | (~a & c)) + X[5];
        d = d << 7 | d >>> (32 - 7);
        c += ((d & a) | (~d & b)) + X[6];
        c = c << 11 | c >>> (32 - 11);
        b += ((c & d) | (~c & a)) + X[7];
        b = b << 19 | b >>> (32 - 19);
        a += ((b & c) | (~b & d)) + X[8];
        a = a << 3 | a >>> (32 - 3);
        d += ((a & b) | (~a & c)) + X[9];
        d = d << 7 | d >>> (32 - 7);
        c += ((d & a) | (~d & b)) + X[10];
        c = c << 11 | c >>> (32 - 11);
        b += ((c & d) | (~c & a)) + X[11];
        b = b << 19 | b >>> (32 - 19);
        a += ((b & c) | (~b & d)) + X[12];
        a = a << 3 | a >>> (32 - 3);
        d += ((a & b) | (~a & c)) + X[13];
        d = d << 7 | d >>> (32 - 7);
        c += ((d & a) | (~d & b)) + X[14];
        c = c << 11 | c >>> (32 - 11);
        b += ((c & d) | (~c & a)) + X[15];
        b = b << 19 | b >>> (32 - 19);

        // Round 2
        a += ((b & (c | d)) | (c & d)) + X[0] + 0x5a827999;
        a = a << 3 | a >>> (32 - 3);
        d += ((a & (b | c)) | (b & c)) + X[4] + 0x5a827999;
        d = d << 5 | d >>> (32 - 5);
        c += ((d & (a | b)) | (a & b)) + X[8] + 0x5a827999;
        c = c << 9 | c >>> (32 - 9);
        b += ((c & (d | a)) | (d & a)) + X[12] + 0x5a827999;
        b = b << 13 | b >>> (32 - 13);
        a += ((b & (c | d)) | (c & d)) + X[1] + 0x5a827999;
        a = a << 3 | a >>> (32 - 3);
        d += ((a & (b | c)) | (b & c)) + X[5] + 0x5a827999;
        d = d << 5 | d >>> (32 - 5);
        c += ((d & (a | b)) | (a & b)) + X[9] + 0x5a827999;
        c = c << 9 | c >>> (32 - 9);
        b += ((c & (d | a)) | (d & a)) + X[13] + 0x5a827999;
        b = b << 13 | b >>> (32 - 13);
        a += ((b & (c | d)) | (c & d)) + X[2] + 0x5a827999;
        a = a << 3 | a >>> (32 - 3);
        d += ((a & (b | c)) | (b & c)) + X[6] + 0x5a827999;
        d = d << 5 | d >>> (32 - 5);
        c += ((d & (a | b)) | (a & b)) + X[10] + 0x5a827999;
        c = c << 9 | c >>> (32 - 9);
        b += ((c & (d | a)) | (d & a)) + X[14] + 0x5a827999;
        b = b << 13 | b >>> (32 - 13);
        a += ((b & (c | d)) | (c & d)) + X[3] + 0x5a827999;
        a = a << 3 | a >>> (32 - 3);
        d += ((a & (b | c)) | (b & c)) + X[7] + 0x5a827999;
        d = d << 5 | d >>> (32 - 5);
        c += ((d & (a | b)) | (a & b)) + X[11] + 0x5a827999;
        c = c << 9 | c >>> (32 - 9);
        b += ((c & (d | a)) | (d & a)) + X[15] + 0x5a827999;
        b = b << 13 | b >>> (32 - 13);

        // Round 3
        a += (b ^ c ^ d) + X[0] + 0x6ed9eba1;
        a = a << 3 | a >>> (32 - 3);
        d += (a ^ b ^ c) + X[8] + 0x6ed9eba1;
        d = d << 9 | d >>> (32 - 9);
        c += (d ^ a ^ b) + X[4] + 0x6ed9eba1;
        c = c << 11 | c >>> (32 - 11);
        b += (c ^ d ^ a) + X[12] + 0x6ed9eba1;
        b = b << 15 | b >>> (32 - 15);
        a += (b ^ c ^ d) + X[2] + 0x6ed9eba1;
        a = a << 3 | a >>> (32 - 3);
        d += (a ^ b ^ c) + X[10] + 0x6ed9eba1;
        d = d << 9 | d >>> (32 - 9);
        c += (d ^ a ^ b) + X[6] + 0x6ed9eba1;
        c = c << 11 | c >>> (32 - 11);
        b += (c ^ d ^ a) + X[14] + 0x6ed9eba1;
        b = b << 15 | b >>> (32 - 15);
        a += (b ^ c ^ d) + X[1] + 0x6ed9eba1;
        a = a << 3 | a >>> (32 - 3);
        d += (a ^ b ^ c) + X[9] + 0x6ed9eba1;
        d = d << 9 | d >>> (32 - 9);
        c += (d ^ a ^ b) + X[5] + 0x6ed9eba1;
        c = c << 11 | c >>> (32 - 11);
        b += (c ^ d ^ a) + X[13] + 0x6ed9eba1;
        b = b << 15 | b >>> (32 - 15);
        a += (b ^ c ^ d) + X[3] + 0x6ed9eba1;
        a = a << 3 | a >>> (32 - 3);
        d += (a ^ b ^ c) + X[11] + 0x6ed9eba1;
        d = d << 9 | d >>> (32 - 9);
        c += (d ^ a ^ b) + X[7] + 0x6ed9eba1;
        c = c << 11 | c >>> (32 - 11);
        b += (c ^ d ^ a) + X[15] + 0x6ed9eba1;
        b = b << 15 | b >>> (32 - 15);

        //Update state.
        a += aa;
        b += bb;
        c += cc;
        d += dd;
    }
}
