package net.sourceforge.cruisecontrol.bootstrappers;

import java.io.File;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.testutil.TestUtil.FilesToDelete;
import net.sourceforge.cruisecontrol.util.IO;

public class AntBootstrapperTest extends TestCase {
    
    private final FilesToDelete filesToDelete = new FilesToDelete();

    protected void tearDown() throws Exception {
        filesToDelete.delete();
    }
    
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
        
        File buildFile = File.createTempFile("testbuild", ".xml");
        writeBuildFile(buildFile);

        bootstrapper.setBuildFile(buildFile.getAbsolutePath());
        bootstrapper.setTempFile("notLog.xml");
        bootstrapper.setTarget("init");
        bootstrapper.validate();
        bootstrapper.bootstrap();
    }

    private void writeBuildFile(File buildFile) throws CruiseControlException {
        StringBuffer contents = new StringBuffer();
        contents.append("<project name='testbuild' default='init'>");
        contents.append("<target name='init'><echo message='called testbulid.xml init target'/></target>");
        contents.append("</project>");
        IO.write(buildFile, contents.toString());
    }
}
