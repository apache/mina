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

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeChangeNotification;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ReflectionException;
import javax.management.RuntimeOperationsException;
import javax.management.modelmbean.InvalidTargetObjectTypeException;
import javax.management.modelmbean.ModelMBean;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanConstructorInfo;
import javax.management.modelmbean.ModelMBeanInfo;
import javax.management.modelmbean.ModelMBeanInfoSupport;
import javax.management.modelmbean.ModelMBeanNotificationInfo;
import javax.management.modelmbean.ModelMBeanOperationInfo;

import org.apache.commons.beanutils.MethodUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.mina.common.AttributeKey;
import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoSessionDataStructureFactory;
import org.apache.mina.common.TrafficMask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DefaultModelMBean implements ModelMBean {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Object source;
    private final ModelMBeanInfo info;

    public DefaultModelMBean(Object source) {
        if (source == null) {
            throw new NullPointerException("source");
        }
        
        this.source = source;
        this.info = createModelMBeanInfo(source);
    }
    
    public Object getAttribute(String name) throws AttributeNotFoundException,
            MBeanException, ReflectionException {
        try {
            return convert(
                    name, PropertyUtils.getNestedProperty(source, name));
        } catch (IllegalAccessException e) {
            throw new ReflectionException(e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw new MBeanException((Exception) cause);
            } else {
                throw new MBeanException(new Exception(cause));
            }
        } catch (NoSuchMethodException e) {
            throw new AttributeNotFoundException(name);
        }
    }

    public AttributeList getAttributes(String names[]) {
        AttributeList answer = new AttributeList();
        for (int i = 0; i < names.length; i++) {
            try {
                answer.add(new Attribute(names[i], getAttribute(names[i])));
            } catch (Exception e) {
                // Ignore.
            }
        }
        return answer;
    }

    public MBeanInfo getMBeanInfo() {
        return (MBeanInfo) info.clone();
    }

    public Object invoke(String name, Object params[], String signature[])
            throws MBeanException, ReflectionException {
        try {
            Class<?>[] paramTypes = new Class[signature.length];
            for (int i = 0; i < paramTypes.length; i ++) {
                paramTypes[i] = getAttributeClass(signature[i]);
            }
            return MethodUtils.invokeMethod(source, name, params, paramTypes);
        } catch (ClassNotFoundException e) {
            throw new ReflectionException(e);
        } catch (NoSuchMethodException e) {
            throw new ReflectionException(e);
        } catch (IllegalAccessException e) {
            throw new ReflectionException(e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw new MBeanException((Exception) cause);
            } else {
                throw new MBeanException(new Exception(cause));
            }
        }
    }

    private Class<?> getAttributeClass(String signature)
            throws ClassNotFoundException {
        if (signature.equals(Boolean.TYPE.getName())) {
            return Boolean.TYPE;
        }
        if (signature.equals(Byte.TYPE.getName())) {
            return Byte.TYPE;
        }
        if (signature.equals(Character.TYPE.getName())) {
            return Character.TYPE;
        }
        if (signature.equals(Double.TYPE.getName())) {
            return Double.TYPE;
        }
        if (signature.equals(Float.TYPE.getName())) {
            return Float.TYPE;
        }
        if (signature.equals(Integer.TYPE.getName())) {
            return Integer.TYPE;
        }
        if (signature.equals(Long.TYPE.getName())) {
            return Long.TYPE;
        }
        if (signature.equals(Short.TYPE.getName())) {
            return Short.TYPE;
        }

        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl != null) {
                return cl.loadClass(signature);
            }
        } catch (ClassNotFoundException e) {
        }
        
        return Class.forName(signature);
    }

    public void setAttribute(Attribute attribute)
            throws AttributeNotFoundException, MBeanException,
            ReflectionException {
        try {
            PropertyUtils.setNestedProperty(
                    source, attribute.getName(), attribute.getValue());
        } catch (IllegalAccessException e) {
            throw new ReflectionException(e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw new MBeanException((Exception) cause);
            } else {
                throw new MBeanException(new Exception(cause));
            }
        } catch (NoSuchMethodException e) {
            throw new AttributeNotFoundException(attribute.getName());
        }
    }

    public AttributeList setAttributes(AttributeList attributes) {
        // Prepare and return our response, eating all exceptions
        String names[] = new String[attributes.size()];
        int n = 0;
        Iterator<Object> items = attributes.iterator();
        while (items.hasNext()) {
            Attribute item = (Attribute) items.next();
            names[n++] = item.getName();
            try {
                setAttribute(item);
            } catch (Exception e) {
                ; // Ignore all exceptions
            }
        }

        return getAttributes(names);
    }

    public void setManagedResource(Object resource, String type)
            throws InstanceNotFoundException, InvalidTargetObjectTypeException,
            MBeanException {
        throw new RuntimeOperationsException(new UnsupportedOperationException());

    }

    public void setModelMBeanInfo(ModelMBeanInfo info) throws MBeanException {
        throw new RuntimeOperationsException(new UnsupportedOperationException());
    }

    public void addAttributeChangeNotificationListener(
            NotificationListener listener, String name, Object handback) {
    }

    public void removeAttributeChangeNotificationListener(
            NotificationListener listener, String name)
            throws ListenerNotFoundException {
    }

    public void sendAttributeChangeNotification(
            AttributeChangeNotification notification) throws MBeanException {
        throw new RuntimeOperationsException(new UnsupportedOperationException());
    }

    public void sendAttributeChangeNotification(Attribute oldValue,
            Attribute newValue) throws MBeanException {
        throw new RuntimeOperationsException(new UnsupportedOperationException());
    }

    public void sendNotification(Notification notification)
            throws MBeanException {
        throw new RuntimeOperationsException(new UnsupportedOperationException());
    }

    public void sendNotification(String message) throws MBeanException {
        throw new RuntimeOperationsException(new UnsupportedOperationException());

    }

    public void addNotificationListener(NotificationListener listener,
            NotificationFilter filter, Object handback)
            throws IllegalArgumentException {
    }

    public MBeanNotificationInfo[] getNotificationInfo() {
        return new MBeanNotificationInfo[0];
    }

    public void removeNotificationListener(NotificationListener listener)
            throws ListenerNotFoundException {
    }

    public void load() throws InstanceNotFoundException, MBeanException,
            RuntimeOperationsException {
        throw new RuntimeOperationsException(new UnsupportedOperationException());
    }

    public void store() throws InstanceNotFoundException, MBeanException,
            RuntimeOperationsException {
        throw new RuntimeOperationsException(new UnsupportedOperationException());
    }

    @Override
    public String toString() {
        return source.toString();
    }
    
    private ModelMBeanInfo createModelMBeanInfo(Object source) {
        String className = source.getClass().getName();
        String description = "";
        
        ModelMBeanConstructorInfo[] constructors = new ModelMBeanConstructorInfo[0];
        ModelMBeanNotificationInfo[] notifications = new ModelMBeanNotificationInfo[0];
        
        List<ModelMBeanAttributeInfo> attributes = new ArrayList<ModelMBeanAttributeInfo>();
        List<ModelMBeanOperationInfo> operations = new ArrayList<ModelMBeanOperationInfo>();
        
        addAttributes(attributes, source, source.getClass(), "");
        
        return new ModelMBeanInfoSupport(
                className, description,
                attributes.toArray(new ModelMBeanAttributeInfo[attributes.size()]),
                constructors,
                operations.toArray(new ModelMBeanOperationInfo[operations.size()]),
                notifications);
    }

    private void addAttributes(
            List<ModelMBeanAttributeInfo> attributes,
            Object object, Class<?> type, String prefix) {
        PropertyDescriptor[] pds = PropertyUtils.getPropertyDescriptors(type);
        for (PropertyDescriptor p: pds) {
            // Ignore a write-only property.
            if (p.getReadMethod() == null) {
                continue;
            }
            
            // Ignore unmanageable property.
            String pname = p.getName();
            if (pname.equals("class")) {
                continue;
            }
            if (IoService.class.isAssignableFrom(type) && pname.equals("filterChain")) {
                continue;
            }
            if (IoSession.class.isAssignableFrom(type) && pname.equals("attachment")) {
                continue;
            }
            if (IoSession.class.isAssignableFrom(type) && pname.equals("closeFuture")) {
                continue;
            }
            
            // Expandable property.
            boolean expanded = false;
            expanded |= expandAttribute(
                    attributes, IoService.class, "sessionConfig", object, type, p);
            expanded |= expandAttribute(
                    attributes, IoService.class, "transportMetadata", object, type, p);
            expanded |= expandAttribute(
                    attributes, IoSession.class, "config", object, type, p);
            expanded |= expandAttribute(
                    attributes, IoSession.class, "transportMetadata", object, type, p);
    
            if (expanded) {
                continue;
            }
    
            // Ordinary property.
            try {
                attributes.add(new ModelMBeanAttributeInfo(
                        prefix + pname, p.getShortDescription(),
                        p.getReadMethod(), p.getWriteMethod()));
            } catch (IntrospectionException e) {
                logger.debug("Unexpected exception.", e);
            }
        }
    }

    private boolean expandAttribute(
            List<ModelMBeanAttributeInfo> attributes,
            Class<?> expectedType, String expectedPropertyName,
            Object object, Class<?> type, PropertyDescriptor descriptor) {
        if (expectedType.isAssignableFrom(type)) {
            if (descriptor.getName().equals(expectedPropertyName)) {
                Object property;
                try {
                    property = PropertyUtils.getProperty(
                            object, expectedPropertyName);
                } catch (Exception e) {
                    logger.debug("Unexpected exception.", e);
                    return false;
                }
                
                addAttributes(
                        attributes,
                        property, property.getClass(),
                        expectedPropertyName + '.');
                return true;
            }
        }
        return false;
    }

    private Object convert(String name, Object v) {
        if (v == null) {
            return null;
        }

        if (v instanceof Class) {
            return ((Class<?>) v).getName();
        }
        
        if (v instanceof Long && name.endsWith("Time")) {
            long time = (Long) v;
            if (time <= 0) {
                return null;
            }
            return new Date(time);
        }
        
        if (v instanceof Set) {
            return convertCollection(v, new HashSet<Object>());
        }
        
        if (v instanceof List) {
            return convertCollection(v, new ArrayList<Object>());
        }
        
        if (v instanceof AttributeKey) {
            return String.valueOf(v);
        }
        
        if (v instanceof IoSession) {
            return ((IoSession) v).getId();
        }
        
        if (v instanceof IoService) {
            // FIXME getId()
            return String.valueOf(v);
        }
        
        if (v instanceof IoSessionDataStructureFactory ||
            v instanceof IoHandler) {
            return v.getClass().getName();
        }
        
        if (v instanceof DefaultIoFilterChainBuilder) {
            List<String> filterNames = new ArrayList<String>();
            for (IoFilterChain.Entry e: ((DefaultIoFilterChainBuilder) v).getAll()) {
                filterNames.add(e.getName());
            }
            return filterNames;
        }

        if (v instanceof IoFilterChain) {
            List<String> filterNames = new ArrayList<String>();
            for (IoFilterChain.Entry e: ((IoFilterChain) v).getAll()) {
                filterNames.add(e.getName());
            }
            return filterNames;
        }
        
        if (v instanceof TrafficMask) {
            TrafficMask m = (TrafficMask) v;
            return (m.isReadable()? "r" : "") + (m.isWritable()? "w" : ""); 
        }

        return v;
    }
    
    private Object convertCollection(Object src, Collection<Object> dst) {
        Collection<?> srcCol = (Collection<?>) src;
        for (Object e: srcCol) {
            dst.add(convert("element", e));
        }
        return dst;
    }
}
