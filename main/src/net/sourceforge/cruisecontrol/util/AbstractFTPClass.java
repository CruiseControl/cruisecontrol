/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Vector;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTP;
import net.sourceforge.cruisecontrol.CruiseControlException;

/**
 * Generic class that acts as a parent to FTP related tasks to push files
 * out to a host.
 *
 * @author <a href="groboclown@users.sourceforge.net">Matt Albrecht</a>
 */
public abstract class AbstractFTPClass {

    private static final Logger LOG = Logger.getLogger(AbstractFTPClass.class);

    private String targetHost;
    private int targetPort = 21;
    private String targetUser = "anonymous";
    private String targetPasswd = "eat@joes.com";
    private String targetDir = ".";
    private String targetSeparator = "/";

    private boolean passive = false;


    public void setTargetUser(String targetUser) {
        this.targetUser = targetUser;
    }

    public void setTargetHost(String targetHost) {
        this.targetHost = targetHost;
    }

    public void setTargetPort(int targetPort) {
        this.targetPort = targetPort;
    }

    public void setTargetPasswd(String targetPasswd) {
        this.targetPasswd = targetPasswd;
    }

    public void setTargetDir(String targetDir) {
        this.targetDir = targetDir;
    }

    public void setTargetSeparator(String targetSeparator) {
        this.targetSeparator = targetSeparator;
    }

    public void setPassive(boolean p) {
        this.passive = p;
    }


    /**
     *  Called after the configuration is read to make sure that all the mandatory parameters
     *  were specified..
     *
     *  @throws net.sourceforge.cruisecontrol.CruiseControlException if there was a configuration error.
     */
    public void validate() throws CruiseControlException {
        if (targetHost == null) {
            throw new CruiseControlException("'targethost' not specified in configuration file");
        }
        if (targetDir == null) {
            targetDir = ".";
        }
    }


