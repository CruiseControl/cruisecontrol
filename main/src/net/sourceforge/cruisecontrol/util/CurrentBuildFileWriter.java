/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.DateFormatFactory;

import org.apache.log4j.Logger;

/**
 * @author <a href="mailto:jcyip@thoughtworks.com">Jason Yip</a>
 * @version $Id$
 */
public final class CurrentBuildFileWriter {

    private static final Logger LOG = Logger.getLogger(CurrentBuildFileWriter.class);

    private CurrentBuildFileWriter() {
    }

    public static void writefile(String info, Date date, String fileName) throws CruiseControlException {
        DateFormat formatter = DateFormatFactory.getDateFormat();
        StringBuffer buffer = new StringBuffer(info).append(formatter.format(date));

        FileWriter writer = null;
        try {
            writer = new FileWriter(fileName);
            writer.write(buffer.toString());
        } catch (IOException e) {
            throw new CruiseControlException("Error writing file: " + fileName, e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    LOG.error("Failed closing writer", e);
                }
            }
        }
    }

    public static void validate(String fileName) throws CruiseControlException {
        File file = new File(fileName);
        File dir = file.getParentFile();
        if (dir != null && !dir.isDirectory()) {
            ValidationHelper.assertTrue(dir.mkdirs(), "directory for file " + fileName
                    + " doesn't exist and couldn't be created.");
        }
    }
}
