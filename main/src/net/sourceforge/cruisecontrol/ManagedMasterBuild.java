package net.sourceforge.cruisecontrol;

import java.io.IOException;
import java.util.Date;

/**
 * Extends the MasterBuild process in CruiseControl to allow the process
 * to be dynamically managed by the JMX wrapper. It changes the main
 * process loop to allow for intermittent checking of different attributes,
 * like the build interval, for changes.
 * 
 * @author <a href="mailto:pj@thoughtworks.com">Paul Julius</a>
 * @author <a href="mailto:jcyip@thoughtworks.com">Jason Yip</a>
 */
public class ManagedMasterBuild extends MasterBuild implements Runnable {

    /**
     * Number of times CruiseControl has checked the repository. Each time
     * a build starts the repository should be checked. This number should
     * be a good indication of the number of times CruiseControl wakes up
     * and does something.
     */
    protected long reposCheckCount = 0;
    
    /**
     * Timestamp for when CruiseControl was started. This value can be used
     * to compute the amount of time that CruiseControl was running.
     */
    protected final Date processStartTime = new Date();
    
    /**
     * Indicates whether or not the build loop should execute as soon as possible.
     */
    protected boolean shouldRunAsap = false;
    
    /**
     * Indicates whether or not the user paused the CruiseControl process.
     */
    protected boolean paused = false;
    
    /**
     * Constructs a new instance. User arguments may be provided to control
     * the same user set attributes controlled by the MasterBuild superclass.
     * 
     * @param args   Arguments to specify user variables.
     */
    public ManagedMasterBuild(String[] args) {
        super(args);
        //When managed, the properties file will only be loaded once.
        try {
            props = new CruiseControlProperties(_propsFileName);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            System.err.println("Error loading properties file. Stopping.");
            System.exit(-1);
        }
    }


    /**
     * Method required by the Thread class which kicks off the main
     * CruiseControl process loop.
     */
    public void run() {
        execute();
    }

    /**
     * Overrides the execute loop from the superclass while providing
     * the ability to periodically check different attributes for changes.
     * This execute loop allows finer control over the process.
     */
    public void execute() {
        try {
            _buildCounter = 0;
            while (true) {
                Date startTime = new Date();
                startLog();
                reposCheckCount++;
                performBuild();

                handleSleeping(startTime);
            }

        } catch (InterruptedException e) {
            log("Exception trying to sleep");
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles the complex sleep behavior required while periodically
     * checking state changes in control attributes, like build interval.
     * 
     * @param startTime Time the previous build attempt started.
     * @exception InterruptedException
     */
    private void handleSleeping(Date startTime) throws InterruptedException {
        long goToSleepTime = System.currentTimeMillis();
        long origWakeUpTime = getWakeUpTime(startTime.getTime(), goToSleepTime);
        endLog(origWakeUpTime - System.currentTimeMillis());

        long wakeUpTime = origWakeUpTime;
        while (paused
               || (System.currentTimeMillis() < wakeUpTime
                    && !shouldRunAsap )) {
            Thread.sleep(100);
            wakeUpTime = getWakeUpTime(startTime.getTime(), goToSleepTime);
        }
        if (origWakeUpTime != wakeUpTime) {
            System.out.println("Build interval changed to " + props.getBuildInterval() + " while sleeping.");
        }
        if (shouldRunAsap) {
            System.out.println("Was told to run as soon as possible. Running now.");
            shouldRunAsap = false;
        }
        long actualSleepSeconds = (System.currentTimeMillis() - goToSleepTime)/1000;
        System.out.println("Actually slept for " + actualSleepSeconds + " seconds.");
    }

    /**
     * Returns the time when the CruiseControl process should wake-up.
     * 
     * @param startTime Time the last build attempt started.
     * @param goToSleepTime
     *                  Time the CruiseControl process went, or will go, to sleep.
     * @return The time when the process should wake up.
     */
    protected long getWakeUpTime(long startTime, long goToSleepTime) {
        long buildInterval = props.getBuildInterval();
        if (props.isIntervalAbsolute()) {
            return startTime + buildInterval;
        } else {
            return goToSleepTime + buildInterval;
        }
    }

    /**
     * Returns the number of times a build was attempted.
     * 
     * @return Number of times the build was attempted.
     */
    public long getRepositoryCheckCount() {
        return reposCheckCount;
    }

    /**
     * Returns a formatted String representing the duration that the CruiseControl
     * process has been running.
     * 
     * @return String formatted for display purposes.
     */
    public String getUpTime() {
        long upTimeMilliseconds = System.currentTimeMillis() - processStartTime.getTime();
        long millis = upTimeMilliseconds % 1000;
        long seconds = upTimeMilliseconds/1000;
        long minutes = seconds / 60;
        seconds = seconds - (minutes * 60);
        long hours = minutes / 60;
        minutes = minutes - (hours * 60);
        long days = hours / 24;
        hours = hours - (days * 24);
        
        return (days <= 0 ? "" : days + " days ")
            + (hours <= 0 ? "" : hours + " hours ")
            + (minutes <= 0 ? "" : minutes + " minutes ")
            + (seconds <= 0 ? "" : seconds + " seconds ") 
            + millis + " milliseconds";
    }

    /**
     * Returns the number of successful builds performed while this CruiseControl process
     * was running.
     * 
     * @return Number of successful builds.
     */
    public long getSuccessfulBuildCount() {
        return _buildCounter;
    }

    /**
     * Tells this CruiseControl process to run as soon as possible.
     */
    public void buildAsap() {
        shouldRunAsap = true;
    }

    /**
     * Tells this CruiseControl process to pause until resume is called. The pause
     * may not occur until after the current build completes.
     */
    public void pause() {
        if (paused) {
            System.out.println("Already paused.");
            return;
        }
        System.out.println("Will pause as soon as possible.");
        paused = true;
    }

    /**
     * Tells this CruiseControl instance to resume, if paused.
     */
    public void resume() {
        if (!paused) {
            System.out.println("Not paused. Still running.");
            return;
        }
        System.out.println("Will resume as soon as possible.");
        paused = false;
    }
}
