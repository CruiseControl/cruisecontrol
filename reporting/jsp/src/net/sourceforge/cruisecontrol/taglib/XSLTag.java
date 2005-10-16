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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;

import javax.servlet.ServletContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import net.sourceforge.cruisecontrol.util.CCTagException;

/**
 *  JSP custom tag to handle xsl transforms.  This tag also caches the output of the transform to disk, reducing the
 *  number of transforms necessary.
 *
 * When no xsl file is defined, it just serves the file as is (possibly unzipped).
 *
 *  @author alden almagro, ThoughtWorks, Inc. 2002
 *  @author <a href="mailto:hak@2mba.dk">Hack Kampbjorn</a>
 */
public class XSLTag extends CruiseControlTagSupport {
    private static final String DEFAULT_XSL_ROOT = "/xsl/";
    private String xslFileName;
    private File toServe;
    private String url;
    private String xslRootContext = DEFAULT_XSL_ROOT;
    private boolean serveContent = true;
    private static final String CACHE_DIR = "_cache";

    public int doStartTag() throws JspException {
        toServe = prepareContent();
        if (url != null) {
            String urlPath = toServe.getPath();
            String baseLogDir = getBaseLogDir();
            String urlValue = urlPath;

            // if logDir was specified with an absolute path, find the relative one.
            // also makes sure we support relative directory names non equal 
            // to the uri prefix used by cruisecontrol ("logs")
            if (urlValue.startsWith(baseLogDir)) {
                urlValue = urlValue.substring(baseLogDir.length());
            }

            urlValue = urlValue.replace('\\', '/');
            if (urlValue.startsWith("/")) {
                urlValue = urlValue.substring(1);
            }

            // add the uri prefix, which must be so that the url will be handled by
            // the log file servlet (Cf. log file servlet servletMapping in web.xml)
            urlValue = "logs/" + urlValue;
            info("log file path is: " + urlPath);
            info("logURL is var: " + url + " value: " + urlValue);
            getPageContext().setAttribute(url, urlValue);
        }
        return EVAL_BODY_INCLUDE; //SKIP_BODY;
    }

    /**
     *  Perform an xsl transform.  This body of this method is based upon the xalan sample code.
     *
     *  @param xmlFile the xml file to be transformed
     *  @param style stream containing the xsl stylesheet. Null means no style applied
     *  @param out stream to output the results of the transformation
     */
    protected void transform(File xmlFile, InputStream style, OutputStream out) throws JspTagException {
        InputStream in = null;

        try {
            TransformerFactory tFactory = TransformerFactory.newInstance();
            javax.xml.transform.URIResolver resolver = new javax.xml.transform.URIResolver() {
                private final ServletContext servletContext = getPageContext().getServletContext();
                public javax.xml.transform.Source resolve(String href, String base) {
                    InputStream styleStream = servletContext.getResourceAsStream(xslRootContext + href);
                    if (styleStream != null) {

                        // XXX Possible stream leak: there is no explicit
                        //     styleStream.close()
                        info("Using nested stylesheet for " + href);
                        return new StreamSource(styleStream);
                    } else {
                        info("Nested stylesheet not found for " + href);
                        return null;
                    }
                }
            };
            tFactory.setURIResolver(resolver);
            Transformer transformer = tFactory.newTransformer(new StreamSource(style));
            in = getXmlFileInputStream(xmlFile);
            transformer.transform(new StreamSource(in), new StreamResult(out));
        } catch (ArrayIndexOutOfBoundsException e) {
            err(e);
            throw new CCTagException("Error transforming '" + xmlFile.getName()
                    + "'. You might be experiencing XML parser issues."
                    + " Are your xalan & xerces jar files mismatched? Check your JVM version. "
                    + e.getMessage(), e);
        } catch (TransformerException e) {
            err(e);
            throw new CCTagException("Error transforming '" + xmlFile.getName()
                    + "': " + e.getMessage(), e);
        } finally {
            closeQuietly(in);
        }
    }

    /**
     *  Unzip
     *
     *  @param xmlFile the xml file to be transformed
     *  @param os stream to output the results of the transformation
     */
    protected void cache(File xmlFile, OutputStream os) throws JspTagException, IOException {
        info("Caching file: " + xmlFile.getAbsolutePath());
        final OutputStreamWriter out = new OutputStreamWriter(os);
        copy(getXmlFileInputStream(xmlFile), out);
        closeQuietly(out);
    }

    private InputStream getXmlFileInputStream(File xmlFile) throws CCTagException {
        InputStream in = null;
        if (xmlFile.getName().endsWith(".gz")) {
            try {
                in = new GZIPInputStream(new FileInputStream(xmlFile));
            } catch (IOException ioe) {
                err(ioe);
            }
        } else {
            try {
                in = new FileInputStream(xmlFile);
            } catch (IOException ioex) {
                err(ioex);
                throw new CCTagException("Assertion error: "
                        + ioex.getMessage(), ioex);
            }
        }
        return in;
    }

    public void setUrl(String url) {
        this.url = url;
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
        boolean isCurrent = false;
        long xmlLastModified = xmlFile.lastModified();
        long cacheLastModified = cacheFile.lastModified();
        try {
            URL xslUrl = getPageContext().getServletContext().getResource(xslFileName);
            URLConnection con = xslUrl.openConnection();
            long xslLastModified = con.getLastModified();
            isCurrent = (cacheLastModified > xmlLastModified) && (cacheLastModified > xslLastModified);
        } catch (Exception e) {
            err("Failed to retrieve lastModified of xsl file " + xslFileName);
        }
        return isCurrent;
    }

