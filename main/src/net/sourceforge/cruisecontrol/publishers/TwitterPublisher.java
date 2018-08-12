package net.sourceforge.cruisecontrol.publishers;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Publisher;
import static net.sourceforge.cruisecontrol.util.ValidationHelper.assertIsSet;
import net.sourceforge.cruisecontrol.util.XMLLogHelper;
import org.jdom2.Element;
import twitter4j.Twitter;
import twitter4j.TwitterException;

public class TwitterPublisher implements Publisher {
    private String username;
    private String password;
    private final TwitterProxy proxy;

    public TwitterPublisher() {
        this(new RealTwitterProxy());
    }

    TwitterPublisher(TwitterProxy proxy) {
        this.proxy = proxy;
    }

    public void publish(Element cruisecontrolLog) throws CruiseControlException {
        XMLLogHelper helper = new XMLLogHelper(cruisecontrolLog);
        proxy.twitter(helper.getProjectName() + " " + helper.getStatusMessage(), username, password);
    }

    public void validate() throws CruiseControlException {
        assertIsSet(username, "username", getClass());
        assertIsSet(password, "password", getClass());
    }

    public void setPassword(String s) {
        password = s;
    }

    public void setUsername(String s) {
        username = s;
    }

    private static class RealTwitterProxy implements TwitterProxy {
        public void twitter(String message, String userName, String password) throws CruiseControlException {
            Twitter twitter = new Twitter(userName, password);
            try {
                twitter.update(message);
            } catch (TwitterException e) {
                throw new CruiseControlException(e);
            }

        }
    }
}
