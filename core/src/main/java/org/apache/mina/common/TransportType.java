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
package org.apache.mina.common;

import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Represents network transport types.
 * MINA provides three transport types by default:
 * <ul>
 *   <li>{@link #SOCKET} - TCP/IP</li>
 *   <li>{@link #DATAGRAM} - UDP/IP</li>
 *   <li>{@link #VM_PIPE} - in-VM pipe support (only available in protocol
 *       layer</li>
 * </ul>
 * <p>
 * You can also create your own transport type.  Please refer to
 * {@link #TransportType(String[], boolean)}.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public final class TransportType implements Serializable {
    private static final long serialVersionUID = 3258132470497883447L;

    private static final Map<String, TransportType> name2type = new HashMap<String, TransportType>();

    private static void register(String[] names, TransportType type) {
        synchronized (name2type) {
            for (int i = names.length - 1; i >= 0; i--) {
                if (name2type.containsKey(names[i])) {
                    throw new IllegalArgumentException("Transport type name '"
                            + names[i] + "' is already taken.");
                }
            }

            for (int i = names.length - 1; i >= 0; i--) {
                name2type.put(names[i].toUpperCase(), type);
            }
        }
    }

    /**
     * Transport type: TCP/IP (Registry name: <tt>"SOCKET"</tt> or <tt>"TCP"</tt>)
     */
    public static final TransportType SOCKET = new TransportType(new String[] {
            "SOCKET", "TCP" }, false);

    /**
     * Transport type: UDP/IP (Registry name: <tt>"DATAGRAM"</tt> or <tt>"UDP"</tt>)
     */
    public static final TransportType DATAGRAM = new TransportType(
            new String[] { "DATAGRAM", "UDP" }, true);

    /**
     * Transport type: in-VM pipe (Registry name: <tt>"VM_PIPE"</tt>)
     * Please refer to
     * <a href="../protocol/vmpipe/package-summary.htm"><tt>org.apache.mina.protocol.vmpipe</tt></a>
     * package.
     */
    public static final TransportType VM_PIPE = new TransportType(
            new String[] { "VM_PIPE" }, Object.class, false);

    /**
     * Returns the transport type of the specified name.
     * All names are case-insensitive.
     *
     * @param name the name of the transport type
     * @return the transport type
     * @throws IllegalArgumentException if the specified name is not available.
     */
    public static TransportType getInstance(String name) {
        TransportType type = name2type.get(name.toUpperCase());
        if (type != null) {
            return type;
        }

        throw new IllegalArgumentException("Unknown transport type name: "
                + name);
    }

    private final String[] names;

    private final transient boolean connectionless;

    private final transient Class<? extends Object> envelopeType;

    /**
     * Creates a new instance.  New transport type is automatically registered
     * to internal registry so that you can look it up using {@link #getInstance(String)}.
     *
     * @param names the name or aliases of this transport type
     * @param connectionless <tt>true</tt> if and only if this transport type is connectionless
     *
     * @throws IllegalArgumentException if <tt>names</tt> are already registered or empty
     */
    public TransportType(String[] names, boolean connectionless) {
        this(names, ByteBuffer.class, connectionless);
    }

    /**
     * Creates a new instance.  New transport type is automatically registered
     * to internal registry so that you can look it up using {@link #getInstance(String)}.
     *
     * @param names the name or aliases of this transport type
     * @param connectionless <tt>true</tt> if and only if this transport type is connectionless
     *
     * @throws IllegalArgumentException if <tt>names</tt> are already registered or empty
     */
    public TransportType(String[] names, Class<? extends Object> envelopeType,
            boolean connectionless) {
        if (names == null) {
            throw new NullPointerException("names");
        }
        if (names.length == 0) {
            throw new IllegalArgumentException("names is empty");
        }
        if (envelopeType == null) {
            throw new NullPointerException("envelopeType");
        }

        for (int i = 0; i < names.length; i++) {
            if (names[i] == null) {
                throw new NullPointerException("strVals[" + i + "]");
            }

            names[i] = names[i].toUpperCase();
        }

        register(names, this);
        this.names = names;
        this.connectionless = connectionless;
        this.envelopeType = envelopeType;
    }

    /**
     * Returns <code>true</code> if the session of this transport type is
     * connectionless.
     */
    public boolean isConnectionless() {
        return connectionless;
    }

    public Class<? extends Object> getEnvelopeType() {
        return envelopeType;
    }

    /**
     * Returns the known names of this transport type.
     */
    public Set<String> getNames() {
        Set<String> result = new TreeSet<String>();
        for (int i = names.length - 1; i >= 0; i--) {
            result.add(names[i]);
        }

        return result;
    }

    @Override
    public String toString() {
        return names[0];
    }

    private Object readResolve() throws ObjectStreamException {
        for (int i = names.length - 1; i >= 0; i--) {
            try {
                return getInstance(names[i]);
            } catch (IllegalArgumentException e) {
                // ignore
            }
        }

        throw new InvalidObjectException("Unknown transport type.");
    }
}
