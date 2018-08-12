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
package net.sourceforge.cruisecontrol.util;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.gendoc.annotations.SkipDoc;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPath;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * <p>This class represents a plugin subelement that returns either a fixed value or uses an xpath expression to
 * retrieve a value from an XML document. Instances of this class represent an element that has either a fixed value,
 * e.g.</p>
 * <pre>
 * &lt;mychild value="foo"/&gt;
 * </pre>
 * <p>or one that has an xpath expression that will be executed against CruiseControl's log file at execution
 * time, e.g.</p>
 * <pre>
 * &lt;mychild xpathExpression="sum(cruisecontrol/testsuite/@tests)"/&gt;
 * </pre>
 * <p>or one that has an xpath expression that will be executed against a named xml file at execution
 * time, e.g.</p>
 * <pre>
 * &lt;mychild xpathExpression="sum(cruisecontrol/testsuite/@tests)" xmlFile="/path/to/my/xml/file"/&gt;
 * </pre>
 * For example usage in a plugin, see
 * {@link net.sourceforge.cruisecontrol.publishers.sfee.SfeeDocumentManagerPublisher#createDescription()}.
 *
 * @author <a href="mailto:pj@thoughtworks.com">Paul Julius</a>
 * @author <a href="mailto:krs@thoughtworks.com">Kent Spillner</a>
 * @see net.sourceforge.cruisecontrol.publishers.sfee.SfeeDocumentManagerPublisher
 */
public class XPathAwareChild {

    private static final Logger LOG = Logger.getLogger(XPathAwareChild.class);

    private String value;
    private String xpathExpression;
    private InputStream in;
    private String xmlFile;
    private boolean wasValidated;

    /**
     * If this value is set, then it is considered to be a "fixed" value that will be returned by the
     * <code>lookupValue</code> method. If this is set, then <code>xpathExpression</code> and <code>xmlFile</code>
     * should NOT be set.
     * @param value the new value
     */
    public void setValue(String value) {
        markDirty();
        this.value = value;
    }

    public void setXPathExpression(String xpathExpression) {
        markDirty();
        this.xpathExpression = xpathExpression;
    }

    /**
     * Used for testing. CruiseControl will not be able to call this method directly, so it has not applicability to
     * the intended use of this class. Use this method from unit tests to set an InputStream instead of an actual
     * xmlFile.
     * @param in the new input stream
     */
    @SkipDoc
    public void setInputStream(InputStream in) {
        markDirty();
        this.in = in;
    }

    public void setXMLFile(String filename) {
        markDirty();
        xmlFile = filename;
    }

    public String getFixedValue() {
        return value;
    }

    public String getXpathExpression() {
        return xpathExpression;
    }

    /**
     * Looks up the appropriate value based on how the class is being used. It will return either the fixed value, or
     * execute the xpath expression against the appropriate xml file/log.
     * @param log element to evaluate xpath expression against.
     * @return value or xpath result
     * @throws CruiseControlException if it breaks
     */
    public String lookupValue(Element log) throws CruiseControlException {
        if (!wasValidated) {
            throw new IllegalStateException(
                    "This child was not validated."
                            + " Should not be calling lookupValue() unless it has first been validated.");
        }

        if (value != null) {
            return value;
        } else {
            try {
                return evaluateXpath(log);
            } catch (Exception e) {
                throw new CruiseControlException(e);
            }
        }
    }

    private String evaluateXpath(Element log) throws IOException, JDOMException, CruiseControlException {

        Object searchContext;
        if (in == null && xmlFile == null && log == null) {
            throw new CruiseControlException("current cruisecontrol log not set.");
        } else if (xmlFile != null) {
            LOG.debug("Using file specified [" + xmlFile + "] to evaluate xpath.");
            searchContext = new SAXBuilder().build(new FileInputStream(new File(xmlFile)));
        } else if (in != null) {
            LOG.debug("Using the specified input stream to evaluate xpath. This should happen during testing.");
            searchContext = new SAXBuilder().build(in);
        } else {
            LOG.debug("Using CruiseControl's log file to evaluate xpath.");
            if (log.getParent() != null) {
                searchContext = log.getParent();
            } else {
                searchContext = new Document(log);
            }
        }

        XPath xpath = XPath.newInstance(xpathExpression);
        String result = xpath.valueOf(searchContext);
        LOG.debug("Evaluated xpath [" + xpathExpression + "] with result [" + result + "]");
        return result;
    }

    /**
     * Must be called after setting all the instance values and before calling <code>lookupValue</code>.
     *
     * @throws CruiseControlException if it breaks
     */
    public void validate() throws CruiseControlException {
        if (xpathExpression == null && xmlFile != null) {
            throw new CruiseControlException("xmlFile should only be set if xpathExpression is also set.");
        }

        if (value == null && xpathExpression == null) {
            throw new CruiseControlException("Either value or xpathExpression must be set.");
        }
        if (value != null && xpathExpression != null) {
            throw new CruiseControlException("value and xpathExpression should not both be set.");
        }


        markClean();
    }

    private void markClean() {
        wasValidated = true;
    }

    /**
     * Called to indicate that this classs needs to be validated, as after a change to a member variable.
     */
    protected void markDirty() {
        wasValidated = false;
    }


}
