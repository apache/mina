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
package org.apache.mina.example.imagine.step3.server;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.example.imagine.step1.ImageRequest;
import org.apache.mina.example.imagine.step1.ImageResponse;
import org.apache.mina.integration.jmx.IoSessionMBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * server-side {@link org.apache.mina.core.service.IoHandler}
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */

public class ImageServerIoHandler extends IoHandlerAdapter {

    private final static String characters = "mina rocks abcdefghijklmnopqrstuvwxyz0123456789";

    public static final String INDEX_KEY = ImageServerIoHandler.class.getName() + ".INDEX";

    private static Logger LOGGER = LoggerFactory.getLogger(ImageServerIoHandler.class);

    private MBeanServer mBeanServer;
    
    /**
     * Creates a new instance of ImageServerIoHandler.  For this step, we pass in a reference
     * to the MBeanServer.  This instance will be used to register new IoSession objects
     * so that the JMX subsystem can report statistics on the sessions.
     *
     * @param mBeanServer
     *  The JMX MBeanServer that will register the sessions
     */
    public ImageServerIoHandler( MBeanServer mBeanServer ) {
        this.mBeanServer = mBeanServer;
    }
    
    /**
     * This method is called first when a new connection to the server is made.  In here we will set
     * up the JMX session MBean.
     * 
     * @see org.apache.mina.core.service.IoHandlerAdapter#sessionCreated(org.apache.mina.core.session.IoSession)
     */
    public void sessionCreated( IoSession session ) throws Exception
    {
        // create a session MBean in order to load into the MBeanServer and allow
        // this session to be managed by the JMX subsystem.
        IoSessionMBean sessionMBean = new IoSessionMBean( session );
        
        // create a JMX ObjectName.  This has to be in a specific format.  
        ObjectName sessionName = new ObjectName( session.getClass().getPackage().getName() + 
            ":type=session,name=" + session.getClass().getSimpleName() + "-" + session.getId());
        
        // register the bean on the MBeanServer.  Without this line, no JMX will happen for
        // this session
        mBeanServer.registerMBean( sessionMBean, sessionName );
    }

    /**
     * Called when the session is opened, which will come after the session created.
     * 
     * @see org.apache.mina.core.service.IoHandlerAdapter#sessionOpened(org.apache.mina.core.session.IoSession)
     */
    public void sessionOpened(IoSession session) throws Exception {
        
        // set the index to zero.  This is used to determine how the build the
        // string that is sent to the client.
        session.setAttribute(INDEX_KEY, 0);
    }

    /**
     * This method will be called whenever an exception occurs.  For this handler,
     * the logger will generate a warning message.
     * 
     * @see org.apache.mina.core.service.IoHandlerAdapter#exceptionCaught(org.apache.mina.core.session.IoSession, java.lang.Throwable)
     */
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        LOGGER.warn(cause.getMessage(), cause);
    }

    /**
     * Handle incoming messages.
     * 
     * @see org.apache.mina.core.service.IoHandlerAdapter#messageReceived(org.apache.mina.core.session.IoSession, java.lang.Object)
     */
    public void messageReceived(IoSession session, Object message) throws Exception {
        ImageRequest request = (ImageRequest) message;
        String text1 = generateString(session, request.getNumberOfCharacters());
        String text2 = generateString(session, request.getNumberOfCharacters());
        BufferedImage image1 = createImage(request, text1);
        BufferedImage image2 = createImage(request, text2);
        ImageResponse response = new ImageResponse(image1, image2);
        session.write(response);
    }

    /**
     * Create an image using the specified request and the text.  
     *
     * @param request
     *  Determines the height and width of the image
     * @param text
     *  The text that is placed in the image
     * @return
     *  a BufferedImage representing the text.
     */
    private BufferedImage createImage(ImageRequest request, String text) {
        BufferedImage image = new BufferedImage(request.getWidth(), request.getHeight(), BufferedImage.TYPE_BYTE_INDEXED);
        Graphics graphics = image.createGraphics();
        graphics.setColor(Color.YELLOW);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        Font serif = new Font("serif", Font.PLAIN, 30);
        graphics.setFont(serif);
        graphics.setColor(Color.BLUE);
        graphics.drawString(text, 10, 50);
        return image;
    }

    /**
     * Generate a string based on the 'characters' field in this class.  The
     * characters that make up the string are based on the session 
     * attribute "INDEX_KEY"
     *
     * @param session
     *  The {@link IoSession} object that will provide the INDEX_KEY attribute
     * @param length
     *  The length that the String will be
     * @return
     *  The generated String
     */
    private String generateString(IoSession session, int length) {
        Integer index = (Integer) session.getAttribute(INDEX_KEY);
        StringBuilder buffer = new StringBuilder(length);
        while (buffer.length() < length) {
            buffer.append(characters.charAt(index));
            index++;
            if (index >= characters.length()) {
                index = 0;
            }
        }
        session.setAttribute(INDEX_KEY, index);
        return buffer.toString();
    }

}
