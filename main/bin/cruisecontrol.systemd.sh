#!/bin/sh
#
# CruiseControl, a Continuous Integration Toolkit
# Copyright (c) 2001, ThoughtWorks, Inc.
# 200 E. Randolph, 25th Floor
# Chicago, IL 60601 USA
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
#
#
# This file is part of systemd.
#
# systemd is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
#
# NOTES:
#
# Although systemd is able to run and manage daemons directly, the
# invocation of cruisecontrol is little bit complicated due to:
#
#     + systemd does not support "recursive" env variables resolving,
#       i.e. variable in /etc/conf.d/defines
#
#          VAR1=something
#          VAR2=anything and $VAR1
#
#       will not be resolved when used in ssystemd service like:
#
#        [Service]
#        EnvironmentFile=/etc/conf.d/defines
#        ExecStart=/bin/command $VAR2
#
#       see 'systemd fix no.1'
#
#     + when a process is started as ordinary user (User=...),
#       it is not started in user's home directory, see systemd fix
#       no.2'
#
# Onece systemd will be able to do it itsels, this script will not
# be necessary anymore
#
# The script requires the following env variables defined:
#
#     + JAVA_OPTS   ... options passed to java command
#     + CRUISE_OPTS ... options passed to cruisecontrol launcher
#     + CRUISE_DIR  ... the directory where the cruisecontrol is
#                       installed



# systemd fix no.1:
# Finish properties resolving. systemd does not do it ...
CRUISE_OPTS=$(eval echo $CRUISE_OPTS)
JAVA_OPTS=$(eval echo $JAVA_OPTS)

# Prepare the command and print it
# Prefere the "user-defined" stuff, but hardcode some other necessary stuff
# for the correct cruisecontrol invocation
EXEC="/usr/bin/java -server -Xincgc $JAVA_OPTS -Dcc.library.dir=$CRUISE_DIR/lib -jar $CRUISE_DIR/dist/cruisecontrol-launcher.jar $CRUISE_OPTS"
logger "Starting CC: $EXEC"

# systemd fix no.2
# Go to the home directory of the user we are running under
cd   $HOME
# Start the cruisecontrol as the very last action of the script
# The command must "replace" the shell. Otherwise the systemd won't be able to trac the failures of
# cruisecontrol (or a waiting shell will be running)
exec $EXEC
