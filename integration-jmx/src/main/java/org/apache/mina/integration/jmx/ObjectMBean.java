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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

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

import ognl.Ognl;
import ognl.OgnlException;
import ognl.OgnlRuntime;

import org.apache.commons.beanutils.MethodUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IoFilter;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoFilterChainBuilder;
import org.apache.mina.common.IoFuture;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoSessionDataStructureFactory;
import org.apache.mina.common.TransportMetadata;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.integration.beans.PropertyEditorFactory;
import org.apache.mina.integration.ognl.IoFilterPropertyAccessor;
import org.apache.mina.integration.ognl.IoServicePropertyAccessor;
import org.apache.mina.integration.ognl.IoSessionPropertyAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ModelMBean} wrapper implementation for a POJO.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 * 
 * @param <T> the type of the managed object
 */
public class ObjectMBean<T> implements ModelMBean, MBeanRegistration {

    private static final Map<ObjectName, Object> sources =
        new ConcurrentHashMap<ObjectName, Object>();
    
    public static Object getSource(ObjectName oname) {
        return sources.get(oname);
    }
    
    static {
        OgnlRuntime.setPropertyAccessor(IoService.class, new IoServicePropertyAccessor());
        OgnlRuntime.setPropertyAccessor(IoSession.class, new IoSessionPropertyAccessor());
        OgnlRuntime.setPropertyAccessor(IoFilter.class, new IoFilterPropertyAccessor());
    }
    
    protected static final void throwMBeanException(OgnlException e) throws MBeanException {
        Throwable reason = e.getReason();
        if (reason == null) {
            throw new MBeanException(new IllegalArgumentException(e.getClass().getName() + ": " + e.getMessage()));
        }
        if (reason instanceof OgnlException) {
            throw new MBeanException(new IllegalArgumentException(reason.getClass().getName() + ": " + reason.getMessage()));
        }
        if (reason instanceof Exception) {
            throw new MBeanException((Exception) reason);
        }
        throw new MBeanException(new IllegalArgumentException(reason.getClass().getName() + ": " + reason.getMessage()));
    }

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final T source;
    private final TransportMetadata transportMetadata;
    private final MBeanInfo info;
    private final Map<String, PropertyDescriptor> propertyDescriptors =
        new HashMap<String, PropertyDescriptor>();
    
    private volatile MBeanServer server;
    private volatile ObjectName name;

    /**
     * Creates a new instance with the specified POJO.
     */
    public ObjectMBean(T source) {
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
        try {
            return convertAttributeValue(
                    name, Ognl.getValue(name, source));
        } catch (OgnlException e) {
            throwMBeanException(e);
            throw new InternalError();
        }
    }
    
