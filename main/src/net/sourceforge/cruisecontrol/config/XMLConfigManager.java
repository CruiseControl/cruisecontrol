/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2005, ThoughtWorks, Inc.
 * 200 E. Randolph, 25th Floor
 * Chicago, IL 60601 USA
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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import net.sourceforge.cruisecontrol.CruiseControlConfig;
import net.sourceforge.cruisecontrol.CruiseControlController;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.ProjectInterface;
import net.sourceforge.cruisecontrol.ResolverHolder;
import net.sourceforge.cruisecontrol.util.Util;

import org.apache.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;

import com.twmacinta.util.MD5OutputStream;

/**
 *
 * @author jerome@coffeebreaks.org
 * @version $Id$
 */
public class XMLConfigManager implements ResolverHolder {

    private static final Logger LOG = Logger.getLogger(XMLConfigManager.class);
    private final File configFile;
    private CruiseControlConfig config;
    private String hash;
    private final Resolver resolver = new Resolver();
    private final CruiseControlController controller;

    public XMLConfigManager(File configurationFile) throws CruiseControlException {
        this(configurationFile, null);
    }

    public XMLConfigManager(File file, CruiseControlController controller) throws CruiseControlException {
        configFile = file;
        this.controller = controller;
        loadConfig(configFile);
        hash = calculateMD5(configFile);
    }

    private void loadConfig(File file) throws CruiseControlException {
        LOG.info("reading settings from config file [" + file.getAbsolutePath() + "]");
        Element element = Util.loadRootElement(file);
        resolver.resetResolvedFiles();
        config = new CruiseControlConfig(element, this, controller);
    }

    public File getConfigFile() {
        return configFile;
    }

    public CruiseControlConfig getCruiseControlConfig() {
        return config;
    }

    public ProjectInterface getProject(String projectName) {
        LOG.info("using settings from config file [" + configFile.getAbsolutePath() + "]");
        return config.getProject(projectName);
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

    /** The implementation of {@link ResolverHolder#getFileResolver} */
    public FileResolver getFileResolver() {
        return resolver;
    }

    /** The implementation of {@link ResolverHolder#getXmlResolver} */
    public XmlResolver getXmlResolver() {
        return resolver;
    }

    private String calculateMD5(final File file)  {
        LOG.debug("Calculating MD5 [" + configFile.getAbsolutePath() + "]");
        String md5 = calculatePartialMD5(file);
        final Set<File> includedFiles = resolver.getResolvedFiles();
        for (final File includedFile : includedFiles) {
            md5 += calculatePartialMD5(includedFile);
        }
        return md5;
    }

    private String calculatePartialMD5(final File file) {
        final MD5OutputStream stream = new MD5OutputStream(new ByteArrayOutputStream());
        // Load as XML
        try {
            final Element element = Util.loadRootElement(file);
            final XMLOutputter outputter = new XMLOutputter();
            outputter.output(element, stream);
            return stream.getMD5().asHex();
        } catch (Exception e) {
            LOG.debug("exception calculating MD5 of XML file " + file.getAbsolutePath(), e);
            LOG.debug("trying to read is as ordinary text file");
        }
        // Load as normal file
        try {
            final String content = Util.readFileToString(file);
            stream.write(content.getBytes());
            return stream.getMD5().asHex();
        } catch (IOException e) {
            LOG.error("exception calculating MD5 of file " + file.getAbsolutePath(), e);
        }

        return "";
    }

    class Resolver implements XmlResolver, FileResolver {
        private final Set<File> resolvedFiles = new HashSet<File>();

        public Element getElement(final String path) throws CruiseControlException {
            final File file = getPath(path);
            resolvedFiles.add(file);
            return Util.loadRootElement(file);
        }

        public InputStream getInputStream(final String path) throws CruiseControlException {
            final File file = getPath(path);
            resolvedFiles.add(file);
            try {
                return new BufferedInputStream(new FileInputStream(file));
            } catch (FileNotFoundException e) {
                throw new CruiseControlException("exception when opening file " + file.getAbsolutePath(), e);
            }
        }

        public Set<File> getResolvedFiles() {
            return resolvedFiles;
        }

        public void resetResolvedFiles() {
            resolvedFiles.clear();
        }

        private File getPath(final String path) throws CruiseControlException {
            try {
              final File file = new File(path);
              // Accessible from the current working dir (i.e. the path is either absolute
              // in relative to the working dir
              if (file.exists()) {
                return file;
                }
              // Not found ...
              LOG.debug("file " + file.getCanonicalPath() + " is not accessible; expecting it relative to "
                + configFile.getParentFile().getCanonicalPath());
              return new File(configFile.getParentFile(), path);

            } catch (IOException e) {
                throw new CruiseControlException("Invalid file path " + path, e);
            }
        }
    }
}
