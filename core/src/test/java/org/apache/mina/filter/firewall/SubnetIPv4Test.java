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
 * TODO Add documentation
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class SubnetIPv4Test {
    @Test
    public void test24() throws UnknownHostException {
        InetAddress a = InetAddress.getByName("127.2.3.0");
        InetAddress b = InetAddress.getByName("127.2.3.4");
        InetAddress c = InetAddress.getByName("127.2.3.255");
        InetAddress d = InetAddress.getByName("127.2.4.4");
        
        Subnet mask = new Subnet(a, 24);
        
        assertTrue(mask.inSubnet(a));
        assertTrue(mask.inSubnet(b));
        assertTrue(mask.inSubnet(c));
        assertFalse(mask.inSubnet(d));
    }

    @Test
    public void test16() throws UnknownHostException {
        InetAddress a = InetAddress.getByName("127.2.0.0");
        InetAddress b = InetAddress.getByName("127.2.3.4");
        InetAddress c = InetAddress.getByName("127.2.129.255");
        InetAddress d = InetAddress.getByName("127.3.4.4");
        
        Subnet mask = new Subnet(a, 16);
        
        assertTrue(mask.inSubnet(a));
        assertTrue(mask.inSubnet(b));
        assertTrue(mask.inSubnet(c));
        assertFalse(mask.inSubnet(d));
    }
    
    @Test
    public void testSingleIp() throws UnknownHostException {
        InetAddress a = InetAddress.getByName("127.2.3.4");
        InetAddress b = InetAddress.getByName("127.2.3.3");
        InetAddress c = InetAddress.getByName("127.2.3.255");
        InetAddress d = InetAddress.getByName("127.2.3.0");
        
        Subnet mask = new Subnet(a, 32);
        
        assertTrue(mask.inSubnet(a));
        assertFalse(mask.inSubnet(b));
        assertFalse(mask.inSubnet(c));
        assertFalse(mask.inSubnet(d));
    }
    
    @Test
    public void testToString() throws UnknownHostException {
        InetAddress a = InetAddress.getByName("127.2.3.0");
        Subnet mask = new Subnet(a, 24);
        
        assertEquals("127.2.3.0/24", mask.toString());
    }

    @Test
    public void testToStringLiteral() throws UnknownHostException {
        InetAddress a = InetAddress.getByName("localhost");
        Subnet mask = new Subnet(a, 32);
        
        assertEquals("127.0.0.1/32", mask.toString());
    }
    
    
    @Test
    public void testEquals() throws UnknownHostException {
        Subnet a = new Subnet(InetAddress.getByName("127.2.3.4"), 32);
        Subnet b = new Subnet(InetAddress.getByName("127.2.3.4"), 32);
        Subnet c = new Subnet(InetAddress.getByName("127.2.3.5"), 32);
        Subnet d = new Subnet(InetAddress.getByName("127.2.3.5"), 24);
        
        assertTrue(a.equals(b));
        assertFalse(a.equals(c));
        assertFalse(a.equals(d));
        assertFalse(a.equals(null));
    }
}
