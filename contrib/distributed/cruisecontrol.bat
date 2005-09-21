@echo off

REM ################################################################################
REM # CruiseControl, a Continuous Integration Toolkit
REM # Copyright (c) 2001, ThoughtWorks, Inc.
REM # 651 W Washington Ave. Suite 600
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

REM Set this if you're using SSH-based CVS
REM set CVS_RSH=

REM Uncomment the following line if you have OutOfMemoryError errors
REM set CC_OPTS=-Xms128m -Xmx256m

REM The root of the CruiseControl directory.  The key requirement is that this is the parent
REM directory of CruiseControl's lib and dist directories.
REM By default assume they are using the batch file from the local directory.
REM Acknowledgments to Ant Project for this batch file incantation
REM %~dp0 is name of current script under NT
set DEFAULT_CCDIR=%~dp0
REM : operator works similar to make : operator
set DEFAULT_CCDIR=%DEFAULT_CCDIR%\..

if not defined CCDIR set CCDIR=%DEFAULT_CCDIR%
set DEFAULT_CCDIR=

:setClassPath
set CRUISE_PATH=

:checkJava
if not defined JAVA_HOME goto noJavaHome
set CRUISE_PATH=%JAVA_HOME%\lib\tools.jar
goto setCruise

:noJavaHome
echo Warning: You have not set the JAVA_HOME environment variable. Any tasks relying on the tools.jar file (such as <javac>) will not work properly.

:setCruise

set LIBDIR=%CCDIR%\lib
set DISTDIR=%CCDIR%\dist
set DISTRIBDIR=%CCDIR%\..\contrib\distributed
set LIBS_JINI=%DISTRIBDIR%\lib\jini-core.jar;%DISTRIBDIR%\lib\jini-ext.jar;%DISTRIBDIR%\lib\jsk-platform.jar;%DISTRIBDIR%\lib\reggie.jar;%DISTRIBDIR%\lib\reggie-dl.jar;%DISTRIBDIR%\lib\start.jar;%DISTRIBDIR%\lib\tools.jar;%DISTRIBDIR%\conf

set CRUISE_PATH=%CRUISE_PATH%;%DISTDIR%\cruisecontrol.jar;%LIBDIR%\log4j.jar;%LIBDIR%\jdom.jar;%LIBDIR%\ant\ant.jar;%LIBDIR%\ant\ant-launcher.jar;%LIBDIR%\xml-apis-2.7.0.jar;%LIBDIR%\xercesImpl-2.7.0.jar;%LIBDIR%\xalan-2.6.0.jar;%LIBDIR%\jakarta-oro-2.0.3.jar;%LIBDIR%\mail.jar;%LIBDIR%\junit.jar;%LIBDIR%\activation.jar;%LIBDIR%\commons-net-1.1.0.jar;%LIBDIR%\mx4j.jar;%LIBDIR%\mx4j-tools.jar;%LIBDIR%\mx4j-remote.jar;%LIBDIR%\fast-md5.jar;%DISTRIBDIR%\dist\agent\lib\cc-agent.jar;%LIBS_JINI%;.

set EXEC="%JAVA_HOME%\bin\java" %CC_OPTS% -cp "%CRUISE_PATH%" -Djavax.management.builder.initial=mx4j.server.MX4JMBeanServerBuilder -Djava.security.policy=%DISTRIBDIR%\conf\insecure.policy CruiseControl -debug %*
echo %EXEC%
%EXEC%

