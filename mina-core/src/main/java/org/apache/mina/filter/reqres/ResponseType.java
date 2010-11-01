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
package org.apache.mina.filter.reqres;

/**
 * Type of Response contained within the {@code Response} class
 *
 * Response can be either a single entity or a multiple partial messages, in which
 * case PARTIAL_LAST signifies the end of partial messages
 *
 * For response contained within a single message/entity the ResponseType shall be
 * WHOLE
 *
 * For response with multiple partial messages, we have respnse type sepcified as
 *
 * [PARTIAL]+ PARTIAL_LAST
 *
 * meaning, we have One or more PARTIAL response type with one PARTIAL_LAST which
 * signifies end of partial messages or completion of response message
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public enum ResponseType {
    WHOLE, PARTIAL, PARTIAL_LAST;
}
