
v 0.1.5.7

Modified Version of CC-Tray Icon for CruiseControl 2.2

Install notes:

Install python and wxPython: 

http://www.python.org/ftp/python/2.4/python-2.4.msi 

http://prdownloads.sourceforge.net/wxpython/wxPython2.5-win32-ansi-2.5.3.1-py24.exe 

Put the attached files in a directory and run "python ccTray_eng.py  -u http://yourCruiseBuildPage.html" (replacing http://yourCruiseBuildPage.html with the url of your cruise build results page) 
(You might have to add your python installation directory to your PATH.) Run "python ccTray_eng.py  -help" to find out the other options. 
Double left click opens the cruise build page. Double right click kills the icon. 

If you want to package this as a .exe for distribution to people who don't want to install Python and wxPython, then install py2exe: 

http://prdownloads.sourceforge.net/py2exe/py2exe-0.5.4.win32-py2.4.exe?download 

and execute "python -OO setup_eng.py py2exe" in the directory where these files are, you'll get a directory "dist" which includes the .exe and the stuff the .exe depends upon, which can then be used to run the system tray icon on machines that don't have python/wxPython installed.

Note: The file "msvcr71.dll" is missed in the dist folder however, copy it from the Python installation folder to the dist folder, after this, the application works on other machines, which don't have python/wxPython installed!

Usage:

available options are:
            -u urlOfCruiseWebPage (including http://) (std:http://localhost:8080/cruisecontrol)
            -p CruiseControl Remote Port (std: notActive)
            -q (quiet = no dialog box on status change)
            -d pollingDelayInSeconds (how long it waits between polling cruise) (std:60)
            -b buildFailedString (existence on cruise page means build broken) (std:failed)
            -c loadConfig from configfile which is given as parameter
            -s saveConfig to 'property.ini'
            -h (help - show this message)

example:



c:\python ccTray_eng.py -c property.ini


or if executable 

c:\ccTray_eng.exe -c property.ini

multiple options

c:\ccTray_eng.exe -s -p 8000


if you don't use the option -c, the programm looks to a default initialisationfile
-> property.ini

if this file exists, the settings will be taken, if not, the programms default settings will
be used.



Priority of Settings:

default Programm Settings < options < property.ini < -c propertyfile

The URL must have the following form, or the Programm will not work correctly:

http://localhost:8080/cruisecontrol/ProjectA
------ -------------- ------------- --------
  1	    2		   3        	4


1: http://

2: ip address:port  	example: 192.168.0.1:80 or localhost:80

3: cruisecontrol index page  default: /cruisecontrol

4: optional Project Page     default: /cruisecontrol/buildresults/Project_A


The buildFailedString is default for Index Page: failed
				 for Project Page: BUILD FAILED


if you are using CC in singlemode Project

the URL musst contain point 1,2 and 3
the buildFailedString : BUILD FAILED

left-click opens the projectwebpage menu
right-click opens the menu of the tray icon app
double left-click opens the project webpage, if a default webbrowser is registered

you can start more than one instance to observe multiple project at time:

just make for each project propertyfiles and start the programm with the option -c propertyfile


When configurating the CC configfile,
the name of the logdirectory of a Project should be 
the same like the Projects name!!! 

Important for Propertyfile:

Once you have created a property file, all options in the propertyfile musst be declared:

Default PropertyFile:

# The value of the 'url' option must start with
# 'http://', or the Programm will not work correctly
# the domain must contain the port in the following form
# http://localhost:80/abc
# refreshtime must be >= 20000  (value in msec)
# showDialog can only be '1' for True or '0' for False
# saveConfig can only be '1' for True or '0' for False
# remoteportactive can only be '1' for True or '0' for False
# If 1, remotePort must be declared
# remotePort is the CC-Remote-Admin Port
# multiple can only be '1' for True or '0' for False
# If 1, multipleProjects must be declared
# It must contain the absolute Projectnames, seperated by ';'
# If useAuth is 1, userName and passwd must be declared
# This option is used to connected to thw webserver, if the
# requested files are in secure area
# If jmxUseAuth is 1, jmxUserName and jmxPassWord must be declared
# Use this option, when the jmx Http Port is Protected with Password
# (Patch of Glenn Brown CC-128)
# If this File exists, all options has to be declared
# Here is the list of all options
#
# url
# failString
# refreshTime
# showDialog
# saveConfig
# remotePortActive
# remotePort
# multipleOn
# multipleProjects
# useAuth
# userName
# passwd
# jmxUseAuth
# jmxUserName
# jmxPassWord
#
#
# Don't play with this file, if you have no idea what to do with,
# just start the application and config it in the configsection
#
# author: Ali Eyertas eyertas@gmx.de 2005

[trayprop]
username = 
remoteport = 8000
jmxuseauth = 0
multipleon = 0
url = http://localhost:8080/cruisecontrol/
multipleprojects = 
failstring = BUILD FAILED
saveconfig = 1
refreshtime = 60000
remoteportactive = 1
passwd = 
showdialog = 1
useauth = 0
jmxusername = 
jmxpassword = 



Good luck

bugreport to eyertas@gmx.de

 




