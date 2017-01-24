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
package net.sourceforge.cruisecontrol.publishers;

import java.util.ArrayList;
import java.util.List;

import org.jdom.Element;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Publisher;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

/**
 * An abstract base class for any publisher which wishes to conditionally
 * execute a set of contained <code>Publisher</code>s.
 */
public abstract class ConditionalPublisher implements Publisher {

    private final List<Publisher> publishers = new ArrayList<Publisher>();

    public void publish(final Element log) throws CruiseControlException {
        if (shouldPublish(log)) {
            for (final Publisher publisher : publishers) {
                publisher.publish(log);
            }
        }
    }

    public void validate() throws CruiseControlException {
        ValidationHelper.assertTrue(publishers.size() > 0,
            "conditional publishers should have at least one nested publisher");

        for (final Publisher publisher : publishers) {
            publisher.validate();
        }
    }

    /**
     * Adds a nested publisher
     *
     * @param publisher The publisher to add
     */
    public void add(final Publisher publisher) {
        publishers.add(publisher);
    }

    /**
     * Determines if the nested publishers should be executed. This method must
     * be implemented by all derived classes.
     *
     * @param log
     *            The build log
     * @return <code>true</code> if the nested publishers should be executed,
     *         <code>false</code> otherwise
     */
    public abstract boolean shouldPublish(Element log);
}
