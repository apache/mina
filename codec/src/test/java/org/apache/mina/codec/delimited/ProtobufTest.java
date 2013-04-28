package org.apache.mina.codec.delimited;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import org.apache.mina.util.ByteBufferOutputStream;
import org.junit.Test;

import ch.fever.code.mina.gpb.AddressBookProtos.Person;

public class ProtobufTest {
    public List<Person> getObjects() {
        List<Person> list = new LinkedList<Person>();

        list.add(Person.newBuilder().setId(1).setName("Jean Dupond").setEmail("john.white@bigcorp.com").build());
        list.add(Person.newBuilder().setId(2).setName("Marie Blanc").setEmail("marie.blanc@bigcorp.com").build());

        return list;
    }

    @Test
    public void test() throws IOException {
        ByteBufferOutputStream bbos = new ByteBufferOutputStream();
        bbos.setElastic(true);
        for (Person p : getObjects())
            p.writeDelimitedTo(bbos);
        ProtobufEncoder<Person> pe = ProtobufEncoder.newInstance(Person.class);
        //        pe.encode(message, context)
//        for (Person p : getObjects())
//            pe.encode(message, context)
//            List<ByteBuffer> buffers = new LinkedList<ByteBuffer>();
    }
}
