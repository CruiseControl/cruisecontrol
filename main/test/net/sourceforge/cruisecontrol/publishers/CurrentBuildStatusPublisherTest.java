package net.sourceforge.cruisecontrol.publishers;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import org.jdom.Element;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class CurrentBuildStatusPublisherTest extends TestCase {

    public CurrentBuildStatusPublisherTest(String name) {
        super(name);
    }

    public void testPublish() {
        Element buildLog = new Element("cruisecontrol");
        CurrentBuildStatusPublisher cbsp = new CurrentBuildStatusPublisher();
        try {
            cbsp.publish(buildLog);
            assertTrue("'file' should be a required attribute on CurrentBuildStatusPublisher", false);
        } catch (CruiseControlException cce) {
        }

    }

    public void testWriteFile() {
        CurrentBuildStatusPublisher cbsb = new CurrentBuildStatusPublisher();
        cbsb.setFile("_testCurrentBuildStatus.txt");
        Date date = new Date();

        try {
            cbsb.writeFile(date, 300);
            SimpleDateFormat formatter = new SimpleDateFormat("MMM/dd/yyyy HH:mm");
            String expected = "<br>&nbsp;<br><b>Next Build Starts At:</b><br>" + formatter.format(new Date(date.getTime() + (300 * 1000)));
            assertEquals(expected, readFileToString("_testCurrentBuildStatus.txt"));

            cbsb.setDateFormat("dd/MMM/yyyy");
            cbsb.writeFile(date, 800);
            formatter = new SimpleDateFormat("dd/MMM/yyyy");
            String expected2 = "<br>&nbsp;<br><b>Next Build Starts At:</b><br>" + formatter.format(new Date(date.getTime() + (800 * 1000)));
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
