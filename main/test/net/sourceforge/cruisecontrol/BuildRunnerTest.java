/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 500
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

import org.apache.tools.ant.Project;
import junit.framework.*;
import org.apache.tools.ant.XmlLogger;

/**
 * @version Revision: 1.1.1
 */
public class BuildRunnerTest extends TestCase {

    private java.io.File testFile;
    private BuildRunner test;
    private CruiseLogger logger = new CruiseLogger(Project.MSG_INFO);
    
    public BuildRunnerTest(java.lang.String testName) {
        super(testName);
    }
    
    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite(BuildRunnerTest.class);
        
        return suite;
    }

    public void setUp() throws java.io.IOException {
        // Copy the contents of the test build (available as a resource stream) to a 
        // temporary file so that Ant can access it.
        testFile = java.io.File.createTempFile("cct", ".xml");
        testFile.deleteOnExit();

        java.io.InputStream buildStream = this.getClass().getResourceAsStream("TestBuild.xml");
        java.io.BufferedInputStream bufferedIn = new java.io.BufferedInputStream(buildStream);
        
        java.io.FileOutputStream fileStream = new java.io.FileOutputStream(testFile);
        java.io.BufferedOutputStream bufferedOut = new java.io.BufferedOutputStream(fileStream);
        
        boolean stillReading = true;
        byte[] data = new byte[1024];
        while (stillReading) {
            int dataRead = bufferedIn.read(data, 0, 1024);
            if (dataRead == -1) {
                stillReading = false;
            }
            else {
                bufferedOut.write(data, 0, dataRead);
            }
        }
        bufferedOut.close();
        fileStream.close();
        bufferedIn.close();
        buildStream.close();
        
        test = new BuildRunner(testFile, "work", "20010710000000", "testing.1", logger);
    }
    
    /** Test of getProject method, of class net.sourceforge.cruisecontrol.BuildRunner. */
    public void testGetProject() {
        assertNotNull(test.getProject());
        assertTrue(test.getProject().getBuildListeners().contains(logger));
    }
    
    /** Test of runBuild method, of class net.sourceforge.cruisecontrol.BuildRunner. */
    public void testRunBuild() throws Throwable {
        test.runBuild();
        if (test.getError() != null) {
            throw test.getError();
        }
        assertEquals("test", test.getProject().getName());
        assertEquals("test", test.getProject().getProperties().get("testProperty"));
        assertEquals("test", test.getProject().getProperties().get("testInitProperty"));
    }
    
    public void testRunBadBuild() {
        BuildRunner badBuild = new BuildRunner(testFile, "dont-work", "20010710000000", 
                                               "testing.1", logger);
        assertTrue(badBuild.runBuild() == false);
    }
    
}

