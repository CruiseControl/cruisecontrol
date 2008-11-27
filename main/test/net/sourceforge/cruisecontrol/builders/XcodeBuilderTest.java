package net.sourceforge.cruisecontrol.builders;


import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.Directory;

import org.jdom.Element;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class XcodeBuilderTest {
    private XcodeBuilder builder;
    
    @Before
    public void setUp() throws Exception {
        Directory directory = new Directory() {
            @Override
            public void validate() {
            }
        };

        builder = new XcodeBuilder();
        builder.directory = directory;
    }

    @After
    public void tearDown() throws Exception {
        builder = null;
    }

    @Test
    public void validateShouldCallDirectoryValidate() throws CruiseControlException {
        final Called validate = new Called();
        Directory directory = new Directory() {
            @Override
            public void validate() {
                validate.called = true;
            }
        };
        builder.directory = directory;
        builder.validate();
        assertTrue(validate.called);
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
    
    private class Called {
        boolean called = false;
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
