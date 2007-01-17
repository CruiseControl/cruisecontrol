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
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import javax.servlet.jsp.tagext.BodyContent;

/**
 *
 * @author <a href="mailto:robertdw@sourceforge.net">Robert Watkins</a>
 */
public class MockBodyContent extends BodyContent {
    private final StringWriter writer = new StringWriter();
    private final PrintWriter printer = new PrintWriter(writer);

    public MockBodyContent() {
        super(null);
    }

    public void close() {
        printer.close();
    }

    public void write(int c) {
        printer.write(c);
    }

    public void write(char[] buf, int off, int len) {
        printer.write(buf, off, len);
    }

    public void write(char[] buf) {
        printer.write(buf);
    }

    public void write(String s, int off, int len) {
        printer.write(s, off, len);
    }

    public void write(String s) {
        printer.write(s);
    }

    public void print(boolean b) {
        printer.print(b);
    }

    public void print(char c) {
        printer.print(c);
    }

    public void print(int i) {
        printer.print(i);
    }

    public void print(long l) {
        printer.print(l);
    }

    public void print(float f) {
        printer.print(f);
    }

    public void print(double d) {
        printer.print(d);
    }

    public void print(char[] s) {
        printer.print(s);
    }

    public void print(String s) {
        printer.print(s);
    }

    public void print(Object obj) {
        printer.print(obj);
    }

    public void println() {
        printer.println();
    }

    public void println(boolean x) {
        printer.println(x);
    }

    public void println(char x) {
        printer.println(x);
    }

    public void println(int x) {
        printer.println(x);
    }

    public void println(long x) {
        printer.println(x);
    }

    public void println(float x) {
        printer.println(x);
    }

    public void println(double x) {
        printer.println(x);
    }

    public void println(char[] x) {
        printer.println(x);
    }

    public void println(String x) {
        printer.println(x);
    }

    public void println(Object x) {
        printer.println(x);
    }

    public Reader getReader() {
        return new StringReader(getString());
    }

    public String getString() {
        printer.flush();
        writer.flush();
        final String body = writer.toString();
        return body;
    }

    public void writeOut(Writer destWriter) throws IOException {
        if (destWriter != null) {
            destWriter.write(getString());
        }
    }

    public void newLine() throws IOException {
        println();
    }

    public void clear() throws IOException {
    }

    public void clearBuffer() throws IOException {
    }

    public int getRemaining() {
        return 0;
    }

    public String toString() {
        return getString();
    }

}
