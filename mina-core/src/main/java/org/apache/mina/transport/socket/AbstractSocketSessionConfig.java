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
package org.apache.mina.transport.socket;

import org.apache.mina.core.session.AbstractIoSessionConfig;
import org.apache.mina.core.session.IoSessionConfig;

/**
 * The TCP transport session configuration.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class AbstractSocketSessionConfig extends AbstractIoSessionConfig implements SocketSessionConfig {
    /**
     * {@inheritDoc}
     */
    @Override
    public void setAll(IoSessionConfig config) {
        super.setAll(config);
        
        if (!(config instanceof SocketSessionConfig)) {
            return;
        }

        if (config instanceof AbstractSocketSessionConfig) {
            // Minimize unnecessary system calls by checking all 'propertyChanged' properties.
            AbstractSocketSessionConfig cfg = (AbstractSocketSessionConfig) config;
            if (cfg.isKeepAliveChanged()) {
                setKeepAlive(cfg.isKeepAlive());
            }
            if (cfg.isOobInlineChanged()) {
                setOobInline(cfg.isOobInline());
            }
            if (cfg.isReceiveBufferSizeChanged()) {
                setReceiveBufferSize(cfg.getReceiveBufferSize());
            }
            if (cfg.isReuseAddressChanged()) {
                setReuseAddress(cfg.isReuseAddress());
            }
            if (cfg.isSendBufferSizeChanged()) {
                setSendBufferSize(cfg.getSendBufferSize());
            }
            if (cfg.isSoLingerChanged()) {
                setSoLinger(cfg.getSoLinger());
            }
            if (cfg.isTcpNoDelayChanged()) {
                setTcpNoDelay(cfg.isTcpNoDelay());
            }
            if (cfg.isTrafficClassChanged() && getTrafficClass() != cfg.getTrafficClass()) {
                setTrafficClass(cfg.getTrafficClass());
            }
        } else {
            SocketSessionConfig cfg = (SocketSessionConfig) config;
            setKeepAlive(cfg.isKeepAlive());
            setOobInline(cfg.isOobInline());
            setReceiveBufferSize(cfg.getReceiveBufferSize());
            setReuseAddress(cfg.isReuseAddress());
            setSendBufferSize(cfg.getSendBufferSize());
            setSoLinger(cfg.getSoLinger());
            setTcpNoDelay(cfg.isTcpNoDelay());
            if (getTrafficClass() != cfg.getTrafficClass()) {
                setTrafficClass(cfg.getTrafficClass());
            }
        }
    }

    /**
     * @return <tt>true</tt> if and only if the <tt>keepAlive</tt> property
     * has been changed by its setter method.  The system call related with
     * the property is made only when this method returns <tt>true</tt>.  By
     * default, this method always returns <tt>true</tt> to simplify implementation
     * of subclasses, but overriding the default behavior is always encouraged.
     */
    protected boolean isKeepAliveChanged() {
        return true;
    }

    /**
     * @return <tt>true</tt> if and only if the <tt>oobInline</tt> property
     * has been changed by its setter method.  The system call related with
     * the property is made only when this method returns <tt>true</tt>.  By
     * default, this method always returns <tt>true</tt> to simplify implementation
     * of subclasses, but overriding the default behavior is always encouraged.
     */
    protected boolean isOobInlineChanged() {
        return true;
    }

    /**
     * @return <tt>true</tt> if and only if the <tt>receiveBufferSize</tt> property
     * has been changed by its setter method.  The system call related with
     * the property is made only when this method returns <tt>true</tt>.  By
     * default, this method always returns <tt>true</tt> to simplify implementation
     * of subclasses, but overriding the default behavior is always encouraged.
     */
    protected boolean isReceiveBufferSizeChanged() {
        return true;
    }

    /**
     * @return <tt>true</tt> if and only if the <tt>reuseAddress</tt> property
     * has been changed by its setter method.  The system call related with
     * the property is made only when this method returns <tt>true</tt>.  By
     * default, this method always returns <tt>true</tt> to simplify implementation
     * of subclasses, but overriding the default behavior is always encouraged.
     */
    protected boolean isReuseAddressChanged() {
        return true;
    }

    /**
     * @return <tt>true</tt> if and only if the <tt>sendBufferSize</tt> property
     * has been changed by its setter method.  The system call related with
     * the property is made only when this method returns <tt>true</tt>.  By
     * default, this method always returns <tt>true</tt> to simplify implementation
     * of subclasses, but overriding the default behavior is always encouraged.
     */
    protected boolean isSendBufferSizeChanged() {
        return true;
    }

    /**
     * @return <tt>true</tt> if and only if the <tt>soLinger</tt> property
     * has been changed by its setter method.  The system call related with
     * the property is made only when this method returns <tt>true</tt>.  By
     * default, this method always returns <tt>true</tt> to simplify implementation
     * of subclasses, but overriding the default behavior is always encouraged.
     */
    protected boolean isSoLingerChanged() {
        return true;
    }

    /**
     * @return <tt>true</tt> if and only if the <tt>tcpNoDelay</tt> property
     * has been changed by its setter method.  The system call related with
     * the property is made only when this method returns <tt>true</tt>.  By
     * default, this method always returns <tt>true</tt> to simplify implementation
     * of subclasses, but overriding the default behavior is always encouraged.
     */
    protected boolean isTcpNoDelayChanged() {
        return true;
    }

    /**
     * @return <tt>true</tt> if and only if the <tt>trafficClass</tt> property
     * has been changed by its setter method.  The system call related with
     * the property is made only when this method returns <tt>true</tt>.  By
     * default, this method always returns <tt>true</tt> to simplify implementation
     * of subclasses, but overriding the default behavior is always encouraged.
     */
    protected boolean isTrafficClassChanged() {
        return true;
    }
}