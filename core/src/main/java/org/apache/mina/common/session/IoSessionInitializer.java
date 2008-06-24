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

import org.apache.mina.common.future.IoFuture;

/**
 * Defines a callback for obtaining the {@link IoSession} during
 * session initialization.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev: 600461 $, $Date: 2007-12-03 02:55:52 -0700 (Mon, 03 Dec 2007) $
 */
public interface IoSessionInitializer<T extends IoFuture> {
    void initializeSession(IoSession session, T future);
}
