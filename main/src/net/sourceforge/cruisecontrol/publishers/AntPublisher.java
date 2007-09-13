/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.publishers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Element;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Publisher;
import net.sourceforge.cruisecontrol.builders.AntBuilder;
import net.sourceforge.cruisecontrol.builders.Property;
import net.sourceforge.cruisecontrol.util.XMLLogHelper;

/**
 * A thin wrapper around the AntBuilder class, this class allows you to call an
 * Ant script as a publisher.
 * <p>
 * All properties set by CC and passed to the builder will be available as
 * properties within Ant.
 *
 * @author <a href="mailto:rjmpsmith@hotmail.com">Robert J. Smith </a>
 */
public class AntPublisher implements Publisher {

    private static final Logger LOG = Logger.getLogger(AntPublisher.class);

    private final AntBuilder delegate = new AntBuilder();

    /**
     * Constructor overrides default AntBuilder.showAntOutput value in delegate.
     * Required if showAntOutput defaults to true.
     */
    public AntPublisher() {
        delegate.setShowAntOutput(false);
    }

    /* (non-Javadoc)
     * @see net.sourceforge.cruisecontrol.Publisher#publish(org.jdom.Element)
     */
    public void publish(Element log) throws CruiseControlException {

        Map properties = new HashMap();

        populatePropertesForAntBuilder(log, properties);

        // Run Ant
        Element result = delegate.build(properties, null);
        if (result == null) {
            LOG.error("Publisher failed.\n\n");
        } else {
            Attribute error = result.getAttribute("error");
            if (error == null) {
                LOG.info("Publisher successful.");
            } else {
                LOG.error("Publisher failed.\n\n"
                        + error.getValue()
                        + "\n");
            }
        }
    }

    public void validate() throws CruiseControlException {
        delegate.validate();
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setSaveLogDir(String)
     */
    public void setSaveLogDir(String dir) {
        delegate.setSaveLogDir(dir);
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setAntWorkingDir(String)
     */
    public void setAntWorkingDir(String dir) {
        delegate.setAntWorkingDir(dir);
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setAntScript(String)
     */
    public void setAntScript(String antScript) {
        delegate.setAntScript(antScript);
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setAntHome(String)
     */
    public void setAntHome(String antHome) {
        delegate.setAntHome(antHome);
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setTempFile(String)
     */
    public void setTempFile(String tempFileName) {
        delegate.setTempFile(tempFileName);
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setTarget(String)
     */
    public void setTarget(String target) {
        delegate.setTarget(target);
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setBuildFile(String)
     */
    public void setBuildFile(String buildFile) {
        delegate.setBuildFile(buildFile);
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setPropertyfile(String)
     */
    public void setPropertyfile(String propertyfile) {
        delegate.setPropertyfile(propertyfile);
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setUseLogger(boolean)
     */
    public void setUseLogger(boolean useLogger) {
        delegate.setUseLogger(useLogger);
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#createJVMArg()
     */
    public Object createJVMArg() {
        return delegate.createJVMArg();
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#createLib()
     */
    public Object createLib() {
        return delegate.createLib();
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#createListener()
     */
    public Object createListener() {
        return delegate.createListener();
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#createProperty()
     */
    public Property createProperty() {
        return delegate.createProperty();
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setUseDebug(boolean)
     */
    public void setUseDebug(boolean debug) {
        delegate.setUseDebug(debug);
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setUseQuiet(boolean)
     */
    public void setUseQuiet(boolean quiet) {
        delegate.setUseQuiet(quiet);
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#getLoggerClassName()
     */
    public String getLoggerClassName() {
        return delegate.getLoggerClassName();
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setLoggerClassName(String)
     */
    public void setLoggerClassName(String string) {
        delegate.setLoggerClassName(string);
    }

    /**
     * @see net.sourceforge.cruisecontrol.builders.AntBuilder#setTimeout(long)
     */
    public void setTimeout(long timeout) {
        delegate.setTimeout(timeout);
    }

    void populatePropertesForAntBuilder(Element log, Map properties) {
        XMLLogHelper helper = new XMLLogHelper(log);
        if (helper.isBuildSuccessful()) {
            properties.put("thisbuildsuccessful", "true");
        } else {
            properties.put("thisbuildsuccessful", "false");
        }

        Iterator propertyIterator = log.getChild("info").getChildren("property").iterator();
        while (propertyIterator.hasNext()) {
            Element property = (Element) propertyIterator.next();
            properties.put(property.getAttributeValue("name"),
                    property.getAttributeValue("value"));
        }
    }
}
