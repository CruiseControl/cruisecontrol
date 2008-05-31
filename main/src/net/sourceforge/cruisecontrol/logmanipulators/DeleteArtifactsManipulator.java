package net.sourceforge.cruisecontrol.logmanipulators;

import net.sourceforge.cruisecontrol.util.DateUtil;
import net.sourceforge.cruisecontrol.util.IO;

import java.io.FilenameFilter;
import java.io.File;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import org.apache.log4j.Logger;

/**
 * Delete old build artifacts.
 * @author Dan Rollo
 * Date: May 29, 2008
 * Time: 11:44:45 PM
 */
public class DeleteArtifactsManipulator extends BaseManipulator {

    private static final Logger LOG = Logger.getLogger(DeleteArtifactsManipulator.class);

    private static final SimpleDateFormat FORMATTER = new SimpleDateFormat(DateUtil.SIMPLE_DATE_FORMAT);


    protected FilenameFilter getFilenameFilter(final Date logdate, final boolean ignoreSuffix) {
        return new ArtifactFilter(logdate);
    }


    private class ArtifactFilter implements FilenameFilter {

        private final Date logdate;

        public ArtifactFilter(final Date logdate) {
            this.logdate = logdate;
        }

        public boolean accept(final File dir, final String name) {

            if (name.startsWith("log")) {
                return false;
            }
            
            final Date artifactDirDate;
            try {
                artifactDirDate = FORMATTER.parse(name);
            } catch (ParseException e) {
                return false;
            }
            if (artifactDirDate.before(logdate)) {
                final File checkArtifactDir = new File(dir, name);
                return checkArtifactDir.exists() && checkArtifactDir.isDirectory();
            }
            return false;
        }

    }


    public void execute(final String logDir) {
        File[] deleteFiles = getRelevantFiles(logDir, true);
        for (int i = 0; i < deleteFiles.length; i++) {
            LOG.debug("Deleting artifacts directory: " + deleteFiles[i].getAbsolutePath());
            IO.delete(deleteFiles[i]);
        }
    }



}
