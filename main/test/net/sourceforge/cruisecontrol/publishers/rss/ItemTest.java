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

import java.util.Date;
import java.io.StringReader;
import java.text.SimpleDateFormat;

import org.jdom.Element;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import junit.framework.TestCase;

/*
 * Copyright (c) 2005 Hewlett-Packard Development Company, L.P.
 * Licensed under the CruiseControl BSD license
 */
public class ItemTest extends TestCase {

    private Element itemElement;

    public void setUp() throws Exception {

        itemElement = new Element(RSS.NODE_ITEM);

        Element titleElement = new Element(RSS.NODE_ITEM_TITLE);
        titleElement.addContent("title");
        itemElement.addContent(titleElement);

        Element linkElement = new Element(RSS.NODE_ITEM_LINK);
        linkElement.addContent("link");
        itemElement.addContent(linkElement);

        Element descriptionElement = new Element(RSS.NODE_ITEM_DESCRIPTION);
        descriptionElement.addContent("description");
        itemElement.addContent(descriptionElement);

        Element dateElement = new Element(RSS.NODE_ITEM_PUBLISH_DATE);
        dateElement.addContent(new SimpleDateFormat(RSS.DATE_FORMAT).format(new Date()));
        itemElement.addContent(dateElement);
    }

    public void testConstructors() throws Exception {
        Item item = new Item();
        assertNull("default constructor should have null title.", item.getTitle());
        assertNull("default constructor should have null description.", item.getDescription());
        assertNull("default constructor should have null link.", item.getLink());
        assertNull("default constructor should have null publish date.", item.getPublishDate());


        item = new Item(itemElement);
        assertEquals("title", item.getTitle());
        assertEquals("link", item.getLink());
        assertEquals("description", item.getDescription());
        assertEquals(
            new SimpleDateFormat(RSS.DATE_FORMAT).parse(itemElement.getChild(RSS.NODE_ITEM_PUBLISH_DATE).getText()),
            item.getPublishDate());
    }

    public void testToXml() throws Exception {

        Item item = new Item(itemElement);
        String output = item.toXml();

        //ensure it's valid XML...
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(new StringReader(output));

        Element root = doc.getRootElement();
        assertEquals(root.getChild(RSS.NODE_ITEM_TITLE).getText(), "title");
        assertEquals(root.getChild(RSS.NODE_ITEM_LINK).getText(), "link");
        assertEquals(root.getChild(RSS.NODE_ITEM_DESCRIPTION).getText(), "description");
        assertEquals(
            root.getChild(RSS.NODE_ITEM_PUBLISH_DATE).getText(),
            itemElement.getChild(RSS.NODE_ITEM_PUBLISH_DATE).getText());
    }
}