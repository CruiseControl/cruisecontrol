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

/**
 * Defines the interface including those attributes exposed by the
 * CruiseControl main process.
 * 
 * @author <a href="mailto:pj@thoughtworks.com">Paul Julius</a>
 * @author <a href="mailto:jcyip@thoughtworks.com">Jason Yip</a>
 */
public interface JMXControllerMBean {

    /**
     * Sets the number of seconds in the build interval.
     * 
     * @param buildInterval
     *               Number of seconds.
     */
    public void setBuildIntervalSeconds(long buildInterval);

    /**
     * Returns the number of seconds in the build interval.
     * 
     * @return Number of seconds.
     */
    public long getBuildIntervalSeconds();

    /**
     * Returns the number of times a build has been attempted, which should
     * include a repository check at the beginning of each.
     * 
     * @return Number of build attempts.
     */
    public long getRepositoryCheckCount();

    /**
     * Returns the duration the managed process has been executing.
     * 
     * @return Execution duration.
     */
    public String getUpTime();

    /**
     * Returns the number of successful builds performed by the managed
     * process.
     * 
     * @return Successful build count.
     */
    public long getSuccessfulBuildCount();

    /**
     * Tells the controlled process to run as soon as possible.
     */
    public void runAsSoonAsPossible();

    /**
     * Pauses the controlled process.
     */
    public void pause();

    /**
     * Resumes the controlled process.
     */
    public void resume();

    /**
     * Stops the controlled process, including any other support processes that may have been started.
     */
    public void stop();
}
