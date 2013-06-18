#!/bin/bash
#
#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing,
#  software distributed under the License is distributed on an
#  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#  KIND, either express or implied.  See the License for the
#  specific language governing permissions and limitations
#  under the License.
#

if hash thrift 2>/dev/null; then
	VERSION=`thrift --version`

	if [[ $VERSION =~  version\ 0\.9\. ]];
	then
		thrift -I src/test/thrift -out src/test/java --gen java:hashcode src/test/thrift/user.thrift
	else
		echo "Need Thrift 0.9.x (found $VERSION)"
	fi
else
	echo "No installation of Thrift found"
fi
