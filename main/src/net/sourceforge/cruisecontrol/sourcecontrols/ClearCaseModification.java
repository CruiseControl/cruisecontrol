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

package net.sourceforge.cruisecontrol.sourcecontrols;

import org.apache.log4j.Logger;
import org.jdom.Element;

import java.text.DateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import net.sourceforge.cruisecontrol.Modification;

/**
 * data structure for holding data about a single modification
 * to a source control tool.
 *
 * @author <a href="mailto:alden@thoughtworks.com">alden almagro</a>
 */
public class ClearCaseModification extends Modification {
    private static final String TAGNAME_LABEL = "label";
    private static final String TAGNAME_ATTRIBUTE = "attribute";
    private static final String TAGNAME_ATTRIBUTE_NAME = "name";

    private static final Logger LOG = Logger.getLogger(Modification.class);

    public List labels = null;
    public Map attributes = null;

    public ClearCaseModification() {
        super("clearcase");
    }

    public Element toElement(DateFormat formatter) {
        Element modificationElement = super.toElement(formatter);

        if (labels != null) {
            for (Iterator it = labels.iterator(); it.hasNext(); ) {
                Element labelElement = new Element(TAGNAME_LABEL);
                labelElement.addContent((String) it.next());
                modificationElement.addContent(labelElement);
            }
        }

        if (attributes != null) {
            for (Iterator it = attributes.keySet().iterator(); it.hasNext(); ) {
                String attName = (String) it.next();
                String attValue = (String) attributes.get(attName);
                Element attElement = new Element(TAGNAME_ATTRIBUTE);
                attElement.setAttribute(TAGNAME_ATTRIBUTE_NAME, attName);
                attElement.addContent(attValue);
                modificationElement.addContent(attElement);
            }
        }

        return modificationElement;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer(super.toString());

        for (Iterator it = labels.iterator(); it.hasNext(); ) {
            sb.append("Tag: ").append(it.next()).append('\n');
        }

        for (Iterator it = attributes.keySet().iterator(); it.hasNext(); ) {
            String attName = (String) it.next();
            String attValue = (String) attributes.get(attName);
            sb.append("Attribute: ").append(attName).append(" = ").append(attValue).append('\n');
        }

        return sb.toString();
    }

    public void log(DateFormat formatter) {
        if (LOG.isDebugEnabled()) {
            super.log(formatter);

            if (labels != null) {
                for (Iterator it = labels.iterator(); it.hasNext(); ) {
                    LOG.debug("Tag: " + it.next());
                }
            }

            if (attributes != null) {
                for (Iterator it = attributes.keySet().iterator(); it.hasNext(); ) {
                    String attName = (String) it.next();
                    String attValue = (String) attributes.get(attName);
                    LOG.debug("Attribute: " + attName + " = " + attValue);
                }
            }

            LOG.debug("");
            LOG.debug("");
        }
    }

    public void fromElement(Element modification, DateFormat formatter) {
        super.fromElement(modification, formatter);

        List modLabels = modification.getChildren(TAGNAME_LABEL);
        if (modLabels != null && modLabels.size() > 0) {
            labels = new Vector();
            Iterator it = modLabels.iterator();
            while (it.hasNext()) {
                Element label = (Element) it.next();
                labels.add(label.getText());
            }
        }

        List modAttrs = modification.getChildren(TAGNAME_ATTRIBUTE);
        if (modAttrs != null && modAttrs.size() > 0) {
            attributes = new HashMap();
            Iterator it = modAttrs.iterator();
            while (it.hasNext()) {
                Element att = (Element) it.next();
                String attName = att.getAttributeValue(TAGNAME_ATTRIBUTE_NAME);
                String attValue = att.getText();
                attributes.put(attName, attValue);
            }
        }
    }
}
