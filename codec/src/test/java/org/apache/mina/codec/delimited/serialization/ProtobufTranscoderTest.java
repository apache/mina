package org.apache.mina.codec.delimited.serialization;

import java.util.LinkedList;
import java.util.List;

import org.apache.mina.codec.delimited.Transcoder;

import com.google.protobuf.DescriptorProtos.DescriptorProto;

public class ProtobufTranscoderTest extends TranscoderTest<DescriptorProto,DescriptorProto> {

    @Override
    public Transcoder<DescriptorProto,DescriptorProto> getTranscoderInstance() throws SecurityException, NoSuchMethodException {
        return ProtobufTranscoder.newInstance(DescriptorProto.class);
    }

    @Override
    public List<DescriptorProto> getObjects() {
        List<DescriptorProto> list = new LinkedList<DescriptorProto>();
        list.add(DescriptorProto.newBuilder().setName("HELLO").build());
        list.add(DescriptorProto.newBuilder().setName("WORLD").build());
        return list;
    }

}
