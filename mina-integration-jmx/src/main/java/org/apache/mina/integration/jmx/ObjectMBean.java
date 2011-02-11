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

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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

import ognl.ExpressionSyntaxException;
import ognl.InappropriateExpressionException;
import ognl.NoSuchPropertyException;
import ognl.Ognl;
import ognl.OgnlContext;
import ognl.OgnlException;
import ognl.OgnlRuntime;
import ognl.TypeConverter;

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.filterchain.IoFilterChainBuilder;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoService;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionDataStructureFactory;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.integration.beans.CollectionEditor;
import org.apache.mina.integration.beans.ListEditor;
import org.apache.mina.integration.beans.MapEditor;
import org.apache.mina.integration.beans.PropertyEditorFactory;
import org.apache.mina.integration.beans.SetEditor;
import org.apache.mina.integration.ognl.IoFilterPropertyAccessor;
import org.apache.mina.integration.ognl.IoServicePropertyAccessor;
import org.apache.mina.integration.ognl.IoSessionPropertyAccessor;
import org.apache.mina.integration.ognl.PropertyTypeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ModelMBean} wrapper implementation for a POJO.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
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

    protected final static Logger LOGGER = LoggerFactory.getLogger(ObjectMBean.class);

    private final T source;
    private final TransportMetadata transportMetadata;
    private final MBeanInfo info;
    private final Map<String, PropertyDescriptor> propertyDescriptors =
        new HashMap<String, PropertyDescriptor>();
    private final TypeConverter typeConverter = new OgnlTypeConverter();

    private volatile MBeanServer server;
    private volatile ObjectName name;

    /**
     * Creates a new instance with the specified POJO.
     */
    public ObjectMBean(T source) {
        if (source == null) {
            throw new IllegalArgumentException("source");
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

    public final Object getAttribute(String fqan) throws AttributeNotFoundException,
            MBeanException, ReflectionException {
        try {
            return convertValue(source.getClass(), fqan, getAttribute0(fqan), false);
        } catch (AttributeNotFoundException e) {
            // Do nothing
        } catch (Throwable e) {
            throwMBeanException(e);
        }

        // Check if the attribute exist, if not throw an exception
        PropertyDescriptor pdesc = propertyDescriptors.get(fqan);
        if (pdesc == null) {
            throwMBeanException(new IllegalArgumentException(
                    "Unknown attribute: " + fqan));
        }

        try {

            Object parent = getParent(fqan);
            boolean writable = isWritable(source.getClass(), pdesc);

            return convertValue(
                    parent.getClass(), getLeafAttributeName(fqan),
                    getAttribute(source, fqan, pdesc.getPropertyType()),
                    writable);
        } catch (Throwable e) {
            throwMBeanException(e);
        }

        throw new IllegalStateException();
    }

    public final void setAttribute(Attribute attribute)
            throws AttributeNotFoundException, MBeanException,
            ReflectionException {
        String aname = attribute.getName();
        Object avalue = attribute.getValue();

        try {
            setAttribute0(aname, avalue);
        } catch (AttributeNotFoundException e) {
            // Do nothing
        } catch (Throwable e) {
            throwMBeanException(e);
        }

        PropertyDescriptor pdesc = propertyDescriptors.get(aname);
        if (pdesc == null) {
            throwMBeanException(new IllegalArgumentException(
                    "Unknown attribute: " + aname));
        }

        try {
            PropertyEditor e = getPropertyEditor(
                    getParent(aname).getClass(),
                    pdesc.getName(), pdesc.getPropertyType());
            e.setAsText((String) avalue);
            OgnlContext ctx = (OgnlContext) Ognl.createDefaultContext(source);
            ctx.setTypeConverter(typeConverter);
            Ognl.setValue(aname, ctx, source, e.getValue());
        } catch (Throwable e) {
            throwMBeanException(e);
        }
    }

    public final Object invoke(String name, Object params[], String signature[])
            throws MBeanException, ReflectionException {

        // Handle synthetic operations first.
        if (name.equals("unregisterMBean")) {
            try {
                server.unregisterMBean(this.name);
                return null;
            } catch (InstanceNotFoundException e) {
                throwMBeanException(e);
            }
        }

        try {
            return convertValue(
                    null, null, invoke0(name, params, signature), false);
        } catch (NoSuchMethodException e) {
            // Do nothing
        } catch (Throwable e) {
            throwMBeanException(e);
        }

        // And then try reflection.
        Class<?>[] paramTypes = new Class[signature.length];
        for (int i = 0; i < paramTypes.length; i ++) {
            try {
                paramTypes[i] = getAttributeClass(signature[i]);
            } catch (ClassNotFoundException e) {
                throwMBeanException(e);
            }

            PropertyEditor e = getPropertyEditor(
                    source.getClass(), "p" + i, paramTypes[i]);
            if (e == null) {
                throwMBeanException(new RuntimeException("Conversion failure: " + params[i]));
            }

            e.setValue(params[i]);
            params[i] = e.getAsText();
        }

        try {
            // Find the right method.
            for (Method m: source.getClass().getMethods()) {
                if (!m.getName().equalsIgnoreCase(name)) {
                    continue;
                }
                Class<?>[] methodParamTypes = m.getParameterTypes();
                if (methodParamTypes.length != params.length) {
                    continue;
                }

                Object[] convertedParams = new Object[params.length];
                for (int i = 0; i < params.length; i ++) {
                    if (Iterable.class.isAssignableFrom(methodParamTypes[i])) {
                        // Generics are not supported.
                        convertedParams = null;
                        break;
                    }
                    PropertyEditor e = getPropertyEditor(source.getClass(), "p" + i, methodParamTypes[i]);
                    if (e == null) {
                        convertedParams = null;
                        break;
                    }

                    e.setAsText((String) params[i]);
                    convertedParams[i] = e.getValue();
                }
                if (convertedParams == null) {
                    continue;
                }

                return convertValue(
                        m.getReturnType(), "returnValue",
                        m.invoke(source, convertedParams), false);
            }

            // No methods matched.
            throw new IllegalArgumentException("Failed to find a matching operation: " + name);
        } catch (Throwable e) {
            throwMBeanException(e);
        }

        throw new IllegalStateException();
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
                // Ignore all exceptions
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
        return (source == null ? "" : source.toString());
    }

    public void addAttributeChangeNotificationListener(
            NotificationListener listener, String name, Object handback) {
        // Do nothing
    }

    public void removeAttributeChangeNotificationListener(
            NotificationListener listener, String name)
            throws ListenerNotFoundException {
        // Do nothing
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
        // Do nothing
    }

    public MBeanNotificationInfo[] getNotificationInfo() {
        return new MBeanNotificationInfo[0];
    }

    public void removeNotificationListener(NotificationListener listener)
            throws ListenerNotFoundException {
        // Do nothing
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
        // Do nothing
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

        PropertyDescriptor[] pdescs;
        try {
            pdescs = Introspector.getBeanInfo(type).getPropertyDescriptors();
        } catch (IntrospectionException e) {
            return;
        }

        for (PropertyDescriptor pdesc: pdescs) {
            // Ignore a write-only property.
            if (pdesc.getReadMethod() == null) {
                continue;
            }

            // Ignore unmanageable property.
            String attrName = pdesc.getName();
            Class<?> attrType = pdesc.getPropertyType();
            if (attrName.equals("class")) {
                continue;
            }
            if (!isReadable(type, attrName)) {
                continue;
            }

            // Expand if possible.
            if (isExpandable(type, attrName)) {
                expandAttribute(attributes, object, prefix, pdesc);
                continue;
            }

            // Ordinary property.
            String fqan = prefix + attrName;
            boolean writable = isWritable(type, pdesc);
            attributes.add(new ModelMBeanAttributeInfo(
                    fqan, convertType(
                            object.getClass(), attrName, attrType, writable).getName(),
                    pdesc.getShortDescription(), true, writable, false));

            propertyDescriptors.put(fqan, pdesc);
        }
    }

    private boolean isWritable(Class<?> type, PropertyDescriptor pdesc) {
        if (type == null) {
            throw new IllegalArgumentException("type");
        }
        if (pdesc == null) {
            return false;
        }
        String attrName = pdesc.getName();
        Class<?> attrType = pdesc.getPropertyType();
        boolean writable = ( pdesc.getWriteMethod() != null ) || isWritable(type, attrName);
        if (getPropertyEditor(type, attrName, attrType) == null) {
            writable = false;
        }
        return writable;
    }

    private void expandAttribute(
            List<ModelMBeanAttributeInfo> attributes,
            Object object, String prefix, PropertyDescriptor pdesc) {
        Object property;
        String attrName = pdesc.getName();
        try {
            property = getAttribute(object, attrName, pdesc.getPropertyType());
        } catch (Exception e) {
            LOGGER.debug("Unexpected exception.", e);
            return;
        }

        if (property == null) {
            return;
        }

        addAttributes(
                attributes,
                property, property.getClass(),
                prefix + attrName + '.');
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
            for (Class<?> paramType: m.getParameterTypes()) {
                String paramName = "p" + (i ++);
                if (getPropertyEditor(source.getClass(), paramName, paramType) == null) {
                    continue;
                }
                signature.add(new MBeanParameterInfo(
                        paramName, convertType(
                                null, null, paramType, true).getName(),
                        paramName));
            }

            Class<?> returnType = convertType(null, null, m.getReturnType(), false);
            operations.add(new ModelMBeanOperationInfo(
                    m.getName(), m.getName(),
                    signature.toArray(new MBeanParameterInfo[signature.size()]),
                    returnType.getName(), ModelMBeanOperationInfo.ACTION));
        }
    }

    private Object getParent(String fqan) throws OgnlException {
        Object parent;
        int dotIndex = fqan.lastIndexOf('.');
        if (dotIndex < 0) {
            parent = source;
        } else {
            parent = getAttribute(source, fqan.substring(0, dotIndex), null);
        }
        return parent;
    }

    private String getLeafAttributeName(String fqan) {
        int dotIndex = fqan.lastIndexOf('.');
        if (dotIndex < 0) {
            return fqan;
        }
        return fqan.substring(dotIndex + 1);
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
            // Do nothing
        }

        return Class.forName(signature);
    }

    private Object getAttribute(Object object, String fqan, Class<?> attrType) throws OgnlException {
        Object property;
        OgnlContext ctx = (OgnlContext) Ognl.createDefaultContext(object);
        ctx.setTypeConverter(new OgnlTypeConverter());
        if (attrType == null) {
            property = Ognl.getValue(fqan, ctx, object);
        } else {
            property = Ognl.getValue(fqan, ctx, object, attrType);
        }
        return property;
    }

    private Class<?> convertType(Class<?> type, String attrName, Class<?> attrType, boolean writable) {
        if (( attrName != null ) && (( attrType == Long.class ) || ( attrType == long.class ))) {
            if (attrName.endsWith("Time") &&
                    ( attrName.indexOf("Total") < 0 ) &&
                    ( attrName.indexOf("Min") < 0 ) &&
                    ( attrName.indexOf("Max") < 0 ) &&
                    ( attrName.indexOf("Avg") < 0 ) &&
                    ( attrName.indexOf("Average") < 0 ) &&
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

        if (!writable) {
            if (Collection.class.isAssignableFrom(attrType) ||
                    Map.class.isAssignableFrom(attrType)) {
                if (List.class.isAssignableFrom(attrType)) {
                    return List.class;
                }
                if (Set.class.isAssignableFrom(attrType)) {
                    return Set.class;
                }
                if (Map.class.isAssignableFrom(attrType)) {
                    return Map.class;
                }
                return Collection.class;
            }

            if (attrType.isPrimitive() ||
                    Date.class.isAssignableFrom(attrType) ||
                    Boolean.class.isAssignableFrom(attrType) ||
                    Character.class.isAssignableFrom(attrType) ||
                    Number.class.isAssignableFrom(attrType)) {
                if (( attrName == null ) || !attrName.endsWith("InMillis") ||
                        !propertyDescriptors.containsKey(
                                attrName.substring(0, attrName.length() - 8))) {
                    return attrType;
                }
            }
        }

        return String.class;
    }

    private Object convertValue(Class<?> type, String attrName, Object v, boolean writable) {
        if (v == null) {
            return null;
        }

        if (( attrName != null ) && ( v instanceof Long )) {
            if (attrName.endsWith("Time") &&
                    ( attrName.indexOf("Total") < 0 ) &&
                    ( attrName.indexOf("Min") < 0 ) &&
                    ( attrName.indexOf("Max") < 0 ) &&
                    ( attrName.indexOf("Avg") < 0 ) &&
                    ( attrName.indexOf("Average") < 0 ) &&
                    !propertyDescriptors.containsKey(attrName + "InMillis")) {
                long time = (Long) v;
                if (time <= 0) {
                    return null;
                }

                return new Date((Long) v);
            }
        }

        if (( v instanceof IoSessionDataStructureFactory ) ||
            ( v instanceof IoHandler )) {
            return v.getClass().getName();
        }

        if (v instanceof IoFilterChainBuilder) {
            Map<String, String> filterMapping = new LinkedHashMap<String, String>();
            if (v instanceof DefaultIoFilterChainBuilder) {
                for (IoFilterChain.Entry e: ((DefaultIoFilterChainBuilder) v).getAll()) {
                    filterMapping.put(e.getName(), e.getFilter().getClass().getName());
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

        if (!writable) {
            if (( v instanceof Collection ) || ( v instanceof Map )) {
                if (v instanceof List) {
                    return convertCollection(v, new ArrayList<Object>());
                }
                if (v instanceof Set) {
                    return convertCollection(v, new LinkedHashSet<Object>());
                }
                if (v instanceof Map) {
                    return convertCollection(v, new LinkedHashMap<Object, Object>());
                }
                return convertCollection(v, new ArrayList<Object>());
            }

            if (( v instanceof Date ) ||
                    ( v instanceof Boolean ) ||
                    ( v instanceof Character ) ||
                    ( v instanceof Number )) {
                if (( attrName == null ) || !attrName.endsWith("InMillis") ||
                        !propertyDescriptors.containsKey(
                                attrName.substring(0, attrName.length() - 8))) {
                    return v;
                }
            }
        }

        PropertyEditor editor = getPropertyEditor(type, attrName, v.getClass());
        if (editor != null) {
            editor.setValue(v);
            return editor.getAsText();
        }

        return v.toString();
    }

    private Object convertCollection(Object src, Collection<Object> dst) {
        Collection<?> srcCol = (Collection<?>) src;
        for (Object e: srcCol) {
            Object convertedValue = convertValue(dst.getClass(), "element", e, false);
            if (( e != null ) && ( convertedValue == null )) {
                convertedValue = (e == null ? "" : e.toString() );
            }
            dst.add(convertedValue);
        }
        return dst;
    }

    private Object convertCollection(Object src, Map<Object, Object> dst) {
        Map<?, ?> srcCol = (Map<?, ?>) src;
        for (Map.Entry<?, ?> e: srcCol.entrySet()) {
            Object convertedKey = convertValue(dst.getClass(), "key", e.getKey(), false);
            Object convertedValue = convertValue(dst.getClass(), "value", e.getValue(), false);
            if (( e.getKey() != null ) && ( convertedKey == null )) {
                convertedKey = e.getKey().toString();
            }
            if (( e.getValue() != null ) && ( convertedValue == null )) {
                convertedKey = e.getValue().toString();
            }
            dst.put(convertedKey, convertedValue);
        }
        return dst;
    }

    private void throwMBeanException(Throwable e) throws MBeanException {
        if (e instanceof OgnlException) {
            OgnlException ognle = (OgnlException) e;
            if (ognle.getReason() != null) {
                throwMBeanException(ognle.getReason());
            } else {
                String message = ognle.getMessage();
                if (e instanceof NoSuchPropertyException) {
                    message = "No such property: " + message;
                } else if (e instanceof ExpressionSyntaxException) {
                    message = "Illegal expression syntax: " + message;
                } else if (e instanceof InappropriateExpressionException) {
                    message = "Inappropriate expression: " + message;
                }
                e = new IllegalArgumentException(ognle.getMessage());
                e.setStackTrace(ognle.getStackTrace());
            }
        }
        if (e instanceof InvocationTargetException) {
            throwMBeanException(e.getCause());
        }

        LOGGER.warn("Unexpected exception.", e);
        if (e.getClass().getPackage().getName().matches("javax?\\..+")) {
            if (e instanceof Exception) {
                throw new MBeanException((Exception) e, e.getMessage());
            }

            throw new MBeanException(
                        new RuntimeException(e), e.getMessage());
        }

        throw new MBeanException(new RuntimeException(
                e.getClass().getName() + ": " + e.getMessage()),
                e.getMessage());
    }

    protected Object getAttribute0(String fqan) throws Exception {
        throw new AttributeNotFoundException(fqan);
    }

    protected void setAttribute0(String attrName, Object attrValue) throws Exception {
        throw new AttributeNotFoundException(attrName);
    }

    protected Object invoke0(String name, Object params[], String signature[]) throws Exception {
        throw new NoSuchMethodException();
    }

    protected boolean isReadable(Class<?> type, String attrName) {
        if (IoService.class.isAssignableFrom(type) && attrName.equals("filterChain")) {
            return false;
        }
        if (IoService.class.isAssignableFrom(type) && attrName.equals("localAddress")) {
            return false;
        }
        if (IoService.class.isAssignableFrom(type) && attrName.equals("defaultLocalAddress")) {
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

    protected Class<?> getElementType(Class<?> type, String attrName) {
        if (( transportMetadata != null ) &&
                IoAcceptor.class.isAssignableFrom(type) &&
                "defaultLocalAddresses".equals(attrName)) {
            return transportMetadata.getAddressType();
        }
        return String.class;
    }

    protected Class<?> getMapKeyType(Class<?> type, String attrName) {
        return String.class;
    }

    protected Class<?> getMapValueType(Class<?> type, String attrName) {
        return String.class;
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

    protected boolean isOperation(String methodName, Class<?>[] paramTypes) {
        return true;
    }

    protected void addExtraAttributes(List<ModelMBeanAttributeInfo> attributes) {
        // Do nothing
    }

    protected void addExtraOperations(List<ModelMBeanOperationInfo> operations) {
        // Do nothing
    }

    protected PropertyEditor getPropertyEditor(Class<?> type, String attrName, Class<?> attrType) {
        if (type == null) {
            throw new IllegalArgumentException("type");
        }

        if (attrName == null) {
            throw new IllegalArgumentException("attrName");
        }

        if (( transportMetadata != null ) && ( attrType == SocketAddress.class )) {
            attrType = transportMetadata.getAddressType();
        }

        if ((( attrType == Long.class ) || ( attrType == long.class ))) {
            if (attrName.endsWith("Time") &&
                    ( attrName.indexOf("Total") < 0 ) &&
                    ( attrName.indexOf("Min") < 0 ) &&
                    ( attrName.indexOf("Max") < 0 ) &&
                    ( attrName.indexOf("Avg") < 0 ) &&
                    ( attrName.indexOf("Average") < 0 ) &&
                    !propertyDescriptors.containsKey(attrName + "InMillis")) {
                return PropertyEditorFactory.getInstance(Date.class);
            }

            if (attrName.equals("id")) {
                return PropertyEditorFactory.getInstance(String.class);
            }
        }

        if (List.class.isAssignableFrom(attrType)) {
            return new ListEditor(getElementType(type, attrName));
        }

        if (Set.class.isAssignableFrom(attrType)) {
            return new SetEditor(getElementType(type, attrName));
        }

        if (Collection.class.isAssignableFrom(attrType)) {
            return new CollectionEditor(getElementType(type, attrName));
        }

        if (Map.class.isAssignableFrom(attrType)) {
            return new MapEditor(
                    getMapKeyType(type, attrName),
                    getMapValueType(type, attrName));
        }

        return PropertyEditorFactory.getInstance(attrType);
    }

    private class OgnlTypeConverter extends PropertyTypeConverter {
        @Override
        protected PropertyEditor getPropertyEditor(
                Class<?> type, String attrName, Class<?> attrType) {
            return ObjectMBean.this.getPropertyEditor(type, attrName, attrType);
        }
    }
}
