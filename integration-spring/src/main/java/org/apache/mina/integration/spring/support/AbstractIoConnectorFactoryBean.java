/*
 *   @(#) $Id$
 *
 *   Copyright 2004 The Apache Software Foundation
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
package org.apache.mina.integration.spring.support;

import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoService;

/**
 * Abstract Spring FactoryBean which creates {@link IoConnector} instances and
 * enables their filters to be configured using Spring.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public abstract class AbstractIoConnectorFactoryBean extends
        AbstractIoServiceFactoryBean
{

    private int connectTimeout = 60;

    /**
     * Creates the {@link IoConnector} configured by this factory bean.
     * 
     * @return the new instance
     * @throws Exception on errors.
     */
    protected abstract IoConnector createIoConnector() throws Exception;

    /**
     * Creates a new {@link IoConnector}. Calls {@link #createIoConnector()} to
     * get the new instance and then calls {@link AbstractIoServiceFactoryBean#initIoService(IoService)}
     * followed by {@link #initIoConnector(IoConnector)}.
     * 
     * @return the new instance
     */
    protected Object createInstance() throws Exception
    {
        IoConnector connector = createIoConnector();

        initIoService( connector );
        initIoConnector( connector );

        return connector;
    }

    /**
     * Initializes the specified <code>connector</code> configured by this
     * factory bean.
     * 
     * @throws Exception on errors.
     */
    protected void initIoConnector( IoConnector connector ) throws Exception
    {
        connector.setConnectTimeout( connectTimeout );
    }

    /**
     * Destroys an IoConnector created by the factory bean by calling
     * {@link AbstractIoServiceFactoryBean#destroyIoService(IoService)}.
     * This method may be overridden by extending classes if additional calls
     * are necessary to shutdown the IoConnector or if the sequence of calls
     * should be different.
     * <p>
     * This method will be called by Spring when the BeanFactory creating this
     * instance is closed. Spring will NOT call this method if this factory bean
     * has been configured for non-singleton use.
     * </p>
     * 
     * @param instance
     *            the IoAcceptor instance to be destroyed.
     */
    protected void destroyInstance( Object instance ) throws Exception
    {
        IoConnector connector = ( IoConnector ) instance;
        destroyIoService( connector );
    }

    public Class getObjectType()
    {
        return IoConnector.class;
    }

    /**
     * Sets the <code>connectTimeout</code> property of the
     * {@link IoConnector} this factory bean will create.
     * 
     * @param connectTimeout
     *            the property value.
     * @see IoConnector#setConnectTimeout(int)
     */
    public void setConnectTimeout( int connectTimeout )
    {
        this.connectTimeout = connectTimeout;
    }
}
