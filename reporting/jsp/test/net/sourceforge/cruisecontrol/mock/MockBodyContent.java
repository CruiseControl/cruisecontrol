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
package net.sourceforge.cruisecontrol.mock;

import java.io.Reader;
import java.io.Writer;
import java.io.IOException;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.JspWriter;

/**
 *
 * @author <a href="mailto:robertdw@sourceforge.net">Robert Watkins</a>
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
