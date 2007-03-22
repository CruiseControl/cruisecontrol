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

import net.sourceforge.cruisecontrol.distributed.core.PropertiesHelper;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.builders.MockBuilder;

/**
 * @author: Dan Rollo
 * Date: May 25, 2005
 * Time: 1:54:38 PM
 */
public class BuildAgentServiceImplTest extends TestCase {

    public static final String TEST_AGENT_PROPERTIES_FILE = "testdist.agent.properties";
    public static final String TEST_USER_DEFINED_PROPERTIES_FILE = "testdist.user-defined.properties";
    public static final String ENTRY_NAME_BUILD_TYPE = "build.type";

    private static final File DIR_LOGS = new File(PropertiesHelper.RESULT_TYPE_LOGS);
    private static final File DIR_OUTPUT = new File(PropertiesHelper.RESULT_TYPE_OUTPUT);

    private Properties origSysProps;

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

        void setAgentStatusChangeCount(int agentStatusChangeCount) {
            this.agentStatusChangeCount = agentStatusChangeCount;
        }
    }

    /** 
     * Re-use of builder caused problems with null value in overrideTarget.
     * This test verifies null values in the Map are allowed.
     * @throws Exception if anything unexpected goes wrong in the test
     */
     public void testGetPropertiesMap() throws Exception {
        final BuildAgentServiceImpl agentImpl = new BuildAgentServiceImpl();
        agentImpl.setAgentPropertiesFilename(TEST_AGENT_PROPERTIES_FILE);

        MyAgentStatusListener agentListener = new MyAgentStatusListener();  
        agentImpl.addAgentStatusListener(agentListener);
        
        String agentAsString = agentImpl.asString();
        assertTrue("Wrong value: " + agentAsString,
                agentAsString.startsWith("Machine Name: "));
        assertTrue("Wrong value: " + agentAsString,
                agentAsString.endsWith("Busy: false;\tSince: null;\tModule: null\n\t"
                + "Pending Restart: false;\tPending Restart Since: null\n\t"
                + "Pending Kill: false;\tPending Kill Since: null"));

        
        final Map distributedAgentProps = new HashMap();
        final String testModuleName = "testModuleName";
        distributedAgentProps.put(PropertiesHelper.DISTRIBUTED_MODULE, testModuleName);

        distributedAgentProps.put(PropertiesHelper.DISTRIBUTED_OVERRIDE_TARGET, null);        
        try {
            // fails at old props.putAll() and now at projectPropertiesMap.toString(), 
            // doesn't fire 2nd agent status event
            agentImpl.doBuild(null, null, distributedAgentProps);
            fail("should fail w/ NPE");
        } catch (NullPointerException e) {
            assertEquals(null, e.getMessage());
        }
        assertEquals("Wrong agent status", 1, agentListener.getAgentStatusChangeCount());

        
        distributedAgentProps.remove(PropertiesHelper.DISTRIBUTED_OVERRIDE_TARGET);
        agentImpl.setBusy(false);
        agentListener.setAgentStatusChangeCount(0);
        try {
            // gets far enough to fire 2nd agent status change
            agentImpl.doBuild(null, new HashMap(), distributedAgentProps);
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
        assertTrue("Wrong value: " + agentAsString,
                agentAsString.endsWith(";\tModule: " + testModuleName
                + "\n\tPending Restart: false;\tPending Restart Since: null\n\t"
                + "Pending Kill: false;\tPending Kill Since: null"));
    }
    
    public void testAsString() throws Exception {
        final BuildAgentServiceImpl agentImpl = new BuildAgentServiceImpl();
        agentImpl.setAgentPropertiesFilename(TEST_AGENT_PROPERTIES_FILE);

        String agentAsString = agentImpl.asString();
        assertTrue("Wrong value: " + agentAsString,
                agentAsString.startsWith("Machine Name: "));
        assertTrue("Wrong value: " + agentAsString,
                agentAsString.endsWith("Busy: false;\tSince: null;\tModule: null\n\t"
                + "Pending Restart: false;\tPending Restart Since: null\n\t"
                + "Pending Kill: false;\tPending Kill Since: null"));

        final Map projectProps = null;
        
        final Map distributedAgentProps = new HashMap();
        final String testModuleName = "testModuleName";
        distributedAgentProps.put(PropertiesHelper.DISTRIBUTED_MODULE, testModuleName);

        try {
            // gets far enough to set Module name...
            agentImpl.doBuild(null, projectProps, distributedAgentProps);
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
                agentAsString.endsWith(";\tModule: " + testModuleName
                + "\n\tPending Restart: false;\tPending Restart Since: null\n\t"
                + "Pending Kill: false;\tPending Kill Since: null"));

        agentImpl.kill(true);
        agentAsString = agentImpl.asString();
        assertTrue("Wrong value: " + agentAsString,
                agentAsString.indexOf("Busy: true;\tSince: ") > -1);
        assertTrue("Wrong value: " + agentAsString,
                agentAsString.indexOf(";\tModule: " + testModuleName
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

    public void testGetBuildingModule() throws Exception {
        final BuildAgentServiceImpl agentImpl = new BuildAgentServiceImpl();
        agentImpl.setAgentPropertiesFilename(TEST_AGENT_PROPERTIES_FILE);

        assertNull(agentImpl.getModule());

        final Map projectProps = null;

        final Map distributedAgentProps = new HashMap();
        final String testModuleName = "testModuleName";
        distributedAgentProps.put(PropertiesHelper.DISTRIBUTED_MODULE, testModuleName);

        try {
            // gets far enough to set Module name...
            agentImpl.doBuild(null, projectProps, distributedAgentProps);
            fail("should fail w/ NPE");
        } catch (NullPointerException e) {
            assertEquals(null, e.getMessage());
        }

        assertEquals(testModuleName, agentImpl.getModule());

        agentImpl.setBusy(false); // fake build finish
        assertNull(agentImpl.getModule());
    }

    public void testKillNoWait() throws Exception {
        final BuildAgentServiceImpl agentImpl = new BuildAgentServiceImpl();
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
        final BuildAgentServiceImpl agentImpl = new BuildAgentServiceImpl();
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
        final BuildAgentServiceImpl agentImpl = new BuildAgentServiceImpl();
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
        final BuildAgentServiceImpl agentImpl = new BuildAgentServiceImpl();
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
        final int delay = 1500;
        System.setProperty(BuildAgentServiceImpl.SYSPROP_CCDIST_DELAY_MS_KILLRESTART, delay + "");

        // fake finish agent build
        assertTrue(agentImpl.isBusy());
        agentImpl.setBusy(false);
        assertTrue("Agent should still be busy due to pending restart", agentImpl.isBusy());
        assertNotNull(agentImpl.getLastDelayedAction());
        assertEquals(BuildAgentServiceImpl.DelayedAction.Type.RESTART,
                agentImpl.getLastDelayedAction().getType());
        // wait for Restart() outside of webstart exception
        Thread.sleep(delay * 2);
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
            throws CruiseControlException, IOException {
        
        final File resultTypeBaseDir
                = (PropertiesHelper.RESULT_TYPE_LOGS.equals(resultType)
                        ? DIR_LOGS : DIR_OUTPUT);

        final File testDirResult = new File(resultTypeBaseDir, 
                (PropertiesHelper.RESULT_TYPE_LOGS.equals(resultType)
                        ? "myTest" + PropertiesHelper.RESULT_TYPE_LOGS + "Dir"
                        : "testmodule-success")); // used default dir since no property is set
        
        testDirResult.deleteOnExit();
        testDirResult.mkdirs();

        final Map distributedAgentProps = new HashMap();
        distributedAgentProps.put(PropertiesHelper.DISTRIBUTED_AGENT_LOGDIR,
                testDirResult.getAbsolutePath());
        final BuildAgentServiceImpl agentImpl = createTestAgent(false, distributedAgentProps);        
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
        
        final File tmpLogSuccessDir = new File(DIR_LOGS, "testmodule-success");
        new File(tmpLogSuccessDir, "TEST-bogustestclassSuccess.xml").delete();
        deleteDirConfirm(tmpLogSuccessDir);

        final File tmpOutputSuccessDir = new File(DIR_OUTPUT, "testmodule-success");
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

        final BuildAgentServiceImpl agentImpl = createTestAgent(false, distributedAgentProps);

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
        final BuildAgentServiceImpl agentImpl = createTestAgent(false, new HashMap());
        try {
            assertTrue(agentImpl.resultsExist(PropertiesHelper.RESULT_TYPE_LOGS));
            assertTrue(agentImpl.resultsExist(PropertiesHelper.RESULT_TYPE_OUTPUT));
            assertTrue("Agent should be busy until build results are retrived and cleared.",
                    agentImpl.isBusy());
        } finally {
            // cleanup left over files
            agentImpl.clearOutputFiles();
        }
        assertFalse(agentImpl.isBusy());
    }

    public void testRetrieveResultsAsZipBuildFail() throws Exception {
        final BuildAgentServiceImpl agentImpl = createTestAgent(true, new HashMap());
        try {
            assertTrue(agentImpl.resultsExist(PropertiesHelper.RESULT_TYPE_LOGS));
            assertFalse(agentImpl.resultsExist(PropertiesHelper.RESULT_TYPE_OUTPUT));
            assertTrue("Agent should be busy until build results are retrived and cleared.",
                    agentImpl.isBusy());
        } finally {
            // cleanup left over files
            agentImpl.clearOutputFiles();
        }
        assertFalse(agentImpl.isBusy());
    }

    private static BuildAgentServiceImpl createTestAgent(boolean isBuildFailure, 
                                                         final Map distributedAgentProps)
            throws CruiseControlException, RemoteException {

        final BuildAgentServiceImpl agentImpl = new BuildAgentServiceImpl();
        agentImpl.setAgentPropertiesFilename(TEST_AGENT_PROPERTIES_FILE);

        callTestDoBuild(isBuildFailure, agentImpl, distributedAgentProps);

        return agentImpl;
    }

    
    /**
     * Call doBuild() on the given agent using test settings
     * @param isBuildFailure if true, run a build that will fail
     * @param agent the build agent on which to run the build
     * @return the build result jdom element
     * @throws CruiseControlException if build dies
     * @throws RemoteException if something else dies
     */
    public static Element callTestDoBuild(boolean isBuildFailure,
                                       final BuildAgentService agent) 
            throws CruiseControlException, RemoteException {
        
        return callTestDoBuild(isBuildFailure, agent, new HashMap());
    }
    
    /**
     * Call doBuild() on the given agent using test settings
     * @param isBuildFailure if true, run a build that will fail
     * @param agent the build agent on which to run the build
     * @param distributedAgentProps the map of distributed props used by the build agent
     * @return the build result jdom element
     * @throws CruiseControlException if build dies
     * @throws RemoteException if something else dies
     */
    public static Element callTestDoBuild(final boolean isBuildFailure,
                                       final BuildAgentService agent,
                                       final Map distributedAgentProps) 
            throws CruiseControlException, RemoteException {
        
        final String moduleName;
        if (isBuildFailure) {
            moduleName = "testmodule-fail";
        } else {
            moduleName = "testmodule-success";
        }
        distributedAgentProps.put(PropertiesHelper.DISTRIBUTED_MODULE, moduleName);
        
        // handle re-use case of DMB when overrideTarget is null
        distributedAgentProps.put(PropertiesHelper.DISTRIBUTED_OVERRIDE_TARGET, null);


        final MockBuilder mockBuilder = new MockBuilder("testCCDistMockChildBuilder") {
            public Element build(Map properties) {
                final Element retVal = super.build(properties);

                // create a files in the expected dirs
                createExpectedBuildArtifact(new File("logs/" + moduleName + "/TEST-bogustestclassSuccess.xml"));
                if (!isBuildFailure) {
                    createExpectedBuildArtifact(new File("output/" + moduleName + "/testoutputSuccess"));
                }

                return retVal;
            }
        };

        return agent.doBuild(mockBuilder, new HashMap(), distributedAgentProps);
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
                    e.getMessage().indexOf("that is busy building module: null") > 0);
        }
        assertEquals(firstClaimDate, agentImpl.getDateClaimed());

        releaseClaimedAgent(agentImpl, firstClaimDate);
    }

    // NOTE: Can't do test w/ RestartPending, since Restart requires Agent running in Webstart.
    public void testClaimWhileKillPending() throws Exception {
        final BuildAgentServiceImpl agentImpl = createAndClaimNewBuildAgent();
        final Date firstClaimDate = agentImpl.getDateClaimed();

        // use a fairly short delay in unit test
        final int delay = 1500;
        System.setProperty(BuildAgentServiceImpl.SYSPROP_CCDIST_DELAY_MS_KILLRESTART, delay + "");

        // set a pending Restart
        agentImpl.kill(true);

        assertFalse("Kill should not have executed.", agentImpl.isDoKillExecuted());

        // call doBuild() on agent
        callDoBuildWithNulls(agentImpl, firstClaimDate);

        assertEquals("Agent kill should be delayed " + delay
                + " milliseconds, and agent should be busy during delay.",
                true, agentImpl.isBusy());
        assertFalse("Kill should not have executed.", agentImpl.isDoKillExecuted());

        // wait long enough for doKill to execute
        Thread.sleep(delay * 2);
        assertTrue("Kill should have executed.", agentImpl.isDoKillExecuted());

        // Agent will still show as "claimed" in unit tests (since it can't system.exit).
        // That is the correct behavior since is prevents agent from being acquired while
        // an action is pending.
        assertEquals("Agent should be busy after doKill executes in unit test.",
                true, agentImpl.isBusy());
        assertEquals(firstClaimDate, agentImpl.getDateClaimed());
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
            agentImpl.doBuild(null, null, null);
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

        final BuildAgentServiceImpl agentImpl = new BuildAgentServiceImpl();
        agentImpl.setAgentPropertiesFilename(TEST_AGENT_PROPERTIES_FILE);

        assertFalse(agentImpl.isBusy());
        assertNull(agentImpl.getDateClaimed());
        agentImpl.claim();
        assertTrue(agentImpl.isBusy());
        assertNotNull(agentImpl.getDateClaimed());
        return agentImpl;
    }
}
