package org.apache.mina.example.chat;

import junit.framework.TestCase;
import org.springframework.context.ConfigurableApplicationContext;
import org.apache.mina.common.IoService;

/**
 */
public class SpringMainTest extends TestCase {

    private ConfigurableApplicationContext appContext;

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (appContext != null) {
            appContext.close();
        }
    }

    public void testContext() {
        appContext = SpringMain.getApplicationContext();
        IoService service = (IoService) appContext.getBean("ioAcceptor");
        IoService ioAcceptorWithSSL = (IoService) appContext.getBean("ioAcceptorWithSSL");
        assertTrue(service.isActive());
        assertTrue(ioAcceptorWithSSL.isActive());
        appContext.close();
        assertFalse(service.isActive());
        assertFalse(ioAcceptorWithSSL.isActive());
    }
}
