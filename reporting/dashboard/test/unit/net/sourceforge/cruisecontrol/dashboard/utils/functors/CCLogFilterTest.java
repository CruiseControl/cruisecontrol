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
package net.sourceforge.cruisecontrol.dashboard.utils.functors;

import junit.framework.TestCase;

public class CCLogFilterTest extends TestCase {
    public void testShouldAcceptFailedLogFile() {
        assertTrue(new CCLogFilter().accept(null, "log19990505080808.xml"));
    }

    public void testShouldAcceptPassedLogFile() {
        assertTrue(new CCLogFilter().accept(null, "log19990505080808Lbuild.123.xml"));
    }

    public void testShouldNotAcceptFileIfDoesnotEndswithXml() {
        assertFalse(new CCLogFilter().accept(null, "file.notxml"));
    }

    public void testShouldNotAcceptFileIfDoesnotStartwithLog() {
        assertFalse(new CCLogFilter().accept(null, "filelog.xml"));
    }

    public void testShouldNotAcceptFileIfLengthShorterThan21() {
        assertFalse(new CCLogFilter().accept(null, "log1234.xml"));
        assertFalse(new CCLogFilter().accept(null, "log.xml"));
    }

    public void testShouldAcceptZippedLogFile() throws Exception {
        assertTrue(new CCLogFilter().accept(null, "log19990505080808.xml.gz"));
        assertTrue(new CCLogFilter().accept(null, "log19990505080808Lbuild.123.xml.gz"));
    }
}
