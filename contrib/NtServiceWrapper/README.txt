To install CruiseControl as a NTService
First bulid CruiseControl.
Download and unzip wrapper_win32_3.1.1.zip 
from 

place wrapper.jar wrapper-test.jar and wrapper.dll in the lib dir.
place wrapper.ece in the bin dir.

run ant build.xml -Dcc.work.dir=/Absolute/Path/To/Dir/ContainingConfig.xml/
Verify the generated settings in wrapper.conf

test the wrapper with 
CruiseControl.bat  

Install the service with
InstallCruiseControlWrapper-NT.bat

Start the service with
CruiseControl.bat start

Stop the service with
CruiseControl.bat stop


