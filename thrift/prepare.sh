#!/bin/bash

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
