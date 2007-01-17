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

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import com.jpeterson.x10.module.CM11A;
import com.jpeterson.x10.module.CM17A;

public class X10PublisherTest extends TestCase {

//    These tests are only useful if you actually have a x10 computer interface connected to the computer.
//    PJ - Sept 1, 2004
//
//    public void testHandleBuild()
//            throws CruiseControlException {
//        X10Publisher x10Publisher = new X10Publisher();
//        x10Publisher.setDeviceCode("3");
//        x10Publisher.setHouseCode("A");
//        x10Publisher.setPort("COM1");
//
//        x10Publisher.handleBuild(true); //light should turn on
//        x10Publisher.handleBuild(false); //light should turn off
//
//        x10Publisher.setOnWhenBroken(false);
//        x10Publisher.handleBuild(true); //light should turn off
//        x10Publisher.handleBuild(false); //light should turn on
//
//    }
//
//    public void testHandleBuildWithAlternateBehavior()
//            throws CruiseControlException {
//        X10Publisher x10Publisher = new X10Publisher();
//        x10Publisher.setDeviceCode("3");
//        x10Publisher.setHouseCode("A");
//        x10Publisher.setPort("COM1");
//        x10Publisher.setOnWhenBroken(false);
//
//        x10Publisher.handleBuild(true); //light should turn off
//        x10Publisher.handleBuild(false); //light should turn on
//    }

    public void testSettingAPort() {
        X10Publisher x10Publisher = new X10Publisher();
        x10Publisher.setDeviceCode("3");
        x10Publisher.setHouseCode("A");
        x10Publisher.setPort("COM1");
        //Shouldn't get an exception, even if the port doesn't exist,
        // publish will just fail.
        x10Publisher.setPort("THIS_ISN'T_A_REAL_PORT");
    }

    public void testRequiredFieldValidation() throws CruiseControlException {
        X10Publisher x10Publisher = new X10Publisher();

        try {
            x10Publisher.validate();
            fail("Should have gotten an exception when "
                    + "required fields left blank.");
        } catch (CruiseControlException e) {
            assertTrue("Expected this exception.", true);
        }

        x10Publisher.setHouseCode("A");
        x10Publisher.setDeviceCode("3");

        x10Publisher.validate();
    }

    public void testHouseCodeValidation() throws CruiseControlException {
        X10Publisher x10Publisher = new X10Publisher();

        x10Publisher.setDeviceCode("3"); //Legal
        x10Publisher.setHouseCode(null); //Not legal
        try {
            x10Publisher.validate();
            fail("Should have gotten an exception when house code set to null");
        } catch (CruiseControlException e) {
            assertTrue("Expected this exception.", true);
        }

        x10Publisher.setHouseCode(""); //Not legal
        try {
            x10Publisher.validate();
            fail("Should have gotten an exception when "
                    + "house code set to blank");
        } catch (CruiseControlException e) {
            assertTrue("Expected this exception.", true);
        }

        x10Publisher.setHouseCode("1"); //Not legal
        try {
            x10Publisher.validate();
            fail("Should have gotten an exception when house code"
                    + " set to a number");
        } catch (CruiseControlException e) {
            assertTrue("Expected this exception.", true);
        }

        x10Publisher.setHouseCode("AA"); //Not legal
        try {
            x10Publisher.validate();
            fail("Should have gotten an exception when house code set "
                    + "to more than one character");
        } catch (CruiseControlException e) {
            assertTrue("Expected this exception.", true);
        }

        String[] illegalHouseCodes = {"Q", "R", "S", "T", "U", "V", "W", "X",
                                      "Y", "Z"};
        for (int i = 0; i < illegalHouseCodes.length; i++) {
            String nextHouseCode = illegalHouseCodes[ i ];
            x10Publisher.setHouseCode(nextHouseCode); //Not legal
            try {
                x10Publisher.validate();
                fail("Should have gotten an exception when house code set to "
                        + nextHouseCode);
            } catch (CruiseControlException e) {
                assertTrue("Expected this exception.", true);
            }
        }

        String[] legalHouseCodes = {"A", "B", "C", "D", "E", "F", "G", "H",
                                    "I", "J", "K", "L", "M", "N", "O", "P"};
        for (int i = 0; i < legalHouseCodes.length; i++) {
            String nextHouseCode = legalHouseCodes[ i ];
            x10Publisher.setHouseCode(nextHouseCode); //Legal!!
            x10Publisher.validate();
        }

        String[] lowerCaseLegalHouseCodes = {"a", "b", "c", "d", "e", "f", "g",
                                             "h", "i", "j", "k", "l", "m", "n",
                                             "o", "p"};
        for (int i = 0; i < lowerCaseLegalHouseCodes.length; i++) {
            String nextHouseCode = lowerCaseLegalHouseCodes[ i ];
            x10Publisher.setHouseCode(nextHouseCode); //Legal!!
            x10Publisher.validate();
        }
    }

