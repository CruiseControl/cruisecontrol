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

/**
 * <<<Short-description-of-what-this-class-does.>>>
 *
 * @author  $Author$
 * @version $Revision$
 */
public class CruiseProject extends org.apache.tools.ant.Project {

    public void reset() {
        super.getBuildListeners().clear();
        super.getDataTypeDefinitions().clear();
        super.getFilters().clear();
        super.getProperties().clear();
        super.getReferences().clear();  // Strongly suspect this one to be the culprit.
        super.getTargets().clear();
        super.getTaskDefinitions().clear();
        super.getUserProperties().clear();
    }
    
    public void fireBuildStarted() {
        super.fireBuildStarted();
    }
    
    public void fireBuildFinished(Throwable error) {
        super.fireBuildFinished(error);
    }
}
