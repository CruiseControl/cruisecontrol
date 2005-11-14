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
package net.sourceforge.cruisecontrol.publishers;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.Attribute;

public class GenericPublisherDetailTest extends TestCase {
    private Attribute[] ftpPublisherAttrs;
    
    private Attribute[] xsltLogPublisherAttrs;

    protected void setUp() throws Exception {
        super.setUp();
        
        ftpPublisherAttrs = new GenericPublisherDetail(FTPPublisher.class).getRequiredAttributes();
        xsltLogPublisherAttrs = new GenericPublisherDetail(XSLTLogPublisher.class).getRequiredAttributes();
    }
    
    public void testShouldDetermineRequiredAttributes() {
        assertEquals(3, ftpPublisherAttrs.length);

        assertEquals("deleteArtifacts", ftpPublisherAttrs[0].getName());
        assertEquals(Boolean.TYPE, ftpPublisherAttrs[0].getDataType());
        assertEquals("destDir", ftpPublisherAttrs[1].getName());
        assertEquals(String.class, ftpPublisherAttrs[1].getDataType());
        assertEquals("srcDir", ftpPublisherAttrs[2].getName());
        assertEquals(String.class, ftpPublisherAttrs[2].getDataType());

        assertEquals(4, xsltLogPublisherAttrs.length);

        assertEquals("directory", xsltLogPublisherAttrs[0].getName());
        assertEquals(String.class, xsltLogPublisherAttrs[0].getDataType());
        assertEquals("outFileName", xsltLogPublisherAttrs[1].getName());
        assertEquals(String.class, xsltLogPublisherAttrs[1].getDataType());
        assertEquals("publishOnFail", xsltLogPublisherAttrs[2].getName());
        assertEquals(Boolean.TYPE, xsltLogPublisherAttrs[2].getDataType());
        assertEquals("xsltFile", xsltLogPublisherAttrs[3].getName());
        assertEquals(String.class, xsltLogPublisherAttrs[3].getDataType());
    }
}
