/*
 * Copyright 2001 QSI Payments, Inc.
 * All rights reserved.  This precautionary copyright notice against
 * inadvertent publication is neither an acknowledgement of publication,
 * nor a waiver of confidentiality.
 *
 * Identification:
 *	$Id$
 *
 * Description:
 *	<<<Short-description-of-what-this-class-does.>>>
 */
package net.sourceforge.cruisecontrol;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;

import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.util.Date;

/**
 * <<<Short-description-of-what-this-class-does.>>>
 *
 * @author  $Author$
 * @version $Revision$
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
