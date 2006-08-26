Updated by Chris Read
June 30, 2006

Based on the initial RPM package stuff by Julian Simpson

Usage:

ant [-Dconfig.version=version] -Dconfig.release=release [rpm] [deb] 

Comments:

The included Ant buildfile is heavily based on one created by
Julian Simpson for creating an RPM package. Key changes made
are:

- Make the initial build and file layout package independant
- Have targets for building .deb and .rpm packages. More may
  follow soon.

The config.version is pulled from the CC build.properties file, 
but you can override it if you really really need to. By default 
it will try to build both the .deb and .rpm packages.

This has been tested on Ubuntu 6.06 and Fedora Core 5
