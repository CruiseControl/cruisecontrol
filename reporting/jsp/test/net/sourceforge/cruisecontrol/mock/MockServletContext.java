////////////////////////////////////////////////////////////////////////////////
//
// Copyright (c) 2002, Suncorp Metway Limited. All rights reserved.
//
// This is unpublished proprietary source code of Suncorp Metway Limited.
// The copyright notice above does not evidence any actual or intended
// publication of such source code.
//
////////////////////////////////////////////////////////////////////////////////
package net.sourceforge.cruisecontrol.mock;

import java.net.URL;
import java.net.MalformedURLException;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Enumeration;
import javax.servlet.ServletContext;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

/**
 *
 * @author <a href="mailto:robert.watkins@suncorp.com.au">Robert Watkins</a>
 */
public class MockServletContext implements ServletContext {
    private File baseResourceDir;

    public MockServletContext() {
    }

    public ServletContext getContext(String s) {
        return null;
    }

    public int getMajorVersion() {
        return 0;
    }

    public int getMinorVersion() {
        return 0;
    }

    public String getMimeType(String s) {
        return null;
    }

    public URL getResource(String s) throws MalformedURLException {
        return null;
    }

    public InputStream getResourceAsStream(String resourceName) {
        try {
            return new FileInputStream(new File(baseResourceDir, resourceName));
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    public RequestDispatcher getRequestDispatcher(String s) {
        return null;
    }

    public RequestDispatcher getNamedDispatcher(String s) {
        return null;
    }

    public Servlet getServlet(String s) throws ServletException {
        return null;
    }

    public Enumeration getServlets() {
        return null;
    }

    public Enumeration getServletNames() {
        return null;
    }

    public void log(String s) {
    }

    public void log(Exception e, String s) {
    }

    public void log(String s, Throwable throwable) {
    }

    public String getRealPath(String s) {
        return null;
    }

    public String getServerInfo() {
        return null;
    }

    public String getInitParameter(String s) {
        return null;
    }

    public Enumeration getInitParameterNames() {
        return null;
    }

    public Object getAttribute(String s) {
        return null;
    }

    public Enumeration getAttributeNames() {
        return null;
    }

    public void setAttribute(String s, Object o) {
    }

    public void removeAttribute(String s) {
    }

    /**
     * Set the base resource dir, for use with getResource and getResourceAsStream.
     * @param dir   the base directory
     */
    public void setBaseResourceDir(File dir) {
        baseResourceDir = dir;
    }
}
