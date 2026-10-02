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

package org.apache.mina.util;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.mina.core.service.IoAcceptor;

/**
 * An unitily class containing a method to bind an acceptor with
 * a retry mechanism, in case the port has already been taken when the 
 * bind occurs.
 * 
 * May be useful to avoid random test failures...
 * 
 */
public class AcceptorBindUtil {
    /** The default number of tries */
    private static final int NB_TRIES = 10;
    
    /**
     * Try to bind an IoAcceptor, using a random port, but retrying
     * if the port get used between the moment it's picked and the moment 
     * the bind occurs. If it fails, we get a untimeException
     * 
     * @param acceptor The IoAccceptor we try to bind
     * @return The port to which the IoAcceptor is bound to
     */
    public static final int tryBind(IoAcceptor acceptor) {
        int nbTry = NB_TRIES;
        
        while (true) {
            int nextAvailable = AvailablePortFinder.getNextAvailable();
            try {
                acceptor.bind(new InetSocketAddress(nextAvailable));
                
                System.out.println( "-------------> " + nextAvailable );
                
                return nextAvailable;
            } catch ( IOException e ) {
                nbTry--;
                
                if (nbTry == 0) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * Try to bind an IoAcceptor, using a random port, but retrying
     * if the port get used between the moment it's picked and the moment 
     * the bind occurs. If it fails, we get a untimeException
     * 
     * @param acceptor The IoAccceptor we try to bind
     * @param host The host to use
     * @return The port to which the IoAcceptor is bound to
     */
    public static final int tryBind(IoAcceptor acceptor, String host) {
        int nbTry = NB_TRIES;
        
        while (true) {
            int nextAvailable = AvailablePortFinder.getNextAvailable();
            try {
                acceptor.bind(new InetSocketAddress(host, nextAvailable));
                
                return nextAvailable;
            } catch ( IOException e ) {
                nbTry--;
                
                if (nbTry == 0) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * Try to bind an IoAcceptor, using a random port, but retrying
     * if the port get used between the moment it's picked and the moment 
     * the bind occurs. If it fails, we get a untimeException
     * 
     * @param acceptor The IoAccceptor we try to bind
     * @param nbTries The number of times it tries
     * @return The port to which the IoAcceptor is bound to
     */
    public static final int tryBind(IoAcceptor acceptor, int nbTries) {
        while (true) {
            int nextAvailable = AvailablePortFinder.getNextAvailable();
            try {
                acceptor.bind(new InetSocketAddress(nextAvailable));
                
                return nextAvailable;
            } catch ( IOException e ) {
                nbTries--;
                
                if (nbTries == 0) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * Try to bind an IoAcceptor, using a random port, but retrying
     * if the port get used between the moment it's picked and the moment 
     * the bind occurs. If it fails, we get a untimeException
     * 
     * @param acceptor The IoAccceptor we try to bind
     * @param host The host to use
     * @param nbTries The number of times it tries
     * @return The port to which the IoAcceptor is bound to
     */
    public static final int tryBind(IoAcceptor acceptor, String host, int nbTries) {
        while (true) {
            int nextAvailable = AvailablePortFinder.getNextAvailable();
            try {
                acceptor.bind(new InetSocketAddress(host, nextAvailable));
                
                return nextAvailable;
            } catch ( IOException e ) {
                nbTries--;
                
                if (nbTries == 0) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
