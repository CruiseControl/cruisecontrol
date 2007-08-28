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

import org.apache.commons.lang.StringEscapeUtils;

import net.sourceforge.cruisecontrol.dashboard.Modification;
import net.sourceforge.cruisecontrol.dashboard.StoryTracker;
import junit.framework.TestCase;

public class ModificationCommandTest extends TestCase {

    public void testShouldreturnHyperLinkWhenCommandIsStoryTrackerSensitive() {
        ModificationCommand command =
                new ModificationCommand(new Modification("user", "type", "story1"), new StoryTracker("pj",
                        "http://mingle/story/", "story"));
        assertEquals("<a href=\"http://mingle/story/1\">story1</a>", command.getComment());
    }

    public void testShouldReturnCommentWhenStoryTrackerIsNull() {
        ModificationCommand command =
                new ModificationCommand(new Modification("user", "type", "story1"), null);
        assertEquals("story1", command.getComment());
    }

    public void testShouldEscapeHtmlTag() throws Exception {
        String comment = "commit message <b>with</b> <some> html <tags>, go...";
        ModificationCommand command =
                new ModificationCommand(new Modification("type", "user1", comment), null);
        String expected = StringEscapeUtils.escapeHtml(comment);
        assertEquals(expected, command.getComment());
    }
}
