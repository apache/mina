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

import org.apache.mina.common.IoSession;

/**
 * Finds {@link IoSession}s that match a boolean OGNL expression.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class IoSessionFinder {
    private final Object expression;
    
    public IoSessionFinder(String query) {
        if (query == null) {
            throw new NullPointerException("query");
        }
        
        query = query.trim();
        if (query.length() == 0) {
            throw new IllegalArgumentException("query is empty.");
        }
        
        try {
            expression = Ognl.parseExpression(query);
        } catch (OgnlException e) {
            throw new IllegalArgumentException("query: " + query);
        }
    }
    
    public Set<IoSession> find(Iterable<IoSession> sessions) throws OgnlException {
        if (sessions == null) {
            throw new NullPointerException("sessions");
        }
        
        Set<IoSession> answer = new LinkedHashSet<IoSession>();
        for (IoSession s: sessions) {
            OgnlContext context = (OgnlContext) Ognl.createDefaultContext(s);
            Boolean found = (Boolean) Ognl.getValue(
                    expression, context, s, Boolean.class);
            if (found != null && found) {
                answer.add(s);
            }
        }
        
        return answer;
    }
}
