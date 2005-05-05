Summary: __SUMMARY__
Name: __NAME__
Version: __VERSION__
Release: __RELEASE__
Copyright: BSD
Group: Development/Build Tools
BuildRoot: __ROOT__

%description
__SRC_DESC__

%prep


%build

%install

%clean

%pre

grep -q __RUNTIME_USER__ /etc/passwd || useradd __RUNTIME_USER__
%post

%preun
%postun

%files -f __FILES__