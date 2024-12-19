<?xml version="1.0" encoding="ISO-8859-1"?>

<!--
  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements.  See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership.  The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License.  You may obtain a copy of the License at
  
  http://www.apache.org/licenses/LICENSE-2.0
  
  Unless required by applicable law or agreed to in writing,
  software distributed under the License is distributed on an
  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  KIND, either express or implied.  See the License for the
  specific language governing permissions and limitations
  under the License.
-->

<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>org.apache.mina</groupId>
    <artifactId>mina-parent</artifactId>
    <version>2.2.4</version>
  </parent>

  <artifactId>mina-core</artifactId>
  <name>Apache MINA Core</name>
  <packaging>bundle</packaging>

  <dependencies>
    <!-- Test dependencies -->
    <dependency>
      <groupId>org.easymock</groupId>
      <artifactId>easymock</artifactId>
    </dependency>
    
    <dependency>
      <groupId>org.mockito</groupId>
      <artifactId>mockito-core</artifactId>
    </dependency>

  </dependencies>
  
  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.felix</groupId>
        <artifactId>maven-bundle-plugin</artifactId>
        <inherited>true</inherited>
        <extensions>true</extensions>
        <configuration>
          <manifestLocation>META-INF</manifestLocation>
          <instructions>
            <Bundle-SymbolicName>${project.groupId}.core</Bundle-SymbolicName>
            <Export-Package>
              org.apache.mina.core,
              org.apache.mina.core.buffer,
              org.apache.mina.core.buffer.matcher,
              org.apache.mina.core.file,
              org.apache.mina.core.filterchain,
              org.apache.mina.core.future,
              org.apache.mina.core.polling,
              org.apache.mina.core.service,
              org.apache.mina.core.session,
              org.apache.mina.core.write,
              org.apache.mina.filter,
              org.apache.mina.filter.buffer,
              org.apache.mina.filter.codec,
              org.apache.mina.filter.codec.demux,
              org.apache.mina.filter.codec.prefixedstring,
              org.apache.mina.filter.codec.serialization,
              org.apache.mina.filter.codec.statemachine,
              org.apache.mina.filter.codec.textline,
              org.apache.mina.filter.errorgenerating,
              org.apache.mina.filter.executor,
              org.apache.mina.filter.firewall,
              org.apache.mina.filter.keepalive,
              org.apache.mina.filter.logging,
              org.apache.mina.filter.ssl,
              org.apache.mina.filter.statistic,
              org.apache.mina.filter.stream,
              org.apache.mina.filter.util,
              org.apache.mina.handler.chain,
              org.apache.mina.handler.demux,
              org.apache.mina.handler.multiton,
              org.apache.mina.handler.stream,
              org.apache.mina.proxy,
              org.apache.mina.proxy.event,
              org.apache.mina.proxy.filter,
              org.apache.mina.proxy.handlers,
              org.apache.mina.proxy.handlers.http,
              org.apache.mina.proxy.handlers.http.basic,
              org.apache.mina.proxy.handlers.http.digest,
              org.apache.mina.proxy.handlers.http.ntlm,
              org.apache.mina.proxy.handlers.socks,
              org.apache.mina.proxy.session,
              org.apache.mina.proxy.utils,
              org.apache.mina.transport.socket,
              org.apache.mina.transport.socket.nio,
              org.apache.mina.transport.vmpipe,
              org.apache.mina.util,
              org.apache.mina.util.byteaccess
            </Export-Package>
            <Import-Package>
              javax.crypto,javax.crypto.spec,javax.net.ssl,javax.security.sasl,org.slf4j;version=${osgi-min-version.slf4j.api}
            </Import-Package>
          </instructions>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
