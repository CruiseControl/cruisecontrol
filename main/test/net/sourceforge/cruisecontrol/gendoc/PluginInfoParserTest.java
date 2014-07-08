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
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.PluginRegistry;
import net.sourceforge.cruisecontrol.config.PluginPlugin;
import junit.framework.TestCase;

/**
 * Test for some of the methods of the PluginInfoParser. Much of the parsing logic
 * is verified in the test for the actual XXXInfo objects.
 * @author pollens@msoe.edu
 *         Date: 9/11/10
 */
public class PluginInfoParserTest extends TestCase {
    
    public void testGetAllPlugins() throws CruiseControlException {
        PluginRegistry registry = PluginRegistry.createRegistry();
        
        // Get a list of all the basic plugins.
        List<PluginInfo> basic =
            new PluginInfoParser(registry, PluginRegistry.ROOT_PLUGIN).getAllPlugins();
        
        // Now register a new plugin.
        PluginPlugin config = GendocTestUtils.createPlugin("mysc", DummySourceControl.class.getName());
        registry.register(config);
        
        // Get a new list, which should contain the new plugin.
        List<PluginInfo> expanded =
            new PluginInfoParser(registry, PluginRegistry.ROOT_PLUGIN).getAllPlugins();
        
        // Compare the two lists.
        assertTrue(basic.size() > 10); // Make sure some plugins exist.
        assertEquals(basic.size(), expanded.size() - 1);
    }
    
    public void testPluginLoadingError() throws CruiseControlException {
        PluginRegistry registry = PluginRegistry.createRegistry();
        
        // Register a nonexistent plugin.
        PluginPlugin config = GendocTestUtils.createPlugin("unicorn", "once.upon.a.time.Unicorn");
        registry.register(config);
        
        // Try to parse the plugins.
        PluginInfoParser parser = new PluginInfoParser(registry, PluginRegistry.ROOT_PLUGIN);
        
        List<String> errors = parser.getParsingErrors();
        assertEquals(1, errors.size());
        assertEquals("Failed to load class from PluginRegistry: once.upon.a.time.Unicorn",
                errors.get(0));
    }
    
    public void testInvalidRootPlugin() {
        PluginRegistry registry = PluginRegistry.createRegistry();
        
        try {
            new PluginInfoParser(registry, "doesn't exist");
            fail("Should have thrown an exception");
        } catch (RuntimeException e) {
            // This was expected.            
        }
    }
    
}
