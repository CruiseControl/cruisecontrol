package net.sourceforge.cruisecontrol.distributed.core;

import org.apache.log4j.Logger;

import java.util.Properties;
import java.util.Date;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;

/**
 * Helper methods to get the build version, date info for CCDist classes.
 * 
 * @author Dan Rollo
 * Date: Feb 3, 2007
 * Time: 12:52:35 PM
 */
public final class CCDistVersion {

    private static final Logger LOG = Logger.getLogger(CCDistVersion.class);

    private static final Properties PROPS_CCDIST_BUILD_VERSION = loadCCDistBuildVersionProperties();

    /** Hidden constructor to prevent instantiation. */
    private CCDistVersion() {        
    }

    /**
     * Retrieves the current version information, as indicated in the
     * ccdist.version.properties file. Copied from CC Main.java
     * @return a props object containing build version info
     */
    private static Properties loadCCDistBuildVersionProperties() {
        Properties props = new Properties();
        try {
            props.load(CCDistVersion.class.getResourceAsStream("/ccdist.version.properties"));
        } catch (IOException e) {
            LOG.error("Error reading version properties", e);
        }
        return props;
    }


    /**
     * Writes the current version information to the logging information stream.
     */
    public static void printCCDistVersion() {
        LOG.info("CCDist Version " + getVersion() + " " + getVersionInfo());
    }

    private static String getVersionInfo() {
        return PROPS_CCDIST_BUILD_VERSION.getProperty("ccdist.version.info");
    }

    public static String getVersion() {
        return PROPS_CCDIST_BUILD_VERSION.getProperty("ccdist.version");
    }

    /** The date format in the properties file, should match the ant format used for ${TODAY} ${TSTAMP} */
    private static final DateFormat DF_ANT_TODAY_TSTAMP = new SimpleDateFormat("MMMM d yyyy hhmm");
    private static Date buildDate;
    public static Date getVersionBuildDate() {
        if (buildDate == null) {
            try {
                buildDate = DF_ANT_TODAY_TSTAMP.parse(PROPS_CCDIST_BUILD_VERSION.getProperty("ccdist.version.date"));
            } catch (ParseException e) {
                LOG.error("Error parsing build date: "
                        + PROPS_CCDIST_BUILD_VERSION.getProperty("ccdist.version.date"), e);
            }
        }
        return buildDate;
    }
}
