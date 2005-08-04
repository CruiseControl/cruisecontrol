CruiseControl Binary Release
Release: %%VERSION%%

This binary release is a trimmed down version of the full CruiseControl release.
It is fully functional and intended to work with a sample CVS project "out of
the box".


++ Quick Start For Windows
1) Unzip the release to directory, such as
   c:\cc-sanbox\cruisecontrol-bin-%%VERSION%%
2) Run c:\cc-sanbox\cruisecontrol-bin-%%VERSION%%\cruisecontrol.bat

++ Quick Start For Unix
1) Unzip the release to a directory, like
   ~/cc-sanbox/cruisecontrol-bin-%%VERSION%%
2) Change the permissions for cruisecontrol-bin-%%VERSION%%/cruisecontrol.sh and
   cruisecontrol-bin-%%VERSION%%/apache-ant-1.6.3/bin to allow for execution
3) Run ~/cc-sanbox/cruisecontrol-bin-%%VERSION%%/cruisecontrol.sh from the
   newly unzipped directory


++ What to Expect
CruiseControl starts an instance of Jetty in a seperate thread to host the
reporting application. At some point in the logs you should see a line like:
[cc]Aug-04 07:40:21 SocketListener- Started SocketListener on 0.0.0.0:8080

Simulataneously, the main CruiseControl daemon starts. It comes pre-configured
to run a continuous integration process against jakarta-commons/math. At some
point in the logs you should see:
[cc]Aug-04 07:43:27 Project       - Project commons-math starting
[cc]Aug-04 07:43:27 Project       - Project commons-math:  idle
[cc]Aug-04 07:43:27 Project       - Project commons-math started
[cc]Aug-04 07:43:27 Project       - Project commons-math:  in build queue

The daemon will execute an initial build of commons-math shortly after starting.
Once the intiial build is complete:
BUILD SUCCESSFUL
Total time: 46 seconds
[cc]Aug-04 07:59:18 Project       - Project commons-math:  merging accumulated l
og files
[cc]Aug-04 07:59:19 Project       - Project commons-math:  build successful
[cc]Aug-04 07:59:19 Project       - Project commons-math:  publishing build resu
lts
[cc]Aug-04 07:59:19 Project       - Project commons-math:  idle
[cc]Aug-04 07:59:19 Project       - Project commons-math:  next build in 5 minut
es
[cc]Aug-04 07:59:19 Project       - Project commons-math:  waiting for next time
 to build

Navigate a web browser to http://localhost:8080/cruisecontrol to view the
results in the reporting application.


++ Configuring the Jetty Port
The Jetty bundle with CruiseControl defaults to port 8080.
The port on which Jetty listens may be configured by specifying an optional
parameter to the CruiseControl script named "webport", e.g.

Windows
c:\cc-sanbox\cruisecontrol-bin-%%VERSION%%\cruisecontrol.bat -webport 3456

Unix
~/cc-sanbox/cruisecontrol-bin-%%VERSION%%/cruisecontrol.sh -webport 3456

After setting that you should see:
[cc]Aug-04 07:46:22 SocketListener- Started SocketListener on 0.0.0.0:3456
