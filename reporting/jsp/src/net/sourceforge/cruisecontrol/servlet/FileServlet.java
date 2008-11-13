/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2003, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.servlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.StringTokenizer;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class FileServlet extends HttpServlet {

    private File rootDir;
    private List<String> indexFiles;

    public File getRootDir() {
        return rootDir;
    }

    public void init(final ServletConfig servletconfig) throws ServletException {
        super.init(servletconfig);
        rootDir = getRootDir(servletconfig);
        indexFiles = getIndexFiles(servletconfig);
    }

    protected File getRootDir(final ServletConfig servletconfig) throws ServletException {
        final String root = servletconfig.getInitParameter("rootDir");
        File rootDirectory = getDirectoryFromName(root);
        if (rootDirectory == null) {
            rootDirectory = getLogDir(servletconfig);
            if (rootDirectory == null) {
                final String message = "ArtifactServlet not configured correctly in web.xml.\n"
                        + "Either rootDir or logDir must point to existing directory.\n"
                        + "rootDir is currently set to <" + root + "> "
                        + "while logDir is <" + getLogDirParameter(servletconfig) + ">";
                throw new ServletException(message);
            }
        }

        return rootDirectory;
    }

    protected String getLogDirParameter(final ServletConfig servletconfig) throws ServletException {
        final ServletContext context = servletconfig.getServletContext();
        return context.getInitParameter("logDir");
    }

    protected File getLogDir(final ServletConfig servletconfig) throws ServletException {
        final String logDir = getLogDirParameter(servletconfig);
        return getDirectoryFromName(logDir);
    }


    List<String> getIndexFiles(final ServletConfig servletconfig) {
        final ServletContext context = servletconfig.getServletContext();
        final String logDir = context.getInitParameter("fileServlet.welcomeFiles");
        List<String> indexes = Collections.emptyList();
        if (logDir != null) {
            final StringTokenizer tokenizer = new StringTokenizer(logDir);
            indexes = new ArrayList<String>();
            while (tokenizer.hasMoreTokens()) {
                final String indexFile = ((String) tokenizer.nextElement());
                // note: I am pretty sure there's a known issue with StringTokenizer returning "" (cf ant)
                // but am offline right now and cannot check.
                if (!"".equals(indexFile)) {
                    indexes.add(indexFile);
                }
            }
        }
        return indexes;
    }

    private static File getDirectoryFromName(final String dir) {
        if (dir == null) {
            return null;
        }

        final File rootDirectory = new File(dir);
        if (!rootDirectory.exists() || rootDirectory.isFile()) {
            return null;
        }
        return rootDirectory;
    }

    public void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        WebFile file = getSubWebFile(request.getPathInfo());

        if (file.isDir()) {
            // note we might want to append the queryString just in case...
            if (!request.getPathInfo().endsWith("/")) {
                response.sendRedirect(response.encodeRedirectURL(request.getRequestURI() + '/'));
                return;
            }
            final String index = getIndexFile(file);
            if (index != null) {
                file = getSubWebFile(request.getPathInfo() + index);
            }
        }

        if (file.isFile()) {
            final String filename = file.getName();
            final String mimeType;
            if (request.getParameter("mimetype") != null) {
                mimeType = request.getParameter("mimetype");
            } else {
                mimeType = getMimeType(filename);
            }
            final Date date = new Date(file.getFile().lastModified());
            response.addDateHeader("Last-Modified", date.getTime());
            response.setContentType(mimeType);
            response.setContentLength((int) file.getFile().length());
            file.write(response.getOutputStream());
            return;
        }

        response.setContentType("text/html");
        final Writer writer = response.getWriter();
        writer.write("<html>");
        writer.write("<body>");
        writer.write("<h1>" + file + "</h1>");
        if (file.isDir()) {
            printDirs(request, file, writer);
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            writer.write("<h1>Invalid File or Directory</h1>");
        }
        writer.write("</body>");
        writer.write("</html>");
    }

    protected String getMimeType(final String filename) {
        String mimeType = getServletContext().getMimeType(filename);
        if (mimeType == null) {
            mimeType = getDefaultMimeType();
        }
        return mimeType;
    }

    protected String getDefaultMimeType() {
        return "text/plain";
    }

    /**
     * @param dir the directory in which to search.
     * @return the name of the first found known index file under the
     *         specified directory or <code>null</code> if none found
     * @throws IllegalArgumentException if the specified WebFile is not a directory
     */
    private String getIndexFile(final WebFile dir) {
        if (!dir.isDir()) {
            throw new IllegalArgumentException(dir + " is not a directory");
        }
        for (final String indexFile : indexFiles) {
            final File file = new File(dir.getFile(), indexFile);
            // what about hidden files? let's display them...
            if (file.exists() && file.isFile()) {
                return indexFile;
            }
        }
        return null;
    }

    /**
     * Returns an HTML snippet that allows the browsing of the directory's content.
     *
     * @param request incoming http request
     * @param file directory to browse
     * @param writer place to write html output
     * @throws IOException if an error occurs
     */
    void printDirs(final HttpServletRequest request, final WebFile file, final Writer writer)
            throws IOException {
        final File[] files = file.list();
        writer.write("<table border='black' borderwidth='1' cellpadding='5'>");
        writer.write("<tr><th>name</th><th>file size</th><th>modified date</th></tr>");
        for (final File currentFile : files) {
            final String requestURI = request.getRequestURI();
            final int jsessionidIdx = requestURI.indexOf(";jsessionid");
            final String shortRequestURI;
            final String jsessionid;
            if (jsessionidIdx >= 0) {
                shortRequestURI = requestURI.substring(0, jsessionidIdx);
                jsessionid = requestURI.substring(jsessionidIdx);
            } else {
                shortRequestURI = requestURI;
                jsessionid = "";
            }

            final String subFilePath = request.getPathInfo() + '/' + currentFile.getName();
            final WebFile sub = getSubWebFile(subFilePath);
            writer.write("\n<tr><td>");
            writer.write(
                    "<a href=\""
                            + shortRequestURI
                            + (shortRequestURI.endsWith("/") ? "" : "/")
                            + currentFile.getName()
                            + jsessionid
                            + "\">"
                            + currentFile.getName()
                            + (sub.isDir() ? "/" : "")
                            + "</a>");
            writer.write("</td><td align='right'>");
            writer.write(formatFileSize(currentFile.length()));
            writer.write("</td><td align='right'>");
            writer.write(formatFileDate(currentFile.lastModified()));
            writer.write("</td></tr>");
        }
        writer.write("</table>");
    }

    private String formatFileDate(final long date) {
        return new Date(date).toString();
    }

    private static final BigDecimal TEN = BigDecimal.valueOf(10);
    private static final BigDecimal KB = BigDecimal.valueOf(1024);
    private static final BigDecimal MB = BigDecimal.valueOf(1024 * 1024);
    private static final BigDecimal GB = BigDecimal.valueOf(1024 * 1024 * 1024);

    private String formatFileSize(final long argL) {

        if (argL < 1024) {
            return String.valueOf(argL);
        }
        if (argL < 1024 * 1024) {
            return BigDecimal.valueOf(argL).multiply(TEN).divideToIntegralValue(KB).divide(TEN) + "K";
        }
        if (argL < 1024 * 1024 * 1024) {
            return BigDecimal.valueOf(argL).multiply(TEN).divideToIntegralValue(MB).divide(TEN) + "M";
        }
        return BigDecimal.valueOf(argL).multiply(TEN).divideToIntegralValue(GB).divide(TEN) + "G";
    }

    protected WebFile getSubWebFile(final String subFilePath) {
        return new WebFile(rootDir, subFilePath);
    }

}

