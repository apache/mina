package org.apache.mina.coap.retry;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;

/**
 * Unit test for {@link ExpiringMap}
 */
public class ExpiringMapTest {

    @Test
    public void put_get() {
        Map<String, String> map = new ExpiringMap<>();
        map.put("key1", "value1");

        assertTrue(map.containsKey("key1"));
        assertEquals("value1", map.get("key1"));

        assertFalse(map.containsKey("key2"));
        assertNull(map.get("key2"));
    }

    @Test
    public void size() {
        Map<String, String> map = new ExpiringMap<>();
        map.put("key1", "value1");
        map.put("key2", "value2");

        assertEquals(2, map.size());
    }

    @Test
    public void remove() {
        Map<String, String> map = new ExpiringMap<>();
        map.put("key1", "value1");
        map.put("key2", "value2");

        String val = map.remove("key2");
        assertEquals("value2", val);

        assertEquals(1, map.size());
        assertTrue(map.containsKey("key1"));
    }

    @Test
    public void expiring_element() throws InterruptedException {
        ExpiringMap<String, String> map = new ExpiringMap<>(5, 1);
        map.start();

        map.put("key1", "value1");

        assertEquals(1, map.size());

        // check before expiration
        Thread.sleep(4000L);

        assertEquals(1, map.size());

        // check after expiration
        Thread.sleep(3000L);

        assertEquals(0, map.size());
    }

}
