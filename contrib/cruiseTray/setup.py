from distutils.core import setup
import py2exe

setup(console=["cruiseTrayIcon.py"],
      description="SYSTEM TRAY ICON, made by CC-CONTRIB, modified by Ali Eyertas (eyertas@gmx.de)",
      version='0.0.3',
      scripts=['cruiseTrayIcon.py'],
      data_files=[(".",
                   ["green.ico", "red.ico", "py.ico"])])