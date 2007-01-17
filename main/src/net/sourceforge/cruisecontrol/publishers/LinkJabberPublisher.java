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
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import net.sourceforge.cruisecontrol.util.XMLLogHelper;

import java.io.File;

/**
 * Implementation of the Jabber publisher which publishes
 * a link to the build results via Jabber Instant Messaging framework.
 *
 * @see net.sourceforge.cruisecontrol.publishers.JabberPublisher
 *
 * @author <a href="mailto:jonas_edgeworth@cal.berkeley.edu">Jonas Edgeworth</a>
 * @version 1.0
 */
public class LinkJabberPublisher extends JabberPublisher {

    private String buildResultsURL;

    public void setBuildResultsURL(String buildResultsURL) {
        this.buildResultsURL = buildResultsURL;
    }

    /**
     *  Validate that all the mandatory parameters were specified in order
     * to properly initial the Jabber client service. Note that this is called
     * after the configuration file is read.
     *
     *  @throws CruiseControlException if there was a configuration error.
     */
    public void validate() throws CruiseControlException {

        super.validate();

        ValidationHelper.assertIsSet(buildResultsURL, "buildresulturl", this.getClass());
    }

    /**
     *  Creates the IM message body.  This currently creates a message
     *  that is a link to a web page with the details of the build.
     *
     *  @return <code>String</code> the link that makes up the body of the IM message
     * @throws CruiseControlException
     */
    protected String createMessage(XMLLogHelper logHelper) throws CruiseControlException {
        String logFileName = logHelper.getLogFileName();
        String baseLogFileName =
                logFileName.substring(
                        logFileName.lastIndexOf(File.separator) + 1,
                        logFileName.lastIndexOf("."));

        StringBuffer message = new StringBuffer();
        message.append("Build results for ");
        message.append(logHelper.isBuildSuccessful() ? "successful" : "failed");
        message.append(" build of project ");
        message.append(logHelper.getProjectName());
        message.append(": ");

        message.append(buildResultsURL);

        if (buildResultsURL.indexOf("?") == -1) {
            message.append("?");
        } else {
            message.append("&");
        }

        message.append("log=");
        message.append(baseLogFileName);

        return message.toString();
    }

}
