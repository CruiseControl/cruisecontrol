#!/bin/bash

# /********************************************************************************
#  * CruiseControl, a Continuous Integration Toolkit                              *
#  * Copyright (C) 2001  ThoughtWorks, Inc.                                       *
#  * 651 W Washington Ave. Suite 500                                              *
#  * Chicago, IL 60661 USA                                                        *
#  *                                                                              *
#  * This program is free software; you can redistribute it and/or                *
#  * modify it under the terms of the GNU General Public License                  *
#  * as published by the Free Software Foundation; either version 2               *
#  * of the License, or (at your option) any later version.                       *
#  *                                                                              *
#  * This program is distributed in the hope that it will be useful,              *
#  * but WITHOUT ANY WARRANTY; without even the implied warranty of               *
#  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                *
#  * GNU General Public License for more details.                                 *
#  *                                                                              *
#  * You should have received a copy of the GNU General Public License            *
#  * along with this program; if not, write to the Free Software                  *
#  * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.  *
#  ********************************************************************************/

CCDIR=`dirname $0`
LIBDIR=$CCDIR/lib
CRUISE_PATH=$JAVA_HOME/lib/tools.jar:$CCDIR/dist/cruisecontrol.jar:$LIBDIR/ant.jar:$LIBDIR/xerces.jar:$LIBDIR/mail.jar:$LIBDIR/optional.jar:$LIBDIR/junit.jar:$LIBDIR/activation.jar:.

EXEC=java -cp $CRUISE_PATH net.sourceforge.cruisecontrol.MasterBuild $@
echo $EXEC
$EXEC

