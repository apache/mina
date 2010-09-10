/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package testcase;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static testcase.MinaRegressionTest.MSG_SIZE;
import static testcase.MinaRegressionTest.OPEN;

/**
 * TODO: Document me !
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class MyRequestDecoder extends CumulativeProtocolDecoder {
  private static final Logger logger = LoggerFactory.getLogger(MyRequestDecoder.class);

  @Override
  protected boolean doDecode(final IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
    if (!session.containsAttribute(OPEN)) {
      logger.error("!decoding for closed session {}", session.getId());
    }

    new Thread(new Runnable() {
      public void run() {
        try {
            logger.debug( "Sleep for 500 ms for session {}", session.getId() );
            Thread.sleep(500);
            logger.debug( "Wake up now from a 500 ms sleep for session {}", session.getId() );
        } catch (InterruptedException ignore) {}
        session.close(true);
      }
    }).start();

    // sleep so that session.close(true) is already called when decoding continues
    logger.debug( "Sleep for 1000 ms for session {}", session.getId() );
    Thread.sleep(1000);
    logger.debug( "Wake up now from a 1000 ms sleep for session {}", session.getId() );

    if (!session.containsAttribute(OPEN)) {
      logger.error("!session {} closed before decoding completes!", session.getId());
      int i = 0;
      
      try
      {
          int j = 2 / i;
      } 
      catch ( Exception e )
      {
          //e.printStackTrace();
      }
    }

    // no full message
    if (in.remaining() < MSG_SIZE) return false;

    logger.info("Done decoding for session {}", session.getId());

    if (in.hasRemaining() && !session.isClosing() && session.isConnected()) {
      IoBuffer tmp = IoBuffer.allocate(in.remaining());
      tmp.put(in);
      tmp.flip();
      out.write(tmp);
      return true;
    }

    return false;
  }
}
