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
package org.apache.mina.proxy;

import static org.apache.mina.proxy.utils.ByteUtilities.asHex;
import static org.junit.Assert.assertEquals;

import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;

import org.apache.mina.proxy.handlers.http.ntlm.NTLMResponses;
import org.apache.mina.proxy.handlers.http.ntlm.NTLMUtilities;
import org.apache.mina.proxy.utils.ByteUtilities;
import org.apache.mina.proxy.utils.MD4Provider;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NTLMTest.java - JUNIT tests of the NTLM authentication mechanism.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @since MINA 2.0.0-M3
 */
public class NTLMTest {
    private final static Logger logger = LoggerFactory
            .getLogger(NTLMTest.class);

    static {
        if (Security.getProvider("MINA") == null) {
            Security.addProvider(new MD4Provider());
        }
    }
    
    /**
     * Tests bytes manipulations.
     */
    @Test
    public void testEncoding() throws UnsupportedEncodingException {
        assertEquals("d204", asHex(ByteUtilities.writeShort((short) 1234)));
        assertEquals("d2040000", asHex(ByteUtilities.writeInt(1234)));
        assertEquals("01000000", asHex(ByteUtilities.writeInt((short) 1)));
        assertEquals("4e544c4d53535000", asHex(NTLMUtilities.NTLM_SIGNATURE));

        assertEquals("680065006c006c006f00", asHex(ByteUtilities
                .getUTFStringAsByteArray("hello")));
        assertEquals("48454c4c4f", asHex(ByteUtilities
                .getOEMStringAsByteArray("HELLO")));
    }

    /**
     * Tests conversions to and from network byte order.
     */
    @Test
    public void testMethods() {
        byte[] buf = new byte[4];
        ByteUtilities.intToNetworkByteOrder(1234, buf, 0, 4);
        assertEquals("000004d2", asHex(buf));
        assertEquals(1234, ByteUtilities.networkByteOrderToInt(buf, 0, 4));
    }

    /**
     * Tests security buffers.
     */
    @Test
    public void testSecurityBuffer() {
        byte[] secBuf = new byte[8];
        NTLMUtilities.writeSecurityBuffer((short) 1234, (short) 1234, 4321,
                secBuf, 0);
        assertEquals("d204d204e1100000", asHex(secBuf));
    }

    /**
     * Tests creating a type 1 message.
     */
    @Test
    public void testType1Message() {
        int customFlags = NTLMUtilities.FLAG_NEGOTIATE_UNICODE
                | NTLMUtilities.FLAG_NEGOTIATE_OEM
                | NTLMUtilities.FLAG_NEGOTIATE_NTLM
                | NTLMUtilities.FLAG_REQUEST_SERVER_AUTH_REALM
                | NTLMUtilities.FLAG_NEGOTIATE_DOMAIN_SUPPLIED
                | NTLMUtilities.FLAG_NEGOTIATE_WORKSTATION_SUPPLIED;

        byte[] osVer = new byte[8];
        NTLMUtilities
                .writeOSVersion((byte) 5, (byte) 0, (short) 2195, osVer, 0);

        String msgType1 = asHex(NTLMUtilities.createType1Message("WORKSTATION",
                "DOMAIN", customFlags, osVer));
        assertEquals(
                "4e544c4d53535000010000000732000006000600330000000b000b0028000000"
                        + "050093080000000f574f524b53544154494f4e444f4d41494e",
                msgType1);

        assertEquals("050093080000000f", asHex(osVer));
        
        //Microsoft Windows XP [version 5.1.2600]
        String os = System.getProperty("os.name");
        if (os != null && os.toUpperCase().contains("WINDOWS") && 
                "5.1".equals(System.getProperty("os.version"))) {
            String hex = asHex(NTLMUtilities.getOsVersion());
            assertEquals("0501", hex.substring(0, 4));
            assertEquals(16, hex.length());
        }
    }

    /**
     * Tests creating a type 3 message.
     * WARNING: Will silently fail if no MD4 digest provider is available.
     */
    @Test
    public void testType3Message() throws Exception {
        try {
            MessageDigest.getInstance("MD4");
        } catch (NoSuchAlgorithmException ex) {
            logger.warn("No MD4 digest provider found !");
            return;
        }

        int flags = 0x00000001 | 0x00000200 | 0x00010000 | 0x00800000;
        String msg = "4e544c4d53535000020000000c000c003000000001028100"
                + "0123456789abcdef0000000000000000620062003c000000"
                + "44004f004d00410049004e0002000c0044004f004d004100"
                + "49004e0001000c0053004500520056004500520004001400"
                + "64006f006d00610069006e002e0063006f006d0003002200"
                + "7300650072007600650072002e0064006f006d0061006900"
                + "6e002e0063006f006d0000000000";

        byte[] challengePacket = ByteUtilities.asByteArray(msg);
        int serverFlags = NTLMUtilities
                .extractFlagsFromType2Message(challengePacket);
        assertEquals(flags, serverFlags);

        NTLMUtilities
                .printTargetInformationBlockFromType2Message(challengePacket,
                        serverFlags, new PrintWriter(System.out, true));

        byte[] osVer = new byte[8];
        NTLMUtilities
                .writeOSVersion((byte) 5, (byte) 0, (short) 2195, osVer, 0);

        byte[] challenge = NTLMUtilities
                .extractChallengeFromType2Message(challengePacket);
        assertEquals("0123456789abcdef", asHex(challenge));

        String expectedTargetInfoBlock = "02000c0044004f004d00410049004e00"
                + "01000c00530045005200560045005200"
                + "0400140064006f006d00610069006e00"
                + "2e0063006f006d000300220073006500"
                + "72007600650072002e0064006f006d00"
                + "610069006e002e0063006f006d000000" + "0000";

        byte[] targetInfo = NTLMUtilities.extractTargetInfoFromType2Message(
                challengePacket, null);
        assertEquals(expectedTargetInfoBlock, asHex(targetInfo));

        assertEquals("DOMAIN", NTLMUtilities.extractTargetNameFromType2Message(
                challengePacket, new Integer(serverFlags)));

        serverFlags = 0x00000001 | 0x00000200;
        String msgType3 = asHex(NTLMUtilities.createType3Message("user",
                "SecREt01", challenge, "DOMAIN", "WORKSTATION", serverFlags,
                null));

        String expected = "4e544c4d5353500003000000180018006a00000018001800"
                + "820000000c000c0040000000080008004c00000016001600"
                + "54000000000000009a0000000102000044004f004d004100"
                + "49004e00750073006500720057004f0052004b0053005400"
                + "4100540049004f004e00c337cd5cbd44fc9782a667af6d42"
                + "7c6de67c20c2d3e77c5625a98c1c31e81847466b29b2df46"
                + "80f39958fb8c213a9cc6";
        assertEquals(expected, msgType3);
    }

