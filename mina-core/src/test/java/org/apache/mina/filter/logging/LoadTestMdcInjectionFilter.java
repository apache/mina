/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.mina.filter.logging;

import junit.framework.JUnit4TestAdapter;
import junit.framework.Test;
import junit.textui.TestRunner;

import java.util.Date;

/**
 * Test the MdcInjectionFilter load for Windows
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class LoadTestMdcInjectionFilter {

    /**
     * The MdcInjectionFilterTest is unstable, it fails sporadically (and only on Windows ?)
     * This is a quick and dirty program to run the MdcInjectionFilterTest many times.
     * To be removed once we consider DIRMINA-784 to be fixed
     *
     */
    public static void main(String[] args) {
        TestRunner runner = new TestRunner();

        try {
            for (int i=0; i<50000; i++) {
                Test test = new JUnit4TestAdapter(MdcInjectionFilterTest.class);
                runner.doRun(test);
                System.out.println("i = " + i + " " + new Date());
            }
            System.out.println("done");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);

    }
}
