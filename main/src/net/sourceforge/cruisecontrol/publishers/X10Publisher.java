/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2004, ThoughtWorks, Inc.
 * 200 E. Randolph, 25th Floor
 * Chicago, IL 60601 USA
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *     + Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     + Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 *     + Neither the name of ThoughtWorks, Inc., CruiseControl, nor the
 *       names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior
 *       written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/
package net.sourceforge.cruisecontrol.publishers;

import com.jpeterson.x10.Gateway;
import com.jpeterson.x10.GatewayException;
import com.jpeterson.x10.SerialGateway;
import com.jpeterson.x10.Transmitter;
import com.jpeterson.x10.event.AddressEvent;
import com.jpeterson.x10.event.OffEvent;
import com.jpeterson.x10.event.OnEvent;
import com.jpeterson.x10.event.X10Event;
import com.jpeterson.x10.module.CM11A;
import com.jpeterson.x10.module.CM17A;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Publisher;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import net.sourceforge.cruisecontrol.util.XMLLogHelper;
import org.apache.log4j.Logger;
import org.jdom.Element;

import java.io.IOException;

/**
 * This Publisher implementation sends a on/off signal to a X10 capable device
 * via the X10 Activehome computer interface, model CM11A. This allows you to
 * control an electronic device when the build breaks. For example, use a
 * flashing red light to indicate a broken build.
 * <p>
 * NOTE: THIS PUBLISHER HAS ONLY BEEN TESTED WITH WINDOWS
 * <p>
 * Quick Start:
 * <ol>
 * <li>Buy the home automation kit found at
 * <a href="http://www.x10.com/automation/x10_ck11a_1.htm">http://www.x10.com/automation/x10_ck11a_1.htm</a></li>
 * <li>Plug the computer interface to your serial port, e.g. COM1, and your powerline</li>
 * <li>Set the lamp module's house and device code, e.g. A3, and plug it into your powerline</li>
 * <li>Plug in an electronic device to the lamp module, e.g. a flashing red light like
 * <a href="http://www.bwild.com/redsiren.html">http://www.bwild.com/redsiren.html</a></li>
 * <li>Install the Java Communications API on your CruiseControl machine by copying the win32com.dll from the
 * CruiseControl lib directory to your <code>JAVA_HOME/bin</code> directory</li>
 * <li>Add the x10 publisher to CruiseControl's config.xml, e.g.
 * <code>&lt;x10 houseCode="A" deviceCode="3" port="COM1"/&gt;</code></li>
 * </ol>
 * <p>
 * For more information about the controller, see
 * <a href="http://www.smarthome.com/1140.html">http://www.smarthome.com/1140.html</a>
 * or
 * <a href="http://www.x10.com/automation/x10_ck11a_1.htm">http://www.x10.com/automation/x10_ck11a_1.htm</a>
 * The controller connects to the computer via a serial port, e.g. COM1, and
 * allows the computer to send (and receive) X10 signals on the power line. For
 * more information on X10 in general, see
 * <a href="http://www.x10.com/support/basicx10.htm">http://www.x10.com/support/basicx10.htm</a>.
 * <p>
 * This module uses a pure Java implementation of the CM11A communication
 * protocol as implemented by Jesse Peterson,
 * <a href="http://www.jpeterson.com/">http://www.jpeterson.com/</a>. To read
 * more about his library, see
 * <a href="http://www.jpeterson.com/rnd/x101.0.1/Readme.html">http://www.jpeterson.com/rnd/x101.0.1/Readme.html</a>.
 * <p>
 * The jpeterson library requires that the Java Communications API be installed.
 * For more information on the COMM API, see
 * <a href="http://java.sun.com/products/javacomm/index.jsp">http://java.sun.com/products/javacomm/index.jsp</a>.
 * For convenience, the Java COMM API is included with the CruiseControl
 * distribution. On windows, copy the win32com.dll from CruiseControl's lib directory to
 * your <code>JAVA_HOME/bin</code> directory.
 * <p>
 * NOTE: If you receive the following error:
 * <PRE><CODE>Error loading win32com: java.lang.UnsatisfiedLinkError: no win32com in java.library.path</CODE></PRE>
 * it probably means that the Windows DLL named win32com.dll needs to be copied
 * from CruiseControl's lib directory into your JDK (or JRE) bin directory, i.e.
 * the same directory that java.exe is found.
 * <p>
 * The standard behavior for this publisher is to send the device the "on"
 * signal when the build breaks and then the "off" signal when the build is
 * successful. If you want the opposite, i.e. on when successful and off when
 * broken, set the onWhenBroken attribute to false.
 * <p>
 * <p>
 * Publisher Attributes:
 * <ul>
 * <li>houseCode - required - the house code for the device to control, A -&gt; P case insensitive</li>
 * <li>deviceCode - required - the device code the device to control, 1 -&gt; 16</li>
 * <li>port - optional - serial port to which the CM11A computer interface controller is
 * connected, defaults to COM2</li>
 * <li>onWhenBroken - optional - set to false if the device should turn on when the build is successful and off
 * when failed, defaults to true</li>
 * <li>interfaceMode - optional - set to either CM11A or CM17A depending on the model of the X10 computer interface
 * being used, defaults to CM11A</li>
 * </ul>
 *
 * @author <a href="mailto:pauljulius@users.sourceforge.net">Paul Julius</a>
 * @since August 26 2004
 */
