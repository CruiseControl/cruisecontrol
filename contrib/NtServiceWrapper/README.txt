To install CruiseControl as a NTService

First build CruiseControl.
Download and unzip wrapper_win32_3.1.1.zip 
from http://sourceforge.net/projects/wrapper

place wrapper.jar, wrappertest.jar and wrapper.dll in the lib dir.
place wrapper.exe in the bin dir.

run ant -Dcc.work.dir=/Absolute/Path/To/Dir/ContainingConfig.xml/
Verify the generated settings in wrapper.conf

test the wrapper with 
cruisecontrol.bat  

Install the service with
InstallCruisecontrolWrapper-NT.bat

Start the service with
cruisecontrol.bat start

Stop the service with
cruisecontrol.bat stop


