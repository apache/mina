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

package org.apache.mina.filter.errorgenerating;

import java.util.Random;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.DefaultWriteRequest;
import org.apache.mina.core.write.WriteRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link IoFilter} implementation generating random bytes and PDU modification in
 * your communication streams.
 * It's quite simple to use :
 * <code>ErrorGeneratingFilter egf = new ErrorGeneratingFilter();</code>
 * For activate the change of some bytes in your {@link IoBuffer}, for a probability of 200 out
 * of 1000 {@link IoBuffer} processed :
 * <code>egf.setChangeByteProbability(200);</code>
 * For activate the insertion of some bytes in your {@link IoBuffer}, for a
 * probability of 200 out of 1000 :
 * <code>egf.setInsertByteProbability(200);</code>
 * And for the removing of some bytes :
 * <code>egf.setRemoveByteProbability(200);</code>
 * You can activate the error generation for write or read with the
 * following methods :
 * <code>egf.setManipulateReads(true);
 * egf.setManipulateWrites(true); </code>
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 * @org.apache.xbean.XBean
 */
public class ErrorGeneratingFilter extends IoFilterAdapter {
    private int removeByteProbability = 0;

    private int insertByteProbability = 0;

    private int changeByteProbability = 0;

    private int removePduProbability = 0;

    private int duplicatePduProbability = 0;

    private int resendPduLasterProbability = 0;

    private int maxInsertByte = 10;

    private boolean manipulateWrites = false;

    private boolean manipulateReads = false;

    private Random rng = new Random();

    final private Logger logger = LoggerFactory
            .getLogger(ErrorGeneratingFilter.class);

    @Override
    public void filterWrite(NextFilter nextFilter, IoSession session,
            WriteRequest writeRequest) throws Exception {
        if (manipulateWrites) {
            // manipulate bytes
            if (writeRequest.getMessage() instanceof IoBuffer) {
                manipulateIoBuffer(session, (IoBuffer) writeRequest
                        .getMessage());
                IoBuffer buffer = insertBytesToNewIoBuffer(session,
                        (IoBuffer) writeRequest.getMessage());
                if (buffer != null) {
                    writeRequest = new DefaultWriteRequest(buffer, writeRequest
                            .getFuture(), writeRequest.getDestination());
                }
                // manipulate PDU
            } else {
                if (duplicatePduProbability > rng.nextInt()) {
                    nextFilter.filterWrite(session, writeRequest);
                }
                
                if (resendPduLasterProbability > rng.nextInt()) {
                    // store it somewhere and trigger a write execution for
                    // later
                    // TODO
                }
                if (removePduProbability > rng.nextInt()) {
                    return;
                }
            }
        }
        nextFilter.filterWrite(session, writeRequest);
    }

    @Override
    public void messageReceived(NextFilter nextFilter, IoSession session,
            Object message) throws Exception {
        if (manipulateReads) {
            if (message instanceof IoBuffer) {
                // manipulate bytes
                manipulateIoBuffer(session, (IoBuffer) message);
                IoBuffer buffer = insertBytesToNewIoBuffer(session,
                        (IoBuffer) message);
                if (buffer != null) {
                    message = buffer;
                }
            } else {
                // manipulate PDU
                // TODO
            }
        }
        nextFilter.messageReceived(session, message);
    }

    private IoBuffer insertBytesToNewIoBuffer(IoSession session, IoBuffer buffer) {
        if (insertByteProbability > rng.nextInt(1000)) {
            logger.info(buffer.getHexDump());
            // where to insert bytes ?
            int pos = rng.nextInt(buffer.remaining()) - 1;

            // how many byte to insert ?
            int count = rng.nextInt(maxInsertByte-1)+1;

            IoBuffer newBuff = IoBuffer.allocate(buffer.remaining() + count);
            for (int i = 0; i < pos; i++)
                newBuff.put(buffer.get());
            for (int i = 0; i < count; i++) {
                newBuff.put((byte) (rng.nextInt(256)));
            }
            while (buffer.remaining() > 0) {
                newBuff.put(buffer.get());
            }
            newBuff.flip();

            logger.info("Inserted " + count + " bytes.");
            logger.info(newBuff.getHexDump());
            return newBuff;
        }
        return null;
    }

