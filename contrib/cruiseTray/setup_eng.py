from distutils.core import setup
import py2exe

setup(windows=[{"script":"ccTray_eng.py","icon_resources": [(1, "py.ico")]} ],
      options = {"py2exe": {"packages": ["encodings"],"optimize":2}},
      description="SYSTEM TRAY ICON, made by CC-CONTRIB, modified by Ali Eyertas (eyertas@gmx.de)",
      version='0.1.5.7',
      data_files=[(".",["green.ico", "red.ico", "py.ico","question.ico"])])