    public void testDeviceCodeValidation() throws CruiseControlException {
        X10Publisher x10Publisher = new X10Publisher();

        x10Publisher.setHouseCode("A"); //Legal
        x10Publisher.setDeviceCode(null); //Not legal
        try {
            x10Publisher.validate();
            fail("Should have gotten an exception when device"
                    + " code set to null");
        } catch (CruiseControlException e) {
            assertTrue("Expected this exception.", true);
        }

        x10Publisher.setDeviceCode(""); //Not legal
        try {
            x10Publisher.validate();
            fail("Should have gotten an exception when device"
                    + " code set to blank");
        } catch (CruiseControlException e) {
            assertTrue("Expected this exception.", true);
        }

        x10Publisher.setDeviceCode("A"); //Not legal
        try {
            x10Publisher.validate();
            fail("Should have gotten an exception when device code"
                    + " set to an alphabetic character");
        } catch (CruiseControlException e) {
            assertTrue("Expected this exception.", true);
        }

        String[] legalDeviceCodes = {"1", "2", "3", "4", "5", "6", "7", "8",
                                     "9", "10", "11", "12", "13", "14", "15",
                                     "16"};
        for (int i = 0; i < legalDeviceCodes.length; i++) {
            String nextDeviceCode = legalDeviceCodes[ i ];
            x10Publisher.setDeviceCode(nextDeviceCode); //Legal!!
            x10Publisher.validate();
        }

        String[] illegalDeviceCodes = {"-1", "17", "0", "-100", "1.1", "1.56",
                                       "13.00000000000000000001"};
        for (int i = 0; i < illegalDeviceCodes.length; i++) {
            String nextDeviceCode = illegalDeviceCodes[ i ];
            x10Publisher.setDeviceCode(nextDeviceCode); //Legal!!
            try {
                x10Publisher.validate();
                fail("Should have gotten an exception when"
                        + " the device code is set to "
                        + nextDeviceCode);
            } catch (CruiseControlException e) {
                assertTrue("Expected this exception", true);
            }
        }
    }

    public void testInterfaceModelValidation() throws CruiseControlException {
        X10Publisher x10Publisher = new X10Publisher();

        x10Publisher.setHouseCode("A"); //Legal
        x10Publisher.setDeviceCode("3"); //Legal
        x10Publisher.setInterfaceModel(null); //Legal
        x10Publisher.validate();

        x10Publisher.setInterfaceModel("CM11A"); //Legal
        x10Publisher.validate();

        x10Publisher.setInterfaceModel("CM17A"); //Legal
        x10Publisher.validate();

        x10Publisher.setInterfaceModel(""); //Legal
        x10Publisher.validate();

        x10Publisher.setInterfaceModel("cm11a"); //Legal
        x10Publisher.validate();

        x10Publisher.setInterfaceModel("cm17a"); //Legal
        x10Publisher.validate();

        x10Publisher.setInterfaceModel("cM11A"); //Legal
        x10Publisher.validate();

        x10Publisher.setInterfaceModel("cM17A"); //Legal
        x10Publisher.validate();

        x10Publisher.setInterfaceModel("jibberish"); //NOT Legal
        try {
            x10Publisher.validate();
            fail("Expected an exception");
        } catch (CruiseControlException e) {
            assertTrue("Expected this exception", true);
        }

        x10Publisher.setInterfaceModel("firecraker"); //NOT Legal
        try {
            x10Publisher.validate();
            fail("Expected an exception");
        } catch (CruiseControlException e) {
            assertTrue("Expected this exception", true);
        }
    }

    public void testGetTransmitter() throws CruiseControlException {
        X10Publisher x10Publisher = new X10Publisher();

        x10Publisher.setHouseCode("A"); //Legal
        x10Publisher.setDeviceCode("3"); //Legal

        x10Publisher.setInterfaceModel(null); //Legal
        assertTrue(x10Publisher.getTransmitter() instanceof CM11A);

        x10Publisher.setInterfaceModel(""); //Legal
        assertTrue(x10Publisher.getTransmitter() instanceof CM11A);

        x10Publisher.setInterfaceModel("cm11a"); //Legal
        assertTrue(x10Publisher.getTransmitter() instanceof CM11A);

        x10Publisher.setInterfaceModel("cm11A"); //Legal
        assertTrue(x10Publisher.getTransmitter() instanceof CM11A);

        x10Publisher.setInterfaceModel("cm17a"); //Legal
        assertTrue(x10Publisher.getTransmitter() instanceof CM17A);

        x10Publisher.setInterfaceModel("cm17A"); //Legal
        assertTrue(x10Publisher.getTransmitter() instanceof CM17A);
    }
}
