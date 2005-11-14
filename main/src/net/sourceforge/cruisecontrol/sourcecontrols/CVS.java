package net.sourceforge.cruisecontrol.sourcecontrols;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.SourceControl;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.OSEnvironment;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

/**
 * This class name is deprecated because of conflicts with specially named cvs folders, specifically within eclipse.
 * It's been deprecated and renamed to ConcurrentVersionsSystem in the same package.
 *
 * @see ConcurrentVersionsSystem
 * @deprecated Use ConcurrentVersionsSystem instead.
 */
public class CVS implements SourceControl {

    private ConcurrentVersionsSystem delegate = new ConcurrentVersionsSystem();

    /**
     * @see ConcurrentVersionsSystem
     * @deprecated Use ConcurrentVersionsSystem instead.
     */
    public List getModifications(Date lastBuild, Date now) {
        return delegate.getModifications(lastBuild, now);
    }

    /**
     * @see ConcurrentVersionsSystem
     * @deprecated Use ConcurrentVersionsSystem instead.
     */
    public void validate() throws CruiseControlException {
        delegate.validate();
    }

    /**
     * @see ConcurrentVersionsSystem
     * @deprecated Use ConcurrentVersionsSystem instead.
     */
    public Hashtable getProperties() {
        return delegate.getProperties();
    }

    /**
     * @see ConcurrentVersionsSystem
     * @deprecated Use ConcurrentVersionsSystem instead.
     */
    public void setCvsRoot(String cvsroot) {
        delegate.setCvsRoot(cvsroot);
    }

    /**
     * @see ConcurrentVersionsSystem
     * @deprecated Use ConcurrentVersionsSystem instead.
     */
    public void setLocalWorkingCopy(String local) {
        delegate.setLocalWorkingCopy(local);
    }

    /**
     * @see ConcurrentVersionsSystem
     * @deprecated Use ConcurrentVersionsSystem instead.
     */
    public void setTag(String tag) {
        delegate.setTag(tag);
    }

    /**
     * @see ConcurrentVersionsSystem
     * @deprecated Use ConcurrentVersionsSystem instead.
     */
    public void setModule(String module) {
        delegate.setModule(module);
    }

    /**
     * @see ConcurrentVersionsSystem
     * @deprecated Use ConcurrentVersionsSystem instead.
     */
    public void setProperty(String property) {
        delegate.setProperty(property);
    }

    /**
     * @see ConcurrentVersionsSystem
     * @deprecated Use ConcurrentVersionsSystem instead.
     */
    public void setPropertyOnDelete(String propertyOnDelete) {
        delegate.setPropertyOnDelete(propertyOnDelete);
    }

    /**
     * @see ConcurrentVersionsSystem
     * @deprecated Use ConcurrentVersionsSystem instead.
     */
    protected ConcurrentVersionsSystem.Version getCvsServerVersion() {
        return delegate.getCvsServerVersion();
    }

    /**
     * @see ConcurrentVersionsSystem
     * @deprecated Use ConcurrentVersionsSystem instead.
     */
    public boolean isCvsNewOutputFormat() {
        return delegate.isCvsNewOutputFormat();
    }

    /**
     * @see ConcurrentVersionsSystem
     * @deprecated Use ConcurrentVersionsSystem instead.
     */
    protected OSEnvironment getOSEnvironment() {
        return delegate.getOSEnvironment();
    }

    /**
     * @see ConcurrentVersionsSystem
     * @deprecated Use ConcurrentVersionsSystem instead.
     */
    void addAliasToMap(String line) {
        delegate.addAliasToMap(line);
    }

    /**
     * @see ConcurrentVersionsSystem
     * @deprecated Use ConcurrentVersionsSystem instead.
     */
    public Commandline buildHistoryCommand(Date lastBuildTime, Date checkTime) throws CruiseControlException {
        return delegate.buildHistoryCommand(lastBuildTime, checkTime);
    }

    /**
     * @see ConcurrentVersionsSystem
     * @deprecated Use ConcurrentVersionsSystem instead.
     */
    protected Commandline getCommandline() {
        return delegate.getCommandline();
    }

    /**
     * @see ConcurrentVersionsSystem
     * @deprecated Use ConcurrentVersionsSystem instead.
     */
    static String formatCVSDate(Date date) {
        return ConcurrentVersionsSystem.formatCVSDate(date);
    }

    /**
     * @see ConcurrentVersionsSystem
     * @deprecated Use ConcurrentVersionsSystem instead.
     */
    protected List parseStream(InputStream input) throws IOException {
        return delegate.parseStream(input);
    }

    /**
     * @see ConcurrentVersionsSystem
     * @deprecated Use ConcurrentVersionsSystem instead.
     */
    protected void setMailAliases(Hashtable mailAliases) {
        delegate.setMailAliases(mailAliases);
    }
}
