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

/**
 * Utility to retrieving the thread stack debug information
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @author Jonathan Valliere
 */
public class StackInspector extends RuntimeException {
	static public final StackTraceElement callee() {
		return Thread.currentThread().getStackTrace()[3];
	}

	static public final StackInspector get(String message) {
		try {
			throw new StackInspector(message);
		} catch (StackInspector e0) {
			return e0;
		}
	}

	static public final StackInspector get(Throwable cause) {
		try {
			throw new StackInspector(cause);
		} catch (StackInspector e0) {
			return e0;
		}
	}

	static public final StackInspector get() {
		try {
			throw new StackInspector("Stack from Thread: " + Thread.currentThread().getName());
		} catch (StackInspector e0) {
			return e0;
		}
	}

	static private final long serialVersionUID = 1L;

	StackInspector() {

	}

	StackInspector(String message) {
		super(message);
	}

	StackInspector(Throwable cause) {
		super(cause);
	}

	StackInspector(String message, Throwable cause) {
		super(message, cause);
	}

	StackInspector(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
