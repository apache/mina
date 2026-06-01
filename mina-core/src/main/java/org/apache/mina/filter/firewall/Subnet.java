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

import org.apache.mina.filter.util.SubnetUtils;
import org.apache.mina.filter.util.SubnetUtils6;

/**
* A IP subnet using the CIDR notation. Currently, only IP version 4
* address are supported.
*
* @author <a href="http://mina.apache.org">Apache MINA Project</a>
*/
public class Subnet {

    private SubnetUtils subnetUtils;
    private SubnetUtils6 subnetUtils6;

    boolean isIpv6;

    /**
    * Creates a subnet from CIDR notation. For example, the subnet
    * 192.168.0.0/24 would be created using the {@link InetAddress}
    * 192.168.0.0 and the mask 24.
    *
    * @param subnet The {@link InetAddress} of the subnet
    * @param mask   The mask
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
            this.subnetUtils = new SubnetUtils(subnet.getHostAddress() + "/" + mask);
            this.subnetUtils.setInclusiveHostCount(true);
            isIpv6 = false;
        } else {
            this.subnetUtils6 = new SubnetUtils6(subnet.getHostAddress(), mask);
            isIpv6 = true;
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

        if (this.isIpv6 ) {
            if (address instanceof Inet6Address) {
                return subnetUtils6.getInfo().isInRange( (Inet6Address) address);
            } else {
                return false;
            }
        } else {
            if (address instanceof Inet4Address) {
                byte[] bytes = address.getAddress();
                int value = ((bytes[0] & 0xFF) << 24) |
                ((bytes[1] & 0xFF) << 16) |
                ((bytes[2] & 0xFF) << 8)  |
                (bytes[3] & 0xFF);
                return subnetUtils.getInfo().isInRange(value);
            } else {
                return false;
            }
        }
    }

    /**
    * @see Object#toString()
    */
    @Override
    public String toString() {
        if (this.isIpv6 ) {
            return subnetUtils6.getInfo().getCidrSignature();
        } else {
            return subnetUtils.getInfo().getCidrSignature();
        }

    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Subnet)) {
            return false;
        }

        Subnet other = (Subnet) obj;

        if (this.isIpv6 != other.isIpv6) {
            return false;
        }

        if (this.isIpv6 ) {
            return this.subnetUtils6.getInfo().getCidrSignature().equals(other.subnetUtils6.getInfo().getCidrSignature());
        } else {
            return this.subnetUtils.getInfo().getCidrSignature().equals(other.subnetUtils.getInfo().getCidrSignature());
        }
    }

}
