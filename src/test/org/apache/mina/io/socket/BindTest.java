/*
 * @(#) $Id$
 */
package org.apache.mina.io.socket;

import java.io.IOException;

import org.apache.mina.io.AbstractBindTest;

public class BindTest extends AbstractBindTest
{

    public BindTest() throws IOException
    {
        super( new SocketAcceptor() );
    }

}
