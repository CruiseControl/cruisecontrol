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
package net.sourceforge.cruisecontrol.mock;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;

import javax.el.ELContext;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.el.ExpressionEvaluator;
import javax.servlet.jsp.el.VariableResolver;

/**
 *
 * @author <a href="mailto:robertdw@users.sourceforge.net">Robert Watkins</a>
 */
public class MockPageContext extends PageContext {
    private MockServletRequest request = new MockServletRequest();
    private JspWriter out = new MockBodyContent();

    private HashMap[] scopes = { new HashMap(), new HashMap(), new HashMap(), new HashMap() };
    private MockServletContext servletContext = new MockServletContext();
    private MockServletConfig servletConfig = new MockServletConfig();

    public MockPageContext() {
        servletConfig.setServletContext(servletContext);
    }

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
        return out;
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
        return servletConfig;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    public void forward(String relativeUrlPath) throws ServletException, IOException {
    }

    public void include(String relativeUrlPath) throws ServletException, IOException {
    }

    public void handlePageException(Exception e) throws ServletException, IOException {
    }

    public void setHttpServletRequest(MockServletRequest mockRequest) {
        request = mockRequest;
    }

    public void handlePageException(Throwable arg0) throws ServletException, IOException {
    }

    public void include(String arg0, boolean arg1) throws ServletException, IOException {
    }

    public ELContext getELContext() {
        return null;
    }

    public ExpressionEvaluator getExpressionEvaluator() {
        return null;
    }

    public VariableResolver getVariableResolver() {
        return null;
    }
    
}
