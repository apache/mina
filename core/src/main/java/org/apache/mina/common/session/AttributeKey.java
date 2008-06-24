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
package org.apache.mina.common.session;

import java.io.Serializable;
import java.util.Map;

/**
 * A key that makes its parent {@link Map} or session attribute to search
 * fast while being debug-friendly by providing the string representation.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public final class AttributeKey implements Serializable {
    /** The serial version UID */
    private static final long serialVersionUID = -583377473376683096L;
    
    /** The attribute's name */
    private final String name;

    /**
     * Creates a new instance. It's built from :
     * - the class' name
     * - the attribute's name
     * - this attribute hashCode
     */
    public AttributeKey(Class<?> source, String name) {
        this.name = source.getName() + '.' + name + '@' + Integer.toHexString(this.hashCode());
    }

    /**
     * The String representation of tis objection is its constructed name.
     */
    @Override
    public String toString() {
        return name;
    }
}
