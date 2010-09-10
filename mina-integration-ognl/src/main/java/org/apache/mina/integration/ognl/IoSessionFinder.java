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

import java.util.LinkedHashSet;
import java.util.Set;

import ognl.Ognl;
import ognl.OgnlContext;
import ognl.OgnlException;
import ognl.TypeConverter;

import org.apache.mina.core.session.IoSession;

/**
 * Finds {@link IoSession}s that match a boolean OGNL expression.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class IoSessionFinder {
    
    private final String query;
    private final TypeConverter typeConverter = new PropertyTypeConverter();
    private final Object expression;
    
    /**
     * Creates a new instance with the specified OGNL expression that returns
     * a boolean value (e.g. <tt>"id == 0x12345678"</tt>).
     */
    public IoSessionFinder(String query) {
        if (query == null) {
            throw new IllegalArgumentException("query");
        }
        
        query = query.trim();
        if (query.length() == 0) {
            throw new IllegalArgumentException("query is empty.");
        }
        
        this.query = query;
        try {
            expression = Ognl.parseExpression(query);
        } catch (OgnlException e) {
            throw new IllegalArgumentException("query: " + query);
        }
    }
    
    /**
     * Finds a {@link Set} of {@link IoSession}s that matches the query
     * from the specified sessions and returns the matches.
     * @throws OgnlException if failed to evaluate the OGNL expression
     */
    public Set<IoSession> find(Iterable<IoSession> sessions) throws OgnlException {
        if (sessions == null) {
            throw new IllegalArgumentException("sessions");
        }
        
        Set<IoSession> answer = new LinkedHashSet<IoSession>();
        for (IoSession s: sessions) {
            OgnlContext context = (OgnlContext) Ognl.createDefaultContext(s);
            context.setTypeConverter(typeConverter);
            context.put(AbstractPropertyAccessor.READ_ONLY_MODE, true);
            context.put(AbstractPropertyAccessor.QUERY, query);
            Object result = Ognl.getValue(expression, context, s);
            if (result instanceof Boolean) {
                if (((Boolean) result).booleanValue()) {
                    answer.add(s);
                }
            } else {
                throw new OgnlException(
                        "Query didn't return a boolean value: " + query);
            }
        }
        
        return answer;
    }
}
