package org.apache.mina.protocol;

import java.util.List;


public interface ProtocolHandlerFilterChain extends ProtocolHandlerFilter {
    static String NEXT_FILTER = "nextFilter";
    
    ProtocolHandlerFilterChain getRoot();
    ProtocolHandlerFilterChain getParent();

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
