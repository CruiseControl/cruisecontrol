package net.sourceforge.cruisecontrol;

import java.io.*;

/**
 * Inner class to hold the build information elements
 * which will be serialized and deseralized by the
 * MasterBuild process.
 */
public class BuildInfo implements Serializable {
    
    public static final String DEFAULT_BUILDINFO_FILENAME = "buildcycleinfo";

    private String  label;
    private String  lastGoodBuild;
    private String  lastBuild;

    private transient String  userList;
    private transient String logfile;
    private transient boolean lastBuildSuccessful = true;
    private transient boolean buildNotNecessary;
    
    /** Getter for property buildNotNecessary.
     * @return Value of property buildNotNecessary.
     */
    public boolean isBuildNotNecessary() {
        return buildNotNecessary;
    }
    
    /** Setter for property buildNotNecessary.
     * @param buildNotNecessary New value of property buildNotNecessary.
     */
    public void setBuildNotNecessary(boolean buildNotNecessary) {
        this.buildNotNecessary = buildNotNecessary;
    }
    
    /** Getter for property label.
     * @return Value of property label.
     */
    public String getLabel() {
        return label;
    }
    
    /** Setter for property label.
     * @param label New value of property label.
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * This method delegates to the dynamically loaded LabelIncrementer. The actual
     * implementing class can be declared in the masterbuild.properties file, or
     * the class DefaultLabelIncrementer will be used.
     *
     */
    public void incrementLabel(String labelIncrementClassName) {
        try {
            Class incrementerClass = Class.forName(labelIncrementClassName);
            LabelIncrementer incr = (LabelIncrementer)incrementerClass.newInstance();

            setLabel(incr.incrementLabel(getLabel()));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    
    /** Getter for property lastBuildAttemptTime.
     * @return Value of property lastBuildAttemptTime.
     */
    public String getLastBuild() {
        return lastBuild;
    }
    
    /** Setter for property lastBuildAttemptTime.
     * @param lastBuildAttemptTime New value of property lastBuildAttemptTime.
     */
    public void setLastBuild(String lastBuild) {
        if (!isCompleteTime(lastBuild)) {
            throw new IllegalArgumentException(
                "Bad format for last build: " + lastBuild);
        }
        this.lastBuild = lastBuild;
    }
    
    private boolean isCompleteTime(String time) {
        //???
        int expectedLength = 14;
        if (time.length() < expectedLength) {
            return false;
        }

        return true;
    }
    
    /** Getter for property lastBuildSuccessful.
     * @return Value of property lastBuildSuccessful.
     */
    public boolean isLastBuildSuccessful() {
        return lastBuildSuccessful;
    }
    
    /** Setter for property lastBuildSuccessful.
     * @param lastBuildSuccessful New value of property lastBuildSuccessful.
     */
    public void setLastBuildSuccessful(boolean lastBuildSuccessful) {
        this.lastBuildSuccessful = lastBuildSuccessful;
    }
    
    /** Getter for property lastGoodBuildTime.
     * @return Value of property lastGoodBuildTime.
     */
    public String getLastGoodBuild() {
        return lastGoodBuild;
    }
    
    /** Setter for property lastGoodBuildTime.
     * @param lastGoodBuildTime New value of property lastGoodBuildTime.
     */
    public void setLastGoodBuild(String lastGoodBuildTime) {
        this.lastGoodBuild = lastGoodBuildTime;
    }
    
    /** Getter for property userList.
     * @return Value of property userList.
     */
    public String getUserList() {
        return userList;
    }
    
    /** Setter for property userList.
     * @param userList New value of property userList.
     */
    public void setUserList(String userList) {
        this.userList = userList;
    }
    
    public String getLogfile() {
        return logfile;
    }
    
    public void setLogfile(String logfile) {
        this.logfile = logfile;
    }

    public void read() {
        read(DEFAULT_BUILDINFO_FILENAME);
    }

    public void read(String filename) {
        File infoFile = new File(filename);
        System.out.println("Reading build information from : " + infoFile.getAbsolutePath());
        if (!infoFile.exists() || !infoFile.canRead()) {
            System.out.println("Cannot read build information.");
            return;
        }

        try {
            ObjectInputStream s = new ObjectInputStream(new FileInputStream(infoFile));
            
            BuildInfo info = (BuildInfo) s.readObject();
            lastGoodBuild = info.lastGoodBuild;
            lastBuild = info.lastBuild;
            label = info.label;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void write() {
        write(DEFAULT_BUILDINFO_FILENAME);
    }

    public void write(String filename) {
        try {
            ObjectOutputStream s = new ObjectOutputStream(new FileOutputStream(filename));
            s.writeObject(this);
            s.flush();
            s.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean ready() {
        return (lastBuild != null) && (label != null);
    }
    
}
