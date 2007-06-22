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
package net.sourceforge.cruisecontrol.publishers.email;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import net.sourceforge.cruisecontrol.CruiseControlException;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

/**
 * Open the Maven POM file, retrieve the information about the developers
 * and wrap them into a property set.
 * @author Gisbert Amm
 *
 */
public class MavenMapperHelper {
    private final String projectFile;
    private Properties props;
    private List developerNodes;
    private String [] mandatoryElements;
    private static final String [] MANDATORY_ELEMENTS_DEFAULT = {"id", "email"};
    private static final String X_PATH_EXPRESSION = "/project//developers//developer";

    public MavenMapperHelper(String projectFile) throws CruiseControlException {
        this.projectFile = projectFile;
        this.props = new Properties();
        this.setMandatoryElements(MANDATORY_ELEMENTS_DEFAULT);
        this.setDeveloperNodes();
        this.setDeveloperPropertySet();
    }

    public Properties getDeveloperPropertySet() {
        return this.props;
    }

    public void setMandatoryElements(String [] mandatoryElementsDefault) {
        this.mandatoryElements = mandatoryElementsDefault;
    }

    private void setDeveloperNodes() throws CruiseControlException {
        Document doc;
        SAXBuilder builder = new SAXBuilder(false);
        try {
            doc = builder.build(new FileInputStream(this.projectFile));
            this.developerNodes = XPath.selectNodes(doc, X_PATH_EXPRESSION);
        } catch (IOException io) {
          throw new CruiseControlException("Cannot open Maven POM " + this.projectFile
                  + ": " + io.getLocalizedMessage());
        } catch (JDOMException jde) {
            throw new CruiseControlException(this.projectFile + "causes XML-Problems: "
                    + jde.getLocalizedMessage());
        }
    }

    private void setDeveloperPropertySet() throws CruiseControlException {
        // create a Properties instance out fo the Maven configuration
        for (int i = 0; i < this.developerNodes.size(); i++) {
           /* the entry in Maven POM looks like this:
            * <developer>
            *   <id/>
            *   <name/>
            *   <email/>
            *   <url/>
            *   <organization/>
            *   <organizationUrl/>
            *   <roles/>
            *   <timezone/>
            *   <properties/>
            * </developer>
           */
           Element developerEntry = (Element) this.developerNodes.get(i);
           // Make sure we get the information about every developer we rely on.

           for (int j = 0; j < this.mandatoryElements.length; j++) {
               Element mandatoryElement = developerEntry.getChild(this.mandatoryElements[j]);
               if (mandatoryElement == null) {
                   throw new CruiseControlException("MavenMapper: Missing <"
                           + this.mandatoryElements[j] + "> child element on "
                           + developerEntry.toString() + " in " + projectFile);
               }
           }
           /* Get all child elements of <developer> and wrap them into a property set.
            * The property set then looks like this:
            * {
            * bar.name=Scholar Foofoo
            * foo.email=foo@barbaz
            * foo.name=Master Foo
            * bar.id=foofoo
            * foo.timezone=1
            * bar.organization=Foo Buildmasters Inc.
            * bar.timezone=3
            * foo.id=foo
            * bar.email=foofoo@barbaz
            * }
            */
           List children = developerEntry.getChildren();
           String developerId = developerEntry.getChild("id").getText();
           for (int k = 0; k < children.size(); k++) {
               Element nextNode = (Element) children.get(k);
               this.props.setProperty(developerId + "."
                       + nextNode.getName(), nextNode.getValue());
           }
        }
    }
}