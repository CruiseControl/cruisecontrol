package net.sourceforge.cruisecontrol.util;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.cruisecontrol.CruiseControlException;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * This class provides helper methods for interacting with Input/Output classes.
 */
public final class IO {

    private IO() {
        //Helper methods only.
    }

    public static void close(final OutputStream o) {
        if (o != null) {
            try {
                o.close();
            } catch (IOException ignored) {
                // nevermind, then
            }
        }
    }

    public static void close(final InputStream i) {
        if (i != null) {
            try {
                i.close();
            } catch (IOException ignored) {
            }
        }
    }

    public static void close(final Reader r) {
        if (r != null) {
            try {
                r.close();
            } catch (IOException ignored) {
                //Never mind
            }
        }
    }

    public static void close(final Writer w) {
        if (w != null) {
            try {
                w.close();
            } catch (IOException ignored) {
                //Never mind
            }
        }
    }

    public static void close(final Process p) {
        try {
            close(p.getInputStream());
            close(p.getOutputStream());
            close(p.getErrorStream());
        } finally {
            if (p != null) {
                p.destroy();
            }
        }
    }

    public static void output(final File to, final Element xml, final String encoding) throws CruiseControlException {

        try {
            final Format format = Format.getPrettyFormat();
            if (encoding != null) {
                format.setEncoding(encoding);
            }
            final XMLOutputter outputter = new XMLOutputter(format);
            final OutputStream logStream = new BufferedOutputStream(new FileOutputStream(to));
            try {
                outputter.output(new Document(xml), logStream);
            } finally {
                close(logStream);
            }
        } catch (IOException e) {
            throw new CruiseControlException(e);
        }
    }

    /**
     * Deletes a File instance. If the file represents a directory, all
     * the subdirectories and files within.
     * @param f the file (or directory) to delete
     */
    public static void delete(final File f) {
        if (f == null || !f.exists()) {
            return;
        }
        if (f.isDirectory()) {
            deleteDir(f);
            return;
        }
        f.delete();
    }

    private static void deleteDir(final File dir) {
        final File[] children = dir.listFiles();
        for (final File child : children) {
            delete(child);
        }
        dir.delete();
    }

    public static void delete(final File f, final boolean debuggerOn, final Logger log) {
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
     * @param f the file to read.
     * @param out the strem to dump the file content to.
     */
    public static void dumpTo(final File f, final PrintStream out) {

        try {
            final BufferedReader in = new BufferedReader(new FileReader(f));
            try {
                while (in.ready()) {
                    out.println(in.readLine());
                }
            } finally {
                close(in);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Write the content to the file.
     * @param fileName file name to create
     * @param content to write to the file
     * @throws CruiseControlException if something breaks
     */
    public static void write(final String fileName, final String content) throws CruiseControlException {
        write(new File(fileName), content);
    }

    /**
     * Write the content to the file.
     * @param f to create
     * @param contents to write to the file
     * @throws CruiseControlException if something breaks
     */
    public static void write(final File f, final String contents) throws CruiseControlException {

        try {
            if (f.getParentFile() != null) {
                f.getParentFile().mkdirs();
            }
            final FileWriter fw = new FileWriter(f);
            try {
                fw.write(contents);
            } finally {
                close(fw);
            }
        } catch (IOException ioe) {
            throw new CruiseControlException("Error writing file: " + f.getAbsolutePath(), ioe);
        }
    }

    /**
     * @param source the input file to read
     * @return List of lines of text (String objects)
     * @throws CruiseControlException if something breaks
     */
    public static List<String> readLines(final File source)
        throws CruiseControlException {

        final List<String> result = new ArrayList<String>();
        try {
            final BufferedReader reader = new BufferedReader(new FileReader(source));
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    result.add(line);
                }
            } finally {
                close(reader);
            }
        } catch (IOException ioe) {
            throw new CruiseControlException("Error reading file: " + source.getAbsolutePath(), ioe);
        }

        return result;
    }

    /**
     * Reads all the text from an InputStream.
     * @param stream The stream to read from.
     * @return The full text read from the stream
     * @throws IOException if an IO error occurs
     */
    public static String readText(final InputStream stream) throws IOException {
        if (stream == null) {
            throw new IOException("No stream provided");
        } else {
            final InputStreamReader reader = new InputStreamReader(stream);
            final StringBuilder textBuilder = new StringBuilder();
            
            final char[] buffer = new char[1000];
            int charsRead;
            while ((charsRead = reader.read(buffer)) > 0) {
               textBuilder.append(buffer, 0, charsRead);
            }
            
            return textBuilder.toString();
        }
    }
}
