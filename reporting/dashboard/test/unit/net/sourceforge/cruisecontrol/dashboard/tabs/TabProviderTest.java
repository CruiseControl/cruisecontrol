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
package net.sourceforge.cruisecontrol.dashboard.tabs;

import org.jmock.cglib.MockObjectTestCase;
import org.jmock.Mock;

public class TabProviderTest extends MockObjectTestCase {
    private Mock mockTabContainer;
    private TabProvider tabProvider;

    protected void setUp() throws Exception {
        mockTabContainer = mock(TabContainer.class);
        tabProvider = new TabProvider((TabContainer) mockTabContainer.proxy());
        tabProvider.setName("DefaultTab");
        tabProvider.setLink("defaulttab");
    }

    public void testShouldAddItselfToTabContainerAfterPropertiesSet() throws Exception {
        mockTabContainer.expects(once()).method("register").with(eq(tabProvider));
        tabProvider.afterPropertiesSet();
    }

    public void testShouldFailIfLinkNotSet() throws Exception {
        mockTabContainer.expects(never()).method("register");
        tabProvider.setLink(null);
        try {
            tabProvider.afterPropertiesSet();
            fail("Should not pass when link not set");
        } catch (Exception expected) {
            assertEquals("Not all properties set", expected.getMessage());
        }
    }

    public void testShouldFailIfNameNotSet() throws Exception {
        mockTabContainer.expects(never()).method("register");
        tabProvider.setName(null);
        try {
            tabProvider.afterPropertiesSet();
            fail("Should not pass when name not set");
        } catch (Exception expected) {
            assertEquals("Not all properties set", expected.getMessage());
        }
    }
}
