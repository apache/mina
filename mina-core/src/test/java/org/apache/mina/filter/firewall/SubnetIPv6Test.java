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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.Test;

/**
*
* @author <a href="http://mina.apache.org">Apache MINA Project</a>
*/
public class SubnetIPv6Test {

    @Test
    public void testIPv6() throws UnknownHostException {

        Subnet subnet = new Subnet(InetAddress.getByName("2001:db8::"), 32);
        assertTrue(!subnet.inSubnet(InetAddress.getByName("2001:db7:ffff:ffff:ffff:ffff:ffff:ffff")));
        assertTrue(!subnet.inSubnet(InetAddress.getByName("2001:db9::")));
        assertTrue(subnet.inSubnet(InetAddress.getByName("2001:db8::1")));
        assertTrue(subnet.inSubnet(InetAddress.getByName("2001:db8:ffff:ffff:ffff:ffff:ffff:ffff")));

    }

    @Test
    public void test32() throws UnknownHostException {
        InetAddress a = InetAddress.getByName("2001:db8::");
        InetAddress b = InetAddress.getByName("2001:db8::1");
        InetAddress c = InetAddress.getByName("2001:db8:ffff:ffff:ffff:ffff:ffff:ffff");
        InetAddress d = InetAddress.getByName("2001:db7:ffff:ffff:ffff:ffff:ffff:ffff");
        InetAddress e = InetAddress.getByName("2001:db9::");

        Subnet mask = new Subnet(a, 32);

        assertTrue(mask.inSubnet(a));
        assertTrue(mask.inSubnet(b));
        assertTrue(mask.inSubnet(c));
        assertFalse(mask.inSubnet(d));
        assertFalse(mask.inSubnet(e));
    }

    @Test
    public void test96() throws UnknownHostException {
        InetAddress a = InetAddress.getByName("2001:db8:dead:beef:abcd:abcd::");
        InetAddress b = InetAddress.getByName("2001:db8:dead:beef:abcd:abcd::");
        InetAddress c = InetAddress.getByName("2001:db8:dead:beef:abcd:abcd:ffff:ffff");
        InetAddress d = InetAddress.getByName("2001:db8:dead:beef:abcd:abce::");
        InetAddress e = InetAddress.getByName("2001:db8:dead:beef:abcd:abcc:ffff:ffff");

        Subnet mask = new Subnet(a, 96);

        assertTrue(mask.inSubnet(a));
        assertTrue(mask.inSubnet(b));
        assertTrue(mask.inSubnet(c));
        assertFalse(mask.inSubnet(d));
        assertFalse(mask.inSubnet(e));
    }

    @Test
    public void testSingleIp() throws UnknownHostException {
        InetAddress a = InetAddress.getByName("2001:db8:dead:beef:f0ca:cc1a:ac1d:ba5e");
        InetAddress b = InetAddress.getByName("2001:db8::");
        InetAddress c = InetAddress.getByName("2001:db8:ffff:ffff:ffff:ffff:ffff:ffff");
        InetAddress d = InetAddress.getByName("2001:db8:dead:beef:f0ca:cc1a:ac1d:ba5f");
        InetAddress e = InetAddress.getByName("2001:db8:dead:beef:f0ca:cc1a:ac1d:ba5d");

        Subnet mask = new Subnet(a, 128);

        assertTrue(mask.inSubnet(a));
        assertFalse(mask.inSubnet(b));
        assertFalse(mask.inSubnet(c));
        assertFalse(mask.inSubnet(d));
        assertFalse(mask.inSubnet(e));
    }

    @Test
    public void testToString() throws UnknownHostException {
        InetAddress a = InetAddress.getByName("2001:db8::");
        Subnet mask = new Subnet(a, 32);

        assertEquals("2001:db8:0:0:0:0:0:0/32", mask.toString());
    }

    @Test
    public void testEquals() throws UnknownHostException {
        Subnet a = new Subnet(InetAddress.getByName("2001:db8::"), 32);
        Subnet b = new Subnet(InetAddress.getByName("2001:db8::"), 32);
        Subnet c = new Subnet(InetAddress.getByName("2001:db8:dead:beef::"), 64);
        Subnet d = new Subnet(InetAddress.getByName("2001:db8:dead:beef::"), 64);

        assertTrue(a.equals(b));
        assertFalse(a.equals(c));
        assertFalse(a.equals(d));
        assertFalse(a.equals(null));
    }
}
