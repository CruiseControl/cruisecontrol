#!/bin/sh
export ext=../3rdparty
export lib=../lib

export CLASSPATH=$lib/cruisecontrol-gui.jar:$ext/arch4j.jar:$ext/arch4j-ui.jar:$ext/cruisecontrol.jar:
export CLASSPATH=$CLASSPATH$ext/jakarta-oro-2.0.3.jar:$ext/jdom.jar:$ext/kunststoff.jar:$ext/log4j.jar:$ext/mail.jar:
export CLASSPATH=$CLASSPATH$ext/Piccolo.jar:$ext/xercesImpl.jar:$ext/xml-apis.jar

java -Darch4j.suppress.constants.message=true net.sourceforge.cruisecontrol.gui.MainWindow