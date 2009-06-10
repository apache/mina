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

package org.apache.mina.example.imagine.step1.client;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import org.apache.mina.example.imagine.step1.ImageRequest;
import org.apache.mina.example.imagine.step1.server.ImageServer;

/**
 * Swing application that acts as a client of the {@link ImageServer}
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class GraphicalCharGenClient extends JFrame implements ImageListener {

    private static final long serialVersionUID = 1L;

    public static final int PORT = 33789;
    public static final String HOST = "localhost";

    public GraphicalCharGenClient() {
        initComponents();
        jSpinnerHeight.setModel(spinnerHeightModel);
        jSpinnerWidth.setModel(spinnerWidthModel);
        jSpinnerChars.setModel(spinnerCharsModel);
        jTextFieldHost.setText(HOST);
        jTextFieldPort.setText(String.valueOf(PORT));
        setTitle("");
    }

    private void jButtonConnectActionPerformed() {
        try {
            setTitle("connecting...");
            String host = jTextFieldHost.getText();
            int port = Integer.valueOf(jTextFieldPort.getText());
            if (imageClient != null) {
                imageClient.disconnect();
            }
            imageClient = new ImageClient(host, port, this);
            imageClient.connect();
            jButtonConnect.setEnabled(!imageClient.isConnected());
        } catch (NumberFormatException e) {
            onException(e);
        } catch (IllegalArgumentException e) {
            onException(e);
        }
    }

    private void jButtonDisconnectActionPerformed() {
        setTitle("disconnecting");
        imageClient.disconnect();
    }

    private void jButtonSendRequestActionPerformed() {
        sendRequest();
    }

    private void sendRequest() {
        int chars = spinnerCharsModel.getNumber().intValue();
        int height = spinnerHeightModel.getNumber().intValue();
        int width = spinnerWidthModel.getNumber().intValue();
        imageClient.sendRequest(new ImageRequest(width, height, chars));
    }

    public void onImages(BufferedImage image1, BufferedImage image2) {
        if (checkBoxContinuous.isSelected()) {
            // already request next image
            sendRequest();
        }
        imagePanel1.setImages(image1, image2);
    }

    public void onException(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        JOptionPane.showMessageDialog(
                this,
                cause.getMessage(),
                throwable.getMessage(),
                JOptionPane.ERROR_MESSAGE);
        setTitle("");
        jButtonConnect.setEnabled(!imageClient.isConnected());
        jButtonDisconnect.setEnabled(imageClient.isConnected());
    }

    public void sessionOpened() {
        jButtonDisconnect.setEnabled(true);
        jButtonSendRequest.setEnabled(true);
        jButtonConnect.setEnabled(false);
        setTitle("connected");
    }

    public void sessionClosed() {
        jButtonDisconnect.setEnabled(false);
        jButtonSendRequest.setEnabled(false);
        jButtonConnect.setEnabled(true);
        setTitle("not connected");
    }

    @Override
    public void setTitle(String title) {
        super.setTitle("MINA - Chargen client - " + title);
    }


    private void initComponents() {
        JLabel jLabel1 = new JLabel();
        jTextFieldHost = new JTextField();
        jButtonConnect = new JButton();
        JLabel jLabel3 = new JLabel();
        jSpinnerWidth = new JSpinner();
        JLabel label5 = new JLabel();
        jSpinnerChars = new JSpinner();
        checkBoxContinuous = new JCheckBox();
        JLabel jLabel2 = new JLabel();
        jTextFieldPort = new JTextField();
        jButtonDisconnect = new JButton();
        JLabel jLabel4 = new JLabel();
        jSpinnerHeight = new JSpinner();
        jButtonSendRequest = new JButton();
        imagePanel1 = new ImagePanel();

        //======== this ========
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(700, 300));
        setPreferredSize(new Dimension(740, 600));
        Container contentPane = getContentPane();
        contentPane.setLayout(new GridBagLayout());
        ((GridBagLayout) contentPane.getLayout()).columnWidths = new int[]{36, 167, 99, 41, 66, 75, 57, 96, 0, 0};
        ((GridBagLayout) contentPane.getLayout()).rowHeights = new int[]{10, 31, 31, 256, 0};
        ((GridBagLayout) contentPane.getLayout()).columnWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0E-4};
        ((GridBagLayout) contentPane.getLayout()).rowWeights = new double[]{0.0, 0.0, 0.0, 1.0, 1.0E-4};

        //---- jLabel1 ----
        jLabel1.setText("Host");
        contentPane.add(jLabel1, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 5, 5, 5), 0, 0));
        contentPane.add(jTextFieldHost, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 5, 5, 10), 0, 0));

        //---- jButtonConnect ----
        jButtonConnect.setText("Connect");
        jButtonConnect.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jButtonConnectActionPerformed();
            }
        });
        contentPane.add(jButtonConnect, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 5, 5, 10), 0, 0));

        //---- jLabel3 ----
        jLabel3.setText("Width");
        contentPane.add(jLabel3, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 5), 0, 0));
        contentPane.add(jSpinnerWidth, new GridBagConstraints(4, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 5, 5, 10), 0, 0));

        //---- label5 ----
        label5.setText("characters");
        contentPane.add(label5, new GridBagConstraints(5, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.VERTICAL,
                new Insets(0, 0, 5, 5), 0, 0));
        contentPane.add(jSpinnerChars, new GridBagConstraints(6, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 10), 0, 0));

        //---- checkBoxContinuous ----
        checkBoxContinuous.setText("continuous");
        contentPane.add(checkBoxContinuous, new GridBagConstraints(7, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 5, 5, 10), 0, 0));

        //---- jLabel2 ----
        jLabel2.setText("Port");
        contentPane.add(jLabel2, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 5, 5, 5), 0, 0));
        contentPane.add(jTextFieldPort, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 5, 5, 10), 0, 0));

        //---- jButtonDisconnect ----
        jButtonDisconnect.setText("Disconnect");
        jButtonDisconnect.setEnabled(false);
        jButtonDisconnect.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jButtonDisconnectActionPerformed();
            }
        });
        contentPane.add(jButtonDisconnect, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 5, 5, 10), 0, 0));

        //---- jLabel4 ----
        jLabel4.setText("Height");
        contentPane.add(jLabel4, new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 5), 0, 0));
        contentPane.add(jSpinnerHeight, new GridBagConstraints(4, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 5, 5, 10), 0, 0));

        //---- jButtonSendRequest ----
        jButtonSendRequest.setText("Send Request");
        jButtonSendRequest.setEnabled(false);
        jButtonSendRequest.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jButtonSendRequestActionPerformed();
            }
        });
        contentPane.add(jButtonSendRequest, new GridBagConstraints(5, 2, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 5, 5, 10), 0, 0));

        //======== imagePanel1 ========
        {
            imagePanel1.setBackground(new Color(51, 153, 255));
            imagePanel1.setPreferredSize(new Dimension(500, 500));

            { // compute preferred size
                Dimension preferredSize = new Dimension();
                for (int i = 0; i < imagePanel1.getComponentCount(); i++) {
                    Rectangle bounds = imagePanel1.getComponent(i).getBounds();
                    preferredSize.width = Math.max(bounds.x + bounds.width, preferredSize.width);
                    preferredSize.height = Math.max(bounds.y + bounds.height, preferredSize.height);
                }
                Insets insets = imagePanel1.getInsets();
                preferredSize.width += insets.right;
                preferredSize.height += insets.bottom;
                imagePanel1.setMinimumSize(preferredSize);
                imagePanel1.setPreferredSize(preferredSize);
            }
        }
        contentPane.add(imagePanel1, new GridBagConstraints(0, 3, 9, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(8, 5, 8, 5), 0, 0));
        pack();
        setLocationRelativeTo(getOwner());
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // ignore
        }
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new GraphicalCharGenClient().setVisible(true);
            }
        });
    }

    private JTextField jTextFieldHost;
    private JButton jButtonConnect;
    private JSpinner jSpinnerWidth;
    private JSpinner jSpinnerChars;
    private JCheckBox checkBoxContinuous;
    private JTextField jTextFieldPort;
    private JButton jButtonDisconnect;
    private JSpinner jSpinnerHeight;
    private JButton jButtonSendRequest;
    private ImagePanel imagePanel1;

    private SpinnerNumberModel spinnerHeightModel = new SpinnerNumberModel(100, 50, 600, 25);
    private SpinnerNumberModel spinnerWidthModel = new SpinnerNumberModel(200, 50, 1000, 25);
    private SpinnerNumberModel spinnerCharsModel = new SpinnerNumberModel(10, 1, 60, 1);

    private ImageClient imageClient = new ImageClient(HOST, PORT, this);
}
