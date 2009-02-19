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
package net.sourceforge.cruisecontrol.publishers.rss;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import org.apache.log4j.Logger;

/**
 *  The feed class acts as a generic RSS Feed (there's no CruiseControl-specific
 *  functionality in this class).
 *
 *  Copyright (c) 2005 Hewlett-Packard Development Company, L.P.
 *  @author Patrick Conant
 */
public class Feed {

    private static final Logger LOG = Logger.getLogger(Feed.class);

    private String channelTitle;
    private String channelLink;
    private String channelDescription;
    private String channelLanguage = "en-US";

    private int maxLength = 20;
    private final List<Item> items = new ArrayList<Item>();

    /**
     *  Constructor
     *
     *  @param publishToFile the file to which the feed should be published.
     */
    public Feed(final File publishToFile) {

        // If we can read the file then load the RSSFeed settings from the existing file...
        if (publishToFile.exists() && publishToFile.canRead()) {
            try {

                final SAXBuilder builder = new SAXBuilder();
                final Document doc = builder.build(publishToFile);

                if (doc.getRootElement() != null
                    && doc.getRootElement().getChild(RSS.NODE_CHANNEL) != null) {

                    final Element channelElement = doc.getRootElement().getChild(RSS.NODE_CHANNEL);
                    if (channelElement.getChild(RSS.NODE_CHANNEL_TITLE) != null) {
                        this.channelTitle =
                            channelElement.getChild(RSS.NODE_CHANNEL_TITLE).getText().trim();
                    }
                    if (channelElement.getChild(RSS.NODE_CHANNEL_LINK) != null) {
                        this.channelLink =
                            channelElement.getChild(RSS.NODE_CHANNEL_LINK).getText().trim();
                    }
                    if (channelElement.getChild(RSS.NODE_CHANNEL_DESCRIPTION) != null) {
                        this.channelDescription =
                            channelElement.getChild(RSS.NODE_CHANNEL_DESCRIPTION).getText().trim();
                    }

                    if (channelElement.getChildren(RSS.NODE_ITEM) != null) {
                        final List itemNodes = channelElement.getChildren(RSS.NODE_ITEM);
                        for (final Object itemNode : itemNodes) {
                            this.items.add(new Item((Element) itemNode));
                        }
                    }
                } else {
                    LOG.info("existing RSS file doesn't appear to be valid.  Missnig root element or channel node.");
                }

                // Now sort the items from the old XML and remove enough to get to maxSize.
                Collections.sort(this.items);
                while (this.items.size() > this.maxLength) {
                    this.items.remove(this.items.size() - 1);
                }
            } catch (JDOMException jex) {
                LOG.error("jdom exception while parsing existing RSS file " + publishToFile.getPath()
                    + "; deleting file and starting over...", jex);
                publishToFile.delete();
            } catch (IOException ioe) {
                LOG.error("IOException while reading existing RSS file " + publishToFile.getPath()
                    + "; deleting file and starting over...", ioe);
                publishToFile.delete();
            }
        } else {
            LOG.info("Unable to locate or read the existing RSS feed file.");
        }
    }



    /**
     *  Set the title of the RSS feed.  The title will go into the /rss/title
     *  element in the RSS XML.
     *
     *  @param title the title of the RSS feed.
     */
    public void setTitle(final String title) {
        this.channelTitle = title;
    }

    /**
     *  Returns the title of the RSS feed.  The title goes into the /rss/title
     *  element in the RSS XML.
     *
     *  @return title the title of the RSS feed.
     */
    public String getTitle() {
        return this.channelTitle;
    }
    public void setLink(final String link) {
        this.channelLink = link;
    }
    public String getLink() {
        return this.channelLink;
    }
    public void setDescription(final String description) {
        this.channelDescription = description;
    }
    public String getDescription() {
        return this.channelDescription;
    }

    public void setMaxLength(final int max) {
        this.maxLength = max;
    }
    public int getMaxLength() {
        return this.maxLength;
    }

    public void addItem(final Item item) {
        synchronized (this.items) {
            if (this.items.size() == this.maxLength) {
                this.items.remove(this.items.size() - 1);
            }
            this.items.add(0, item);
        }
    }

    public List getItems() {
        return this.items;
    }

    public void write(final Writer wr) throws IOException {

        final BufferedWriter br = new BufferedWriter(wr);
        br.write("<?xml version=\"1.0\" ?>\n");
        br.write("<rss version=\"2.0\">\n");
        br.write("  <channel>\n");
        br.write("    <title>");
        if (this.getTitle() != null) {
            br.write(this.getTitle());
        }
        br.write("</title>\n");
        br.write("    <link>");
        if (this.getLink() != null) {
            br.write(this.getLink());
        }
        br.write("</link>\n");
        br.write("    <description>");
        if (this.getDescription() != null) {
            br.write(this.getDescription());
        }
        br.write("</description>\n");
        br.write("    <language>");
        br.write(this.channelLanguage);
        br.write("</language>\n");

        for (final Item item : this.items) {
            //write each item...
            if (item != null) {
                br.write(item.toXml());
            }
        }
        br.write("  </channel>\n");
        br.write("</rss>\n");
        br.flush();
    }
}
