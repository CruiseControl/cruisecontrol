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

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.Date;

import org.jdom.Element;

/**
 *  A generic RSS Feed Item (includes the contents of an RSS feed between the
 *  &lt:item&gt; and &lt;/item&gt; tags).
 *
 *  Copyright (c) 2005 Hewlett-Packard Development Company, L.P.
 *  @author Patrick Conant
 */
public class Item implements Comparable<Item> {

    private String title;
    private String link;
    private String description;
    private Date publishDate;
    private final SimpleDateFormat rssDateFormatter = new SimpleDateFormat(RSS.DATE_FORMAT);

    public Item() {
    }

    /**
     *  Construct from a JDOM element.
     * @param itemNode item node
     */
    public Item(Element itemNode) {
        if (itemNode.getChild(RSS.NODE_ITEM_TITLE) != null) {
            this.title = itemNode.getChild(RSS.NODE_ITEM_TITLE).getText();
        }
        if (itemNode.getChild(RSS.NODE_ITEM_LINK) != null) {
            this.link = itemNode.getChild(RSS.NODE_ITEM_LINK).getText();
        }
        if (itemNode.getChild(RSS.NODE_ITEM_DESCRIPTION) != null) {
            this.description = itemNode.getChild(RSS.NODE_ITEM_DESCRIPTION).getText();
        }
        if (itemNode.getChild(RSS.NODE_ITEM_PUBLISH_DATE) != null) {
           try {
                this.publishDate = rssDateFormatter.parse(itemNode.getChild(RSS.NODE_ITEM_PUBLISH_DATE).getText());
            } catch (ParseException pex) {
                // LOG?
                this.publishDate = new Date(0);
            }
        }
    }

    public int compareTo(Item other) {
        return other.getPublishDate().compareTo(this.getPublishDate());
    }

    public String getTitle() {
        return this.title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getDescription() {
        return this.description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getLink() {
        return this.link;
    }
    public void setLink(String link) {
        this.link = link;
    }
    public Date getPublishDate() {
        return this.publishDate;
    }
    public void setPublishDate(Date publishDate) {
        this.publishDate = publishDate;
    }

    public String toXml() {
        final StringBuilder output = new StringBuilder();

        output.append("    <");
        output.append(RSS.NODE_ITEM);
        output.append(">\n");

        //TITLE
        output.append("        <");
        output.append(RSS.NODE_ITEM_TITLE);
        output.append(">");
        if (this.getTitle() != null) {
            output.append(this.getTitle());
        }
        output.append("</");
        output.append(RSS.NODE_ITEM_TITLE);
        output.append(">\n");

        //LINK
        output.append("        <");
        output.append(RSS.NODE_ITEM_LINK);
        output.append(">");
        if (this.getLink() != null) {
            output.append(this.getLink());
        }
        output.append("</");
        output.append(RSS.NODE_ITEM_LINK);
        output.append(">\n");


        //DESCRIPTION
        output.append("        <");
        output.append(RSS.NODE_ITEM_DESCRIPTION);
        output.append("><![CDATA[");
        if (this.getDescription() != null) {
            output.append(this.getDescription());
        }
        output.append("]]></");
        output.append(RSS.NODE_ITEM_DESCRIPTION);
        output.append(">\n");

        //PUBLISH DATE
        if (this.getPublishDate() != null) {
            output.append("        <");
            output.append(RSS.NODE_ITEM_PUBLISH_DATE);
            output.append(">");
            output.append(rssDateFormatter.format(this.getPublishDate()));
            output.append("</");
            output.append(RSS.NODE_ITEM_PUBLISH_DATE);
            output.append(">\n");
        }


        //GUID & isPermaLink
        output.append("        <");
        output.append(RSS.NODE_ITEM_GUID);
        output.append(" ");
        output.append(RSS.ATTRIB_ITEM_IS_PERMA_LINK);
        output.append("=\"true\">");
        if (this.getLink() != null) {
            output.append(this.getLink());
        }
        output.append("</");
        output.append(RSS.NODE_ITEM_GUID);
        output.append(">\n");

        output.append("    </");
        output.append(RSS.NODE_ITEM);
        output.append(">\n");

        return output.toString();
    }
}