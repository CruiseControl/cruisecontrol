package net.sourceforge.cruisecontrol.bootstrappers;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.*;

public class VssBootstrapperTest extends TestCase {

  public VssBootstrapperTest(String name) { super(name); }

  public void testValidate() {
    VssBootstrapper bootstrapper = new VssBootstrapper();
    try {
      bootstrapper.validate();
      fail("VssBootstrapper should throw exception if required attributes not set");
    }
    catch (CruiseControlException ex) {
      String message = ex.getMessage();
      assertEquals("exception message when required attributes not set", "VssBootstrapper has required attributes vssPath and filePath", message);
    }

    bootstrapper.setVssPath("$test/vss/path/file.ext");
    try {
      bootstrapper.validate();
      fail("VssBootstrapper should throw exception if required attributes not set");
    }
    catch (CruiseControlException ex) {
      String message = ex.getMessage();
      assertEquals("exception message when required attributes not set", "VssBootstrapper has required attributes vssPath and filePath", message);
    }

    bootstrapper.setLocalDirectory(".");
    try {
      bootstrapper.validate();
    }
    catch (CruiseControlException ex) {
      fail("validate() shouldn't fail when required attributes have been set");
    }

    bootstrapper.setLocalDirectory("c:/not/an/existing/directory");
    try {
      bootstrapper.validate();
      fail("validate() should fail when given a file path that doesn't exist");
    }
    catch (CruiseControlException ex) {
    }
  }

  public void testCommandLine() {
    VssBootstrapper bootstrapper = new VssBootstrapper();
    String commandLine = bootstrapper.generateCommandLine();
    this.assertNotNull("command line should never be null", commandLine);

    final String vssPath = "$Project/subproject/file.ext";
    bootstrapper.setVssPath(vssPath);
    final String localDirectory = "c:/foo";
    bootstrapper.setLocalDirectory(localDirectory);

    commandLine = bootstrapper.generateCommandLine();
    String expectedCommandLine = "ss.exe get \"" + vssPath + "\" -GL\"" + localDirectory + "\" -I-N";
    this.assertEquals(expectedCommandLine, commandLine);

    bootstrapper.setLogin("bob,password");
    commandLine = bootstrapper.generateCommandLine();
    expectedCommandLine = expectedCommandLine + " -Ybob,password";
    this.assertEquals(expectedCommandLine, commandLine);
  }
}