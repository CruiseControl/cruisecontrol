Install python and wxPython: 

http://www.python.org/ftp/python/2.3.4/Python-2.3.4.exe 

http://prdownloads.sourceforge.net/wxpython/wxPythonWIN32-2.5.1.5-Py23.exe 

Put the attached files in a directory and run "python cruiseTrayIcon.py  -u http://yourCruiseBuildPage.html" (replacing http://yourCruiseBuildPage.html with the url of your cruise build results page) 
(You might have to add your python installation directory to your PATH.) Run "python cruiseTrayIcon.py  -help" to find out the other options. 
Double left click opens the cruise build page. Double right click kills the icon. 

If you want to package this as a .exe for distribution to people who don't want to install Python and wxPython, then install py2exe: 

http://prdownloads.sourceforge.net/py2exe/py2exe-0.5.3.win32-py2.3.exe?download 

and execute "python setup.py py2exe" in the directory where these files are, you'll get a directory "dist" which includes the .exe and the stuff the .exe depends upon, which can then be used to run the system tray icon on machines that don't have python/wxPython installed.

If you're on Windows and you don't want to see the console window modify the line in setup.py that says:

setup(console=["cruiseTrayIcon.py"],

to:

setup(window=["cruiseTrayIcon.py"],

