package org.apache.mina.codec.delimited.serialization;

import static org.junit.Assert.assertEquals;

import java.util.LinkedList;
import java.util.List;

import org.apache.mina.codec.delimited.ByteBufferDecoder;
import org.apache.mina.codec.delimited.ByteBufferEncoder;
import org.junit.Test;

import ch.fever.code.mina.thrift.UserProfile;

public class ThriftTest extends GenericSerializerTest<UserProfile> {

    @Override
    public List<UserProfile> getObjects() {
        List<UserProfile> list = new LinkedList<UserProfile>();

        list.add(new UserProfile().setUid(1).setName("Jean Dupond"));
        list.add(new UserProfile().setUid(2).setName("Marie Blanc"));

        return list;
    }

    @Override
    public ByteBufferDecoder<UserProfile> getDecoder() throws Exception {
        return ThriftMessageDecoder.newInstance(UserProfile.class);
    }

    @Override
    public ByteBufferEncoder<UserProfile> getEncoder() throws Exception {
        return ThriftMessageEncoder.newInstance(UserProfile.class);
    }

    @Test
    public void testDynamic() throws Exception {
        ByteBufferEncoder<UserProfile> encoder = getEncoder();
        ThriftDynamicMessageDecoder decoder = new ThriftDynamicMessageDecoder();

        for (UserProfile object : getObjects()) {
            ThriftDynamicMessageDecoder.ThriftSerializedMessage message = decoder.decode(encoder.encode(object));
            assertEquals(object, message.get(UserProfile.class));
        }
    }
}
