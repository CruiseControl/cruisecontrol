/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2007, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.dashboard.web.command;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.dashboard.StoryTracker;
import org.apache.commons.lang.StringEscapeUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ModificationCommandTest extends TestCase {
    private Date date;

    protected void setUp() throws Exception {
        date = new Date();
    }

    public void testShouldreturnHyperLinkWhenCommandIsStoryTrackerSensitive() {
        StoryTracker storyTracker = new StoryTracker("pj", "http://mingle/story/", "story");
        ModificationCommand command = new ModificationCommand(createModification("story1"), storyTracker);
        assertEquals("<a href=\"http://mingle/story/1\">story1</a>", command.getComment());
    }

    private Modification createModification(String comment) {
        List files = new ArrayList();
        Modification.ModifiedFile file1 = new Modification.ModifiedFile("file1.txt", "123", "folder", "deleted");
        Modification.ModifiedFile file2 = new Modification.ModifiedFile("file1.txt", "123", "folder", "added");
        files.add(file1);
        files.add(file2);
        return new Modification("svn", "user", comment, "use@email.com", date, "1234", files);

    }

    public void testShouldReturnCommentWhenStoryTrackerIsNull() {
        ModificationCommand command = new ModificationCommand(createModification("story1"), null);
        assertEquals("story1", command.getComment());
    }

    public void testShouldEscapeHtmlTag() throws Exception {
        String comment = "commit message <b>with</b> <some> html <tags>, go...";
        ModificationCommand command = new ModificationCommand(createModification(comment), null);
        String expected = StringEscapeUtils.escapeHtml(comment);
        assertEquals(expected, command.getComment());
    }

    public void testShouldReturnJsonDataMap() throws Exception {
        ModificationCommand command = new ModificationCommand(createModification("story1"), null);
        Map map = command.toJsonData();
        assertEquals("svn", map.get("type"));
        assertEquals("user", map.get("user"));
        assertEquals("story1", map.get("comment"));
        assertEquals(new Long(date.getTime()), map.get("modifiedtime"));
        List files = (List) map.get("files");
        assertEquals(2, files.size());
        assertEquals("file1.txt", ((Map) files.get(0)).get("filename"));
        assertEquals("123", ((Map) files.get(0)).get("revision"));
        assertEquals("folder", ((Map) files.get(0)).get("folder"));
        assertEquals("deleted", ((Map) files.get(0)).get("action"));
    }
}

