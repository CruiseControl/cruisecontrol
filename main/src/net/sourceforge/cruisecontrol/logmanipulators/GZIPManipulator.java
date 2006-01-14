package net.sourceforge.cruisecontrol.logmanipulators;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

public class GZIPManipulator extends BaseManipulator {

    public void execute(String logDir) {
        File[] filesToGZip = getRelevantFiles(logDir, false);
        for (int i = 0; i < filesToGZip.length; i++) {
            File file = filesToGZip[i];
            gzipFile(file, logDir);        
        }
    }

    private void gzipFile(File logfile, String logDir) {
        try {
            String fileName = logfile.getName() + ".gz";

            GZIPOutputStream out = new GZIPOutputStream(
                    new FileOutputStream(new File(logDir, fileName)));
            FileInputStream in = new FileInputStream(logfile);
            int len;
            while ((len = in.read()) > 0) {
                out.write(len);
            }
            in.close();
            out.flush();
            out.close();
            logfile.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
