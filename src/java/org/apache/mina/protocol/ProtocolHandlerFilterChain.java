package org.apache.mina.protocol;

import java.util.List;


public interface ProtocolHandlerFilterChain {
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
