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

import org.apache.tools.ant.Target;
import org.apache.tools.ant.XmlLogger;

/**
 * <<<Short-description-of-what-this-class-does.>>>
 *
 * @author  $Author$
 * @version $Revision$
 */
public class BuildRunner {

    /* ========================================================================
     * Static class members.
     */

    /* ========================================================================
     * Instance members.
     */
    
    private CruiseProject _project;
    private Target _target;
    
    private String _lastBuildTime;
    private String _label;

    private java.io.PrintStream _out = System.out;
    private java.io.PrintStream _err = System.err;
    
    private CruiseLogger _logger;
    
    /* ========================================================================
     * Constructors
     */
    
    /** Creates new BuildRunner */
    public BuildRunner(String buildFileName, String target, String lastBuildTime, String label, CruiseLogger logger) {
        this(new java.io.File(buildFileName), target, lastBuildTime, label, logger);
    }

    public BuildRunner(java.io.File buildFile, String target, String lastBuildTime, String label, CruiseLogger logger) {
        _logger = logger;
        loadProject(buildFile);
        loadTarget(target);

        _lastBuildTime = lastBuildTime;
        _label = label;
    }

    /* ========================================================================
     * Public Methods.
     */

    public boolean runBuild() {
        setDefaultLogger();
        getProject().fireBuildStarted();
        
        getProject().init();
        
        getProject().setUserProperty("lastGoodBuildTime", _lastBuildTime);
        getProject().setUserProperty("label", _label);
        
        Throwable error = null;
        try {
            getProject().executeTarget(getTarget().getName());
            return true;
        }
        catch (Throwable theError) {
            error = theError;
            return false;
        }
        finally {
            getProject().fireBuildFinished(error);
        }
    }
    
    public CruiseProject getProject() {
        return _project;
    }
    
    public Target getTarget() {
        return _target;
    }
    
    public void reset() {
        getProject().reset();
    }
    
    /* ========================================================================
     * Public Methods.
     */

    void loadProject(java.io.File buildFile) {
        _project = new CruiseProject();
        _project.setBaseDir(buildFile.getParentFile());
        org.apache.tools.ant.ProjectHelper.configureProject(_project, buildFile);
        _project.addBuildListener(_logger);
    }
    
    void loadTarget(String targetName) {
        String theTarget = targetName;
        if (targetName == null || "".equals(targetName)) {
            theTarget = getProject().getDefaultTarget();
        }

        _target = (Target)getProject().getTargets().get(theTarget);
        if (_target == null) {
            throw new RuntimeException("There is no target called '" + targetName + "'");
        }
    }
    
    void setDefaultLogger() {
        org.apache.tools.ant.DefaultLogger defaultLogger = new org.apache.tools.ant.DefaultLogger();
        defaultLogger.setMessageOutputLevel(_logger.getMessageLevel());
        defaultLogger.setOutputPrintStream(_out);
        defaultLogger.setErrorPrintStream(_err);
        getProject().addBuildListener(defaultLogger);
    }
}
