Name: cruisecontrol
Version: @VERSION@
Release: @RELEASE@
Summary: @SUMMARY@
License: BSD
Group: Development/Build Tools
BuildRoot: @ROOT@
Requires: jdk, subversion >= 1.4

%description
@DESCRIPTION@

Subversion packages for RHEL can be found at http://dag.wieers.com/rpm/packages/subversion/

%prep

%build

%install

%clean

%pre

%preun

@PRE@

%postun

%files
/usr/share/cruisecontrol/
/usr/share/doc/cruisecontrol
/usr/bin/cruisecontrol
/etc/init.d/cruisecontrol
/etc/default/cruisecontrol
/etc/cruisecontrol/cruisecontrol.xml
/var/run/cruisecontrol.pid
/var/spool/cruisecontrol/cc.pid
/var/spool/cruisecontrol/log4j.properties
/var/spool/cruisecontrol/checkout/
/var/spool/cruisecontrol/.subversion/
%dir /var/spool/cruisecontrol/
%dir /var/spool/cruisecontrol/artifacts/
%dir /var/spool/cruisecontrol/logs/
%dir /var/cache/cruisecontrol

%post

@POST@

