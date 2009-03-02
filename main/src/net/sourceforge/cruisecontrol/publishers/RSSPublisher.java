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
package net.sourceforge.cruisecontrol.publishers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import org.jdom.Element;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Publisher;
import net.sourceforge.cruisecontrol.util.XMLLogHelper;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import net.sourceforge.cruisecontrol.util.IO;

import net.sourceforge.cruisecontrol.publishers.rss.CruiseControlFeed;
import net.sourceforge.cruisecontrol.publishers.rss.CruiseControlItem;
import net.sourceforge.cruisecontrol.publishers.rss.Item;


/**
 * Publisher Plugin which publishes the most recent build information to an
 * RSS (Really Simple Syndication) Feed.
 *
 * Copyright (c) 2005 Hewlett-Packard Development Company, L.P.
 * Licensed under the CruiseControl BSD license
 * @author Patrick Conant
 */
public class RSSPublisher implements Publisher {

    private static final Logger LOG = Logger.getLogger(RSSPublisher.class);

    //Map to hold all RSSFeed instances.  The RSSFeeds are keyed according to
    // filename so that multiple projects can share a single RSSFeed.
    private static final Map<String, CruiseControlFeed> RSS_FEEDS = new HashMap<String, CruiseControlFeed>();

    private String fileName;
    private String buildresultsurl;
    private String channelLinkURL;
    private int maxLength = 10;

    private CruiseControlFeed rssFeed;


    /**
     *  Static method that allows multiple project RSSPublishers to share a
     *  single RSSFeed instance.
     * @param publishToFile the file identifying a feed
     * @return feed for the given file
     */
    public static CruiseControlFeed getRSSFeed(final File publishToFile) {

        // FIXME not thread safe

        String pathToPublishFile;
        try {
            pathToPublishFile = publishToFile.getCanonicalPath();
        } catch (IOException ioe) {
            pathToPublishFile = publishToFile.getAbsolutePath().toLowerCase();
        }
        CruiseControlFeed rssfeed = RSS_FEEDS.get(pathToPublishFile);

        if (rssfeed == null) {
            //Create a new RSS Feed and add it to the collection.
            rssfeed = new CruiseControlFeed(publishToFile);
            RSS_FEEDS.put(pathToPublishFile, rssfeed);
        }

        rssfeed.incrementProjectCount();
        return rssfeed;
    }


    /**
     *  Define the publishing.
     *
     *  @param cruisecontrolLog JDOM Element representation of the main cruisecontrol build log
     */
    public void publish(final Element cruisecontrolLog) throws CruiseControlException {

        final XMLLogHelper helper = new XMLLogHelper(cruisecontrolLog);


        // Get a reference to the RSSFeed
        if (this.rssFeed == null) {
            this.rssFeed = getRSSFeed(new File(this.fileName));

            // Ensure that the rssFeed matches the config properties of the publisher.
            this.rssFeed.setProjectName(helper.getProjectName());
            this.rssFeed.setMaxLength(this.maxLength);
            this.rssFeed.setLink(this.channelLinkURL);
        }

        // Create the RSSFeedItem
        final Item rssItem = new CruiseControlItem(helper, this.buildresultsurl);
        rssFeed.addItem(rssItem);

        // Publish the feed.
        publishFeed();
    }


    protected void publishFeed() throws CruiseControlException {

        FileWriter fw = null;
        try {
            fw = new FileWriter(fileName);
            this.rssFeed.write(fw);
        } catch (IOException ioe) {
            throw new CruiseControlException("Error writing file: " + fileName, ioe);
        } finally {
            IO.close(fw);
        }
    }

    /**
     *  Called after the configuration is read to make sure that all the mandatory parameters
     *  were specified..
     *
     *  @throws CruiseControlException if there was a configuration error.
     */
    public void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet(fileName, "filename", this.getClass());
        ValidationHelper.assertIsSet(buildresultsurl, "buildresultsurl", this.getClass());
    }

    /**
     *  @param fileName the name of the file to which the RSS feed should be pushed.
     */
    public void setFile(final String fileName) {
        // FIXME do we need to trim() here? isn't it enforced by the API
        this.fileName = fileName.trim();
    }

    /**
     *  @param buildResultsURL the build results URL.  This should be of the format:
     *  http://[SERVER]/cruisecontrol/buildresults/[OPTIONAL_PROJECT_NAME]
     */
    public void setBuildResultsURL(final String buildResultsURL) {
        this.buildresultsurl = buildResultsURL.trim();
    }


    /**
     *  @param channelLinkURL the channel link URL.  In many newsreaders, this is the URL linked
     *  from the feed title.
     */
    public void setChannelLinkURL(final String channelLinkURL) {
        this.channelLinkURL = channelLinkURL.trim();
    }

    /**
     *  @param max maximum number of entries to include in the RSS feed.  Default is 10.
     */
    public void setMaxLength(final int max) {
        if (max > 0) {
            this.maxLength = max;
        } else {
            LOG.warn("ignoring command to set maxRecords to invalud value (" + max + ");");
        }
    }
}
