/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2007, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.publishers.email;

import java.io.File;
import java.util.Properties;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Logger;

/*
 * Mapper that extracts email addresses out of the Maven1 project.xml or
 * the Maven2 pom.xml. It creates a properties instance out of the developer
 * nodes in the POM and then maps the id of the given user to the email address.
 */
public class MavenMapper extends EmailAddressMapper {

    private static final long serialVersionUID = -2121211825257529130L;
    private static final Logger LOG = Logger.getLogger(MavenMapper.class);
    private String projectFile = null;
    private Properties props = new Properties();

    public MavenMapper() {
        super();
    }

    public void setProjectFile(String projectFile) {
        this.projectFile = projectFile;
    }

    /**
     * @see net.sourceforge.cruisecontrol.publishers.email.EmailAddressMapper#open()
     */
    public void open() throws CruiseControlException {
        MavenMapperHelper mmh = new MavenMapperHelper(this.projectFile);
        this.props = mmh.getDeveloperPropertySet();
        LOG.debug("DeveloperPropertySet: " + this.props);
    }

    /**
     * Check if plugin has been configured properly.
     * @throws net.sourceforge.cruisecontrol.CruiseControlException If the pluing isn't valid.
     */
    public void validate() throws CruiseControlException {
        File f = new File(projectFile);
        ValidationHelper.assertIsSet(projectFile, "projectFile", getClass());
        ValidationHelper.assertFalse(projectFile.equals(""), "empty string is not a valid value of projectFile for "
                + getClass().getName());
        ValidationHelper.assertExists(f, "projectFile", getClass());
        ValidationHelper.assertIsReadable(f, "projectFile", getClass());
        // this check could also be moved to ValidationHelper
        if (!f.isFile()) {
            throw new CruiseControlException(projectFile + " is not a file");
        }
    }

    /**
     * The actual purpose of the plugin: Map a user id (=id out of the Maven POM) to the
     * respective email address.
     * @see net.sourceforge.cruisecontrol.publishers.email.EmailAddressMapper#mapUser(java.lang.String)
     */
    public String mapUser(String user) {
        return this.props.getProperty(user + ".email");
    }
}
