Welcome to the JavaServiceWrapper tuned for use with CruiseControl.  This wrapper uses the
3.1.1 distribution of Java Service Wrapper from http://wrapper.tanukisoftware.org/.

It is designed to run out of the box on NT and Linux using the default distribution of
CruiseControl with no changes to the configuration file.

Your CruiseControl config.xml file should be at root of the unzipped cruisecontrol:
/config.xml
/main
/reporting
/contrib/JavaServiceWrapper

NT or Linux
1) navigate to /main/
2) run build.bat or build.sh as appropriate
3) verify /main/dist/cruisecontrol.jar exists

NT Installation:
1) navigate to /contrib/JavaServiceWrapper/bin

2) Run "cruisecontrol.bat console".  This will start it up and verify everything
is working.  If your config.xml hasn't been created then it won't start.  Just
put an empty one to verify JMX is up etc.

3) Install the service by running "InstallCruiseControlWrapper-NT.bat"

4) Start the service with "net start cruisecontrol"

5) Stop the service with "net stop cruisecontrol"

6) Remove the service by running "UninstallCruiseControlWrapper-NT.bat"


Linux Installation: