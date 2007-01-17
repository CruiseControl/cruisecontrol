/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2003, ThoughtWorks, Inc.
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import net.sourceforge.cruisecontrol.util.XMLLogHelper;

import org.apache.log4j.Logger;

import com.lotus.sametime.announcement.AnnouncementService;
import com.lotus.sametime.community.CommunityService;
import com.lotus.sametime.community.Login;
import com.lotus.sametime.community.LoginEvent;
import com.lotus.sametime.community.LoginListener;
import com.lotus.sametime.core.comparch.DuplicateObjectException;
import com.lotus.sametime.core.comparch.STSession;
import com.lotus.sametime.core.types.STGroup;
import com.lotus.sametime.core.types.STObject;
import com.lotus.sametime.core.types.STUser;
import com.lotus.sametime.lookup.GroupContentEvent;
import com.lotus.sametime.lookup.GroupContentGetter;
import com.lotus.sametime.lookup.GroupContentListener;
import com.lotus.sametime.lookup.LookupService;
import com.lotus.sametime.lookup.ResolveEvent;
import com.lotus.sametime.lookup.ResolveListener;
import com.lotus.sametime.lookup.Resolver;

/**
 * Publish (simple) build results by sending a Sametime announcement.
 * <p>Requires Sametime 3.0 Java Toolkit.  See http://www-10.lotus.com/ldd/toolkits<br>
 * In particular, requires STComm.jar
 * @author Richard Lewis-Shell
 */
