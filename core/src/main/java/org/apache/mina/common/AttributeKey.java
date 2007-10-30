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
package org.apache.mina.common;

import java.io.Serializable;
import java.util.Map;

/**
 * A key that makes its parent {@link Map} or session attribute to search
 * fast while being debug-friendly by providing the spring representation.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public final class AttributeKey implements Serializable {
    private static final long serialVersionUID = -583377473376683096L;
    
    private final String name;

    /**
     * Creates a new instance.
     */
    public AttributeKey(Class<?> source, String name) {
        this.name = source.getName() + '.' + String.valueOf(name) + '@' +
                Integer.toHexString(System.identityHashCode(this));
    }

    @Override
    public String toString() {
        return name;
    }
}
