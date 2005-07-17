/****************************************************************************
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
****************************************************************************/

package net.sourceforge.cruisecontrol.distributed.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

public final class PropertiesHelper {

    private static final Logger LOG = Logger.getLogger(PropertiesHelper.class);

    public static final String DISTRIBUTED_MODULE = "distributed.module";
    public static final String DISTRIBUTED_AGENT_LOGDIR = "distributed.agentlogdir";
    public static final String DISTRIBUTED_AGENT_OUTPUTDIR = "distributed.agentoutputdir";

    public static final String RESULT_TYPE_LOGS = "logs";
    public static final String RESULT_TYPE_OUTPUT = "output";

    private PropertiesHelper() { }

    public static Map loadOptionalProperties(String filename) {
        Properties optionalProperties = new Properties();
        try {
            optionalProperties = (Properties) loadRequiredProperties(filename);
        } catch (RuntimeException e) {
            LOG.warn("Failed to load optional properties file '" + filename + "'");
            System.out.println("Failed to load optional properties file '" + filename + "'");
        }
        return optionalProperties;
    }

    public static Map loadRequiredProperties(String filename) throws RuntimeException {
        Properties requiredProperties = new Properties();
        try {
            InputStream fileStream = ClassLoader.getSystemResourceAsStream(filename);
            requiredProperties.load(fileStream);
        } catch (NullPointerException e) {
            throw new RuntimeException("Failed to load required properties file '" + filename + "'", e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load required properties file '" + filename + "'", e);
        }
        return requiredProperties;
    }

}
