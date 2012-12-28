package org.apache.mina.transport.nio;

import java.nio.channels.SelectionKey;

public interface RegistrationCallback {

    void done(SelectionKey selectionKey);
}
