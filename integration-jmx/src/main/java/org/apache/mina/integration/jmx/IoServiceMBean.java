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

import java.net.SocketAddress;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.ObjectName;
import javax.management.modelmbean.ModelMBeanOperationInfo;

import ognl.Ognl;

import org.apache.mina.core.service.IoService;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.integration.ognl.IoSessionFinder;

/**
 * A JMX MBean wrapper for an {@link IoSession}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
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
    protected Object invoke0(String name, Object[] params, String[] signature) throws Exception {
        if (name.equals("findSessions")) {
            IoSessionFinder finder = new IoSessionFinder((String) params[0]);
            return finder.find(getSource().getManagedSessions().values());
        }

        if (name.equals("findAndRegisterSessions")) {
            IoSessionFinder finder = new IoSessionFinder((String) params[0]);
            Set<IoSession> registeredSessions = new LinkedHashSet<IoSession>();
            for (IoSession s: finder.find(getSource().getManagedSessions().values())) {
                try {
                    getServer().registerMBean(
                            new IoSessionMBean(s),
                            new ObjectName(
                                    getName().getDomain() +
                                    ":type=session,name=" +
                                    getSessionIdAsString(s.getId())));
                    registeredSessions.add(s);
                } catch (Exception e) {
                    LOGGER.warn("Failed to register a session as a MBean: " + s, e);
                }
            }

            return registeredSessions;
        }

        if (name.equals("findAndProcessSessions")) {
            IoSessionFinder finder = new IoSessionFinder((String) params[0]);
            String command = (String) params[1];
            Object expr = Ognl.parseExpression(command);
            Set<IoSession> matches = finder.find(getSource().getManagedSessions().values());

            for (IoSession s: matches) {
                try {
                    Ognl.getValue(expr, s);
                } catch (Exception e) {
                    LOGGER.warn("Failed to execute '" + command + "' for: " + s, e);
                }
            }
            return matches;
        }

        return super.invoke0(name, params, signature);
    }

    @Override
    protected void addExtraOperations(List<ModelMBeanOperationInfo> operations) {
        operations.add(new ModelMBeanOperationInfo(
                "findSessions", "findSessions",
                new MBeanParameterInfo[] {
                        new MBeanParameterInfo(
                                "ognlQuery", String.class.getName(), "a boolean OGNL expression")
                }, Set.class.getName(), MBeanOperationInfo.INFO));
        operations.add(new ModelMBeanOperationInfo(
                "findAndRegisterSessions", "findAndRegisterSessions",
                new MBeanParameterInfo[] {
                        new MBeanParameterInfo(
                                "ognlQuery", String.class.getName(), "a boolean OGNL expression")
                }, Set.class.getName(), MBeanOperationInfo.ACTION_INFO));
        operations.add(new ModelMBeanOperationInfo(
                "findAndProcessSessions", "findAndProcessSessions",
                new MBeanParameterInfo[] {
                        new MBeanParameterInfo(
                                "ognlQuery", String.class.getName(), "a boolean OGNL expression"),
                        new MBeanParameterInfo(
                                "ognlCommand", String.class.getName(), "an OGNL expression that modifies the state of the sessions in the match result")
                }, Set.class.getName(), MBeanOperationInfo.ACTION_INFO));
    }

    @Override
    protected boolean isOperation(String methodName, Class<?>[] paramTypes) {
        // Ignore some IoServide methods.
        if (methodName.matches(
                "(newSession|broadcast|(add|remove)Listener)")) {
            return false;
        }

        if ((methodName.equals("bind") || methodName.equals("unbind")) &&
                (paramTypes.length > 1 ||
                        paramTypes.length == 1 && !SocketAddress.class.isAssignableFrom(paramTypes[0]))) {
            return false;
        }

        return super.isOperation(methodName, paramTypes);
    }
}
