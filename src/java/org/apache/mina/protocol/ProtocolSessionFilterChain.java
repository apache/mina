package org.apache.mina.protocol;

/**
 * TODO document me
 */
public class ProtocolSessionFilterChain extends AbstractProtocolHandlerFilterChain {

    private final ProtocolSessionManagerFilterChain prevChain;

    public ProtocolSessionFilterChain( ProtocolSessionManagerFilterChain prevChain )
    {
        this.prevChain = prevChain;
    }

    protected void doWrite( ProtocolSession session, Object message )
    {
        prevChain.filterWrite( session, message );
    }
}
