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

set ANT_CLASSPATH=lib/ant.jar;lib/ant-junit.jar;lib/ant-launcher.jar;lib/junit-3.8.2.jar;lib/xercesImpl-2.8.0.jar;lib/xml-apis-2.8.0.jar
echo %ANT_CLASSPATH%

if "%JAVA_HOME%" == "" goto noJavaFound

if not "%JIKESPATH%" == "" goto useJikes

echo Using Javac!
if exist "%JAVA_HOME%\lib\tools.jar" goto useModern
if exist "%JAVA_HOME%\lib\classes.zip" goto useClassic
goto noJavaFound

:useClassic
set BUILDCOMPILER=classic
set ANT_CLASSPATH=%ANT_CLASSPATH%;"%JAVA_HOME%\lib\classes.zip"
goto exec

:useModern
set BUILDCOMPILER=modern
set ANT_CLASSPATH=%ANT_CLASSPATH%;"%JAVA_HOME%\lib\tools.jar"
goto exec

:useJikes
set BUILDCOMPILER=jikes
echo Using Jikes!
goto exec

:exec
"%JAVA_HOME%\bin\java" -classpath %ANT_CLASSPATH% -Dbuild.compiler="%BUILDCOMPILER%" org.apache.tools.ant.Main %1 %2 %3 %4 %5 %6 %7 %8 %9
goto end

:noJavaFound
echo Java not found!
goto end

:end
