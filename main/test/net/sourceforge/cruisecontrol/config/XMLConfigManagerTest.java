/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2006, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.config;

import java.io.File;

import org.jdom.Element;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.IO;

public class XMLConfigManagerTest extends TestCase {
    private File configurationFile;

    protected void setUp() throws Exception {
        configurationFile = File.createTempFile("config", "xml");
        configurationFile.deleteOnExit();
        writeConfigurationFile(
                "<cruisecontrol><project name=\"DOESNTMATTER\"><schedule>"
                        + "<ant/></schedule></project></cruisecontrol>\n");
    }

    public void testShouldStoreUpdatedMD5HashWhenItChanges() throws Exception {
        XMLConfigManager configManager = new XMLConfigManager(configurationFile);

        assertFalse(configManager.reloadIfNecessary());

        writeConfigurationFile(
                "<cruisecontrol><project name=\"DOESNTMATTER\"><schedule interval=\"30\">"
                        + "<ant/></schedule></project></cruisecontrol>");
        assertTrue(configManager.reloadIfNecessary());

        assertFalse(configManager.reloadIfNecessary());
    }

    public void testResolverShouldReturnCorrectElement() throws Exception {
        XMLConfigManager configManager = new XMLConfigManager(configurationFile);
        File file = File.createTempFile("XmlConfigManagerTest", ".xml", configurationFile.getParentFile());
        file.deleteOnExit();
        IO.write(file, "<foo><bar/></foo>");
        XmlResolver resolver = configManager.new Resolver();
        Element element = resolver.getElement(file.getName());
        assertEquals("foo", element.getName());
    }

    private void writeConfigurationFile(String contents) throws CruiseControlException {
        StringBuffer configuration = new StringBuffer();
        configuration.append(contents);
        IO.write(configurationFile, configuration.toString());
    }
}
