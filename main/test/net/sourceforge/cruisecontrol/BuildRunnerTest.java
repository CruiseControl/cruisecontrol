/*
 * Copyright 2001 QSI Payments, Inc.
 * All rights reserved.  This precautionary copyright notice against
 * inadvertent publication is neither an acknowledgement of publication,
 * nor a waiver of confidentiality.
 *
 * Identification:
 *	$Id$
 *
 */
package net.sourceforge.cruisecontrol;

import org.apache.tools.ant.Project;
import junit.framework.*;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.XmlLogger;

/**
 * @version $Revision$
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
        assertEquals("test", test.getProject().getName());
        assertTrue(test.getProject().getBuildListeners().contains(logger));
    }
    
    /** Test of getTarget method, of class net.sourceforge.cruisecontrol.BuildRunner. */
    public void testGetTarget() {
        assertNotNull(test.getTarget());
        assertEquals("work", test.getTarget().getName());
    }
    
    /** Test of runBuild method, of class net.sourceforge.cruisecontrol.BuildRunner. */
    public void testRunBuild() {
        assertTrue(test.runBuild());
        assertEquals("test", test.getProject().getProperties().get("testProperty"));
        assertEquals("test", test.getProject().getProperties().get("testInitProperty"));
    }
    
    public void testRunBadBuild() {
        BuildRunner badBuild = new BuildRunner(testFile, "dont-work", "20010710000000", 
                                               "testing.1", logger);
        assertTrue(badBuild.runBuild() == false);
    }
    
}

