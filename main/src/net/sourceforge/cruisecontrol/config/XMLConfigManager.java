/**
 * Created by IntelliJ IDEA.
 * User: jerome
 * Date: Oct 30, 2005
 * Time: 8:52:36 AM
 */
// START LICENSE
// END LICENSE

package net.sourceforge.cruisecontrol.config;

import net.sourceforge.cruisecontrol.ConfigManager;
import net.sourceforge.cruisecontrol.CruiseControlConfig;
import net.sourceforge.cruisecontrol.ProjectConfig;
import net.sourceforge.cruisecontrol.CruiseControlException;

import net.sourceforge.cruisecontrol.util.Util;

import java.io.File;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.Set;
import java.util.Collections;

import org.jdom.Element;
import org.jdom.output.XMLOutputter;

import org.apache.log4j.Logger;
import com.twmacinta.util.MD5OutputStream;

/**
 *
 * @author jerome@coffeebreaks.org
 * @version $Id$
 */
public class XMLConfigManager implements ConfigManager {

    private static final Logger LOG = Logger.getLogger(XMLConfigManager.class);
    private File configFile;
    private CruiseControlConfig config;
    private String hash;

    public XMLConfigManager(File file) throws CruiseControlException {
        setConfigFile(file);
    }

    private void loadConfig(File file) throws CruiseControlException {
        LOG.info("reading settings from config file [" + hash + "]");
        Element element = Util.loadConfigFile(file);
        config = new CruiseControlConfig();
        config.configure(element);
    }

    public void setConfigFile(File fileName) throws CruiseControlException {
        LOG.debug("Config file set to [" + fileName + "]");
        configFile = fileName;
        LOG.info("Calculating MD5 [" + configFile.getAbsolutePath() + "]");
        hash = calculateMD5(configFile);
        loadConfig(configFile);
    }

    public Set getProjectNames() {
        return Collections.unmodifiableSet(config.getProjectNames());
    }

    public boolean reloadIfNecessary() throws CruiseControlException {
        LOG.info("Calculating MD5 [" + configFile.getAbsolutePath() + "]");
        String newHash = calculateMD5(configFile);
        final boolean fileChanged = !newHash.equals(hash);
        if (fileChanged) {
            loadConfig(configFile);
        }
        return fileChanged;
    }

    public ProjectConfig getConfig(String projectName) throws CruiseControlException {
        LOG.info("using settings from config file [" + configFile.getAbsolutePath() + "]");
        return config.getConfig(projectName);
    }

    public File getConfigFile() {
        return configFile;
    }

    public static String calculateMD5(File file) {
        String md5 = null;
        MD5OutputStream stream = null;
        try {
            Element element = Util.loadConfigFile(file);
            stream = new MD5OutputStream(new ByteArrayOutputStream());
            XMLOutputter outputter = new XMLOutputter();
            outputter.output(element, stream);
            md5 = stream.getMD5().asHex();
        } catch (IOException e) {
            LOG.error("exception calculating MD5 of config file " + file.getAbsolutePath(), e);
        } catch (CruiseControlException e) {
            LOG.error("exception calculating MD5 of config file " + file.getAbsolutePath(), e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ignore) {
                }
            }
        }
        return md5;
    }

    /** For tests purposes. FIXME. move tests in same package */
    public CruiseControlConfig getCruiseControlConfig() {
        return config;
    }
}
