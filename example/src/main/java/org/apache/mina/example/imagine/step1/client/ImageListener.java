package org.apache.mina.example.imagine.step1.client;

import java.awt.image.BufferedImage;

public interface ImageListener {
    void onImages(BufferedImage image1, BufferedImage image2);

    void onException(Throwable throwable);

    void sessionOpened();

    void sessionClosed();
}
