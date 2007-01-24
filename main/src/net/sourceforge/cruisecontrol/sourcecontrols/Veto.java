package net.sourceforge.cruisecontrol.sourcecontrols;

import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.SourceControl;

public class Veto implements SourceControl {

    private Triggers triggers;
    private BuildStatus buildStatus;

    public List getModifications(Date lastBuild, Date now) {
        
        List triggerMods = triggers.getModifications(lastBuild, now);
        if (triggerMods.isEmpty()) {
            return Collections.EMPTY_LIST;
        }
        
        List buildStatusMods = buildStatus.getModifications(lastBuild, now);
        if (buildStatusMods.isEmpty()) {
            throw new OutOfDateException("trigger changes with no buildstatus changes");
        }
        
        Modification latestBuildStatusMod = getLatestModification(buildStatusMods);
        Modification latestTriggerMod = getLatestModification(triggerMods);
        
        if (latestBuildStatusMod.modifiedTime.before(latestTriggerMod.modifiedTime)) {
            throw new OutOfDateException("buildstatus out of date compared to trigger changes");
        }
        
        return Collections.EMPTY_LIST;
    }

    private Modification getLatestModification(List mods) {
        Modification latest = null;
        for (Iterator iter = mods.iterator(); iter.hasNext();) {
            Modification mod = (Modification) iter.next();
            if (latest == null || mod.modifiedTime.after(latest.modifiedTime)) {
                latest = mod;
            }
        }
        return latest;
    }

    public Map getProperties() {
        return Collections.EMPTY_MAP;
    }

    public void validate() throws CruiseControlException {
        if (triggers == null) {
            throw new CruiseControlException("veto requires nested triggers element");
        }
        triggers.validate();
        if (buildStatus == null) {
            throw new CruiseControlException("veto requires a nested buildstatus element");
        }
        buildStatus.validate();
    }

    public Triggers createTriggers() throws CruiseControlException {
        if (triggers != null) {
            throw new CruiseControlException("only one nested triggers allowed");
        }
        triggers = new Triggers(this);
        return triggers;
    }

    public BuildStatus createBuildStatus() throws CruiseControlException {
        if (buildStatus != null) {
            throw new CruiseControlException("only one nested buildstatus allowed");
        }
        buildStatus = getBuildStatus();
        return buildStatus;
    }

    protected BuildStatus getBuildStatus() {
        return new BuildStatus();
    }

    private class OutOfDateException extends RuntimeException {

        public OutOfDateException(String string) {
            super(string);
        }
    }
    
}
