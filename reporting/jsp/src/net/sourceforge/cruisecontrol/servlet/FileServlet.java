/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2003, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 500
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
package net.sourceforge.cruisecontrol.servlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Writer;

public class FileServlet extends HttpServlet {

    private File rootDir;

    public void init(ServletConfig servletconfig) throws ServletException {
        super.init(servletconfig);
        String dir = servletconfig.getInitParameter("rootDir");
        if (dir == null) {
            throw new ServletException("Must specify rootDir paramter");
        }
        rootDir = new File(dir);
        if (!rootDir.exists() || rootDir.isFile()) {
            throw new ServletException(rootDir + " must be a valid directory");
        }
    }


    public void service(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {
        WebFile file = new WebFile(rootDir, request.getPathInfo());
        if (file.isFile()) {
            file.write(response.getOutputStream());
            return;
        }

        Writer writer = response.getWriter();
        writer.write("<html>");
        writer.write("<body>");
        writer.write("<h1>" + file + "</h1>");
        if (file.isDir()) {
            printDirs(request, file, writer);
        } else {
            writer.write("<h1>Invalid File or Directory</h1>");
        }
        writer.write("</body>");
        writer.write("</html>");
    }


    private void printDirs(HttpServletRequest request, WebFile file, Writer writer)
            throws IOException {
        String[] files = file.list();
        writer.write("<ul>");
        for (int i = 0; i < files.length; i++) {
            writer.write("<li><a href=" + request.getRequestURI() + "/" + files[i] + ">" + files[i] + "</a></li>");
        }
        writer.write("</ul>");
    }

}

class WebFile {

    private final File file;

    public WebFile(File root, String path) {
        file = WebFile.parsePath(root, path);
    }

    /**
     * @return
     */
    public boolean isDir() {
        return file.isDirectory();
    }

    public void write(ServletOutputStream stream) throws IOException {
        FileInputStream input = new FileInputStream(file);
        try {
            int i;
            while ((i = input.read()) != -1) {
                stream.write(i);
            }
        } finally {
            input.close();
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

    public String[] list() {
        String[] files = file.list() == null ? new String[0] : file.list();
        return files;
    }

    public String toString() {
        return file.toString();
    }

}
