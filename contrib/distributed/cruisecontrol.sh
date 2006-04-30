#!/bin/bash

################################################################################
# CruiseControl, a Continuous Integration Toolkit
# Copyright (c) 2001, ThoughtWorks, Inc.
# 651 W Washington Ave. Suite 600
# Chicago, IL 60661 USA
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions
# are met:
#
#     + Redistributions of source code must retain the above copyright
#       notice, this list of conditions and the following disclaimer.
#
#     + Redistributions in binary form must reproduce the above
#       copyright notice, this list of conditions and the following
#       disclaimer in the documentation and/or other materials provided
#       with the distribution.
#
#     + Neither the name of ThoughtWorks, Inc., CruiseControl, nor the
#       names of its contributors may be used to endorse or promote
#       products derived from this software without specific prior
#       written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
# "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
# LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
# A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
# CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
# EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
# PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
# PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
# LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
# NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
# SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
################################################################################

# The root of the CruiseControl directory.  The key requirement is that this is the parent
# directory of CruiseControl's lib and dist directories.

# Uncomment the following line if you have OutOfMemoryError errors
# CC_OPTS="-Xms128m -Xmx256m"

# Inspired by Ant's wrapper script
if [ -z "$CCDIR" ] ; then
  # resolve links - $0 may be a softlink
  PRG="$0"

  # need this for relative symlinks
  while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
      PRG="$link"
    else
      PRG=`dirname "$PRG"`"/$link"
    fi
  done

  saveddir=`pwd`

  CCDIR=`dirname "$PRG"`/..

  # make it fully qualified
  CCDIR=`cd "$CCDIR" && pwd`

  cd $saveddir
  echo Using Cruise Control at $CCDIR
fi
LIBDIR=$CCDIR/lib
DISTDIR=$CCDIR/dist
DISTRIBDIR=$CCDIR/../contrib/distributed
LIBS_JINI=$DISTRIBDIR/lib/jini-core.jar:$DISTRIBDIR/lib/jini-ext.jar:$DISTRIBDIR/lib/jsk-platform.jar:$DISTRIBDIR/lib/reggie.jar:$DISTRIBDIR/lib/reggie-dl.jar:$DISTRIBDIR/lib/start.jar:$DISTRIBDIR/lib/tools.jar:$DISTRIBDIR/conf

CRUISE_PATH=$JAVA_HOME/lib/tools.jar:$DISTDIR/cruisecontrol.jar:$LIBDIR/log4j.jar:$LIBDIR/jdom.jar:$LIBDIR/ant/ant.jar:$LIBDIR/ant/ant-launcher.jar:$LIBDIR/xml-apis-2.8.0.jar:$LIBDIR/xercesImpl-2.8.0.jar:$LIBDIR/xalan-2.7.0.jar:$LIBDIR/serializer-2.7.0.jar:$LIBDIR/jakarta-oro-2.0.3.jar:$LIBDIR/mail.jar:$LIBDIR/junit.jar:$LIBDIR/activation.jar:$LIBDIR/commons-net-1.1.0.jar:$LIBDIR/starteam-sdk.jar:$LIBDIR/mx4j.jar:$LIBDIR/mx4j-tools.jar:$LIBDIR/mx4j-remote.jar:$LIBDIR/smack.jar:$LIBDIR/fast-md5.jar:$LIBDIR/maven-embedder-2.0.3-dep.jar:$DISTRIBDIR/dist/agent/lib/cc-agent.jar:$LIBS_JINI:.

# convert the existing path to unix
if [ `uname | grep -n CYGWIN` ]; then
  CRUISE_PATH=`cygpath --path --windows "$CRUISE_PATH"`
fi

EXEC="$JAVA_HOME/bin/java $CC_OPTS -cp $CRUISE_PATH -Djavax.management.builder.initial=mx4j.server.MX4JMBeanServerBuilder -Djava.security.policy=$DISTRIBDIR/conf/insecure.policy CruiseControl $@" 
echo $EXEC
exec $EXEC
