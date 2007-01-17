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
package net.sourceforge.cruisecontrol.publishers;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Publisher;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import net.sourceforge.cruisecontrol.util.XMLLogHelper;
import net.sourceforge.cruisecontrol.util.IO;
import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.transform.JDOMSource;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Publisher Plugin which performs an xslt transform of the
 * jdom Element representation of the build log to an output file of choice.
 *
 * @author David Cole
 */
public class XSLTLogPublisher implements Publisher {
    private static final Logger LOG = Logger.getLogger(XSLTLogPublisher.class);

    private String directory;
    private String xsltFile;
    private boolean publishOnFail = true;
    private String outFileName;

    /**
     * Full path to the xslt file used in the transform
     *
     * @param fileName (required)
     */
    public void setXsltFile(String fileName) {
        this.xsltFile = fileName;
    }

    /**
     * Name of the output file where the transformed
     * log contents is to be written.
     * <br>
     * If omitted, a default will be provided matching the
     * build label name
     *
     * @param name
     */
    public void setOutFileName(String name) {
        this.outFileName = name;
    }

    /**
     * Directory where the transformed log will is to be written
     *
     * @param directory
     */
    public void setDirectory(String directory) {
        this.directory = directory;
    }

    /**
     * If true then publish the log contents even if the build failed.
     * If false then only publish the log if the build was successful
     * <br>
     * Defaults to true
     *
     * @param pof
     */
    public void setPublishOnFail(boolean pof) {
        this.publishOnFail = pof;
    }

    /**
     * Called after the configuration is read to make sure that all the mandatory parameters
     * were specified..
     *
     * @throws CruiseControlException if there was a configuration error.
     */
    public void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet(xsltFile, "xsltFile", this.getClass());
        ValidationHelper.assertIsSet(directory, "directory", this.getClass());
    }

    /**
     * Perform the log transform and publish the results.
     */
    public void publish(Element cruisecontrolLog) throws CruiseControlException {
        XMLLogHelper helper = new XMLLogHelper(cruisecontrolLog);
        Boolean buildSuccess = null;
        try {
            buildSuccess = (helper.isBuildSuccessful()) ? Boolean.TRUE : Boolean.FALSE;
        } catch (NullPointerException ne) {
            //Do Nothing - leave buildSuccess = null
        }

        //if the cruisecontrollog element or the buildSuccess attribute
        //turn out to be null, then there is nothing to do, so just return
        if (cruisecontrolLog == null || buildSuccess == null) {
            return;
        }

        //if the build failed and we are not supposed to publish on fail the return immediately
        if (!buildSuccess.booleanValue() && !publishOnFail) {
            LOG.info("Build failed and publishOnFail is false: Not publishing log.");
            return;
        }

        //If the outFileName attribute is null then construct the outFileName based
        //upon the build label that was created
        if (outFileName == null) {
            String label = helper.getCruiseControlInfoProperty("label");
            if (label == null || label.trim().length() == 0) {
                throw new CruiseControlException("The Label property is not set in the log file..."
                        + "unable to publish the log.");
            }
            LOG.debug(
                    "Using the cruise control info label property to construct the file name which is set to: "
                            + label);
            outFileName = label + ".log";
        }

        //Make sure that the directory exists and is a directory
        //Attempt to create an empty directory if necessary
        File dir = new File(directory);
        dir.mkdirs();
        if (!dir.isDirectory()) {
            throw new CruiseControlException(
                    "Unable to locate or create the output directory (" + directory + "): Failed to publish log file.");
        }

        String filePath = directory + File.separator + outFileName;
        LOG.info("Publishing log file to: " + filePath);
        writeFile(cruisecontrolLog, filePath);
        LOG.info("Log file successfully published to the file at path: " + filePath);
    }

    /**
     * Performs the transform of the cruisecontrolLog, writing the results to the locations
     * specified by the path parameter
     * @param cruisecontrolLog
     * @param path
     * @throws CruiseControlException
     */
    protected void writeFile(Element cruisecontrolLog, String path) throws CruiseControlException {
        FileInputStream xslFileStream = null;
        OutputStream out = null;
        try {
            //Make sure that the xsltFile exists
            try {
                xslFileStream = new FileInputStream(this.xsltFile);
            } catch (IOException ioe) {
                throw new CruiseControlException("Error reading the xsltFile: " + this.xsltFile, ioe);
            }

            //construct a FileWriter to the outputFile path location
            try {
                out = new FileOutputStream(path);
            } catch (IOException ioe) {
                throw new CruiseControlException("Unable to write to the file location: " + path);
            }

            //Prepare the transformer
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer(new StreamSource(xslFileStream));

            //cruisecontrolLog.get
            XMLLogHelper helper = new XMLLogHelper(cruisecontrolLog);
            String logFileName = helper.getLogFileName();
            LOG.info(
                    "Transforming the log file: " + logFileName + " to: " + path + " using the xslt: " + this.xsltFile);

            //perform the transform, writing out the results to the output location
            transformer.transform(new JDOMSource(cruisecontrolLog), new StreamResult(out));

        } catch (TransformerException te) {
            throw new CruiseControlException("An error occurred during the transformation process", te);
        } catch (Exception ioe) {
            throw new CruiseControlException("An unexpected exception occurred, unable to publish the log file.", ioe);
        } finally {
            IO.close(xslFileStream);
            IO.close(out);
        }
    }
}
