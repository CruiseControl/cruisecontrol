package net.sourceforge.cruisecontrol.dashboard.web.command;

import net.sourceforge.cruisecontrol.dashboard.Modification;
import net.sourceforge.cruisecontrol.dashboard.StoryTracker;
import junit.framework.TestCase;

public class ModificationCommandTest extends TestCase {

    public void testShouldreturnHyperLinkWhenCommandIsStoryTrackerSensitive() {
        ModificationCommand command =
                new ModificationCommand(new Modification("user", "type", "story1"), new StoryTracker("pj",
                        "http://mingle/story/", "story"));
        assertEquals("<a href=\"http://mingle/story/1\">story1</a>", command.getComment());
    }

    public void testShouldReturnCommentWhenStoryTrackerIsNull() {
        ModificationCommand command =
                new ModificationCommand(new Modification("user", "type", "story1"), null);
        assertEquals("story1", command.getComment());
    }
}
