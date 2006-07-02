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
CCDIR=`pwd`

# Uncomment the following line if you have OutOfMemoryError errors
# CC_OPTS="-Xms128m -Xmx256m"

LIBDIR=$CCDIR/lib

# convert the existing path to unix
if [ `uname | grep -n CYGWIN` ]; then
   JAVA_HOME=`cygpath --path --unix "$JAVA_HOME"`
fi

CRUISE_PATH=$JAVA_HOME/lib/tools.jar:$LIBDIR/cruisecontrol.jar:$LIBDIR/log4j.jar:$LIBDIR/jdom.jar:$LIBDIR/ant.jar:$LIBDIR/ant-launcher.jar:$LIBDIR/jasper-compiler.jar:$LIBDIR/jasper-runtime.jar:$LIBDIR/xercesImpl-2.8.0.jar:$LIBDIR/xml-apis-2.8.0.jar:$LIBDIR/xmlrpc-2.0.1.jar:$LIBDIR/saxon8.jar:$LIBDIR/saxon8-dom.jar:$LIBDIR/serializer-2.7.0.jar:$LIBDIR/jakarta-oro-2.0.3.jar:$LIBDIR/mail.jar:$LIBDIR/junit.jar:$LIBDIR/activation.jar:$LIBDIR/commons-net-1.1.0.jar:$LIBDIR/starteam-sdk.jar:$LIBDIR/mx4j.jar:$LIBDIR/mx4j-tools.jar:$LIBDIR/mx4j-remote.jar:$LIBDIR/smack.jar:$LIBDIR/comm.jar:$LIBDIR/x10.jar:$LIBDIR/fast-md5.jar:$LIBDIR/maven-embedder-2.0.3-dep.jar:$LIBDIR/javax.servlet.jar:$LIBDIR/org.mortbay.jetty.jar:$LIBDIR/commons-logging.jar:$LIBDIR/commons-el.jar:.

# convert the existing path to unix
if [ `uname | grep -n CYGWIN` ]; then
  CRUISE_PATH=`cygpath --path --windows "$CRUISE_PATH"`
fi

EXEC="$JAVA_HOME/bin/java $CC_OPTS -cp $CRUISE_PATH -Djavax.management.builder.initial=mx4j.server.MX4JMBeanServerBuilder CruiseControlWithJetty $@ -jmxport 8000"
echo $EXEC
exec $EXEC
