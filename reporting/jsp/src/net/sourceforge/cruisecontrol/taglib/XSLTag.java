/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.taglib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;

import javax.servlet.ServletContext;
import javax.servlet.jsp.JspException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 *  JSP custom tag to handle xsl transforms.  This tag also caches the output of the transform to disk, reducing the
 *  number of transforms necessary.
 *
 *  @author alden almagro, ThoughtWorks, Inc. 2002
 */
public class XSLTag extends CruiseControlTagSupport {
    private static final String DEFAULT_XSL_ROOT = "/xsl/";
    private String xslFileName;
    private String xslRootContext = DEFAULT_XSL_ROOT;
    private static final String CACHE_DIR = "_cache";

    /**
     *  Perform an xsl transform.  This body of this method is based upon the xalan sample code.
     *
     *  @param xmlFile the xml file to be transformed
     *  @param in stream containing the xsl stylesheet
     *  @param out writer to output the results of the transform
     */
    protected void transform(File xmlFile, InputStream in, Writer out) {
        try {
            TransformerFactory tFactory = TransformerFactory.newInstance();
            javax.xml.transform.URIResolver resolver = new javax.xml.transform.URIResolver() {
                public javax.xml.transform.Source resolve(String href, String base) {
                    final ServletContext servletContext = getPageContext().getServletContext();
                    InputStream styleStream = servletContext.getResourceAsStream(xslRootContext + href);
                    if (styleStream != null) {
                        info("Using nested stylesheet for " + href);
                        return new StreamSource(styleStream);
                    } else {
                        info("Nested stylesheet not found for " + href);
                        return null;
                    }
                }
            };
            tFactory.setURIResolver(resolver);
            Transformer transformer = tFactory.newTransformer(new StreamSource(in));
            transformer.transform(new StreamSource(xmlFile), new StreamResult(out));
        } catch (TransformerException e) {
            err(e);
        }
    }

    /**
     *  Determine whether the cache file is current or not.  The file will be current if it is newer than both the
     *  xml log file and the xsl file used to create it.
     *
     *  @return true if the cache file is current.
     */
    protected boolean isCacheFileCurrent(File xmlFile, File cacheFile) {
        if (!cacheFile.exists()) {
            return false;
        }
        long xmlLastModified = xmlFile.lastModified();
        long xslLastModified = xmlLastModified;
        long cacheLastModified = cacheFile.lastModified();
        try {
            URL url = getPageContext().getServletContext().getResource(xslFileName);
            URLConnection con = url.openConnection();
            xslLastModified = con.getLastModified();
        } catch (Exception e) {
            err("Failed to retrieve lastModified of xsl file " + xslFileName);
        }
        return (cacheLastModified > xmlLastModified) && (cacheLastModified > xslLastModified);
    }

    /**
     *  Serves the cached copy rather than re-performing the xsl transform for every request.
     *
     *  @param cacheFile The filename of the cached copy of the transform.
     *  @param out The writer to write to
     */
    protected void serveCachedCopy(File cacheFile, Writer out) {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(cacheFile));
            char[] cbuf = new char[8192];
            while (true) {
                int charsRead = in.read(cbuf);
                if (charsRead == -1) {
                    break;
                }
                out.write(cbuf, 0, charsRead);
            }
            in.close();
        } catch (IOException e) {
            err(e);
        }
    }

    /**
     *  Create a filename for the cached copy of this transform.  This filename will be the concatenation of the
     *  log file and the xsl file used to create it.
     *
     *  @param xmlFile The log file used as input to the transform
     *  @return The filename for the cached file
     */
    protected String getCachedCopyFileName(File xmlFile) {
        String xmlFileName = xmlFile.getName().substring(0, xmlFile.getName().lastIndexOf("."));
        String styleSheetName = xslFileName.substring(xslFileName.lastIndexOf("/") + 1, xslFileName.lastIndexOf("."));
        return xmlFileName + "-" + styleSheetName + ".html";
    }

    /**
     *  Gets the correct log file, based on the query string and the log directory.
     *
     *  @param logName The name of the log file.
     *  @param logDir The directory where the log files reside.
     *  @return The specifed log file or the latest log, if nothing is specified
     */
    protected File getXMLFile(String logName, File logDir) {
        File xmlFile = null;
        if (logName == null || logName.trim().equals("")) {
            xmlFile = getLatestLogFile(logDir);
            info("Using latest log file: " + xmlFile.getAbsolutePath());
        } else {
            xmlFile = new File(logDir, logName + ".xml");
            info("Using specified log file: " + xmlFile.getAbsolutePath());
        }
        return xmlFile;
    }

    /**
     *  Sets the xsl file to use. It is expected that this can be found by the <code>ServletContext</code> for this
     *  web application.
     *
     *  @param xslFile The path to the xslFile.
     */
    public void setXslFile(String xslFile) {
        xslFileName = xslFile;
    }

    /**
     * Set the dir to use. It is expected that this can be found by the <code>ServletContext</code> for
     * this web application.
     * <p>
     * This defaults to "/xsl".
     * <p>
     * Note that you only need to set this if you've used nested style sheets.
     * @param dir  the root directory.
     */
    public void setXslRootContext(String dir) {
        xslRootContext = dir;
    }

    /**
     *  Write the transformed log content to page writer given.
     */
    protected void writeContent(Writer out) throws JspException {
        File logDir = findLogDir();
        File xmlFile = findLogFile(logDir);
        File cacheFile = findCacheFile(logDir, xmlFile);
        if (!isCacheFileCurrent(xmlFile, cacheFile)) {
            updateCacheFile(xmlFile, cacheFile);
        } else {
            info("Using cached copy: " + cacheFile.getAbsolutePath());
        }
        serveCachedCopy(cacheFile, out);
    }

    private void updateCacheFile(File xmlFile, File cacheFile) {
        try {
            final InputStream styleSheetStream = getPageContext().getServletContext().getResourceAsStream(xslFileName);
            final FileWriter out = new FileWriter(cacheFile);
            transform(xmlFile, styleSheetStream, out);
            out.close();
            styleSheetStream.close();
        } catch (IOException e) {
            err(e);
        }
    }

    private File findCacheFile(File logDir, File xmlFile) {
        File cacheDir = new File(logDir, CACHE_DIR);
        if (!cacheDir.exists()) {
            cacheDir.mkdir();
        }
        File cacheFile = new File(cacheDir, getCachedCopyFileName(xmlFile));
        return cacheFile;
    }

    private File findLogFile(File logDir) {
        info("Scanning directory: " + logDir.getAbsolutePath() + " for log files.");
        String logFile = getPageContext().getRequest().getParameter("log");
        File xmlFile = getXMLFile(logFile, logDir);
        return xmlFile;
    }

    public int doEndTag() throws JspException {
        writeContent(getPageContext().getOut());
        return EVAL_PAGE;
    }
}