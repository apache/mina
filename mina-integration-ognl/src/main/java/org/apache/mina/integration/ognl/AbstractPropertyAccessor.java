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

import ognl.ObjectPropertyAccessor;
import ognl.OgnlContext;
import ognl.OgnlException;
import ognl.OgnlRuntime;
import ognl.PropertyAccessor;

/**
 * An abstract OGNL {@link PropertyAccessor} for MINA constructs.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
@SuppressWarnings("unchecked")
public abstract class AbstractPropertyAccessor extends ObjectPropertyAccessor {

    static final Object READ_ONLY_MODE = new Object();
    static final Object QUERY = new Object();
    
    @Override
    public final boolean hasGetProperty(OgnlContext context, Object target,
            Object oname) throws OgnlException {
        if (oname == null) {
            return false;
        }
        
        if (hasGetProperty0(context, target, oname.toString())) {
            return true;
        } else {
            return super.hasGetProperty(context, target, oname);
        }
    }

    @Override
    public final boolean hasSetProperty(OgnlContext context, Object target,
            Object oname) throws OgnlException {
        if (context.containsKey(READ_ONLY_MODE)) {
            // Return true to trigger setPossibleProperty to throw an exception.
            return true;
        }
        
        if (oname == null) {
            return false;
        }
        
        if (hasSetProperty0(context, target, oname.toString())) {
            return true;
        } else {
            return super.hasSetProperty(context, target, oname);
        }
    }

    @Override
    public final Object getPossibleProperty(Map context, Object target, String name)
            throws OgnlException {
        Object answer = getProperty0((OgnlContext) context, target, name);
        if (answer == OgnlRuntime.NotFound) {
            answer = super.getPossibleProperty(context, target, name);
        }
        return answer;
    }

    @Override
    public final Object setPossibleProperty(Map context, Object target, String name,
            Object value) throws OgnlException {
        if (context.containsKey(READ_ONLY_MODE)) {
            throw new OgnlException("Expression must be read-only: " + context.get(QUERY));
        }
        
        Object answer = setProperty0((OgnlContext) context, target, name, value);
        if (answer == OgnlRuntime.NotFound) {
            answer = super.setPossibleProperty(context, target, name, value);
        }
        return answer;
    }
    
    protected abstract boolean hasGetProperty0(
            OgnlContext context, Object target, String name) throws OgnlException;

    protected abstract boolean hasSetProperty0(
            OgnlContext context, Object target, String name) throws OgnlException;

    protected abstract Object getProperty0(
            OgnlContext context, Object target, String name) throws OgnlException;

    protected abstract Object setProperty0(
            OgnlContext context, Object target, String name, Object value) throws OgnlException;


    // The following methods uses the four method above, so there's no need
    // to override them.
    
    @Override
    public final Object getProperty(Map context, Object target, Object oname)
            throws OgnlException {
        return super.getProperty(context, target, oname);
    }

    @Override
    public final boolean hasGetProperty(Map context, Object target, Object oname)
            throws OgnlException {
        return super.hasGetProperty(context, target, oname);
    }

    @Override
    public final boolean hasSetProperty(Map context, Object target, Object oname)
            throws OgnlException {
        return super.hasSetProperty(context, target, oname);
    }

    @Override
    public final void setProperty(Map context, Object target, Object oname,
            Object value) throws OgnlException {
        super.setProperty(context, target, oname, value);
    }
}
