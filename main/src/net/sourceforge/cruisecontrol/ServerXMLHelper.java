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

import net.sourceforge.cruisecontrol.util.Util;
import org.apache.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.DataConversionException;
import org.jdom2.Element;

import java.io.File;

/**
 * @author Jared Richardson jared.richardson@sas.com
 *         <p>
 *         this routine encapsulates the config file for the application.
 *         it fetches a value from the config file and handles errors.
 */

public class ServerXMLHelper {
    private static final Logger LOG = Logger.getLogger(ServerXMLHelper.class);

    private int numThreads = 1;

    public ServerXMLHelper(File configFile) {
        try {
            Element configLogElement = Util.loadRootElement(configFile);
            Element systemElement = configLogElement.getChild("system");
            if (systemElement == null) {
                LOG.debug("no system element found in config.xml");
                return;
            }
            Element configurationElement = systemElement.getChild("configuration");
            if (configurationElement == null) {
                LOG.debug("no configuraiton element found in config.xml");
                return;
            }
            Element threadElement = configurationElement.getChild("threads");
            if (threadElement == null) {
                LOG.debug("no threads element found in config.xml");
                return;
            }
            Attribute threadCount = threadElement.getAttribute("count");
            LOG.debug("count attribute on threads is " + threadCount.toString());
            try {
                numThreads = threadCount.getIntValue();
            } catch (DataConversionException dce) {
                LOG.error("Expected a numeric value for system-configuration-threads-count in config.xml but found "
                        + threadCount.toString(), dce);
                numThreads = 1;
            }
        } catch (Exception e) {
            LOG.warn("error parsing thread count from config file; defaulting to 1 thread.", e);
            numThreads = 1;
        }
    }

    /**
     * @return int the number of threads that CruiseControl should allow to run at the same time
     *         to process pending build requests
     */
    public int getNumThreads() {
        return numThreads;
    }
}
