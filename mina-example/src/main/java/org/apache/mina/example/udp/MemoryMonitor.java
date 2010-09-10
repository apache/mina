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
package org.apache.mina.example.udp;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.DatagramSessionConfig;
import org.apache.mina.transport.socket.nio.NioDatagramAcceptor;

/**
 * The class that will accept and process clients in order to properly
 * track the memory usage.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class MemoryMonitor {

    private static final long serialVersionUID = 1L;

    public static final int PORT = 18567;

    protected static final Dimension PANEL_SIZE = new Dimension(300, 200);

    private JFrame frame;

    private JTabbedPane tabbedPane;

    private ConcurrentHashMap<SocketAddress, ClientPanel> clients;

    public MemoryMonitor() throws IOException {

        NioDatagramAcceptor acceptor = new NioDatagramAcceptor();
        acceptor.setHandler(new MemoryMonitorHandler(this));

        DefaultIoFilterChainBuilder chain = acceptor.getFilterChain();
        chain.addLast("logger", new LoggingFilter());

        DatagramSessionConfig dcfg = acceptor.getSessionConfig();
        dcfg.setReuseAddress(true);

        frame = new JFrame("Memory monitor");
        tabbedPane = new JTabbedPane();
        tabbedPane.add("Welcome", createWelcomePanel());
        frame.add(tabbedPane, BorderLayout.CENTER);
        clients = new ConcurrentHashMap<SocketAddress, ClientPanel>();
        frame.pack();
        frame.setLocation(300, 300);
        frame.setVisible(true);

        acceptor.bind(new InetSocketAddress(PORT));
        System.out.println("UDPServer listening on port " + PORT);
    }

    private JPanel createWelcomePanel() {
        JPanel panel = new JPanel();
        panel.setPreferredSize(PANEL_SIZE);
        panel.add(new JLabel("Welcome to the Memory Monitor"));
        return panel;
    }

    protected void recvUpdate(SocketAddress clientAddr, long update) {
        ClientPanel clientPanel = clients.get(clientAddr);
        if (clientPanel != null) {
            clientPanel.updateTextField(update);
        } else {
            System.err.println("Received update from unknown client");
        }
    }

    protected void addClient(SocketAddress clientAddr) {
        if (!containsClient(clientAddr)) {
            ClientPanel clientPanel = new ClientPanel(clientAddr.toString());
            tabbedPane.add(clientAddr.toString(), clientPanel);
            clients.put(clientAddr, clientPanel);
        }
    }

    protected boolean containsClient(SocketAddress clientAddr) {
        return clients.contains(clientAddr);
    }

    protected void removeClient(SocketAddress clientAddr) {
        clients.remove(clientAddr);
    }

    public static void main(String[] args) throws IOException {
        new MemoryMonitor();
    }
}
