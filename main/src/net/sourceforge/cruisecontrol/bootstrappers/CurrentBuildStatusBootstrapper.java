package net.sourceforge.cruisecontrol.bootstrappers;

import net.sourceforge.cruisecontrol.Bootstrapper;
import net.sourceforge.cruisecontrol.CruiseControlException;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CurrentBuildStatusBootstrapper implements Bootstrapper {

    private String _fileName;
    private String _dateFormat = "MMM/dd/yyyy HH:mm";

    public void setFile(String fileName) {
        _fileName = fileName;
    }

    public void setDateFormat(String dateFormat) {
        _dateFormat = dateFormat;
    }

    public void bootstrap() throws CruiseControlException {
        if (_fileName == null) {
            throw new CruiseControlException("'filename' is required for CurrentBuildStatusBootstrapper");
        }
        writeFile(new Date());
    }

    protected void writeFile(Date date) throws CruiseControlException {
        SimpleDateFormat formatter = new SimpleDateFormat(_dateFormat);
        StringBuffer sb = new StringBuffer();
        sb.append("<br>&nbsp;<br><b>Current Build Started At:</b><br>");
        sb.append(formatter.format(date));
        FileWriter fw = null;
        try {
            fw = new FileWriter(_fileName);
            fw.write(sb.toString());
            fw.close();
        } catch (IOException ioe) {
            throw new CruiseControlException("Error Writing File: " + _fileName);
        } finally {
            fw = null;
        }
    }
}
