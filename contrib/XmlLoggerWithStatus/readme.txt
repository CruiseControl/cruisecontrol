Our build takes around 25 minutes to complete, this is why it is
sometimes important to see what exactly CC is doing. We came up with a
custom ant logger (extending XmlLogger) which adds information about ant
target currently running to the buildstatus snippet. See attached
screenshot to get an idea.

Configuration example:
  <ant ...
       loggerClassName="net.sourceforge.cruisecontrol.util.XmlLoggerWithStatus">
    <property name="XmlLoggerWithStatus.file" value="logs/proj1/buildstatus.txt"/>
  </ant>

Be sure to make the class available to the Ant classloader if you're using
the antscript attribute.
