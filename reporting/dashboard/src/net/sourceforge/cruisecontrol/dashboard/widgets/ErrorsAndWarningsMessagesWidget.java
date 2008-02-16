package net.sourceforge.cruisecontrol.dashboard.widgets;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.sourceforge.cruisecontrol.dashboard.BuildMessage;
import net.sourceforge.cruisecontrol.dashboard.LogFile;
import net.sourceforge.cruisecontrol.dashboard.MessageLevel;
import net.sourceforge.cruisecontrol.dashboard.saxhandler.BuildMessageExtractor;
import net.sourceforge.cruisecontrol.dashboard.saxhandler.CompositeExtractor;
import net.sourceforge.cruisecontrol.dashboard.saxhandler.SAXBasedExtractor;
import net.sourceforge.cruisecontrol.dashboard.saxhandler.StackTraceExtractor;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.xml.sax.SAXException;

public class ErrorsAndWarningsMessagesWidget implements Widget {

    private final SAXBasedExtractor extractor;

    public ErrorsAndWarningsMessagesWidget() {
        this(new CompositeExtractor(Arrays.asList(handlers())));
    }

    private static SAXBasedExtractor[] handlers() {
        return new SAXBasedExtractor[] {new BuildMessageExtractor(), new StackTraceExtractor()};
    }

    ErrorsAndWarningsMessagesWidget(SAXBasedExtractor extractor) {
        this.extractor = extractor;
    }

    public String getDisplayName() {
        return "Errors and Warnings";
    }

    public Object getOutput(Map parameters) {
        try {
            parseLogfile(parameters);
            HashMap props = new HashMap();
            extractor.report(props);
            return parseMessage(props);

        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private void parseLogfile(Map parameters) throws ParserConfigurationException, SAXException, IOException {
        SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
        LogFile logFile = (LogFile) parameters.get(Widget.PARAM_BUILD_LOG_FILE);
        saxParser.parse(logFile.getInputStream(), extractor);
    }

    private String parseMessage(Map props) {
        String replaced = buildError(props, HTML_TEMPLATE_START);
        replaced = errorsAndWarnings((List) props.get(BuildMessageExtractor.KEY_MESSAGES), replaced);
        replaced = stacktrace(props, replaced);
        return replaced + "</div>";
    }

    private String errorsAndWarnings(List messages, String currentHtml) {
        StringBuffer sb = new StringBuffer();
        for (Iterator iter = messages.iterator(); iter.hasNext();) {
            BuildMessage message = (BuildMessage) iter.next();
            MessageLevel level = message.getLevel();
            if (MessageLevel.WARN.equals(level) || MessageLevel.ERROR.equals(level)) {
                sb.append(message.getMessage()).append("<br/>");
            }
        }
        String error = StringUtils.defaultIfEmpty(sb.toString(), "No errors or warnings");

        String errorsAndWarningsHtml = StringUtils.replace(ERRORS_AND_WARNINGS_HTML, "$errors",
                StringEscapeUtils.escapeHtml(error));
        boolean hasErrorsOrWarnings = !StringUtils.isEmpty(sb.toString());
        return currentHtml + makeToggleable(errorsAndWarningsHtml, "errors_and_warnings_element", hasErrorsOrWarnings);
    }

    private String makeToggleable(String htmlSnippet, String element, boolean shouldToggle) {
        String className = shouldToggle
               ? "class=\"collapsible_title title_message_collapsed\""
               : "";
        String style = shouldToggle
                ? "style='display:none;'"
                : "";
        String nextElementClassName = shouldToggle
        ? "class='collapsible_content'"
                : "";
        String newSnippet = htmlSnippet;
        newSnippet = StringUtils.replace(newSnippet, "$className", className);
        newSnippet = StringUtils.replace(newSnippet, "$style", style);
        newSnippet = StringUtils.replace(newSnippet, "$nextElementClassName", nextElementClassName);
        return newSnippet;
    }

    private String stacktrace(Map props, String currentHtml) {
        boolean hasStacktrace = !StringUtils.isEmpty(props.get(StackTraceExtractor.KEY_STACKTRACE).toString());
        String stacktrace = StringUtils.replace(STACKTRACE_HTML, "$stacktrace",
                getMessage(props, StackTraceExtractor.KEY_STACKTRACE, "No stacktrace"));
        return currentHtml + makeToggleable(stacktrace, "stacktrace", hasStacktrace);
    }

    private String buildError(Map props, String currentHtml) {
        String buildErrorMessage = StringUtils.replace(BUILD_ERROR_MESSAGE_HTML, "$buildError",
                getMessage(props, StackTraceExtractor.KEY_ERROR, "No error message"));
        return currentHtml + buildErrorMessage;
    }

    private String getMessage(Map props, String key, String defaultMsg) {
        return StringUtils.defaultIfEmpty(props.get(key).toString(), defaultMsg);
    }

    private static final String HTML_TEMPLATE_START =
        "<div>";
    private static final String BUILD_ERROR_MESSAGE_HTML =
        "<h2>Build Error Message</h2>$buildError<hr/>";
    private static final String ERRORS_AND_WARNINGS_HTML =
        "<h2 $className>Errors and Warnings</h2>"
            + "<div id='errors_and_warnings_element' $nextElementClassName $style><pre>$errors</pre></div><hr/>";
    private static final String STACKTRACE_HTML =
        "<h2 $className>Stacktrace</h2>"
            + "<div  $nextElementClassName $style><pre>$stacktrace</pre></div>";

}
