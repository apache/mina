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
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
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
     * <code>offset</code>. A security buffer defines a pointer to an area 
     * in the data that defines some data with a variable length. This allows
     * to have a semi-fixed length header thus making a little bit easier 
     * the decoding process in the NTLM protocol.
     * 
     * @param length the length of the security buffer
     * @param allocated the allocated space for the security buffer (should be
     * greater or equal to <code>length</code>
     * @param bufferOffset the offset from the main array where the currently
     * defined security buffer will be written
     * @param b the buffer in which we write the security buffer
     * @param offset the offset at which to write to the b buffer
     */
    public final static void writeSecurityBuffer(short length, short allocated,
            int bufferOffset, byte[] b, int offset) {
        ByteUtilities.writeShort(length, b, offset);
        ByteUtilities.writeShort(allocated, b, offset + 2);
        ByteUtilities.writeInt(bufferOffset, b, offset + 4);
    }

    /**
     * Writes the Windows OS version passed in as three byte values
     * (majorVersion.minorVersion.buildNumber) to the given byte array
     * at <code>offset</code>.
     * 
     * @param majorVersion the major version number
     * @param minorVersion the minor version number
     * @param buildNumber the build number
     * @param b the target byte array 
     * @param offset the offset at which to write in the array
     */
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
     * Tries to return a valid OS version on Windows systems. If it fails to 
     * do so or if we're running on another OS then a fake Windows XP OS 
     * version is returned because the protocol uses it.
     * 
     * @return a NTLM OS version byte buffer
     */
    public final static byte[] getOsVersion() {
        String os = System.getProperty("os.name");
        
        if (os == null || !os.toUpperCase().contains("WINDOWS")) {
            return DEFAULT_OS_VERSION;
        }
        
        byte[] osVer = new byte[8];

        // Let's enclose the code by a try...catch in order to
        // manage incorrect strings. In this case, we will generate 
        // an exception and deal with the special cases.
        try {
            Process pr = Runtime.getRuntime().exec("cmd /C ver");
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(pr.getInputStream()));
            pr.waitFor();
            
            String line;
        
            // We loop as we may have blank lines.
            do {
                  line = reader.readLine();
            } while ((line != null) && (line.length() != 0));
            
            reader.close();

            // If line is null, we must not go any farther
            if (line == null) {
                // Throw an exception to jump into the catch() part
                throw new Exception();
            }

            // The command line should return a response like :
            // Microsoft Windows XP [version 5.1.2600]
            int pos = line.toLowerCase().indexOf("version");

            if (pos == -1) {
                // Throw an exception to jump into the catch() part
                throw new Exception();
            }

            pos += 8;
            line = line.substring(pos, line.indexOf(']'));
            StringTokenizer tk = new StringTokenizer(line, ".");

            if (tk.countTokens() != 3) {
                // Throw an exception to jump into the catch() part
                throw new Exception();
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

    /**
     * see http://davenport.sourceforge.net/ntlm.html#theType1Message
     * 
     * @param workStation the workstation name
     * @param domain the domain name
     * @param customFlags custom flags, if null then 
     * <code>NTLMConstants.DEFAULT_CONSTANTS</code> is used
     * @param osVersion the os version of the client, if null then 
     * <code>NTLMConstants.DEFAULT_OS_VERSION</code> is used
     * @return the type 1 message
     */
    public final static byte[] createType1Message(String workStation,
            String domain, Integer customFlags, byte[] osVersion) {
        byte[] msg = null;

        if (osVersion != null && osVersion.length != 8) {
            throw new IllegalArgumentException(
                    "osVersion parameter should be a 8 byte wide array");
        }

        if (workStation == null || domain == null) {
            throw new IllegalArgumentException(
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
     * Writes a security buffer and returns the pointer of the position 
     * where to write the next security buffer.
     * 
     * @param baos the stream where the security buffer is written
     * @param len the length of the security buffer 
     * @param pointer the position where the security buffer can be written
     * @return the position where the next security buffer will be written
     * @throws IOException if writing to the ByteArrayOutputStream fails
     */
    public final static int writeSecurityBufferAndUpdatePointer(
            ByteArrayOutputStream baos, short len, int pointer)
            throws IOException {
        baos.write(writeSecurityBuffer(len, pointer));
        return pointer + len;
    }

    /**
     * Extracts the NTLM challenge from the type 2 message as an 8 byte array.
     * 
     * @param msg the type 2 message byte array
     * @return the challenge
     */
    public final static byte[] extractChallengeFromType2Message(byte[] msg) {
        byte[] challenge = new byte[8];
        System.arraycopy(msg, 24, challenge, 0, 8);
        return challenge;
    }

    /**
     * Extracts the NTLM flags from the type 2 message.
     * 
     * @param msg the type 2 message byte array
     * @return the proxy flags as an int
     */
    public final static int extractFlagsFromType2Message(byte[] msg) {
        byte[] flagsBytes = new byte[4];

        System.arraycopy(msg, 20, flagsBytes, 0, 4);
        ByteUtilities.changeWordEndianess(flagsBytes, 0, 4);

        return ByteUtilities.makeIntFromByte4(flagsBytes);
    }

    /**
     * Reads the byte array described by the security buffer stored at the
     * <code>securityBufferOffset</code> offset.
     * 
     * @param msg the message where to read the security buffer and it's value
     * @param securityBufferOffset the offset at which to read the security buffer
     * @return a new byte array holding the data pointed by the security buffer 
     */
    public final static byte[] readSecurityBufferTarget(
            byte[] msg, int securityBufferOffset) {
        byte[] securityBuffer = new byte[8];

        System.arraycopy(msg, securityBufferOffset, securityBuffer, 0, 8);
        ByteUtilities.changeWordEndianess(securityBuffer, 0, 8);
        int length = ByteUtilities.makeIntFromByte2(securityBuffer);
        int offset = ByteUtilities.makeIntFromByte4(securityBuffer, 4);

        byte[] secBufValue = new byte[length];
        System.arraycopy(msg, offset, secBufValue, 0, length);
        
        return secBufValue;
    }
    
    /**
     * Extracts the target name from the type 2 message.
     * 
     * @param msg the type 2 message byte array
     * @param msgFlags the flags if null then flags are extracted from the 
     * type 2 message
     * @return the target name
     * @throws UnsupportedEncodingException if unable to use the 
     * needed UTF-16LE or ASCII charsets 
     */
    public final static String extractTargetNameFromType2Message(byte[] msg,
            Integer msgFlags) throws UnsupportedEncodingException {
        // Read the security buffer to determine where the target name
        // is stored and what it's length is
        byte[] targetName = readSecurityBufferTarget(msg, 12);

        // now we convert it to a string
        int flags = msgFlags == null ? extractFlagsFromType2Message(msg)
                : msgFlags;
        if (ByteUtilities.isFlagSet(flags, FLAG_NEGOTIATE_UNICODE)) {
            return new String(targetName, "UTF-16LE");
        }
        
        return new String(targetName, "ASCII");
    }

    /**
     * Extracts the target information block from the type 2 message.
     * 
     * @param msg the type 2 message byte array
     * @param msgFlags the flags if null then flags are extracted from the 
     * type 2 message
     * @return the target info
     */
    public final static byte[] extractTargetInfoFromType2Message(byte[] msg,
            Integer msgFlags) {
        int flags = msgFlags == null ? extractFlagsFromType2Message(msg)
                : msgFlags;

        if (!ByteUtilities.isFlagSet(flags, FLAG_NEGOTIATE_TARGET_INFO))
            return null;

        int pos = 40; //isFlagSet(flags, FLAG_NEGOTIATE_LOCAL_CALL) ? 40 : 32;

        return readSecurityBufferTarget(msg, pos);
    }

    /**
     * Prints to the {@link PrintWriter} the target information block extracted
     * from the type 2 message.
     * 
     * @param msg the type 2 message
     * @param msgFlags the flags if null then flags are extracted from the 
     * type 2 message
     * @param out the output target for the information
     * @throws UnsupportedEncodingException if unable to use the 
     * needed UTF-16LE or ASCII charsets 
     */
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
                    out.println(new String(infoBlock, pos + 4, length, "ASCII"));
                }
                pos += 4 + length;
                out.flush();
            }
        }
    }

    /**
     * @see http://davenport.sourceforge.net/ntlm.html#theType3Message
     * 
     * @param user the user name
     * @param password the user password
     * @param challenge the challenge response
     * @param target the target name
     * @param workstation the client workstation's name
     * @param serverFlags the flags set by the client
     * @param osVersion the os version of the client
     * @return the type 3 message
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

            // Session Key Security Buffer ??!
            baos.write(new byte[] { 0, 0, 0, 0, (byte) 0x9a, 0, 0, 0 });
            
            baos.write(ByteUtilities.writeInt(flags));

            if (osVersion != null) {
                baos.write(osVersion);
            }

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
