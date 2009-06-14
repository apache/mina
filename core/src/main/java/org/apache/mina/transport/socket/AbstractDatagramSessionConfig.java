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
 * TODO Add documentation
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class AbstractDatagramSessionConfig extends
        AbstractIoSessionConfig implements DatagramSessionConfig {

    private static final boolean DEFAULT_CLOSE_ON_PORT_UNREACHABLE = true;

    private boolean closeOnPortUnreachable = DEFAULT_CLOSE_ON_PORT_UNREACHABLE;
    
    protected AbstractDatagramSessionConfig() {
        // Do nothing
    }

    @Override
    protected void doSetAll(IoSessionConfig config) {
        if (!(config instanceof DatagramSessionConfig)) {
            return;
        }
        
        if (config instanceof AbstractDatagramSessionConfig) {
            // Minimize unnecessary system calls by checking all 'propertyChanged' properties.
            AbstractDatagramSessionConfig cfg = (AbstractDatagramSessionConfig) config;
            if (cfg.isBroadcastChanged()) {
                setBroadcast(cfg.isBroadcast());
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
            if (cfg.isTrafficClassChanged() && getTrafficClass() != cfg.getTrafficClass()) {
                setTrafficClass(cfg.getTrafficClass());
            }
        } else {
            DatagramSessionConfig cfg = (DatagramSessionConfig) config;
            setBroadcast(cfg.isBroadcast());
            setReceiveBufferSize(cfg.getReceiveBufferSize());
            setReuseAddress(cfg.isReuseAddress());
            setSendBufferSize(cfg.getSendBufferSize());
            if (getTrafficClass() != cfg.getTrafficClass()) {
                setTrafficClass(cfg.getTrafficClass());
            }
        }
    }
    
    /**
     * Returns <tt>true</tt> if and only if the <tt>broadcast</tt> property
     * has been changed by its setter method.  The system call related with
     * the property is made only when this method returns <tt>true</tt>.  By
     * default, this method always returns <tt>true</tt> to simplify implementation
     * of subclasses, but overriding the default behavior is always encouraged.
     */
    protected boolean isBroadcastChanged() {
        return true;
    }

    /**
     * Returns <tt>true</tt> if and only if the <tt>receiveBufferSize</tt> property
     * has been changed by its setter method.  The system call related with
     * the property is made only when this method returns <tt>true</tt>.  By
     * default, this method always returns <tt>true</tt> to simplify implementation
     * of subclasses, but overriding the default behavior is always encouraged.
     */
    protected boolean isReceiveBufferSizeChanged() {
        return true;
    }

    /**
     * Returns <tt>true</tt> if and only if the <tt>reuseAddress</tt> property
     * has been changed by its setter method.  The system call related with
     * the property is made only when this method returns <tt>true</tt>.  By
     * default, this method always returns <tt>true</tt> to simplify implementation
     * of subclasses, but overriding the default behavior is always encouraged.
     */
    protected boolean isReuseAddressChanged() {
        return true;
    }

    /**
     * Returns <tt>true</tt> if and only if the <tt>sendBufferSize</tt> property
     * has been changed by its setter method.  The system call related with
     * the property is made only when this method returns <tt>true</tt>.  By
     * default, this method always returns <tt>true</tt> to simplify implementation
     * of subclasses, but overriding the default behavior is always encouraged.
     */
    protected boolean isSendBufferSizeChanged() {
        return true;
    }

    /**
     * Returns <tt>true</tt> if and only if the <tt>trafficClass</tt> property
     * has been changed by its setter method.  The system call related with
     * the property is made only when this method returns <tt>true</tt>.  By
     * default, this method always returns <tt>true</tt> to simplify implementation
     * of subclasses, but overriding the default behavior is always encouraged.
     */
    protected  boolean isTrafficClassChanged() {
        return true;
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean isCloseOnPortUnreachable() {
        return closeOnPortUnreachable;
    }

    /**
     * {@inheritDoc}
     */
    public void setCloseOnPortUnreachable(boolean closeOnPortUnreachable) {
        this.closeOnPortUnreachable = closeOnPortUnreachable;
    }
}