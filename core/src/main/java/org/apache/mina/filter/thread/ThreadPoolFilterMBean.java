package org.apache.mina.filter.thread;

public interface ThreadPoolFilterMBean
{
    String getThreadNamePrefix();

    void setThreadNamePrefix( String threadNamePrefix );

    int getPoolSize();

    int getMaximumPoolSize();

    int getKeepAliveTime();

    void setMaximumPoolSize( int maximumPoolSize );

    void setKeepAliveTime( int keepAliveTime );
}