    protected FTPClient openFTP() throws CruiseControlException {
        LOG.info("Opening FTP connection to " + targetHost);

        FTPClient ftp = new FTPClient();

        try {
            ftp.connect(targetHost, targetPort);
            if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
                throw new CruiseControlException("FTP connection failed: "
                     + ftp.getReplyString());
            }

            LOG.info("logging in to FTP server");
            if (!ftp.login(targetUser, targetPasswd)) {
                throw new CruiseControlException("Could not login to FTP server");
            }
            LOG.info("login succeeded");

            if (passive) {
                setPassive(ftp);
            }
        } catch (IOException ioe) {
            LOG.error(ioe);
            throw new CruiseControlException(ioe.getMessage());
        }
        return ftp;
    }


    protected void closeFTP(FTPClient ftp) {
        if (ftp != null && ftp.isConnected()) {
            try {
                LOG.info("disconnecting");
                ftp.logout();
                ftp.disconnect();
            } catch (IOException ex) {
                // ignore it
            }
        }
    }


    protected void setBinary(FTPClient ftp) throws CruiseControlException {
        try {
            ftp.setFileType(FTP.IMAGE_FILE_TYPE);
            if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
                throw new CruiseControlException(
                    "could not set transfer type: "
                    + ftp.getReplyString());
            }
        } catch (IOException ex) {
            LOG.error(ex);
            throw new CruiseControlException(ex.getMessage());
        }
    }


    private void setPassive(FTPClient ftp) throws CruiseControlException {
        LOG.info("entering passive mode");
        ftp.enterLocalPassiveMode();
        if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
            throw new CruiseControlException("could not enter into passive "
                 + "mode: " + ftp.getReplyString());
        }
    }


    protected void makeDir(FTPClient ftp, String dir, boolean ignoreFailures)
            throws CruiseControlException {
        dir = targetDir + targetSeparator + dir;
        try {
            if (!ftp.makeDirectory(dir)) {
                // codes 521, 550 and 553 can be produced by FTP Servers
                //  to indicate that an attempt to create a directory has
                //  failed because the directory already exists.
                int rc = ftp.getReplyCode();
                if (!(ignoreFailures
                     && (rc == 550 || rc == 553 || rc == 521))) {
                    throw new CruiseControlException(
                        "could not create directory: "
                        + ftp.getReplyString());
                }
                LOG.info("directory already exists");
            } else {
                LOG.info("directory created OK");
            }
        } catch (IOException ex) {
            LOG.error(ex);
            throw new CruiseControlException(ex.getMessage());
        }
    }


    /**
     * The parent directories need to exist before putting this file.
     */
    protected void sendFile(FTPClient ftp, File infile, String outfilename)
            throws CruiseControlException {
        InputStream instream = null;
        try {
            LOG.info("transferring " + infile.getAbsolutePath());

            instream = new BufferedInputStream(new FileInputStream(infile));
            sendStream(ftp, instream, outfilename);
        } catch (IOException ioe) {
            throw new CruiseControlException(ioe.getMessage());
        } finally {
            IO.close(instream);
        }
    }


    /**
     * The parent directories need to exist before putting this file.
     */
    protected void sendStream(FTPClient ftp, InputStream instream,
            String outfilename) throws CruiseControlException {
        LOG.info("transferring to file " + outfilename);
        outfilename = targetDir + targetSeparator + resolveFile(outfilename);

        try {
            ftp.storeFile(outfilename, instream);
            boolean success = FTPReply.isPositiveCompletion(ftp.getReplyCode());

            if (!success) {
                throw new CruiseControlException("could not put file: "
                    + ftp.getReplyString());
            }
        } catch (IOException ex) {
            LOG.error(ex);
            throw new CruiseControlException(ex.getMessage());
        }
    }


    protected String resolveFile(String file) {
        return file.replace(File.separatorChar, targetSeparator.charAt(0));
    }


    protected void makeDirsForFile(FTPClient ftp, String filename,
            Vector knownPaths) throws CruiseControlException {
        String fname = resolveFile(filename);
        LOG.info("making dirs for file " + fname);
        int pos = fname.lastIndexOf(targetSeparator);
        if (pos > 0) {
            makeDirs(ftp, fname.substring(0, pos), knownPaths);
        }
    }


    /**
     * Creates all parent directories specified in a complete relative
     * pathname. Attempts to create existing directories will not cause
     * errors.
     */
    protected void makeDirs(FTPClient ftp, String pathname,
            Vector knownPaths) throws CruiseControlException {
        if (knownPaths == null) {
            knownPaths = new Vector();
        }

        StringTokenizer st = new StringTokenizer(targetDir + targetSeparator
            + resolveFile(pathname), targetSeparator, false);

        try {
            String cwd = ftp.printWorkingDirectory();
            LOG.info("makeDirs: current dir = " + cwd);
            String fullPath = targetDir;
            while (st.hasMoreTokens()) {
                String dir = st.nextToken();
                if (dir == null || dir.length() <= 0) {
                    continue;
                }
                fullPath += targetSeparator + dir;
                LOG.info("makeDirs: dir = " + dir + ", fullPath = "
                    + fullPath);
                /* we need to CD into the directory, whether it exists
                or not.
                if (knownPaths != null && knownPaths.contains(fullPath)) {
                    continue;
                }
                */
                if (!ftp.changeWorkingDirectory(dir)) {
                    LOG.info("makeDirs: could not CD into " + dir);
                    if (!ftp.makeDirectory(dir)) {
                        throw new CruiseControlException(
                            "could not create directory [" + dir + ", full="
                            + fullPath + "]: "
                            + ftp.getReplyString());
                    }
                    LOG.info("makeDirs: created dir " + dir);
                    if (!ftp.changeWorkingDirectory(dir)) {
                        throw new CruiseControlException(
                            "could not change to directory: "
                            + ftp.getReplyString());
                    }
                    LOG.info("makeDirs: CDed into " + dir);
                }
                knownPaths.addElement(fullPath);
            }
            ftp.changeWorkingDirectory(cwd);
        } catch (IOException ex) {
            LOG.error(ex);
            throw new CruiseControlException(ex.getMessage());
        }
    }

    /**
     * Sends the specified text into the specified path on the FTP server
     * @param text
     * @param path
     * @throws CruiseControlException
     */
    protected void sendFileToFTPPath(String text, String path) throws CruiseControlException {
        ByteArrayInputStream bais = new ByteArrayInputStream(
            text.getBytes());

        FTPClient ftp = openFTP();

        // we're sending text; don't set binary!

        try {
            makeDirsForFile(ftp, path, null);
            sendStream(ftp, bais, path);
        } finally {
            closeFTP(ftp);
        }
    }
}
