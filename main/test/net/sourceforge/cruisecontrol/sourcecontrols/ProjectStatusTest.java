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

package net.sourceforge.cruisecontrol.sourcecontrols;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Date;
import java.util.List;

import javax.management.RuntimeErrorException;

import org.jdom2.Element;
import org.junit.Test;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.BuildQueue;
import net.sourceforge.cruisecontrol.Builder;
import net.sourceforge.cruisecontrol.CruiseControlConfig;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.ProjectConfig;
import net.sourceforge.cruisecontrol.ProjectInterface;
import net.sourceforge.cruisecontrol.builders.MockBuilder;
import net.sourceforge.cruisecontrol.testutil.TestUtil.FilesToDelete;
import net.sourceforge.cruisecontrol.util.IO;
import net.sourceforge.cruisecontrol.util.Util;

/**
 * Unit tests for {@link ProjectStatus}
 * @author Dan Tihelka
 */
public class ProjectStatusTest extends TestCase {

    private final FilesToDelete filesToDelete = new FilesToDelete();
    private CruiseControlConfig config;
    private BuildQueue queue;
    private File proj1Data;

    protected void setUp() throws Exception {
        proj1Data = filesToDelete.add(this);
        IO.write(proj1Data, "");

        final String xml = "<cruisecontrol>" 
                         + " <plugin name='mock.builder'   classname='" + MockBuilder.class.getName() + "' />"

                         + " <project name='proj1'>"
                         + "  <modificationset quietperiod='0'>"
                         + "    <filesystem folder='" + proj1Data.getAbsoluteFile() + "'/>"
                         + "  </modificationset>"
                         + "  <schedule interval='1'><mock.builder/></schedule>"
                         + " </project>"

                         + " <project name='proj.main'>"
                         + "  <modificationset quietperiod='0'>"
                         + "    <projectstatus project='proj1' buildonsuccess='true'/>"
                         + "  </modificationset>"
                         + "  <schedule interval='1'><mock.builder/></schedule>"
                         + " </project>"
                         + "</cruisecontrol>";

        // Create the config
        Element ccElement = Util.loadRootElement(new ByteArrayInputStream(xml.getBytes()));
        config = new CruiseControlConfig(ccElement);
        
        // Set the build queue to all projects
        queue = new BuildQueue();
        for (String proj : config.getProjectNames()) {
             ProjectInterface pi = config.getProject(proj);
             // Configure and set the queue
             pi.configureProject();
             pi.setBuildQueue(queue);
             // Prepare *.ser project files for removal
             final File serFile = new File(Builder.getFileSystemSafeProjectName(pi.getName()) + ".ser");
             filesToDelete.add(serFile);
             // clear log files as well
             filesToDelete.add(new File(pi.getLogDir()));
        }
    }

    protected void tearDown() {
        // Stop all projects
        for (String proj : config.getProjectNames()) {
            config.getProject(proj).stop();
        }
        // Clear the rest
        config = null;
        filesToDelete.delete();
    }    
    /**
     * Make sure the validate() method works properly.
     */
    @Test
    public void testValidate() throws Exception {
        ProjectStatus status = new ProjectStatus();
        
        // Project is not set
        assertInvalid(status, "'project' is required for ProjectStatus");

        // Project is set, but it is not valid
        status.setProject("p1");
        assertInvalid(status, "No project named 'p1'");
    }

