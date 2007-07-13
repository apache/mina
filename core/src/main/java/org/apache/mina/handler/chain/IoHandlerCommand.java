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
package org.apache.mina.handler.chain;

import org.apache.mina.common.IoSession;

/**
 * <p>A {@link IoHandlerCommand} encapsulates a unit of processing work to be
 * performed, whose purpose is to examine and/or modify the state of a
 * transaction that is represented by custom attributes provided by 
 * {@link IoSession}.  Individual {@link IoHandlerCommand}s can be assembled into
 * a {@link IoHandlerChain}, which allows them to either complete the
 * required processing or delegate further processing to the next
 * {@link IoHandlerCommand} in the {@link IoHandlerChain}.</p>
 *
 * <p>{@link IoHandlerCommand} implementations typically retrieve and store state
 * information in the {@link IoSession} that is passed as a parameter to
 * the {@link #execute(NextCommand,IoSession,Object)} method, using custom
 * session attributes.  If you think getting attributes is tedious process,
 * you can create a bean which contains getters and setters of all properties
 * and store the bean as a session attribute:</p>
 *
 * <pre>
 * public class MyContext {
 *   public String getPropertyX() { ... };
 *   public void setPropertyX(String propertyX) { ... };
 *   public int getPropertyZ() { ... };
 *   public void setPropertyZ(int propertyZ) { ... };
 * }
 * 
 * public class MyHandlderCommand implements IoHandlerCommand {
 *   public void execute( NextCommand next, IoSession session, Object message ) throws Exception {
 *     MyContext ctx = session.getAttribute( "mycontext" );
 *     ...
 *   }
 * }
 * </pre>
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface IoHandlerCommand {
    /**
     * <p>Execute a unit of processing work to be performed.  This
     * {@link IoHandlerCommand} may either complete the required processing
     * and just return to stop the processing, or delegate remaining
     * processing to the next {@link IoHandlerCommand} in a {@link IoHandlerChain}
     * containing this {@link IoHandlerCommand} by calling
     * {@link NextCommand#execute(IoSession,Object)}.
     *
     * @param next an indirect reference to the next {@link IoHandlerCommand} that
     *             provides a way to forward the request to the next {@link IoHandlerCommand}.
     * @param session the {@link IoSession} which is associated with 
     *                this request
     * @param message the message object of this request
     *
     * @exception Exception general purpose exception return
     *                      to indicate abnormal termination
     */
    void execute(NextCommand next, IoSession session, Object message)
            throws Exception;

    /**
     * Represents an indirect reference to the next {@link IoHandlerCommand} of
     * the {@link IoHandlerChain}.  This interface provides a way to forward
     * the request to the next {@link IoHandlerCommand}.
     *
     * @author The Apache Directory Project (mina-dev@directory.apache.org)
     * @version $Rev$, $Date$
     */
    public interface NextCommand {
        /**
         * Forwards the request to the next {@link IoHandlerCommand} in the
         * {@link IoHandlerChain}.
         */
        void execute(IoSession session, Object message) throws Exception;
    }
}
