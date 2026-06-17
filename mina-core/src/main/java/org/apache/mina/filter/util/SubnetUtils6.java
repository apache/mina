/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*      https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.mina.filter.util;

import java.math.BigInteger;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
* Performs subnet calculations given an IPv6 network address and a prefix length.
* <p>
* This is the IPv6 equivalent of {@link SubnetUtils}. Addresses are parsed and formatted
* using {@link InetAddress}, which accepts the text representations described in
* <a href="https://datatracker.ietf.org/doc/html/rfc5952">RFC 5952</a>.
* </p>
*
* This class is extracted from Apache commons-net project
* @see SubnetUtils
* @see <a href="https://datatracker.ietf.org/doc/html/rfc5952">RFC 5952 - A Recommendation for IPv6 Address Text Representation</a>
* @since 3.13.0
*/
public class SubnetUtils6 {

    /**
    * Contains IPv6 subnet summary information.
    */
    public final class SubnetInfo {

        private SubnetInfo() { }

        /**
        * Gets the address used to initialize this subnet.
        *
        * @return the address as a string in standard IPv6 format.
        */
        public String getAddress() {
            return format(address);
        }

        /**
        * Gets the count of available addresses in this subnet.
        * <p>
        * For IPv6, this can be astronomically large. A /64 subnet has 2^64 addresses.
        * </p>
        *
        * @return the count of addresses as a BigInteger.
        */
        public BigInteger getAddressCount() {
            // 2^(128 - prefixLength)
            return TWO.pow(NBITS - prefixLength);
        }

        /**
        * Gets the CIDR notation for this subnet.
        *
        * @return the CIDR signature (e.g., "2001:db8::1/64").
        */
        public String getCidrSignature() {
            return format(address) + "/" + prefixLength;
        }

        /**
        * Gets the highest address in this subnet.
        *
        * @return the high address as a string in standard IPv6 format.
        */
        public String getHighAddress() {
            return format(high);
        }

        /**
        * Gets the lowest address in this subnet (the network address).
        *
        * @return the low address as a string in standard IPv6 format.
        */
        public String getLowAddress() {
            return format(network);
        }

        /**
        * Gets the network address for this subnet.
        *
        * @return the network address as a string in standard IPv6 format.
        */
        public String getNetworkAddress() {
            return format(network);
        }

        /**
        * Gets the prefix length for this subnet.
        *
        * @return the prefix length (0-128).
        */
        public int getPrefixLength() {
            return prefixLength;
        }

        /**
        * Tests if the given address is within this subnet range.
        *
        * @param addr the IPv6 address to test (as a BigInteger).
        * @return true if the address is in range.
        */
        public boolean isInRange(final BigInteger addr) {
            if (addr == null) {
                return false;
            }
            return addr.compareTo(network) >= 0 && addr.compareTo(high) <= 0;
        }

        /**
        * Tests if the given address is within this subnet range.
        *
        * @param addr the IPv6 address to test as a byte array (16 bytes).
        * @return true if the address is in range.
        */
        public boolean isInRange(final byte[] addr) {
            if (addr == null || addr.length != 16) {
                return false;
            }
            return isInRange(new BigInteger(1, addr));
        }

        /**
        * Tests if the given address is within this subnet range.
        *
        * @param addr the IPv6 address to test.
        * @return true if the address is in range.
        */
        public boolean isInRange(final Inet6Address addr) {
            if (addr == null) {
                return false;
            }
            return isInRange(addr.getAddress());
        }

        /**
        * Tests if the given address is within this subnet range.
        *
        * @param addr the IPv6 address to test as a string.
        * @return true if the address is in range.
        * @throws IllegalArgumentException if the address cannot be parsed.
        */
        public boolean isInRange(final String addr) {
            return isInRange(toBytes(addr));
        }

        /**
        * Returns a summary of this subnet for debugging.
        *
        * @return a multi-line debug string summarizing this subnet.
        */
        @Override
        public String toString() {
            final StringBuilder buf = new StringBuilder();
            buf.append("CIDR Signature:\t[").append(getCidrSignature()).append("]\n")
            .append("  Network: [").append(getNetworkAddress()).append("]\n")
            .append("  First address: [").append(getLowAddress()).append("]\n")
            .append("  Last address: [").append(getHighAddress()).append("]\n")
            .append("  Address Count: [").append(getAddressCount()).append("]\n");
            return buf.toString();
        }
    }

    private static final int NBITS = 128;
    private static final String PARSE_FAIL = "Could not parse [%s]";
    private static final BigInteger TWO = BigInteger.valueOf(2);
    private static final BigInteger MAX_VALUE = TWO.pow(NBITS).subtract(BigInteger.ONE);

