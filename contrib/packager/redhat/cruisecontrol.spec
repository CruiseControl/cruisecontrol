Name: cruisecontrol
Version: @VERSION@
Release: @RELEASE@
Summary: @SUMMARY@
License: BSD
Group: Development/Build Tools
BuildRoot: @ROOT@

%description
@DESCRIPTION@

%prep

%build

%install

%clean

%pre

%preun

%postun

%files
/usr/share/cruisecontrol/
/usr/share/doc/cruisecontrol
/usr/bin/cruisecontrol
/etc/init.d/cruisecontrol
/etc/logrotate.d/cruisecontrol
/etc/default/cruisecontrol
/etc/cruisecontrol/cruisecontrol.xml
/var/run/cruisecontrol.pid
%dir /var/spool/cruisecontrol/
%dir /var/spool/cruisecontrol/artifacts/
%dir /var/spool/cruisecontrol/logs/
%dir /var/spool/cruisecontrol/checkout/
%dir /var/log/cruisecontrol

%post

