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
package org.apache.mina;

import org.apache.mina.session.AbstractIoSessionTest;
import org.apache.mina.session.AttributeContainerTest;
import org.apache.mina.session.AttributeKeyTest;
import org.apache.mina.util.AbstractIoFutureTest;
import org.apache.mina.util.AssertTest;
import org.apache.mina.util.ByteBufferDumper;
import org.apache.mina.util.IoBufferTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * The Test-Suite for all Mina Tests
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
@RunWith(Suite.class)
@SuiteClasses(value = {
        //filter.codec

        //filterchain

        //session
        AbstractIoSessionTest.class, 
        AttributeContainerTest.class, 
        AttributeKeyTest.class,

        //transport.tcp

        //util
        AbstractIoFutureTest.class, 
        AssertTest.class, 
        ByteBufferDumper.class, 
        IoBufferTest.class })
public class AllTests {
}
