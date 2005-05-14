@echo off

set ext=..\3rdparty
set lib=..\lib

set classpath=%lib%\cruisecontrol-gui.jar;%ext%\arch4j.jar;%ext%\arch4j-ui.jar;%ext%\cruisecontrol.jar;
set classpath=%classpath%%ext%\jakarta-oro-2.0.3.jar;%ext%\jdom.jar;%ext%\kunststoff.jar;%ext%\log4j.jar;%ext%\mail.jar;
set classpath=%classpath%%ext%\Piccolo.jar;%ext%\xercesImpl.jar;%ext%\xml-apis.jar;%ext%\ant.jar

java -Darch4j.suppress.constants.message=true net.sourceforge.cruisecontrol.gui.MainWindow
