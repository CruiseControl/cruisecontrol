/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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

package net.sourceforge.cruisecontrol;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LogMerger {

    public List _logs = new ArrayList();

    public void addLog(String logFileName) {
        _logs.add(new File(logFileName));
    }

    public List getLogs() {
        List logElements = new ArrayList();
        Iterator logIterator = _logs.iterator();
        while (logIterator.hasNext()) {
            File log = (File) logIterator.next();
            if (log.isDirectory()) {
                File[] childLogs = log.listFiles();
                for (int i = 0; i < childLogs.length; i++) {
                    Element logFileElement = getFileAsElement(log);
                    if (logFileElement != null)
                        logElements.add(logFileElement);
                }
            } else {
                Element logFileElement = getFileAsElement(log);
                if (logFileElement != null)
                    logElements.add(getFileAsElement(log));
            }
        }
        return logElements;
    }

    protected Element getFileAsElement(File f) {
        SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
        try {
            return builder.build(f).getRootElement();
        } catch (JDOMException je) {
            je.printStackTrace();
        }
        return null;
    }
}
