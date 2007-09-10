package net.sourceforge.cruisecontrol.distributed;

import junit.framework.TestCase;

import java.util.Properties;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Date;

import java.io.File;
import java.io.IOException;

import java.rmi.RemoteException;

import org.jdom.Element;
import org.apache.log4j.Logger;

import net.sourceforge.cruisecontrol.distributed.core.PropertiesHelper;
import net.sourceforge.cruisecontrol.distributed.core.CCDistVersion;
import net.sourceforge.cruisecontrol.distributed.core.jnlputil.JNLPServiceUtil;
import net.sourceforge.cruisecontrol.builders.MockBuilder;
import net.sourceforge.cruisecontrol.builders.DistributedMasterBuilderTest;
import net.sourceforge.cruisecontrol.builders.AntBuilder;
import net.sourceforge.cruisecontrol.builders.CompositeBuilder;
import net.sourceforge.cruisecontrol.builders.AntScript;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Progress;

import javax.jnlp.UnavailableServiceException;

/**
 * @author Dan Rollo
 * Date: May 25, 2005
 * Time: 1:54:38 PM
 */
public class BuildAgentServiceImplTest extends TestCase {

    private static final Logger LOG = Logger.getLogger(BuildAgentServiceImplTest.class);

    public static final String TEST_AGENT_PROPERTIES_FILE = "testdist.agent.properties";
    public static final String TEST_USER_DEFINED_PROPERTIES_FILE = "testdist.user-defined.properties";
    public static final String ENTRY_NAME_BUILD_TYPE = "build.type";

    private static final File DIR_LOGS = new File(PropertiesHelper.RESULT_TYPE_LOGS);
    private static final File DIR_OUTPUT = new File(PropertiesHelper.RESULT_TYPE_OUTPUT);

    private Properties origSysProps;
    private static final String TEST_PROJECT_FAIL = "testproject-fail";
    private static final String TEST_PROJECT_SUCCESS = "testproject-success";
    private static final int KILL_DELAY = 1000;

    private static final String EXPECTED_SUFFIX_AS_STRING_VER
            = "\n\tVersion: " + CCDistVersion.getVersion()
            + " (Compiled: " + CCDistVersion.getVersionBuildDate() + ")";

    protected void setUp() throws Exception {
        DIR_LOGS.delete();
        DIR_OUTPUT.delete();
        origSysProps = System.getProperties();
    }

    protected void tearDown() throws Exception {
        System.setProperties(origSysProps);
        deleteDirConfirm(DIR_LOGS);
        deleteDirConfirm(DIR_OUTPUT);
    }

    private static void deleteDirConfirm(final File dirToDelete) {
        if (dirToDelete.exists()) {
            assertTrue("Error cleaning up test directory: " + dirToDelete.getAbsolutePath()
                    +  "\nDir Contents:\n" + Arrays.asList(dirToDelete.listFiles()),
                    dirToDelete.delete());
        }
    }


    private static class MyAgentStatusListener implements BuildAgent.AgentStatusListener
    {
        private int agentStatusChangeCount;

        public void statusChanged(BuildAgentService buildAgentServiceImpl) {
            agentStatusChangeCount++;
        }

        int getAgentStatusChangeCount() {
            return agentStatusChangeCount;
        }

        void resetAgentStatusChangeCount() {
            this.agentStatusChangeCount = 0;
        }
    }


    private static final File LOGGER_JAR_REAL_MAIN_DIST
            // @todo Change path to main/dist is we ever move CCDist into main
            = new File("../../main/dist", AntScript.LIBNAME_PROGRESS_LOGGER);
    private static final File LOGGER_JAR_MOVED = new File(LOGGER_JAR_REAL_MAIN_DIST.getParentFile(),
            "hidden" + AntScript.LIBNAME_PROGRESS_LOGGER);
    private static void hideAntProgressLoggerLib() {
        LOGGER_JAR_REAL_MAIN_DIST.renameTo(LOGGER_JAR_MOVED);
    }
    private static void unhideAntProgressLoggerLib() {
        LOGGER_JAR_MOVED.renameTo(LOGGER_JAR_REAL_MAIN_DIST);
    }

    public void testInjectAntProgressLoggerLibIfNeeded() throws  Exception {
        hideAntProgressLoggerLib();
        try {

            final AntBuilder antBuilder = new AntBuilder();

            // should attempt injection
            try {
                BuildAgentServiceImpl.injectAntProgressLoggerLibIfNeeded(antBuilder);
                fail("Expected JNLP Basic Service to be unavailable");
            } catch (JNLPServiceUtil.JNLPServiceException e) {
                assertTrue("Unexpected exception cause.",
                        e.getCause() instanceof UnavailableServiceException);
            }

            // test found via default lib location
            unhideAntProgressLoggerLib();
            BuildAgentServiceImpl.injectAntProgressLoggerLibIfNeeded(antBuilder);
            hideAntProgressLoggerLib();

            // test path already set
            antBuilder.setProgressLoggerLib("someProgressLoggerLibPath");
            BuildAgentServiceImpl.injectAntProgressLoggerLibIfNeeded(antBuilder);
        } finally {
            unhideAntProgressLoggerLib();
        }
    }

