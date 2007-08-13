package net.sourceforge.cruisecontrol.builders;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.DefaultLogger;

import java.io.PrintStream;

/**
 * Ant Logger impl used to send progress messages back to an AntBuilder.
 * Intended for use in conjuction with {@link AntProgressXmlListener}.
 *
 * Ideas adapted from contrib/XmlLoggerWithStatus, written by IgorSemenko (igor@semenko.com).
 *
 * @author Dan Rollo
 * Date: Aug 10, 2007
 * Time: 5:07:53 AM
 * To change this template use File | Settings | File Templates.
 */
public class AntProgressXmlLogger extends DefaultLogger {

    public void targetStarted(BuildEvent event) {
        if (Project.MSG_INFO <= msgOutputLevel
                && !event.getTarget().getName().equals("")) {

            final String name = event.getTarget().getName();

            // @todo Add filter support, like XmlLoggerWithStatus
//        if (this.targetFilter != null && name.matches(this.targetFilter)){
//            return;
//        }

            final String msg = AntProgressLogger.MSG_PREFIX_ANT_PROGRESS + name;
            // use super to actually print to sysout
            super.printMessage(msg, out, event.getPriority());
            log(msg);
        }
    }

    /**
     * Prints a message to a PrintStream.
     *
     * @param message  The message to print.
     *                 Should not be <code>null</code>.
     * @param stream   A PrintStream to print the message to.
     *                 Must not be <code>null</code>.
     * @param priority The priority of the message.
     *                 (Ignored in this implementation.)
     */
    protected void printMessage(final String message,
                                final PrintStream stream,
                                final int priority) {

        // no-op to stop console output - similar behavior to XmlLogger when used as Logger;
    }


    /** @return current value of message output level */
    public int getMessageOutputLevel() {
        return msgOutputLevel;
    }
}
