@echo off

REM /********************************************************************************
REM  * CruiseControl, a Continuous Integration Toolkit                              *
REM  * Copyright (C) 2001  ThoughtWorks, Inc.                                       *
REM  * 651 W Washington Ave. Suite 500                                              *
REM  * Chicago, IL 60661 USA                                                        *
REM  *                                                                              *
REM  * This program is free software; you can redistribute it and/or                *
REM  * modify it under the terms of the GNU General Public License                  *
REM  * as published by the Free Software Foundation; either version 2               *
REM  * of the License, or (at your option) any later version.                       *
REM  *                                                                              *
REM  * This program is distributed in the hope that it will be useful,              *
REM  * but WITHOUT ANY WARRANTY; without even the implied warranty of               *
REM  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                *
REM  * GNU General Public License for more details.                                 *
REM  *                                                                              *
REM  * You should have received a copy of the GNU General Public License            *
REM  * along with this program; if not, write to the Free Software                  *
REM  * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.  *
REM  ********************************************************************************/

if "%OS%"=="Windows_NT" goto NtStart

:win9xStart
rem Assume they are using the batch file from the local directory.
set CCDIR=.
goto setClassPath

:ntStart
rem %~dp0 is name of current script's directory under NT
set CCDIR=%~dp0

:setClassPath
set CRUISE_PATH=

:checkJava
if "%JAVA_HOME%" == "" goto noJavaHome
set CRUISE_PATH=%JAVA_HOME%\lib\tools.jar
goto setCruise

:noJavaHome
echo Warning: You have not set the JAVA_HOME environment variable. Any tasks relying on the tools.jar file (such as <javac>) will not work properly.

:setCruise
 
set LIBDIR=%CCDIR%\lib

set CRUISE_PATH=%CRUISE_PATH%;%CCDIR%\dist\cruisecontrol.jar;%LIBDIR%\ant.jar;%LIBDIR%\xerces.jar;%LIBDIR%\mail.jar;%LIBDIR%\optional.jar;%LIBDIR%\junit.jar;%LIBDIR%\activation.jar;.

set EXEC=java -cp %CRUISE_PATH% net.sourceforge.cruisecontrol.MasterBuild %1 %2 %3 %4 %5 %6
echo %EXEC%
%EXEC%

