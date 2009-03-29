package net.sourceforge.cruisecontrol.jmx;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.ModificationSet;
import net.sourceforge.cruisecontrol.Project;
import net.sourceforge.cruisecontrol.ProjectConfig;
import net.sourceforge.cruisecontrol.SourceControl;
import net.sourceforge.cruisecontrol.MockProject;
import net.sourceforge.cruisecontrol.ProjectTest;
import net.sourceforge.cruisecontrol.util.IO;
import net.sourceforge.cruisecontrol.util.Util;
import net.sourceforge.cruisecontrol.bootstrappers.AntBootstrapper;
import net.sourceforge.cruisecontrol.labelincrementers.DefaultLabelIncrementer;

public class ProjectControllerTest extends TestCase {

    public void testShouldBeAbleToGetCommitMessage() throws Exception {
        Project project = new Project();
        project.setName("TestProject");
        ModificationSet modicationSet = new ModificationSet();
        modicationSet.add(new SVNStub());
        ProjectConfig projectConfig = new ProjectConfig();
        projectConfig.add(new DefaultLabelIncrementer());
        projectConfig.add(modicationSet);

        project.setProjectConfig(projectConfig);

        ProjectMBean controller = new ProjectController(project);
        String[][] message = controller.commitMessages();
        assertEquals(message[0][0], "user1");
        assertEquals(message[0][1], "comment1");
        assertEquals(message[1][0], "user2");
        assertEquals(message[1][1], "comment2");
    }

    public void testShouldRetrieveBuildOutputWhenProjectIsBuilding() throws Exception {
        final Project project = new Project();
        project.setName("project1");

        final File validFile = new File("project1");
        Util.doMkDirs(validFile);
        try {
            final AntBootstrapper bootstrapper = new AntBootstrapper();

            bootstrapper.setBuildFile(validFile.getAbsolutePath());

            final File expectedTempFile = new File(validFile, "notLog.xml");
            bootstrapper.setTempFile(expectedTempFile.getName());

            bootstrapper.setTarget("init");
            bootstrapper.setAntWorkingDir(validFile.getAbsolutePath());
            bootstrapper.validate();
            try {
                bootstrapper.bootstrap();
            } catch (CruiseControlException e) {
                assertEquals("ant logfile " + expectedTempFile.getAbsolutePath() + " does not exist.", e.getMessage());
            }

            final ProjectMBean mbean = new ProjectController(project);
            String[] output = mbean.getBuildOutput(0);
            assertNotNull(output);
            assertEquals("AntBuilder/Bootstrapper/Publisher only create build output if useLogger, showOutput are true",
                    0, output.length);


            IO.delete(validFile);
            Util.doMkDirs(validFile);
    
            bootstrapper.setUseLogger(true);
            bootstrapper.setShowAntOutput(true);
            bootstrapper.setProgressLoggerLib("dummyLib");
            try {
                bootstrapper.bootstrap();
            } catch (Exception e) {
                assertEquals("ant logfile " + expectedTempFile.getAbsolutePath()
                    + " is empty. Your build probably failed. Check your CruiseControl logs.", e.getMessage());
            }

            output = mbean.getBuildOutput(0);
            assertNotNull(output);
            assertTrue("Unexpected empty build output", output.length > 0);

        } finally {
            IO.delete(validFile);
        }
    }


    public void testIsLastBuildSuccessful() {
        final ProjectController mbean = new ProjectController(new Project());
        assertTrue(mbean.isLastBuildSuccessful());
    }

    public void testIsLastBuildSuccessfulFromMockProjectFollowsValueChanges() throws Exception {
        final MockProject project = new MockProject();
        final ProjectController mbean = new ProjectController(project);

        ProjectTest.setWasLastBuildSuccessful(project, false);
        assertFalse(mbean.isLastBuildSuccessful());

        ProjectTest.setWasLastBuildSuccessful(project, true);
        assertTrue(mbean.isLastBuildSuccessful());

        ProjectTest.setWasLastBuildSuccessful(project, false);
        assertFalse(mbean.isLastBuildSuccessful());
    }



    private class SVNStub implements SourceControl {
        private static final long serialVersionUID = 1L;

        public List<Modification> getModifications(Date lastBuild, Date now) {
            final List<Modification> modications = new ArrayList<Modification>();
            Modification modification = new Modification();
            modification.userName = "user1";
            modification.comment = "comment1";
            modications.add(modification);
            modification = new Modification();
            modification.userName = "user2";
            modification.comment = "comment2";
            modications.add(modification);
            return modications;
        }

        public Map<String, String> getProperties() {
            return null;
        }

        public void validate() throws CruiseControlException {
        }
    }
}