public class X10Publisher implements Publisher {
    private static final Logger LOG = Logger.getLogger(X10Publisher.class);

    private String houseCode;
    private String deviceCode;
    private String port;
    private boolean onWhenBroken = true;
    private String interfaceModel;

    public void publish(Element cruisecontrolLog)
            throws CruiseControlException {

        XMLLogHelper logHelper = new XMLLogHelper(cruisecontrolLog);
        handleBuild(!logHelper.isBuildSuccessful());
    }

    public void handleBuild(boolean isBroken) throws CruiseControlException {
        if ((isBroken && onWhenBroken) || (!isBroken && !onWhenBroken)) {
            turnOn();
        } else {
            turnOff();
        }
    }

    public void turnOn() throws CruiseControlException {
        final char houseCodeChar = houseCode.charAt(0);
        final int deviceCodeInt = Integer.valueOf(deviceCode).intValue();

        X10Event[] events = new X10Event[ 2 ];
        events[ 0 ] = new AddressEvent(this, houseCodeChar, deviceCodeInt);
        events[ 1 ] = new OnEvent(this, houseCodeChar);

        send(events);
    }

    public void turnOff() throws CruiseControlException {
        final char houseCodeChar = houseCode.charAt(0);
        final int deviceCodeInt = Integer.valueOf(deviceCode).intValue();

        X10Event[] events = new X10Event[ 2 ];
        events[ 0 ] = new AddressEvent(this, houseCodeChar, deviceCodeInt);
        events[ 1 ] = new OffEvent(this, houseCodeChar);

        send(events);

    }

    private void send(X10Event[] events) throws CruiseControlException {
        LOG.info("Sending X10 events...");
        Transmitter transmitter = getTransmitter();
        if (port != null) {
            ((SerialGateway) transmitter).setPortName(port);
        }
        try {
            ((Gateway) transmitter).allocate();
        } catch (Exception e) {
            throw new CruiseControlException("Trouble allocating the x10 gateway.", e);
        }

        for (int j = 0; j < events.length; j++) {
            LOG.debug("Transmitting: " + events[ j ]);
            try {
                transmitter.transmit(events[ j ]);
            } catch (IOException e) {
                throw new CruiseControlException("Trouble transmitting event " + events[ j ], e);
            }
        }

        if (transmitter instanceof Gateway) {
            Gateway gateway = (Gateway) transmitter;

            try {
                LOG.debug("Wait for empty queue...");
                gateway.waitGatewayState(Transmitter.QUEUE_EMPTY);
                LOG.debug("Done");
            } catch (InterruptedException e) {
            }

            LOG.debug("Deallocating...");
            try {
                gateway.deallocate();
            } catch (GatewayException e) {
                LOG.warn("Error deallocation gateway: " + e.getMessage(), e);
            }
            LOG.debug("Done");
        }
        LOG.debug("Done sending X10 events...");
    }

