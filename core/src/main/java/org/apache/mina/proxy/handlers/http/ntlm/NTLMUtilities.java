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
package org.apache.mina.proxy.handlers.http.ntlm;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.StringTokenizer;

import org.apache.mina.proxy.utils.ByteUtilities;

/**
 * NTLMUtilities.java - NTLM functions used for authentication and unit testing.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 * @since MINA 2.0.0-M3
 */
public class NTLMUtilities implements NTLMConstants {
    /**
     * @see #writeSecurityBuffer(short, short, int, byte[], int)
     */
    public final static byte[] writeSecurityBuffer(short length,
            int bufferOffset) {
        byte[] b = new byte[8];
        writeSecurityBuffer(length, length, bufferOffset, b, 0);
        return b;
    }

    /**
     * Writes a security buffer to the given array <code>b</code> at offset 
     * <code>offset</code>.
     * 
     * @param length the length of the security buffer
     * @param allocated the allocated space for the security buffer (should be
     * greater or equal to <code>length</code>
     * @param bufferOffset the offset since the beginning of the <code>b</code>
     * buffer where the buffer iswritten
     * @param b the buffer in which we write the security buffer
     * @param offset the offset at which to write to the buffer
     */
    public final static void writeSecurityBuffer(short length, short allocated,
            int bufferOffset, byte[] b, int offset) {
        ByteUtilities.writeShort(length, b, offset);
        ByteUtilities.writeShort(allocated, b, offset + 2);
        ByteUtilities.writeInt(bufferOffset, b, offset + 4);
    }

    public final static void writeOSVersion(byte majorVersion,
            byte minorVersion, short buildNumber, byte[] b, int offset) {
        b[offset] = majorVersion;
        b[offset + 1] = minorVersion;
        b[offset + 2] = (byte) buildNumber;
        b[offset + 3] = (byte) (buildNumber >> 8);
        b[offset + 4] = 0;
        b[offset + 5] = 0;
        b[offset + 6] = 0;
        b[offset + 7] = 0x0F;
    }

