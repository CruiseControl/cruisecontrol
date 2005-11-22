package net.sourceforge.cruisecontrol.bootstrappers;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;

public class AntBootstrapperTest extends TestCase {
    public void testValidate() {
        AntBootstrapper bootstrapper = new AntBootstrapper();

        try {
            bootstrapper.validate();
        } catch (CruiseControlException e) {
            fail("antbuilder has no required attributes");
        }

        bootstrapper.setBuildFile("buildfile");
        bootstrapper.setTarget("target");

        try {
            bootstrapper.validate();
        } catch (CruiseControlException e) {
            fail("validate should not throw exceptions when options are set.");
        }

        bootstrapper.setSaveLogDir("I/hope/this/dir/does/not/exist/");
        try {
            bootstrapper.validate();
            fail("validate should throw exceptions when saveLogDir doesn't exist");
        } catch (CruiseControlException e) {
        }
    }

    public void testBootstrap() throws Exception {
        AntBootstrapper bootstrapper = new AntBootstrapper();

        bootstrapper.setBuildFile("testbuild.xml");
        bootstrapper.setTempFile("notLog.xml");
        bootstrapper.setTarget("init");
        bootstrapper.bootstrap();
    }
}
