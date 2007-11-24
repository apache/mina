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
import java.beans.PropertyEditor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeChangeNotification;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
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
import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoFilter;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoFilterChainBuilder;
import org.apache.mina.common.IoFuture;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoSessionDataStructureFactory;
import org.apache.mina.common.TransportMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DefaultModelMBean implements ModelMBean, MBeanRegistration {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Object source;
    private final TransportMetadata transportMetadata;
    private final MBeanInfo info;
    private final Map<String, PropertyDescriptor> propertyDescriptors =
        new HashMap<String, PropertyDescriptor>();
    
    private volatile MBeanServer server;
    private volatile ObjectName name;

    public DefaultModelMBean(Object source) {
        if (source == null) {
            throw new NullPointerException("source");
        }
        
        this.source = source;
        
        if (source instanceof IoService) {
            transportMetadata = ((IoService) source).getTransportMetadata();
        } else if (source instanceof IoSession) {
            transportMetadata = ((IoSession) source).getTransportMetadata();
        } else {
            transportMetadata = null;
        }
        
        this.info = createModelMBeanInfo(source);
    }
    
    public Object getAttribute(String name) throws AttributeNotFoundException,
            MBeanException, ReflectionException {
        
        // Handle synthetic attributes first.
        if (source instanceof IoSession) {
            IoSession s = (IoSession) source;
            if (name.equals("attributes")) {
                Map<Object, Object> result = new HashMap<Object, Object>();
                for (Object key: s.getAttributeKeys()) {
                    result.put(key, s.getAttribute(key));
                }
                return convertAttributeValue("attributes", result);
            }
            if (name.equals("readerIdleCount")) {
                return s.getIdleCount(IdleStatus.READER_IDLE);
            }
            if (name.equals("writerIdleCount")) {
                return s.getIdleCount(IdleStatus.WRITER_IDLE);
            }
            if (name.equals("bothIdleCount")) {
                return s.getIdleCount(IdleStatus.BOTH_IDLE);
            }
            if (name.startsWith("config.readerIdleTime")) {
                return s.getConfig().getIdleTime(IdleStatus.READER_IDLE) *
                       (name.endsWith("InMillis")? 1000L : 1L);
            }
            if (name.startsWith("config.writerIdleTime")) {
                return s.getConfig().getIdleTime(IdleStatus.WRITER_IDLE) *
                (name.endsWith("InMillis")? 1000L : 1L);
            }
            if (name.startsWith("config.bothIdleTime")) {
                return s.getConfig().getIdleTime(IdleStatus.BOTH_IDLE) *
                (name.endsWith("InMillis")? 1000L : 1L);
            }
        }
        
        if (source instanceof IoService) {
            IoService s = (IoService) source;
            if (name.equals("readerIdleCount")) {
                return s.getIdleCount(IdleStatus.READER_IDLE);
            }
            if (name.equals("writerIdleCount")) {
                return s.getIdleCount(IdleStatus.WRITER_IDLE);
            }
            if (name.equals("bothIdleCount")) {
                return s.getIdleCount(IdleStatus.BOTH_IDLE);
            }
            if (name.startsWith("readerIdleTime")) {
                return s.getIdleTime(IdleStatus.READER_IDLE) *
                       (name.endsWith("InMillis")? 1000 : 1);
            }
            if (name.startsWith("writerIdleTime")) {
                return s.getIdleTime(IdleStatus.WRITER_IDLE) *
                (name.endsWith("InMillis")? 1000 : 1);
            }
            if (name.startsWith("bothIdleTime")) {
                return s.getIdleTime(IdleStatus.BOTH_IDLE) *
                (name.endsWith("InMillis")? 1000 : 1);
            }
            if (name.startsWith("sessionConfig.readerIdleTime")) {
                return s.getSessionConfig().getIdleTime(IdleStatus.READER_IDLE) *
                       (name.endsWith("InMillis")? 1000L : 1L);
            }
            if (name.startsWith("sessionConfig.writerIdleTime")) {
                return s.getSessionConfig().getIdleTime(IdleStatus.WRITER_IDLE) *
                (name.endsWith("InMillis")? 1000L : 1L);
            }
            if (name.startsWith("sessionConfig.bothIdleTime")) {
                return s.getSessionConfig().getIdleTime(IdleStatus.BOTH_IDLE) *
                (name.endsWith("InMillis")? 1000L : 1L);
            }
        }
        
        // And then try reflection.
        try {
            return convertAttributeValue(
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
        return info;
    }

    public Object invoke(String name, Object params[], String signature[])
            throws MBeanException, ReflectionException {

        // Handle synthetic operations first.
        if (name.equals("unregisterMBean")) {
            try {
                server.unregisterMBean(this.name);
                return null;
            } catch (InstanceNotFoundException e) {
                throw new MBeanException(e);
            }
        }
        
        // And then try reflection.
        try {
            Class<?>[] paramTypes = new Class[signature.length];
            for (int i = 0; i < paramTypes.length; i ++) {
                paramTypes[i] = getAttributeClass(signature[i]);
            }
            return convertAttributeValue(
                    "", MethodUtils.invokeMethod(source, name, params, paramTypes));
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
        String aname = attribute.getName();
        Object avalue = attribute.getValue();
        
        // Handle synthetic attributes first.
        if (source instanceof IoSession) {
            IoSession s = (IoSession) source;
            if (aname.equals("config.readerIdleTime")) {
                s.getConfig().setIdleTime(
                        IdleStatus.READER_IDLE,
                        (Integer) convert(avalue, Integer.class));
                return;
            }
            if (aname.equals("config.writerIdleTime")) {
                s.getConfig().setIdleTime(
                        IdleStatus.WRITER_IDLE,
                        (Integer) convert(avalue, Integer.class));
                return;
            }
            if (aname.equals("config.bothIdleTime")) {
                s.getConfig().setIdleTime(
                        IdleStatus.BOTH_IDLE,
                        (Integer) convert(avalue, Integer.class));
                return;
            }
        }
        
        if (source instanceof IoService) {
            IoService s = (IoService) source;
            if (aname.equals("readerIdleTime")) {
                s.setIdleTime(
                        IdleStatus.READER_IDLE,
                        (Integer) convert(avalue, Integer.class));
                return;
            }
            if (aname.equals("writerIdleTime")) {
                s.setIdleTime(
                        IdleStatus.WRITER_IDLE,
                        (Integer) convert(avalue, Integer.class));
                return;
            }
            if (aname.equals("bothIdleTime")) {
                s.setIdleTime(
                        IdleStatus.BOTH_IDLE,
                        (Integer) convert(avalue, Integer.class));
                return;
            }
            if (aname.equals("sessionConfig.readerIdleTime")) {
                s.getSessionConfig().setIdleTime(
                        IdleStatus.READER_IDLE,
                        (Integer) convert(avalue, Integer.class));
                return;
            }
            if (aname.equals("sessionConfig.writerIdleTime")) {
                s.getSessionConfig().setIdleTime(
                        IdleStatus.WRITER_IDLE,
                        (Integer) convert(avalue, Integer.class));
                return;
            }
            if (aname.equals("sessionConfig.bothIdleTime")) {
                s.getSessionConfig().setIdleTime(
                        IdleStatus.BOTH_IDLE,
                        (Integer) convert(avalue, Integer.class));
                return;
            }
        }
        
        // And then try reflection.
        try {
            PropertyUtils.setNestedProperty(
                    source, aname, convert(
                            avalue, propertyDescriptors.get(aname).getPropertyType()));
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

    public ObjectName preRegister(MBeanServer server, ObjectName name)
            throws Exception {
        this.server = server;
        this.name = name;
        return name;
    }

    public void postRegister(Boolean registrationDone) {
    }

    public void preDeregister() throws Exception {
    }

    public void postDeregister() {
        this.server = null;
        this.name = null;
    }

    @Override
    public String toString() {
        return source.toString();
    }

    protected MBeanInfo createModelMBeanInfo(Object source) {
        String className = source.getClass().getName();
        String description = "";
        
        ModelMBeanConstructorInfo[] constructors = new ModelMBeanConstructorInfo[0];
        ModelMBeanNotificationInfo[] notifications = new ModelMBeanNotificationInfo[0];
        
        List<ModelMBeanAttributeInfo> attributes = new ArrayList<ModelMBeanAttributeInfo>();
        List<ModelMBeanOperationInfo> operations = new ArrayList<ModelMBeanOperationInfo>();
        
        addAttributes(attributes, source);
        addOperations(operations, source);
        
        return new ModelMBeanInfoSupport(
                className, description,
                attributes.toArray(new ModelMBeanAttributeInfo[attributes.size()]),
                constructors,
                operations.toArray(new ModelMBeanOperationInfo[operations.size()]),
                notifications);
    }
    
    protected void addAttributes(
            List<ModelMBeanAttributeInfo> attributes, Object object) {
        
        addAttributes(attributes, object, object.getClass(), "");
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
            if (IoSession.class.isAssignableFrom(type) && pname.equals("attributeKeys")) {
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
            String fqpn = prefix + pname;
            boolean writable = p.getWriteMethod() != null | isWritable(type, pname);
            attributes.add(new ModelMBeanAttributeInfo(
                    fqpn, convertAttributeType(fqpn, p.getPropertyType()).getName(),
                    p.getShortDescription(),
                    true, writable,
                    p.getReadMethod().getName().startsWith("is")));
            
            propertyDescriptors.put(fqpn, p);
        }
        
        if (object instanceof IoSession) {
            attributes.add(new ModelMBeanAttributeInfo(
                    "attributes", Map.class.getName(), "attributes",
                    true, false, false));
            attributes.add(new ModelMBeanAttributeInfo(
                    "readerIdleCount", Integer.class.getName(), "readerIdleCount",
                    true, false, false));
            attributes.add(new ModelMBeanAttributeInfo(
                    "writerIdleCount", Integer.class.getName(), "writerIdleCount",
                    true, false, false));
            attributes.add(new ModelMBeanAttributeInfo(
                    "bothIdleCount", Integer.class.getName(), "bothIdleCount",
                    true, false, false));
            attributes.add(new ModelMBeanAttributeInfo(
                    "config.readerIdleTime", Long.class.getName(), "config.readerIdleTime",
                    true, true, false));
            attributes.add(new ModelMBeanAttributeInfo(
                    "config.writerIdleTime", Long.class.getName(), "config.writerIdleTime",
                    true, true, false));
            attributes.add(new ModelMBeanAttributeInfo(
                    "config.bothIdleTime", Long.class.getName(), "config.bothIdleTime",
                    true, true, false));
            attributes.add(new ModelMBeanAttributeInfo(
                    "config.readerIdleTimeInMillis", Long.class.getName(), "config.readerIdleTimeInMillis",
                    true, false, false));
            attributes.add(new ModelMBeanAttributeInfo(
                    "config.writerIdleTimeInMillis", Long.class.getName(), "config.writerIdleTimeInMillis",
                    true, false, false));
            attributes.add(new ModelMBeanAttributeInfo(
                    "config.bothIdleTimeInMillis", Long.class.getName(), "config.bothIdleTimeInMillis",
                    true, false, false));
        }
        
        if (object instanceof IoService) {
            attributes.add(new ModelMBeanAttributeInfo(
                    "readerIdleCount", Integer.class.getName(), "readerIdleCount",
                    true, false, false));
            attributes.add(new ModelMBeanAttributeInfo(
                    "writerIdleCount", Integer.class.getName(), "writerIdleCount",
                    true, false, false));
            attributes.add(new ModelMBeanAttributeInfo(
                    "bothIdleCount", Integer.class.getName(), "bothIdleCount",
                    true, false, false));
            attributes.add(new ModelMBeanAttributeInfo(
                    "readerIdleTime", Long.class.getName(), "readerIdleTime",
                    true, true, false));
            attributes.add(new ModelMBeanAttributeInfo(
                    "writerIdleTime", Long.class.getName(), "writerIdleTime",
                    true, true, false));
            attributes.add(new ModelMBeanAttributeInfo(
                    "bothIdleTime", Long.class.getName(), "bothIdleTime",
                    true, true, false));
            attributes.add(new ModelMBeanAttributeInfo(
                    "readerIdleTimeInMillis", Long.class.getName(), "readerIdleTimeInMillis",
                    true, false, false));
            attributes.add(new ModelMBeanAttributeInfo(
                    "writerIdleTimeInMillis", Long.class.getName(), "writerIdleTimeInMillis",
                    true, false, false));
            attributes.add(new ModelMBeanAttributeInfo(
                    "bothIdleTimeInMillis", Long.class.getName(), "bothIdleTimeInMillis",
                    true, false, false));
            attributes.add(new ModelMBeanAttributeInfo(
                    "sessionConfig.readerIdleTime", Long.class.getName(), "sessionConfig.readerIdleTime",
                    true, true, false));
            attributes.add(new ModelMBeanAttributeInfo(
                    "sessionConfig.writerIdleTime", Long.class.getName(), "sessionConfig.writerIdleTime",
                    true, true, false));
            attributes.add(new ModelMBeanAttributeInfo(
                    "sessionConfig.bothIdleTime", Long.class.getName(), "sessionConfig.bothIdleTime",
                    true, true, false));
            attributes.add(new ModelMBeanAttributeInfo(
                    "sessionConfig.readerIdleTimeInMillis", Long.class.getName(), "sessionConfig.readerIdleTimeInMillis",
                    true, false, false));
            attributes.add(new ModelMBeanAttributeInfo(
                    "sessionConfig.writerIdleTimeInMillis", Long.class.getName(), "sessionConfig.writerIdleTimeInMillis",
                    true, false, false));
            attributes.add(new ModelMBeanAttributeInfo(
                    "sessionConfig.bothIdleTimeInMillis", Long.class.getName(), "sessionConfig.bothIdleTimeInMillis",
                    true, false, false));
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
    
    private void addOperations(
            List<ModelMBeanOperationInfo> operations, Object object) {

        for (Method m: object.getClass().getMethods()) {
            String mname = m.getName();
            // Ignore getters and setters.
            if (mname.startsWith("is") || mname.startsWith("get") ||
                mname.startsWith("set")) {
                continue;
            }
            
            // Ignore Object methods.
            if (mname.matches(
                    "(wait|notify|notifyAll|toString|equals|compareTo|hashCode)")) {
                continue;
            }
            
            // Ignore some IoServide methods.
            if (object instanceof IoService && mname.matches(
                    "(newSession|broadcast|(add|remove)Listener)")) {
                continue;
            }

            // Ignore some IoSession methods.
            if (object instanceof IoSession && mname.matches(
                    "(write|read|(remove|replace|contains)Attribute)")) {
                continue;
            }
            
            // Ignore some IoFilter methods.
            if (object instanceof IoFilter && mname.matches(
                    "(init|destroy|on(Pre|Post)(Add|Remove)|" +
                    "session(Created|Opened|Idle|Closed)|" +
                    "exceptionCaught|message(Received|Sent)|" +
                    "filter(Close|Write|SetTrafficMask))")) {
                continue;
            }
            
            List<MBeanParameterInfo> signature = new ArrayList<MBeanParameterInfo>();
            int i = 1;
            for (Class<?> ptype: m.getParameterTypes()) {
                String pname = "p" + (i ++);
                signature.add(new MBeanParameterInfo(
                        pname, convertAttributeType("", ptype).getName(), pname));
            }

            operations.add(new ModelMBeanOperationInfo(
                    m.getName(), m.getName(),
                    signature.toArray(new MBeanParameterInfo[signature.size()]),
                    convertOperationReturnType(m.getReturnType()).getName(),
                    ModelMBeanOperationInfo.ACTION));
        }
        
        operations.add(new ModelMBeanOperationInfo(
                "unregisterMBean", "unregisterMBean",
                new MBeanParameterInfo[0], void.class.getName(), 
                ModelMBeanOperationInfo.ACTION));
    }
    
    protected boolean isWritable(Class<?> type, String pname) {
        return IoService.class.isAssignableFrom(type) && 
               pname.equals("localAddresses");
    }
    
    protected Object convert(Object v, Class<?> dstType) throws ReflectionException {
        if (v == null) {
            return null;
        }
        
        if (dstType.isAssignableFrom(v.getClass())) {
            return v;
        }
        
        if (v instanceof String) {
            PropertyEditor editor = getPropertyEditor(dstType);
            if (editor == null) {
                throw new ReflectionException(new ClassNotFoundException(
                        "Failed to find a PropertyEditor for " +
                        dstType.getSimpleName()));
            }
            editor.setAsText((String) v);
            return editor.getValue();
        }
        
        if (v instanceof Number) {
            Number n = (Number) v;
            if (Number.class.isAssignableFrom(dstType)) {
                if (dstType == Byte.class) {
                    return n.byteValue();
                }
                if (dstType == Double.class) {
                    return n.doubleValue();
                }
                if (dstType == Float.class) {
                    return n.floatValue();
                }
                if (dstType == Integer.class) {
                    return n.intValue();
                }
                if (dstType == Short.class) {
                    return n.shortValue();
                }
            }
        }

        return v;
    }

    protected PropertyEditor getPropertyEditor(Class<?> propType) {
        if (transportMetadata != null && propType == SocketAddress.class) {
            propType = transportMetadata.getAddressType();
        }

        try {
            return (PropertyEditor) getClass().getClassLoader().loadClass(
                "org.apache.mina.integration.beans." +
                propType.getSimpleName() + "Editor").newInstance();
        } catch (Exception e) {
            return null;
        }
    }
    
    protected Class<?> convertOperationReturnType(Class<?> opReturnType) {
        if (IoFuture.class.isAssignableFrom(opReturnType)) {
            return void.class;
        }
        if (opReturnType == void.class || opReturnType == Void.class) {
            return void.class;
        }

        return convertAttributeType("", opReturnType);
    }

    protected Class<?> convertAttributeType(
            String attrName, Class<?> attrType) {

        if ((attrType == Long.class || attrType == long.class)) {
            if (attrName.endsWith("Time") &&
                !propertyDescriptors.containsKey(attrName + "InMillis")) {
                return Date.class;
            }
            
            if (attrName.equals("id")) {
                return String.class;
            }
        }
        
        if (IoFilterChain.class.isAssignableFrom(attrType)) {
            return List.class;
        }
        
        if (IoFilterChainBuilder.class.isAssignableFrom(attrType)) {
            return List.class;
        }
        
        if (attrType.isPrimitive()) {
            if (attrType == boolean.class) {
                return Boolean.class;
            }
            if (attrType == byte.class) {
                return Byte.class;
            }
            if (attrType == char.class) {
                return Character.class;
            }
            if (attrType == double.class) {
                return Double.class;
            }
            if (attrType == float.class) {
                return Float.class;
            }
            if (attrType == int.class) {
                return Integer.class;
            }
            if (attrType == long.class) {
                return Long.class;
            }
            if (attrType == short.class) {
                return Short.class;
            }
        }
        
        if (Date.class.isAssignableFrom(attrType) ||
            Boolean.class.isAssignableFrom(attrType) ||
            Character.class.isAssignableFrom(attrType) ||
            Number.class.isAssignableFrom(attrType)) {
            return attrType;
        }

        return String.class;
    }
    
    protected Object convertAttributeValue(String attrName, Object v) {
        if (v == null) {
            return null;
        }
        
        if (v instanceof Class) {
            return ((Class<?>) v).getName();
        }
        
        if (v instanceof Long) {
            long l = (Long) v;
            if (attrName.endsWith("Time") &&
                !propertyDescriptors.containsKey(attrName + "InMillis")) {
                if (l <= 0) {
                    return null;
                }
                return new Date(l);
            }
            
            if (attrName.equals("id")) {
                // ID in MINA is a unsigned 32-bit integer.
                String id = Long.toHexString(l).toUpperCase();
                while (id.length() < 8) {
                    id = '0' + id; // padding
                }
                return id;
            }
        }
        
        if (v instanceof Set) {
            return convertCollection(v, new HashSet<Object>());
        }
        
        if (v instanceof List) {
            return convertCollection(v, new ArrayList<Object>());
        }
        
        if (v instanceof Map) {
            return convertCollection(v, new HashMap<Object, Object>());
        }
        
        if (v instanceof IoSessionDataStructureFactory ||
            v instanceof IoHandler) {
            return v.getClass().getName();
        }
        
        if (v instanceof IoFilterChainBuilder) {
            List<String> filterNames = new ArrayList<String>();
            if (v instanceof DefaultIoFilterChainBuilder) {
                for (IoFilterChain.Entry e: ((DefaultIoFilterChainBuilder) v).getAll()) {
                    filterNames.add(e.getName());
                }
            } else {
                filterNames.add("Unknown builder: " + v.getClass().getName());
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
        
        if (v instanceof IoFilterChainBuilder) {
            return v.getClass().getName();
        }
        
        if (v.getClass().isPrimitive() ||
            Date.class.isAssignableFrom(v.getClass()) ||
            Boolean.class.isAssignableFrom(v.getClass()) ||
            Character.class.isAssignableFrom(v.getClass()) ||
            Number.class.isAssignableFrom(v.getClass())) {
            return v;
        }

        PropertyEditor editor = getPropertyEditor(v.getClass());
        if (editor != null) {
            editor.setValue(v);
            return editor.getAsText();
        }
        
        return v.toString();
    }
    
    private Object convertCollection(Object src, Collection<Object> dst) {
        Collection<?> srcCol = (Collection<?>) src;
        for (Object e: srcCol) {
            Object convertedValue = convertAttributeValue("element", e);
            if (e != null && convertedValue == null) {
                convertedValue = e.toString();
            }
            dst.add(convertedValue);
        }
        return dst;
    }

    private Object convertCollection(Object src, Map<Object, Object> dst) {
        Map<?, ?> srcCol = (Map<?, ?>) src;
        for (Map.Entry<?, ?> e: srcCol.entrySet()) {
            Object convertedKey = convertAttributeValue("key", e.getKey());
            Object convertedValue = convertAttributeValue("value", e.getValue());
            if (e.getKey() != null && convertedKey == null) {
                convertedKey = e.getKey().toString();
            }
            if (e.getValue() != null && convertedValue == null) {
                convertedKey = e.getValue().toString();
            }
            dst.put(convertedKey, convertedValue);
        }
        return dst;
    }
}
