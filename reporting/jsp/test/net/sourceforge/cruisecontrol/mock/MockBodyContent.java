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

import java.io.Reader;
import java.io.Writer;
import java.io.IOException;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.JspWriter;

/**
 *
 * @author <a href="mailto:robert.watkins@suncorp.com.au">Robert Watkins</a>
 */
public class MockBodyContent extends BodyContent {

    public MockBodyContent() {
        super(null);
    }

    protected MockBodyContent(JspWriter jspWriter) {
        super(jspWriter);
    }

    public Reader getReader() {
        return null;
    }

    public String getString() {
        return null;
    }

    public void writeOut(Writer writer) throws IOException {
    }

    public void newLine() throws IOException {
    }

    public void print(boolean b) throws IOException {
    }

    public void print(char c) throws IOException {
    }

    public void print(int i) throws IOException {
    }

    public void print(long l) throws IOException {
    }

    public void print(float v) throws IOException {
    }

    public void print(double v) throws IOException {
    }

    public void print(char[] chars) throws IOException {
    }

    public void print(String s) throws IOException {
    }

    public void print(Object o) throws IOException {
    }

    public void println() throws IOException {
    }

    public void println(boolean b) throws IOException {
    }

    public void println(char c) throws IOException {
    }

    public void println(int i) throws IOException {
    }

    public void println(long l) throws IOException {
    }

    public void println(float v) throws IOException {
    }

    public void println(double v) throws IOException {
    }

    public void println(char[] chars) throws IOException {
    }

    public void println(String s) throws IOException {
    }

    public void println(Object o) throws IOException {
    }

    public void clear() throws IOException {
    }

    public void clearBuffer() throws IOException {
    }

    public void close() throws IOException {
    }

    public int getRemaining() {
        return 0;
    }

    public void write(char cbuf[], int off, int len) throws IOException {
    }
}
