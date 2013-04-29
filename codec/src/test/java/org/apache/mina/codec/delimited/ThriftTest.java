package org.apache.mina.codec.delimited;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import org.apache.thrift.TSerializer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TMemoryBuffer;

import ch.fever.code.mina.thrift.UserProfile;

public class ThriftTest extends DelimitTest<UserProfile> {
  

    public List<UserProfile> getObjects() {

        List<UserProfile> list = new LinkedList<UserProfile>();

        list.add(new UserProfile().setUid(1).setName("Jean Dupond"));
        list.add(new UserProfile().setUid(2).setName("Marie Blanc"));

        return list;
    }

    protected ByteBuffer delimitWithOriginal() throws Exception {

        TMemoryBuffer m = new TMemoryBuffer(1000000);
        TFramedTransport t = new TFramedTransport(m);
        TSerializer tt = new TSerializer();
        for (UserProfile up : getObjects()) {
            t.write(tt.serialize(up));
            t.flush();
        }
        return ByteBuffer.wrap(m.getArray(), 0, m.length());
        
    }

    public SizePrefixedEncoder<UserProfile> getSerializer() throws SecurityException, NoSuchMethodException {
        return ThriftEncoder.newInstance(UserProfile.class);
    }

}
