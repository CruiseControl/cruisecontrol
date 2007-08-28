package net.sourceforge.cruisecontrol.dashboard.widgets;

import java.util.HashMap;

import net.sourceforge.cruisecontrol.dashboard.saxhandler.BuildMessageExtractor;
import net.sourceforge.cruisecontrol.dashboard.saxhandler.CompositeExtractor;
import net.sourceforge.cruisecontrol.dashboard.saxhandler.SAXBasedExtractor;
import net.sourceforge.cruisecontrol.dashboard.saxhandler.StackTraceExtractor;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;

import org.jmock.cglib.MockObjectTestCase;

public class ErrorsAndWarningsMessagesWidgetTest extends MockObjectTestCase {
    public void testShouldInvokeBuildMessageExtractorToParseLogFile() throws Exception {
        ErrorsAndWarningsMessagesWidget widget =
                new ErrorsAndWarningsMessagesWidget(new CompositeExtractor(new SAXBasedExtractor[] {
                        new BuildMessageExtractor(), new StackTraceExtractor()}));
        assertEquals("Errors and Warnings", widget.getDisplayName());
        HashMap params = new HashMap();
        params.put(Widget.PARAM_BUILD_LOG_FILE, DataUtils.getFailedBuildLbuildAsFile());
        String output = (String) widget.getOutput(params);
        assertEquals("<h2>Build Error Message</h2>This is my error message<h2>"
                + "Errors and Warnings</h2>Detected OS: Windows XP<br>Cannot"
                + " find something<br><h2>Stacktrace</h2>This is my stacktrace", output);
    }
}