    public void testInjectAntProgressLoggerLibIfNeededWithComposite() throws  Exception {
        hideAntProgressLoggerLib();
        try {
            final CompositeBuilder compositeBuilder = new CompositeBuilder();
            BuildAgentServiceImpl.injectAntProgressLoggerLibIfNeeded(compositeBuilder);

            compositeBuilder.add(new MockBuilder());
            BuildAgentServiceImpl.injectAntProgressLoggerLibIfNeeded(compositeBuilder);

            final AntBuilder antBuilder = new AntBuilder();
            compositeBuilder.add(antBuilder);

            // should attempt injection
            try {
                BuildAgentServiceImpl.injectAntProgressLoggerLibIfNeeded(compositeBuilder);
                fail("Expected JNLP Basic Service to be unavailable");
            } catch (JNLPServiceUtil.JNLPServiceException e) {
                assertTrue("Unexpected exception cause.",
                        e.getCause() instanceof UnavailableServiceException);
            }
            // test path already set
            antBuilder.setProgressLoggerLib("someProgressLoggerLibPath");
            BuildAgentServiceImpl.injectAntProgressLoggerLibIfNeeded(compositeBuilder);

            // test with composite inside composite
            final CompositeBuilder compositeBuilder2 = new CompositeBuilder();
            compositeBuilder.add(compositeBuilder2);
            BuildAgentServiceImpl.injectAntProgressLoggerLibIfNeeded(compositeBuilder);

            final AntBuilder antBuilder2 = new AntBuilder();
            compositeBuilder2.add(antBuilder2);
            // should attempt injection
            try {
                BuildAgentServiceImpl.injectAntProgressLoggerLibIfNeeded(compositeBuilder);
                fail("Expected JNLP Basic Service to be unavailable");
            } catch (JNLPServiceUtil.JNLPServiceException e) {
                assertTrue("Unexpected exception cause.",
                        e.getCause() instanceof UnavailableServiceException);
            }
            // test path already set
            antBuilder2.setProgressLoggerLib("someProgressLoggerLibPath");
            BuildAgentServiceImpl.injectAntProgressLoggerLibIfNeeded(compositeBuilder);
        } finally {
            unhideAntProgressLoggerLib();
        }
    }

    public void testBuildOverrideTarget() throws Exception {
        final BuildAgentServiceImpl agentImpl = new BuildAgentServiceImpl(null);
        agentImpl.setAgentPropertiesFilename(TEST_AGENT_PROPERTIES_FILE);

        final Map distributedAgentProps = new HashMap();
        // build w/out override to verify null target value after build
        distributedAgentProps.put(PropertiesHelper.DISTRIBUTED_OVERRIDE_TARGET, null);

        final MockBuilder mockBuilder = createMockBuilder(false);
        assertNull(mockBuilder.getTarget());

        final Map projectProperties = new HashMap();
        projectProperties.put(PropertiesHelper.PROJECT_NAME, TEST_PROJECT_SUCCESS);

        try {
            assertNotNull(agentImpl.doBuild(mockBuilder, projectProperties, distributedAgentProps, null));
            clearDefaultSuccessResultDirs();
            assertNull(mockBuilder.getTarget());
        } finally {
            agentImpl.clearOutputFiles();
        }

        // build with override
        distributedAgentProps.put(PropertiesHelper.DISTRIBUTED_OVERRIDE_TARGET, TEST_PROJECT_SUCCESS);
        assertNull(mockBuilder.getTarget());
        try {
            assertNotNull(agentImpl.doBuild(mockBuilder, projectProperties, distributedAgentProps, null));
            clearDefaultSuccessResultDirs();
            assertEquals(TEST_PROJECT_SUCCESS, mockBuilder.getTarget());
        } finally {
            agentImpl.clearOutputFiles();
        }
    }

    /**
     * Re-use of builder caused problems with null value in overrideTarget.
     * This test verifies null values in the Map are allowed.
     * @throws Exception if anything unexpected goes wrong in the test
     */
     public void testGetPropertiesMap() throws Exception {
        final BuildAgentServiceImpl agentImpl = new BuildAgentServiceImpl(null);
        agentImpl.setAgentPropertiesFilename(TEST_AGENT_PROPERTIES_FILE);

        MyAgentStatusListener agentListener = new MyAgentStatusListener();  
        agentImpl.addAgentStatusListener(agentListener);
        
        String agentAsString = agentImpl.asString();
        assertTrue("Wrong value: " + agentAsString,
                agentAsString.startsWith("Machine Name: "));

        String expectedSuffix = "Busy: false;\tSince: null;\tProject: null\n\t"
                + "Pending Restart: false;\tPending Restart Since: null\n\t"
                + "Pending Kill: false;\tPending Kill Since: null"
                + EXPECTED_SUFFIX_AS_STRING_VER;
        assertTrue("Wrong value: \n" + agentAsString + "\n expected suffix: \n" + expectedSuffix,
                agentAsString.endsWith(expectedSuffix));

        
        final Map distributedAgentProps = new HashMap();
        final String testProjectName = "testProjectName";
        final Map projectProperties = new HashMap();
        projectProperties.put(PropertiesHelper.PROJECT_NAME, testProjectName);

        distributedAgentProps.put(PropertiesHelper.DISTRIBUTED_OVERRIDE_TARGET, null);
        try {
            // fails at nestedBuilder.validate() due to null nestedBuilder
            agentImpl.doBuild(null, projectProperties, distributedAgentProps, null);
            fail("should fail w/ NPE");
        } catch (NullPointerException e) {
            assertEquals(null, e.getMessage());
        }
        assertEquals("Wrong agent status", 2, agentListener.getAgentStatusChangeCount());

        
        distributedAgentProps.remove(PropertiesHelper.DISTRIBUTED_OVERRIDE_TARGET);
        agentImpl.setBusy(false);
        agentListener.resetAgentStatusChangeCount();
        try {
            // gets far enough to fire 2nd agent status change
            agentImpl.doBuild(null, projectProperties, distributedAgentProps, null);
            fail("should fail w/ NPE");
        } catch (NullPointerException e) {
            assertEquals(null, e.getMessage());
        }
        assertEquals("Wrong agent status", 2, agentListener.getAgentStatusChangeCount());

        agentAsString = agentImpl.asString();
        assertTrue("Wrong value: " + agentAsString,
                agentAsString.startsWith("Machine Name: "));
        assertTrue("Wrong value: " + agentAsString,
                agentAsString.indexOf("Busy: true;\tSince: ") > -1);

        expectedSuffix = ";\tProject: " + testProjectName
                + "\n\tPending Restart: false;\tPending Restart Since: null\n\t"
                + "Pending Kill: false;\tPending Kill Since: null"
                + EXPECTED_SUFFIX_AS_STRING_VER;

        assertTrue("Wrong value: \n" + agentAsString + "\n expected suffix: \n" + expectedSuffix,
                agentAsString.endsWith(expectedSuffix));
    }
    
