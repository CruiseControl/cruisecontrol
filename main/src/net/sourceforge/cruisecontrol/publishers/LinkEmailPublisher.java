/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 500
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
package net.sourceforge.cruisecontrol.publishers;

import net.sourceforge.cruisecontrol.util.XMLLogHelper;
import net.sourceforge.cruisecontrol.CruiseControlException;

import java.io.File;

import org.apache.log4j.Logger;

/**
 *  Concrete implementation of the <code>EmailPublisher</code> abstract class.  This class handles the simplest
 *  implementation where the message body is just a link to a web page detailing the build.
 *
 *  @author alden almagro, ThoughtWorks, Inc. 2002
 */
public class LinkEmailPublisher extends EmailPublisher {

    /** enable logging for this class */
    private static Logger log = Logger.getLogger(LinkEmailPublisher.class);

    /**
     *  Creates the email message body.  This implementation of <code>EmailPublisher</code> just creates a message
     *  that is a link to a web page with the details of the build.
     *
     *  @return <code>String</code> the link that makes up the body of the email message
     */
    protected String createMessage(XMLLogHelper logHelper) {
        String logFileName = "";
        try {
            logFileName = logHelper.getLogFileName();
        } catch (CruiseControlException e) {
            log.error("", e);
        }
        String baseLogFileName = logFileName.substring(logFileName.lastIndexOf(File.separator) + 1, logFileName.lastIndexOf("."));

        StringBuffer message = new StringBuffer();
        message.append("View results here -> ");
        message.append(_servletUrl);
        message.append("?log=");
        message.append(baseLogFileName);
        return message.toString();

    }
}