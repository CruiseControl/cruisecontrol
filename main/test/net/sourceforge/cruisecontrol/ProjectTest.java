/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
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
 ********************************************************************************/
package net.sourceforge.cruisecontrol;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.builders.MockBuilder;
import net.sourceforge.cruisecontrol.buildloggers.MergeLogger;
import net.sourceforge.cruisecontrol.events.BuildProgressEvent;
import net.sourceforge.cruisecontrol.events.BuildProgressListener;
import net.sourceforge.cruisecontrol.events.BuildResultEvent;
import net.sourceforge.cruisecontrol.events.BuildResultListener;
import net.sourceforge.cruisecontrol.labelincrementers.DefaultLabelIncrementer;
import net.sourceforge.cruisecontrol.util.DateUtil;
import net.sourceforge.cruisecontrol.util.Util;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.jdom.Element;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ProjectTest extends TestCase {
    private static final org.apache.log4j.Logger LOG4J = org.apache.log4j.Logger.getLogger(ProjectTest.class);

    private static final String TEST_DIR = "tmp";

    private Project project;
    private final List filesToClear = new ArrayList();

    public ProjectTest(String name) {
        super(name);

        // Turn off logging
        BasicConfigurator.configure();
    }

    protected void setUp() {
        project = new Project();
        project.setName("TestProject");
        LOG4J.getLoggerRepository().setThreshold(Level.OFF);
    }

    public void tearDown() {
        project = null;

        LOG4J.getLoggerRepository().setThreshold(Level.ALL);
        for (Iterator iterator = filesToClear.iterator(); iterator.hasNext();) {
            File file = (File) iterator.next();
            Util.deleteFile(file);
        }
    }

    public void testNotifyListeners() {
        MockListener listener = new MockListener();
        List listeners = new ArrayList();
        listeners.add(listener);
        project.setListeners(listeners);
        ProjectEvent event = new ProjectEvent("foo") {
        };
        project.notifyListeners(event);
        assertTrue(listener.wasNotified());
    }

    public void testBuild() throws Exception {
        Date now = new Date();
        MockModificationSet modSet = new MockModificationSet();
        modSet.setTimeOfCheck(now);
        MockSchedule sched = new MockSchedule();
        project.setSchedule(sched);
        project.setLabel("1.2.2");
        project.setName("myproject");
        project.setWasLastBuildSuccessful(false);
        File logDir = new File(TEST_DIR + File.separator + "test-results");
        logDir.mkdir();
        project.getLog().setLogDir(TEST_DIR + File.separator + "test-results");
        project.setLogXmlEncoding("ISO-8859-1");
        MergeLogger logger = new MergeLogger();
        logger.setFile(TEST_DIR + File.separator + "_auxLog1.xml");
        project.getLog().addLogger(logger);

        logger = new MergeLogger();
        logger.setDir(TEST_DIR + File.separator + "_auxLogs");
        project.getLog().addLogger(logger);

        project.setLabelIncrementer(new DefaultLabelIncrementer());
        project.setModificationSet(modSet);
        project.setLastBuild(formatTime(now));
        project.setLastSuccessfulBuild(formatTime(now));
        writeFile(TEST_DIR + File.separator + "_auxLog1.xml", "<one/>");
        File auxLogsDirectory = new File(TEST_DIR + File.separator + "_auxLogs");
        auxLogsDirectory.mkdir();
        writeFile(TEST_DIR + File.separator + "_auxLogs/_auxLog2.xml",
                "<testsuite><properties><property/></properties><testcase/></testsuite>");
        writeFile(TEST_DIR + File.separator + "_auxLogs/_auxLog3.xml", "<testsuite/>");

        final ArrayList resultEvents = new ArrayList();
        project.addBuildResultListener(new BuildResultListener() {
            public void handleBuildResult(BuildResultEvent event) {
                resultEvents.add(event);
            }
        });

        final ArrayList progressEvents = new ArrayList();
        project.addBuildProgressListener(new BuildProgressListener() {
            public void handleBuildProgress(BuildProgressEvent event) {
                progressEvents.add(event);
            }
        });

        project.build();
        filesToClear.add(project.getLog().getLastLogFile());

        assertTrue(project.isLastBuildSuccessful());

        String expected =
                "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><cruisecontrol><modifications />"
                + "<info>"
                + "<property name=\"projectname\" value=\"myproject\" />"
                + "<property name=\"lastbuild\" value=\""
                + DateUtil.getFormattedTime(now)
                + "\" />"
                + "<property name=\"lastsuccessfulbuild\" value=\""
                + project.getLastSuccessfulBuild()
                + "\" />"
                + "<property name=\"builddate\" value=\""
                + new SimpleDateFormat(DateFormatFactory.getFormat()).format(now)
                + "\" />"
                + "<property name=\"cctimestamp\" value=\""
                + DateUtil.getFormattedTime(now)
                + "\" />"
                + "<property name=\"label\" value=\"1.2.2\" />"
                + "<property name=\"interval\" value=\"300\" />"
                + "<property name=\"lastbuildsuccessful\" value=\"false\" />"
                + "<property name=\"logdir\" value=\""
                + logDir.getAbsolutePath()
                + "\" />"
                + "<property name=\"logfile\" value=\""
                + "log"
                + DateUtil.getFormattedTime(now)
                + "L1.2.2.xml\" />"
                + "</info><build /><one /><testsuite><testcase /></testsuite><testsuite /></cruisecontrol>";
        assertEquals(expected, Util.readFileToString(project.getLog().getLastLogFile()));
        assertEquals("Didn't increment the label", "1.2.3", project.getLabel().intern());

        //look for sourcecontrol properties
        java.util.Map props = sched.getBuildProperties();
        assertNotNull("Build properties were null.", props);
        assertEquals("Build property count.", 8, props.size());
        assertTrue("filemodified not found.", props.containsKey("filemodified"));
        assertTrue("fileremoved not found.", props.containsKey("fileremoved"));
        assertEquals(project.getLastSuccessfulBuild(), props.get("cclastgoodbuildtimestamp"));
        assertEquals(project.getLastBuild(), props.get("cclastbuildtimestamp"));
        assertTrue("cvstimestamp not passed.", props.containsKey("cvstimestamp"));

        // check that the proper events were fired
        assertEquals("Should be exactly one build result event", 1, resultEvents.size());
        BuildResultEvent resultEvent = (BuildResultEvent) resultEvents.get(0);
        assertTrue("Should be successful build result event", resultEvent.isBuildSuccessful());
        assertEquals("Should be exactly 6 build progress events", 6, progressEvents.size());
    }

    public void testBadLabel() {
        try {
            project.validateLabel("build_0", new DefaultLabelIncrementer());
            fail("Expected exception due to bad label");
        } catch (CruiseControlException expected) {

        }
    }

    public void testPublish() throws CruiseControlException {
        MockSchedule sched = new MockSchedule();
        project.setSchedule(sched);

        MockPublisher publisher = new MockPublisher();
        Publisher exceptionThrower = new MockPublisher() {
            public void publish(Element log) throws CruiseControlException {
                throw new CruiseControlException("exception");
            }
        };

        List publishers = new ArrayList();
        publishers.add(publisher);
        publishers.add(exceptionThrower);
        publishers.add(publisher);

        project.setPublishers(publishers);
        project.setName("projectName");
        project.setLabel("label");
        //Element element = project.getProjectPropertiesElement(new Date());
        project.publish();

        assertEquals(2, publisher.getPublishCount());
    }

    public void testSetLastBuild() throws CruiseControlException {
        String lastBuild = "20000101120000";

        project.setLastBuild(lastBuild);

        assertEquals(lastBuild, project.getLastBuild());
    }

    public void testNullLastBuild() throws CruiseControlException {
        try {
            project.setLastBuild(null);
            fail("Expected an IllegalArgumentException for a null last build");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testBadLastBuild() {
        try {
            project.setLastBuild("af32455432");
            fail("Expected a CruiseControlException for a bad last build");
        } catch (CruiseControlException e) {
        }
    }

    public void testGetFormattedTime() {
        assertNull(DateUtil.getFormattedTime(null));
    }

    public void testGetModifications() throws CruiseControlException {
        MockModificationSet modSet = new MockModificationSet();
        Element modifications = modSet.getModifications(null);
        project.setModificationSet(modSet);

        modSet.setModified(true);
        assertEquals(modifications, project.getModifications());
        modSet.setModified(false);
        assertEquals(null, project.getModifications());

        project.setBuildForced(true);
        assertEquals(modifications, project.getModifications());
        project.setBuildForced(false);
        assertEquals(null, project.getModifications());

        project.setBuildForced(true);
        modSet.setModified(true);
        assertEquals(modifications, project.getModifications());

        modSet.setModified(false);
        project.setBuildForced(false);
        assertEquals(null, project.getModifications());

        // TODO: need tests for when lastBuildSuccessful = false
    }

    public void testCheckOnlySinceLastBuild() throws CruiseControlException {

        project.setLastBuild("20030218010101");
        project.setLastSuccessfulBuild("20030218010101");
        assertEquals(false, project.checkOnlySinceLastBuild());

        project.setLastBuild("20030218020202");
        assertEquals(false, project.checkOnlySinceLastBuild());

        project.setBuildAfterFailed(false);
        assertEquals(true, project.checkOnlySinceLastBuild());

        project.setLastBuild("20030218010102");
        assertEquals(false, project.checkOnlySinceLastBuild());

        project.setLastBuild("20020101010101");
        assertEquals(false, project.checkOnlySinceLastBuild());
    }

    public void testWaitIfPaused() throws InterruptedException {
        MockProject mockProject = new MockProject() {
            public void run() {
                loop();
            }

            void checkWait() throws InterruptedException {
                waitIfPaused();
            }
        };

        new Thread(mockProject).start();

        int firstLoopCount = mockProject.getLoopCount();
        Thread.sleep(100);
        int secondLoopCount = mockProject.getLoopCount();
        assertTrue("loop counts are different when not paused", firstLoopCount != secondLoopCount);

        mockProject.setPaused(true);
        Thread.sleep(100);
        firstLoopCount = mockProject.getLoopCount();
        Thread.sleep(100);
        secondLoopCount = mockProject.getLoopCount();
        assertTrue("loop counts are the same when paused", firstLoopCount == secondLoopCount);

        mockProject.setPaused(false);
        Thread.sleep(100);
        int lastLoopCount = mockProject.getLoopCount();
        assertTrue("loop count increased after pause ended", lastLoopCount > secondLoopCount);

        mockProject.stopLooping();
    }

    public void testWaitForNextBuild() throws InterruptedException {
        MockProject mockProject = new MockProject() {
            public void run() {
                loop();
            }

            void checkWait() throws InterruptedException {
                waitForNextBuild();
            }
        };
        mockProject.overrideBuildInterval(1000);
        mockProject.setSchedule(new MockSchedule());
        new Thread(mockProject).start();

        Thread.sleep(100);
        assertEquals(1, mockProject.getLoopCount());

        Thread.sleep(100);
        assertEquals(1, mockProject.getLoopCount());

        mockProject.forceBuild();
        Thread.sleep(100);
        assertEquals(2, mockProject.getLoopCount());

        mockProject.stopLooping();
    }

    public void testWaitForBuildToFinish() throws InterruptedException {
        MockProject mockProject = new MockProject() {
            public void run() {
                loop();
            }

            void checkWait() throws InterruptedException {
                waitForBuildToFinish();
            }
        };

        new Thread(mockProject).start();

        Thread.sleep(100);
        assertEquals(1, mockProject.getLoopCount());

        Thread.sleep(100);
        assertEquals(1, mockProject.getLoopCount());

        mockProject.buildFinished();
        Thread.sleep(100);
        assertEquals(2, mockProject.getLoopCount());

        mockProject.stopLooping();
    }

    public void testNeedToWait() {
        assertTrue(Project.needToWaitForNextBuild(1));
        assertFalse(Project.needToWaitForNextBuild(0));
        assertFalse(Project.needToWaitForNextBuild(-1));
    }

    public void testToString() {
        project.setName("foo");
        assertEquals("Project foo: stopped", project.toString());
        project.setPaused(true);
        assertEquals("Project foo: stopped (paused)", project.toString());
    }

    public void testInit() throws CruiseControlException {
        try {
            project.init();
        } catch (IllegalStateException expected) {
            assertEquals("set config file on project before calling init()", expected.getMessage());
        }
    }

    public void testSerialization() throws IOException {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ObjectOutputStream objects = new ObjectOutputStream(outBytes);

        objects.writeObject(new Project());
        objects.flush();
        objects.close();
    }

    public void testDeserialization() throws Exception {
        File f = new File("test.ser");
        filesToClear.add(f);
        FileOutputStream outFile = new FileOutputStream(f);
        ObjectOutputStream objects = new ObjectOutputStream(outFile);

        objects.writeObject(new Project());
        objects.flush();
        objects.close();

        FileInputStream inFile = new FileInputStream(f);
        ObjectInputStream inObjects = new ObjectInputStream(inFile);

        Object p = inObjects.readObject();
        inObjects.close();
        assertNotNull("Read object must not be null", p);
        assertTrue("Object must be instanceof Project class", p instanceof Project);
        Project deserializedProject = (Project) p;
        deserializedProject.addBuildProgressListener(new BuildProgressListener() {
            public void handleBuildProgress(BuildProgressEvent event) {

            }
        });
    }

    public void testGetProjectPropertiesMap() throws CruiseControlException {
        String label = "LaBeL";
        project.setLabel(label);
        String lastBuild = "20000101120000";
        project.setLastBuild(lastBuild);
        String lastGoodBuild = "19990101120000";
        project.setLastSuccessfulBuild(lastGoodBuild);
        project.setWasLastBuildSuccessful(true);
        Date now = new Date();

        Map map = project.getProjectPropertiesMap(now);

        assertEquals(label, map.get("label"));
        assertEquals(DateUtil.getFormattedTime(now), map.get("cctimestamp"));
        assertEquals(lastGoodBuild, map.get("cclastgoodbuildtimestamp"));
        assertEquals(lastBuild, map.get("cclastbuildtimestamp"));
        assertEquals("true", map.get("lastbuildsuccessful"));
    }
    
    public void testGetTimeToNextBuild_AfterShortBuild() {
        Schedule schedule = new Schedule();
        MockBuilder noonBuilder = new MockBuilder();
        noonBuilder.setTime("1200");
        noonBuilder.setBuildLogXML(new Element("builder1"));
        schedule.addBuilder(noonBuilder);
        project.setSchedule(schedule);
        
        Calendar cal = Calendar.getInstance();
        cal.set(2001, Calendar.NOVEMBER, 22);
        cal.set(Calendar.HOUR_OF_DAY, 12);
        cal.set(Calendar.MINUTE, 00);
        Date noonBuild = cal.getTime();
        project.setBuildStartTime(noonBuild);
        
        cal.set(Calendar.SECOND, 30);
        Date postNoonBuild = cal.getTime();
        
        long time = project.getTimeToNextBuild(postNoonBuild);
        assertEquals(Schedule.ONE_DAY, time);
    }

    private void writeFile(String fileName, String contents) throws IOException {

        File theFile = new File(fileName);
        filesToClear.add(theFile);
        FileWriter fw = new FileWriter(theFile);
        fw.write(contents);
        fw.close();
    }

    private static String formatTime(Date time) {
        return new SimpleDateFormat("yyyyMMddHHmmss").format(time);
    }

    class MockPublisher implements Publisher {
        private int publishCount = 0;

        public void validate() {
        }

        public void publish(Element log) throws CruiseControlException {
            publishCount++;
        }

        int getPublishCount() {
            return publishCount;
        }
    }

    class MockListener implements Listener {
        private boolean notified = false;

        public boolean wasNotified() {
            return notified;
        }

        public void handleEvent(ProjectEvent event) throws CruiseControlException {
            notified = true;
        }

        public void validate() throws CruiseControlException {
        }
    }
}
