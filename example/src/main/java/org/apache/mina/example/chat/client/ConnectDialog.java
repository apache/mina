/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.mina.example.chat.client;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * TODO Add documentation
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 *
 */
public class ConnectDialog extends JDialog {
    private static final long serialVersionUID = 2009384520250666216L;

    private String serverAddress;

    private String username;

    private boolean useSsl;

    private boolean cancelled = false;

    public ConnectDialog(Frame owner) throws HeadlessException {
        super(owner, "Connect", true);

        serverAddress = "localhost:1234";
        username = "user" + Math.round(Math.random() * 10);

        final JTextField serverAddressField = new JTextField(serverAddress);
        final JTextField usernameField = new JTextField(username);
        final JCheckBox useSslCheckBox = new JCheckBox("Use SSL", false);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));
        content.add(new JLabel("Server address"));
        content.add(serverAddressField);
        content.add(new JLabel("Username"));
        content.add(usernameField);
        content.add(useSslCheckBox);

        JButton okButton = new JButton();
        okButton.setAction(new AbstractAction("OK") {
            private static final long serialVersionUID = -2292183622613960604L;

            public void actionPerformed(ActionEvent e) {
                serverAddress = serverAddressField.getText();
                username = usernameField.getText();
                useSsl = useSslCheckBox.isSelected();
                ConnectDialog.this.dispose();
            }
        });

        JButton cancelButton = new JButton();
        cancelButton.setAction(new AbstractAction("Cancel") {
            private static final long serialVersionUID = 6122393546173723305L;

            public void actionPerformed(ActionEvent e) {
                cancelled = true;
                ConnectDialog.this.dispose();
            }
        });

        JPanel buttons = new JPanel();
        buttons.add(okButton);
        buttons.add(cancelButton);

        getContentPane().add(content, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public String getUsername() {
        return username;
    }

    public boolean isUseSsl() {
        return useSsl;
    }
}
