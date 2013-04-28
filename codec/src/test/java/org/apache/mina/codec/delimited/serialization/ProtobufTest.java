package org.apache.mina.codec.delimited.serialization;


import static org.junit.Assert.assertEquals;

import java.util.LinkedList;
import java.util.List;

import org.apache.mina.codec.delimited.ByteBufferDecoder;
import org.apache.mina.codec.delimited.ByteBufferEncoder;
import org.apache.mina.codec.delimited.serialization.ProtobufDynamicDecoder.SerializedMessage;
import org.junit.Test;

import com.google.protobuf.DescriptorProtos.DescriptorProto;


public class ProtobufTest extends GenericSerializerTest<DescriptorProto, DescriptorProto> {

    @Override
    public List<DescriptorProto> getObjects() {
        List<DescriptorProto> list = new LinkedList<DescriptorProto>();
        list.add(DescriptorProto.newBuilder().setName("HELLO").build());
        list.add(DescriptorProto.newBuilder().setName("WORLD").build());
        return list;
    }

    @Override
    public ByteBufferDecoder<DescriptorProto> getDecoder() throws Exception {
        return ProtobufDecoder.newInstance(DescriptorProto.class);
    }

    @Override
    public ByteBufferEncoder<DescriptorProto> getEncoder() throws Exception {
        return new ProtobufEncoder<DescriptorProto>();
    }

    @Test
    public void testDynamic() throws Exception {
        ByteBufferEncoder<DescriptorProto> encoder = getEncoder();
        ProtobufDynamicDecoder decoder = new ProtobufDynamicDecoder();

        for (DescriptorProto object : getObjects()) {
            SerializedMessage message=decoder.decode(encoder.encode(object));
            assertEquals(object,message.get(DescriptorProto.class));
        }
    }
}
