package org.apache.mina.example.imagine.step1.client;

import org.apache.mina.example.imagine.step1.ImageRequest;

import javax.swing.JOptionPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import java.awt.image.BufferedImage;
import java.awt.Checkbox;

/**
 * @author Maarten Bosteels
 */
@SuppressWarnings({"FieldCanBeLocal"})
public class GraphicalCharGenClient extends javax.swing.JFrame implements ImageListener {
    public static final int PORT = 33789;
    public static final String HOST = "localhost";

    public GraphicalCharGenClient() {
        initComponents();
    }

    private void jButtonConnectActionPerformed() {
        setTitle("connecting...");
        imageClient.connect();
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
        if (checkboxContinous.getState()) {
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

    public void setTitle(String title) {
        super.setTitle("MINA - Chargen client - " + title);
    }

    private void initComponents() {
        setTitle("");
        jPanel1 = new javax.swing.JPanel();
        jTextFieldHost = new javax.swing.JTextField();
        jTextFieldPort = new javax.swing.JTextField();
        jButtonConnect = new javax.swing.JButton();
        jButtonDisconnect = new javax.swing.JButton();
        jSpinnerWidth = new javax.swing.JSpinner();
        jSpinnerHeight = new javax.swing.JSpinner();
        jButtonSendRequest = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jSpinnerChars = new javax.swing.JSpinner();
        jLabel5 = new javax.swing.JLabel();
        checkboxContinous = new java.awt.Checkbox();
        imagePanel1 = new ImagePanel();

        jSpinnerHeight.setModel(spinnerHeightModel);
        jSpinnerWidth.setModel(spinnerWidthModel);
        jSpinnerChars.setModel(spinnerCharsModel);


        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        jTextFieldHost.setText("localhost");

        jTextFieldPort.setText("33789");

        checkboxContinous.setLabel("continuous");
        jButtonConnect.setText("Connect");
        jButtonConnect.setName("connectButton");
        jButtonConnect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonConnectActionPerformed();
            }
        });

        jButtonDisconnect.setText("Disconnect");
        jButtonDisconnect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDisconnectActionPerformed();
            }
        });

        jButtonSendRequest.setText("Send Request");
        jButtonSendRequest.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSendRequestActionPerformed();
            }
        });

        jLabel1.setText("Host");
        jLabel2.setText("Port");
        jLabel3.setText("Width");
        jLabel4.setText("Height");
        jLabel5.setText("characters");
        jButtonDisconnect.setEnabled(false);
        jButtonSendRequest.setEnabled(false);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jTextFieldPort, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTextFieldHost, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 111, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jButtonConnect, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButtonDisconnect, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(39, 39, 39)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel3)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jSpinnerHeight)
                    .addComponent(jSpinnerWidth, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(14, 14, 14)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jSpinnerChars))
                    .addComponent(jButtonSendRequest))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 39, Short.MAX_VALUE)
                .addComponent(checkboxContinous, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jTextFieldHost, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jButtonConnect)
                        .addComponent(jLabel1))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 8, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextFieldPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonDisconnect)
                    .addComponent(jLabel2)))
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                    .addGap(12, 12, 12)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jSpinnerWidth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel3)
                        .addComponent(jLabel5)
                        .addComponent(jSpinnerChars, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 12, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jSpinnerHeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4)
                    .addComponent(jButtonSendRequest)))
                .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(checkboxContinous, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(35, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout imagePanel1Layout = new javax.swing.GroupLayout(imagePanel1);
        imagePanel1.setLayout(imagePanel1Layout);
        imagePanel1Layout.setHorizontalGroup(
            imagePanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGap(0, 544, Short.MAX_VALUE)
        );
        imagePanel1Layout.setVerticalGroup(
            imagePanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGap(0, 308, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addContainerGap())
                .addComponent(imagePanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(imagePanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        pack();
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

    private ImagePanel imagePanel1;
    private javax.swing.JButton jButtonConnect;
    private javax.swing.JButton jButtonDisconnect;
    private javax.swing.JButton jButtonSendRequest;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JSpinner jSpinnerChars;
    private javax.swing.JSpinner jSpinnerHeight;
    private javax.swing.JSpinner jSpinnerWidth;
    private javax.swing.JTextField jTextFieldHost;
    private javax.swing.JTextField jTextFieldPort;
    private SpinnerNumberModel spinnerHeightModel = new SpinnerNumberModel(100, 50, 600, 25);
    private SpinnerNumberModel spinnerWidthModel = new SpinnerNumberModel(175, 50, 1000, 25);
    private SpinnerNumberModel spinnerCharsModel = new SpinnerNumberModel(10, 1, 60, 1);
    private Checkbox checkboxContinous;
    private ImageClient imageClient = new ImageClient(HOST, PORT, this);

}

