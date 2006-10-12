/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2003, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 600
 * Chicago, IL 60661 USA
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
package net.sourceforge.cruisecontrol.taglib;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.Tag;

/**
 * Extracts the base JMX HTTP URL from System properties and application context.
 * @author <a href="mailto:hak@2mba.dk">Hack Kampbjorn</a>
 */
public class JmxBaseTag extends CruiseControlTagSupport {
    private static final String JMX_HOST = "cruisecontrol.jmxhost";
    private static final String JMX_PORT = "cruisecontrol.jmxport";
    private static final String DEFAULT_JMX_HOST = "localhost";
    private static final String DEFAULT_JMX_PATH = "/";
    private static final int DEFAULT_JMX_PORT = 8000;

    public int doStartTag() throws JspException {
        URL jmxBase = null;
        try {
            jmxBase = createJmxUrl();
        } catch (MalformedURLException muex) {
            try {
                jmxBase = new URL("http", DEFAULT_JMX_HOST, DEFAULT_JMX_PORT, DEFAULT_JMX_PATH);
            } catch (MalformedURLException e2) {
                throw new AssertionError(e2);
            }
        }
        getPageContext().setAttribute(getId(), jmxBase);
        return Tag.SKIP_BODY;
    }

    private String getParameter(String param) {
        String value = System.getProperty(param);
        if (value == null) {
            value = getContextParam(param);
        }
        return value;
    }

    private URL createJmxUrl() throws JspException, MalformedURLException {
        String jmxHost = getParameter(JMX_HOST);
        if (jmxHost == null) {       
            try {
                jmxHost = InetAddress.getLocalHost().getCanonicalHostName();
            } catch (IOException e) {
                try {
                    jmxHost = InetAddress.getLocalHost().getHostName();
                } catch (IOException e2) {
                    jmxHost = "localhost";
                }
            }
        }

        String jmxPortNumber = getParameter(JMX_PORT);
        int jmxPort = 0;
        if (jmxPortNumber == null) {
            jmxPort = DEFAULT_JMX_PORT;
        } else {
            jmxPort = Integer.parseInt(jmxPortNumber);
        }

        return new URL("http", jmxHost, jmxPort, DEFAULT_JMX_PATH);
    }
}
