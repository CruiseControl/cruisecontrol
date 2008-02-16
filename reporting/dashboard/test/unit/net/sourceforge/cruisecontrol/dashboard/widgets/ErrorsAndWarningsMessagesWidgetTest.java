package net.sourceforge.cruisecontrol.dashboard.widgets;

import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import net.sourceforge.cruisecontrol.dashboard.saxhandler.BuildMessageExtractor;
import net.sourceforge.cruisecontrol.dashboard.saxhandler.CompositeExtractor;
import net.sourceforge.cruisecontrol.dashboard.saxhandler.SAXBasedExtractor;
import net.sourceforge.cruisecontrol.dashboard.saxhandler.StackTraceExtractor;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;

import org.jmock.cglib.MockObjectTestCase;

public class ErrorsAndWarningsMessagesWidgetTest extends MockObjectTestCase {

    public void testShouldBeAbleToParseErrorsAndWarnsFromXmlFile() throws Exception {
        ErrorsAndWarningsMessagesWidget widget = createWidget();
        assertEquals("Errors and Warnings", widget.getDisplayName());
        HashMap params = new HashMap();
        params.put(Widget.PARAM_BUILD_LOG_FILE, DataUtils.getFailedBuildLbuildAsFile());
        String output = (String) widget.getOutput(params);
        assertTrue(output.indexOf("This is my error message") != -1);
        assertTrue(output.indexOf("This is my stacktrace") != -1);
        assertTrue(output.indexOf("Cannot find something") != -1);
    }

    public void testShouldBeAbleToParseErrorsAndWarnsFromZippedFile() throws Exception {
        ErrorsAndWarningsMessagesWidget widget = createWidget();
        assertEquals("Errors and Warnings", widget.getDisplayName());
        HashMap params = new HashMap();
        params.put(Widget.PARAM_BUILD_LOG_FILE, DataUtils.getZippedBuildAsFile());
        String output = (String) widget.getOutput(params);
        assertTrue(output.indexOf("This is my stacktrace") != -1);
        assertTrue(output.indexOf("Cannot find something") != -1);
    }

    private ErrorsAndWarningsMessagesWidget createWidget() {
        return new ErrorsAndWarningsMessagesWidget(new CompositeExtractor(handlers()));
    }

    private List handlers() {
        return Arrays
                .asList(new SAXBasedExtractor[] { new BuildMessageExtractor(), new StackTraceExtractor() });
    }
}