    /**
     * Tests, if a modification is returned when successful build of <code>proj1</code> is after the
     * last build of the main project. It means that default values are used for all values.
     */
    @Test
    public void testBuildOnSuccess() throws Exception {
        final ProjectStatus status = new ProjectStatus();
        final ProjectConfig proj1 = (ProjectConfig) config.getProject("proj1");
        long lastP1;
        
        // configure the status watcher
        status.setProject("proj1");
        status.validate();
        lastP1 = new Date().getTime();
        // Check the status - proj1 was not build yet. The status must return 'not modified'
        // since the proj1 has not been built yet and we are waiting for its successful build here
        assertNoModifs(status, lastP1 -1000, lastP1 +1000);
        assertNoModifs(status, lastP1 +1000, lastP1 +2000);
        
        // Force the build.
        proj1.start();
        proj1.execute();
        lastP1 = proj1.successLastBuild().getTime();
        
        // the last successful build of proj1 (just made at time lastP1) is after the last build of
        // proj.main. Therefore, it must report "have modification" to trigger the build of proj.main  
        assertModified(status, lastP1 -1000, lastP1 +1000); 
        // the last successful build of proj1 is before the last build of proj.main, so it looks like
        // there is no modification of which should trigger the build of proj.main
        assertNoModifs(status, lastP1 +1000, lastP1 +2000); 
    
        // Write to the file which will introduce modification in proj1 since its last build
        // Sleep ensures that the file modification time will be AFTER lastP1, which is crucial for
        // the test
        Thread.sleep(1100 - lastP1 % 1000);
        IO.write(proj1Data, "test.BuildOnSuccess: " + new Date());

        // The same as the previous check - although there is a modification in proj1 waiting for
        // the build, it is not taken into account here (VetoIfModified was not set)
        assertModified(status, lastP1 -1000, lastP1 +1000); 
        assertNoModifs(status, lastP1 +1000, lastP1 +2000);
        
        // Force the build. The status must return 'has modifications' now
        proj1.execute();
        lastP1 = proj1.successLastBuild().getTime();
        
        // the last successful build of proj1 (just made at time lastP1) is after the last build of
        // proj.main. Therefore, it must report "have modification" to trigger the build of proj.main  
        assertModified(status, lastP1 -1000, lastP1 +1000);
        // the last successful build of proj1 is before the last build of proj.main, so it looks like
        // there is no modification of which should trigger the build of proj.main
        assertNoModifs(status, lastP1 +1000, lastP1 +2000);
    }
    
    /**
     * The same as {@link #testBuildOnSuccess()}, plus veto must be signalized if checked when there
     * is a modification in <code>proj1</code> after its successful build. It means that 
     * {@link ProjectStatus#setVetoIfModified(boolean)} is set to <code>true</code>.
     */
    @Test
    public void testVetoIfModified() throws Exception {
        final ProjectStatus status = new ProjectStatus();
        final ProjectConfig proj1 = (ProjectConfig) config.getProject("proj1");
        long lastP1;
        
        // configure the status watcher
        status.setProject("proj1");
        status.setVetoIfModified(true);
        status.validate();
        lastP1 = new Date().getTime();
        
        // The proj1 has not been built yet and we depend on it. Therefore, our build must be
        // delayed until the successful build of proj1
        assertVeto(status, lastP1 -1000, lastP1 +1000);
        // Here the last build is after ...
        // TODO: DOPSAT!!!!
        assertNoModifs(status, lastP1 +1000, lastP1 +2000);
        
        // Force the build.
        proj1.start();
        proj1.execute();
        lastP1 = proj1.successLastBuild().getTime();
            
        // The same as in testBuildOnSuccess
        assertModified(status, lastP1 -1000, lastP1 +1000); 
        assertNoModifs(status, lastP1 +1000, lastP1 +2000); 
    
        // Write to the file ... (the same as in testBuildOnSuccess)
        Thread.sleep(1100 - lastP1 % 1000);
        IO.write(proj1Data, "test.VetoIfModified: " + new Date());

        // The successful build of proj1 after the last build of proj.main should trigger the build
        // of proj.main. However, there is a modification which must veto the the build.
        assertVeto(status, lastP1 -1000, lastP1 +1000); 
        // The same as in testBuildOnSuccess
        assertNoModifs(status, lastP1 +1000, lastP1 +2000);
        
        // Force the build. The status must return 'has modifications' now
        proj1.execute();
        lastP1 = proj1.successLastBuild().getTime();
        
        // The same as in testBuildOnSuccess
        assertModified(status, lastP1 -1000, lastP1 +1000);
        assertNoModifs(status, lastP1 +1000, lastP1 +2000);
    }
    
