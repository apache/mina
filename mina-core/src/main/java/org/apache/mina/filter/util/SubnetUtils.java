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

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
* Performs subnet calculations given a network address and a subnet mask.
*
* This class is extracted from Apache commons-net project
* @see <a href="https://datatracker.ietf.org/doc/html/rfc1519">Classless Inter-Domain Routing (CIDR): an Address Assignment and Aggregation Strategy</a>
* @see SubnetUtils6
* @since 2.0
*/
public class SubnetUtils {

    /**
    * Allows an object to be the target of the "for-each loop" statement for a SubnetInfo.
    */
    private static final class SubnetAddressStringIterable implements Iterable<String> {

        private final SubnetInfo subnetInfo;

        /**
        * Constructs a new instance.
        *
        * @param subnetInfo the SubnetInfo to iterate.
        */
        private SubnetAddressStringIterable(final SubnetInfo subnetInfo) {
            this.subnetInfo = subnetInfo;
        }

        @Override
        public Iterator<String> iterator() {
            return new SubnetAddressStringIterator(subnetInfo);
        }
    }

    /**
    * Iterates over a SubnetInfo.
    */
    private static final class SubnetAddressStringIterator implements Iterator<String> {

        private int currentAddress;

        private final SubnetInfo subnetInfo;

        /**
        * Constructs a new instance.
        *
        * @param subnetInfo the SubnetInfo to iterate.
        */
        private SubnetAddressStringIterator(final SubnetInfo subnetInfo) {
            this.subnetInfo = subnetInfo;
            currentAddress = subnetInfo.low();
        }

        @Override
        public boolean hasNext() {
            return subnetInfo.getAddressCountLong() > 0 && currentAddress <= subnetInfo.high();
        }

        @Override
        public String next() {
            return format(toArray4(currentAddress++));
        }
    }

    /**
    * Contains subnet summary information.
    */
    public final class SubnetInfo {

        /** Mask to convert unsigned int to a long (i.e. keep 32 bits). */
        private static final long UNSIGNED_INT_MASK = 0x0FFFFFFFFL;

        private SubnetInfo() {
        }

        /**
        * Converts a dotted decimal format address to a packed integer format.
        *
        * @param address a dotted decimal format address.
        * @return packed integer formatted int.
        */
        public int asInteger(final String address) {
            return toInteger(address);
        }

        private long broadcastLong() {
            return broadcast & UNSIGNED_INT_MASK;
        }

        /**
        * Gets this instance's address into a dotted decimal String.
        *
        * @return a dotted decimal String.
        */
        public String getAddress() {
            return format(toArray4(address));
        }

        /**
        * Gets the count of available addresses. Will be zero for CIDR/31 and CIDR/32 if the inclusive flag is false.
        *
        * @return the count of addresses, may be zero.
        * @throws RuntimeException if the correct count is greater than {@code Integer.MAX_VALUE}
        * @deprecated (3.4) use {@link #getAddressCountLong()} instead
        */
        @Deprecated
        public int getAddressCount() {
            final long countLong = getAddressCountLong();
            if (countLong > Integer.MAX_VALUE) {
                throw new IllegalStateException("Count is larger than an integer: " + countLong);
            }
            // Cannot be negative here
            return (int) countLong;
        }

        /**
        * Gets the count of available addresses. Will be zero for CIDR/31 and CIDR/32 if the inclusive flag is false.
        *
        * @return the count of addresses, may be zero.
        * @since 3.4
        */
        public long getAddressCountLong() {
            final long b = broadcastLong();
            final long n = networkLong();
            final long count = b - n + (isInclusiveHostCount() ? 1 : -1);
            return count < 0 ? 0 : count;
        }

