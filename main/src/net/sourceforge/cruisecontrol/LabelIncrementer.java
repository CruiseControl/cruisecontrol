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

import org.jdom.Element;

/**
 * This interface defines the method required to increment
 * the label used in the MasterBuild process. This label
 * is incorporated into the log filename when a successful
 * build occurs.
 * 
 * @author <a href="mailto:alden@thoughtworks.com">alden almagro</a>
 * @author <a href="mailto:pj@thoughtworks.com">Paul Julius</a>
 */
public interface LabelIncrementer {

    /**
     * Increments the label when a successful build occurs.
     * The oldLabel should be transformed and returned as
     * the new label.  The build log is also passed in so that some
     * more complex label incrementing can be handled.  For example, a
     * label incrementer could find the ant target that was called and increment based on that
     * information.
     *
     * @param buildLog JDOM <code>Element</code> representation of the build.
     * @param oldLabel Label from previous successful build.
     * @return Label to use for most recent successful build.
     */
    String incrementLabel(String oldLabel, Element buildLog);

    /**
     *  Some implementations of <code>LabelIncrementer</code>, such as those involving
     *  dates, are better suited to being incremented before building rather
     *  than after building.  This method determines whether to increment before
     *  building or after building.
     */
    boolean isPreBuildIncrementer();

    /**
     *  Check the validity of a user-supplied label, making sure that it can be incremented successfully by
     *  the appropriate implementation of <code>LabelIncrementer</code>
     *
     *  @param label user-supplied label
     *  @return true if it is a valid label.
     */
    boolean isValidLabel(String label);

    /**
     * Called by Project when there is no previously serialized label.
     * @return defaultLabel
     */
    String getDefaultLabel();
}
