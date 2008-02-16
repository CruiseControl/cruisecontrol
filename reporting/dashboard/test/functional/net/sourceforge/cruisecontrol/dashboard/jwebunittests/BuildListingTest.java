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
package net.sourceforge.cruisecontrol.dashboard.jwebunittests;

public class BuildListingTest extends BaseFunctionalTest {

    public void testShouldListAllBuildsOfProject() throws Exception {
        tester.beginAt("/project/list/all/project1");
        tester.assertTextPresent("build.489");
    }

    public void testShouldShowCommitMessageForSuccessfulBuild() throws Exception {
        tester.beginAt("/project/list/passed/project1");
        tester.assertTextPresent("project1");
        tester.assertTextPresent("readcb");
        tester.assertTextPresent("project name changed to cache");
        tester.assertLinkPresentWithExactText("story123");
    }

    public void testShouldNOTShowCommitMessageIfNoAnySuccessfulBuild() throws Exception {
        tester.beginAt("/project/list/passed/project2");
        tester.assertTextPresent("project2");
        tester.assertTextPresent("No successful builds found");
    }

    public void testShouldListProjectNameWithSpace() throws Exception {
        tester.beginAt("/project/list/passed/project%20space");
        tester.assertTextPresent("project space");
        tester.beginAt("/project/list/all/project%20space");
        tester.assertTextPresent("project space");
    }
}