    public void setAttribute(Attribute attribute)
            throws AttributeNotFoundException, MBeanException,
            ReflectionException {
        String aname = attribute.getName();
        Object avalue = attribute.getValue();
        
        try {
            Ognl.setValue(aname, source, convert(
                    avalue, propertyDescriptors.get(aname).getPropertyType()));
        } catch (OgnlException e) {
            throwMBeanException(e);
        }
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
            return convertReturnValue(
                    MethodUtils.invokeMethod(source, name, params, paramTypes));
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

    public final T getSource() {
        return source;
    }
    
    public final MBeanServer getServer() {
        return server;
    }
    
    public final ObjectName getName() {
        return name;
    }

    public final MBeanInfo getMBeanInfo() {
        return info;
    }

    public final AttributeList getAttributes(String names[]) {
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

    public final AttributeList setAttributes(AttributeList attributes) {
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

    public final void setManagedResource(Object resource, String type)
            throws InstanceNotFoundException, InvalidTargetObjectTypeException,
            MBeanException {
        throw new RuntimeOperationsException(new UnsupportedOperationException());

    }

    public final void setModelMBeanInfo(ModelMBeanInfo info) throws MBeanException {
        throw new RuntimeOperationsException(new UnsupportedOperationException());
    }

    @Override
    public final String toString() {
        return source.toString();
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

    public final ObjectName preRegister(MBeanServer server, ObjectName name)
            throws Exception {
        this.server = server;
        this.name = name;
        return name;
    }

    public final void postRegister(Boolean registrationDone) {
        if (registrationDone) {
            sources.put(name, source);
        }
    }

    public final void preDeregister() throws Exception {
    }

    public final void postDeregister() {
        sources.remove(name);
        this.server = null;
        this.name = null;
    }

    private MBeanInfo createModelMBeanInfo(T source) {
        String className = source.getClass().getName();
        String description = "";
        
        ModelMBeanConstructorInfo[] constructors = new ModelMBeanConstructorInfo[0];
        ModelMBeanNotificationInfo[] notifications = new ModelMBeanNotificationInfo[0];
        
        List<ModelMBeanAttributeInfo> attributes = new ArrayList<ModelMBeanAttributeInfo>();
        List<ModelMBeanOperationInfo> operations = new ArrayList<ModelMBeanOperationInfo>();
        
        addAttributes(attributes, source);
        addExtraAttributes(attributes);
        
        addOperations(operations, source);
        addExtraOperations(operations);
        operations.add(new ModelMBeanOperationInfo(
                "unregisterMBean", "unregisterMBean",
                new MBeanParameterInfo[0], void.class.getName(), 
                ModelMBeanOperationInfo.ACTION));

        return new ModelMBeanInfoSupport(
                className, description,
                attributes.toArray(new ModelMBeanAttributeInfo[attributes.size()]),
                constructors,
                operations.toArray(new ModelMBeanOperationInfo[operations.size()]),
                notifications);
    }
    
    private void addAttributes(
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
            if (!isReadable(type, pname)) {
                continue;
            }
            
            // Expand if possible.
            if (isExpandable(type, pname)) {
                expandAttribute(attributes, object, prefix, pname);
                continue;
            }
    
            // Ordinary property.
            String fqpn = prefix + pname;
            boolean writable = p.getWriteMethod() != null || isWritable(type, pname);
            attributes.add(new ModelMBeanAttributeInfo(
                    fqpn, convertAttributeType(fqpn, p.getPropertyType()).getName(),
                    p.getShortDescription(),
                    true, writable,
                    p.getReadMethod().getName().startsWith("is")));
            
            propertyDescriptors.put(fqpn, p);
        }
    }

    private void expandAttribute(
            List<ModelMBeanAttributeInfo> attributes,
            Object object, String prefix, String propName) {
        Object property;
        try {
            property = PropertyUtils.getProperty(
                    object, propName);
        } catch (Exception e) {
            logger.debug("Unexpected exception.", e);
            return;
        }
        
        addAttributes(
                attributes,
                property, property.getClass(),
                prefix + propName + '.');
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
                    "(wait|notify|notifyAll|toString|equals|compareTo|hashCode|clone)")) {
                continue;
            }
            
            // Ignore other user-defined non-operations.
            if (!isOperation(mname, m.getParameterTypes())) {
                continue;
            }
            
            List<MBeanParameterInfo> signature = new ArrayList<MBeanParameterInfo>();
            int i = 1;
            for (Class<?> ptype: m.getParameterTypes()) {
                String pname = "p" + (i ++);
                signature.add(new MBeanParameterInfo(
                        pname, convertParameterType(ptype).getName(), pname));
            }

            operations.add(new ModelMBeanOperationInfo(
                    m.getName(), m.getName(),
                    signature.toArray(new MBeanParameterInfo[signature.size()]),
                    convertReturnType(m.getReturnType()).getName(),
                    ModelMBeanOperationInfo.ACTION));
        }
    }
    
    protected boolean isReadable(Class<?> type, String attrName) {
        if (IoService.class.isAssignableFrom(type) && attrName.equals("filterChain")) {
            return false;
        }
        if (IoSession.class.isAssignableFrom(type) && attrName.equals("attachment")) {
            return false;
        }
        if (IoSession.class.isAssignableFrom(type) && attrName.equals("attributeKeys")) {
            return false;
        }
        if (IoSession.class.isAssignableFrom(type) && attrName.equals("closeFuture")) {
            return false;
        }
        
        if (ThreadPoolExecutor.class.isAssignableFrom(type) && attrName.equals("queue")) {
            return false;
        }

        return true;
    }
    
    protected boolean isWritable(Class<?> type, String attrName) {
        if (IoService.class.isAssignableFrom(type) && attrName.startsWith("defaultLocalAddress")) {
            return true;
        }
        return false;
    }
    