    public void testAsString() throws Exception {
        final BuildAgentServiceImpl agentImpl = new BuildAgentServiceImpl(null);
        agentImpl.setAgentPropertiesFilename(TEST_AGENT_PROPERTIES_FILE);

        String agentAsString = agentImpl.asString();
        assertTrue("Wrong value: " + agentAsString,
                agentAsString.startsWith("Machine Name: "));
        assertTrue("Wrong value: " + agentAsString,
                agentAsString.endsWith("Busy: false;\tSince: null;\tProject: null\n\t"
                        + "Pending Restart: false;\tPending Restart Since: null\n\t"
                        + "Pending Kill: false;\tPending Kill Since: null"
                        + EXPECTED_SUFFIX_AS_STRING_VER));


        final Map distributedAgentProps = new HashMap();

        final String testProjectName = "testProjectName";
        final Map projectProperties = new HashMap();
        projectProperties.put(PropertiesHelper.PROJECT_NAME, testProjectName);

        try {
            // gets far enough to set Project name...
            agentImpl.doBuild(null, projectProperties, distributedAgentProps, null);
            fail("should fail w/ NPE");
        } catch (NullPointerException e) {
            assertEquals(null, e.getMessage());
        }

        agentAsString = agentImpl.asString();
        assertTrue("Wrong value: " + agentAsString,
                agentAsString.startsWith("Machine Name: "));
        assertTrue("Wrong value: " + agentAsString,
                agentAsString.indexOf("Busy: true;\tSince: ") > -1);
        assertTrue("Wrong value: " + agentAsString,
                agentAsString.endsWith(";\tProject: " + testProjectName
                        + "\n\tPending Restart: false;\tPending Restart Since: null\n\t"
                        + "Pending Kill: false;\tPending Kill Since: null"
                        + EXPECTED_SUFFIX_AS_STRING_VER));

        agentImpl.kill(true);
        agentAsString = agentImpl.asString();
        assertTrue("Wrong value: " + agentAsString,
                agentAsString.indexOf("Busy: true;\tSince: ") > -1);
        assertTrue("Wrong value: " + agentAsString,
                agentAsString.indexOf(";\tProject: " + testProjectName
                        + "\n\tPending Restart: false;\tPending Restart Since: null\n\t"
                + "Pending Kill: true;\tPending Kill Since: ") > -1);
        assertNotNull(agentImpl.getPendingKillSince());

        agentImpl.setBusy(false); // fake build finish
        assertTrue("Pending kill should keep agent marked busy", agentImpl.isBusy());
        agentAsString = agentImpl.asString();
        assertTrue("Wrong value: " + agentAsString,
                agentAsString.indexOf("Pending Restart: false;\tPending Restart Since: null\n\t"
                + "Pending Kill: true;\tPending Kill Since: ") > -1);
        assertNotNull(agentImpl.getPendingKillSince());
    }

    public void testGetBuildingProject() throws Exception {
        final BuildAgentServiceImpl agentImpl = new BuildAgentServiceImpl(null);
        agentImpl.setAgentPropertiesFilename(TEST_AGENT_PROPERTIES_FILE);

        assertNull(agentImpl.getProjectName());

        final Map distributedAgentProps = new HashMap();

        final String testProjectName = "testProjectName";
        final Map projectProperties = new HashMap();
        projectProperties.put(PropertiesHelper.PROJECT_NAME, testProjectName);

        try {
            // gets far enough to set Project name...
            agentImpl.doBuild(null, projectProperties, distributedAgentProps, null);
            fail("should fail w/ NPE");
        } catch (NullPointerException e) {
            assertEquals(null, e.getMessage());
        }

        assertEquals(testProjectName, agentImpl.getProjectName());

        agentImpl.setBusy(false); // fake build finish
        assertNull(agentImpl.getProjectName());
    }

