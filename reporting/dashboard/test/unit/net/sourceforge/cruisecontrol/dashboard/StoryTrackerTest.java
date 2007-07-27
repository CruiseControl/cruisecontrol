package net.sourceforge.cruisecontrol.dashboard;

import junit.framework.TestCase;

public class StoryTrackerTest extends TestCase {
    public void testShouldReturnTrueWhenMessageContainsKeywords() {
        StoryTracker tracker = new StoryTracker("cce", "http://mingleurl/", "#");
        assertEquals("http://mingleurl/1", tracker.getStoryURL("#1"));
        assertEquals("", tracker.getStoryURL("#blabla"));
    }

    public void testShouldReturnTrueWhenMultipleKeywordsDefined() {
        StoryTracker tracker = new StoryTracker("cce", "http://mingleurl/", "#,Story,Defect");
        assertEquals("http://mingleurl/1", tracker.getStoryURL(" #1"));
        assertEquals("http://mingleurl/9", tracker.getStoryURL("I finished Story9"));
        assertEquals("http://mingleurl/1123", tracker.getStoryURL("Defect1123 is done"));
    }

}
