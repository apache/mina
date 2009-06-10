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
package org.apache.mina.example.sumup.message;

/**
 * <code>RESULT</code> message in SumUp protocol.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ResultMessage extends AbstractMessage {
    private static final long serialVersionUID = 7371210248110219946L;

    private boolean ok;

    private int value;

    public ResultMessage() {
    }

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    @Override
    public String toString() {
        if (ok) {
            return getSequence() + ":RESULT(" + value + ')';
        } else {
            return getSequence() + ":RESULT(ERROR)";
        }
    }
}