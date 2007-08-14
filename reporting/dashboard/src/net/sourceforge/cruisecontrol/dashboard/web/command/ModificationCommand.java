package net.sourceforge.cruisecontrol.dashboard.web.command;

import java.util.List;

import net.sourceforge.cruisecontrol.dashboard.Modification;
import net.sourceforge.cruisecontrol.dashboard.ModificationKey;
import net.sourceforge.cruisecontrol.dashboard.StoryTracker;

public class ModificationCommand {
    private Modification modification;

    private final StoryTracker storyTracker;

    public ModificationCommand(Modification modification, StoryTracker storyTracker) {
        this.storyTracker = storyTracker;
        this.modification = modification;
    }

    public String getComment() {
        if (storyTracker == null) {
            return modification.getComment();
        }
        return storyTracker.getTextWithUrls(modification.getComment());
    }

    public ModificationKey getModificationKey() {
        return modification.getModificationKey();
    }

    public List getModifiedFiles() {
        return modification.getModifiedFiles();
    }

    public String getType() {
        return modification.getType();
    }

    public String getUser() {
        return modification.getUser();
    }
}
