package org.apache.mina.protocol;

import java.util.List;

import org.apache.mina.common.FilterChainType;


public interface ProtocolHandlerFilterChain extends ProtocolHandlerFilter {
    ProtocolHandlerFilterChain getRoot();
    ProtocolHandlerFilterChain getParent();
    FilterChainType getType();

    ProtocolHandlerFilter getChild( String name );
    List getChildren();
    List getChildrenReversed();
    void addFirst( String name, ProtocolHandlerFilter filter );
    void addLast( String name, ProtocolHandlerFilter filter );
    void addBefore( String baseName, String name, ProtocolHandlerFilter filter );
    void addAfter( String baseName, String name, ProtocolHandlerFilter filter );
    void remove( String name );
    void clear();
}
