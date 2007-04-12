/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.mina.filter.reqres;

import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev$, $Date$
 */
public class Request {
    private final Object id;
    private final Object message;
    private final long timeoutMillis;
    private TimerTask timerTask;
    
    public Request(Object id, Object message, long timeoutMillis) {
        this(id, message, timeoutMillis, TimeUnit.MILLISECONDS);
    }
    
    public Request(Object id, Object message, long timeout, TimeUnit unit) {
        if (id == null) {
            throw new NullPointerException("id");
        }
        if (message == null) {
            throw new NullPointerException("message");
        }
        if (timeout < 0) {
            throw new IllegalArgumentException(
                    "timeout: " + timeout + " (expected: 0+)");
        } else if (timeout == 0) {
            timeout = Long.MAX_VALUE;
        }

        if (unit == null) {
            throw new NullPointerException("unit");
        }
        
        this.id = id;
        this.message = message;
        this.timeoutMillis = unit.toMillis(timeout);
    }
    
    public Object getId() {
        return id;
    }

    public Object getMessage() {
        return message;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }
    
    @Override
    public int hashCode() {
        return getId().hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        
        if (o == null) {
            return false;
        }
        
        if (!(o instanceof Request)) {
            return false;
        }
        
        Request that = (Request) o;
        return this.getId().equals(that.getId());
    }

    @Override
    public String toString() {
        String timeout = (getTimeoutMillis() == Long.MAX_VALUE)?
                "max" : String.valueOf(getTimeoutMillis());

        return "request: { id=" + getId() +
               ", timeout=" + timeout + ", message=" + getMessage() + " }";
    }
    
    TimerTask getTimerTask() {
        return timerTask;
    }
    
    void setTimerTask(TimerTask timerTask) {
        this.timerTask = timerTask;
    }
}
