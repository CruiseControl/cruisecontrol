package net.sourceforge.cruisecontrol.taglib;

import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTag;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;

public class CurrentBuildStatusTag implements Tag, BodyTag {

    private BodyContent _bodyOut;
    private Tag _parent;
    private PageContext _pageContext;

    public int doAfterBody() throws JspException {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(_pageContext.getServletConfig().getInitParameter("currentBuildStatusFile")));
            String s = br.readLine();
            while(s != null) {
                _bodyOut.getEnclosingWriter().write(s);
                s = br.readLine();
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            br = null;
        }

        return SKIP_BODY;
    }

    public int doEndTag() throws JspException {
        return EVAL_PAGE;
    }

    public int doStartTag() throws JspException {
        return EVAL_BODY_TAG;
    }

    public Tag getParent() {
        return _parent;
    }

    public void release() {
    }

    public void setPageContext(PageContext pageContext) {
        _pageContext = pageContext;
    }

    public void setParent(Tag parent) {
        _parent = parent;
    }

    public void doInitBody() throws JspException {
    }

    public void setBodyContent(BodyContent bodyOut) {
        _bodyOut = bodyOut;
    }
}
