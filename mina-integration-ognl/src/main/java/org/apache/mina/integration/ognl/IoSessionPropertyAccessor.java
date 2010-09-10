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
package org.apache.mina.integration.ognl;

import java.util.Map;
import java.util.TreeMap;

import ognl.OgnlContext;
import ognl.OgnlException;
import ognl.OgnlRuntime;
import ognl.PropertyAccessor;

import org.apache.mina.core.session.IoSession;

/**
 * An OGNL {@link PropertyAccessor} for {@link IoSession}.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class IoSessionPropertyAccessor extends AbstractPropertyAccessor {

    @Override
    protected Object getProperty0(OgnlContext context, Object target,
            String name) throws OgnlException {
        if (target instanceof IoSession && "attributes".equals(name)) {
            Map<String, Object> attributes = new TreeMap<String, Object>();
            IoSession s = (IoSession) target;
            for (Object key: s.getAttributeKeys()) {
                Object value = s.getAttribute(key);
                if (value == null) {
                    continue;
                }
                attributes.put(String.valueOf(key), value);
            }
            return attributes;
        }
        
        return OgnlRuntime.NotFound;
    }

    @Override
    protected boolean hasGetProperty0(OgnlContext context, Object target,
            String name) throws OgnlException {
        return target instanceof IoSession && "attributes".equals(name);
    }

    @Override
    protected boolean hasSetProperty0(OgnlContext context, Object target,
            String name) throws OgnlException {
        return false;
    }

    @Override
    protected Object setProperty0(OgnlContext context, Object target,
            String name, Object value) throws OgnlException {
        return OgnlRuntime.NotFound;
    }
}