    public void testKillNoWait() throws Exception {
        final BuildAgentServiceImpl agentImpl = new BuildAgentServiceImpl(null);
        agentImpl.setAgentPropertiesFilename(TEST_AGENT_PROPERTIES_FILE);

        assertFalse(agentImpl.isBusy());
        assertFalse(agentImpl.isPendingKill());
        assertNull(agentImpl.getPendingKillSince());
        assertFalse(agentImpl.isPendingRestart());
        assertNull(agentImpl.getPendingRestartSince());
        // make agent think it's building now
        agentImpl.claim();
        assertTrue(agentImpl.isBusy());
        assertFalse(agentImpl.isPendingKill());
        assertFalse(agentImpl.isPendingRestart());
        agentImpl.kill(false);
        assertTrue(agentImpl.isBusy());
        assertTrue(agentImpl.isPendingKill());
        assertNotNull(agentImpl.getPendingKillSince());
        assertFalse(agentImpl.isPendingRestart());
        assertNull(agentImpl.getPendingRestartSince());

        // fake finish agent build - not really needed here
        agentImpl.setBusy(false);
    }

    public void testRestartNoWait() throws Exception {
        final BuildAgentServiceImpl agentImpl = new BuildAgentServiceImpl(null);
        agentImpl.setAgentPropertiesFilename(TEST_AGENT_PROPERTIES_FILE);

        assertFalse(agentImpl.isBusy());
        assertFalse(agentImpl.isPendingKill());
        assertNull(agentImpl.getPendingKillSince());
        assertFalse(agentImpl.isPendingRestart());
        assertNull(agentImpl.getPendingRestartSince());
        // make agent think it's building now
        agentImpl.claim();
        assertTrue(agentImpl.isBusy());
        assertFalse(agentImpl.isPendingKill());
        assertFalse(agentImpl.isPendingRestart());
        // fake finish agent build
        try {
            agentImpl.restart(false);
            fail("Restart should fail outside of webstart");
        } catch (RuntimeException e) {
            assertEquals("Couldn't find webstart Basic Service. Is Agent running outside of webstart?",
                    e.getMessage());
        }
        assertTrue(agentImpl.isBusy());
        assertFalse(agentImpl.isPendingKill());
        assertNull(agentImpl.getPendingKillSince());
        assertTrue(agentImpl.isPendingRestart());
        assertNotNull(agentImpl.getPendingRestartSince());
    }

    public void testKillWithWait() throws Exception {
        final BuildAgentServiceImpl agentImpl = new BuildAgentServiceImpl(null);
        agentImpl.setAgentPropertiesFilename(TEST_AGENT_PROPERTIES_FILE);

        assertFalse(agentImpl.isBusy());
        assertFalse(agentImpl.isPendingKill());
        assertNull(agentImpl.getPendingKillSince());
        assertFalse(agentImpl.isPendingRestart());
        assertNull(agentImpl.getPendingRestartSince());
        // make agent think it's building now
        agentImpl.claim();
        assertTrue(agentImpl.isBusy());
        assertFalse(agentImpl.isPendingKill());
        assertFalse(agentImpl.isPendingRestart());
        agentImpl.kill(true);
        assertTrue(agentImpl.isBusy());
        assertTrue(agentImpl.isPendingKill());
        assertNotNull(agentImpl.getPendingKillSince());
        assertFalse(agentImpl.isPendingRestart());

        // fake finish agent build
        agentImpl.setBusy(false);
    }

    public void testRestartWithWait() throws Exception {
        final BuildAgentServiceImpl agentImpl = new BuildAgentServiceImpl(null);
        agentImpl.setAgentPropertiesFilename(TEST_AGENT_PROPERTIES_FILE);

        assertFalse(agentImpl.isBusy());
        assertFalse(agentImpl.isPendingKill());
        assertNull(agentImpl.getPendingKillSince());
        assertFalse(agentImpl.isPendingRestart());
        assertNull(agentImpl.getPendingRestartSince());
        // make agent think it's building now
        agentImpl.claim();
        assertTrue(agentImpl.isBusy());
        assertFalse(agentImpl.isPendingKill());
        assertNull(agentImpl.getPendingRestartSince());
        assertFalse(agentImpl.isPendingRestart());
        assertNull(agentImpl.getPendingRestartSince());
        assertNull(agentImpl.getLastDelayedAction());
        agentImpl.restart(true);
        assertTrue(agentImpl.isBusy());
        assertFalse(agentImpl.isPendingKill());
        assertTrue(agentImpl.isPendingRestart());
        assertNotNull(agentImpl.getPendingRestartSince());

        // use a fairly short delay in unit test
        System.setProperty(BuildAgentServiceImpl.SYSPROP_CCDIST_DELAY_MS_KILLRESTART, KILL_DELAY + "");

        // fake finish agent build
        assertTrue(agentImpl.isBusy());
        agentImpl.setBusy(false);
        assertTrue("Agent should still be busy due to pending restart", agentImpl.isBusy());
        assertNotNull(agentImpl.getLastDelayedAction());
        assertEquals(BuildAgentServiceImpl.DelayedAction.Type.RESTART,
                agentImpl.getLastDelayedAction().getType());
        waitForDelayedAction(agentImpl);

        assertEquals("Couldn't find webstart Basic Service. Is Agent running outside of webstart?",
                agentImpl.getLastDelayedAction().getThrown().getMessage());
    }

