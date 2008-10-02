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

import java.io.Serializable;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;

import net.sourceforge.cruisecontrol.publishers.EmailPublisher;

import org.apache.log4j.Logger;

public class EmailMapperHelper implements Serializable {

    private static final long serialVersionUID = 3831878181334595122L;

    private static final Logger LOG = Logger.getLogger(EmailMapperHelper.class);

    /*
     * CACHE contains a Map with mapped users for each Publisher
     */
    private static final Map<Object, Map<String, String>> CACHE = new Hashtable<Object, Map<String, String>>();

    public static void addCacheEntry(final Object cache, final String user, final String mappedUser) {
        Map<String, String> map = CACHE.get(cache);
        if (map == null) {
            map = new Hashtable<String, String>();
            CACHE.put(cache, map);
        }
        map.put(user, mappedUser);
    }

    public static String getCachedUser(final Object cache, final String user) {
        String mappedUser = null;
        final Map<String, String> map = CACHE.get(cache);
        if (map != null) {
            mappedUser = map.get(user);
        }
        return mappedUser;
    }

    public void mapUsers(final EmailPublisher publisher, final Set<String> users, final Set<String> mappedUsers) {
        // first iterate over the users and check if mapping is cached
        for (Iterator userIterator = users.iterator(); userIterator.hasNext(); ) {
            final String user = (String) userIterator.next();
            final String mappedUser = getCachedUser(publisher, user);
            if (mappedUser != null) {
                LOG.debug("User " + user + " found in cache.  Mapped to: " + mappedUser);
                userIterator.remove();
                mappedUsers.add(mappedUser);
            }
        }

        // iterate over each mapper,
        // put the mapping into the cache if a hit and partissapates in caching
        final EmailMapper[] mappers = publisher.getEmailMapper();
        for (int i = 0; i < mappers.length; i++) {
            mappers[i].mapUsers(users, mappedUsers);
        }

        // take the still unmapped users and move them into the mappedUsers set
        for (Iterator<String> userIterator = users.iterator(); userIterator.hasNext(); ) {
            mappedUsers.add(userIterator.next());
            userIterator.remove();
        }
    }
}
