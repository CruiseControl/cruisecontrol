/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 600
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

import java.util.Date;

/**
 * @author jfredrick
 */
public class MockProject extends Project {

    private int buildCount = 0;
    private Date lastBuild;
    private static final long ONE_SECOND = 1000;
    private boolean keepLooping = false;
    private int loopCount = 0;
    private ProjectState mockState;

    public ProjectState getState() {
        if (mockState == null) {
            return super.getState();
        }

        return mockState;
    }

    void setMockState(ProjectState newState) {
        mockState = newState;
    }

    public void execute() {
        buildCount++;
        lastBuild = new Date();
        try {
            Thread.sleep(ONE_SECOND);
        } catch (InterruptedException e) {
            String message = "MockProject.execute() interrupted";
            System.out.println(message);
            throw new RuntimeException(message);
        }
    }

    public int getBuildCount() {
        return buildCount;
    }

    public Date getLastBuildDate() {
        return lastBuild;
    }

    void loop() {
        loopCount = 0;
        keepLooping = true;
        while (keepLooping) {
            loopCount++;
            try {
                checkWait();
                Thread.sleep(50);
            } catch (InterruptedException e) {
                String message = "MockProject.loop() interrupted";
                throw new RuntimeException(message);
            }
        }
    }

    void checkWait() throws InterruptedException {
    }

    void stopLooping() {
        keepLooping = false;
    }

    int getLoopCount() {
        return loopCount;
    }

    /*
     * don't do anything
     * 
     * @see net.sourceforge.cruisecontrol.Project#checkLogDirectory()
     */
    protected void checkLogDirectory() {
    }

}