    public void testRecursiveFilesExist() throws Exception {
        final File testDir = new File(DIR_LOGS, "testDir");
        testDir.deleteOnExit();
        assertFalse("Non-existant base dir should show empty", 
                BuildAgentServiceImpl.recursiveFilesExist(testDir));    

        final File testDirSub = new File(testDir, "testSubDir");
        testDirSub.deleteOnExit();

        final File testDirSubSub = new File(testDirSub, "testSubSubDir");
        testDirSubSub.deleteOnExit();

        final File testFileSub = new File(testDirSub, "testFileSub");
        testFileSub.deleteOnExit();
        
        try {
            testDir.mkdirs();
            assertFalse("Existant, empty base dir should show empty", 
                    BuildAgentServiceImpl.recursiveFilesExist(testDir));
            
            testDirSub.mkdirs();
            assertFalse("Existant, empty base dir, empty sub dir should show empty", 
                    BuildAgentServiceImpl.recursiveFilesExist(testDir));            
            
            testDirSubSub.mkdirs();
            assertFalse("Existant, empty base dir, empty sub, empty sub 2 should show empty", 
                    BuildAgentServiceImpl.recursiveFilesExist(testDir));            
            
            testFileSub.createNewFile();
            assertTrue("Existant, empty base dir, non-empty sub dir should show files exist", 
                    BuildAgentServiceImpl.recursiveFilesExist(testDir));            
            
        } finally {
            testDirSubSub.delete();
            testFileSub.delete();
            testDirSub.delete();
            testDir.delete();            
        }
    }
    
    public void testResultsExistAfterBuild() throws Exception {
        checkResultsExistByType(PropertiesHelper.RESULT_TYPE_LOGS);
        checkResultsExistByType(PropertiesHelper.RESULT_TYPE_OUTPUT);
    }

    private static void checkResultsExistByType(String resultType)
            throws IOException {
        
        final File resultTypeBaseDir
                = (PropertiesHelper.RESULT_TYPE_LOGS.equals(resultType)
                        ? DIR_LOGS : DIR_OUTPUT);

        final File testDirResult = new File(resultTypeBaseDir, 
                (PropertiesHelper.RESULT_TYPE_LOGS.equals(resultType)
                        ? "myTest" + PropertiesHelper.RESULT_TYPE_LOGS + "Dir"
                        : TEST_PROJECT_SUCCESS)); // used default dir since no property is set
        
        testDirResult.deleteOnExit();
        testDirResult.mkdirs();

        final Map distributedAgentProps = new HashMap();
        distributedAgentProps.put(PropertiesHelper.DISTRIBUTED_AGENT_LOGDIR,
                testDirResult.getAbsolutePath());
        final BuildAgentServiceImpl agentImpl = createTestAgentDoBuild(false, distributedAgentProps);
        // clear out default result dirs
        clearDefaultSuccessResultDirs();

        final File testFileResult = new File(testDirResult, "myTestFile");
        final File testDirResultSub = new File(testDirResult, "myTestResultSubDir");
        testDirResultSub.deleteOnExit();
        final File testDirResultSubSub = new File(testDirResultSub, "myTestSubSubDir");
        testDirResultSubSub.deleteOnExit();
        final File testFileSub = new File(testDirResultSub, "myTestFileSub");

        // test behavior if log dir exists, and is empty except for sub dirs.
        // use case: subdirs hold test report files, and a compile error occurs 
        // before unit tests are run, and hence no test log files have been created yet 
        // in sub dirs.
        try {
            assertFalse(agentImpl.resultsExist(resultType));
            testDirResultSubSub.mkdirs();
            assertFalse("Result dir with no files, only subdirs, should be considered empty",
                    agentImpl.resultsExist(resultType));

            testFileSub.createNewFile();
            assertTrue("Any file in tree should produce results",
                    agentImpl.resultsExist(resultType));

            testFileResult.createNewFile();
            testFileResult.deleteOnExit();
            assertTrue("Multiple files in tree should produce results", 
                    agentImpl.resultsExist(resultType));

        } finally {
            // cleanup left over files
            agentImpl.clearOutputFiles();
            testFileSub.delete();
            testFileResult.delete();
            testDirResultSubSub.delete();
            testDirResultSub.delete();
            testDirResult.delete();
        }
    }

    private static void clearDefaultSuccessResultDirs() {
        
        final File tmpLogSuccessDir = new File(DIR_LOGS, TEST_PROJECT_SUCCESS);
        new File(tmpLogSuccessDir, "TEST-bogustestclassSuccess.xml").delete();
        deleteDirConfirm(tmpLogSuccessDir);

        final File tmpOutputSuccessDir = new File(DIR_OUTPUT, TEST_PROJECT_SUCCESS);
        new File(tmpOutputSuccessDir, "testoutputSuccess").delete();
        deleteDirConfirm(tmpOutputSuccessDir);
    }

