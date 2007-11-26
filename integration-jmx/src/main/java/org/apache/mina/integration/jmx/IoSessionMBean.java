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

import java.util.List;
import java.util.Map;

import javax.management.modelmbean.ModelMBeanAttributeInfo;

import org.apache.mina.common.IoSession;

/**
 * A JMX MBean wrapper for an {@link IoSession}.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class IoSessionMBean extends ObjectMBean<IoSession> {

    public IoSessionMBean(IoSession source) {
        super(source);
    }
    
    @Override
    protected void addExtraAttributes(List<ModelMBeanAttributeInfo> attributes) {
        attributes.add(new ModelMBeanAttributeInfo(
                "attributes", Map.class.getName(), "attributes",
                true, false, false));
    }
    
    @Override
    protected boolean isOperation(String methodName) {
        // Ignore some IoSession methods.
        if (methodName.matches(
                "(write|read|(remove|replace|contains)Attribute)")) {
            return false;
        }
        
        return super.isOperation(methodName);
    }

    @Override
    protected Class<?> convertAttributeType(String attrName, Class<?> attrType) {
        if ((attrType == Long.class || attrType == long.class)) {
            if (attrName.equals("id")) {
                return String.class;
            }
        }
        
        return super.convertAttributeType(attrName, attrType);
    }

    @Override
    protected Object convertAttributeValue(String attrName, Object v) {
        if (v instanceof Long) {
            if (attrName.equals("id")) {
                return IoServiceMBean.getSessionIdAsString((Long) v);
            }
        }

        return super.convertAttributeValue(attrName, v);
    }
}
