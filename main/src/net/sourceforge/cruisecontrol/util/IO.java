package net.sourceforge.cruisecontrol.util;

import net.sourceforge.cruisecontrol.CruiseControlException;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.Iterator;

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
            } catch (IOException ignored) {
                // nevermind, then
            }
        }
    }

    public static void close(InputStream i) {
        if (i != null) {
            try {
                i.close();
            } catch (IOException ignored) {
            }
        }
    }

    public static void close(Reader r) {
        if (r != null) {
            try {
                r.close();
            } catch (IOException ignored) {
                //Never mind
            }
        }
    }

    public static void close(Writer w) {
        if (w != null) {
            try {
                w.close();
            } catch (IOException ignored) {
                //Never mind
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
    public static void delete(File f) {
        if (f == null || !f.exists()) {
            return;
        }
        if (f.isDirectory()) {
            deleteDir(f);
            return;
        }
        f.delete();
    }

    private static void deleteDir(File dir) {
        File[] children = dir.listFiles();
        for (int i = 0; i < children.length; i++) {
            File child = children[i];
            delete(child);
        }
        dir.delete();
    }

    public static void delete(Collection files) {
        for (Iterator iterator = files.iterator(); iterator.hasNext();) {
            delete((File) iterator.next());
        }
    }

    public static void delete(File f, boolean debuggerOn, Logger log) {
        try {
            delete(f);
            if (debuggerOn) {
                log.info("Removed temp file " + f.getAbsolutePath());
            }
        } catch (Exception ignored) {
            //never mind
        }
    }

    /**
     * Writes the contents of a file to a PrintStream.
     */
    public static void dumpTo(File f, PrintStream out) {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(f));
            while (in.ready()) {
                out.println(in.readLine());
            }
        } catch (Exception ignored) {
        } finally {
            close(in);
        }

    }
}
