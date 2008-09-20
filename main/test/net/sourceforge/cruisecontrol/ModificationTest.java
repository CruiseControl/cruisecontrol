/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.util.DateUtil;

import org.jdom.Element;
import org.jdom.output.XMLOutputter;

public class ModificationTest extends TestCase {

    public void testToElement() throws Exception {
        Date modifiedTime = new Date();
        Modification mod = new Modification();

        Modification.ModifiedFile modfile = mod.createModifiedFile("File\"Name&", "Folder'Name");
        modfile.action = "checkin";

        mod.modifiedTime = modifiedTime;
        mod.userName = "User<>Name";
        mod.comment = "Comment";

        String base =
            "<modification type=\"unknown\">"
                + "<file action=\"checkin\"><filename>File\"Name&amp;</filename>"
                + "<project>Folder'Name</project></file>"
                + "<date>" + DateUtil.formatIso8601(modifiedTime) + "</date>"
                + "<user>User&lt;&gt;Name</user>"
                + "<comment><![CDATA[Comment]]></comment>";
        String closingTag = "</modification>";
        String expected = base + closingTag;

        assertEquals(expected, xmlStringFromElement(mod.toElement()));

        String expectedWithEmail =
            base + "<email>foo.bar@quuuux.quuux.quux.qux</email>" + closingTag;
        mod.emailAddress = "foo.bar@quuuux.quuux.quux.qux";

        assertEquals(expectedWithEmail, xmlStringFromElement(mod.toElement()));
    }

    public void testBadComment() throws IOException {
        Date modifiedTime = new Date();
        Modification mod = new Modification();

        Modification.ModifiedFile modfile = mod.createModifiedFile("File\"Name&", "Folder'Name");
        modfile.action = "checkin";

        mod.modifiedTime = modifiedTime;
        mod.userName = "User<>Name";
        mod.comment = "Attempting to heal the wounded build.\0x18";

        String base =
            "<modification type=\"unknown\">"
            + "<file action=\"checkin\"><filename>File\"Name&amp;</filename>"
            + "<project>Folder'Name</project></file>"
            + "<date>" + DateUtil.formatIso8601(modifiedTime) + "</date>"
            + "<user>User&lt;&gt;Name</user>"
            + "<comment><![CDATA[Unable to parse comment.  It contains illegal data.]]></comment>";
        String closingTag = "</modification>";
        String expected = base + closingTag;

        assertEquals(expected, xmlStringFromElement(mod.toElement()));
    }

    public void testToElementAndBack() throws Exception {
        Date modifiedTime = new Date();
        Modification mod = new Modification();

        Modification.ModifiedFile modfile = mod.createModifiedFile("File\"Name&", "Folder'Name");
        modfile.action = "checkin";

        mod.modifiedTime = modifiedTime;
        mod.userName = "User<>Name";
        mod.comment = "Attempting to heal the wounded build.\0x18";

        Modification modification = new Modification();
        modification.fromElement(mod.toElement());
        mod.equals(modification);

        // Test getFullPath() of Modificaiton object.
        assertEquals("Folder'Name/File\"Name&", modification.getFullPath());
    }


    public void testGettingModifiedFiles() {
        Modification mod = new Modification();

        // Test getting when no files are present
        assertNull(mod.getFileName());
        assertNull(mod.getFolderName());
        assertNotNull(mod.getModifiedFiles());
        assertEquals(0, mod.getModifiedFiles().size());
        
        // Add first file
        final String filename1 = "filename-1";
        final String folder1 = "folder-1";
        Modification.ModifiedFile file1 = mod.createModifiedFile(filename1, folder1);
        
        assertEquals(filename1, file1.fileName);
        assertEquals(folder1, file1.folderName);
        
        assertEquals(filename1, mod.getFileName());
        assertEquals(folder1, mod.getFolderName());
        
        List modList = mod.getModifiedFiles();
        assertEquals(1, modList.size());
        assertSame(file1, modList.get(0));
        
        // Add second file
        final String filename2 = "filename-2";
        final String folder2 = "folder-2";
        Modification.ModifiedFile file2 = mod.createModifiedFile(filename2, folder2);
        
        assertEquals(filename2, file2.fileName);
        assertEquals(folder2, file2.folderName);
        
        assertEquals(filename1, mod.getFileName());
        assertEquals(folder1, mod.getFolderName());
        
        modList = mod.getModifiedFiles();
        assertEquals(2, modList.size());
        assertSame(file1, modList.get(0));
        assertSame(file2, modList.get(1));
        
        // Add third file
        final String filename3 = "filename-3";
        final String folder3 = "folder-3";
        Modification.ModifiedFile file3 = mod.createModifiedFile(filename3, folder3);
        
        assertEquals(filename3, file3.fileName);
        assertEquals(folder3, file3.folderName);
        
        assertEquals(filename1, mod.getFileName());
        assertEquals(folder1, mod.getFolderName());
        
        modList = mod.getModifiedFiles();
        assertEquals(3, modList.size());
        assertSame(file1, modList.get(0));
        assertSame(file2, modList.get(1));
        assertSame(file3, modList.get(2));
    }
    
    private String xmlStringFromElement(Element element) throws IOException {
        XMLOutputter outputter = new XMLOutputter();
        return outputter.outputString(element);
    }
    
}
