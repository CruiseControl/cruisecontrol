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

import org.apache.tools.ant.Target;
import org.apache.tools.ant.XmlLogger;

/**
 * Executes a CruiseProject.
 *
 * @author  robertdw@bigpond.net.au
 * @version Revision: 1.1.1
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
