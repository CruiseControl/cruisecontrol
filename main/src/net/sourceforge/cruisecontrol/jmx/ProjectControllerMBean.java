// DON'T TOUCH THIS SECTION
//
// COPYRIGHT isMobile.com AB 2000
//
// The copyright of the computer program herein is the property of
// isMobile.com AB, Sweden. The program may be used and/or copied
// only with the written permission from isMobile.com AB or in the
// accordance with the terms and conditions stipulated in the
// agreement/contract under which the program has been supplied.
//
// $Id$
//
// END DON'T TOUCH

package net.sourceforge.cruisecontrol.jmx;


public interface ProjectControllerMBean {

    /**
     * Pauses the controlled process.
     */
    public void pause();

    /**
     * Resumes the controlled process.
     */
    public void resume();

    /**
     * Returns the duration the managed process has been executing.
     * 
     * @return Execution duration.
     */
    public long getUpTime();

    /**
     * Returns the number of successful builds performed by the managed
     * process.
     * 
     * @return Successful build count.
     */
    public long getSuccessfulBuildCount();

    /**
     * Is the project paused?
     * 
     * @return Pause state
     */
    public boolean isPaused();

}
