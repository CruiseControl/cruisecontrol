/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2003, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 600
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

package net.sourceforge.cruisecontrol.taglib;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import java.io.Writer;
import javax.servlet.jsp.JspException;

public class CurrentBuildStatusTag extends CruiseControlTagSupport {

    public int doEndTag() throws JspException {
        File logDir = findLogDir();

        String currentBuildFileName = getFileName();
        if (currentBuildFileName != null) {
            File currentBuildFile = getFile(logDir, currentBuildFileName);
            if (currentBuildFile != null) {
                writeStatus(currentBuildFile, getPageContext().getOut());
            }
        }

        return EVAL_PAGE;
    }

    private void writeStatus(File currentBuildFile, Writer out) throws JspException {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(currentBuildFile));
            String line = br.readLine();
            while (line != null) {
                out.write(line);
                out.write('\n');
                line = br.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new JspException(
                "Error reading status file: " + currentBuildFile.getName() + " : " + e.getMessage());
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            br = null;
        }
    }

    private File getFile(File logDir, String currentBuildFileName) {
        File currentBuildFile = new File(logDir, currentBuildFileName);
        if (currentBuildFile.isDirectory()) {
            System.err.println(
                "CruiseControl: currentBuildStatusFile "
                    + currentBuildFile.getAbsolutePath()
                    + " is a directory." 
                    + " Edit the web.xml to provide the path to the correct file.");
            return null;
        }
        if (!currentBuildFile.exists()) {
            System.err.println(
                "CruiseControl: currentBuildStatusFile "
                    + currentBuildFile.getAbsolutePath()
                    + " does not exist."
                    + " You may need to update the value in the web.xml"
                    + " or the location specified in your CruiseControl config.xml.");
            return null;
        }
        return currentBuildFile;
    }

    private String getFileName() {
        String currentBuildFileName = getContextParam("currentBuildStatusFile");
        if (currentBuildFileName == null || currentBuildFileName.equals("")) {
            System.err.println("CruiseControl: currentBuildStatusFile not defined in the web.xml");
            return null;
        }
        return currentBuildFileName;
    }
}
