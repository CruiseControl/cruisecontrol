/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2005 ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.sourcecontrols;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.Attribute;

public class GenericSourceControlDetailsTest extends TestCase {
    private Attribute[] cvsAttrs;
    private Attribute[] svnAttrs;

    protected void setUp() throws Exception {
        super.setUp();
        
        cvsAttrs = new GenericSourceControlDetail(ConcurrentVersionsSystem.class).getRequiredAttributes();
        svnAttrs = new GenericSourceControlDetail(SVN.class).getRequiredAttributes();
    }

    public void testShouldDetermineRequiredAttributes() throws Exception {
        assertEquals(6, cvsAttrs.length);

        assertEquals("cvsRoot", cvsAttrs[0].getName());
        assertEquals(String.class, cvsAttrs[0].getDataType());
        assertEquals("localWorkingCopy", cvsAttrs[1].getName());
        assertEquals(String.class, cvsAttrs[1].getDataType());
        assertEquals("module", cvsAttrs[2].getName());
        assertEquals(String.class, cvsAttrs[2].getDataType());
        assertEquals("property", cvsAttrs[3].getName());
        assertEquals(String.class, cvsAttrs[3].getDataType());
        assertEquals("propertyOnDelete", cvsAttrs[4].getName());
        assertEquals(String.class, cvsAttrs[4].getDataType());
        assertEquals("tag", cvsAttrs[5].getName());
        assertEquals(String.class, cvsAttrs[5].getDataType());
        
        assertEquals(6, svnAttrs.length);
        
        assertEquals("localWorkingCopy", svnAttrs[0].getName());
        assertEquals(String.class, svnAttrs[0].getDataType());
        assertEquals("password", svnAttrs[1].getName());
        assertEquals(String.class, svnAttrs[1].getDataType());
        assertEquals("property", svnAttrs[2].getName());
        assertEquals(String.class, svnAttrs[2].getDataType());
        assertEquals("propertyOnDelete", svnAttrs[3].getName());
        assertEquals(String.class, svnAttrs[3].getDataType());
        assertEquals("repositoryLocation", svnAttrs[4].getName());
        assertEquals(String.class, svnAttrs[4].getDataType());
        assertEquals("username", svnAttrs[5].getName());
        assertEquals(String.class, svnAttrs[5].getDataType());
    }
}
