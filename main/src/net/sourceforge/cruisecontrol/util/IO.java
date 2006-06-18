package net.sourceforge.cruisecontrol.util;

import org.jdom.Element;
import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.OutputStream;
import java.io.IOException;
import java.io.File;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.Iterator;

import net.sourceforge.cruisecontrol.CruiseControlException;

/**
 * This class provides helper methods for interacting with Input/Output classes.
 */
public final class IO {

    private IO() {
        //Helper methods only.
    }

    public static void close(OutputStream o) {
        if (o != null) {
            try {
                o.close();
            } catch (IOException e) {
                // nevermind, then
            }
        }
    }

    public static void output(File to, Element xml, String encoding) throws CruiseControlException {
        OutputStream logStream = null;
        try {
            Format format = Format.getPrettyFormat();
            if (encoding != null) {
                format.setEncoding(encoding);
            }
            XMLOutputter outputter = new XMLOutputter(format);
            logStream = new BufferedOutputStream(new FileOutputStream(to));
            outputter.output(new Document(xml), logStream);
        } catch (IOException e) {
            throw new CruiseControlException(e);
        } finally {
            close(logStream);
        }
    }

    /**
     * Deletes a File instance. If the file represents a directory, all
     * the subdirectories and files within.
     */
    public static void deleteFile(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            deleteDir(file);
            return;
        }
        file.delete();
    }

    private static void deleteDir(File dir) {
        File[] children = dir.listFiles();
        for (int i = 0; i < children.length; i++) {
            File child = children[i];
            deleteFile(child);
        }
        dir.delete();
    }

    public static void delete(Collection files) {
        for (Iterator iterator = files.iterator(); iterator.hasNext();) {
            deleteFile((File) iterator.next());
        }
    }
}
