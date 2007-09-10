package net.sourceforge.cruisecontrol.distributed.core.jnlputil;

import junit.framework.TestCase;

import javax.jnlp.ServiceManager;
import javax.jnlp.BasicService;
import javax.jnlp.UnavailableServiceException;

/**
 * @author Dan Rollo
 * @version 1.0
 */

public final class JNLPServiceUtilTest extends TestCase {
    private static final String TEST_MUFFIN = "testMuffin";
    private static final String TEST_MUFFIN_VALUE = "yadda yadda c:\\lsd.kfjslasdfas asrea sdf";
    private static final String TEST_MUFFIN_VALUE2 = "rewritten value";


    protected final void tearDown() throws Exception {
        /** @todo Figure out how to unit test JWS */
        if (isJWSAvailable()) {
            JNLPServiceUtil.delete(TEST_MUFFIN);
        }
    }


    private static boolean isJWSAvailable() {
        try {
            // See if JWS is active first before doing our own service
            ServiceManager.lookup(BasicService.class.getName());
            return true;
        } catch (UnavailableServiceException ex) {
            return false;
        }
    }


    // Begin Test Methods

    /**
     * Save a muffin.
     */
    public static void testSaveLoadValue() {

        /** @todo Figure out how to unit test JWS */
        if (!isJWSAvailable()) {
            return;
        }

        // make sure muffin doesn't already exist
        String result = JNLPServiceUtil.load(TEST_MUFFIN);
        assertNull(result);

        JNLPServiceUtil.save(TEST_MUFFIN, TEST_MUFFIN_VALUE);
        result = JNLPServiceUtil.load(TEST_MUFFIN);
        assertEquals(TEST_MUFFIN_VALUE, result);

        // rewrite muffin
        JNLPServiceUtil.save(TEST_MUFFIN, TEST_MUFFIN_VALUE2);
        result = JNLPServiceUtil.load(TEST_MUFFIN);
        assertEquals(TEST_MUFFIN_VALUE2, result);

        // delete and create muffin
        JNLPServiceUtil.delete(TEST_MUFFIN);
        assertNull(result);

        JNLPServiceUtil.save(TEST_MUFFIN, TEST_MUFFIN_VALUE);
        result = JNLPServiceUtil.load(TEST_MUFFIN);
        assertEquals(TEST_MUFFIN_VALUE, result);

    }
}
