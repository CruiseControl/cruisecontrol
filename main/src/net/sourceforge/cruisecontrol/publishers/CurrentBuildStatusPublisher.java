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
package net.sourceforge.cruisecontrol.publishers;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Publisher;
import net.sourceforge.cruisecontrol.util.CurrentBuildFileWriter;
import net.sourceforge.cruisecontrol.util.XMLLogHelper;
import org.jdom.Element;

import java.util.Date;

public class CurrentBuildStatusPublisher implements Publisher {
    private String fileName;

    public void setFile(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Called after the configuration is read to make sure that all the mandatory parameters
     * were specified..
     *
     * @throws CruiseControlException if there was a configuration error.
     */
    public void validate() throws CruiseControlException {
        if (fileName == null) {
            throw new CruiseControlException("'filename' is required for CurrentBuildStatusPublisher");
        }
        CurrentBuildFileWriter.validate(fileName);
    }

    public void publish(Element cruisecontrolLog) throws CruiseControlException {
        XMLLogHelper helper = new XMLLogHelper(cruisecontrolLog);
        long interval = Long.parseLong(helper.getCruiseControlInfoProperty("interval"));
        writeFile(new Date(), interval);
    }

    protected void writeFile(Date date, long interval) throws CruiseControlException {
        Date datePlusInterval = new Date(date.getTime() + (interval * 1000));
        CurrentBuildFileWriter.writefile("<span class=\"link\">Next Build Starts At:<br>", datePlusInterval, fileName);
    }
}