    protected boolean isExpandable(Class<?> type, String attrName) {
        if (IoService.class.isAssignableFrom(type) && attrName.equals("sessionConfig")) {
            return true;
        }
        if (IoService.class.isAssignableFrom(type) && attrName.equals("transportMetadata")) {
            return true;
        }
        if (IoSession.class.isAssignableFrom(type) && attrName.equals("config")) {
            return true;
        }
        if (IoSession.class.isAssignableFrom(type) && attrName.equals("transportMetadata")) {
            return true;
        }

        if (ExecutorFilter.class.isAssignableFrom(type)) {
            if (attrName.equals("executor")) {
                return true;
            }
        }
        if (ThreadPoolExecutor.class.isAssignableFrom(type)) {
            if (attrName.equals("queueHandler")) {
                return true;
            }
        }
        return false;
    }
    
    @SuppressWarnings("unused")
    protected boolean isOperation(String methodName, Class<?>[] paramTypes) {
        return true;
    }
    
    @SuppressWarnings("unused")
    protected void addExtraAttributes(List<ModelMBeanAttributeInfo> attributes) {}
    
    @SuppressWarnings("unused")
    protected void addExtraOperations(List<ModelMBeanOperationInfo> operations) {}

    @SuppressWarnings("unchecked")
    protected Object convert(Object v, Class<?> dstType) throws ReflectionException {
        if (v == null) {
            return null;
        }
        
        if (dstType.isAssignableFrom(v.getClass())) {
            return v;
        }
        
        if (v instanceof String) {
            if (dstType.isEnum()) {
                return Enum.valueOf(dstType.asSubclass(Enum.class), (String) v);
            }
            
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

        return PropertyEditorFactory.getInstance(propType);
    }
    
    protected Class<?> convertAttributeType(
            String attrName, Class<?> attrType) {

        if ((attrType == Long.class || attrType == long.class)) {
            if (attrName.endsWith("Time") &&
                attrName.indexOf("Total") < 0 &&
                attrName.indexOf("Min") < 0 &&
                attrName.indexOf("Max") < 0 &&
                attrName.indexOf("Avg") < 0 &&
                attrName.indexOf("Average") < 0 &&
                !propertyDescriptors.containsKey(attrName + "InMillis")) {
                return Date.class;
            }
        }
        
        if (IoFilterChain.class.isAssignableFrom(attrType)) {
            return Map.class;
        }
        
        if (IoFilterChainBuilder.class.isAssignableFrom(attrType)) {
            return Map.class;
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
                attrName.indexOf("Total") < 0 &&
                attrName.indexOf("Min") < 0 &&
                attrName.indexOf("Max") < 0 &&
                attrName.indexOf("Avg") < 0 &&
                attrName.indexOf("Average") < 0 &&
                !propertyDescriptors.containsKey(attrName + "InMillis")) {
                if (l <= 0) {
                    return null;
                }
                return new Date(l);
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
            Map<String, String> filterMapping = new LinkedHashMap<String, String>();
            if (v instanceof DefaultIoFilterChainBuilder) {
                for (IoFilterChain.Entry e: ((DefaultIoFilterChainBuilder) v).getAll()) {
                    filterMapping.put(e.getName(), e.getClass().getName());
                }
            } else {
                filterMapping.put("Unknown builder type", v.getClass().getName());
            }
            return filterMapping;
        }

        if (v instanceof IoFilterChain) {
            Map<String, String> filterMapping = new LinkedHashMap<String, String>();
            for (IoFilterChain.Entry e: ((IoFilterChain) v).getAll()) {
                filterMapping.put(e.getName(), e.getFilter().getClass().getName());
            }
            return filterMapping;
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
    
    protected Class<?> convertParameterType(Class<?> paramType) {
        if (paramType.isPrimitive()) {
            return paramType;
        }
        
        return convertAttributeType("parameter", paramType);
    }

    protected Class<?> convertReturnType(Class<?> opReturnType) {
        if (IoFuture.class.isAssignableFrom(opReturnType)) {
            return void.class;
        }
        if (opReturnType == void.class || opReturnType == Void.class) {
            return void.class;
        }
    
        return convertAttributeType("", opReturnType);
    }

    protected Object convertReturnValue(Object value) {
        return convertAttributeValue("", value);
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
