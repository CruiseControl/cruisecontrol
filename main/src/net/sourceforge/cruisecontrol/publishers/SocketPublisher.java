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
package net.sourceforge.cruisecontrol.publishers;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Publisher;
import net.sourceforge.cruisecontrol.gendoc.annotations.SkipDoc;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import net.sourceforge.cruisecontrol.util.XMLLogHelper;
import org.jdom.Element;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.rmi.UnknownHostException;
import org.apache.log4j.Logger;

/**
 * @author <a href="mailto:dcotterill@thoughtworks.com">Darren Cotterill</a>
 */
public class SocketPublisher implements Publisher {
    private static final Logger LOG = Logger.getLogger(SocketPublisher.class);

    private final SocketFactory factory;
    private String socketServer;
    private int port;
    private boolean isProjectNameSendingEnabled;
    private boolean isConsideringFixedEnabled;

    public SocketPublisher() {
        this(new SocketFactory() {
            public Socket createSocket(String server, int port) throws IOException {
                return new Socket(server, port);
            }
        });
    }

    public SocketPublisher(SocketFactory sf) {
       super();
        this.factory = sf;
        this.port = 0;
        this.isProjectNameSendingEnabled = true;
        this.isConsideringFixedEnabled = false;
    }

    public void validate() throws CruiseControlException {

        ValidationHelper.assertIsSet(getSocketServer(), "socketServer", this.getClass());
        ValidationHelper.assertFalse(getPort() == 0,
            "'port' not specified for SocketPublisher");
    }

    public void publish(Element cruisecontrolLog)
        throws CruiseControlException {

        XMLLogHelper helper = new XMLLogHelper(cruisecontrolLog);
        try {
            StringBuffer messageBuffer = new StringBuffer();
            messageBuffer.append(getBuildResultRepresentationFor(helper));
            if (this.isProjectNameSendingEnabled) {
               messageBuffer.append(" ");
               messageBuffer.append(helper.getProjectName());
            }
            writeToSocket(messageBuffer.toString());
        } catch (IOException e) {
            throw new CruiseControlException(e);
        }
    }

    protected String getBuildResultRepresentationFor(XMLLogHelper helper) throws CruiseControlException {
               String result = "Failure";
        if (helper.isBuildSuccessful()) {
            result = "Success";
            if (this.isConsideringFixedEnabled && helper.isBuildFix()) {
               result = "Fixed";
            }
        }
        return result;
    }

    protected void writeToSocket(String result) throws IOException {
        Socket echoSocket = null;
        PrintWriter out = null;

        try {
            echoSocket = factory.createSocket(socketServer, getPort());
            out = new PrintWriter(echoSocket.getOutputStream(), true);
        } catch (UnknownHostException e) {
            LOG.error("Don't know about host:" + socketServer);
        } catch (IOException e) {
            LOG.error("Couldn't get I/O for the connection to:" + socketServer);
        }

        if (out != null) {
            out.write(result);
            out.close();
        }
        if (echoSocket != null) {
            echoSocket.close();
        }
    }

    public String getSocketServer() {
        return socketServer;
    }
    public void setSocketServer(String port) {
        socketServer = port;
    }
    public int getPort() {
        return port;
    }
    public void setPort(int port) {
        this.port = port;
    }
    
    @SkipDoc
    public void setPort(String port) {
        this.port = Integer.parseInt(port);
    }
    
    public void setSendProjectName(boolean state) {
       this.isProjectNameSendingEnabled = state;
    }
    public void setSendFixed(boolean state) {
       this.isConsideringFixedEnabled = state;
    }
}
