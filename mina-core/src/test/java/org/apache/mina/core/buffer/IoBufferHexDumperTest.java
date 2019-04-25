package org.apache.mina.core.buffer;

import static org.junit.Assert.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class IoBufferHexDumperTest {

    @Test
    public void checkHexDumpLength() {
	IoBuffer buf = IoBuffer.allocate(5000);

	for (int i = 0; i < 20; i++) {
	    buf.putShort((short) 0xF0A0);
	}

	buf.flip();

//	System.out.println(buf.getHexDump());
//	System.out.println(buf.getHexDump(20));
//	System.out.println(buf.getHexDump(50));

	/* special case */
	assertEquals(0, buf.getHexDump(0).length());
	
	/* no truncate needed */
	assertEquals(buf.limit() * 3 - 1, buf.getHexDump().length());
	assertEquals((Math.min(300, buf.limit()) * 3) - 1, buf.getHexDump(300).length());
	
	/* must truncate */
	assertEquals((7 * 3) + 2, buf.getHexDump(7).length());
	assertEquals((10 * 3) + 2, buf.getHexDump(10).length());
	assertEquals((30 * 3) + 2, buf.getHexDump(30).length());
	
    }
}
