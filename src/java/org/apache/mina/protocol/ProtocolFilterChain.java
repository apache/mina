package org.apache.mina.protocol;

import java.util.List;


public interface ProtocolFilterChain {
    ProtocolFilter getChild( String name );
    List getChildren();
    List getChildrenReversed();
    void addFirst( String name, ProtocolFilter filter );
    void addLast( String name, ProtocolFilter filter );
    void addBefore( String baseName, String name, ProtocolFilter filter );
    void addAfter( String baseName, String name, ProtocolFilter filter );
    void remove( String name );
    void clear();
}