    /**
     * Tests flags manipulations.
     */
    @Test
    public void testFlags() {
        int flags = NTLMUtilities.FLAG_NEGOTIATE_UNICODE
                | NTLMUtilities.FLAG_REQUEST_SERVER_AUTH_REALM
                | NTLMUtilities.FLAG_NEGOTIATE_NTLM
                | NTLMUtilities.FLAG_NEGOTIATE_ALWAYS_SIGN;

        int flags2 = NTLMUtilities.FLAG_NEGOTIATE_UNICODE
                | NTLMUtilities.FLAG_REQUEST_SERVER_AUTH_REALM
                | NTLMUtilities.FLAG_NEGOTIATE_NTLM;

        assertEquals(flags2, flags
                & (~NTLMUtilities.FLAG_NEGOTIATE_ALWAYS_SIGN));
        assertEquals(flags2, flags2
                & (~NTLMUtilities.FLAG_NEGOTIATE_ALWAYS_SIGN));
        assertEquals("05820000", asHex(ByteUtilities.writeInt(flags)));

        byte[] testFlags = ByteUtilities.asByteArray("7F808182");
        assertEquals("7f808182", asHex(testFlags));
        ByteUtilities.changeByteEndianess(testFlags, 0, 4);
        assertEquals("807f8281", asHex(testFlags));
        ByteUtilities.changeByteEndianess(testFlags, 0, 4);
        ByteUtilities.changeWordEndianess(testFlags, 0, 4);
        assertEquals("8281807f", asHex(testFlags));
    }

    /**
     * Tests response computing.
     * WARNING: Will silently fail if no MD4 digest provider is available.
     */
    @Test
    public void testResponses() throws Exception {
        try {
            MessageDigest.getInstance("MD4");
        } catch (NoSuchAlgorithmException ex) {
            logger.warn("No MD4 digest provider found !");
            return;
        }

        String LMResponse = "c337cd5cbd44fc9782a667af6d427c6de67c20c2d3e77c56";

        assertEquals(LMResponse, asHex(NTLMResponses.getLMResponse("SecREt01",
                ByteUtilities.asByteArray("0123456789abcdef"))));

        String NTLMResponse = "25a98c1c31e81847466b29b2df4680f39958fb8c213a9cc6";

        assertEquals(NTLMResponse, asHex(NTLMResponses.getNTLMResponse(
                "SecREt01", ByteUtilities.asByteArray("0123456789abcdef"))));

        String LMv2Response = "d6e6152ea25d03b7c6ba6629c2d6aaf0ffffff0011223344";

        assertEquals(LMv2Response, asHex(NTLMResponses.getLMv2Response(
                "DOMAIN", "user", "SecREt01", ByteUtilities
                        .asByteArray("0123456789abcdef"), ByteUtilities
                        .asByteArray("ffffff0011223344"))));

        String NTLM2Response = "10d550832d12b2ccb79d5ad1f4eed3df82aca4c3681dd455";

        assertEquals(NTLM2Response, asHex(NTLMResponses
                .getNTLM2SessionResponse("SecREt01", ByteUtilities
                        .asByteArray("0123456789abcdef"), ByteUtilities
                        .asByteArray("ffffff0011223344"))));

        String NTLMv2Response = "cbabbca713eb795d04c97abc01ee4983"
                + "01010000000000000090d336b734c301"
                + "ffffff00112233440000000002000c00"
                + "44004f004d00410049004e0001000c00"
                + "53004500520056004500520004001400"
                + "64006f006d00610069006e002e006300"
                + "6f006d00030022007300650072007600"
                + "650072002e0064006f006d0061006900"
                + "6e002e0063006f006d00000000000000" + "0000";

        String targetInformation = "02000c0044004f004d00410049004e00"
                + "01000c00530045005200560045005200"
                + "0400140064006f006d00610069006e00"
                + "2e0063006f006d000300220073006500"
                + "72007600650072002e0064006f006d00"
                + "610069006e002e0063006f006d000000" + "0000";

        assertEquals(NTLMv2Response, asHex(NTLMResponses.getNTLMv2Response(
                "DOMAIN", "user", "SecREt01", ByteUtilities
                        .asByteArray(targetInformation), ByteUtilities
                        .asByteArray("0123456789abcdef"), ByteUtilities
                        .asByteArray("ffffff0011223344"), 1055844000000L)));
    }
}