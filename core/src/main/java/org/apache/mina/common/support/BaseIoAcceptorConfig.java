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
package org.apache.mina.common.support;

import org.apache.mina.common.IoAcceptorConfig;

/**
 * A base implementation of {@link IoAcceptorConfig}.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public abstract class BaseIoAcceptorConfig extends BaseIoServiceConfig implements IoAcceptorConfig
{
    private boolean disconnectOnUnbind = true;
    
    protected BaseIoAcceptorConfig()
    {
        super();
    }


    public boolean isDisconnectOnUnbind()
    {
        return disconnectOnUnbind;
    }

    public void setDisconnectOnUnbind( boolean disconnectClientsOnUnbind )
    {
        this.disconnectOnUnbind = disconnectClientsOnUnbind;
    }
}