    public void testRetrieveResultsWithAgentLogDir() throws Exception {
        final File testLogDir = new File(DIR_LOGS, "myTestLogDir");
        testLogDir.deleteOnExit();
        testLogDir.mkdirs();

        final Map distributedAgentProps = new HashMap();
        distributedAgentProps.put(PropertiesHelper.DISTRIBUTED_AGENT_LOGDIR,
                testLogDir.getAbsolutePath());

        final BuildAgentServiceImpl agentImpl = createTestAgentDoBuild(false, distributedAgentProps);
        // delete result zips here to avoid left overs when prepareLogsAndArtifacts() is called below.
        //assertTrue(agentImpl.getResultsZip(PropertiesHelper.RESULT_TYPE_LOGS).delete());
        assertTrue(agentImpl.getResultsZip(PropertiesHelper.RESULT_TYPE_OUTPUT).delete());

        // clear out default log dir
        clearDefaultSuccessResultDirs();

        final File testLog = new File(testLogDir, "myTestLog");
        final File testLogSubDir = new File(testLogDir, "myTestLogSubDir");
        testLogSubDir.deleteOnExit();
        final File testLogSubSubDir = new File(testLogSubDir, "myTestLogSubSubDir");
        testLogSubSubDir.deleteOnExit();
        final File testLogSub = new File(testLogSubDir, "myTestLogSubSubFile");
        try {
            // test behavior if log dir exists, and is empty except for sub dirs.
            // use case: subdirs hold test report files, and a compile error occurs 
            // before unit tests are run, and hence no test log files have been created yet 
            // in sub dirs.
            assertFalse(agentImpl.resultsExist(PropertiesHelper.RESULT_TYPE_LOGS));
            testLogSubSubDir.mkdirs();
            assertFalse("Result dir with no files, only subdirs, should be considered empty",
                    agentImpl.resultsExist(PropertiesHelper.RESULT_TYPE_LOGS));
            agentImpl.prepareLogsAndArtifacts();
            try {
                agentImpl.retrieveResultsAsZip(PropertiesHelper.RESULT_TYPE_LOGS);
                fail("Attempt to retrieve empty zip file should fail");
            } catch (RuntimeException e) {
                assertTrue(e.getMessage().startsWith("Unable to get file "
                        + new File(testLogDir.getParentFile().getCanonicalPath())
                                .getParentFile().getCanonicalPath()));
            }
            testLogSub.createNewFile();
            assertTrue("Any file in tree should produce results",
                    agentImpl.resultsExist(PropertiesHelper.RESULT_TYPE_LOGS));

            // make sure agent looks in myTestLog dir for files 
            testLog.createNewFile();
            testLog.deleteOnExit();
            assertTrue(agentImpl.resultsExist(PropertiesHelper.RESULT_TYPE_LOGS));

            assertFalse("Default output files should have been deleted during unit test",
                    agentImpl.resultsExist(PropertiesHelper.RESULT_TYPE_OUTPUT));
            
            assertTrue("Agent should be busy until build results are retrived and cleared.",
                    agentImpl.isBusy());
        } finally {
            // cleanup left over files
            agentImpl.clearOutputFiles();
            testLog.delete();
            testLogDir.delete();
        }
        assertFalse(agentImpl.isBusy());
    }

    public void testRetrieveResultsAsZipBuildSuccess() throws Exception {
        final BuildAgentServiceImpl agentImpl = createTestAgentDoBuild(false, new HashMap());
        try {
            assertTrue(agentImpl.resultsExist(PropertiesHelper.RESULT_TYPE_LOGS));
            assertTrue(agentImpl.resultsExist(PropertiesHelper.RESULT_TYPE_OUTPUT));
            assertTrue("Agent should be busy until build results are retrived and cleared.",
                    agentImpl.isBusy());

            // make sure we can actually get the results
            assertNonEmptyResultsZip(agentImpl, PropertiesHelper.RESULT_TYPE_LOGS);
            assertNonEmptyResultsZip(agentImpl, PropertiesHelper.RESULT_TYPE_OUTPUT);
        } finally {
            // cleanup left over files
            agentImpl.clearOutputFiles();
        }
        assertFalse(agentImpl.isBusy());
    }

    private static void assertNonEmptyResultsZip(final BuildAgentServiceImpl agentImpl, final String resultType)
            throws RemoteException {

        final byte[] resultZip = agentImpl.retrieveResultsAsZip(resultType);
        assertNotNull("Null " + resultType + " zip bytes.", resultZip);
        assertTrue("Empty " + resultType + " zip bytes.", resultZip.length > 0);
    }

    public void testRetrieveResultsAsZipBuildFail() throws Exception {
        final BuildAgentServiceImpl agentImpl = createTestAgentDoBuild(true, new HashMap());
        try {
            assertTrue(agentImpl.resultsExist(PropertiesHelper.RESULT_TYPE_LOGS));
            assertNonEmptyResultsZip(agentImpl, PropertiesHelper.RESULT_TYPE_LOGS);

            assertFalse(agentImpl.resultsExist(PropertiesHelper.RESULT_TYPE_OUTPUT));
            try {
                agentImpl.retrieveResultsAsZip(PropertiesHelper.RESULT_TYPE_OUTPUT);
                fail("should've failed retrieval of non-existant results.");
            } catch (RuntimeException e) {
                assertTrue(e.getMessage().startsWith("Unable to get file "));
            }
            
            assertTrue("Agent should be busy until build results are retrived and cleared.",
                    agentImpl.isBusy());
        } finally {
            // cleanup left over files
            agentImpl.clearOutputFiles();
        }
        assertFalse(agentImpl.isBusy());
    }

