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

import java.io.*;
import java.text.*;
import java.util.*;
import javax.mail.*;
import org.apache.tools.ant.*;
import org.apache.log4j.Category;
import org.jdom.*;
import net.sourceforge.cruisecontrol.publishers.CurrentBuildStatusPublisher;

/**
 * Class that will run the "Master Build" -- a
 * loop over the build process so that builds can
 * be automatically run.
 *
 * @author <a href="mailto:alden@thoughtworks.com">alden almagro</a>
 * @author <a href="mailto:pj@thoughtworks.com">Paul Julius</a>
 * @author Robert Watkins
 * @author <a href="mailto:jcyip@thoughtworks.com">Jason Yip</a>
 * @author <a href="mailto:johnny.cass@epiuse.com">Johnny Cass</a>
 * @author <a href="mailto:davidl@iis.com">David Le Strat</a>
 */
public class MasterBuild {

    /** enable logging for this class */
    private static Category log = Category.getInstance(MasterBuild.class.getName());

    private static String BUILDINFO_FILENAME = "buildcycleinfo";
    private final String DEFAULT_EMAILMAP = "emailmap.properties";
    private final String DEFAULT_PROPERTIES_FILENAME = "cruisecontrol.properties";
    public static final String XML_LOGGER_FILE = "log.xml";
    private final String DEFAULT_BUILD_STATUS_FILENAME = "currentbuild.txt";
    private final String DEFAULT_LOG_DIR = "logs";
    private final String DEFAULT_BUILD_FILE = "build.xml";
    private final String DEFAULT_TARGET = "masterbuild";
    private final String DEFAULT_CLEAN_TARGET = "cleanbuild";

    //build properties
    protected String _propsFileName;

    private String _projectName;
    
    protected BuildInfo info;
    protected CruiseControlProperties props;

    protected int _buildCounter;
    
    /**
     * Entry point.  Verifies that all command line arguments are correctly 
     * specified.
     */
    public static void main(String[] args) {
        MasterBuild mb = new MasterBuild(args);
        mb.execute();
    }

    /**
     * Constructs a new MasterBuild instance, performing required initialization steps.
     * 
     * @param args   User specified arguments to MasterBuild.
     */
    public MasterBuild(String[] args) {
        log.info("***** Starting automated build process *****\n");

        readBuildInfo();
        overwriteWithUserArguments(args);

        if (!buildInfoSpecified()) {
            usage();
        }    
    }

    /**
     * Returns the properties instance controlling this process.
     * 
     * @return CruiseControlProperties being used by this process.
     */
    public CruiseControlProperties getProperties() {
        return props;
    }

    /**
     * Deserialize the label and timestamp of the last good build.
     */
    public void readBuildInfo() {
        info = new BuildInfo();
        info.read();
    }

    public void overwriteWithUserArguments(String[] args) {
        for (int i = 0; i < args.length - 1; i++) {
            try {
                if (args[i].equals("-lastbuild")) {
                    info.setLastBuild(args[i + 1]);
                    info.setLastGoodBuild(info.getLastBuild());
                } else if (args[i].equals("-label")) {
                    //(PENDING) check format of label
                    info.setLabel(args[i + 1]);
                } else if (args[i].equals("-properties")) {
                    _propsFileName = args[i + 1];
                }
            } catch (RuntimeException re) {
                re.printStackTrace();
                usage();
            }
        }
    }
    
    public boolean buildInfoSpecified() {
        return info.ready();
    }
    
