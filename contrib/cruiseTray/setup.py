from distutils.core import setup
import py2exe

setup(console=["cruiseTrayIcon.py"],
      description="A system tray icon for monitoring cruise control",
      version='0.0.1',
      scripts=['cruiseTrayIcon.py'],
      data_files=[(".",
                   ["green.ico", "red.ico"])])