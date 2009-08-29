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

import net.sourceforge.cruisecontrol.dashboard.exception.ConfigurationException;

import org.apache.log4j.Logger;

/**
 * @author Ketan Padegaonkar
 */
public abstract class AbstractXslOutputWidget implements Widget {
    private static final Logger LOGGER = Logger.getLogger(AbstractXslOutputWidget.class);

    public AbstractXslOutputWidget() {
        super();
    }

    public Object getOutput(Map parameters) {
        File logFile = new File(parameters.get(Widget.PARAM_BUILD_LOG_FILE).toString());
        File xslFile = new File(parameters.get(Widget.PARAM_WEBAPP_ROOT) + "/" + getXslPath());
        try {
            if (!xslFile.exists()) {
                throw new ConfigurationException("Unable to find file "
                        + xslFile.getAbsolutePath());
            }
            Source xsltSource = new StreamSource(xslFile);
            Source xmlSource = new StreamSource(logFile);

            TransformerFactory transFact = TransformerFactory.newInstance();
            Transformer trans = transFact.newTransformer(xsltSource);

            StringWriter writer = new StringWriter();
            trans.transform(xmlSource, new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            LOGGER.error("Failed to transform log file " + logFile
                    + " using xsl " + getXslPath(), e);
            return null;
        }
    }

    protected abstract String getXslPath();
}
