package net.sourceforge.cruisecontrol.jmx;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.BuildQueue;
import net.sourceforge.cruisecontrol.BuildQueueTest;
import net.sourceforge.cruisecontrol.BuilderTest;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Log;
import net.sourceforge.cruisecontrol.MockProject;
import net.sourceforge.cruisecontrol.MockSchedule;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.ModificationSet;
import net.sourceforge.cruisecontrol.Progress;
import net.sourceforge.cruisecontrol.ProgressImplTest;
import net.sourceforge.cruisecontrol.Project;
import net.sourceforge.cruisecontrol.ProjectConfig;
import net.sourceforge.cruisecontrol.ProjectConfigTest;
import net.sourceforge.cruisecontrol.ProjectTest;
import net.sourceforge.cruisecontrol.SourceControl;
import net.sourceforge.cruisecontrol.builders.AntBuilder;
import net.sourceforge.cruisecontrol.builders.AntOutputLogger;
import net.sourceforge.cruisecontrol.builders.AntScriptTest;
import net.sourceforge.cruisecontrol.events.BuildResultEvent;
import net.sourceforge.cruisecontrol.events.BuildResultListener;
import net.sourceforge.cruisecontrol.util.IO;
import net.sourceforge.cruisecontrol.util.Util;
import net.sourceforge.cruisecontrol.bootstrappers.AntBootstrapper;
import net.sourceforge.cruisecontrol.labelincrementers.DefaultLabelIncrementer;
import net.sourceforge.cruisecontrol.sourcecontrols.AlwaysBuild;

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

    public void testBootstrapperBuildOutputWhenProjectIsBuilding() throws Exception {
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
            assertFalse("Null data file should NOT have appended anything to ID: " + mbean.getOutputLoggerID(),
                    mbean.getOutputLoggerID().endsWith(AntOutputLogger.DEFAULT_OUTFILE_NAME));

            IO.delete(validFile);
            Util.doMkDirs(validFile);

            bootstrapper.setUseLogger(true);
            bootstrapper.setLiveOutput(true);
            bootstrapper.setProgressLoggerLib("dummyLib");
            try {
                bootstrapper.bootstrap();
            } catch (Exception e) {
                assertEquals("ant logfile " + expectedTempFile.getAbsolutePath()
                    + " is empty. Your build probably failed. Check your CruiseControl logs.", e.getMessage());
            }

            output = mbean.getBuildOutput(0);
            assertNotNull(output);
            // @todo this test will likely change if we implement 'live output' for bootstrappers
            //assertTrue("Unexpected empty bootstrapper build output", output.length > 0);
            assertEquals("Expected empty bootstrapper build output", 0, output.length);

        } finally {
            IO.delete(validFile);
        }
    }


    public void testBuilderShouldRetrieveBuildOutputWhenProjectIsBuilding() throws Exception {
        final Project project = new Project();
        project.setName("project1");

        final File validFile = new File("project1");
        Util.doMkDirs(validFile);
        try {
            final AntBuilder antBuilder = new AntBuilder();

            antBuilder.setBuildFile(validFile.getAbsolutePath());

            final File expectedTempFile = new File(validFile, "notLog.xml");
            antBuilder.setTempFile(expectedTempFile.getName());

            antBuilder.setTarget("init");
            antBuilder.setAntWorkingDir(validFile.getAbsolutePath());
            antBuilder.validate();
            
            // default value of LiveOutput is different between AntBuilder and AntBootstrapper,
            // must set to false to avoid output logging.
            antBuilder.setLiveOutput(false);
            final Map<String, String> buildProperties = BuilderTest.createPropsWithProjectName(project.getName());
            try {
                antBuilder.build(buildProperties, null);
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

            final String target = "echoProgress";
            final File buildFile = new File(validFile, "testBuild.xml");
            stringToFile("<project name=\"testProj\">\n"
                    + "<target name=\"" + target + "\">\n"
                    + "<echo>" + AntScriptTest.MSG_PREFIX_ANT_PROGRESS + "some stuff</echo>\n"
                    + "</target>\n"
                    + "</project>\n",
                    buildFile);
            antBuilder.setBuildFile(buildFile.getAbsolutePath());
            antBuilder.setTarget(target);
            antBuilder.setUseLogger(true);
            antBuilder.setLiveOutput(true);
            antBuilder.setProgressLoggerLib("dummyLib");

            // use Progress to make build wait for a while.
            final String done = "done";
            final Progress progress = new ProgressImplTest.MockProgress() {
                private static final long serialVersionUID = 6607524919179888057L;

                public void setValue(String value) {
                    synchronized (done) {
                        super.setValue(value);
                        done.notifyAll();
                    }
                }
            };

            final String buildDone = "buildDone";
            new Thread("RunBuild") {
                public void run() {
                    try {
                        antBuilder.build(buildProperties, progress);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        synchronized (buildDone) {
                            progress.setValue(buildDone);
                            buildDone.notifyAll();
                        }
                    }
                }
            } .start();
            // wait for progress message to come through
            synchronized (done) {
                while (progress.getText() == null) {
                    done.wait(5000);
                }
            }

            output = mbean.getBuildOutput(0);
            assertNotNull(output);
            assertTrue("Unexpected empty build output", output.length > 0);

            // wait for build/scriptRun to complete
            synchronized (buildDone) {
                while (!buildDone.equals(progress.getText())) {
                    buildDone.wait(5000);
                }
            }
            assertTrue("Cleared data file should have reset ID: " + mbean.getOutputLoggerID(),
                    mbean.getOutputLoggerID().endsWith("__0"));
        } finally {
            IO.delete(validFile);
        }
    }

    private static void stringToFile(final String data, final File outFile) throws IOException {
        final FileOutputStream fos = new FileOutputStream(outFile);
        try {
            final OutputStreamWriter oos = new OutputStreamWriter(fos);
            try {
                oos.write(data);
            } finally {
                oos.close();
            }
        } finally {
            fos.close();
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

    public void testBuildWithTargetAndAddedProps() throws Exception {
        final ProjectConfig projectConfig = new ProjectConfig();
        projectConfig.setName("testproject");
        projectConfig.add(new DefaultLabelIncrementer());

        // Override to allow us to read props and check for existence of extra props.
        final class MockScheduleExposingProps extends MockSchedule {
            private static final long serialVersionUID = -8394080367390959431L;

            protected Map<String, String> getBuildProperties() { return super.getBuildProperties(); }
        }
        final MockScheduleExposingProps mockSchedule = new MockScheduleExposingProps();
        projectConfig.add(mockSchedule);

        final ModificationSet modificationSet = new ModificationSet();
        modificationSet.add(new AlwaysBuild());
        modificationSet.setQuietPeriod(0);
        projectConfig.add(modificationSet);

        final Log log = new Log() {
            private static final long serialVersionUID = 1101933013536150534L;

            public void writeLogFile(final Date now) throws CruiseControlException {
                // don't write anything, so we don't need to clean up temp files.
            }
        };
        log.setDir(System.getProperty("java.io.tmpdir"));
        projectConfig.add(log);
        projectConfig.configureProject();

        final Project project = ProjectConfigTest.getProjectFromProjectConfig(projectConfig);
        final ProjectController mbean = new ProjectController(project);

        final Map<String, String> properties = new HashMap<String, String>();
        properties.put("environment", "UIT");
        properties.put("webbrowser", "IE6");

        final BuildQueue buildQueue = new BuildQueue();
        projectConfig.setBuildQueue(buildQueue);
        BuildQueueTest.startBuildQueue(buildQueue);
        project.start();


        final class Result {
            boolean isBuilt;
        }
        final Result result = new Result();

        project.addBuildResultListener(new BuildResultListener() {
            public void handleBuildResult(final BuildResultEvent event) {
                synchronized (result) {
                    result.isBuilt = true;
                    result.notifyAll();
                }
            }
        });

        // trigger build with added props
        mbean.buildWithTarget("sometarget", properties);

        int count = 0;
        while (!result.isBuilt && count < 5) {
            count++;
            synchronized (result) {
                result.wait(count * 1000);
            }
        }

        final Map<String, String> props = mockSchedule.getBuildProperties();
        assertTrue("Property environment should exist", props.containsKey("environment"));
        assertEquals(props.get("environment"), "UIT");

        assertTrue("Property webbrowser should exist", props.containsKey("webbrowser"));
        assertEquals(props.get("webbrowser"), "IE6");
    }

    private static class SVNStub implements SourceControl {
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
