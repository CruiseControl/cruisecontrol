package net.sourceforge.cruisecontrol.builders;


import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sourceforge.cruisecontrol.BuilderTest;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.BuildOutputLogger;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.Directory;

import org.jdom2.Element;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class XcodeBuilderTest {
    private XcodeBuilder builder;

    private Directory directoryDoesntFailValidation;

    private Map<String, String> buildProperties;

    @Before
    public void setUp() throws Exception {
        directoryDoesntFailValidation = new Directory() {
            @Override
            public void validate() {
            }
        };

        builder = new XcodeBuilder();
        builder.directory = directoryDoesntFailValidation;

        buildProperties = BuilderTest.createPropsWithProjectName("testproject");
    }

    @After
    public void tearDown() throws Exception {
        builder = null;
    }

    @Test
    public void validateShouldCallDirectoryValidate() throws CruiseControlException {
        final Called validate = new Called();
        builder.directory = new Directory() {
            @Override
            public void validate() {
                validate.called = true;
            }
        };
        builder.validate();
        assertTrue(validate.called);
    }
    
    @Test(expected = CruiseControlException.class)
    public void validateShouldMakeSureArgsArentEmpty() throws CruiseControlException {
        builder.createArg().setValue(" ");
        builder.validate();
    }
    
    @Test
    public void getExitCodeShouldReturnSetExitCode() {
        builder.setExitCode(17);
        assertEquals(17, builder.getExitCode());
    }
    
    @Test
    public void workingDirectoryShouldBeDirectory() throws CruiseControlException {
        builder.setDirectory("path");
        Commandline cmdLine = builder.buildCommandline();
        assertEquals("path", cmdLine.getWorkingDirectory().getPath());
    }
    
    @Test
    public void executableShouldBeXcodebuild() throws CruiseControlException {
        Commandline cmdLine = builder.buildCommandline();
        assertEquals("xcodebuild", cmdLine.getExecutable());
    }
    
    @Test
    public void shouldBeNoDefaultArguments() throws CruiseControlException {
        Commandline cmdLine = builder.buildCommandline();
        assertArrayEquals(new String[] {}, cmdLine.getArguments());
    }
    
    @Test
    public void argsShouldBeOnCommandLine() throws CruiseControlException {
        String arg1 = "hello";
        String arg2 = "world";
        builder.createArg().setValue(arg1);
        builder.createArg().setValue(arg2);
        Commandline cmdLine = builder.buildCommandline();
        String[] args = cmdLine.getArguments();
        assertEquals(arg1, args[0]);
        assertEquals(arg2, args[1]);
        assertEquals(2, args.length);
    }
    
    @Test
    public void argsShouldHavePropertiesSubstituted() throws CruiseControlException {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("key", "value");
        builder.setProperties(properties);
        builder.createArg().setValue("${key}");
        Commandline cmdLine = builder.buildCommandline();
        String[] args = cmdLine.getArguments();
        assertEquals("value", args[0]);
        assertEquals(1, args.length);
    }
    
    @Test
    public void compileLineShouldNotCreateAnElement() {
        Element e = builder.getElementFromLine(COMPILE_LINE);
        assertNull(e);
    }
    
    @Test
    public void warningLineShouldReturnWarningElement() {
        Element e = builder.getElementFromLine(WARNING_LINE);
        assertMessageAtLevel(WARNING_LINE, e, "warn");
    }

    private void assertMessageAtLevel(String line, Element e, String level) {
        assertNotNull(e);
        assertEquals("target", e.getName());
        Element task = e.getChild("task");
        assertNotNull(task);
        Element message = task.getChild("message");
        assertNotNull(message);
        assertEquals(level, message.getAttributeValue("priority"));
        assertEquals(line, message.getText());
    }
    
    @Test
    public void errorLineShouldReturnErrorElement() {
        Element e = builder.getElementFromLine(ERROR_LINE);
        assertMessageAtLevel(ERROR_LINE, e, "error");
    }
    
    @Test
    public void buildFaildShouldReturnErrorElement() {
        Element e = builder.getElementFromLine(BUILD_FAILED_LINE);
        assertMessageAtLevel(BUILD_FAILED_LINE, e, "error");
    }
    
    @Test
    public void everythingAfterBuildFailedShouldReturnElement() {
        Element e = builder.getElementFromLine(COMMANDS_FAILED_LINE);
        assertNull(e);        
        e = builder.getElementFromLine(BUILD_FAILED_LINE);
        assertMessageAtLevel(BUILD_FAILED_LINE, e, "error");
        e = builder.getElementFromLine(COMMANDS_FAILED_LINE);
        assertMessageAtLevel(COMMANDS_FAILED_LINE, e, "error");        
    }
    
    @Test
    public void timeoutShouldBePassedToScriptRunner() throws CruiseControlException {
        final Called runScript = new Called();
        final ScriptRunner runner = new ScriptRunner() {
            @Override
            public boolean runScript(Script script, long timeout, BuildOutputLogger logger)
                  throws CruiseControlException {
                runScript.called = true;
                runScript.with = String.valueOf(timeout);
                return true;
            }
        };
        builder = new XcodeBuilder() {
            @Override
            ScriptRunner createScriptRunner() {
                return runner;
            }
            
            @Override
            Element elementFromFile(OutputFile file) {
                return null;
            }
        };
        
        builder.setTimeout(515);
        builder.build(buildProperties, null);
        assertTrue(runScript.called);
        assertEquals("515", runScript.with);
    }
    
    @Test
    public void timingOutShouldResultInFailedBuild() throws CruiseControlException {
        final MockOutputFile outputFile = new MockOutputFile();
        outputFile.lines.add("hello world");
        
        final ScriptRunner runner = new ScriptRunner() {
            @Override
            public boolean runScript(Script script, long timeout, BuildOutputLogger logger)
                  throws CruiseControlException {
                return false; // returned when timeout happens
            }
        };
        builder = new XcodeBuilder() {
            @Override
            ScriptRunner createScriptRunner() {
                return runner;
            }
            
            @Override
            OutputFile createOutputFile(Directory d, String filename) {
                return outputFile;
            }
        };
        
        builder.setTimeout(515);
        Element result = builder.build(buildProperties, null);
        assertNotNull(result.getAttribute("error"));
        assertEquals("build timed out", result.getAttributeValue("error"));
    }
    
    @Test
    public void buildWithTargetShouldBePassedToCommandLine() throws CruiseControlException {
        final Called cmdLine = new Called();        
        builder = builderForBuildTest(cmdLine);
        
        builder.buildWithTarget(buildProperties, "target", null);
        assertTrue(cmdLine.called);
        assertTrue(cmdLine.with.contains("-target target"));
    }

    @Test
    public void buildWithTargetShouldBeTransient() throws CruiseControlException {
        final Called cmdLine = new Called();        
        builder = builderForBuildTest(cmdLine);
        
        builder.buildWithTarget(buildProperties, "target", null);
        builder.build(buildProperties, null);
        assertTrue(cmdLine.called);
        assertFalse(cmdLine.with.contains("-target target"));
    }
    
    @Test
    public void buildWithTargetShouldReplaceExistingTarget() throws CruiseControlException {
        final Called cmdLine = new Called();        
        builder = builderForBuildTest(cmdLine);
        
        builder.createArg().setValue("-target oldTarget");
        builder.buildWithTarget(buildProperties, "newTarget", null);
        assertTrue(cmdLine.called);
        assertTrue(cmdLine.with.contains("-target newTarget"));
        assertFalse(cmdLine.with.contains("-target oldTarget"));
    }
    
    @Test
    public void afterBuildWithTargetOriginalTargetShouldBeRestored() throws CruiseControlException {
        final Called cmdLine = new Called();        
        builder = builderForBuildTest(cmdLine);
        
        builder.createArg().setValue("-target oldTarget");
        builder.buildWithTarget(buildProperties, "newTarget", null);
        builder.build(buildProperties, null);
        assertTrue(cmdLine.called);
        assertTrue(cmdLine.with.contains("-target oldTarget"));
        assertFalse(cmdLine.with.contains("-target newTarget"));
    }
    
    private XcodeBuilder builderForBuildTest(final Called cmdLine) {
        final ScriptRunner runner = new ScriptRunner() {
            @Override
            public boolean runScript(Script script, long timeout, BuildOutputLogger logger)
                  throws CruiseControlException {
                cmdLine.called = true;
                cmdLine.with = script.buildCommandline().toStringNoQuoting();
                return true;
            }
        };
        XcodeBuilder builderForBuildTest = new XcodeBuilder() {
            @Override
            ScriptRunner createScriptRunner() {
                return runner;
            }
            
            @Override
            OutputFile createOutputFile(Directory d, String filename) {
                return new MockOutputFile();
            }
        };
        builderForBuildTest.directory = directoryDoesntFailValidation;

        return builderForBuildTest;
    }
    
    private class Called {
        boolean called = false;
        String with;
    }
    
    private class MockOutputFile extends XcodeBuilder.OutputFile {
        final List<String> lines = new ArrayList<String>();
        private Iterator iterator;

        MockOutputFile() {
            super(directoryDoesntFailValidation, ".");
        }
        
        @Override
        public String nextLine() {
            return (String) iterator.next();
        }

        @Override
        public boolean hasMoreLines() {
            if (iterator == null) {
                iterator = lines.iterator();
            }

            return iterator.hasNext();
        }
    }
    
    private static final String COMPILE_LINE = "    /Developer/usr/bin/ibtool --errors --warnings --notices"
        + " --output-format human-readable-text --compile "
        + "/Users/jfredrick/projects/App/build/Release-iphoneos/App.app/RegionView.nib "
        + "/Users/jfredrick/projects/App/src/com/jeffreyfredrick/app/view/RegionView.xib";
    
    private static final String WARNING_LINE = "/Users/jfredrick/projects/"
        + "App/src/com/jeffreyfredrick/app/view/RegionView.xib:28: "
        + "warning: Setting a UIScrollView's minimum or maximum zoom to anything other than 1.0 will be ignored "
        + "on iPhone OS versions prior to 2.1.";
    
    private static final String ERROR_LINE = "/Users/jfredrick/projects/"
        + "App/src/com/jeffreyFredrick/EventDispatcher.h:11:16: "
        + "error: foo.h: No such file or directory";
    
    private static final String BUILD_FAILED_LINE = "** BUILD FAILED **";
    
    private static final String COMMANDS_FAILED_LINE = "The following build commands failed:";

}
