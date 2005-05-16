/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2005, ThoughtWorks, Inc.
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

import org.mortbay.http.SocketListener;
import org.mortbay.jetty.Server;
import java.io.IOException;

/**
 * Start up for CruiseControl and Jetty.
 *
 * @author <a href="mailto:pj@thoughtworks.com">Paul Julius</a>
 */
public class CruiseControlWithJetty {

    public static void main(final String[] args) throws Exception {
        //A Thread for Jetty...
        new Thread(new Runnable() {
            public void run() {
                Server server = new Server();
                SocketListener listener = new SocketListener();
                listener.setPort(8080);
                server.addListener(listener);
                try {
                    server.addWebApplication("cruisecontrol", "./webapps/cruisecontrol");
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException("Exception adding cruisecontrol webapp");
                }
                try {
                    server.start();
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException("Exception occured in server execution");
                }
            }
        }).start();

        //A Thread for CruiseControl
        new Thread(new Runnable() {
            public void run() {
                CruiseControl.main(args);
            }
        }).start();
    }
}
