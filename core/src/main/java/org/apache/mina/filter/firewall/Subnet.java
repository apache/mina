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
import java.net.InetAddress;

/**
 * A IP subnet using the CIDR notation. Currently, only IP version 4
 * address are supported.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class Subnet {

    private static final int IP_MASK = 0x80000000;
    private static final int BYTE_MASK = 0xFF;

    private InetAddress subnet;
    private int subnetInt;
    private int subnetMask;
    private int suffix;

    /**
     * Creates a subnet from CIDR notation. For example, the subnet
     * 192.168.0.0/24 would be created using the {@link InetAddress}  
     * 192.168.0.0 and the mask 24.
     * @param subnet The {@link InetAddress} of the subnet
     * @param mask The mask
     */
    public Subnet(InetAddress subnet, int mask) {
        if(subnet == null) {
            throw new IllegalArgumentException("Subnet address can not be null");
        }
        if(!(subnet instanceof Inet4Address)) {
            throw new IllegalArgumentException("Only IPv4 supported");
        }

        if(mask < 0 || mask > 32) {
            throw new IllegalArgumentException("Mask has to be an integer between 0 and 32");
        }
        
        this.subnet = subnet;
        this.subnetInt = toInt(subnet);
        this.suffix = mask;
        
        // binary mask for this subnet
        this.subnetMask = IP_MASK >> (mask - 1);
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
     * Converts an IP address to a subnet using the provided 
     * mask
     * @param address The address to convert into a subnet
     * @return The subnet as an integer
     */
    private int toSubnet(InetAddress address) {
        return toInt(address) & subnetMask;
    }
    
    /**
     * Checks if the {@link InetAddress} is within this subnet
     * @param address The {@link InetAddress} to check
     * @return True if the address is within this subnet, false otherwise
     */
    public boolean inSubnet(InetAddress address) {
        return toSubnet(address) == subnetInt;
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
        if(!(obj instanceof Subnet)) {
            return false;
        }
        
        Subnet other = (Subnet) obj;
        
        return other.subnetInt == subnetInt && other.suffix == suffix;
    }

    
}
