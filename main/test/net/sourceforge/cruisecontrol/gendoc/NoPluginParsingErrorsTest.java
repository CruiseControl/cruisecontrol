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
package net.sourceforge.cruisecontrol.gendoc;

import java.util.List;
import net.sourceforge.cruisecontrol.PluginRegistry;
import junit.framework.TestCase;

/**
 * A test case to make sure that no parsing errors have occurred in the parsed html.
 * This will ensure that the build fails if there are errors in the gendoc output.
 * @author lovea@msoe.edu
 * @version 1.0
 */
public class NoPluginParsingErrorsTest extends TestCase {
    PluginInfoParser parser;
    
    /**
     * Generates the HTML and parses it for testing purposes.
     */
    protected void setUp() throws Exception {        
        parser = new PluginInfoParser(
                PluginRegistry.createRegistry(), PluginRegistry.ROOT_PLUGIN);
    } 
    
    public void testParsingErrors() {
        boolean errorsPresent = errorListTest(parser.getParsingErrors());
        for(PluginInfo info: parser.getAllPlugins()) {
            errorsPresent = errorsPresent || errorListTest(info.getParsingErrors());
        }
        assertFalse(errorsPresent);
    }
    
    private boolean errorListTest(List<String> errors) {
        boolean errorsPresent = false;
        if(errors.size() > 0) {
            errorsPresent = true;
            for(String err : errors) { 
                System.err.println(err);
            }
        }
        return errorsPresent;
    }
}
