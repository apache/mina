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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.management.MBeanException;
import javax.management.MBeanParameterInfo;
import javax.management.ObjectName;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanOperationInfo;

import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.session.IoSession;

/**
 * A JMX MBean wrapper for an {@link IoSession}.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class IoSessionMBean extends ObjectMBean<IoSession> {

    public IoSessionMBean(IoSession source) {
        super(source);
    }
    
    @Override
    protected Object getAttribute0(String fqan) throws Exception {
        if (fqan.equals("attributes")) {
            Map<String, String> answer = new LinkedHashMap<String, String>();
            for (Object key: getSource().getAttributeKeys()) {
                answer.put(String.valueOf(key), String.valueOf(getSource().getAttribute(key)));
            }
            return answer;
        }
        
        return super.getAttribute0(fqan);
    }

    @Override
    protected Object invoke0(String name, Object[] params, String[] signature) throws Exception {
        if (name.equals("addFilterFirst")) {
            String filterName = (String) params[0];
            ObjectName filterRef = (ObjectName) params[1];
            IoFilter filter = getFilter(filterRef);
            getSource().getFilterChain().addFirst(filterName, filter);
            return null;
        }
        
        if (name.equals("addFilterLast")) {
            String filterName = (String) params[0];
            ObjectName filterRef = (ObjectName) params[1];
            IoFilter filter = getFilter(filterRef);
            getSource().getFilterChain().addLast(filterName, filter);
            return null;
        }
        
        if (name.equals("addFilterBefore")) {
            String filterBaseName = (String) params[0];
            String filterName = (String) params[1];
            ObjectName filterRef = (ObjectName) params[2];
            IoFilter filter = getFilter(filterRef);
            getSource().getFilterChain().addBefore(filterBaseName, filterName, filter);
            return null;
        }
        
        if (name.equals("addFilterAfter")) {
            String filterBaseName = (String) params[0];
            String filterName = (String) params[1];
            ObjectName filterRef = (ObjectName) params[2];
            IoFilter filter = getFilter(filterRef);
            getSource().getFilterChain().addAfter(filterBaseName, filterName, filter);
            return null;
        }
        
        if (name.equals("removeFilter")) {
            String filterName = (String) params[0];
            getSource().getFilterChain().remove(filterName);
            return null;
        }
        
        return super.invoke0(name, params, signature);
    }

    private IoFilter getFilter(ObjectName filterRef) throws MBeanException {
        Object object = ObjectMBean.getSource(filterRef);
        if (object == null) {
            throw new MBeanException(new IllegalArgumentException(
                    "MBean not found: " + filterRef));
        }
        if (!(object instanceof IoFilter)) {
            throw new MBeanException(new IllegalArgumentException(
                    "MBean '" + filterRef + "' is not an IoFilter."));
        }
        
        return (IoFilter) object;
    }

    @Override
    protected void addExtraAttributes(List<ModelMBeanAttributeInfo> attributes) {
        attributes.add(new ModelMBeanAttributeInfo(
                "attributes", Map.class.getName(), "attributes",
                true, false, false));
    }
    
    @Override
    protected void addExtraOperations(List<ModelMBeanOperationInfo> operations) {
        operations.add(new ModelMBeanOperationInfo(
                "addFilterFirst", "addFilterFirst", new MBeanParameterInfo[] {
                        new MBeanParameterInfo(
                                "name", String.class.getName(), "the new filter name"),
                        new MBeanParameterInfo(
                                "filter", ObjectName.class.getName(), "the ObjectName reference to the filter")
                }, void.class.getName(), ModelMBeanOperationInfo.ACTION));

        operations.add(new ModelMBeanOperationInfo(
                "addFilterLast", "addFilterLast", new MBeanParameterInfo[] {
                        new MBeanParameterInfo(
                                "name", String.class.getName(), "the new filter name"),
                        new MBeanParameterInfo(
                                "filter", ObjectName.class.getName(), "the ObjectName reference to the filter")
                }, void.class.getName(), ModelMBeanOperationInfo.ACTION));

        operations.add(new ModelMBeanOperationInfo(
                "addFilterBefore", "addFilterBefore", new MBeanParameterInfo[] {
                        new MBeanParameterInfo(
                                "baseName", String.class.getName(), "the next filter name"),
                        new MBeanParameterInfo(
                                "name", String.class.getName(), "the new filter name"),
                        new MBeanParameterInfo(
                                "filter", ObjectName.class.getName(), "the ObjectName reference to the filter")
                }, void.class.getName(), ModelMBeanOperationInfo.ACTION));

        operations.add(new ModelMBeanOperationInfo(
                "addFilterAfter", "addFilterAfter", new MBeanParameterInfo[] {
                        new MBeanParameterInfo(
                                "baseName", String.class.getName(), "the previous filter name"),
                        new MBeanParameterInfo(
                                "name", String.class.getName(), "the new filter name"),
                        new MBeanParameterInfo(
                                "filter", ObjectName.class.getName(), "the ObjectName reference to the filter")
                }, void.class.getName(), ModelMBeanOperationInfo.ACTION));
        
        operations.add(new ModelMBeanOperationInfo(
                "removeFilter", "removeFilter", new MBeanParameterInfo[] {
                        new MBeanParameterInfo(
                                "name", String.class.getName(), "the name of the filter to be removed"),
                }, void.class.getName(), ModelMBeanOperationInfo.ACTION));
    }

    @Override
    protected boolean isOperation(String methodName, Class<?>[] paramTypes) {
        // Ignore some IoSession methods.
        if (methodName.matches(
                "(write|read|(remove|replace|contains)Attribute)")) {
            return false;
        }
        
        return super.isOperation(methodName, paramTypes);
    }
}
