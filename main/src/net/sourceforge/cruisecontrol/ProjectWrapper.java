/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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

import net.sourceforge.cruisecontrol.util.threadpool.WorkerThread;

import org.apache.log4j.Logger;

/**
 * A thin wrapper around a Project object
 *  all it is designed to do is run the Project in it's own thread
 * 
 * @author Jared Richardson
 */

public class ProjectWrapper implements WorkerThread {

  private Project myProject = null;
  private static final Logger LOG = Logger.getLogger(ProjectWrapper.class);

  public ProjectWrapper(Project thisProject) {
    LOG.debug("Project " + thisProject.getName() + " is being wrapped");
    myProject = thisProject;
  }

  public void run() {
    if (myProject == null) {
      return;
    }
    LOG.debug("executing project " + myProject.getName());
    myProject.execute();
  }

  public Object getResult() {
    if (myProject.getState() == ProjectState.IDLE || myProject.getState() == ProjectState.WAITING) {
      return "true";
    } else {
      return null;
    }
  }

  public void terminate() {
    myProject.stop();
  }

  public String getName() {
    return myProject.getName();
  }

  public void setName(String newName) {
    myProject.setName(newName);
  }
}
