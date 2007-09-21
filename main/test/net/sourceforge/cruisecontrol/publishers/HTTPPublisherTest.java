/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
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
import net.sourceforge.cruisecontrol.CruiseControlException;

/**
 * Test for HTTPPublisher
 *
 * @see net.sourceforge.cruisecontrol.publishers.HTTPPublisher
 *
 * @author <a href="jonathan@indiekid.org">Jonathan Gerrish</a>
 */
public class HTTPPublisherTest extends TestCase {

    public void testValidateInvalidURL() {
        HTTPPublisher publisher = new HTTPPublisher();
        publisher.setUrl("zzz://invalid;;url;;");
        publisher.setRequestMethod("POST");
        try {
            publisher.validate();
            fail("Must throw exception if url is invalid.");
        } catch (CruiseControlException expected) {
            // Expected
        }
    }

    public void testValidateInvalidRequestMethod() {
        HTTPPublisher publisher = new HTTPPublisher();
        publisher.setUrl("http://www.google.com");
        publisher.setRequestMethod("INVALID_HTTP_METHOD_NAME");
        try {
            publisher.validate();
            fail("Must throw exception if url is invalid.");
        } catch (CruiseControlException expected) {
            // Expected
        }
    }

    public void testValidate() throws Exception {
        HTTPPublisher publisher = new HTTPPublisher();
        publisher.setUrl("http://www.google.com");
        publisher.setRequestMethod("GET");

        publisher.validate();
    }
}
