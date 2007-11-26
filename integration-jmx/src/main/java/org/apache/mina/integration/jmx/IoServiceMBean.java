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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.management.MBeanException;
import javax.management.MBeanParameterInfo;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.modelmbean.ModelMBeanOperationInfo;

import ognl.OgnlException;

import org.apache.mina.common.IoService;
import org.apache.mina.common.IoSession;
import org.apache.mina.integration.ognl.IoSessionFinder;

/**
 * A JMX MBean wrapper for an {@link IoSession}.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class IoServiceMBean extends ObjectMBean<IoService> {

    static String getSessionIdAsString(long l) {
        // ID in MINA is a unsigned 32-bit integer.
        String id = Long.toHexString(l).toUpperCase();
        while (id.length() < 8) {
            id = '0' + id; // padding
        }
        id = "0x" + id;
        return id;
    }
    
    public IoServiceMBean(IoService source) {
        super(source);
    }

    @Override
    public Object invoke(String name, Object[] params, String[] signature)
            throws MBeanException, ReflectionException {
        if (name.equals("findSessions")) {
            try {
                IoSessionFinder finder = new IoSessionFinder((String) params[0]);
                return convertReturnValue(finder.find(
                        (getSource()).getManagedSessions()));
            } catch (OgnlException e) {
                throwMBeanException(e);
            }
        }
        
        if (name.equals("findAndRegisterSessions")) {
            try {
                IoSessionFinder finder = new IoSessionFinder((String) params[0]);
                Set<IoSession> registeredSessions = new LinkedHashSet<IoSession>();
                for (IoSession s: finder.find(
                        (getSource()).getManagedSessions())) {
                    try {
                        getServer().registerMBean(
                                new IoSessionMBean(s),
                                new ObjectName(
                                        getName().getDomain() + 
                                        ":type=session,name=" + 
                                        getSessionIdAsString(s.getId())));
                        registeredSessions.add(s);
                    } catch (Exception e) {
                        logger.warn("Failed to register a session as a MBean: " + s, e);
                    }
                }
                
                return convertReturnValue(registeredSessions);
            } catch (OgnlException e) {
                throwMBeanException(e);
            }
        }
        
        return super.invoke(name, params, signature);
    }

    @Override
    protected void addExtraOperations(List<ModelMBeanOperationInfo> operations) {
        operations.add(new ModelMBeanOperationInfo(
                "findSessions", "findSessions",
                new MBeanParameterInfo[] {
                        new MBeanParameterInfo(
                                "ognlQuery", String.class.getName(), "a boolean OGNL expression")
                }, Set.class.getName(), ModelMBeanOperationInfo.INFO));
        operations.add(new ModelMBeanOperationInfo(
                "findAndRegisterSessions", "findAndRegisterSessions",
                new MBeanParameterInfo[] {
                        new MBeanParameterInfo(
                                "ognlQuery", String.class.getName(), "a boolean OGNL expression")
                }, Set.class.getName(), ModelMBeanOperationInfo.ACTION_INFO));
    }

    @Override
    protected boolean isOperation(String methodName) {
        // Ignore some IoServide methods.
        if (methodName.matches(
                "(newSession|broadcast|(add|remove)Listener)")) {
            return false;
        }

        return super.isOperation(methodName);
    }
}
