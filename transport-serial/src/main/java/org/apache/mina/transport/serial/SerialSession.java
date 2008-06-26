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
package org.apache.mina.transport.serial;

import org.apache.mina.core.session.IoSession;

/**
 * An {@link IoSession} for serial communication transport.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev: 529590 $, $Date: 2007-04-17 15:14:17 +0200 (mar., 17 avr. 2007) $
 */
public interface SerialSession extends IoSession {
    SerialSessionConfig getConfig();
    SerialAddress getRemoteAddress();
    SerialAddress getLocalAddress();
    SerialAddress getServiceAddress();}
