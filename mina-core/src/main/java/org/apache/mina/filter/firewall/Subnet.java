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

package org.apache.mina.filter.firewall;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;

/**
 * A IP subnet using the CIDR notation. Currently, only IP version 4
 * address are supported.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class Subnet {

    private static final int IP_MASK_V4 = 0x80000000;

    private static final long IP_MASK_V6 = 0x8000000000000000L;

    private static final int BYTE_MASK = 0xFF;

    private InetAddress subnet;

    /** An int representation of a subnet for IPV4 addresses */
    private int subnetInt;

    /** An long representation of a subnet for IPV6 addresses */
    private long subnetLong;

    private long subnetMask;

    private int suffix;

    /**
     * Creates a subnet from CIDR notation. For example, the subnet
     * 192.168.0.0/24 would be created using the {@link InetAddress}
     * 192.168.0.0 and the mask 24.
     * @param subnet The {@link InetAddress} of the subnet
     * @param mask The mask
     */
    public Subnet(InetAddress subnet, int mask) {
        if (subnet == null) {
            throw new IllegalArgumentException("Subnet address can not be null");
        }

        if (!(subnet instanceof Inet4Address) && !(subnet instanceof Inet6Address)) {
            throw new IllegalArgumentException("Only IPv4 and IPV6 supported");
        }

        if (subnet instanceof Inet4Address) {
            // IPV4 address
            if ((mask < 0) || (mask > 32)) {
                throw new IllegalArgumentException("Mask has to be an integer between 0 and 32 for an IPV4 address");
            } else {
                this.subnet = subnet;
                subnetInt = toInt(subnet);
                this.suffix = mask;

                // binary mask for this subnet
                this.subnetMask = IP_MASK_V4 >> (mask - 1);
            }
        } else {
            // IPV6 address
            if ((mask < 0) || (mask > 128)) {
                throw new IllegalArgumentException("Mask has to be an integer between 0 and 128 for an IPV6 address");
            } else {
                this.subnet = subnet;
                subnetLong = toLong(subnet);
                this.suffix = mask;

                // binary mask for this subnet
                this.subnetMask = IP_MASK_V6 >> (mask - 1);
            }
        }
    }

    /**
     * Converts an IP address into an integer
     */
    private int toInt(InetAddress inetAddress) {
        byte[] address = inetAddress.getAddress();
        int result = 0;

        for (int i = 0; i < address.length; i++) {
            result <<= 8;
            result |= address[i] & BYTE_MASK;
        }

        return result;
    }

    /**
     * Converts an IP address into a long
     */
    private long toLong(InetAddress inetAddress) {
        byte[] address = inetAddress.getAddress();
        long result = 0;

        for (int i = 0; i < address.length; i++) {
            result <<= 8;
            result |= address[i] & BYTE_MASK;
        }

        return result;
    }

    /**
     * Converts an IP address to a subnet using the provided mask
     * 
     * @param address
     *            The address to convert into a subnet
     * @return The subnet as an integer
     */
    private long toSubnet(InetAddress address) {
        if (address instanceof Inet4Address) {
            return toInt(address) & (int) subnetMask;
        } else {
            return toLong(address) & subnetMask;
        }
    }

    /**
     * Checks if the {@link InetAddress} is within this subnet
     * @param address The {@link InetAddress} to check
     * @return True if the address is within this subnet, false otherwise
     */
    public boolean inSubnet(InetAddress address) {
        if (address.isAnyLocalAddress()) {
            return true;
        }

        if (address instanceof Inet4Address) {
            return (int) toSubnet(address) == subnetInt;
        } else {
            return toSubnet(address) == subnetLong;
        }
    }

    /**
     * @see Object#toString()
     */
    @Override
    public String toString() {
        return subnet.getHostAddress() + "/" + suffix;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Subnet)) {
            return false;
        }

        Subnet other = (Subnet) obj;

        return other.subnetInt == subnetInt && other.suffix == suffix;
    }

}