    /**
    * Formats a BigInteger as an IPv6 address string using {@link InetAddress#getHostAddress()}.
    *
    * @param addr the address as a BigInteger.
    * @return the formatted IPv6 address string.
    * @see <a href="https://datatracker.ietf.org/doc/html/rfc5952">RFC 5952</a>
    */
    private static String format(final BigInteger addr) {
        final byte[] bytes = toByteArray16(addr);
        try {
            return InetAddress.getByAddress(bytes).getHostAddress();
        } catch (final UnknownHostException e) {
            // Should never happen with a valid 16-byte array
            throw new IllegalStateException("Unexpected error formatting IPv6 address", e);
        }
    }

    /**
    * Converts a BigInteger to a 16-byte array, padding with leading zeros if necessary.
    *
    * @param value the BigInteger to convert.
    * @return a 16-byte array.
    */
    private static byte[] toByteArray16(final BigInteger value) {
        final byte[] raw = value.toByteArray();
        if (raw.length == 16) {
            return raw;
        }
        final byte[] result = new byte[16];
        if (raw.length > 16) {
            // BigInteger may have a leading sign byte; skip it
            System.arraycopy(raw, raw.length - 16, result, 0, 16);
        } else {
            // Pad with leading zeros
            System.arraycopy(raw, 0, result, 16 - raw.length, raw.length);
        }
        return result;
    }

    /**
    * Parses an IPv6 address string to a byte array.
    *
    * @param address the IPv6 address string.
    * @return the 16-byte representation.
    * @throws IllegalArgumentException if the address cannot be parsed.
    */
    private static byte[] toBytes(final String address) {
        try {
            final InetAddress inetAddr = InetAddress.getByName(address);
            if (inetAddr instanceof Inet6Address) {
                return inetAddr.getAddress();
            }
            throw new IllegalArgumentException(String.format(PARSE_FAIL, address) + " - not an IPv6 address");
        } catch (final UnknownHostException e) {
            throw new IllegalArgumentException(String.format(PARSE_FAIL, address), e);
        }
    }

    private final BigInteger address;
    private final BigInteger high;
    private final BigInteger network;
    private final int prefixLength;

    /**
    * Constructs an instance from a CIDR-notation string, e.g., "2001:db8::1/64".
    *
    * @param cidrNotation a CIDR-notation string, e.g., "2001:db8::1/64".
    * @throws IllegalArgumentException if the parameter is invalid.
    */
    public SubnetUtils6(final String cidrNotation) {
        if (cidrNotation == null) {
            throw new IllegalArgumentException(String.format(PARSE_FAIL, "null") + " - null input");
        }

        final int slashIndex = cidrNotation.indexOf('/');
        if (slashIndex < 0) {
            throw new IllegalArgumentException(String.format(PARSE_FAIL, cidrNotation) + " - missing prefix length");
        }

        final String addressPart = cidrNotation.substring(0, slashIndex);
        final String prefixPart = cidrNotation.substring(slashIndex + 1);

        // Parse and validate prefix length
        try {
            this.prefixLength = Integer.parseInt(prefixPart);
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException(String.format(PARSE_FAIL, cidrNotation) + " - invalid prefix length", e);
        }

        if (this.prefixLength < 0 || this.prefixLength > NBITS) {
            throw new IllegalArgumentException(String.format(PARSE_FAIL, cidrNotation) +
            " - prefix length must be between 0 and " + NBITS);
        }

        // Parse and validate IPv6 address
        final byte[] addressBytes = toBytes(addressPart);
        this.address = new BigInteger(1, addressBytes);

        // Create netmask: prefixLength 1-bits followed by (128 - prefixLength) 0-bits
        final BigInteger netmask;
        if (this.prefixLength == 0) {
            netmask = BigInteger.ZERO;
        } else {
            netmask = MAX_VALUE.shiftLeft(NBITS - this.prefixLength).and(MAX_VALUE);
        }

        // Calculate network address
        this.network = this.address.and(netmask);

        // Calculate the highest address in the range
        final BigInteger hostmask = MAX_VALUE.xor(netmask);
        this.high = this.network.or(hostmask);
    }

    /**
    * Constructs an instance from an IPv6 address and prefix length.
    *
    * @param address      an IPv6 address, e.g., "2001:db8::1".
    * @param prefixLength the prefix length (0-128).
    * @throws IllegalArgumentException if the parameters are invalid.
    */
    public SubnetUtils6(final String address, final int prefixLength) {
        this(address + "/" + prefixLength);
    }

    /**
    * Gets a {@link SubnetInfo} instance that contains subnet-specific statistics.
    *
    * @return a new SubnetInfo instance.
    */
    public SubnetInfo getInfo() {
        return new SubnetInfo();
    }

    /**
    * Returns a summary of this subnet for debugging.
    * <p>
    * Delegates to {@link SubnetInfo#toString()}. This is a diagnostic format and is not suitable for parsing.
    * Use {@link SubnetInfo#getCidrSignature()} to obtain a string that can be fed back into
    * {@link #SubnetUtils6(String)}.
    * </p>
    *
    * @return a multi-line debug string summarizing this subnet.
    */
    @Override
    public String toString() {
        return getInfo().toString();
    }
}
