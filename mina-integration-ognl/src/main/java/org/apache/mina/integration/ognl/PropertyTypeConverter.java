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

import java.beans.PropertyEditor;
import java.lang.reflect.Member;
import java.util.Map;

import ognl.OgnlContext;
import ognl.TypeConverter;

import org.apache.mina.integration.beans.PropertyEditorFactory;

/**
 * {@link PropertyEditor}-based implementation of OGNL {@link TypeConverter}.
 * This converter uses the {@link PropertyEditor} implementations in
 * <tt>mina-integration-beans</tt> module to perform conversion.  To use this
 * converter:
 * <pre><code>
 * OgnlContext ctx = Ognl.createDefaultContext(root);
 * ctx.put(OgnlContext.TYPE_CONVERTER_CONTEXT_KEY, new PropertyTypeConverter());
 * </code></pre>
 * You can also override {@link #getPropertyEditor(Class, String, Class)}
 * method to have more control over how an appropriate {@link PropertyEditor}
 * is chosen.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class PropertyTypeConverter implements TypeConverter {
    
    @SuppressWarnings("unchecked")
    public Object convertValue(Map ctx, Object target, Member member,
            String attrName, Object value, Class toType) {
        if (value == null) {
            return null;
        }

        if (attrName == null) {
            // I don't know why but OGNL gives null attrName almost always.
            // Fortunately, we can get the actual attrName with a tiny hack.
            OgnlContext ognlCtx = (OgnlContext) ctx;
            attrName = ognlCtx.getCurrentNode().toString().replaceAll(
                    "[\" \']+", "");
        }

        if (toType.isAssignableFrom(value.getClass())) {
            return value;
        }

        PropertyEditor e1 = getPropertyEditor(
                target.getClass(), attrName, value.getClass());
        if (e1 == null) {
            throw new IllegalArgumentException("Can't convert "
                    + value.getClass().getSimpleName() + " to "
                    + String.class.getSimpleName());
        }
        e1.setValue(value);

        PropertyEditor e2 = getPropertyEditor(
                target.getClass(), attrName, toType);
        if (e2 == null) {
            throw new IllegalArgumentException("Can't convert "
                    + String.class.getSimpleName() + " to "
                    + toType.getSimpleName());
        }

        e2.setAsText(e1.getAsText());
        return e2.getValue();
    }
    
    protected PropertyEditor getPropertyEditor(Class<?> type, String attrName, Class<?> attrType) {
        return PropertyEditorFactory.getInstance(attrType);
    }
}