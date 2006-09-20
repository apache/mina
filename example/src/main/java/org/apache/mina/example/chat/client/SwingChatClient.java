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
package org.apache.mina.example.chat.client;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.apache.mina.example.chat.client.SwingChatClientHandler.Callback;
import org.apache.mina.transport.socket.nio.SocketConnector;

/**
 * Simple chat client based on Swing & MINA that implements the chat protocol.
 *  
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class SwingChatClient extends JFrame implements Callback
{
	private JTextField inputText;
	private JButton loginButton;
	private JButton quitButton;
	private JButton closeButton;
	private JTextField serverField;
	private JTextField nameField;
	private JTextArea area;
	private JScrollBar scroll;

	private ChatClientSupport client;
	private SwingChatClientHandler handler;
    private SocketConnector connector;

	public SwingChatClient()
    {
        super( "Chat Client based on Apache MINA" );
        
        connector = new SocketConnector();
        
        loginButton = new JButton( new LoginAction() );
        loginButton.setText( "Connect" );
        quitButton = new JButton( new LogoutAction() );
        quitButton.setText( "Disconnect" );
        closeButton = new JButton( new QuitAction() );
        closeButton.setText( "Quit" );
        inputText = new JTextField( 30 );
        inputText.setAction( new BroadcastAction() );
        area = new JTextArea( 10, 50 );
        area.setLineWrap( true );
        area.setEditable( false );
        scroll = new JScrollBar();
        scroll.add( area );
        nameField = new JTextField( 10 );
        nameField.setEditable( false );
        serverField = new JTextField( 10 );
        serverField.setEditable( false );

        JPanel h = new JPanel();
        h.setLayout( new BoxLayout( h, BoxLayout.LINE_AXIS ) );
        h.setBorder( BorderFactory.createEmptyBorder( 10, 10, 10, 10 ) );
        JLabel nameLabel = new JLabel( "Name: " );
        JLabel serverLabel = new JLabel( "Server: " );
        h.add( nameLabel );
        h.add( Box.createRigidArea( new Dimension( 10, 0 ) ) );
        h.add( nameField );
        h.add( Box.createRigidArea( new Dimension( 10, 0 ) ) );
        h.add( Box.createHorizontalGlue() );
        h.add( Box.createRigidArea( new Dimension( 10, 0 ) ) );
        h.add( serverLabel );
        h.add( Box.createRigidArea( new Dimension( 10, 0 ) ) );
        h.add( serverField );

        JPanel p = new JPanel();
        p.setLayout( new BoxLayout( p, BoxLayout.LINE_AXIS ) );
        p.setBorder( new EmptyBorder( 10, 10, 10, 10 ) );

        JPanel left = new JPanel();
        left.setLayout( new BoxLayout( left, BoxLayout.PAGE_AXIS ) );
        left.add( area );
        left.add( Box.createRigidArea( new Dimension( 0, 5 ) ) );
        left.add( Box.createHorizontalGlue() );
        left.add( inputText );

        JPanel right = new JPanel();
        right.setLayout( new BoxLayout( right, BoxLayout.PAGE_AXIS ) );
        right.add( loginButton );
        right.add( Box.createRigidArea( new Dimension( 0, 5 ) ) );
        right.add( quitButton );
        right.add( Box.createHorizontalGlue() );
        right.add( Box.createRigidArea( new Dimension( 0, 25 ) ) );
        right.add( closeButton );

        p.add( left );
        p.add( Box.createRigidArea( new Dimension( 10, 0 ) ) );
        p.add( right );

        getContentPane().add( h, BorderLayout.NORTH );
        getContentPane().add( p );

        closeButton.addActionListener( new ActionListener()
        {
            public void actionPerformed( ActionEvent e )
            {
                dispose();
            }
        } );
        setLoggedOut();
        setDefaultCloseOperation( EXIT_ON_CLOSE );
    }
	
	public class LoginAction extends AbstractAction
    {
        public void actionPerformed( ActionEvent e )
        {

            String value = JOptionPane.showInputDialog( SwingChatClient.this,
                    "Specify server address (default: localhost:1234)" ); // "localhost";
            if( value == null || value.length() == 0 )
            {
                value = "localhost:1234";
            }
            SocketAddress address = parseSocketAddress( value );
            String name = JOptionPane.showInputDialog( SwingChatClient.this,
                            "Specify name for chat session with no spaces (default: userN)" );
            if( name == null || name.length() == 0 )
            {
                name = "user" + Math.round( Math.random() * 10 );
            }
            name = name.trim();
            handler = new SwingChatClientHandler( SwingChatClient.this );
            client = new ChatClientSupport( name, handler );
            nameField.setText( name );
            serverField.setText( value );
            try
            {
                client.connect( connector, address );
                area.setText( "" );
            }
            catch( Exception ex )
            {
                JOptionPane.showMessageDialog( SwingChatClient.this,
                        "Could not connect to " + value + ". " );
            }
        }
    }
    
	private class LogoutAction extends AbstractAction
    {
        public void actionPerformed( ActionEvent e )
        {
            try
            {
                client.quit();
                setLoggedOut();
            }
            catch( Exception e1 )
            {
                JOptionPane.showMessageDialog( SwingChatClient.this,
                        "Session could not be closed." );
            }
        }
    }
	
	private class BroadcastAction extends AbstractAction
    {
        public void actionPerformed( ActionEvent e )
        {
            client.broadcast( inputText.getText() );
            inputText.setText( "" );
        }
    }
	
	private class QuitAction extends AbstractAction
    {
        public void actionPerformed( ActionEvent e )
        {
            if( client != null )
            {
                client.quit();
            }
            SwingChatClient.this.dispose();
        }
    }
	
    private void setLoggedOut()
    {
        inputText.setEnabled( false );
        quitButton.setEnabled( false );
        loginButton.setEnabled( true );
    }

    private void setLoggedIn()
    {
        inputText.setEnabled( true );
        quitButton.setEnabled( true );
        loginButton.setEnabled( false );
    }

    private void append( String text )
    {
        area.append( text );
    }

	private void notifyError( String message )
    {
        JOptionPane.showMessageDialog( this, message );
    }

	private SocketAddress parseSocketAddress( String s )
    {
        s = s.trim();
        int colonIndex = s.indexOf( ":" );
        if( colonIndex > 0 )
        {
            String host = s.substring( 0, colonIndex );
            int port = parsePort( s.substring( colonIndex + 1 ) );
            return new InetSocketAddress( host, port );
        }
        else
        {
            int port = parsePort( s.substring( colonIndex + 1 ) );
            return new InetSocketAddress( port );
        }
    }

	private int parsePort( String s )
    {
        try
        {
            return Integer.parseInt( s );
        }
        catch( NumberFormatException nfe )
        {
            throw new IllegalArgumentException( "Illegal port number: " + s );
        }
    }    
	
	public void connected()
    {
        client.login();
    }

    public void disconnected()
    {
        setLoggedOut();
    }

    public void error( String message )
    {
        notifyError( message + "\n" );
    }

    public void loggedIn()
    {
        setLoggedIn();
        append( "You have joined the chat session.\n" );
    }

    public void loggedOut()
    {
        append( "You have left the chat session.\n" );
        setLoggedOut();
    }

    public void messageReceived( String message )
    {
        append( message + "\n" );
    }

    public static void main( String[] args )
    {
        SwingChatClient client = new SwingChatClient();
        client.pack();
        client.setVisible( true );
    }
}
