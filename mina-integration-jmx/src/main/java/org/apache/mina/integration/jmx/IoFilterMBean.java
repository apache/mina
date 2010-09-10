/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.mina.integration.jmx;

import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.session.IoSession;

/**
 * A JMX MBean wrapper for an {@link IoSession}.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class IoFilterMBean extends ObjectMBean<IoFilter> {

    public IoFilterMBean(IoFilter source) {
        super(source);
    }
    
    @Override
    protected boolean isOperation(String methodName, Class<?>[] paramTypes) {
        // Ignore some IoFilter methods.
        if (methodName.matches(
                "(init|destroy|on(Pre|Post)(Add|Remove)|" +
                "session(Created|Opened|Idle|Closed)|" +
                "exceptionCaught|message(Received|Sent)|" +
                "filter(Close|Write|SetTrafficMask))")) {
            return false;
        }
        
        return super.isOperation(methodName, paramTypes);
    }
}
