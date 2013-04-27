package org.apache.mina.codec.delimited.serialization;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

import org.apache.mina.codec.ProtocolDecoderException;
import org.apache.mina.codec.delimited.Transcoder;
import org.apache.mina.util.ByteBufferInputStream;

import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.ExtensionRegistryLite;
import com.google.protobuf.GeneratedMessage;

public class ProtobufTranscoder<TYPE extends GeneratedMessage> extends Transcoder<TYPE,TYPE> {

    final private Method parseMethod;

    final private ExtensionRegistry registry;

    public static <TYPE extends GeneratedMessage> ProtobufTranscoder<TYPE> newInstance(Class<TYPE> c)
            throws SecurityException, NoSuchMethodException {
        return newInstance(c, ExtensionRegistry.getEmptyRegistry());
    }

    public static <TYPE extends GeneratedMessage> ProtobufTranscoder<TYPE> newInstance(Class<TYPE> c,
            ExtensionRegistry registry) throws SecurityException, NoSuchMethodException {
        return new ProtobufTranscoder<TYPE>(c, registry);
    }

    private ProtobufTranscoder(Class<TYPE> clazz, ExtensionRegistry registry) throws SecurityException,
            NoSuchMethodException {
        super();
        parseMethod = clazz.getDeclaredMethod("parseFrom", InputStream.class, ExtensionRegistryLite.class);
        this.registry = registry;
    }

    @SuppressWarnings("unchecked")
    @Override
    public TYPE decode(ByteBuffer input) throws ProtocolDecoderException {
        try {
            return (TYPE) parseMethod.invoke(null, new ByteBufferInputStream(input), registry);
        } catch (Exception e) {
            throw new ProtocolDecoderException(e);
        }
    }

    @Override
    public int getEncodedSize(TYPE message) {
        return message.getSerializedSize();
    }

    @Override
    public void writeTo(TYPE message, ByteBuffer buffer) {
        buffer.put(message.toByteArray());
    }
}
