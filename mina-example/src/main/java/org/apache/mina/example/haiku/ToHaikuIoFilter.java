/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.mina.example.haiku;

import java.util.ArrayList;
import java.util.List;

import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IoSession;

/**
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ToHaikuIoFilter extends IoFilterAdapter {

    @SuppressWarnings( { "unchecked" })
    @Override
    public void messageReceived(NextFilter nextFilter, IoSession session,
            Object message) throws Exception {
        List<String> phrases = (List<String>) session.getAttribute("phrases");

        if (null == phrases) {
            phrases = new ArrayList<String>();
            session.setAttribute("phrases", phrases);
        }

        phrases.add((String) message);

        if (phrases.size() == 3) {
            session.removeAttribute("phrases");

            super.messageReceived(nextFilter, session, new Haiku(phrases
                    .toArray(new String[3])));
        }
    }
}
