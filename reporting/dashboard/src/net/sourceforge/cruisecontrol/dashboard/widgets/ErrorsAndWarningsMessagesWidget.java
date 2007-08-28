package net.sourceforge.cruisecontrol.dashboard.widgets;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.sourceforge.cruisecontrol.dashboard.BuildMessage;
import net.sourceforge.cruisecontrol.dashboard.MessageLevel;
import net.sourceforge.cruisecontrol.dashboard.saxhandler.BuildMessageExtractor;
import net.sourceforge.cruisecontrol.dashboard.saxhandler.CompositeExtractor;
import net.sourceforge.cruisecontrol.dashboard.saxhandler.SAXBasedExtractor;
import net.sourceforge.cruisecontrol.dashboard.saxhandler.StackTraceExtractor;

import org.apache.commons.lang.StringUtils;
import org.xml.sax.SAXException;

public class ErrorsAndWarningsMessagesWidget implements Widget {

    private final SAXBasedExtractor extractor;

    public ErrorsAndWarningsMessagesWidget() {
        this(new CompositeExtractor(new SAXBasedExtractor[] {new BuildMessageExtractor(),
                new StackTraceExtractor()}));
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
        saxParser.parse((File) parameters.get(Widget.PARAM_BUILD_LOG_FILE), extractor);
    }

    private String parseMessage(Map props) {
        List messages = (List) props.get(BuildMessageExtractor.KEY_MESSAGES);
        String replaced = antError(props);
        replaced = stacktrace(props, replaced);
        return errorsAndWarnings(messages, replaced);
    }

    private String errorsAndWarnings(List messages, String replaced) {
        StringBuffer sb = new StringBuffer();
        for (Iterator iter = messages.iterator(); iter.hasNext();) {
            BuildMessage message = (BuildMessage) iter.next();
            MessageLevel level = message.getLevel();
            if (MessageLevel.WARN.equals(level) || MessageLevel.ERROR.equals(level)) {
                sb.append(message.getMessage()).append("<br>");
            }
        }
        String errors = StringUtils.defaultIfEmpty(sb.toString(), "No errors or warnings");
        return StringUtils.replace(replaced, "$errors", errors);
    }

    private String stacktrace(Map props, String replaced) {
        return StringUtils.replace(replaced, "$stacktrace", getMessage(props,
                StackTraceExtractor.KEY_STACKTRACE, "No stacktrace"));
    }

    private String antError(Map props) {
        return StringUtils.replace(HTML_TEMPLATE, "$antError", getMessage(props,
                StackTraceExtractor.KEY_ERROR, "No error message"));
    }

    private String getMessage(Map props, String key, String defaultMsg) {
        return StringUtils.defaultIfEmpty(props.get(key).toString(), defaultMsg);
    }

    private static final String HTML_TEMPLATE =
            "<h2>Build Error Message</h2>$antError<h2>Errors and Warnings</h2>$errors<h2>Stacktrace</h2>$stacktrace";

}
