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
package net.sourceforge.cruisecontrol.taglib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import net.sourceforge.cruisecontrol.LogFile;
import net.sourceforge.cruisecontrol.util.CCTagException;

/**
 *  JSP custom tag to handle xsl transforms.  This tag also caches the output of the transform to disk, reducing the
 *  number of transforms necessary.
 *
 *  @author alden almagro, ThoughtWorks, Inc. 2002
 *  @author <a href="mailto:hak@2mba.dk">Hack Kampbjorn</a>
 */
public class XSLTag extends CruiseControlTagSupport {
    private static final String XSLT_PARAMETER_PREFIX = "xslt.";
    private String xslFileName;
    private static final String CACHE_DIR = "_cache";

    public void release() {
        xslFileName = null;
    }


    /**
     *  Perform an xsl transform.  This body of this method is based upon the xalan sample code.
     *
     *  @param xmlFile the xml file to be transformed
     *  @param style resource containing the xsl stylesheet
     *  @param out stream to output the results of the transformation
     */
    protected void transform(LogFile xmlFile, URL style, OutputStream out) throws JspTagException {
        InputStream in = null;

        try {
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer(new StreamSource(style.toExternalForm()));
            Map parameters = getXSLTParameters();
            if (!parameters.isEmpty()) {
                transformer.clearParameters();
                for (Iterator i = parameters.entrySet().iterator(); i.hasNext(); ) {
                    Map.Entry entry = (Map.Entry) i.next();
                    transformer.setParameter((String) entry.getKey(), entry.getValue());
                }
            }

            try {
                in = xmlFile.getInputStream();
            } catch (IOException ioex) {
                err(ioex);
                throw new CCTagException("Cannot read logfile: "
                        + ioex.getMessage(), ioex);
            }
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
     *  Determine whether the cache file is current or not.  The file will be current if it is newer than both the
     *  xml log file and the xsl file used to create it.
     *
     *  @return true if the cache file is current.
     */
    protected boolean isCacheFileCurrent(File xmlFile, File cacheFile) {
        if (!cacheFile.exists() || cacheFile.length() == 0) {
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
     */
    protected void serveCachedCopy(File cacheFile, Writer out) throws JspTagException {
        try {
            InputStream input = new FileInputStream(cacheFile);
            copy(input, out);
        } catch (IOException e) {
            err(e);
            throw new CCTagException("Error reading file '"
                    + cacheFile.getName() + "': " + e.getMessage(), e);
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

        // The use of '/' is correct, xslFileName is a resource URL so it will
        // always start with a slash and only always use normal slashes
        int slashIndex = xslFileName.lastIndexOf("/");
        String styleSheetName = xslFileName.substring(slashIndex + 1, xslFileName.lastIndexOf("."));
        return xmlFileName + "-" + styleSheetName + ".html";
    }

    Map getXSLTParameters() {
        Map xsltParameters = new HashMap();
        ServletConfig config = pageContext.getServletConfig();
        Enumeration names = config.getInitParameterNames();
        while (names.hasMoreElements()) {
            String parameterName = (String) names.nextElement();
            if (parameterName.startsWith(XSLT_PARAMETER_PREFIX)) {
                String value = config.getInitParameter(parameterName);
                String name = parameterName.substring(XSLT_PARAMETER_PREFIX.length());
                info("using XSLT parameter: " + name + "=" + value);
                xsltParameters.put(name, value);
            }
        }
        ServletContext context = config.getServletContext();
        names = context.getInitParameterNames();
        while (names.hasMoreElements()) {
            String parameterName = (String) names.nextElement();
            if (parameterName.startsWith(XSLT_PARAMETER_PREFIX)) {
                String value = context.getInitParameter(parameterName);
                String name = parameterName.substring(XSLT_PARAMETER_PREFIX.length());
                info("using XSLT parameter: " + name + "=" + value);
                xsltParameters.put(name, value);
            }
        }

        return xsltParameters;
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
     * Prepare the content if there's need to.
     * The content must be prepared if a transformation is required.
     *
     * @return the file to serve
     */
    File prepareContent() throws JspException {
        LogFile xmlFile = findLogFile();
        File cacheFile = findCacheFile(xmlFile);
        if (!isCacheFileCurrent(xmlFile.getFile(), cacheFile)) {
            info("Updating cached copy: " + cacheFile.getAbsolutePath());
            updateCacheFile(xmlFile, cacheFile);
        } else {
            info("Using cached copy: " + cacheFile.getAbsolutePath());
        }
        return cacheFile;
    }

    protected void updateCacheFile(LogFile xmlFile, File cacheFile) throws JspTagException {
        OutputStream out = null;
        try {
            out = new FileOutputStream(cacheFile);
            URL style = getPageContext().getServletContext().getResource(xslFileName);
            transform(xmlFile, style, out);
        } catch (IOException e) {
            err(e);
            throw new CCTagException("Error saving a cached transformation '"
                    + cacheFile.getName() + "': " + e.getMessage(), e);
        } finally {
            closeQuietly(out);
        }
    }

    private File findCacheFile(LogFile xmlFile) {
        String cacheRoot = getContextParam("cacheRoot");
        File cacheDir = cacheRoot == null
            ? new File(xmlFile.getLogDirectory(), CACHE_DIR)
            : new File(cacheRoot + File.separator + getProject());
        if (!cacheDir.exists()) {
            cacheDir.mkdir();
        }
        File cacheFile = new File(cacheDir, getCachedCopyFileName(xmlFile.getFile()));
        return cacheFile;
    }

    public int doEndTag() throws JspException {
        File cachedFile = prepareContent();
        serveCachedCopy(cachedFile, getPageContext().getOut());
        return EVAL_PAGE;
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
}
