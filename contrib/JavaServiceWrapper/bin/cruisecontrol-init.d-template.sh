#!/bin/sh

#
# Edit these variables to suit your Cruise Control installation
# chkconfig: - 345 99 05
# description: Script used to start and stop the CruiseControl auto-build system
# pulled from: http://confluence.public.thoughtworks.org/display/CC/RunningCruiseControlFromUnixInit
#
#Put this script into /etc/init.d (or wherever your system holds init-style scripts)
#as "cruisecontrol". 
#Add symlinks in each of your /etc/rc.* directories to it (On RedHat, use chkconfig). 
#Your mileage may vary, depending on your OS and level of UNIX admin expertise.

CCDIR=/usr/local/cruisecontrol
CCSTARTSCRIPT=${CCDIR}/contrib/JavaServiceWrapper/cruisecontrol-wrapper.sh
CCLOGFILE=/${CCDIR}/contrib/JavaServiceWrapper/logs/console.log
CCCOMMAND="${CCSTARTSCRIPT} start" 

#
# DO NOT CHANGE ANTHING BELOW THIS LINE
#

umask 002

export CCDIR

PPID=`ps -ea -o "pid ppid args" | grep -v grep | grep "${CCSTARTSCRIPT}" \
    | sed -e 's/^  *//' -e 's/ .*//'`
if [ "${PPID}" != "" ]
then
  PID=`ps -ea -o "pid ppid args" | grep -v grep | grep java | grep ${PPID} | \
      sed -e 's/^  *//' -e 's/ .*//'`
fi

case "$1" in

  'start')
    ${CCCOMMAND} > ${CCLOGFILE} 2>&1 & RETVAL=$?
    echo "cruisecontrol started"
    ;;

  'stop')
    if [ "${PID}" != "" ]
    then
      kill -9 ${PID} ${PPID}
      $0 status
      RETVAL=$?
    else
      echo "cruisecontrol is not running"
      RETVAL=1
    fi
    ;;

  'status')
    kill -0 ${PID} >/dev/null 2>&1
    if [ "$?" = "0" ]
    then
      echo "cruisecontrol (pid ${PPID} ${PID}) is running"
      RETVAL=0
    else
      echo "cruisecontrol is stopped"
      RETVAL=1
    fi
    ;;

  'restart')
    $0 stop && $0 start
    RETVAL=$?
    ;;

  *)
    echo "Usage: $0 { start | stop | status | restart }"
    exit 1
    ;;

esac
exit ${RETVAL}
