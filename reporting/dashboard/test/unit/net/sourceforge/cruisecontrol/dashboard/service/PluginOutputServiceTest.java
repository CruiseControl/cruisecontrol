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
package net.sourceforge.cruisecontrol.dashboard.service;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.dashboard.BuildDetail;
import net.sourceforge.cruisecontrol.dashboard.Configuration;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;
import net.sourceforge.cruisecontrol.util.OSEnvironment;
import org.apache.commons.lang.StringUtils;

public class PluginOutputServiceTest extends TestCase {

    private PluginOutputService service;
    private BuildDetail buildDetail;

    protected void setUp() throws Exception {
        Configuration configuration = new Configuration(new ConfigXmlFileService(new OSEnvironment()));
        configuration.setCruiseConfigLocation(DataUtils.getConfigXmlAsFile().getAbsolutePath());
        service = new PluginOutputService(configuration);
        Map props = new HashMap();
        props.put("projectname", "project1");
        props.put("logfile",
                new File(DataUtils.getConfigXmlAsFile().getParent() + "/logs/project1/log20051209122103.xml"));
        buildDetail = new BuildDetail(props);
    }

    protected void tearDown() throws Exception {
        buildDetail.getPluginOutputs().clear();
    }

    public void testShouldBeAbleToReturnEmptyListWhenCfgIsNotDefined() throws Exception {
        String wrongLocation = DataUtils.getPassingBuildLbuildAsFile().getAbsolutePath();
        Configuration configuration = new Configuration(new ConfigXmlFileService(new OSEnvironment()));
        configuration.setCruiseConfigLocation(wrongLocation);
        service = new PluginOutputService(configuration);
        service.mergePluginOutput(buildDetail, new HashMap());
        assertEquals(0, buildDetail.getPluginOutputs().size());
    }

    public void testShouldIngoreNotExistClassAndConintueInitiailizeTheResult() throws Exception {
        service.mergePluginOutput(buildDetail, new HashMap());
        assertEquals(2, buildDetail.getPluginOutputs().size());
    }

    public void testShouldBeAbleToReturnInitializedServiceWhenCfgIsDefined() throws Exception {
        service.mergePluginOutput(buildDetail, new HashMap());
        assertEquals(2, buildDetail.getPluginOutputs().size());
        assertTrue(buildDetail.getPluginOutputs().containsKey("Merged Check Style"));
        String content = (String) buildDetail.getPluginOutputs().get("Merged Check Style");
        assertTrue(StringUtils.contains(content, "Line has trailing spaces."));
    }
}