    /**
     * The same as {@link #testVetoIfModified()}, except the modifications are not reported. It means
     * that {@link ProjectStatus#setVetoIfModified(boolean)} is set to <code>true</code> and
     * {@link ProjectStatus#setTriggerOnSuccess(boolean)} is set to <code>false</code>.
     */
    @Test
    public void testVetoNoTrigger() throws Exception {
        final ProjectStatus status = new ProjectStatus();
        final ProjectConfig proj1 = (ProjectConfig) config.getProject("proj1");
        long lastP1;
        
        // configure the status watcher
        status.setProject("proj1");
        status.setVetoIfModified(true);
        status.setTriggerOnSuccess(false); // Do not report modifications 
        status.validate();
        lastP1 = new Date().getTime();
        
        // The same as in #testVetoIfModified()
        assertVeto(status, lastP1 -1000, lastP1 +1000);
        // Never returns any modification
        assertNoModifs(status, lastP1 +1000, lastP1 +2000);
        
        // Force the build.
        proj1.start();
        proj1.execute();
        lastP1 = proj1.successLastBuild().getTime();
            
        // Never returns any modification
        assertNoModifs(status, lastP1 -1000, lastP1 +1000); 
        assertNoModifs(status, lastP1 +1000, lastP1 +2000); 
    
        // Write to the file ... (the same as in testBuildOnSuccess)
        Thread.sleep(1100 - lastP1 % 1000);
        IO.write(proj1Data, "test.VetoIfModified: " + new Date());

        // The same as in #testVetoIfModified()
        assertVeto(status, lastP1 -1000, lastP1 +1000); 
        // Never returns any modification
        assertNoModifs(status, lastP1 +1000, lastP1 +2000);
        
        // Force the build. The status must return 'has modifications' now
        proj1.execute();
        lastP1 = proj1.successLastBuild().getTime();
        
        // Never returns any modification
        assertNoModifs(status, lastP1 -1000, lastP1 +1000);
        assertNoModifs(status, lastP1 +1000, lastP1 +2000);
    }
    
    /**
     * The case when no modification and no veto is set, It means that only 
     * {@link ProjectStatus#setTriggerOnSuccess(boolean)} is set to <code>false</code>.
     */
    @Test
    public void testNoTriggerOnSuccess() throws Exception {
        final ProjectStatus status = new ProjectStatus();
        final ProjectConfig proj1 = (ProjectConfig) config.getProject("proj1");
        long lastP1;
        
        // configure the status watcher
        status.setProject("proj1");
        status.setTriggerOnSuccess(false);
        status.validate();
        lastP1 = new Date().getTime();

        // Never returns any modification
        assertNoModifs(status, lastP1 -1000, lastP1 +1000);
        assertNoModifs(status, lastP1 +1000, lastP1 +2000);
        
        // Force the build.
        proj1.start();
        proj1.execute();
        lastP1 = proj1.successLastBuild().getTime();
            
        // Never returns any modification
        assertNoModifs(status, lastP1 -1000, lastP1 +1000); 
        assertNoModifs(status, lastP1 +1000, lastP1 +2000); 
    
        // Write to the file ... (the same as in testBuildOnSuccess)
        Thread.sleep(1100 - lastP1 % 1000);
        IO.write(proj1Data, "test.BuildOnSuccess: " + new Date());

        // Never returns any modification
        assertNoModifs(status, lastP1 -1000, lastP1 +1000); 
        assertNoModifs(status, lastP1 +1000, lastP1 +2000);
        
        // Force the build
        proj1.execute();
        lastP1 = proj1.successLastBuild().getTime();
        
        // Never returns any modification
        assertNoModifs(status, lastP1 -1000, lastP1 +1000);
        assertNoModifs(status, lastP1 +1000, lastP1 +2000);
        
    }    
    
    
    /** Expects that {@link ProjectStatus#getModifications(Date, Date)} returns non-empty array */ 
    private void assertModified(ProjectStatus status, long lastBuild, long now) {
        assertFalse("At least one modification expected, none got", 
                status.getModifications(new Date(lastBuild), new Date(now)).isEmpty());        
    }
    /** Expects that {@link ProjectStatus#getModifications(Date, Date)} returns empty array */ 
    private void assertNoModifs(ProjectStatus status, long lastBuild, long now) {
        final List<Modification> modifs = status.getModifications(new Date(lastBuild), new Date(now));
        assertTrue("No modification expected, " + modifs.size() + " got", modifs.isEmpty());        
    }
    
    /** Expects that {@link ProjectStatus#getModifications(Date, Date)} throws {@link RuntimeErrorException}
     *  exception */ 
    private void assertVeto(ProjectStatus status, long lastBuild, long now) {
        try {
            status.getModifications(new Date(lastBuild), new Date(now));
            fail("Invalid status returned");
        } catch (RuntimeException exc) {
            // OK
        }
    }

    /** Expects that {@link ProjectStatus#validate()} throws {@link CruiseControlException}
     *  exception with the given message */ 
    private void assertInvalid(ProjectStatus status, String message) {
        // Verify log directory is mandatory
        try {
            status.validate();
            fail("Validation passed while it should not");
        } catch (CruiseControlException exc) {
            assertEquals("Wrong exception", message, exc.getMessage());
        }
    }
}
