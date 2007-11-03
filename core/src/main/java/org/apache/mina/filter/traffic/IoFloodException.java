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
package org.apache.mina.filter.traffic;

/**
 * A tagging interface for identifying {@link ReadFloodException}
 * and {@link WriteFloodException}; It's not really an exception.
 * <p>
 * This interface is useful when you want to do something if either
 * reader or writer side is flooded:
 * <pre><code>
 * if (exception instanceof IoFloodException) {
 *     session.close();
 * }
 * </code></pre>
 * which is a way simpler than:
 * <pre><code>
 * if (exception instanceof ReadFloodException ||
 *     exception instanceof WriteFloodException) {
 *     
 *     session.close();
 * }
 * </code></pre>
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public interface IoFloodException {
}
