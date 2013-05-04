#!/bin/bash

if hash protoc 2>/dev/null; then
	VERSION=`protoc --version`

	if [[ $VERSION =~ libprotoc\ 2\.5\. ]];
	then
		protoc --java_out=src/test/java src/test/protobuf/addressbook.proto src/test/protobuf/calc.proto
	else
		echo "Need Google Protocol Buffer 2.5.x (found $VERSION)"
	fi
else
	echo "No installation of Google Protocol Buffer found"
fi

