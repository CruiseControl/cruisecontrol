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
package net.sourceforge.cruisecontrol.dashboard;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.joda.time.DateTime;

public class ModificationSetTest extends TestCase {

    public void testShouldGroupModificationsByCommentAndUser() {
        ModificationSet set = new ModificationSet();

        String firstUser = "Boris";
        String secondUser = "Norman";

        String initialCheckinComment = "Initial Checkin";
        set.add("cvs", firstUser, initialCheckinComment, "1.2", ModificationAction.ADD, "somefile.xml", new DateTime());
        set.add("cvs", firstUser, initialCheckinComment, "1.3", ModificationAction.MODIFIED, "anotherfile.xml",
                new DateTime());
        set.add("cvs", secondUser, initialCheckinComment, "1.4", ModificationAction.ADD, "anotherfile.xml",
                new DateTime());

        Collection modifications = set.getModifications();

        assertEquals(2, modifications.size());

        Iterator iterator = modifications.iterator();
        iterator.next();
        Modification secondChangeSet = (Modification) iterator.next();
        assertEquals(ToStringBuilder.reflectionToString(secondChangeSet), secondChangeSet.toString());

        assertEquals("cvs", secondChangeSet.getType());
        assertEquals(initialCheckinComment, secondChangeSet.getComment());

        List files = secondChangeSet.getModifiedFiles();
        assertEquals(2, files.size());

        ModifiedFile firstFile = (ModifiedFile) files.get(0);
        ModifiedFile secondFile = (ModifiedFile) files.get(1);

        assertEquals("somefile.xml", firstFile.getFilename());
        assertEquals("1.2", firstFile.getRevision());
        assertEquals(ModificationAction.ADD, firstFile.getAction());

        assertEquals("anotherfile.xml", secondFile.getFilename());
        assertEquals("1.3", secondFile.getRevision());
        assertEquals(ModificationAction.MODIFIED, secondFile.getAction());
    }

}