    public void testClearOutputFilesBuildValidateError() throws Exception {

        final Map projectProperties = new HashMap();
        projectProperties.put(PropertiesHelper.PROJECT_NAME, TEST_PROJECT_FAIL);

        final HashMap distributedAgentProps = new HashMap();
        final BuildAgentServiceImpl agentImpl = createTestAgent(distributedAgentProps);
        try {
            callTestDoBuild(projectProperties, agentImpl, distributedAgentProps, true);
        } catch (RemoteException e) {
            assertTrue("Wrong root cause of exception", e.getCause() instanceof CruiseControlException);
            CruiseControlException ce = (CruiseControlException) e.getCause();
            assertEquals("Wrong excecption msg", MSG_BUILDER_VALIDATE_FAIL, ce.getMessage());
        }
        try {
            // NOTE: prepareLogsAndArtifacts() sets log/output member vars, but is not called if
            // builder.validate() fails. May want to change this?
            //agentImpl.prepareLogsAndArtifacts();
            //assertFalse(agentImpl.resultsExist(PropertiesHelper.RESULT_TYPE_LOGS));
            //assertFalse(agentImpl.resultsExist(PropertiesHelper.RESULT_TYPE_OUTPUT));

            assertFalse("Agent should NOT be busy after builder.validate() error.",
                    agentImpl.isBusy());
        } finally {
            // cleanup left over files
            agentImpl.clearOutputFiles();
        }
        assertFalse(agentImpl.isBusy());
    }

    private static BuildAgentServiceImpl createTestAgentDoBuild(final boolean isBuildFailure,
                                                         final Map distributedAgentProps)
            throws RemoteException {

        final BuildAgentServiceImpl agentImpl = createTestAgent(distributedAgentProps);

        final Map projectProperties = new HashMap();
        if (isBuildFailure) {
            projectProperties.put(PropertiesHelper.PROJECT_NAME, TEST_PROJECT_FAIL);
        } else {
            projectProperties.put(PropertiesHelper.PROJECT_NAME, TEST_PROJECT_SUCCESS);
        }

        callTestDoBuild(projectProperties, agentImpl, distributedAgentProps, false);

        return agentImpl;
    }

    private static BuildAgentServiceImpl createTestAgent(final Map distributedAgentProps) {
        final BuildAgentServiceImpl agentImpl = new BuildAgentServiceImpl(null);
        agentImpl.setAgentPropertiesFilename(TEST_AGENT_PROPERTIES_FILE);

        if (!distributedAgentProps.containsKey(PropertiesHelper.DISTRIBUTED_OVERRIDE_TARGET)) {
            distributedAgentProps.put(PropertiesHelper.DISTRIBUTED_OVERRIDE_TARGET, null);
        }

        return agentImpl;
    }


    /**
     * Call doBuild() on a build that should succeed on the given agent using test settings
     * @param agent the build agent on which to run the build
     * @return the build result jdom element
     * @throws RemoteException if something else dies
     */
    public static Element callTestDoBuildSuccess(final BuildAgentService agent)
            throws RemoteException {

        final Map projectProperties = new HashMap();
        projectProperties.put(PropertiesHelper.PROJECT_NAME, TEST_PROJECT_SUCCESS);

        final Map distributedAgentProps = new HashMap();
        // handle re-use case of DMB when overrideTarget is null
        distributedAgentProps.put(PropertiesHelper.DISTRIBUTED_OVERRIDE_TARGET, null);

        return callTestDoBuild(projectProperties, agent, distributedAgentProps, false);
    }
    
    /**
     * Call doBuild() on the given agent using test settings
     * @param projectProperties map passed to build() method, contains "projectname" property
     * @param agent the build agent on which to run the build
     * @param distributedAgentProps the map of distributed props used by the build agent
     * @param isValidateFailure if true, force the builder.validate() call to fail.
     * @return the build result jdom element
     * @throws RemoteException if something else dies
     */
    private static Element callTestDoBuild(final Map projectProperties,
                                       final BuildAgentService agent,
                                       final Map distributedAgentProps,
                                       final boolean isValidateFailure)
            throws RemoteException {
        
        final MockBuilder mockBuilder = createMockBuilder(isValidateFailure);

        return agent.doBuild(mockBuilder, projectProperties, distributedAgentProps, null);
    }

    
    private static final String MSG_BUILDER_VALIDATE_FAIL = "Unit Test forced builder.validate() failure.";

    private static MockBuilder createMockBuilder(final boolean isValidateFailure) {

        return new MockBuilder("testCCDistMockChildBuilder") {

            public void validate() throws CruiseControlException {
                super.validate();
                if (isValidateFailure) {
                    throw new CruiseControlException(MSG_BUILDER_VALIDATE_FAIL);
                }
            }

            public Element build(Map properties, Progress progress) {
                final String projectName = (String) properties.get(PropertiesHelper.PROJECT_NAME);

                final boolean isBuildFailure = TEST_PROJECT_FAIL.equals(projectName);

                final Element retVal = super.build(properties, progress);

                // create a files in the expected dirs
                createExpectedBuildArtifact(new File("logs/" + projectName + "/TEST-bogustestclassSuccess.xml"));
                if (!isBuildFailure) {
                    createExpectedBuildArtifact(new File("output/" + projectName + "/testoutputSuccess"));
                }

                return retVal;
            }
        };
    }

    private static void createExpectedBuildArtifact(File buildProducedFile) {
        if (!buildProducedFile.getParentFile().exists()) {
            buildProducedFile.getParentFile().mkdirs();
        }
        try {
            if (!buildProducedFile.exists()) {
                assertTrue(buildProducedFile.createNewFile());
            }
        } catch (IOException e) {
            fail("couldn't create expected output build produced file: " + buildProducedFile.getAbsolutePath());
        }
        buildProducedFile.deleteOnExit();
        buildProducedFile.getParentFile().deleteOnExit();
        buildProducedFile.getParentFile().getParentFile().deleteOnExit();
    }

