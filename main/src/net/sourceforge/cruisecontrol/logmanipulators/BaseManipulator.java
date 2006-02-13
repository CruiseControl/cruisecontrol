package net.sourceforge.cruisecontrol.logmanipulators;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Manipulator;
import net.sourceforge.cruisecontrol.Log;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

public abstract class BaseManipulator implements Manipulator {

    private static final Map UNITS;
    
    private transient Integer unit = null;
    private transient int every = -1;

    static {
        Map units = new HashMap(4, 1.0f);
        units.put("DAY", new Integer(Calendar.DAY_OF_MONTH));
        units.put("WEEK", new Integer(Calendar.WEEK_OF_YEAR));
        units.put("MONTH", new Integer(Calendar.MONTH));
        units.put("YEAR", new Integer(Calendar.YEAR));
        UNITS = Collections.unmodifiableMap(units);
    }

    public BaseManipulator() {
        super();
    }

    /**
     * Identifies the relevant Logfiles from the given Logdir
     * @param logDir the logDir as String
     * @return File-Array of the the relevant files.
     */
    protected File[] getRelevantFiles(String logDir, boolean ignoreSuffix) {
        File[] backupFiles = null;
        if (this.every != -1 && this.unit != null) {
            File dir = new File(logDir);
            Calendar cal = Calendar.getInstance();
            
            cal.add(unit.intValue(), -every);

            backupFiles = dir.listFiles(new LogfileNameFilter(cal.getTime(), ignoreSuffix));
        }
        return backupFiles;
    }

    public void validate() throws CruiseControlException {
        ValidationHelper.assertFalse(every == -1 || unit == null, 
                "BackupEvery and backupUnit must be set");
    }
    
    /**
     * sets the backup keep amount
     * 
     * @param every
     * @throws CruiseControlException
     */
    public void setEvery(int every) throws CruiseControlException {
        this.every = every;
    }

    /**
     * sets the unit on which the backup should run. valid are YEAR, MONTH, WEEK, DAY
     * 
     * @param unit String that is used as Key for the Calendar-Constants
     * @throws CruiseControlException
     */
    public void setUnit(String unit) throws CruiseControlException {
        this.unit = (Integer) UNITS.get(unit.toUpperCase());
    }

    Integer getUnit() {
        return this.unit;
    }

    private class LogfileNameFilter implements FilenameFilter {

        private Date logdate = null;
        
        private boolean ignoreSuffix = false;

        public LogfileNameFilter(Date logdate, boolean ignoreSuffix) {
            this.logdate = logdate;
            this.ignoreSuffix = ignoreSuffix;
        }

        public boolean accept(File dir, String name) {
            boolean result = true;
            result &= name.startsWith("log");
            if (!ignoreSuffix) {
                result &= name.endsWith(".xml");
            }
            if (result) {
                try {
                    Date logfileDate = Log.parseDateFromLogFileName(name);
                    result &= logfileDate.before(logdate);
                } catch (Exception e) {
                    result = false;
                }
            }
            return result;
        }

    }

}
