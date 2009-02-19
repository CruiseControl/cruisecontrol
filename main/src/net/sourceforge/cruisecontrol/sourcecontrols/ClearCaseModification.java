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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import net.sourceforge.cruisecontrol.Modification;

import org.apache.log4j.Logger;
import org.jdom.Element;

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

    public List<String> labels;
    public Map<String, String> attributes = null;

    public ClearCaseModification() {
        super("clearcase");
    }

    public Element toElement() {
        final Element modificationElement = super.toElement();

        if (labels != null) {
            for (final String label : labels) {
                final Element labelElement = new Element(TAGNAME_LABEL);
                labelElement.addContent(label);
                modificationElement.addContent(labelElement);
            }
        }

        if (attributes != null) {
            for (final String attName : attributes.keySet()) {
                final String attValue = attributes.get(attName);
                final Element attElement = new Element(TAGNAME_ATTRIBUTE);
                attElement.setAttribute(TAGNAME_ATTRIBUTE_NAME, attName);
                attElement.addContent(attValue);
                modificationElement.addContent(attElement);
            }
        }

        return modificationElement;
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder(super.toString());

        for (final String label : labels) {
            sb.append("Tag: ").append(label).append('\n');
        }

        for (final String attName : attributes.keySet()) {
            final String attValue = attributes.get(attName);
            sb.append("Attribute: ").append(attName).append(" = ").append(attValue).append('\n');
        }

        return sb.toString();
    }

    public void log() {
        if (LOG.isDebugEnabled()) {
            super.log();

            if (labels != null) {
                for (final String label : labels) {
                    LOG.debug("Tag: " + label);
                }
            }

            if (attributes != null) {
                for (final String attName : attributes.keySet()) {
                    final String attValue = attributes.get(attName);
                    LOG.debug("Attribute: " + attName + " = " + attValue);
                }
            }

            LOG.debug("");
            LOG.debug("");
        }
    }

    public void fromElement(final Element modification) {
        super.fromElement(modification);

        final List modLabels = modification.getChildren(TAGNAME_LABEL);
        if (modLabels != null && modLabels.size() > 0) {
            labels = new ArrayList<String>();
            for (final Object modLabel : modLabels) {
                final Element label = (Element) modLabel;
                labels.add(label.getText());
            }
        }

        final List modAttrs = modification.getChildren(TAGNAME_ATTRIBUTE);
        if (modAttrs != null && modAttrs.size() > 0) {
            attributes = new HashMap<String, String>();
            for (final Object modAttr : modAttrs) {
                final Element att = (Element) modAttr;
                final String attName = att.getAttributeValue(TAGNAME_ATTRIBUTE_NAME);
                final String attValue = att.getText();
                attributes.put(attName, attValue);
            }
        }
    }
}
