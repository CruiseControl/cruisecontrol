@echo off

REM ################################################################################
REM # CruiseControl, a Continuous Integration Toolkit
REM # Copyright (c) 2001, ThoughtWorks, Inc.
REM # 200 E. Randolph, 25th Floor
REM # Chicago, IL 60601 USA
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

:checkJava
if not defined JAVA_HOME goto noJavaHome
set JAVA_PATH="%JAVA_HOME%"\bin\java
goto setCruise

:noJavaHome
echo WARNING: You have not set the JAVA_HOME environment variable. Any tasks relying on the tools.jar file (such as javac) will not work properly.
set JAVA_PATH=java

:setCruise
set LIBDIR=%CCDIR%\lib
set DISTDIR=%CCDIR%\dist
set DISTRIBDIR=%CCDIR%\..\contrib\distributed
set DISTRIB_LIBDIR=%DISTRIBDIR%\lib
set DISTRIB_CONFDIR=%DISTRIBDIR%\conf

set EXEC=%JAVA_PATH% -Djavax.management.builder.initial=mx4j.server.MX4JMBeanServerBuilder -Djava.security.policy=%DISTRIBDIR%\conf\insecure.policy -Dcc.library.dir=%LIBDIR% -Dcc.dist.dir=%DISTDIR% -jar %DISTDIR%\cruisecontrol-launcher.jar -lib "%JAVA_HOME%\lib\tools.jar" -lib %DISTRIBDIR%\dist\agent\lib\cc-agent.jar -lib %DISTRIB_LIBDIR% -lib %DISTRIB_CONFDIR% %*
echo %EXEC%
%EXEC%

