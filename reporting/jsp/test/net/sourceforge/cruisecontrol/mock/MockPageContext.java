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

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.JspWriter;
import javax.servlet.Servlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

/**
 *
 * @author <a href="mailto:robert.watkins@suncorp.com.au">Robert Watkins</a>
 */
public class MockPageContext extends PageContext {
    private MockServletRequest request = new MockServletRequest();

    private HashMap[] scopes = { new HashMap(), new HashMap(), new HashMap(), new HashMap() };
    public void initialize(Servlet servlet, ServletRequest servletRequest, ServletResponse servletResponse,
                           String errorPageURL, boolean needsSession, int bufferSize, boolean autoFlush)
            throws IOException, IllegalStateException, IllegalArgumentException {
    }

    public void release() {
    }

    public void setAttribute(String name, Object attribute) {
        setAttribute(name, attribute, PageContext.PAGE_SCOPE);
    }

    public void setAttribute(String name, Object attribute, int scope) {
        scopeToMap(scope).put(name, attribute);
    }

    private HashMap scopeToMap(int scope) {
        return scopes[scope - 1];
    }

    public Object getAttribute(String name) {
        return getAttribute(name, PageContext.PAGE_SCOPE);
    }

    public Object getAttribute(String name, int scope) {
        return scopeToMap(scope).get(name);
    }

    public Object findAttribute(String name) {
        return null;
    }

    public void removeAttribute(String name) {
        removeAttribute(name, PageContext.PAGE_SCOPE);
    }

    public void removeAttribute(String name, int scope) {
    }

    public int getAttributesScope(String name) {
        return 0;
    }

    public Enumeration getAttributeNamesInScope(int scope) {
        return null;
    }

    public JspWriter getOut() {
        return null;
    }

    public HttpSession getSession() {
        return null;
    }

    public Object getPage() {
        return null;
    }

    public ServletRequest getRequest() {
        return request;
    }

    public ServletResponse getResponse() {
        return null;
    }

    public Exception getException() {
        return null;
    }

    public ServletConfig getServletConfig() {
        return null;
    }

    public ServletContext getServletContext() {
        return null;
    }

    public void forward(String relativeUrlPath) throws ServletException, IOException {
    }

    public void include(String relativeUrlPath) throws ServletException, IOException {
    }

    public void handlePageException(Exception e) throws ServletException, IOException {
    }
}
