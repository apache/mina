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

import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

/**
 * JPanel capable of drawing two {@link BufferedImage}'s
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class ImagePanel extends JPanel {

    private static final long serialVersionUID = 1L;
    
    private BufferedImage image1;
    private BufferedImage image2;

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (image1 != null) {
            g.drawImage(image1, 20, 20, null);
            if (image2 != null) {
                g.drawImage(image2, 20, image1.getHeight() + 80, null);
            }
        }
    }

    public void setImages(BufferedImage image1, BufferedImage image2) {
        this.image1 = image1;
        this.image2 = image2;
        repaint();
    }

}
