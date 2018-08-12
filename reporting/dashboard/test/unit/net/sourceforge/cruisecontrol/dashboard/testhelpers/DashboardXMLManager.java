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
package net.sourceforge.cruisecontrol.dashboard.testhelpers;

import java.io.File;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.ProjectInterface;
import net.sourceforge.cruisecontrol.ResolverHolder;
import net.sourceforge.cruisecontrol.config.FileResolver;
import net.sourceforge.cruisecontrol.config.XmlResolver;
import net.sourceforge.cruisecontrol.util.Util;

import org.apache.log4j.Logger;
import org.jdom2.Element;

/**
 * 
 * @author jerome@coffeebreaks.org
 * @version $Id: XMLConfigManager.java 3052 2007-04-22 06:56:35Z jfredrick $
 */
public class DashboardXMLManager implements ResolverHolder {

    private static final Logger LOG = Logger.getLogger(DashboardXMLManager.class);

    private final File configFile;

    private DashboardConfig config;

    private Resolver resolver = new Resolver();

    public DashboardXMLManager(File file) throws CruiseControlException {
        configFile = file;
        loadConfig(configFile);
    }

    private void loadConfig(final File file) throws CruiseControlException {
        final Element element = Util.loadRootElement(file);
        resolver.resetResolvedFiles();
        config = new DashboardConfig(element, this);
    }

    public File getConfigFile() {
        return configFile;
    }

    public DashboardConfig getCruiseControlConfig() {
        return config;
    }

    public ProjectInterface getProject(String projectName) {
        LOG.info("using settings from config file [" + configFile.getAbsolutePath() + "]");
        return config.getProject(projectName);
    }

    /** Th implementation of {@link ResolverHolder#getFileResolver} */
    public FileResolver getFileResolver() {
        return resolver;
    }

    /** Th implementation of {@link ResolverHolder#getXmlResolver} */
    public XmlResolver getXmlResolver() {
        return resolver;
    }

    
    class Resolver implements XmlResolver, FileResolver {
        private Set<File> resolvedFiles = new HashSet<File>();

        public Element getElement(final String path) throws CruiseControlException {
            final File file = new File(configFile.getParentFile(), path);
            resolvedFiles.add(file);
            return Util.loadRootElement(file);
        }

        /** The implementation of @{FileResolver#getInputStream(String)}. Throws exception
         *  at this time! */
        public InputStream getInputStream(final String path) throws CruiseControlException {
            throw new CruiseControlException("Method not implemented");
        }

        public Set<File> getResolvedFiles() {
            return resolvedFiles;
        }

        public void resetResolvedFiles() {
            resolvedFiles.clear();
        }
    }
}
