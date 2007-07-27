package net.sourceforge.cruisecontrol.dashboard.web.command;

import java.util.List;

import org.apache.commons.lang.StringUtils;

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
        String url = storyTracker.getStoryURL(modification.getComment());
        if (StringUtils.isEmpty(url)) {
            return modification.getComment();
        } else {
            return "<a href=\"" + url + "\">" + modification.getComment() + "</a>";
        }
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
