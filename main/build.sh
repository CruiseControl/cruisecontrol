#!/bin/sh

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

#--------------------------------------------
# You may modify the default values below.
#--------------------------------------------

# The name of the build file to use
BUILDFILE=build.xml

# Root directory for the project
PROJECTDIR=.

# Directory where necessary build Java libraries are found
LIBDIR=${PROJECTDIR}/lib

#--------------------------------------------
# No need to edit anything past here
#--------------------------------------------

# Try to find Java Home directory, from JAVA_HOME environment 
# or java executable found in PATH

if test -z "${JAVA_HOME}" ; then
    # JAVA_HOME is not set, try to set it if java is in PATH
    JAVABIN=`type java`
    if [ $? -eq 0 ]
    then
        # We found something, clean the path to get a valid JAVA_HOME
        JAVA_HOME=`echo $javart | sed -e 's/\/bin\/java.*//' `
    else
        echo "ERROR: JAVA_HOME not found in your environment."
        echo "Please, set the JAVA_HOME variable in your environment to match the"
        echo "location of the Java Virtual Machine you want to use."
        exit
    fi
fi

# convert the existing path to unix
if [ "$OSTYPE" = "cygwin32" ] || [ "$OSTYPE" = "cygwin" ] ; then
   CLASSPATH=`cygpath --path --unix "$CLASSPATH"`
   JAVA_HOME=`cygpath --path --unix "$JAVA_HOME"`
fi

# Define the java executable path
if [ "$JAVABIN" = "" ] ; then
  JAVABIN=${JAVA_HOME}/bin/java
fi

# Find all jars defined in LIBDIR and add them to the classpath
for lib in ${LIBDIR:-.}/*.jar
do
    CLASSPATH=${CLASSPATH}:${lib}
done

# Try to include tools.jar for compilation
if test -f ${JAVA_HOME}/lib/tools.jar ; then
    CLASSPATH=${CLASSPATH}:${JAVA_HOME}/lib/tools.jar
fi

# convert the unix path to windows
if [ "$OSTYPE" = "cygwin32" ] || [ "$OSTYPE" = "cygwin" ] ; then
   CLASSPATH=`cygpath --path --windows "$CLASSPATH"`
   JAVA_HOME=`cygpath --path --windows "$JAVA_HOME"`
fi

# Call Ant
${JAVABIN} -cp "${CLASSPATH}" org.apache.tools.ant.Main \
           -buildfile "${BUILDFILE}" "$@"
