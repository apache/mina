package org.apache.mina.common;

/**
 * TODO document me
 */
public class FilterChainType {
    
    /**
     * 'Preprocess chain' passes events to the next filter
     * of the parent chain after processing its children.
     */
    public static final FilterChainType PREPROCESS = new FilterChainType();

    /**
     * 'Postprocess chain' passes events to the next filter
     * of the parent chain before processing its children.
     */
    public static final FilterChainType POSTPROCESS = new FilterChainType();

    private FilterChainType()
    {
    }
}
