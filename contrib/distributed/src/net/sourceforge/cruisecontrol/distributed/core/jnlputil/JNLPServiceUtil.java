package net.sourceforge.cruisecontrol.distributed.core.jnlputil;

import org.apache.log4j.Logger;

import javax.jnlp.PersistenceService;
import javax.jnlp.ServiceManager;
import javax.jnlp.UnavailableServiceException;
import javax.jnlp.BasicService;
import javax.jnlp.FileContents;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.OutputStream;

/**
 * Provides features to read/store settings in webstart environment.
 * @author Dan Rollo
 * Date: Sep 9, 2007
 * Time: 10:39:19 PM
 */
public final class JNLPServiceUtil {

    // @todo Add installer support for logging via Log4J
    private static final Logger LOG = Logger.getLogger(JNLPServiceUtil.class);

    // @todo Add installer support for logging via Log4J
    private static void log(final String msg) {
        System.out.println(msg);
        LOG.info(msg);
    }


    private static final String CLASS_NAME_BASIC_SERVICE =
            javax.jnlp.BasicService.class.getName();

    private static final String CLASS_NAME_PERSISTENCE_SERVICE =
            javax.jnlp.PersistenceService.class.getName();

    private static BasicService basicService;
    private static PersistenceService persistenceService;


    /** Private constructor for utility class. */
    private JNLPServiceUtil() {
    }


    private static BasicService getBasicService() {
        if (basicService == null) {
            try {
                basicService = (BasicService) ServiceManager.lookup(CLASS_NAME_BASIC_SERVICE);
            } catch (UnavailableServiceException e) {
                throw new JNLPServiceException("Error getting Basic Service class: "
                        + CLASS_NAME_BASIC_SERVICE, e);
            }
        }

        return basicService;
    }


    private static PersistenceService getPersistenceService() {
        if (persistenceService == null) {
            try {
                persistenceService = (PersistenceService) ServiceManager.lookup(CLASS_NAME_PERSISTENCE_SERVICE);
            } catch (UnavailableServiceException e) {
                throw new JNLPServiceException("Error getting Persistence Service class: "
                        + CLASS_NAME_PERSISTENCE_SERVICE, e);
            }
        }

        return persistenceService;
    }


    private static URL getMuffinURL(final String muffinName) {
        final URL codebase = getBasicService().getCodeBase();
        return getMuffinURL(codebase, muffinName);
    }
    private static URL getMuffinURL(final URL codebase, final String muffinName) {
        final URL muffinURL;
        try {
            muffinURL = new URL((codebase.toString() + muffinName));
        } catch (MalformedURLException e) {
            throw new JNLPServiceException(muffinName, e);
        }

        return muffinURL;
    }


    public static void delete(final String muffinName) {

        final URL muffinURL = getMuffinURL(muffinName);

        delete(muffinURL);
    }
    private static void delete(final URL muffinURL) {

        try {
            getPersistenceService().delete(muffinURL);
        } catch (IOException e) {
            throw new JNLPServiceException(muffinURL.toString(), e);
        }
        log("Muffin deleted: " + muffinURL);
    }


    private static FileContents getMuffinFileContents(final URL muffinURL) {

        final FileContents fc;
        try {
            fc = getPersistenceService().get(muffinURL);
        } catch (FileNotFoundException e) {
            throw new JNLPServiceException(muffinURL.toString(), e);
        } catch (IOException e) {
            throw new JNLPServiceException(muffinURL.toString(), e);
        }

        return fc;
    }

    public static String load(final String muffinName) {

        final URL muffinURL = getMuffinURL(muffinName);

        final String result;
        try {
            // read value from muffin
            final BufferedReader in = new BufferedReader(new InputStreamReader(
                    getMuffinFileContents(muffinURL).getInputStream()
            ));

            try {
                result = in.readLine();
            } finally {
                in.close();
            }
        } catch (FileNotFoundException e) {
            throw new JNLPServiceException(muffinName, e);
        } catch (IOException e) {
            throw new JNLPServiceException(muffinName, e);
        }

        return result;
    }



    private static void createMuffin(final URL muffinURL, final long maxSize) {

        // @todo Ignore create error until a better way is found.
        try {
            delete(muffinURL);
        } catch (Exception e) {
            // @todo Ignore create error until a better way is found.
        }

//        long actualMaxSize;
//        final boolean hadCreationException = false;
        try {
//            actualMaxSize =
                    getPersistenceService().create(muffinURL, maxSize);
            log("Muffin created: " + muffinURL);
        } catch (IOException ioe) {
            // @todo Ignore create error until a better way is found.
            //throw new JWSServiceException(muffinURL.toString(), ioe);
            //hadCreationException = true;
            log("Muffin create failed: " + muffinURL + "; ioe: " + ioe);            
        }

        // @todo Ignore create error until a better way is found.
        //if ((!hadCreationException) && (actualMaxSize != maxSize)) {
//        if (actualMaxSize != maxSize) {
//            throw new JWSServiceException(
//                      "MaxSize error. requested: " + maxSize
//                      + "; received: " + actualMaxSize,
//                      null);
//        }
    }

    /**
     * Deletes the muffin if it already exists, create a new muffin
     * and returns an OutputStream for writing to the new muffin.
     * @param muffinName the name to be appended to the muffin url (codebase)
     * @param maxSize the size required to store the muffin value
     * @return an OutputStream for writing to the new muffin
     * @throws IOException if something breaks
     */
    private static OutputStream getMuffinOutputStream(final String muffinName, final long maxSize)
                  throws IOException {

        final URL muffinURL = getMuffinURL(muffinName);

        // if muffin exists, delete it
//        if (isMuffinDefined(muffinURL)) {
//            delete(muffinURL);
//        }

        createMuffin(muffinURL, maxSize);

        return getMuffinFileContents(muffinURL).getOutputStream(false);
    }

    public static void save(final String muffinName, final String muffinValue) {

        log("saving muffin: " + muffinName + ", value: " + muffinValue);

        final int maxSize = muffinValue.length();

        BufferedWriter out = null;
        try {
            // write value to muffin
            out = new BufferedWriter(new OutputStreamWriter(getMuffinOutputStream(muffinName, maxSize)));

            out.write(muffinValue);
            out.flush();
        } catch (FileNotFoundException fnfe) {
            throw new JNLPServiceException(muffinName + "; " + muffinValue, fnfe);
        } catch (IOException ioe) {
            throw new JNLPServiceException(muffinName + "; " + muffinValue, ioe);
        } finally {
            if (out != null) {
                try { out.close(); } catch (IOException ioe) {
                    // ignore
                }
            }
        }
    }

    
    public static final class JNLPServiceException extends RuntimeException {
        private JNLPServiceException(final String msg, final Throwable cause) {
            super(msg, cause);
        }
    }
}
