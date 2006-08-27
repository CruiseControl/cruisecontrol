/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2005, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 600
 * Chicago, IL 60661 USA
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *     + Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     + Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 *     + Neither the name of ThoughtWorks, Inc., CruiseControl, nor the
 *       names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior
 *       written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/
package net.sourceforge.cruisecontrol.config;

import net.sourceforge.cruisecontrol.ConfigManager;
import net.sourceforge.cruisecontrol.CruiseControlConfig;
import net.sourceforge.cruisecontrol.ProjectConfig;
import net.sourceforge.cruisecontrol.CruiseControlException;

import net.sourceforge.cruisecontrol.util.Util;
import net.sourceforge.cruisecontrol.util.IO;

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
    private CruiseControlConfig config =  new CruiseControlConfig();
    private String hash;

    public XMLConfigManager(File file) throws CruiseControlException {
        setConfigFile(file);
    }

    private void loadConfig(File file) throws CruiseControlException {
        LOG.info("reading settings from config file [" + file.getAbsolutePath() + "]");
        Element element = Util.loadConfigFile(file);
        config = new CruiseControlConfig();
        config.configure(element);
    }

    public void setConfigFile(File fileName) throws CruiseControlException {
        LOG.debug("Config file set to [" + fileName + "]");
        configFile = fileName;
        LOG.debug("Calculating MD5 [" + configFile.getAbsolutePath() + "]");
        hash = calculateMD5(configFile);
        loadConfig(configFile);
    }

    public Set getProjectNames() {
        return Collections.unmodifiableSet(config.getProjectNames());
    }

    public boolean reloadIfNecessary() throws CruiseControlException {
        LOG.debug("Calculating MD5 [" + configFile.getAbsolutePath() + "]");
        String newHash = calculateMD5(configFile);
        final boolean fileChanged = !newHash.equals(hash);
        if (fileChanged) {
            loadConfig(configFile);
            hash = newHash;
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
            IO.close(stream);
        }
        return md5;
    }

    /** For tests purposes. FIXME. move tests in same package */
    public CruiseControlConfig getCruiseControlConfig() {
        return config;
    }
}
