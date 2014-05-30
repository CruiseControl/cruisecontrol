/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol;

import java.util.Date;
import java.util.List;
import java.util.Map;


/**
 * Interface accessing the state of a project.
 * @author <a href="mailto:dtihelka@gmail.com">Dan Tihelka</a>
 */
public interface ProjectQuery {

    /**
     * @return the name of the project
     */
    String getName();
    /**
     * @return the path where log files are stored
     */
    String getLogDir();

    /**
     * @return the unchangeable set of project properties
     */
    Map<String, String> getProperties();

    /**
     * @return the unchangeable list of modifications since the last successful build of the
     *      project
     */
    List<Modification> modificationsSinceLastBuild();

    /**
     * @return the unchangeable list of modifications since the given date/time, or empty list if
     *     this information cannot be retrieved.
     * @param since the instant since the modifications are required
     */
    List<Modification> modificationsSince(Date since);

    /**
     * @return the date of the last successful build. If the project has not been built yet,
     *   returns <code>Date(0)</code> instance.
     */
    Date successLastBuild();
    /**
     * @return the label of the last successful build. If the project has not been built yet,
     *   returns empty string.
     */
    String successLastLabel();
    /**
     * @return the name of the log file with the last successful build info. If the project has
     *   not been built yet, returns empty string.
     */
    String successLastLog();
}
