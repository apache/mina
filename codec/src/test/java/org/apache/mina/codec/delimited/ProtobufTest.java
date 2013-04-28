package org.apache.mina.codec.delimited;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import org.apache.mina.util.ByteBufferOutputStream;
import org.apache.thrift.transport.TFastFramedTransport;
import org.junit.Test;

import ch.fever.code.mina.gpb.AddressBookProtos.Person;

public class ProtobufTest {
    public void t() {
        TFastFramedTransport t;
    }

    public List<Person> getObjects() {

        List<Person> list = new LinkedList<Person>();

        list.add(Person.newBuilder().setId(1).setName("Jean Dupond").setEmail("john.white@bigcorp.com").build());
        list.add(Person.newBuilder().setId(2).setName("Marie Blanc").setEmail("marie.blanc@bigcorp.com").build());

        return list;
    }

    protected ByteBuffer delimitWithProtobuf() throws IOException {
        ByteBufferOutputStream bbos = new ByteBufferOutputStream();
        bbos.setElastic(true);
        for (Person p : getObjects())
            p.writeDelimitedTo(bbos);
        return bbos.getByteBuffer();
    }

    protected ByteBuffer delimitWithMina() {
        ProtobufEncoder<Person> pe = ProtobufEncoder.newInstance(Person.class);

        List<ByteBuffer> buffers = new LinkedList<ByteBuffer>();
        for (Person p : getObjects())
            buffers.add(pe.encode(p, null));

        int size = 0;
        for (ByteBuffer b : buffers)
            size += b.remaining();

        ByteBuffer buffer = ByteBuffer.allocate(size);
        for (ByteBuffer b : buffers)
            buffer.put(b);
        buffer.flip();
        return buffer;
    }

    @Test
    public void testDelimit() throws IOException {
        assertEquals(delimitWithProtobuf(), delimitWithMina());
    }
}
