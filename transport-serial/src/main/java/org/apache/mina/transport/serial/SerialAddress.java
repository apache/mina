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

package org.apache.mina.transport.serial;

import gnu.io.SerialPort;

import java.net.SocketAddress;
import java.security.InvalidParameterException;

/**
 * An address for a serial port communication.
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev: 529576 $, $Date: 2007-04-17 14:25:07 +0200 (mar., 17 avr. 2007) $
 */
public class SerialAddress extends SocketAddress {

    private static final long serialVersionUID = 1735370510442384505L;

    public enum DataBits {
        DATABITS_5, DATABITS_6, DATABITS_7, DATABITS_8
    }

    public enum Parity {
        NONE, ODD, EVEN, MARK, SPACE
    }

    public enum StopBits {
        BITS_1, BITS_2, BITS_1_5
    }

    public enum FlowControl {
        NONE, RTSCTS_IN, RTSCTS_OUT, XONXOFF_IN, XONXOFF_OUT
    }

    private String name;

    private int bauds;

    private int dataBits;

    private StopBits stopBits;

    private Parity parity;

    private FlowControl flowControl;

    /**
     * Create an address for a serial communication, associating a serial interface and
     * various serial signal carcteristics.
     * @param name name of the device, COM1 COM2 for Windows, /dev/ttyS0 for Unix
     * @param bauds baud rate for the communication
     * @param dataBits number of data bits per bytes
     * @param stopBits number of stop bits
     * @param parity parity used
     * @param flowControl flow control used
     */
    public SerialAddress(String name, int bauds, int dataBits,
            StopBits stopBits, Parity parity, FlowControl flowControl) {
        super();
        this.name = name;
        this.bauds = bauds;
        this.dataBits = dataBits;
        this.stopBits = stopBits;
        this.parity = parity;
        this.flowControl = flowControl;
    }

    /**
     * Bauds rate for the communication.
     * @return the bauds (bits per seconds) for this serial link
     */
    public int getBauds() {
        return bauds;
    }

    /**
     * Number of data bits for each communicated bytes.
     * @return the data bits
     */
    public int getDataBits() {
        return dataBits;
    }

    /**
     * The flow control policie used for this communication.
     * @return the flow control
     */
    public FlowControl getFlowControl() {
        return flowControl;
    }

    /**
     * The name of the device. Can be COM1, COM2, /dev/ttyS0, /dev/ttyUSB1, etc..
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * The parity check for this communication.
     * @return parity type
     */
    public Parity getParity() {
        return parity;
    }

    /**
     * Number of stop bits used.
     * @return stop bits number
     */
    public StopBits getStopBits() {
        return stopBits;
    }

    /**
     * Convert this serial address to a human readable string.
     */
    @Override
    public String toString() {
        return "serial(" + name + ",bauds:" + bauds + ",databits:" + dataBits
                + ",stopbits:" + stopBits + ",parity:" + parity
                + ",flowcontrol:" + flowControl + ")";
    }

    int getDataBitsForRXTX() {
        switch (dataBits) {
        case 5:
            return SerialPort.DATABITS_5;
        case 6:
            return SerialPort.DATABITS_6;
        case 7:
            return SerialPort.DATABITS_7;
        case 8:
            return SerialPort.DATABITS_8;
        }
        throw new InvalidParameterException("broken databits");
    }

    int getStopBitsForRXTX() {
        switch (stopBits) {
        case BITS_1:
            return SerialPort.STOPBITS_1;
        case BITS_1_5:
            return SerialPort.STOPBITS_1_5;
        case BITS_2:
            return SerialPort.STOPBITS_2;
        }
        throw new InvalidParameterException("broken stopbits");
    }

    int getParityForRXTX() {
        switch (parity) {
        case EVEN:
            return SerialPort.PARITY_EVEN;
        case MARK:
            return SerialPort.PARITY_MARK;
        case NONE:
            return SerialPort.PARITY_NONE;
        case ODD:
            return SerialPort.PARITY_ODD;
        case SPACE:
            return SerialPort.PARITY_SPACE;
        }
        throw new InvalidParameterException("broken parity");
    }

    int getFLowControlForRXTX() {
        switch (flowControl) {
        case NONE:
            return SerialPort.FLOWCONTROL_NONE;
        case RTSCTS_IN:
            return SerialPort.FLOWCONTROL_RTSCTS_IN;
        case RTSCTS_OUT:
            return SerialPort.FLOWCONTROL_RTSCTS_OUT;
        case XONXOFF_IN:
            return SerialPort.FLOWCONTROL_XONXOFF_IN;
        case XONXOFF_OUT:
            return SerialPort.FLOWCONTROL_XONXOFF_OUT;
        }
        throw new InvalidParameterException("broken stopbits");
    }
}