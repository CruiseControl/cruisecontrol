@echo off

REM ################################################################################
REM # CruiseControl, a Continuous Integration Toolkit
REM # Copyright (c) 2001, ThoughtWorks, Inc.
REM # 651 W Washington Ave. Suite 500
REM # Chicago, IL 60661 USA
REM # All rights reserved.
REM # 
REM # Redistribution and use in source and binary forms, with or without 
REM # modification, are permitted provided that the following conditions
REM # are met:
REM # 
REM #     + Redistributions of source code must retain the above copyright 
REM #       notice, this list of conditions and the following disclaimer. 
REM #       
REM #     + Redistributions in binary form must reproduce the above 
REM #       copyright notice, this list of conditions and the following 
REM #       disclaimer in the documentation and/or other materials provided 
REM #       with the distribution. 
REM #       
REM #     + Neither the name of ThoughtWorks, Inc., CruiseControl, nor the 
REM #       names of its contributors may be used to endorse or promote 
REM #       products derived from this software without specific prior 
REM #       written permission. 
REM # 
REM # THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
REM # "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT 
REM # LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR 
REM # A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR 
REM # CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
REM # EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
REM # PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
REM # PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF 
REM # LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING 
REM # NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
REM # SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
REM ################################################################################

REM This build file is used to run a continuous integration build on CruiseControl itself

rem Assume they are using the batch file from the local directory.
set CCDIR=.

rem Build copy of cruisecontrol that is being built
set LOCALCOPY=ccBuild

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

set CRUISE_PATH=%CRUISE_PATH%;cruisecontrol.jar;%LIBDIR%\ant.jar;%LIBDIR%\xerces.jar;%LIBDIR%\mail.jar;%LIBDIR%\optional.jar;%LIBDIR%\junit.jar;%LIBDIR%\activation.jar;.
set CRUISE_PATH=%CRUISE_PATH%;%LIBDIR%\jmxtools.jar
set CRUISE_PATH=%CRUISE_PATH%;%LIBDIR%\jmxri.jar

set EXEC=java -cp %CRUISE_PATH% -DlocalCopy="%LOCALCOPY%" net.sourceforge.cruisecontrol.JMXController %1 %2 %3 %4 %5 %6
echo %EXEC%
%EXEC%

