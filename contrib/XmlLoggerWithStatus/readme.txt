Our build takes around 25 minutes to complete, this is why it is
sometimes important to see what exactly CC is doing. We came up with a
custom ant logger (extending XmlLogger) which adds information about ant
target currently running to the buildstatus snippet. See attached
screenshot to get an idea.

You may also specify a regular expression to filter out targets you do not
want to see. This can be useful if you have an internal target that is called 
many times during your build.

Configuration example:
  <ant ...
       loggerClassName="net.sourceforge.cruisecontrol.util.XmlLoggerWithStatus">
    <property name="XmlLoggerWithStatus.file" value="logs/proj1/buildstatus.txt"/>
    <property name="XmlLoggerWithStatus.filter" value="-my-internal-target"/>
  </ant>

Be sure to make the class available to the Ant classloader if you're using
the antscript attribute.
