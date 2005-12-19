package net.sourceforge.cruisecontrol.publishers.sfee;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Publisher;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

public abstract class SfeePublisher implements Publisher {
    private String url;
    private String username;
    private String password;

    public String getServerURL() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setServerURL(String url) {
        this.url = url;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public final void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet(url, "serverurl", this.getClass());
        ValidationHelper.assertIsSet(username, "username", this.getClass());
        ValidationHelper.assertIsSet(password, "password", this.getClass());

        subValidate();
    }

    public abstract void subValidate() throws CruiseControlException;
}