    private void manipulateIoBuffer(IoSession session, IoBuffer buffer) {
        if ((buffer.remaining() > 0) && (removeByteProbability > rng.nextInt(1000))) {
            logger.info(buffer.getHexDump());
            // where to remove bytes ?
            int pos = rng.nextInt(buffer.remaining());
            // how many byte to remove ?
            int count = rng.nextInt(buffer.remaining() - pos) + 1;
            if (count == buffer.remaining())
                count = buffer.remaining() - 1;

            IoBuffer newBuff = IoBuffer.allocate(buffer.remaining() - count);
            for (int i = 0; i < pos; i++)
                newBuff.put(buffer.get());

            buffer.skip(count); // hole
            while (newBuff.remaining() > 0)
                newBuff.put(buffer.get());
            newBuff.flip();
            // copy the new buffer in the old one
            buffer.rewind();
            buffer.put(newBuff);
            buffer.flip();
            logger.info("Removed " + count + " bytes at position " + pos + ".");
            logger.info(buffer.getHexDump());
        }
        if ((buffer.remaining() > 0) && (changeByteProbability > rng.nextInt(1000))) {
            logger.info(buffer.getHexDump());
            // how many byte to change ?
            int count = rng.nextInt(buffer.remaining() - 1) + 1;

            byte[] values = new byte[count];
            rng.nextBytes(values);
            for (int i = 0; i < values.length; i++) {
                int pos = rng.nextInt(buffer.remaining());
                buffer.put(pos, values[i]);
            }
            logger.info("Modified " + count + " bytes.");
            logger.info(buffer.getHexDump());
        }
    }

    public int getChangeByteProbability() {
        return changeByteProbability;
    }
    
    /**
     * Set the probability for the change byte error.
     * If this probability is > 0 the filter will modify a random number of byte
     * of the processed {@link IoBuffer}.
     * @param changeByteProbability probability of modifying an IoBuffer out of 1000 processed {@link IoBuffer} 
     */
    public void setChangeByteProbability(int changeByteProbability) {
        this.changeByteProbability = changeByteProbability;
    }

    public int getDuplicatePduProbability() {
        return duplicatePduProbability;
    }
    
    /**
     * not functional ATM
     * @param duplicatePduProbability
     */
    public void setDuplicatePduProbability(int duplicatePduProbability) {
        this.duplicatePduProbability = duplicatePduProbability;
    }

    public int getInsertByteProbability() {
        return insertByteProbability;
    }

    /**
     * Set the probability for the insert byte error.
     * If this probability is > 0 the filter will insert a random number of byte
     * in the processed {@link IoBuffer}.
     * @param changeByteProbability probability of inserting in IoBuffer out of 1000 processed {@link IoBuffer} 
     */
    public void setInsertByteProbability(int insertByteProbability) {
        this.insertByteProbability = insertByteProbability;
    }

    public boolean isManipulateReads() {
        return manipulateReads;
    }

    /**
     * Set to true if you want to apply error to the read {@link IoBuffer}
     * @param manipulateReads
     */
    public void setManipulateReads(boolean manipulateReads) {
        this.manipulateReads = manipulateReads;
    }

    public boolean isManipulateWrites() {
        return manipulateWrites;
    }

    /**
     * Set to true if you want to apply error to the written {@link IoBuffer}
     * @param manipulateWrites
     */
    public void setManipulateWrites(boolean manipulateWrites) {
        this.manipulateWrites = manipulateWrites;
    }

    public int getRemoveByteProbability() {
        return removeByteProbability;
    }

    /**
     * Set the probability for the remove byte error.
     * If this probability is > 0 the filter will remove a random number of byte
     * in the processed {@link IoBuffer}.
     * @param changeByteProbability probability of modifying an {@link IoBuffer} out of 1000 processed IoBuffer 
     */
    public void setRemoveByteProbability(int removeByteProbability) {
        this.removeByteProbability = removeByteProbability;
    }

    public int getRemovePduProbability() {
        return removePduProbability;
    }

    /**
     * not functional ATM
     * @param removePduProbability
     */
    public void setRemovePduProbability(int removePduProbability) {
        this.removePduProbability = removePduProbability;
    }

    public int getResendPduLasterProbability() {
        return resendPduLasterProbability;
    }
    /**
     * not functional ATM
     * @param resendPduLasterProbability
     */
    public void setResendPduLasterProbability(int resendPduLasterProbability) {
        this.resendPduLasterProbability = resendPduLasterProbability;
    }

    public int getMaxInsertByte() {
        return maxInsertByte;
    }

    /**
     * Set the maximum number of byte the filter can insert in a {@link IoBuffer}.
     * The default value is 10.
     * @param maxInsertByte maximum bytes inserted in a {@link IoBuffer} 
     */
    public void setMaxInsertByte(int maxInsertByte) {
        this.maxInsertByte = maxInsertByte;
    }
}