    protected Transmitter getTransmitter() throws CruiseControlException {
        if (interfaceModel != null && interfaceModel.equalsIgnoreCase("CM17A")) {
            return new CM17A();
        } else if (interfaceModel == null || interfaceModel.equals("") || interfaceModel.equalsIgnoreCase("CM11A")) {
            return new CM11A();
        } else {
            throw new CruiseControlException("Unknown interface model specified [" + interfaceModel + "].");
        }
    }

    public void validate() throws CruiseControlException {
        ValidationHelper.assertFalse(houseCode == null || deviceCode == null,
                "Both houseCode and deviceCode are required fields.");

        ValidationHelper.assertTrue(isLegalHouseCode(houseCode),
            "The house code must be a single alphabetic "
                    + "letter between A and P, inclusive. You specified ["
                    + houseCode
                    + "].");

        ValidationHelper.assertTrue(isLegalDeviceCode(deviceCode),
            "The device code must be an integer between"
                    + " 1 and 16, inclusive. You specified ["
                    + deviceCode + "]");

        ValidationHelper.assertTrue(isLegalInterfaceModel(interfaceModel),
            "The interface model must is not a legal value. You specified ["
                    + deviceCode + "]");
    }

    private static boolean isLegalInterfaceModel(String model) {
        return model == null
                || "".equals(model)
                || "cm11a".equalsIgnoreCase(model)
                || "cm17a".equalsIgnoreCase(model);
    }

    private static boolean isLegalDeviceCode(String deviceCode) {
        return "1".equals(deviceCode)
                || "2".equals(deviceCode)
                || "3".equals(deviceCode)
                || "4".equals(deviceCode)
                || "5".equals(deviceCode)
                || "6".equals(deviceCode)
                || "7".equals(deviceCode)
                || "8".equals(deviceCode)
                || "9".equals(deviceCode)
                || "10".equals(deviceCode)
                || "11".equals(deviceCode)
                || "12".equals(deviceCode)
                || "13".equals(deviceCode)
                || "14".equals(deviceCode)
                || "15".equals(deviceCode)
                || "16".equals(deviceCode);
    }

    private static boolean isLegalHouseCode(String houseCode) {
        return "A".equalsIgnoreCase(houseCode)
                || "B".equalsIgnoreCase(houseCode)
                || "C".equalsIgnoreCase(houseCode)
                || "D".equalsIgnoreCase(houseCode)
                || "E".equalsIgnoreCase(houseCode)
                || "F".equalsIgnoreCase(houseCode)
                || "G".equalsIgnoreCase(houseCode)
                || "H".equalsIgnoreCase(houseCode)
                || "I".equalsIgnoreCase(houseCode)
                || "J".equalsIgnoreCase(houseCode)
                || "K".equalsIgnoreCase(houseCode)
                || "L".equalsIgnoreCase(houseCode)
                || "M".equalsIgnoreCase(houseCode)
                || "N".equalsIgnoreCase(houseCode)
                || "O".equalsIgnoreCase(houseCode)
                || "P".equalsIgnoreCase(houseCode);
    }

    public void setHouseCode(String code) {
        this.houseCode = code;
    }

    public void setDeviceCode(String code) {
        this.deviceCode = code;
    }

    /**
     * The x10 cm11a library defaults to com2 for the port, so this
     * attribute is optional.
     */
    public void setPort(String portName) {
        this.port = portName;
    }

    public void setOnWhenBroken(boolean shouldTurnOn) {
        this.onWhenBroken = shouldTurnOn;
    }

    public void setInterfaceModel(String model) {
        this.interfaceModel = model;
    }
}
