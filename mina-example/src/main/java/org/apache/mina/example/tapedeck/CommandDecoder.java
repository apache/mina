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
package org.apache.mina.example.tapedeck;

import java.nio.charset.Charset;
import java.util.LinkedList;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.textline.LineDelimiter;
import org.apache.mina.filter.codec.textline.TextLineDecoder;

/**
 * MINA {@link ProtocolDecoder} which decodes bytes into {@link Command}
 * objects.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class CommandDecoder extends TextLineDecoder {
    
    public CommandDecoder() {
        super(Charset.forName("UTF8"), LineDelimiter.WINDOWS);
    }
    
    private Object parseCommand(String line) throws CommandSyntaxException {
        String[] temp = line.split(" +", 2);
        String cmd = temp[0].toLowerCase();
        String arg = temp.length > 1 ? temp[1] : null;
        
        if (LoadCommand.NAME.equals(cmd)) {
            if (arg == null) {
                throw new CommandSyntaxException("No tape number specified");
            }
            try {
                return new LoadCommand(Integer.parseInt(arg));
            } catch (NumberFormatException nfe) {
                throw new CommandSyntaxException("Illegal tape number: " + arg);
            }
        } else if (PlayCommand.NAME.equals(cmd)) {
            return new PlayCommand();
        } else if (PauseCommand.NAME.equals(cmd)) {
            return new PauseCommand();
        } else if (StopCommand.NAME.equals(cmd)) {
            return new StopCommand();
        } else if (ListCommand.NAME.equals(cmd)) {
            return new ListCommand();
        } else if (EjectCommand.NAME.equals(cmd)) {
            return new EjectCommand();
        } else if (QuitCommand.NAME.equals(cmd)) {
            return new QuitCommand();
        } else if (InfoCommand.NAME.equals(cmd)) {
            return new InfoCommand();
        } else if (UserCommand.NAME.equals(cmd)) {
            if (arg == null) {
                throw new CommandSyntaxException("No username specified");
            }
            return new UserCommand(arg);
        } else if (PasswordCommand.NAME.equals(cmd)) {
            if (arg == null) {
                throw new CommandSyntaxException("No password specified");
            }
            return new PasswordCommand(arg);
        }
        
        throw new CommandSyntaxException("Unknown command: " + cmd);
    }

    @Override
    public void decode(IoSession session, IoBuffer in, final ProtocolDecoderOutput out) 
            throws Exception {
        
        final LinkedList<String> lines = new LinkedList<String>();
        super.decode(session, in, new ProtocolDecoderOutput() {
            public void write(Object message) {
                lines.add((String) message);
            }
            public void flush(NextFilter nextFilter, IoSession session) {}
        });
        
        for (String s: lines) {
            out.write(parseCommand(s));
        }
    }

}