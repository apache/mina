package org.apache.mina.codec.delimited;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import org.apache.mina.util.ByteBufferOutputStream;

import ch.fever.code.mina.gpb.AddressBookProtos.Person;

public class ProtobufTest extends DelimitTest<Person> {
  

    public List<Person> getObjects() {

        List<Person> list = new LinkedList<Person>();

        list.add(Person.newBuilder().setId(1).setName("Jean Dupond").setEmail("john.white@bigcorp.com").build());
        list.add(Person.newBuilder().setId(2).setName("Marie Blanc").setEmail("marie.blanc@bigcorp.com").build());

        return list;
    }

    protected ByteBuffer delimitWithOriginal() throws IOException {
        ByteBufferOutputStream bbos = new ByteBufferOutputStream();
        bbos.setElastic(true);
        for (Person p : getObjects())
            p.writeDelimitedTo(bbos);
        return bbos.getByteBuffer();
    }

    public SizePrefixedEncoder<Person> getSerializer() {
        return ProtobufEncoder.newInstance(Person.class);
    }
}