        /**
        * Gets all addresses in this subnet, the return array could be huge.
        * <p>
        * For large ranges, you can iterate or stream over the addresses instead using {@link #iterableAddressStrings()} or {@link #streamAddressStrings()}.
        * </p>
        *
        * @return all addresses in this subnet.
        * @see #iterableAddressStrings()
        * @see #streamAddressStrings()
        */
        public String[] getAllAddresses() {
            final int ct = getAddressCount();
            final String[] addresses = new String[ct];
            if (ct == 0) {
                return addresses;
            }
            final int high = high();
            for (int add = low(), j = 0; add <= high; ++add, ++j) {
                addresses[j] = format(toArray4(add));
            }
            return addresses;
        }

        /**
        * Gets the broadcast address for this subnet.
        *
        * @return the broadcast address for this subnet.
        */
        public String getBroadcastAddress() {
            return format(toArray4(broadcast));
        }

        /**
        * Gets the CIDR signature for this subnet.
        *
        * @return the CIDR signature for this subnet.
        */
        public String getCidrSignature() {
            return format(toArray4(address)) + "/" + Integer.bitCount(netmask);
        }

        /**
        * Gets the high address as a dotted IP address. Will be zero for CIDR/31 and CIDR/32 if the inclusive flag is false.
        *
        * @return the IP address in dotted format, may be "0.0.0.0" if there is no valid address
        */
        public String getHighAddress() {
            return format(toArray4(high()));
        }

        /**
        * Gets the low address as a dotted IP address. Will be zero for CIDR/31 and CIDR/32 if the inclusive flag is false.
        *
        * @return the IP address in dotted format, may be "0.0.0.0" if there is no valid address
        */
        public String getLowAddress() {
            return format(toArray4(low()));
        }

        /**
        * Gets the network mask for this subnet.
        *
        * @return the network mask for this subnet.
        */
        public String getNetmask() {
            return format(toArray4(netmask));
        }

        /**
        * Gets the network address for this subnet.
        *
        * @return the network address for this subnet.
        */
        public String getNetworkAddress() {
            return format(toArray4(network));
        }

        /**
        * Gets the next address for this subnet.
        *
        * @return the next address for this subnet.
        */
        public String getNextAddress() {
            return format(toArray4(address + 1));
        }

        /**
        * Gets the previous address for this subnet.
        *
        * @return the previous address for this subnet.
        */
        public String getPreviousAddress() {
            return format(toArray4(address - 1));
        }

        private int high() {
            return isInclusiveHostCount() ? broadcast : broadcastLong() - networkLong() > 1 ? broadcast - 1 : 0;
        }

        /**
        * Tests if the parameter {@code address} is in the range of usable endpoint addresses for this subnet. This excludes the network and broadcast
        * addresses by default. Use {@link SubnetUtils#setInclusiveHostCount(boolean)} to change this.
        *
        * @param address the address to check
        * @return true if it is in range
        * @since 3.4 (made public)
        */
        public boolean isInRange(final int address) {
            if (address == 0) { // cannot ever be in range; rejecting now avoids problems with CIDR/31,32
                return false;
            }
            final long addLong = address & UNSIGNED_INT_MASK;
            final long lowLong = low() & UNSIGNED_INT_MASK;
            final long highLong = high() & UNSIGNED_INT_MASK;
            return addLong >= lowLong && addLong <= highLong;
        }

        /**
        * Tests if the parameter {@code address} is in the range of usable endpoint addresses for this subnet. This excludes the network and broadcast
        * addresses. Use {@link SubnetUtils#setInclusiveHostCount(boolean)} to change this.
        *
        * @param address A dot-delimited IPv4 address, e.g. "192.168.0.1"
        * @return True if in range, false otherwise
        */
        public boolean isInRange(final String address) {
            return isInRange(toInteger(address));
        }

        /**
        * Creates a new Iterable of address Strings.
        *
        * @return a new Iterable of address Strings
        * @see #getAllAddresses()
        * @see #streamAddressStrings()
        * @since 3.12.0
        */
        public Iterable<String> iterableAddressStrings() {
            return new SubnetAddressStringIterable(this);
        }