    public void testClaim() {
        final BuildAgentServiceImpl agentImpl = createAndClaimNewBuildAgent();
        final Date firstClaimDate = agentImpl.getDateClaimed();

        try {
            agentImpl.claim();
            fail("Should throw exception if attempt is made to claim a busy agent");
        } catch (IllegalStateException e) {
            assertTrue("Unexpected error message w/ invalid call to claim(): " + e.getMessage(),
                    e.getMessage().startsWith("Cannot claim agent on "));
            assertTrue("Unexpected error message w/ invalid call to claim(): " + e.getMessage(),
                    e.getMessage().indexOf("that is busy building project: null") > 0);
        }
        assertEquals(firstClaimDate, agentImpl.getDateClaimed());

        releaseClaimedAgent(agentImpl, firstClaimDate);
    }

    // NOTE: Can't do test w/ RestartPending, since Restart requires Agent running in Webstart.
    public void testClaimWhileKillPending() throws Exception {
        final BuildAgentServiceImpl agentImpl = createAndClaimNewBuildAgent();
        final Date firstClaimDate = agentImpl.getDateClaimed();

        // use a fairly short delay in unit test
        System.setProperty(BuildAgentServiceImpl.SYSPROP_CCDIST_DELAY_MS_KILLRESTART, KILL_DELAY + "");

        // set a pending Restart
        agentImpl.kill(true);

        assertFalse("Kill should not have executed.", agentImpl.isDoKillExecuted());

        // call doBuild() on agent
        callDoBuildWithNulls(agentImpl, firstClaimDate);

        assertEquals("Agent kill should be delayed " + KILL_DELAY
                + " milliseconds, and agent should be busy during delay.",
                true, agentImpl.isBusy());
        assertFalse("Kill should not have executed.", agentImpl.isDoKillExecuted());

        // wait for doKill to execute
        waitForDelayedAction(agentImpl);

        assertTrue("Kill should have executed.", agentImpl.isDoKillExecuted());

        // Agent will still show as "claimed" in unit tests (since it can't system.exit).
        // That is the correct behavior since is prevents agent from being acquired while
        // an action is pending.
        assertEquals("Agent should be busy after doKill executes in unit test.",
                true, agentImpl.isBusy());
        assertEquals(firstClaimDate, agentImpl.getDateClaimed());
    }


    private static void waitForDelayedAction(BuildAgentServiceImpl agentImpl) throws InterruptedException {
        // wait for Restart() outside of webstart exception
        final long begin = System.currentTimeMillis();

        final BuildAgentServiceImpl.DelayedAction delayedAction = agentImpl.getLastDelayedAction();

        final BuildAgentServiceImpl.DelayedAction.FinishedListener finishedListener
                = new BuildAgentServiceImpl.DelayedAction.FinishedListener() {

            public void finished(final BuildAgentServiceImpl.DelayedAction delayedAction) {
                synchronized (delayedAction) {
                    delayedAction.notifyAll();
                }
            }
        };

        delayedAction.setFinishedListener(finishedListener);
        try {
            synchronized (delayedAction) {
                if (!delayedAction.isFinished()) {
                    delayedAction.wait(10 * 1000);
                }
            }
        } finally {
            delayedAction.setFinishedListener(null);
        }

        assertTrue("Delayed action didn't finish before timeout.", delayedAction.isFinished());

        LOG.info(DistributedMasterBuilderTest.MSG_PREFIX_STATS + "Unit test Agent Delayed Action took: "
                + (System.currentTimeMillis() - begin) / 1000f + " sec");
    }


    // @todo should agent expose a release() method to clear busy flag? Very dangerous if agent is building...
    private static void releaseClaimedAgent(BuildAgentServiceImpl agentImpl, Date firstClaimDate)
    {
        callDoBuildWithNulls(agentImpl, firstClaimDate);

        assertFalse("Expected agent busy flag to be false after cleanup call.", agentImpl.isBusy());
        assertNull(agentImpl.getDateClaimed());
    }

    private static void callDoBuildWithNulls(BuildAgentServiceImpl agentImpl, Date firstClaimDate)
    {
        try {
            agentImpl.doBuild(null, null, null, null);
            fail("Should have failed to build");
        } catch (NullPointerException e) {
            assertEquals("Unexpected build error: " + e.getMessage(), null, e.getMessage());
        } catch (RemoteException e) {
            assertTrue("Unexpected build error: " + e.getMessage(),
                    e.getMessage().startsWith("Failed to complete build on agent; nested exception is: \n"
                    + "\tnet.sourceforge.cruisecontrol.CruiseControlException: ant logfile "));
        }
        assertEquals(firstClaimDate, agentImpl.getDateClaimed());

        // we now avoid NPE error during cleanup since logDir and outputDir can be null
        agentImpl.clearOutputFiles();
    }

    private static BuildAgentServiceImpl createAndClaimNewBuildAgent() {

        final BuildAgentServiceImpl agentImpl = new BuildAgentServiceImpl(null);
        agentImpl.setAgentPropertiesFilename(TEST_AGENT_PROPERTIES_FILE);

        assertFalse(agentImpl.isBusy());
        assertNull(agentImpl.getDateClaimed());
        agentImpl.claim();
        assertTrue(agentImpl.isBusy());
        assertNotNull(agentImpl.getDateClaimed());
        return agentImpl;
    }
}
