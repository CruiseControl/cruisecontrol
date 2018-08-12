package net.sourceforge.cruisecontrol.publishers;

import static junit.framework.Assert.fail;
import static junit.framework.Assert.assertEquals;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.testutil.TestUtil;
import org.junit.Test;
import org.jdom2.Element;


public class TwitterPublisherTest {
    @Test
    public void validateShouldNotThrowExceptionWhenUsernameAndPasswordAreSet() throws CruiseControlException {
        TwitterPublisher twitter = new TwitterPublisher();
        twitter.setUsername("username");
        twitter.setPassword("password");
        twitter.validate();
    }

    @Test
    public void validateExceptionShouldIndicateMissingUsername() {
        TwitterPublisher twitter = new TwitterPublisher();
        twitter.setPassword("arbitrary");
        String expected = "'username' is required for TwitterPublisher";
        assertValidateFails(twitter, expected);
    }

    private void assertValidateFails(TwitterPublisher twitter, String expected) {
        try {
            twitter.validate();
            fail();
        } catch (CruiseControlException e) {
            assertEquals(expected, e.getMessage());
        }
    }

    @Test
    public void validateExceptionShouldIndicateMissingPassword() {
        TwitterPublisher twitter = new TwitterPublisher();
        twitter.setUsername("arbitrary");
        assertValidateFails(twitter, "'password' is required for TwitterPublisher");
    }

    @Test
    public void messageShouldBeBuildFailure() throws CruiseControlException {
        TestTwitterProxy proxy = new TestTwitterProxy();
        TwitterPublisher twitter = new TwitterPublisher(proxy);
        Element failed = TestUtil.createFailedBuild();
        twitter.publish(failed);
        assertEquals("someproject failed", proxy.status);
    }

    @Test
    public void messageShouldBePassedWhenBuildPasses() throws CruiseControlException {
        TestTwitterProxy proxy = new TestTwitterProxy();
        TwitterPublisher twitter = new TwitterPublisher(proxy);
        Element passed = TestUtil.createPassingBuild();
        twitter.publish(passed);
        assertEquals("someproject successful", proxy.status);
    }

    @Test
    public void messageShouldBeFixedWhenBuildIsFixed() throws CruiseControlException {
        TestTwitterProxy proxy = new TestTwitterProxy();
        TwitterPublisher twitter = new TwitterPublisher(proxy);
        Element fixed = TestUtil.createFixedBuild();
        twitter.publish(fixed);
        assertEquals("someproject fixed", proxy.status);
    }

    @Test
    public void messageShouldIncludeProjectName() throws CruiseControlException {
        TestTwitterProxy proxy = new TestTwitterProxy();
        TwitterPublisher twitter = new TwitterPublisher(proxy);
        Element passed = TestUtil.createPassingBuild();
        String projectName = "myProject" + System.currentTimeMillis();
        setProjectName(passed, projectName);
        twitter.publish(passed);
        assertEquals(projectName + " successful", proxy.status);
    }

    private void setProjectName(Element passed, String s) {
        passed.removeChild("info");
        Element info = new Element("info");
        TestUtil.addProperty(info, "projectname", s);
        TestUtil.addProperty(info, "lastbuildsuccessful", "true");
        passed.addContent(info);
    }

    @Test
    public void usernameShouldBeSetOnProxy() throws CruiseControlException {
        TestTwitterProxy proxy = new TestTwitterProxy();
        TwitterPublisher twitter = new TwitterPublisher(proxy);
        twitter.setUsername("user");
        Element log = TestUtil.createPassingBuild();
        twitter.publish(log);
        assertEquals("user", proxy.username);
    }

    @Test
    public void passwordShouldBeSetOnProxy() throws CruiseControlException {
        TestTwitterProxy proxy = new TestTwitterProxy();
        TwitterPublisher twitter = new TwitterPublisher(proxy);
        twitter.setPassword("pass");
        Element log = TestUtil.createPassingBuild();
        twitter.publish(log);
        assertEquals("pass", proxy.password);
    }


    private class TestTwitterProxy implements TwitterProxy {
        public String status;
        public String username;
        public String password;

        public void twitter(String message, String username, String password) {
            status = message;
            this.username = username;
            this.password = password;
        }
    }
}
