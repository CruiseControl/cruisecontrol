package net.sourceforge.cruisecontrol.sourcecontrols;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.SourceControl;

public class Veto implements SourceControl {
    /**
     * enable logging for this class
     */
    private static final Logger LOG = Logger.getLogger(Veto.class);

    private Triggers triggers;

    private BuildStatus buildStatus;

    public List<Modification> getModifications(final Date lastBuild, final Date now) {

        final List<Modification> triggerMods = triggers.getModifications(lastBuild, now);
        if (triggerMods.isEmpty()) {
            return Collections.emptyList();
        }

        final List<Modification> buildStatusMods = buildStatus.getModifications(lastBuild, now);
        if (buildStatusMods.isEmpty()) {
            throw new VetoException("trigger changes with no buildstatus changes");
        }

        final Modification latestBuildStatusMod = getLatestModification(buildStatusMods);

        if (!getNewerModifications(triggerMods, latestBuildStatusMod).isEmpty()) {
            throw new VetoException("buildstatus out of date compared to trigger changes");
        }

        return Collections.emptyList();
    }

    private Modification getLatestModification(final List<Modification> mods) {
        Modification latest = null;
        for (final Modification mod : mods) {
            if (latest == null || mod.modifiedTime.after(latest.modifiedTime)) {
                latest = mod;
            }
        }
        return latest;
    }

    private List getNewerModifications(final List<Modification> mods, final Modification buildStatusMod) {
        List<Modification> newerMods = new ArrayList<Modification>();
        LOG.debug("Comparing all trigger mods against buildStatusMod with date [" + buildStatusMod.modifiedTime + "]");
        for (final Modification mod : mods) {
            if (mod.modifiedTime.after(buildStatusMod.modifiedTime)) {
                newerMods.add(mod);
                LOG.debug("Newer file : " + mod.getFullPath() + " at [" + mod.modifiedTime + "]");
            }
        }
        if (!newerMods.isEmpty()) {
            LOG.info("Found " + newerMods.size() + " modifications since last build status.");
        }
        return newerMods;
    }

    public Map<String, String> getProperties() {
        return Collections.emptyMap();
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

}
