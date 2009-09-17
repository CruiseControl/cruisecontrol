package net.sourceforge.cruisecontrol.dashboard.widgets;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
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

    ErrorsAndWarningsMessagesWidget(final SAXBasedExtractor extractor) {
        this.extractor = extractor;
    }

    public String getDisplayName() {
        return "Errors and Warnings";
    }

    public Object getOutput(final Map parameters) {
        try {
            parseLogfile(parameters);
            final HashMap props = new HashMap();
            extractor.report(props);
            return parseMessage(props);

        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private void parseLogfile(final Map parameters) throws ParserConfigurationException, SAXException, IOException {
        final SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
        final LogFile logFile = (LogFile) parameters.get(Widget.PARAM_BUILD_LOG_FILE);
        saxParser.parse(logFile.getInputStream(), extractor);
    }

    private String parseMessage(final Map props) {
        String replaced = buildError(props);
        replaced = errorsAndWarnings((List<BuildMessage>) props.get(BuildMessageExtractor.KEY_MESSAGES), replaced);
        replaced = stacktrace(props, replaced);
        return replaced + "</div>";
    }

    private String errorsAndWarnings(final List<BuildMessage> messages, final String currentHtml) {
        final StringBuffer sb = new StringBuffer();
        for (final BuildMessage message : messages) {
            final MessageLevel level = message.getLevel();
            if (MessageLevel.WARN.equals(level) || MessageLevel.ERROR.equals(level)) {
                sb.append(message.getMessage()).append("\n"); // RHT 08/05/2008 was <br/>
            }
        }
        final String error = StringUtils.defaultIfEmpty(sb.toString(), "No errors or warnings");

        final String errorsAndWarningsHtml = StringUtils.replace(ERRORS_AND_WARNINGS_HTML, "$errors",
                StringEscapeUtils.escapeHtml(error));
        final boolean hasErrorsOrWarnings = !StringUtils.isEmpty(sb.toString());
        return currentHtml + makeToggleable(errorsAndWarningsHtml, hasErrorsOrWarnings);
    }

    private String makeToggleable(final String htmlSnippet, final boolean shouldToggle) {
        final String className = shouldToggle
               ? "class=\"collapsible_title title_message_collapsed\""
               : "";
        final String style = shouldToggle
                ? "style='display:none;'"
                : "";
        final String nextElementClassName = shouldToggle
        ? "class='collapsible_content'"
                : "";
        String newSnippet = htmlSnippet;
        newSnippet = StringUtils.replace(newSnippet, "$className", className);
        newSnippet = StringUtils.replace(newSnippet, "$style", style);
        newSnippet = StringUtils.replace(newSnippet, "$nextElementClassName", nextElementClassName);
        return newSnippet;
    }

    private String stacktrace(final Map props, final String currentHtml) {
        final boolean hasStacktrace = !StringUtils.isEmpty(props.get(StackTraceExtractor.KEY_STACKTRACE).toString());
        final String stacktrace = StringUtils.replace(STACKTRACE_HTML, "$stacktrace",
                getMessage(props, StackTraceExtractor.KEY_STACKTRACE, "No stacktrace"));
        return currentHtml + makeToggleable(stacktrace, hasStacktrace);
    }

    private String buildError(final Map props) {
        final String buildErrorMessage = StringUtils.replace(BUILD_ERROR_MESSAGE_HTML, "$buildError",
                getMessage(props, StackTraceExtractor.KEY_ERROR, "No error message"));
        return ErrorsAndWarningsMessagesWidget.HTML_TEMPLATE_START + buildErrorMessage;
    }

    private String getMessage(final Map props, final String key, final String defaultMsg) {
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
