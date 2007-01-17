/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2005, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.util;

import net.sourceforge.cruisecontrol.CruiseControlException;

/**
 * <p>Just like XPathAwareChild, but has an additional "name" attribute associated with it. This is useful when you
 * want repeating children, e.g.</p>
 * <pre>
 * &lt;myfavoriteplugin&gt;
 *     &lt;field name="name" value="Tony"/&gt;
 *     &lt;field name="address" value="Your Street, USA"/&gt;
 *     &lt;field name="unittestcount" xpathExpression="sum(cruisecontrol/testsuite/@tests)"/&gt;
 * &lt;/myfavoriteplugin&gt;
 * </pre>
 * For example usage, see
 * {@link net.sourceforge.cruisecontrol.publishers.sfee.SfeeTrackerPublisher#createField()}.
 *
 * @author <a href="mailto:pj@thoughtworks.com">Paul Julius</a>
 * @author <a href="mailto:krs@thoughtworks.com">Kent Spillner</a>
 */
public class NamedXPathAwareChild extends XPathAwareChild {
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        markDirty();
        this.name = name;
    }

    public void validate() throws CruiseControlException {
        if (name == null) {
            throw new CruiseControlException("name must be set.");
        }

        super.validate();
    }

}
