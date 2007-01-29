/****************************************************************************
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
****************************************************************************/

package net.sourceforge.cruisecontrol.distributed.core;

import java.rmi.RMISecurityManager;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.List;

import net.jini.core.entry.Entry;

import org.apache.log4j.Logger;

public final class ReggieUtil {

    private ReggieUtil() { }

    private static final Logger LOG = Logger.getLogger(ReggieUtil.class);

    /**
     * converts an entries search string into a list of entries. An example
     * entries search string: "hostname=gandalf;os.name=Windows XP"
     *
     * @param searchString this semicolon delimited list of search entries
     * @return entriesList an array of entry objects
     */
    public static Entry[] convertStringEntries(final String searchString) {
        final StringTokenizer tokenizer = new StringTokenizer(searchString.trim(), ";");
        final List entriesList = new ArrayList();
        while (tokenizer.hasMoreElements()) {
            final String token = tokenizer.nextToken();
            final PropertyEntry entry = new PropertyEntry();
            entry.name = token.substring(0, token.indexOf("=")).trim();
            entry.value = token.substring(token.indexOf("=") + 1).trim();
            entriesList.add(entry);
        }

        LOG.debug("Entry List: " + entriesList);

        final Entry[] arrEntries;
        if (entriesList.size() == 0) {
            arrEntries = null;
        } else {
            arrEntries = (Entry[]) entriesList.toArray(new Entry[entriesList.size()]);
        }
        return arrEntries;
    }

    /**
     * Install the RMISecurityManager if not already installed.
     */
    public static void setupRMISecurityManager() {
        final SecurityManager origSecurityManager = System.getSecurityManager();
        if (origSecurityManager == null) {
            System.setSecurityManager(new RMISecurityManager());
        } else if (origSecurityManager.getClass().getName().indexOf("JavaWebStart") > -1) {
            // do nothing, we're running under webstart
        } else if (!(origSecurityManager instanceof RMISecurityManager)) {
            final String msg = "Unexpected Security Manager. origSecurityManager: "
                    + origSecurityManager;
            LOG.error(msg);
            throw new IllegalStateException(msg);
        }
    }
}
