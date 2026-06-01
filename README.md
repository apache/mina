# Apache MINAa developer guide

This document gathers the minimal information about how to build the project.

All the detailed information can be found in the (MINA Developer Guide page)[https://mina.apache.org/mina-project/developer-guide.html]

## Building MINA

You need Git to check out the source code from our source code repository.

We have 3 branches:

* 2.2.X, The latest version
* 2.1.X 
* 2.0.X

The following example shows how to get the current stable branch (2.2.X).

$ git clone -b 2.2.X https://gitbox.apache.org/repos/asf/mina.git mina-2.2.X
$ cd mina-2.2.X

## Prerequisites

MINA requires Maven 3.8.5 at least, but builds well with recent version (we haven't yet tested it with Maven 4)

You will need different versions of Java for the three branches:

* 2.2.X: Java 17 is required
* 2.1.X and 2.0.X: Java 1.8 is required

## Building MINA

It's as simple as typing:

```
$ mvn clean install [-Pserial]
```

(The '-Pserial' flag is optional. It's onlt use if you want to generate the code using the LGPL rxtx library).

You are done...

## Code convention

Like it or not, we follow the ancient Sun's standard Java convention. Not tabs, 4 spaces instead. Please respect this convezntion, it saves the committers a lot of time when merging PRs.


