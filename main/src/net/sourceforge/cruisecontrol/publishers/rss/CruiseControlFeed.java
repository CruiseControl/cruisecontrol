/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2005, ThoughtWorks, Inc.
 * 200 E. Randolph, 25th Floor
 * Chicago, IL 60601 USA
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
package net.sourceforge.cruisecontrol.publishers.rss;

import java.io.File;

/**
 *  The CruiseControlFeed class extends the generic Feed class with
 *  CruiseControl-specific functionality.  Specifically, the CruiseControlFeed
 *  class keeps track of how many projects are being published into the feed
 *  and uses this information to generate a default feed title and description.
 *
 * Copyright (c) 2005 Hewlett-Packard Development Company, L.P.
 * Licensed under the CruiseControl BSD license
 *  @author Patrick Conant
 */
public class CruiseControlFeed extends Feed {

    /**
     * Project count indicates how many CC projects are using this feed.
     * This affects the link URL, title and description of the feed.
     */
    private int projectCount;

    private String projectName;

    /**
     *  Constructor.
     */
    public CruiseControlFeed(File publishToFile) {
        super(publishToFile);
    }

    /**
     *  incrementProjectCount is called by the RSSPublisher when a new project
     *  is feeding into this Feed instance.
     */
    public void incrementProjectCount() {
        this.projectCount++;
    }

    /**
     *  getProjectCount is a convenience method used internally to determine
     *  whether multiple projects are publishing into the same feed.
     */
    public int getProjectCount() {
        return this.projectCount;
    }

    /**
     *  Set the name of the project being published into this feed.
     */
    public void setProjectName(String name) {
        if (isNotEmpty(this.projectName)) {
            this.projectName = this.projectName + ", " + name;
        } else {
            this.projectName = name;
        }
    }

    /**
     *  Return the name of the project(s) that are publishing into this feed.
     */
    public String getProjectName() {
        return this.projectName;
    }

    /**
     *  Generate a description of this feed based on the names of the projects
     *  flowing into the feed.  This method over-rides the base Feed class'
     *  getDescription() method.
     */
    public String getDescription() {
        if (isNotEmpty(super.getDescription())) {
            return super.getDescription();
        } else if (isNotEmpty(this.projectName)) {
            return "Automated build results for CruiseControl project(s) " + this.projectName;
        } else {
            return "Automated build results for CruiseControl.";
        }
    }

    /**
     *  Generate a title of this feed based on the names of the projects
     *  flowing into the feed.This method over-rides the base Feed class'
     *  getTitle() method.
     */
    public String getTitle() {
        if (isNotEmpty(super.getTitle())) {
            return super.getTitle();
        } else {
            return "CruiseControl Build Results";
        }
    }

    private static boolean isNotEmpty(String string) {
        return string != null && string.trim().length() > 0;
    }
}



