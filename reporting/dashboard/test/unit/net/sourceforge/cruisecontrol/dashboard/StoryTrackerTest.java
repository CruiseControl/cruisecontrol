package net.sourceforge.cruisecontrol.dashboard;

import junit.framework.TestCase;

public class StoryTrackerTest extends TestCase {
    public void testShouldReturnTrueWhenMessageContainsKeywords() {
        StoryTracker tracker = new StoryTracker("cce", "http://mingleurl/", "#");
        assertEquals("<a href=\"http://mingleurl/1\">#1</a>", tracker.getTextWithUrls("#1"));
        assertEquals("#blabla", tracker.getTextWithUrls("#blabla"));
    }

    public void testShouldReturnTrueWhenMultipleKeywordsDefined() {
        StoryTracker tracker = new StoryTracker("cce", "http://mingleurl/", "#,Story,Defect");
        assertEquals(" <a href=\"http://mingleurl/1\">#1</a>", tracker.getTextWithUrls(" #1"));
        assertEquals("I finished <a href=\"http://mingleurl/9\">Story9</a>", tracker
                .getTextWithUrls("I finished Story9"));
        assertEquals("<a href=\"http://mingleurl/1123\">Defect1123</a> is done", tracker
                .getTextWithUrls("Defect1123 is done"));
    }

    public void testShouldSupportTabOrSpacesBetweenKeywordAndNumber() throws Exception {
        StoryTracker tracker = new StoryTracker("cce", "http://mingleurl/", "#,Story,Defect");
        assertEquals(" <a href=\"http://mingleurl/1\"># 1</a>", tracker.getTextWithUrls(" # 1"));
        assertEquals("I finished <a href=\"http://mingleurl/9\">Story 9</a>", tracker
                .getTextWithUrls("I finished Story 9"));
        assertEquals("<a href=\"http://mingleurl/1123\">Defect   1123</a> is done", tracker
                .getTextWithUrls("Defect   1123 is done"));
    }

    public void testShouldSupportMultiLinks() throws Exception {
        StoryTracker tracker = new StoryTracker("cce", "http://mingleurl/", "#,Story,Defect");
        String input = "aaa# 12bbbStory 43 ccccc";
        String expected =
                "aaa<a href=\"http://mingleurl/12\"># 12</a>bbb<a href=\"http://mingleurl/43\">Story 43</a> ccccc";
        assertEquals(expected, tracker.getTextWithUrls(input));
    }

    public void testShouldReturnOriginalStringIfNOTMatched() throws Exception {
        StoryTracker tracker = new StoryTracker("cce", "http://mingleurl/", "#,Story,Defect");
        String input = "not.macthed";
        String expected = input;
        assertEquals(expected, tracker.getTextWithUrls(input));
    }
}
