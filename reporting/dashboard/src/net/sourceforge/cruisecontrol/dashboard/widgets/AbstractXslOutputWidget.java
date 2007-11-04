/*******************************************************************************
 *  Copyright 2007 Ketan Padegaonkar http://ketan.padegaonkar.name
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 ******************************************************************************/
package net.sourceforge.cruisecontrol.dashboard.widgets;

import java.io.File;
import java.io.StringWriter;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * @author Ketan Padegaonkar
 */
public abstract class AbstractXslOutputWidget implements Widget {

    public AbstractXslOutputWidget() {
        super();
    }

    public Object getOutput(Map parameters) {
        File logFile = (File) parameters.get(Widget.PARAM_BUILD_LOG_FILE);
        try {
            File ccHome = (File) parameters.get(Widget.PARAM_CC_ROOT);
            File xsl = new File(ccHome.getCanonicalPath() + "/" + getXslPath());
            Source xmlSource = new StreamSource(logFile);
            Source xsltSource = new StreamSource(xsl);

            TransformerFactory transFact = TransformerFactory.newInstance();
            Transformer trans = transFact.newTransformer(xsltSource);

            StringWriter writer = new StringWriter();
            trans.transform(xmlSource, new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            return null;
        }
    }

    protected abstract String getXslPath();
}