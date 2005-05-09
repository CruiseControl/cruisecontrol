Contributed by Julian Simpson
February 25, 2005


Usage:


ant -Dconfig.version=foo -Dconfig.release=bar


Comments:

Included is an Ant buildfile that puts CC into an RPM package using
the Ant 'rpm' task. It's been tested on Fedora Core 2. It was mainly
built on Debian but should work on any Linux distro that has the 'rpm'
or 'rpmbuild' command. My original intent was to deploy the
reporting webapp to the Tomcat that is included in Fedora, but I ran
into some trouble with the GCJ compiled tomcat that they distribute.

I had originally intended to take the CVS changelog and turn it into
an RPM changelog, but have since decided that it was gilding the lily.
 I have the code and stylesheets if anybody is interested. I'd also
prefer to see some feedback from the list and add features that people
actually want :) The bulk of it installs in /usr/share or /usr/doc.
It includes the following work directories:

/var/cruisecontrol/artifacts
/var/cruisecontrol/checkout
/var/cruisecontrol/logs

I made it that way so you could mount '/var/cruisecontrol' on another
disk should you want to, and avoid disk I/O issues. This is because I
just encountered that problem on a project however.