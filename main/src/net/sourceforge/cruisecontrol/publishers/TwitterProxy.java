package net.sourceforge.cruisecontrol.publishers;

import net.sourceforge.cruisecontrol.CruiseControlException;

public interface TwitterProxy {
  void twitter(String message, String userName, String password) throws CruiseControlException;
}