        private int low() {
            return isInclusiveHostCount() ? network : broadcastLong() - networkLong() > 1 ? network + 1 : 0;
        }

        /** Long versions of the values (as unsigned int) which are more suitable for range checking. */
        private long networkLong() {
            return network & UNSIGNED_INT_MASK;
        }

        /**
        * Creates a new Stream of address Strings.
        *
        * @return a new Stream of address Strings.
        * @see #getAllAddresses()
        * @see #iterableAddressStrings()
        * @since 3.12.0
        */
        public Stream<String> streamAddressStrings() {
            return StreamSupport.stream(iterableAddressStrings().spliterator(), false);
        }

        /**
        * {@inheritDoc}
        *
        * @since 2.2
        */
        @Override
        public String toString() {
            final StringBuilder buf = new StringBuilder();
            // @formatter:off
            buf.append("CIDR Signature:\t[").append(getCidrSignature()).append("]\n")
            .append("  Netmask: [").append(getNetmask()).append("]\n")
            .append("  Network: [").append(getNetworkAddress()).append("]\n")
            .append("  Broadcast: [").append(getBroadcastAddress()).append("]\n")
            .append("  First address: [").append(getLowAddress()).append("]\n")
            .append("  Last address: [").append(getHighAddress()).append("]\n")
            .append("  Address Count: [").append(getAddressCountLong()).append("]\n");
            // @formatter:on
            return buf.toString();
        }
    }

    private static final String IP_ADDRESS = "(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})";

    private static final String SLASH_FORMAT = IP_ADDRESS + "/(\\d{1,2})"; // 0 -> 32

    private static final Pattern ADDRESS_PATTERN = Pattern.compile(IP_ADDRESS);
    private static final Pattern CIDR_PATTERN = Pattern.compile(SLASH_FORMAT);
    private static final int NBITS = 32;
    private static final String PARSE_FAIL = "Could not parse [%s]";

    /**
    * Converts a 4-element array into dotted decimal format.
    */
    private static String format(final int[] octets) {
        final int last = octets.length - 1;
        final StringBuilder builder = new StringBuilder();
        for (int i = 0;; i++) {
            builder.append(octets[i]);
            if (i == last) {
                return builder.toString();
            }
            builder.append('.');
        }
    }

    /**
    * Extracts the components of a dotted decimal address and pack into an integer using a regex match
    */
    private static int matchAddress(final Matcher matcher) {
        int addr = 0;
        for (int i = 1; i <= 4; ++i) {
            final int n = rangeCheck(Integer.parseInt(matcher.group(i)), 0, 255);
            addr |= (n & 0xff) << 8 * (4 - i);
        }
        return addr;
    }

    /**
    * Checks integer boundaries. Checks if a value x is in the range [begin,end]. Returns x if it is in range, throws an exception otherwise.
    */
    private static int rangeCheck(final int value, final int begin, final int end) {
        if (value >= begin && value <= end) { // (begin,end]
            return value;
        }
        throw new IllegalArgumentException("Value [" + value + "] not in range [" + begin + "," + end + "]");
    }

    /**
    * Converts a packed integer address into a 4-element array
    */
    private static int[] toArray4(final int val) {
        final int[] ret = new int[4];
        for (int j = 3; j >= 0; --j) {
            ret[j] |= val >>> 8 * (3 - j) & 0xff;
        }
        return ret;
    }

    /**
    * Converts a dotted decimal format address to a packed integer format.
    */
    private static int toInteger(final String address) {
        final Matcher matcher = ADDRESS_PATTERN.matcher(address);
        if (matcher.matches()) {
            return matchAddress(matcher);
        }
        throw new IllegalArgumentException(String.format(PARSE_FAIL, address));
    }

    private final int address;

    private final int broadcast;

    /** Whether the broadcast/network address are included in host count */
    private boolean inclusiveHostCount;

    private final int netmask;

    private final int network;

