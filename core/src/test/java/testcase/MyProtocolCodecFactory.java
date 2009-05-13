package testcase;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;

public class MyProtocolCodecFactory implements ProtocolCodecFactory {
  private ProtocolDecoder decoder = new MyRequestDecoder();
  private ProtocolEncoder encoder = new MyResponseEncoder();

  public ProtocolDecoder getDecoder(IoSession sessionIn) throws Exception {
    return decoder;
  }

  public ProtocolEncoder getEncoder(IoSession sessionIn) throws Exception {
    return encoder;
  }
}
