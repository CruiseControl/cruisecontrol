package net.sourceforge.cruisecontrol.logmanipulators;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;
import net.sourceforge.cruisecontrol.util.IO;
import org.apache.log4j.Logger;

public class GZIPManipulator extends BaseManipulator {
    private static final Logger LOG = Logger.getLogger(GZIPManipulator.class);
    private static final int BUFFER_SIZE = 4 * 1024;

    public void execute(String logDir) {
        File[] filesToGZip = getRelevantFiles(logDir, false);
        for (int i = 0; i < filesToGZip.length; i++) {
            File file = filesToGZip[i];
            gzipFile(file, logDir);        
        }
    }

    private void gzipFile(File logfile, String logDir) {
        OutputStream out = null;
        InputStream in = null;
        try {
            String fileName = logfile.getName() + ".gz";

            out = new GZIPOutputStream(
                    new FileOutputStream(new File(logDir, fileName)));
            in = new FileInputStream(logfile);
            int len;
            byte[] buffer = new byte[BUFFER_SIZE];
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0 ,len);
            }
            out.flush();
            logfile.delete();
        } catch (IOException e) {
            LOG.warn("could not gzip " + logfile.getName() + ": " +e.getMessage(), e);
        } finally {
            IO.close(out);
            IO.close(in);
        }
    }

}