    /**
    * Constructs an instance from a CIDR-notation string, e.g. "192.168.0.1/16"
    *
    * @param cidrNotation A CIDR-notation string, e.g. "192.168.0.1/16"
    * @throws IllegalArgumentException if the parameter is invalid, i.e. does not match n.n.n.n/m where n=1-3 decimal digits, m = 1-2 decimal digits in range
    *                                  0-32
    */
    public SubnetUtils(final String cidrNotation) {
        final Matcher matcher = CIDR_PATTERN.matcher(cidrNotation);

        if (!matcher.matches()) {
            throw new IllegalArgumentException(String.format(PARSE_FAIL, cidrNotation));
        }
        this.address = matchAddress(matcher);

        // Create a binary netmask from the number of bits specification /x

        final int trailingZeroes = NBITS - rangeCheck(Integer.parseInt(matcher.group(5)), 0, NBITS);

        //
        // An IPv4 netmask consists of 32 bits, a contiguous sequence
        // of the specified number of ones followed by all zeros.
        // So, it can be obtained by shifting an unsigned integer (32 bits) to the left by
        // the number of trailing zeros which is (32 - the # bits specification).
        // Note that there is no unsigned left shift operator, so we have to use
        // a long to ensure that the left-most bit is shifted out correctly.
        //
        this.netmask = (int) (0x0FFFFFFFFL << trailingZeroes);

        // Calculate base network address
        this.network = address & netmask;

        // Calculate broadcast address
        this.broadcast = network | ~netmask;
    }

    /**
    * Constructs an instance from a dotted decimal address and a dotted decimal mask.
    *
    * @param address An IP address, e.g. "192.168.0.1"
    * @param mask    A dotted decimal netmask e.g. "255.255.0.0"
    * @throws IllegalArgumentException if the address or mask is invalid, i.e. does not match n.n.n.n where n=1-3 decimal digits and the mask is not all zeros
    */
    public SubnetUtils(final String address, final String mask) {
        this.address = toInteger(address);
        this.netmask = toInteger(mask);

        if ((this.netmask & -this.netmask) - 1 != ~this.netmask) {
            throw new IllegalArgumentException(String.format(PARSE_FAIL, mask));
        }

        // Calculate base network address
        this.network = this.address & this.netmask;

        // Calculate broadcast address
        this.broadcast = this.network | ~this.netmask;
    }

    /**
    * Gets a {@link SubnetInfo} instance that contains subnet-specific statistics
    *
    * @return new instance
    */
    public final SubnetInfo getInfo() {
        return new SubnetInfo();
    }

    /**
    * Gets the next subnet for this instance.
    *
    * @return the next subnet for this instance.
    */
    public SubnetUtils getNext() {
        return new SubnetUtils(getInfo().getNextAddress(), getInfo().getNetmask());
    }

    /**
    * Gets the previous subnet for this instance.
    *
    * @return the next previous for this instance.
    */
    public SubnetUtils getPrevious() {
        return new SubnetUtils(getInfo().getPreviousAddress(), getInfo().getNetmask());
    }

    /**
    * Tests if the return value of {@link SubnetInfo#getAddressCount()} includes the network and broadcast addresses.
    *
    * @return true if the host count includes the network and broadcast addresses
    * @since 2.2
    */
    public boolean isInclusiveHostCount() {
        return inclusiveHostCount;
    }

    /**
    * Sets to {@code true} if you want the return value of {@link SubnetInfo#getAddressCount()} to include the network and broadcast addresses. This also
    * applies to {@link SubnetInfo#isInRange(int)}
    *
    * @param inclusiveHostCount true if network and broadcast addresses are to be included
    * @since 2.2
    */
    public void setInclusiveHostCount(final boolean inclusiveHostCount) {
        this.inclusiveHostCount = inclusiveHostCount;
    }

    /**
    * Converts this instance to a debug String.
    *
    * @return {@code this} instance to a debug String.
    * @since 3.11.0
    */
    @Override
    public String toString() {
        return getInfo().toString();
    }
}
