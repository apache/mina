package org.apache.mina.common;

import org.apache.mina.filter.ThreadPoolFilter;

public class PooledThreadModel implements ThreadModel
{
    /**
     * @see ThreadPoolFilter#DEFAULT_MAXIMUM_POOL_SIZE
     */
    public static final int DEFAULT_MAXIMUM_POOL_SIZE = ThreadPoolFilter.DEFAULT_MAXIMUM_POOL_SIZE;

    /**
     * @see ThreadPoolFilter#DEFAULT_KEEP_ALIVE_TIME
     */
    public static final int DEFAULT_KEEP_ALIVE_TIME = ThreadPoolFilter.DEFAULT_KEEP_ALIVE_TIME;
    
    private static int id = 1;

    private final ThreadPoolFilter filter;
    
    public PooledThreadModel()
    {
        this( "AnonymousIoService-" + id++, DEFAULT_MAXIMUM_POOL_SIZE );
    }

    public PooledThreadModel( String threadNamePrefix )
    {
        this( threadNamePrefix, DEFAULT_MAXIMUM_POOL_SIZE );
    }

    public PooledThreadModel( String threadNamePrefix, int maxThreads )
    {
        filter = new ThreadPoolFilter();
        setMaximumPoolSize( maxThreads );
        setThreadNamePrefix( threadNamePrefix );
    }

    public String getThreadNamePrefix()
    {
        return filter.getThreadNamePrefix();
    }

    public void setThreadNamePrefix( String threadNamePrefix )
    {
        filter.setThreadNamePrefix( threadNamePrefix );
    }
    
    public int getPoolSize()
    {
        return filter.getPoolSize();
    }

    public int getMaximumPoolSize()
    {
        return filter.getMaximumPoolSize();
    }

    public int getKeepAliveTime()
    {
        return filter.getKeepAliveTime();
    }

    public void setMaximumPoolSize( int maximumPoolSize )
    {
        filter.setMaximumPoolSize( maximumPoolSize );
    }

    public void setKeepAliveTime( int keepAliveTime )
    {
        filter.setKeepAliveTime( keepAliveTime );
    }

    public void buildFilterChain( IoFilterChain chain ) throws Exception
    {
        chain.addFirst( PooledThreadModel.class.getName(), filter );
    }
}
