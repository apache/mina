/*
 * @(#) $Id$
 */
package org.apache.mina.transport.vmpipe.support;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.mina.common.IoAcceptorConfig;
import org.apache.mina.common.IoHandler;
import org.apache.mina.transport.vmpipe.VmPipeAcceptor;
import org.apache.mina.transport.vmpipe.VmPipeAddress;

public class VmPipe
{
    private final VmPipeAcceptor acceptor;
    private final VmPipeAddress address;
    private final IoHandler handler;
    private final IoAcceptorConfig config;
    private final Set managedClientSessions = Collections.synchronizedSet( new HashSet() );
    private final Set managedServerSessions = Collections.synchronizedSet( new HashSet() );
    
    public VmPipe( VmPipeAcceptor acceptor,
                   VmPipeAddress address,
                   IoHandler handler,
                   IoAcceptorConfig config )
    {
        this.acceptor = acceptor;
        this.address = address;
        this.handler = handler;
        this.config = config;
    }

    public VmPipeAcceptor getAcceptor()
    {
        return acceptor;
    }

    public VmPipeAddress getAddress()
    {
        return address;
    }

    public IoHandler getHandler()
    {
        return handler;
    }
    
    public IoAcceptorConfig getConfig()
    {
        return config;
    }

    public Set getManagedClientSessions()
    {
        return managedClientSessions;
    }
    
    public Set getManagedServerSessions()
    {
        return managedServerSessions;
    }
}