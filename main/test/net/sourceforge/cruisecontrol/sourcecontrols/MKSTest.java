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
package net.sourceforge.cruisecontrol.sourcecontrols;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.util.Commandline;

public class MKSTest extends TestCase {

    public void testShouldReturnPreviousModificationOnlyWhenPassedSameLastBuildDate() {
        final List expected = new ArrayList();
        
        MKS mks = new MKS() {

            Commandline createResyncCommandLine(String projectFilePath) {
                return null;
            }

            void executeResyncAndParseModifications(Commandline cmdLine, List modifications) {
                Modification mod = new Modification();
                mod.modifiedTime = new Date();
                mod.userName = "";
                
                expected.add(mod);
                modifications.add(mod);
            }

            String getProjectFilePath() {
                return null;
            }
            
        };
        
        Date lastBuild = new Date(10000);  
        
        List actual = mks.getModifications(lastBuild, null);
        assertEquals(1, actual.size());
        assertEquals(expected.get(0), actual.get(0));
        
        actual = mks.getModifications(lastBuild, null);
        assertEquals(2, actual.size());
        assertEquals(expected.get(0), actual.get(0));
        assertEquals(expected.get(1), actual.get(1));
        
        Date newLastBuild = new Date(20000);
        actual = mks.getModifications(newLastBuild, null);
        assertEquals(1, actual.size());
        assertEquals(expected.get(2), actual.get(0));
    }
    
    public void testValidate() {

        MKS mks = new MKS();

        try {
            mks.validate();
            fail("MKS should throw exceptions when required attributes are not set.");
        } catch (CruiseControlException e) {
            assertTrue(true);
        }

        mks.setLocalWorkingDir("empty");

        try {
            mks.validate();
            fail("MKS should throw exceptions when required attributes are not set.");
        } catch (CruiseControlException e) {
            assertTrue(true);
        }

        mks.setProject("empty");

        try {
            mks.validate();
            assertTrue(true);
        } catch (CruiseControlException e) {
            fail("MKS should not throw exceptions when required attributes are set.");
        }
    }
}
