/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
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

package net.sourceforge.cruisecontrol.publishers.email;

import net.sourceforge.cruisecontrol.CruiseControlException;

import org.apache.log4j.Logger;
import java.util.Set;
import java.util.Iterator;

public class EmailAddressMapper extends EmailMapper {

    private static final Logger LOG = Logger.getLogger(EmailAddressMapper.class);

    public EmailAddressMapper() {
    }

    /*
     * invoked to initialize each time before this mapped is used to map a set of users.
     * this method can be invoked multiple times during instance lifetime
     */
    public void open() throws CruiseControlException {
    }

    /*
     * invoked after a set of users has been mapped.
     * this method can be invoked multiple times during instance lifetime
     */
    public void close() {
    }

    /*
     * invoked to map a user, return null if this mapper can not map the user.
     * invoked zero or more times between invocations of open and close
     */
    public String mapUser(String user) {
        return null;
    }
    /*
     * returns whether the result can be cached.
     * invoked immediately after non-null resulting invocations of mapUser
     */
    public boolean cacheable() {
        return true;
    }

    /*
     * If a need exists to override this method, extend EmailMapper instead of this class.
     *
     * @see net.sourceforge.cruisecontrol.publishers.EmailMapper#mapUsers(java.util.Set, java.util.Set)
     */
    public final void mapUsers(Set users, Set mappedUsers) {
        try {
            open();

            // iterate over all users
            for (Iterator userIterator = users.iterator(); userIterator.hasNext(); ) {
                String user = (String) userIterator.next();
                String mappedUser = mapUser(user);

                // if hit, remove from users and add to mappedUsers
                if (mappedUser != null) {
                    LOG.debug("Mapped user " + user + " to " + mappedUser);
                    mappedUsers.add(mappedUser);
                    userIterator.remove();
                    // cache the results unless the mapper doesn't want to
                    if (cacheable()) {
                        EmailMapperHelper.addCacheEntry(getEmailPublisher(), user, mappedUser);
                    }
                }
            }
        } catch (CruiseControlException ce) {
            LOG.error(ce.getMessage());
        } finally {
            close();
        }
    }
}
