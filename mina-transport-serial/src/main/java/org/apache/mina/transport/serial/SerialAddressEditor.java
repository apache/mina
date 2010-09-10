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

import java.beans.PropertyEditor;

import org.apache.mina.integration.beans.AbstractPropertyEditor;
import org.apache.mina.transport.serial.SerialAddress.DataBits;
import org.apache.mina.transport.serial.SerialAddress.FlowControl;
import org.apache.mina.transport.serial.SerialAddress.Parity;
import org.apache.mina.transport.serial.SerialAddress.StopBits;

/**
 * A {@link PropertyEditor} which converts a {@link String} into a
 * {@link SerialAddress} and vice versa.  Valid values specify 6 address
 * components separated by colon (e.g. <tt>COM1:9600:7:1:even:rtscts-in</tt>);
 * port name, bauds, data bits, stop bits, parity and flow control respectively.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class SerialAddressEditor extends AbstractPropertyEditor {
    @Override
    protected String toText(Object value) {
        SerialAddress addr = (SerialAddress) value;
        return addr.getName() + ':' +
               addr.getBauds() + ':' +
               toText(addr.getDataBits()) + ':' +
               toText(addr.getStopBits()) + ':' +
               toText(addr.getParity()) + ':' +
               toText(addr.getFlowControl());
    }

    private String toText(DataBits bits) {
        switch (bits) {
        case DATABITS_5:
            return "5";
        case DATABITS_6:
            return "6";
        case DATABITS_7:
            return "7";
        case DATABITS_8:
            return "8";
        default:
            throw new IllegalArgumentException("Unknown dataBits: " + bits);
        }
    }

    private String toText(StopBits bits) {
        switch (bits) {
        case BITS_1:
            return "1";
        case BITS_1_5:
            return "1.5";
        case BITS_2:
            return "2";
        default:
            throw new IllegalArgumentException("Unknown stopBits: " + bits);
        }
    }

    private String toText(Parity parity) {
        switch (parity) {
        case EVEN:
            return "even";
        case ODD:
            return "odd";
        case MARK:
            return "mark";
        case NONE:
            return "none";
        case SPACE:
            return "space";
        default:
            throw new IllegalArgumentException("Unknown parity: " + parity);
        }
    }

    private String toText(FlowControl flowControl) {
        switch (flowControl) {
        case NONE:
            return "none";
        case RTSCTS_IN:
            return "rtscts-in";
        case RTSCTS_OUT:
            return "rtscts-out";
        case XONXOFF_IN:
            return "xonxoff-in";
        case XONXOFF_OUT:
            return "xonxoff-out";
        default:
            throw new IllegalArgumentException("Unknown flowControl: " + flowControl);
        }
    }

    @Override
    protected Object toValue(String text) throws IllegalArgumentException {
        String[] components = text.split(":");
        if (components.length != 6) {
            throw new IllegalArgumentException(
                    "SerialAddress must have 6 components separated " +
                    "by colon: " + text);
        }

        return new SerialAddress(
                components[0].trim(),
                toBauds(components[1].trim()),
                toDataBits(components[2].trim()),
                toStopBits(components[3].trim()),
                toParity(components[4].trim()),
                toFlowControl(components[5].trim()));
    }

    private int toBauds(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("bauds: " + text);
        }
    }

    private DataBits toDataBits(String text) {
        try {
            return DataBits.valueOf("DATABITS_" + Integer.parseInt(text));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("dataBits: " + text);
        }
    }

    private StopBits toStopBits(String text) {
        try {
            return StopBits.valueOf("BITS_" + text.replace('.', '_'));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("stopBits: " + text);
        }
    }

    private Parity toParity(String text) {
        try {
            return Parity.valueOf(text.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("parity: " + text);
        }
    }

    private FlowControl toFlowControl(String text) {
        String normalizedText = text.toUpperCase().replaceAll("(-|_)", "");
        if (normalizedText.endsWith("IN")) {
            normalizedText = normalizedText.substring(0, normalizedText.length() - 2) + "_IN";
        }
        if (normalizedText.endsWith("OUT")) {
            normalizedText = normalizedText.substring(0, normalizedText.length() - 3) + "_OUT";
        }

        try {
            return FlowControl.valueOf(normalizedText);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("flowControl: " + text);
        }
    }
}