public class SametimeAnnouncementPublisher extends LinkEmailPublisher
             implements LoginListener, ResolveListener, GroupContentListener {
    private static final Logger LOG = Logger.getLogger(SametimeAnnouncementPublisher.class);

    public static final String RESOLVE_CONFLICTS_RECIPIENT = "recipient"; // default
    public static final String RESOLVE_CONFLICTS_IGNORE = "ignore";
    public static final String RESOLVE_CONFLICTS_WARN = "warn";
    public static final String RESOLVE_CONFLICTS_ERROR = "error";

    public static final String RESOLVE_FAIL_IGNORE = "ignore";
    public static final String RESOLVE_FAIL_WARN = "warn";
    public static final String RESOLVE_FAIL_ERROR = "error"; // default

    public static final String QUERY_GROUP_CONTENT_FAIL_IGNORE = "ignore";
    public static final String QUERY_GROUP_CONTENT_FAIL_WARN = "warn";
    public static final String QUERY_GROUP_CONTENT_FAIL_ERROR = "error"; // default

    // Configurable properties

    // Sametime community
    private String community;
    // whether to resolve addresses as users
    private boolean resolveUsers = true;
    // whether to resolve addresses as groups
    private boolean resolveGroups = true;
    // send to group contents, rather than the group itself
    private boolean useGroupContent = true;
    // valid values: recipient, ignore, fail
    private String handleResolveConflicts = RESOLVE_CONFLICTS_RECIPIENT;
    // how to handle resolve failures
    private String handleResolveFails = RESOLVE_FAIL_ERROR;
    // how to handle query group content failures
    private String handleQueryGroupContentFails = QUERY_GROUP_CONTENT_FAIL_ERROR;
    // how long to wait (in seconds) before giving up on an interaction with the sametime server
    private int timeout = 10;
    // how long to sleep (in milliseconds) before looking for a response from the sametime server
    private int sleepMillis = 5;

    // Internal state

    // Sametime components
    private STSession session;
    private CommunityService communityService;
    private AnnouncementService announcementService;
    private LookupService lookupService;

    // list of users/groups to resolve into STObjects
    private Set usernamesToResolveSet;
    // login, null if not logged in
    private Login login;
    // list of resolved STUsers
    private Set recipientUserSet = null;
    // list of resolved STGroups
    private Set recipientGroupSet = null;
    // list of resovled user/group NAMES
    private Set resolvedNameSet = null;
    // used to temporarily hold group content (while getting)
    private STObject[] groupContent = null;
    // error messages constructed in a different thread, thrown as exceptions
    // in the main thread
    private String resolveFailMessage = null;
    private String resolveConflictMessage = null;
    private String queryGroupContentFailMessage = null;

    // rename the mailhost property
    public String getHost() {
        return this.getMailHost();
    }

    // rename the mailhost property
    public void setHost(String value) {
        this.setMailHost(value);
    }

    public String getCommunity() {
        return this.community;
    }

    public void setCommunity(String value) {
        this.community = value;
    }

    public boolean isResolveUsers() {
        return this.resolveUsers;
    }

    public void setResolveUsers(boolean value) {
        this.resolveUsers = value;
    }

    public boolean isResolveGroups() {
        return this.resolveGroups;
    }

    public void setResolveGroups(boolean value) {
        this.resolveGroups = value;
    }

    public boolean isUseGroupContent() {
        return this.useGroupContent;
    }

    public void setUseGroupContent(boolean value) {
        this.useGroupContent = value;
    }

    public String getHandleQueryGroupContentFails() {
        return this.handleQueryGroupContentFails;
    }

    public String getHandleResolveConflicts() {
        return this.handleResolveConflicts;
    }

    public String getHandleResolveFails() {
        return this.handleResolveFails;
    }

    public void setHandleQueryGroupContentFails(String string) {
        this.handleQueryGroupContentFails = string;
    }

    public void setHandleResolveConflicts(String value) {
        this.handleResolveConflicts = value;
    }

    public void setHandleResolveFails(String value) {
        this.handleResolveFails = value;
    }

    public int getTimeout() {
        return this.timeout;
    }

    private int getTimeoutMillis() {
        return this.getTimeout() * 1000;
    }

    public int getSleepMillis() {
        return sleepMillis;
    }

    public void setTimeout(int value) {
        timeout = value;
    }

    public void setSleepMillis(int value) {
        sleepMillis = value;
    }

    // override this so we can otherwise rely on EmailPublisher's validation
    // returnAddress makes no sense for Sametime
    public String getReturnAddress() {
        return "";
    }

    public void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet(getHost(), "host", this.getClass());
        ValidationHelper.assertIsSet(getUsername(), "username", this.getClass());

        ensureInEqualsIgnoreCase(this.getHandleResolveFails(), "handleResolveFails",
            new String[] {RESOLVE_FAIL_ERROR, RESOLVE_FAIL_WARN, RESOLVE_FAIL_WARN});

        ensureInEqualsIgnoreCase(this.getHandleResolveConflicts(), "handleResolveConflicts",
            new String[] {RESOLVE_CONFLICTS_ERROR, RESOLVE_CONFLICTS_IGNORE,
                          RESOLVE_CONFLICTS_RECIPIENT, RESOLVE_CONFLICTS_WARN});

        ensureInEqualsIgnoreCase(this.getHandleQueryGroupContentFails(), "handleQueryGroupContentFails",
            new String[] {QUERY_GROUP_CONTENT_FAIL_ERROR, QUERY_GROUP_CONTENT_FAIL_IGNORE,
                          QUERY_GROUP_CONTENT_FAIL_WARN});
    }

    private static void ensureInEqualsIgnoreCase(String attribute, String attributeName,
                                                 String[] strings) throws CruiseControlException {
        for (int i = 0; i < strings.length; i++) {
            String string = strings[i];
            if (attribute.equalsIgnoreCase(string)) {
                return;
            }
        }
        StringBuffer buf = new StringBuffer("'");
        buf.append(attributeName).append("' attribute invalid. - valid values are ");
        for (int i = 0; i < strings.length; i++) {
            if (i > 0) {
                buf.append(" | ");
            }
            buf.append(strings[i]);
        }
        ValidationHelper.fail(buf.toString());
    }

    // use the build results URL as the message content
    protected String createMessage(XMLLogHelper logHelper) {
        return this.getBuildResultsURL() == null ? null : super.createMessage(logHelper);
    }

    // not really sending mail, but LinkEmailPublisher has a lot of useful logic
    // to reuse - skipUsers, spamWhileBroken etc...
    protected boolean sendMail(String toList, String subject, String message, boolean important)
        throws CruiseControlException {

        boolean announcementSent = false;

        LOG.info("Sending sametime notifications.");

        // turn the comma separated toList into a list of users/groups to be resolved
        this.usernamesToResolveSet = new HashSet();
        for (StringTokenizer strtok = new StringTokenizer(toList, ","); strtok.hasMoreTokens(); ) {
            String token = strtok.nextToken();
            this.usernamesToResolveSet.add(token.trim());
        }

        try {
            this.session = new STSession("CruiseControl build notification" + this);
        } catch (DuplicateObjectException ex) {
            throw new RuntimeException("cannot create sametime session" + ex);
        }
        this.session.loadSemanticComponents();
        this.session.start();

        this.communityService = (CommunityService) session.getCompApi(CommunityService.COMP_NAME);
        this.lookupService = (LookupService) session.getCompApi(LookupService.COMP_NAME);
        this.announcementService = (AnnouncementService) session.getCompApi(AnnouncementService.COMP_NAME);
        this.communityService.addLoginListener(this);
        if (this.getPassword() != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("loginByPassword(" + this.getHost() + ", " + this.getUsername() + ", ****, "
                    + this.getCommunity() + ")");
            }
            this.communityService.loginByPassword(this.getHost(), this.getUsername(), this.getPassword(),
                                                  this.getCommunity());
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("loginAsAnon(" + this.getHost() + ", " + this.getUsername()
                    + this.getCommunity() + ")");
            }
            this.communityService.loginAsAnon(this.getHost(), this.getUsername(), this.getCommunity()); // not tested
        }

        // if we lose the connection, just give up
        this.communityService.disableAutomaticReconnect();

        boolean bored = false;
        long waitStart = System.currentTimeMillis();
        while (!this.isLoggedIn() && !bored) {
            try {
                Thread.sleep(this.getSleepMillis());
            } catch (InterruptedException ex) {
                throw new RuntimeException("sleep interrupted: " + ex);
            }
            bored = System.currentTimeMillis() - waitStart > this.getTimeoutMillis();
        }
        if (bored && !this.isLoggedIn()) {
            throw new RuntimeException("bored waiting for login");
        }

        try {
            this.resolve();

            if (this.isUseGroupContent()) {
                this.getGroupContent();
            } else if (this.recipientGroupSet != null) {
                this.recipientUserSet.addAll(this.recipientGroupSet);
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("announce to: " + this.recipientUserSet);
            }
            // "\r\n" appears to be the end of line marker sametime uses
            String announcementMessage = message == null ? subject : subject + "\r\n" + message;
            this.announcementService.sendAnnouncement((STObject[]) this.recipientUserSet.toArray(
                                      new STObject[this.recipientUserSet.size()]), false, announcementMessage);
            announcementSent = true;
        } finally {
            try {
                this.communityService.logout();
            } finally {
                try {
                    this.session.stop();
                } finally {
                    this.session.unloadSession();
                }
            }
            return announcementSent;
        }
    }

    private void resolve() throws CruiseControlException {
        Resolver resolver = lookupService.createResolver(false, false, this.isResolveUsers(), this.isResolveGroups());
        resolver.addResolveListener(this);

        try {
            this.recipientUserSet = new HashSet();
            this.recipientGroupSet = new HashSet();
            this.resolvedNameSet = new HashSet();

            if (LOG.isDebugEnabled()) {
                LOG.debug("resolving: " + this.usernamesToResolveSet);
            }
            resolver.resolve(
               (String[]) this.usernamesToResolveSet.toArray(new String[this.usernamesToResolveSet.size()]));

            // how long do we wait to resolve?
            boolean bored = false;
            long waitStart = System.currentTimeMillis();
            while (!this.isResolvedAllUsersAndGroups() && !bored && !this.isResolveError()) {
                try {
                    Thread.sleep(this.getSleepMillis());
                } catch (InterruptedException ex) {
                    throw new RuntimeException("sleep interrupted: " + ex);
                }
                bored = System.currentTimeMillis() - waitStart > this.getTimeoutMillis();
            }
            if (this.resolveFailMessage != null
                && RESOLVE_FAIL_ERROR.equalsIgnoreCase(this.getHandleResolveFails())) {
                throw new CruiseControlException(this.resolveFailMessage);
            }
            if (this.resolveConflictMessage != null
                && RESOLVE_CONFLICTS_ERROR.equalsIgnoreCase(this.getHandleResolveConflicts())) {
                throw new CruiseControlException(this.resolveConflictMessage);
            }
            if (bored && !this.isResolvedAllUsersAndGroups()) {
                throw new CruiseControlException("bored waiting for user/group resolving");
            }
        } finally {
            resolver.removeResolveListener(this);
        }
    }

    private boolean haveGroupContent() {
        return this.groupContent != null;
    }

    // convert the recipientGroupList into group content users
    private void getGroupContent() throws CruiseControlException {
        if (this.recipientGroupSet == null)  {
            return;
        }
        GroupContentGetter groupContentGetter = this.lookupService.createGroupContentGetter();
        groupContentGetter.addGroupContentListener(this);
        try {
            for (Iterator i = this.recipientGroupSet.iterator(); i.hasNext(); ) {
                this.groupContent = null;
                STGroup group = (STGroup) i.next();
                groupContentGetter.queryGroupContent(group);
                // how long do we wait to resolve?
                boolean bored = false;
                long waitStart = System.currentTimeMillis();
                while (!this.haveGroupContent() && !bored && !this.isQueryGroupContentError()) {
                    try {
                        Thread.sleep(this.getSleepMillis());
                    } catch (InterruptedException ex) {
                        throw new CruiseControlException("sleep interrupted: " + ex);
                    }
                    bored = System.currentTimeMillis() - waitStart > this.getTimeoutMillis();
                }
                if (this.isQueryGroupContentError()) {
                    throw new CruiseControlException(this.queryGroupContentFailMessage);
                }
                if (bored && !this.haveGroupContent()) {
                    throw new CruiseControlException("bored waiting to get group content for " + group);
                }
            }
        } finally {
            groupContentGetter.removeGroupContentListener(this);
        }
    }

    private synchronized boolean isQueryGroupContentError() {
        return this.queryGroupContentFailMessage != null
               && QUERY_GROUP_CONTENT_FAIL_ERROR.equalsIgnoreCase(this.getHandleQueryGroupContentFails());
    }

    private synchronized boolean isLoggedIn() {
        return this.communityService != null && this.communityService.isLoggedIn() && this.login != null;
    }

    // return true once all the users/groups to be resolved have been resolved
    private synchronized boolean isResolvedAllUsersAndGroups() {
        if (this.usernamesToResolveSet == null) {
            return true;
        }
        for (Iterator i = this.usernamesToResolveSet.iterator(); i.hasNext(); ) {
            String username = (String) i.next();
            if (!this.resolvedNameSet.contains(username.toLowerCase())) {
                return false;
            }
        }
        return true;
    }

    private synchronized boolean isResolveError() {
        return this.resolveFailMessage != null
               && RESOLVE_FAIL_ERROR.equalsIgnoreCase(this.getHandleResolveFails())
               || this.resolveConflictMessage != null
               && RESOLVE_CONFLICTS_ERROR.equalsIgnoreCase(this.getHandleResolveConflicts());
    }

    public synchronized void loggedIn(LoginEvent loginEvent) {
        this.login = loginEvent.getLogin();
    }

    public synchronized void loggedOut(LoginEvent arg0) {
        this.login = null;
    }

    public synchronized void resolveConflict(ResolveEvent resolveEvent) {
        STObject[] resolvedList = resolveEvent.getResolvedList();
        if (LOG.isDebugEnabled()) {
            LOG.debug("resolve conflict: " + Arrays.asList(resolvedList));
        }
        if (RESOLVE_CONFLICTS_RECIPIENT.equalsIgnoreCase(this.getHandleResolveConflicts())) {
            if (resolvedList != null) {
                for (int i = 0; i < resolvedList.length; i++) {
                    this.addRecipient(resolvedList[i]);
                }
            }
        } else if (RESOLVE_CONFLICTS_ERROR.equalsIgnoreCase(this.getHandleResolveConflicts())) {
            this.resolveConflictMessage = "resolveConflicts: " + Arrays.asList(resolvedList);
        } else if (RESOLVE_CONFLICTS_WARN.equalsIgnoreCase(this.getHandleResolveConflicts())) {
            LOG.warn("resolveConflicts: " + Arrays.asList(resolvedList));
        }
    }

    private synchronized void addRecipient(STObject recipient) {
        this.resolvedNameSet.add(recipient.getName().toLowerCase());
        if (recipient instanceof STGroup) {
            this.recipientGroupSet.add(recipient);
        } else if (recipient instanceof STUser) {
            this.recipientUserSet.add(recipient);
        }
    }

    public synchronized void resolved(ResolveEvent resolveEvent) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("resolved: " + resolveEvent.getResolved());
        }
        this.addRecipient(resolveEvent.getResolved());
    }

    public synchronized void resolveFailed(ResolveEvent resolveEvent) {
        String[] failedNames = resolveEvent.getFailedNames();
        if (LOG.isDebugEnabled()) {
            LOG.debug("resolve failed: " + Arrays.asList(failedNames));
        }
        this.resolveFailMessage = "cannot resolve, reason "
                         + resolveEvent.getReason() + ": " + Arrays.asList(failedNames);
        if (RESOLVE_FAIL_WARN.equalsIgnoreCase(this.getHandleResolveFails())) {
            LOG.warn(this.resolveFailMessage);
        }
    }

    public synchronized void groupContentQueried(GroupContentEvent groupContentEvent) {
        STObject[] eventGroupContent = groupContentEvent.getGroupContent();
        if (eventGroupContent != null) {
            for (int i = 0; i < eventGroupContent.length; i++) {
                // add directly to the user set as we are iterating over the goup set, so cannot modify it
                // this means that we will not support groups within groups
                if (eventGroupContent[i] instanceof STUser) {
                    this.recipientUserSet.add(eventGroupContent[i]);
                } else {
                    throw new UnsupportedOperationException("groups within groups not supported - found subgroup "
                       + eventGroupContent[i].getName() + " while querying group "
                       + groupContentEvent.getGroup().getName());
                }
            }
        }
        this.groupContent = eventGroupContent;
    }

    public synchronized void queryGroupContentFailed(GroupContentEvent groupContentEvent) {
        this.queryGroupContentFailMessage = "queryGroupContent failed for group "
             + groupContentEvent.getGroup().getName()
             + ", reason " + groupContentEvent.getReason();
        if (QUERY_GROUP_CONTENT_FAIL_WARN.equalsIgnoreCase(this.getHandleQueryGroupContentFails())) {
            LOG.warn(this.queryGroupContentFailMessage);
        }
    }

}
