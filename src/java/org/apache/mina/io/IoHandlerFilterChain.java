package org.apache.mina.io;

import java.util.List;


public interface IoHandlerFilterChain extends IoHandlerFilter {
    static String NEXT_FILTER = "nextFilter";
    
    IoHandlerFilterChain getRoot();
    IoHandlerFilterChain getParent();

    IoHandlerFilter getChild( String name );
    List getChildren();
    List getChildrenReversed();
    void addFirst( String name, IoHandlerFilter filter );
    void addLast( String name, IoHandlerFilter filter );
    void addBefore( String baseName, String name, IoHandlerFilter filter );
    void addAfter( String baseName, String name, IoHandlerFilter filter );
    void remove( String name );
    void clear();
}
