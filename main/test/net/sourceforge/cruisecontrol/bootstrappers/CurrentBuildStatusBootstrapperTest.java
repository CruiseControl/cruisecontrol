package net.sourceforge.cruisecontrol.bootstrappers;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.text.SimpleDateFormat;

public class CurrentBuildStatusBootstrapperTest extends TestCase {

    public CurrentBuildStatusBootstrapperTest(String name) {
        super(name);
    }


    public void testBootstrap() {
        CurrentBuildStatusBootstrapper cbsb = new CurrentBuildStatusBootstrapper();
        try {
            cbsb.bootstrap();
            assertTrue("'file' should be a required attribute on CurrentBuildStatusBootstrapper", false);
        } catch (CruiseControlException cce) {
        }
    }

    public void testWriteFile() {
        CurrentBuildStatusBootstrapper cbsb = new CurrentBuildStatusBootstrapper();
        cbsb.setFile("_testCurrentBuildStatus.txt");
        Date date = new Date();

        try {
            cbsb.writeFile(date);
            SimpleDateFormat formatter = new SimpleDateFormat("MMM/dd/yyyy HH:mm");
            String expected = "<br>&nbsp;<br><b>Current Build Started At:</b><br>" + formatter.format(date);
            assertEquals(expected, readFileToString("_testCurrentBuildStatus.txt"));

            cbsb.setDateFormat("dd/MMM/yyyy");
            cbsb.writeFile(date);
            formatter = new SimpleDateFormat("dd/MMM/yyyy");
            String expected2 = "<br>&nbsp;<br><b>Current Build Started At:</b><br>" + formatter.format(date);
            assertEquals(expected2, readFileToString("_testCurrentBuildStatus.txt"));
        } catch (CruiseControlException cce2) {
            cce2.printStackTrace();
        }
    }

    private String readFileToString(String fileName) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(fileName));
            String s = br.readLine();
            StringBuffer sb = new StringBuffer();
            while (s != null) {
                sb.append(s);
                s = br.readLine();
            }
            br.close();
            return sb.toString();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            br = null;
        }
        return "";
    }
}
