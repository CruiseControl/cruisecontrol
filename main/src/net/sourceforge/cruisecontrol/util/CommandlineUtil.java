package net.sourceforge.cruisecontrol.util;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.InputStream;

import org.apache.log4j.Logger;


/**
 * A utility that helps to run command lines.
 * 
 * @author Scott Coplin
 */
public final class CommandlineUtil {
    private static final Logger LOG =
        Logger.getLogger(CommandlineUtil.class);
    
    /**
     * Private constructor to prevent instantiation.
     */
    private CommandlineUtil() {
        // Do nothing
    }
    
    /**
     * Run a command line and stream the output.  It is expected that the 
     * stream that is returned is read to the end and then closed.  This
     * will ensure that it releases any associated resources.  The error stream
     * will be sent to the log.
     * 
     * @param command The command to run.
     * @return The output stream of the command.
     */
    public static InputStream streamOutput(Commandline command) {        
        try {
            Process p = command.execute();
            WarningConsumer consumer = new WarningConsumer(LOG);
            StreamPumper errorPumper = new StreamPumper(p.getErrorStream(), consumer);
            new Thread(errorPumper).start();
            return new ProcessStreamWrapper(p);
        } catch (Exception e) {
            LOG.error("Error in executing the command : ", e);
            return new ByteArrayInputStream(new byte[0]);
        }
    }
    
    private static final class WarningConsumer implements StreamConsumer {
        private final Logger log;

        private WarningConsumer(Logger log) {
            super();
            this.log = log;
        }

        public void consumeLine(String line) {
            this.log.warn(line);
        }
    }

    private static final class ProcessStreamWrapper
        extends FilterInputStream {
        private Process process;
        
        public ProcessStreamWrapper(Process process) {
            super(process.getInputStream());
            this.process = process;
        }
        
        public void close() throws IOException {
            try {
                this.process.waitFor();
            } catch (InterruptedException e) {
                InterruptedIOException ioe = new InterruptedIOException();
                ioe.initCause(e);
                throw ioe;
            }
            
            this.process.getInputStream().close();
            this.process.getOutputStream().close();
            this.process.getErrorStream().close();
        }
    }
}
