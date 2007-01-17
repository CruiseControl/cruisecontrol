/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
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
/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package net.sourceforge.cruisecontrol.util;

import junit.framework.Assert;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * Mocks Commandline objects.
 *
 * @author jerome@coffeebreaks.org
 */
public class MockCommandline extends Commandline {

    // private static final Logger LOG = Logger.getLogger(Commandline.class);

    private String[] expectedCommandline;
    private String expectedWorkingDirectory;
    private InputStream processErrorStream = null;
    private InputStream processInputStream = null;
    private OutputStream processOutputStream = null;
    private boolean assertCorrectCommandline = true;

    public MockCommandline() {
    }

    /**
     * @param expectedCommandline The expectedCommandline to set.
     */
    public void setExpectedCommandline(String[] expectedCommandline) {
        this.expectedCommandline = expectedCommandline;
    }

    /**
     * @param expectedWorkingDirectory The expectedWorkingDirectory to set.
     */
    public void setExpectedWorkingDirectory(String expectedWorkingDirectory) {
        this.expectedWorkingDirectory = expectedWorkingDirectory;
    }

    /**
     * @param processErrorStream The processErrorStream to set.
     */
    public void setProcessErrorStream(InputStream processErrorStream) {
        this.processErrorStream = processErrorStream;
    }

    /**
     * @param processInputStream The processInputStream to set.
     */
    public void setProcessInputStream(InputStream processInputStream) {
        this.processInputStream = processInputStream;
    }

    /**
     * @param processOutputStream The processOutputStream to set.
     */
    public void setProcessOutputStream(OutputStream processOutputStream) {
        this.processOutputStream = processOutputStream;
    }

    /**
     * Do not perform assertions.
     * @param assertCorrectCommandline
     */
    public void setAssertCorrectCommandline(boolean assertCorrectCommandline) {
        this.assertCorrectCommandline = assertCorrectCommandline;
    }

    public void ensureCommandline() {
        if (!Arrays.equals(expectedCommandline, getCommandline())) {
            Assert.fail("Command line error expected: "
                + buildString(expectedCommandline) + " - got: " + buildString(getCommandline()));
        }
    }

    public void ensureWorkingDirectory() {
        Assert.assertEquals("WorkingDirectory error", expectedWorkingDirectory, getWorkingDirectory());
    }

    private static final String buildString(String[] array) {
        if (array == null) {
            return "null";
        }
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < array.length; i++) {
            sb.append(array[i]).append(" ");
        }
        return sb.toString();
    }

    /**
     * Fakes the execution of the command.
     * Checks that the command line and the working directory are correctly set / computed.
     *
     * @return a MockProcess that uses the streams registered with that instance.
     */
    public Process execute() {

        if (assertCorrectCommandline) {
            ensureCommandline();
            ensureWorkingDirectory();
        }

        MockProcess process = getMockProcess();
        process.setErrorStream(processErrorStream);
        process.setInputStream(processInputStream);
        process.setOutputStream(processOutputStream);

        return process;
    }

    protected MockProcess getMockProcess() {
        return new MockProcess();
    }

}
