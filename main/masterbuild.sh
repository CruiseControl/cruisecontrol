#!/bin/sh

#-------------------------------------------------------------------------------
# This build file is used to run a continuous integration build on 
# CruiseControl itself
#-------------------------------------------------------------------------------

# Assume we are running the masterbuild script from the local directory.
CCDIR=.

# Directory containing the java libraries.
LIBDIR=${CCDIR}/lib

# Build copy of cruisecontrol that is being built.
LOCALCOPY=ccBuild

#-------------------------------------------------------------------------------
# No need to edit anything past here
#-------------------------------------------------------------------------------

# Try to find Java Home directory, from JAVA_HOME environment 
# or java executable found in PATH.

if test -z "${JAVA_HOME}" ; then
   echo "ERROR: JAVA_HOME not found in your environment."
   echo "Please, set the JAVA_HOME variable in your environment to match the"
   echo "location of the Java Virtual Machine you want to use."
   exit
fi

# Convert the existing path to unix.
if [ "$OSTYPE" = "cygwin32" ] || [ "$OSTYPE" = "cygwin" ] ; then
   CRUISE_PATH=`cygpath --path --unix "$CRUISE_PATH"`
   JAVA_HOME=`cygpath --path --unix "$JAVA_HOME"`
fi

# Define the java executable path.
if [ "$JAVABIN" = "" ] ; then
  JAVABIN=${JAVA_HOME}/bin/java
fi

# Find all jars defined in lib dir and add them to CRUISE_PATH.
for lib in ${LIBDIR:-.}/*.jar
do
    CRUISE_PATH=${CRUISE_PATH}:${lib}
done

CRUISE_PATH=${CRUISE_PATH}:${CCDIR}/dist/cruisecontrol.jar

# Try to include tools.jar for compilation
if test -f ${JAVA_HOME}/lib/tools.jar ; then
    CRUISE_PATH=${CRUISE_PATH}:${JAVA_HOME}/lib/tools.jar
fi

# Convert the unix path to windows.
if [ "$OSTYPE" = "cygwin32" ] || [ "$OSTYPE" = "cygwin" ] ; then
   CRUISE_PATH=`cygpath --path --windows "$CRUISE_PATH"`
   JAVA_HOME=`cygpath --path --windows "$JAVA_HOME"`
fi

# Call Cruise Control
${JAVABIN} -cp "${CRUISE_PATH}" -Dlocalcopy="${LOCALCOPY}" net.sourceforge.cruisecontrol.MasterBuild "$@"
