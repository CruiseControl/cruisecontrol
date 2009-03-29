package net.sourceforge.cruisecontrol.publishers.sfee;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import junit.framework.Assert;

public class SfeeTestUtils {
    private Object inMemorySfee;
    private boolean notUsingInMemory;

    void loadSfeeInMemory(String serverUrl, String username, String password) {
        // Instantiate the in-memory stub implementation of SFEE using reflection so that
        // this class will still compile and run when the REAL implementation of SFEE is used.
        try {
            final Class< ? > inMemSfeeFactoryClass = Class.forName("com.vasoftware.sf.InMemorySfeeFactory");
            final Method resetMethod = inMemSfeeFactoryClass.getMethod("reset");
            resetMethod.invoke(null, (Object[]) null);
            final Method createMethod = inMemSfeeFactoryClass.getMethod("create",
                    String.class, String.class, String.class);
            inMemorySfee = createMethod.invoke(null,
                    serverUrl, username, password);
        } catch (NoSuchMethodException e) {
            Assert.fail("Must be using the wrong version of the sfee soap stubs.");
        } catch (IllegalAccessException e) {
            Assert.fail("Must be using the wrong version of the sfee soap stubs.");
        } catch (InvocationTargetException e) {
            Assert.fail("Must be using the wrong version of the sfee soap stubs.");
        } catch (ClassNotFoundException e) {
            // Must be running with the real SFEE implementation, which does NOT contain
            // the InMemorySfeeFactory class. So, we can ignore this
            // exception and go on with the test.
            notUsingInMemory = true;
        }
    }

    void addProject(String projectName) {
        if (notUsingInMemory) {
            return;
        }

        try {
            Method addProjectMethod = inMemorySfee.getClass().getMethod("addProject", String.class);
            addProjectMethod.invoke(inMemorySfee, projectName);
        } catch (NoSuchMethodException e) {
            Assert.fail("Must be using the wrong version of the sfee soap stubs.");
        } catch (IllegalAccessException e) {
            Assert.fail("Must be using the wrong version of the sfee soap stubs.");
        } catch (InvocationTargetException e) {
            Assert.fail("Must be using the wrong version of the sfee soap stubs.");
        }
    }

    void addTracker(String trackerName, String projectName) {
        if (notUsingInMemory) {
            return;
        }

        try {
            Method addTracker = inMemorySfee.getClass().getMethod("addTracker",
                    String.class, String.class);
            addTracker.invoke(inMemorySfee, trackerName, projectName);
        } catch (NoSuchMethodException e) {
            Assert.fail("Must be using the wrong version of the sfee soap stubs.");
        } catch (IllegalAccessException e) {
            Assert.fail("Must be using the wrong version of the sfee soap stubs.");
        } catch (InvocationTargetException e) {
            Assert.fail("Must be using the wrong version of the sfee soap stubs.");
        }
    }

    void createFolders(String projectName, String path) {
        if (notUsingInMemory) {
            return;
        }

        try {
            Method createFolders = inMemorySfee.getClass().getMethod("createFolders",
                    String.class, String.class);
            createFolders.invoke(inMemorySfee, projectName, path);
        } catch (NoSuchMethodException e) {
            Assert.fail("Must be using the wrong version of the sfee soap stubs.");
        } catch (IllegalAccessException e) {
            Assert.fail("Must be using the wrong version of the sfee soap stubs.");
        } catch (InvocationTargetException e) {
            Assert.fail("Must be using the wrong version of the sfee soap stubs.");
        }
    }
}
