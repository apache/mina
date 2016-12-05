/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.mina.integration.beans;

import java.beans.PropertyEditor;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * A factory that creates a new {@link PropertyEditor} which is appropriate for
 * the specified object or class. 
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public final class PropertyEditorFactory {
    private PropertyEditorFactory() {
    }

    /**
     * Creates a new instance of editor, depending on the given object's type
     * 
     * @param object The object we need an editor to be created for
     * @return The created editor
     */
    @SuppressWarnings("unchecked")
    public static PropertyEditor getInstance(Object object) {
        if (object == null) {
            return new NullEditor();
        }

        if (object instanceof Collection<?>) {
            Class<?> elementType = null;
            
            for (Object e : (Collection<Object>) object) {
                if (e != null) {
                    elementType = e.getClass();
                    
                    break;
                }
            }

            if (elementType != null) {
                if (object instanceof Set) {
                    return new SetEditor(elementType);
                }

                if (object instanceof List) {
                    return new ListEditor(elementType);
                }

                return new CollectionEditor(elementType);
            }
        }

        if (object instanceof Map) {
            Class<?> keyType = null;
            Class<?> valueType = null;
            
            for (Object entry : ((Map<?,?>) object).entrySet()) {
                Map.Entry<?,?> e = (Map.Entry<?,?>) entry;
                
                if ((e.getKey() != null) && (e.getValue() != null)) {
                    keyType = e.getKey().getClass();
                    valueType = e.getValue().getClass();
                    
                    break;
                }
            }

            if ((keyType != null) && (valueType != null)) {
                return new MapEditor(keyType, valueType);
            }
        }

        return getInstance(object.getClass());
    }

    /**
     * Creates a new instance of editor, depending on the given type
     * 
     * @param type The type of editor to create
     * @return The created editor
     */
    public static PropertyEditor getInstance(Class<?> type) {
        if (type == null) {
            throw new IllegalArgumentException("type");
        }

        if (type.isEnum()) {
            return new EnumEditor(type);
        }

        if (type.isArray()) {
            return new ArrayEditor(type.getComponentType());
        }

        if (Collection.class.isAssignableFrom(type)) {
            if (Set.class.isAssignableFrom(type)) {
                return new SetEditor(String.class);
            }

            if (List.class.isAssignableFrom(type)) {
                return new ListEditor(String.class);
            }

            return new CollectionEditor(String.class);
        }

        if (Map.class.isAssignableFrom(type)) {
            return new MapEditor(String.class, String.class);
        }

        if (Properties.class.isAssignableFrom(type)) {
            return new PropertiesEditor();
        }

        try {
            return (PropertyEditor) PropertyEditorFactory.class
                    .getClassLoader()
                    .loadClass(
                            PropertyEditorFactory.class.getPackage().getName() + '.' + 
                            filterPrimitiveType(type).getSimpleName() + "Editor")
                    .newInstance();
        } catch (Exception e) {
            return null;
        }
    }

    private static Class<?> filterPrimitiveType(Class<?> type) {
        if (type.isPrimitive()) {
            if (type == boolean.class) {
                return Boolean.class;
            }
            
            if (type == byte.class) {
                return Byte.class;
            }
            
            if (type == char.class) {
                return Character.class;
            }
            
            if (type == double.class) {
                return Double.class;
            }
            
            if (type == float.class) {
                return Float.class;
            }
            
            if (type == int.class) {
                return Integer.class;
            }
            
            if (type == long.class) {
                return Long.class;
            }
            
            if (type == short.class) {
                return Short.class;
            }
        }
        
        return type;
    }
}