    /**
     *  Serves the cached copy rather than re-performing the xsl transform for every request.
     *
     *  @param cacheFile The filename of the cached copy of the transform.
     *  @param out The writer to write to
     * @deprecated
     */
    protected void serveCachedCopy(File cacheFile, Writer out) throws JspTagException {
        serveFile(cacheFile, out);
    }

    /**
     *  Serves the file.
     *
     *  @param file The filename of the cached copy of the transform.
     *  @param out The writer to write to
     */
    protected void serveFile(File file, Writer out) throws JspTagException {
        try {
            InputStream input = new FileInputStream(file);
            copy(input, out);
        } catch (IOException e) {
            err(e);
            throw new CCTagException("Error reading file '"
                    + file.getName() + "': " + e.getMessage(), e);
        }
    }

    private void copy(InputStream input, Writer out) throws IOException {
        BufferedReader in;
        in = new BufferedReader(new InputStreamReader(input, "UTF-8"));

        try {
            char[] cbuf = new char[8192];
            while (true) {
                int charsRead = in.read(cbuf);
                if (charsRead == -1) {
                    break;
                }
                out.write(cbuf, 0, charsRead);
            }
        } finally {
            closeQuietly(in);
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
        return shouldBeTransformed()
            ? xmlFileName + "-"
              + xslFileName.substring(xslFileName.lastIndexOf("/") + 1, xslFileName.lastIndexOf("."))
              + ".html"
            : xmlFileName;
    }

    /**
     *  Gets the correct log file, based on the query string and the log directory.
     *
     *  @param logName The name of the log file.
     *  @param logDir The directory where the log files reside.
     *  @return The specifed log file or the latest log, if nothing is specified
     */
    protected File getXMLFile(String logName, File logDir) {
        File xmlFile;
        if (logName == null || logName.trim().equals("")) {
            xmlFile = getLatestLogFile(logDir);
            info("Using latest log file: " + xmlFile.getAbsolutePath());
        } else {
            xmlFile = new File(logDir, logName + ".xml");
            if (!xmlFile.exists()) {
                xmlFile = new File(logDir, logName + ".xml.gz");
            }
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
     * Prepare the content if there's need to.
     * THe content must be prepared if it is zipped or/and if a transformation is required.
     *
     * @return the file served/to serve
     */
    File prepareContent() throws JspException {
        File logDir = findLogDir();
        File xmlFile = findLogFile(logDir);
        File fileToServe;
        if (isCacheRequired(xmlFile)) {
            File cacheFile = findCacheFile(logDir, xmlFile);
            if (!isCacheFileCurrent(xmlFile, cacheFile)) {
                info("Updating cached copy: " + cacheFile.getAbsolutePath());
                updateCacheFile(xmlFile, cacheFile);
            } else {
                info("Using cached copy: " + cacheFile.getAbsolutePath());
            }
            fileToServe = cacheFile;
        } else {
            fileToServe = xmlFile;
        }
        return fileToServe;
    }

    private boolean isCacheRequired(File file) {
        return shouldBeTransformed() || file.getName().endsWith(".gz");
    }

    protected void updateCacheFile(File xmlFile, File cacheFile) throws JspTagException {
        OutputStream out = null;
        try {
            out = new FileOutputStream(cacheFile);
            if (shouldBeTransformed()) {
                InputStream style = null;
                try {
                    style = getPageContext().getServletContext().getResourceAsStream(xslFileName);
                    transform(xmlFile, style, out);
                } finally {
                    closeQuietly(style);
                }
            } else {
                cache(xmlFile, out);
            }
        } catch (IOException e) {
            err(e);
            throw new CCTagException("Error saving a cached transformation '"
                    + cacheFile.getName() + "': " + e.getMessage(), e);
        } finally {
            closeQuietly(out);
        }
    }

    private boolean shouldBeTransformed() {
        return xslFileName != null;
    }

    private File findCacheFile(File logDir, File xmlFile) {
        String cacheRoot = getContextParam("cacheRoot");
        File cacheDir = cacheRoot == null 
            ? new File(logDir, CACHE_DIR)
            : new File(cacheRoot + File.separator + getProject());
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
        if (serveContent) {
          serveFile(toServe, getPageContext().getOut());
        }
        return EVAL_PAGE;
    }

    public void setServeContent(boolean serveContent) {
        this.serveContent = serveContent;
    }

    private void closeQuietly(InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException ioex) {
                info("Ignored " + ioex.getMessage() + " while closing stream");
            }
        }
    }
    private void closeQuietly(Reader in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException ioex) {
                info("Ignored " + ioex.getMessage() + " while closing reader");
            }
        }
    }
    private void closeQuietly(OutputStream out) {
        if (out != null) {
            try {
                out.close();
            } catch (IOException ioex) {
                info("Ignored " + ioex.getMessage() + " while closing stream");
            }
        }
    }

    private void closeQuietly(Writer out) {
        if (out != null) {
            try {
                out.close();
            } catch (IOException ioex) {
                info("Ignored " + ioex.getMessage() + " while closing writer");
            }
        }
    }
}
