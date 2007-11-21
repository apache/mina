package org.apache.mina.example.imagine.step1.client;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;

/**
 * @author Maarten
 */
public class ImagePanel extends JPanel {

    private BufferedImage image1;
    private BufferedImage image2;

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