    /**
     * Tries to return a valid Os version on windows systems.
     */
    public final static byte[] getOsVersion() {
        String os = System.getProperty("os.name");
        if (os == null || !os.toUpperCase().contains("WINDOWS")) {
            return DEFAULT_OS_VERSION;
        } else {
            byte[] osVer = new byte[8];
            try {
                Process pr = Runtime.getRuntime().exec("cmd /C ver");
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(pr.getInputStream()));
                pr.waitFor();
                
                String line;
                do {
                	  line = reader.readLine();
                } while ((line != null) && (line.length() != 0));
                
                int pos = line.toLowerCase().indexOf("version");

                if (pos == -1) {
                    throw new NullPointerException();
                }

                pos += 8;
                line = line.substring(pos, line.indexOf(']'));
                StringTokenizer tk = new StringTokenizer(line, ".");
                if (tk.countTokens() != 3) {
                    throw new NullPointerException();
                }

                writeOSVersion(Byte.parseByte(tk.nextToken()), Byte
                        .parseByte(tk.nextToken()), Short.parseShort(tk
                        .nextToken()), osVer, 0);
            } catch (Exception ex) {
                try {
                    String version = System.getProperty("os.version");
                    writeOSVersion(Byte.parseByte(version.substring(0, 1)),
                            Byte.parseByte(version.substring(2, 3)), (short) 0,
                            osVer, 0);
                } catch (Exception ex2) {
                    return DEFAULT_OS_VERSION;
                }
            }
            return osVer;
        }
    }

    /**
     * see http://davenport.sourceforge.net/ntlm.html#theType1Message
     * 
     * @param workStation the workstation name
     * @param domain the domain name
     * @param customFlags custom flags, if null then 
     * <code>NTLMConstants.DEFAULT_CONSTANTS</code> is used
     * @param osVersion the os version of the client, if null then 
     * <code>NTLMConstants.DEFAULT_OS_VERSION</code> is used
     * @return
     */
    public final static byte[] createType1Message(String workStation,
            String domain, Integer customFlags, byte[] osVersion) {
        byte[] msg = null;

        if (osVersion != null && osVersion.length != 8) {
            throw new IllegalArgumentException(
                    "osVersion parameter should be a 8 byte wide array");
        }

        if (workStation == null || domain == null) {
            throw new NullPointerException(
                    "workStation and domain must be non null");
        }

        int flags = customFlags != null ? customFlags
                | FLAG_NEGOTIATE_WORKSTATION_SUPPLIED
                | FLAG_NEGOTIATE_DOMAIN_SUPPLIED : DEFAULT_FLAGS;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            baos.write(NTLM_SIGNATURE);
            baos.write(ByteUtilities.writeInt(MESSAGE_TYPE_1));
            baos.write(ByteUtilities.writeInt(flags));

            byte[] domainData = ByteUtilities.getOEMStringAsByteArray(domain);
            byte[] workStationData = ByteUtilities
                    .getOEMStringAsByteArray(workStation);

            int pos = (osVersion != null) ? 40 : 32;
            baos.write(writeSecurityBuffer((short) domainData.length, pos
                    + workStationData.length));
            baos
                    .write(writeSecurityBuffer((short) workStationData.length,
                            pos));

            if (osVersion != null) {
                baos.write(osVersion);
            }

            // Order is not mandatory since a pointer is given in the security buffers
            baos.write(workStationData);
            baos.write(domainData);

            msg = baos.toByteArray();
            baos.close();
        } catch (IOException e) {
            return null;
        }

        return msg;
    }

    /**
     * Write a security buffer and returns the pointer of the position 
     * where to write the next security buffer.
     * 
     * @param baos the stream where the security buffer is written
     * @param len the length of the security buffer 
     * @param pointer the position where the security buffer can be written
     * @return the position where the next security buffer will be written
     * @throws IOException
     */
    public final static int writeSecurityBufferAndUpdatePointer(
            ByteArrayOutputStream baos, short len, int pointer)
            throws IOException {
        baos.write(writeSecurityBuffer(len, pointer));
        return pointer + len;
    }

    public final static byte[] extractChallengeFromType2Message(byte[] msg) {
        byte[] challenge = new byte[8];
        System.arraycopy(msg, 24, challenge, 0, 8);
        return challenge;
    }

    public final static int extractFlagsFromType2Message(byte[] msg) {
        byte[] flagsBytes = new byte[4];

        System.arraycopy(msg, 20, flagsBytes, 0, 4);
        ByteUtilities.changeWordEndianess(flagsBytes, 0, 4);

        return ByteUtilities.makeIntFromByte4(flagsBytes);
    }

    public final static String extractTargetNameFromType2Message(byte[] msg,
            Integer msgFlags) throws UnsupportedEncodingException {
        byte[] targetName = null;

        // Read security buffer
        byte[] securityBuffer = new byte[8];

        System.arraycopy(msg, 12, securityBuffer, 0, 8);
        ByteUtilities.changeWordEndianess(securityBuffer, 0, 8);
        int length = ByteUtilities.makeIntFromByte2(securityBuffer);
        int offset = ByteUtilities.makeIntFromByte4(securityBuffer, 4);

        targetName = new byte[length];
        System.arraycopy(msg, offset, targetName, 0, length);

        int flags = msgFlags == null ? extractFlagsFromType2Message(msg)
                : msgFlags;
        if (ByteUtilities.isFlagSet(flags, FLAG_NEGOTIATE_UNICODE)) {
            return new String(targetName, "UTF-16LE");
        } else {
            return new String(targetName, "ASCII");
        }
    }

    public final static byte[] extractTargetInfoFromType2Message(byte[] msg,
            Integer msgFlags) {
        int flags = msgFlags == null ? extractFlagsFromType2Message(msg)
                : msgFlags;
        byte[] targetInformationBlock = null;

        if (!ByteUtilities.isFlagSet(flags, FLAG_NEGOTIATE_TARGET_INFO))
            return null;

        int pos = 40; //isFlagSet(flags, FLAG_NEGOTIATE_LOCAL_CALL) ? 40 : 32;

        // Read security buffer
        byte[] securityBuffer = new byte[8];

        System.arraycopy(msg, pos, securityBuffer, 0, 8);
        ByteUtilities.changeWordEndianess(securityBuffer, 0, 8);
        int length = ByteUtilities.makeIntFromByte2(securityBuffer);
        int offset = ByteUtilities.makeIntFromByte4(securityBuffer, 4);

        targetInformationBlock = new byte[length];
        System.arraycopy(msg, offset, targetInformationBlock, 0, length);

        return targetInformationBlock;
    }

    public final static void printTargetInformationBlockFromType2Message(
            byte[] msg, Integer msgFlags, PrintWriter out)
            throws UnsupportedEncodingException {
        int flags = msgFlags == null ? extractFlagsFromType2Message(msg)
                : msgFlags;

        byte[] infoBlock = extractTargetInfoFromType2Message(msg, flags);
        if (infoBlock == null) {
            out.println("No target information block found !");
        } else {
            int pos = 0;
            while (infoBlock[pos] != 0) {
                out.print("---\nType " + infoBlock[pos] + ": ");
                switch (infoBlock[pos]) {
                case 1:
                    out.println("Server name");
                    break;
                case 2:
                    out.println("Domain name");
                    break;
                case 3:
                    out.println("Fully qualified DNS hostname");
                    break;
                case 4:
                    out.println("DNS domain name");
                    break;
                case 5:
                    out.println("Parent DNS domain name");
                    break;
                }
                byte[] len = new byte[2];
                System.arraycopy(infoBlock, pos + 2, len, 0, 2);
                ByteUtilities.changeByteEndianess(len, 0, 2);

                int length = ByteUtilities.makeIntFromByte2(len, 0);
                out.println("Length: " + length + " bytes");
                out.print("Data: ");
                if (ByteUtilities.isFlagSet(flags, FLAG_NEGOTIATE_UNICODE)) {
                    out.println(new String(infoBlock, pos + 4, length,
                            "UTF-16LE"));
                } else {
                    out
                            .println(new String(infoBlock, pos + 4, length,
                                    "ASCII"));
                }
                pos += 4 + length;
                out.flush();
            }
        }
    }

    /**
     * http://davenport.sourceforge.net/ntlm.html#theType3Message
     */
    public final static byte[] createType3Message(String user, String password,
            byte[] challenge, String target, String workstation,
            Integer serverFlags, byte[] osVersion) {
        byte[] msg = null;

        if (challenge == null || challenge.length != 8) {
            throw new IllegalArgumentException(
                    "challenge[] should be a 8 byte wide array");
        }

        if (osVersion != null && osVersion.length != 8) {
            throw new IllegalArgumentException(
                    "osVersion should be a 8 byte wide array");
        }

        //TOSEE breaks tests
        /*int flags = serverFlags != null ? serverFlags | 
        		FLAG_NEGOTIATE_WORKSTATION_SUPPLIED | 
        		FLAG_NEGOTIATE_DOMAIN_SUPPLIED : DEFAULT_FLAGS;*/
        int flags = serverFlags != null ? serverFlags : DEFAULT_FLAGS;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            baos.write(NTLM_SIGNATURE);
            baos.write(ByteUtilities.writeInt(MESSAGE_TYPE_3));

            byte[] dataLMResponse = NTLMResponses.getLMResponse(password,
                    challenge);
            byte[] dataNTLMResponse = NTLMResponses.getNTLMResponse(password,
                    challenge);

            boolean useUnicode = ByteUtilities.isFlagSet(flags,
                    FLAG_NEGOTIATE_UNICODE);
            byte[] targetName = ByteUtilities.encodeString(target, useUnicode);
            byte[] userName = ByteUtilities.encodeString(user, useUnicode);
            byte[] workstationName = ByteUtilities.encodeString(workstation,
                    useUnicode);

            int pos = osVersion != null ? 72 : 64;
            int responsePos = pos + targetName.length + userName.length
                    + workstationName.length;
            responsePos = writeSecurityBufferAndUpdatePointer(baos,
                    (short) dataLMResponse.length, responsePos);
            writeSecurityBufferAndUpdatePointer(baos,
                    (short) dataNTLMResponse.length, responsePos);
            pos = writeSecurityBufferAndUpdatePointer(baos,
                    (short) targetName.length, pos);
            pos = writeSecurityBufferAndUpdatePointer(baos,
                    (short) userName.length, pos);
            writeSecurityBufferAndUpdatePointer(baos,
                    (short) workstationName.length, pos);

            /**
            LM/LMv2 Response security buffer 
            20 NTLM/NTLMv2 Response security buffer 
            28 Target Name security buffer 
            36 User Name security buffer 
            44 Workstation Name security buffer 
            (52) Session Key (optional) security buffer 
            (60) Flags (optional) long 
            (64) OS Version Structure (Optional) 8 bytes
            **/

            baos.write(new byte[] { 0, 0, 0, 0, (byte) 0x9a, 0, 0, 0 }); // Session Key Security Buffer ??!
            baos.write(ByteUtilities.writeInt(flags));

            if (osVersion != null) {
                baos.write(osVersion);
            }
            //else
            //	baos.write(DEFAULT_OS_VERSION);

            // Order is not mandatory since a pointer is given in the security buffers
            baos.write(targetName);
            baos.write(userName);
            baos.write(workstationName);

            baos.write(dataLMResponse);
            baos.write(dataNTLMResponse);

            msg = baos.toByteArray();
            baos.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return msg;
    }
}