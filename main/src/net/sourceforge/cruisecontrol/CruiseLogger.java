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
package net.sourceforge.cruisecontrol;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;

import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.util.Date;

/**
 * This class provides the extensions to Ant's XmlLogger class that we need, mostly
 * to do with filtering out undesired messages.
 *
 * @author  Author: robertdw@bigpond.net.au
 * @version Revision: 1.1.1
 */
public class CruiseLogger extends org.apache.tools.ant.XmlLogger {
    static public String CVS_ID = "@(#)$Id$";

    /* ========================================================================
     * Static class members.
     */

    /* ========================================================================
     * Instance members.
     */
    private final int _messageLevel;
    
    /* ========================================================================
     * Constructors
     */
    
    /** Creates new CruiseLogger */
    public CruiseLogger(int messageLevel) {
        _messageLevel = messageLevel;
    }

    /* ========================================================================
     * Public Methods.
     */

    public int getMessageLevel() {
        return _messageLevel;
    }

    public void buildStarted(org.apache.tools.ant.BuildEvent buildEvent) {
        if (!canWriteXMLLoggerFile()) {
            throw new BuildException("No write access to " + MasterBuild.XML_LOGGER_FILE);
        }

        super.buildStarted(buildEvent);
    }
    
    /**
     * Wraps the XmlLogger's method with a logging level check
     */
    public void messageLogged(BuildEvent event) {
        int logLevel = event.getPriority();
        if (logLevel > _messageLevel) {
            // Message priority is too low.
            return;
        }
        super.messageLogged(event);
    }    
    
    /* ========================================================================
     * Private Methods.
     */

    boolean canWriteXMLLoggerFile() {
        File logFile = new File(MasterBuild.XML_LOGGER_FILE);
        if (!logFile.exists() || logFile.canWrite()) {
            return true;
        }
        
        return false;
    }
    
    public void buildFinished(org.apache.tools.ant.BuildEvent buildEvent) {
        //If the XmlLogger.file property doesn't exist, we will set it here to a default
        //  value. This will short circuit XmlLogger from setting the default value.
        org.apache.tools.ant.Project proj = buildEvent.getProject();
        String prop = proj.getProperty("XmlLogger.file");
        if (prop == null || prop.trim().length() == 0) {
            proj.setProperty("XmlLogger.file", MasterBuild.XML_LOGGER_FILE);
        }

        super.buildFinished(buildEvent);
    }
    
}
