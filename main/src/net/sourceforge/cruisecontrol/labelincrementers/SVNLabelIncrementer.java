package net.sourceforge.cruisecontrol.labelincrementers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import net.sourceforge.cruisecontrol.LabelIncrementer;

import org.apache.log4j.Logger;
import org.jdom2.Element;

/**
 * This class provides a label incrementation based on svn revision numbers.
 * This class expects the label format to be "x&lt;sep&gt;y[&lt;sep&gt;z]",
 * where x is any String and y is an integer and &lt;sep&gt; a separator, the
 * last part z, is optional, and gets generated and later incremented in case a
 * build is forced. The default separator is "." and can be modified using
 * {@link #setSeparator}.
 *
 * @author Ketan Padegaonkar &lt; KetanPadegaonkar gmail &gt;
 */
public class SVNLabelIncrementer implements LabelIncrementer {
    private static final Logger LOG = Logger.getLogger(SVNLabelIncrementer.class);

    private String workingCopyPath = ".";

    private String labelPrefix = "svn";

    private String separator = ".";

    public boolean isPreBuildIncrementer() {
        return true;
    }

    public String incrementLabel(String oldLabel, Element buildLog) {
        String revisionNumber = "";
        String result = oldLabel;
        try {
            revisionNumber = getSvnRevision();
            if (revisionNumber == null || revisionNumber.equals("")) {
                return labelPrefix;
            }
            result = labelPrefix + getSeparator() + revisionNumber;

            if (oldLabel.indexOf(result) > -1) {
                int lastSeparator = oldLabel.lastIndexOf(getSeparator());
                int firstSeparator = oldLabel.indexOf(getSeparator());
                int lastPart = 1;
                if (lastSeparator != firstSeparator) {
                    String suffix = oldLabel.substring(lastSeparator + 1);
                    lastPart = Integer.parseInt(suffix) + 1;
                }
                result += getSeparator() + lastPart;
            }
            LOG.debug("Incrementing label from " + oldLabel + " to " + result);
        } catch (IOException e) {
            LOG.error("could not execute svn binary", e);
        } catch (NumberFormatException e) {
            LOG.error("could not increment label. Old label was " + oldLabel + ". svn revision was " + revisionNumber,
                    e);
        }

        return result;
    }

    protected String getSvnRevision() throws IOException {
        String rev;
        Process p = null;
        try {
            p = Runtime.getRuntime().exec(new String[]{"svnversion", workingCopyPath});
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            rev = stdInput.readLine();
        } finally {
            if (p != null) {
                p.destroy();
            }
        }
        LOG.debug("SVN revision is: " + rev);
        return rev;
    }

    public boolean isValidLabel(String label) {
        // we don't mind what the previous label is,
        // when the next label is built, then parsing is performed to add / increment a suffix.
        return true;
    }

    public void setWorkingCopyPath(String path) {
        LOG.debug("Working Path is: " + path);
        workingCopyPath = path;
    }

    public String getLabelPrefix() {
        return this.labelPrefix;
    }

    public void setLabelPrefix(String labelPrefix) {
        this.labelPrefix = labelPrefix;
    }

    public String getDefaultLabel() {
        return getLabelPrefix() + getSeparator() + "0";
    }

    public String getSeparator() {
        return this.separator;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }
}