class WebFile {

    private final File file;

    public WebFile(File logfile) {
        file = logfile;
    }

    public WebFile(File root, String path) {
        file = WebFile.parsePath(root, path);
    }

    public String getName() {
        return file.getName();
    }

    public boolean isDir() {
        return file.isDirectory();
    }

    protected InputStream getInputStream() throws IOException {
        return new FileInputStream(file);
    }

    public void write(ServletOutputStream stream) throws IOException {
        InputStream input = new BufferedInputStream(getInputStream());
        OutputStream output = new BufferedOutputStream(stream);
        try {
            int i;
            while ((i = input.read()) != -1) {
                output.write(i);
            }
        } finally {
            input.close();
            output.flush();
        }
    }

    public boolean isFile() {
        return file.isFile();
    }

    private static File parsePath(File rootDir, String string) {
        if (string == null || string.trim().length() == 0 || string.equals("/")) {
            return rootDir;
        }
        String filename = string.replace('/', File.separatorChar);
        filename = filename.replace('\\', File.separatorChar);
        return new File(rootDir, filename);
    }

    public File[] list() {
        File[] files = file.listFiles();
        if (files == null) {
            files = new File[0];
        } else {
            Arrays.sort(files, new FileNameComparator());
        }
        return files;
    }

    public String toString() {
        return file.toString();
    }

    public File getFile() {
        return file;
    }

    class FileNameComparator
            implements Comparator<File> {

        /* (non-Javadoc)
        * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
        */
        /**
         * {@inheritDoc}
         */
        public int compare(final File f1, final File f2) {
            return f1.getName().compareTo(f2.getName());
        }

    }
}