    /**
     * Loop infinitely, firing off the build as necessary.  Reloads the 
     * properties file every time so that the build process need not be stopped,
     * only the property file needs to be edited if changes are necessary.  Will
     * execute an alternate ant task every n builds, so that we can possibly 
     * execute a full clean build, etc.
     */
    public void execute() {
        try {
            _buildCounter = 0;
            while (true) {
                Date startTime = new Date();
                startLog();
                //Reload the properties file.
                props = new CruiseControlProperties(_propsFileName);
                performBuild(startTime);
                long timeToSleep = getSleepTime(startTime);
                endLog(timeToSleep);
                Thread.sleep(timeToSleep);
            }

        } catch (InterruptedException e) {
            log.error("Exception trying to sleep");
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Performs a build, defaulting the start time for the build to NOW.
     */
    protected void performBuild() throws Exception {
        performBuild(new Date());
    }

    /**
     * Does the work of performing the actual build.
     */
    protected void performBuild(Date startTime) throws Exception {
        
        boolean previousBuildSuccessful = (info.getLastBuild() == info.getLastGoodBuild());       

        //logCurrentBuildStatus(true, startTime);
        Bootstrapper currentBuildStatusBootstrapper = props.createCurrentBuildStatusBootstrapper();
        currentBuildStatusBootstrapper.bootstrap();

        int messageLevel = props.isDebug() ? Project.MSG_DEBUG :
                                    (props.isVerbose() ? Project.MSG_VERBOSE : Project.MSG_INFO);
        CruiseLogger logger = new CruiseLogger(messageLevel);
        
        log.debug("Opening build file: " + props.getAntFile());
        String target = null; // remember: Null target means default target.
        if (((_buildCounter % props.getCleanBuildEvery()) == 0) 
            && props.getCleanAntTarget() != "") {

            log.debug("Using clean target: " + props.getCleanAntTarget());
            target = props.getCleanAntTarget();

        } else if (props.getAntTarget() != "") {
            log.debug("Using normal target: " + props.getAntTarget());
            target = props.getAntTarget();
        }
        BuildRunner runner = new BuildRunner(props.getAntFile(), target, 
                                             info.getLastGoodBuild(),
                                             info.getLastBuild (),
                                             info.getLabel(), logger);
        
        boolean successful = runner.runBuild();

        //doing this temporarily to simulate a build log for CurrentBuildStatusPublisher.
        Element buildLog = new Element("build");
        Element cruisecontrolLog = new Element("cruisecontrol");
        cruisecontrolLog.addContent(info.toElement());
        cruisecontrolLog.addContent(props.toElement());
        buildLog.addContent(cruisecontrolLog);

        //logCurrentBuildStatus(false, startTime);
        CurrentBuildStatusPublisher currentBuildStatusPublisher = props.createCurrentBuildStatusPublisher();
        currentBuildStatusPublisher.publish(buildLog);


        checkModificationSetInvoked(runner.getProject());
        
        buildFinished(runner.getProject(), successful);

        //(PENDING) do this in buildFinished?
        if (info.isBuildNotNecessary()) {
            if (!info.isLastBuildSuccessful() && props.shouldSpamWhileBroken()) {
                sendBuildEmail(_projectName + "Build still failing...");
            }
        } else {
            if (info.isLastBuildSuccessful()) {
                _buildCounter++;
                if(props.getReportSuccess().equalsIgnoreCase("always")) {
                    sendBuildEmail(_projectName + " Build " + info.getLabel() + 
                    " Successful");
                } else if(props.getReportSuccess().equalsIgnoreCase("fixes")) {                    
                    if (!previousBuildSuccessful) {                        
                        sendBuildEmail(_projectName + " Build Fixed, " + 
                        info.getLabel() + " Successful");
                    } else {
                        log.info("Skipping email notifications for successful builds");
                    }
                } else {
                    log.info("Skipping email notifications for fixed and " +
                    "successful builds");
                }
                
                //info.incrementLabel(props.getLabelIncrementerClassName());
                LabelIncrementer labelIncrementer = props.createLabelIncrementer();
                info.setLabel(labelIncrementer.incrementLabel(info.getLabel(), null));

            } else {
                sendBuildEmail(_projectName + "Build Failed");
            }            
            info.write(); 
        }
        runner.reset();
    }

    /**
     * Esnures that a ModificationSet was invoked during the last build cycle.
     * If none was invoked, then an error message is logged and a BuildException
     * will be thrown.
     *
     * @throws BuildException if no ModificationSet was invoked during the
     *  previous build cycle.
     */
    void checkModificationSetInvoked(Project project) throws BuildException {
        // There might be a better way to do this, perhaps by quering the project for
        // a list of executed tasks??? For now, this will suffice, as this property
        // should always be set by the ModificationSet (even if there are no changes).
        if (project.getProperty(ModificationSet.MODIFICATIONSET_INVOKED) == null) {
            // This means that there was never a modification set task called.
            log.error("The specified Ant target did not result in a ModificationSet task being called.");
            log.error("Without a ModificationSet task, CruiseControl can not work correctly");
            throw new BuildException("No ModificationSet task invoked");
        }
    }

    private void sendBuildEmail(String message) {
        CruiseControlMailer mailer = new CruiseControlMailer(props.getMailhost(),
                                                             props.getReturnAddress());
        mailer.emailReport(props, info.getUserList(), message,
                           info.getLogfile(), info.isLastBuildSuccessful());
    }
    
    protected long getSleepTime(Date startTime) {
        if (props.isIntervalAbsolute()) {
            // We need to sleep up until startTime + buildInterval.
            // Therefore, we need startTime + buildInterval - now.
            Date now = new Date();
            long sleepTime = startTime.getTime() + props.getBuildInterval() - now.getTime();
            sleepTime = (sleepTime < 0 ? 0 : sleepTime);
            return sleepTime;
        }
        else {
            return props.getBuildInterval();
        }
    }
    
    /**
     *  Print header for each build attempt.
     */
    protected void startLog() {
        log.info("***** Starting Build Cycle");
        log.info("***** Label: " + info.getLabel());
        log.info("***** Last Good Build: " + info.getLastGoodBuild());
        log.info("\n");
    }

    /**
     *  Print footer for each build attempt.
     */
    protected void endLog(long sleepTime) {
        log.info("\n");
        log.info("***** Ending Build Cycle, sleeping " + (sleepTime/1000.0) + " seconds until next build.\n\n\n");
        log.info("***** Label: " + info.getLabel());
        log.info("***** Last Good Build: " + info.getLastGoodBuild());
        log.info("\n");
    }

    /**
     *  Print usage instructions if command line arguments are not correctly specified.
     */
    public void usage() {
        System.out.println("Usage:");
        System.out.println("");
        System.out.println("Starts a continuous integration loop");
        System.out.println("");
        System.out.println("java MasterBuild [options]");
        System.out.println("where options are:");
        printOptions(System.out, "    ");
        System.out.println("");

        //REDTAG - Paul Julius - Ideally we would get rid of all calls to
        //  System.exit so that if someone else wraps us in a way we don't
        //  predict now, they don't get snaffued by our calls to kill the VM.
        //  Note ANT and Weblogic utils suffer from similar badness.
        System.exit(0);
    }

    /**
     * Prints the options required by this class. This method can be called by other classes
     * which want to append their own options to a command line client.
     */
    public static void printOptions(PrintStream out, String indent) {
        if (indent == null) {
            indent = "";
        }
        out.println(indent + "-lastbuild timestamp   where timestamp is in yyyyMMddHHmmss format.  note HH is the 24 hour clock.");
        out.println(indent + "-label label           where label is in x.y format, y being an integer.  x can be any string.");
        out.println(indent + "-properties file       where file is the masterbuild properties file, and is available in the classpath");
    }


    /**
     * Gets us the timestamp that we performed
     * a "get" on our source control repository and whether or not the build was 
     * successful.
     */
    public void buildFinished(Project proj, boolean successful) {
        _projectName = proj.getName();

        //If no build was required, because no changes were committed to the
        //  repository, then ModificationSet will have set a property on the
        //  project to indicate as such.
        boolean buildUnnecessary =
                proj.getProperty(ModificationSet.BUILDUNNECESSARY) != null;
        info.setBuildNotNecessary(buildUnnecessary);

        //And if no build was required, then we are done.
        if (info.isBuildNotNecessary()) {
            return;
        }

        //Otherwise, a build was required, so we need to setup some of the
        //  details on the BuildInfo instance.
        info.setUserList(proj.getProperty(ModificationSet.USERS));
        info.setLastBuild(proj.getProperty(ModificationSet.SNAPSHOTTIMESTAMP));
        if (successful) {
            info.setLastBuildSuccessful(true);
            info.setLastGoodBuild(info.getLastBuild());
        } else {
            info.setLastBuildSuccessful(false);
        }

        //Combine all the xml log files output from the build using
        //  a FileMerger.
        FileMerger merger = new FileMerger(proj, props.getAuxLogProperties());
        merger.mergeAuxXmlFiles(proj, info, props.getLogDir());
    }
}
