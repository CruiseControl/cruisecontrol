package net.sourceforge.cruisecontrol.publishers;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Publisher;
import org.jdom.DataConversionException;
import org.jdom.Element;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CurrentBuildStatusPublisher implements Publisher {

    private String _fileName;
    private String _dateFormat = "MMM/dd/yyyy HH:mm";

    public void setFile(String fileName) {
        _fileName = fileName;
    }

    public void setDateFormat(String dateFormat) {
        _dateFormat = dateFormat;
    }

    public void publish(Element cruisecontrolLog) throws CruiseControlException {
        if (_fileName == null) {
            throw new CruiseControlException("'filename' is required for CurrentBuildStatusBootstrapper");
        }
        try {
            long interval = cruisecontrolLog.getChild("interval").getAttribute("seconds").getLongValue();
            writeFile(new Date(), interval);
        } catch (DataConversionException dce) {
            throw new CruiseControlException("");
        }
    }

    protected void writeFile(Date date, long interval) throws CruiseControlException {
        SimpleDateFormat formatter = new SimpleDateFormat(_dateFormat);
        Date datePlusInterval = new Date(date.getTime() + (interval * 1000));
        StringBuffer sb = new StringBuffer();
        sb.append("<br>&nbsp;<br><b>Next Build Starts At:</b><br>");
        sb.append(formatter.format(datePlusInterval));
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
