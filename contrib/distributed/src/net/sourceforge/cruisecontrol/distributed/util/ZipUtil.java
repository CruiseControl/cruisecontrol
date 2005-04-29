/****************************************************************************
* CruiseControl, a Continuous Integration Toolkit
* Copyright (c) 2001, ThoughtWorks, Inc.
* 651 W Washington Ave. Suite 600
* Chicago, IL 60661 USA
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
****************************************************************************/

package net.sourceforge.cruisecontrol.distributed.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;

public class ZipUtil {

    private static final Logger LOG = Logger.getLogger(ZipUtil.class);

    public static void zipFolderContents(String outFilename, String folderToZip) {
        validateParams(outFilename, folderToZip);
        BufferedOutputStream bos = null;
        ZipOutputStream zipOut = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(outFilename));
            zipOut = new ZipOutputStream(bos);
            File folder = new File(folderToZip);
            String message = "Zipping files to: " + outFilename;
            LOG.info(message);
            System.out.println(message);
            zipFiles(folder, folder, zipOut);
            message = "Finished zipping files";
            LOG.info(message);
            System.out.println(message);
        } catch (FileNotFoundException fnfe) {
            String message = "File not found while zipping files to: " + outFilename;
            LOG.error(message, fnfe);
            System.err.println(message + " - " + fnfe.getMessage());
            throw new RuntimeException(message, fnfe);
        } finally {
            try {
                // TODO: Need to do a flush here?
                // zipOut.flush();
                zipOut.close();
            } catch (ZipException ze) {
                File file = new File(outFilename);
                if ((file.length() == 0) && (file.exists())) {
                    String message = "Empty zip file created: " + outFilename;
                    LOG.debug(message);
                    System.out.println(message);
                    try {
                        // this is required in order to close the file stream if zip is empty
                        bos.close();
                    } catch (IOException e) {
                        LOG.error(e);
                    }
                    // @todo Should empty zips be created, or should we delete them here?
                    //*
                    // delete the empty zip file
                    if (!file.delete()) {
                        throw new RuntimeException("Error deleting empty zip file: "
                                + file.getAbsolutePath());
                    }
                    String message2 = "Deleted empty zip file: " + outFilename;
                    LOG.debug(message2);
                    System.out.println(message2);
                    ///*/
                }
            } catch (IOException ioe) {
                String message = "Error occured while closing zip file: " + outFilename;
                LOG.error(message, ioe);
                System.err.println(message);
                throw new RuntimeException(message, ioe);
            }
        }
    }

    private static void zipFiles(File rootDir, File folderToZip, ZipOutputStream zipOutputStream) {
        byte[] buf = new byte[1024];

        String relativePath = folderToZip.toString().substring(rootDir.toString().length());

        FileInputStream in = null;
        File[] files = folderToZip.listFiles();

        for (int i = 0; i < files.length; i++) {
            String filename = files[i].getName();
            if (files[i].isDirectory()) {
                String dirName = relativePath + File.separator + filename;
                LOG.debug("adding dir [" + dirName + "]");
                zipFiles(rootDir, files[i], zipOutputStream);
            } else {
                String filePath = relativePath + File.separator + filename;
                if (filePath.charAt(0) == File.separatorChar) {
                    filePath = filePath.substring(1);
                }
                LOG.debug("adding file [" + filePath + "]");
                try {
                    in = new FileInputStream(new File(folderToZip, filename));
                    zipOutputStream.putNextEntry(new ZipEntry(filePath.replace(File.separatorChar, '/')));
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        zipOutputStream.write(buf, 0, len);
                    }
                    zipOutputStream.closeEntry();
                    in.close();
                } catch (IOException ioe) {
                    String message = "Error occured while zipping file " + filePath;
                    LOG.error(message, ioe);
                    System.err.println(message + " - " + ioe.getMessage());
                    throw new RuntimeException(message, ioe);
                }
            }
        }
    }

    private static void validateParams(String outFilename, String folderToZip) {
        if (outFilename == null) {
            String message = "Missing output zip file name";
            LOG.error(message);
            throw new IllegalArgumentException(message);
        }
        if (new File(outFilename).isDirectory()) {
            String message = "Output file already exists as directory";
            LOG.error(message);
            throw new IllegalArgumentException(message);
        }
        if (folderToZip == null) {
            String message = "Missing folder to zip";
            LOG.error(message);
            throw new IllegalArgumentException(message);
        }
        if (!(new File(folderToZip).isDirectory())) {
            String message = "Target folder to zip does not exist or is not a directory";
            LOG.error(message);
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * @param zipFilePath
     * @param toDirName
     * @throws IOException
     */
    public static void unzipFileToLocation(String zipFilePath, String toDirName) throws IOException {
        ZipFile zipFile;
        Enumeration enumr;
        boolean isEmptyFile = false;

        try {
            zipFile = new ZipFile(zipFilePath);
            if (zipFile.size() == 0) {
                isEmptyFile = true;
            } else {
	            String infoMessage = "Unzipping file: " + zipFilePath;
	            LOG.info(infoMessage);
	            System.out.println(infoMessage);
	
	            enumr = zipFile.entries();
	            while (enumr.hasMoreElements()) {
	                ZipEntry target = (ZipEntry) enumr.nextElement();
	                String message = "Exploding: " + target.getName();
	                LOG.debug(message);
	                System.out.println(message);
	                saveItem(zipFile, toDirName, target);
	            }
	            zipFile.close();
            }
        } catch (FileNotFoundException fnfe) {
            String message = "Could not find zip file" + zipFilePath;
            LOG.error(message, fnfe);
            System.err.println(message + " - " + fnfe.getMessage());
            throw new RuntimeException(message, fnfe);
        } catch (ZipException ze) {
            String message = "Zip error occured while unzipping file " + zipFilePath;
            LOG.error(message, ze);
            System.err.println(message + " - " + ze.getMessage());
            throw new RuntimeException(message, ze);
        } catch (IOException ioe) {
            String message = "Error occured while unzipping file " + zipFilePath;
            LOG.error(message, ioe);
            System.err.println(message + " - " + ioe.getMessage());
            throw new RuntimeException(message, ioe);
        }
        
        if (isEmptyFile) {
            String message="Zip file has no entries: " + zipFilePath;
            LOG.warn(message);
            System.err.println(message);
            throw new IOException(message);
        }

        String infoMessage = "Unzip complete";
        LOG.info(infoMessage);
        System.out.println(infoMessage);
    }

    private static void saveItem(ZipFile zipFile, String rootDirName, ZipEntry entry) throws ZipException, IOException {
        InputStream is = null;
        BufferedInputStream inStream = null;
        FileOutputStream outStream = null;
        BufferedOutputStream bufferedOutStream = null;
        try {
            File file = new File(rootDirName, entry.getName());
            if (entry.isDirectory()) {
                file.mkdirs();
            } else {
                is = zipFile.getInputStream(entry);
                inStream = new BufferedInputStream(is);
                File dir = new File(file.getParent());
                dir.mkdirs();
                outStream = new FileOutputStream(file);
                bufferedOutStream = new BufferedOutputStream(outStream);

                int c;
                while ((c = inStream.read()) != -1) {
                    bufferedOutStream.write((byte) c);
                }
            }
        } catch (ZipException ze) {
            String message = "Zip error unzipping entry: " + entry.getName();
            LOG.error(message, ze);
            System.err.println(message + " - " + ze.getMessage());
            throw new RuntimeException(message, ze);
        } catch (IOException ioe) {
            String message = "I/O error unzipping entry: " + entry.getName();
            LOG.error(message, ioe);
            System.err.println(message + " - " + ioe.getMessage());
            throw new RuntimeException(message, ioe);
        } finally {
            bufferedOutStream.close();
            inStream.close();
        }
    }
}
