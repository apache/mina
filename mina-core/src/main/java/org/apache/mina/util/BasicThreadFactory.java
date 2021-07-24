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
package org.apache.mina.util;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility for creating thread factories
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @author Jonathan Valliere
 */
public class BasicThreadFactory implements java.util.concurrent.ThreadFactory {
	public final AtomicInteger count = new AtomicInteger(0);
	public final String name;

	public final boolean deamon;
	public final int priority;

	public BasicThreadFactory(String basename, boolean daemon, int priority) {
		this.name = basename;
		this.deamon = daemon;
		this.priority = priority;
	}

	public BasicThreadFactory(String basename, boolean daemon) {
		this(basename, daemon, Thread.NORM_PRIORITY);
	}

	public BasicThreadFactory(String basename) {
		this(basename, false, Thread.NORM_PRIORITY);
	}

	@Override
	public Thread newThread(Runnable pool) {
		Thread t = new Thread(pool);

		t.setName(this.name + "-" + this.count.getAndIncrement());
		t.setPriority(this.priority);
		t.setDaemon(this.deamon);

		return t;
	}

}
