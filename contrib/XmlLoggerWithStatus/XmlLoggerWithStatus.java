package net.sourceforge.cruisecontrol.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.XmlLogger;

/**
 * Extends XmlLogger by adding information about currently running target to a
 * file specified by system property <br>
 * XmlLoggerWithStatus.file
 * 
 * @author IgorSemenko (igor@semenko.com)
 */
public class XmlLoggerWithStatus extends XmlLogger {

    private Writer out;
    private boolean inited;
    private String targetFilter;

    private static final SimpleDateFormat FORMATTER = new SimpleDateFormat("HH:mm:ss");

    /**
     * Reads current content of file defined by system property
     * XmlLoggerWithStatus.file and creates a writer to it.
     * 
     * NOTE: this code MAY NOT be placed into buildStarted event because
     * properties are not available at that point yet.
     */
    public void init(BuildEvent event) {
        if (!inited) {
            String statusFileName = event.getProject().getProperty("XmlLoggerWithStatus.file");
            if (statusFileName == null) {
                statusFileName = "buildstatus.txt";
            }
            this.targetFilter = event.getProject().getProperty("XmlLoggerWithStatus.filter");

            //check directory
            File parentDir = new File(statusFileName).getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            // create a writer
            try {
                out = new FileWriter(statusFileName, true);
            } catch (Exception e) {
                System.err.println("Error opening file " + statusFileName + " for appending");
                e.printStackTrace(System.err);
            }
            inited = true;
        }
    }

    /**
     * Closes writer to build status.
     */
    public void buildFinished(BuildEvent event) {
        super.buildFinished(event);
        if (out != null) {
            try {
                out.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Adds a line with started target and timestamp.
     */
    public void targetStarted(BuildEvent event) {
        super.targetStarted(event);
        init(event);
        if (out != null) {
            String name = event.getTarget().getName();
            if (this.targetFilter != null && name.matches(this.targetFilter)){
                return;
            }
            StringBuffer content = new StringBuffer();
            content.append(System.getProperty("line.separator"));
            content.append(FORMATTER.format(new Date()));
            content.append(" [");
            content.append(name);
            content.append("] ");
            writeStatus(content);
        }
    }

    /**
     * Writes a line and flushes.
     */
    private void writeStatus(StringBuffer content) {
        try {
            out.write(content.toString());
            out.flush();
        } catch (IOException e) {
            System.err.println("Error writing statusline to writer");
            e.printStackTrace(System.err);
        }
    }

}
