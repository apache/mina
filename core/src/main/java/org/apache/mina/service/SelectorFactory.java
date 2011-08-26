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
package org.apache.mina.service;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A factory used by {@link SelectorStrategy} for instantiating selectors when needed.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class SelectorFactory {
    /** A logger for this class */
    static final Logger LOG = LoggerFactory.getLogger(SelectorFactory.class);
        
    private Constructor<? extends SelectorProcessor> constructor;
    
    /**
     * create a factory for the given {@link SelectorProcessor} type.
     * @param selectorClass
     */
    public SelectorFactory(Class<? extends SelectorProcessor> selectorClass) {
        try {
            constructor = selectorClass.getDeclaredConstructor(new Class<?>[]{String.class,SelectorStrategy.class});
        } catch (NoSuchMethodException e) {
            LOG.error("NoSuchMethodException while instantiating selector",e);
        } catch (SecurityException e) {
            LOG.error("SecurityException while instantiating selector",e);
        }   
    }
    
    /**
     * 
     */
    public SelectorProcessor getNewSelector(String name,SelectorStrategy strategy) {
        try {
            return (SelectorProcessor)constructor.newInstance(name,strategy);
        } catch (SecurityException e) {
            LOG.error("SecurityException while instantiating selector",e);
        } catch (IllegalArgumentException e) {
            LOG.error("IllegalArgumentException while instantiating selector",e);
        } catch (InstantiationException e) {
            LOG.error("InstantiationException while instantiating selector",e);
        } catch (IllegalAccessException e) {
            LOG.error("IllegalAccessException while instantiating selector",e);
        } catch (InvocationTargetException e) {
            LOG.error("InvocationTargetException while instantiating selector",e);
        }
        return null;
    }
}
