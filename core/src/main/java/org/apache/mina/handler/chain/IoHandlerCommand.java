/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
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
 * session attributes.  To improve interoperability of {@link IoHandlerCommand}
 * implementations, a useful design pattern is to expose the key values
 * used as JavaBeans properties of the {@link IoHandlerCommand} implementation class
 * itself.  For example, a {@link IoHandlerCommand} that requires an input and an
 * output key might implement the following properties:</p>
 *
 * <pre>
 *   private String inputKey = "input";
 *   public String getInputKey() {
 *     return (this.inputKey);
 *   }
 *   public void setInputKey(String inputKey) {
 *     this.inputKey = inputKey;
 *   }
 *
 *   private String outputKey = "output";
 *   public String getOutputKey() {
 *     return (this.outputKey);
 *   }
 *   public void setOutputKey(String outputKey) {
 *     this.outputKey = outputKey;
 *   }
 * </pre>
 *
 * <p>And the operation of accessing the "input" information in the context
 * would be executed by calling:</p>
 *
 * <pre>
 *   String input = (String) session.getAttribute(getInputKey());
 * </pre>
 *
 * <p>instead of hard coding the attribute name.  The use of the "Key"
 * suffix on such property names is a useful convention to identify properties
 * being used in this fashion, as opposed to JavaBeans properties that simply
 * configure the internal operation of this {@link IoHandlerCommand}.</p>
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface IoHandlerCommand
{
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
    void execute( NextCommand next, IoSession session, Object message ) throws Exception;
    
    /**
     * Represents an indirect reference to the next {@link IoHandlerCommand} of
     * the {@link IoHandlerChain}.  This interface provides a way to forward
     * the request to the next {@link IoHandlerCommand}.
     *
     * @author The Apache Directory Project (mina-dev@directory.apache.org)
     * @version $Rev$, $Date$
     */
    public interface NextCommand
    {
        /**
         * Forwards the request to the next {@link IoHandlerCommand} in the
         * {@link IoHandlerChain}.
         */
        void execute( IoSession session, Object message ) throws Exception;
    }
}
