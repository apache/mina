package org.apache.mina.io;

import java.util.List;

import org.apache.mina.common.FilterChainType;


public interface IoHandlerFilterChain extends IoHandlerFilter {
    IoHandlerFilterChain getRoot();
    IoHandlerFilterChain getParent();
    FilterChainType getType();

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
