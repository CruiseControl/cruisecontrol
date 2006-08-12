
; The name of the installer
Name "Cruise Control"

; The file to write
OutFile "..\..\target\CruiseControl.exe"

; The default installation directory
InstallDir $PROGRAMFILES\CruiseControl

; Registry key to check for directory (so if you install again, it will 
; overwrite the old one automatically)
InstallDirRegKey HKLM "Software\CruiseControl" "Install_Dir"

;--------------------------------

; Pages
Page components
Page directory
Page instfiles

UninstPage uninstConfirm
UninstPage instfiles

  Function .onInstSuccess
    MessageBox MB_YESNO "CruiseControl installed successfully. View readme?" IDNO NoReadme
      Exec "notepad.exe $INSTDIR/README.txt"
    NoReadme:
  FunctionEnd

; The stuff to install
Section "Cruise"

  SectionIn RO

  SetOutPath $INSTDIR
  File /r /x CVS "..\..\target\windows-install\cruisecontrol\*.*"
  File "..\..\RELEASENOTES.txt"
  Delete $INSTDIR\cruisecontrol.sh
  Delete $INSTDIR\logs\commons-math

  ; Write the installation path into the registry
  WriteRegStr HKLM SOFTWARE\CruiseControl "Install_Dir" "$INSTDIR"
  
  ; Write the uninstall keys for Windows
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\CruiseControl" "DisplayName" "NSIS CruiseControl"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\CruiseControl" "UninstallString" '"$INSTDIR\uninstall.exe"'
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\CruiseControl" "NoModify" 1
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\CruiseControl" "NoRepair" 1
  WriteUninstaller "uninstall.exe"
  
SectionEnd


; Optional section (can be disabled by the user)
Section "Start Menu Shortcuts"

  SetOutPath $INSTDIR
  CreateDirectory "$SMPROGRAMS\CruiseControl"
  CreateShortCut "$SMPROGRAMS\CruiseControl\Uninstall.lnk" "$INSTDIR\uninstall.exe" "" "$INSTDIR\uninstall.exe" 0
  CreateShortCut "$SMPROGRAMS\CruiseControl\Docs.lnk" "$INSTDIR\docs\index.html" "" "" 0 "" "" "Documentation"
  CreateShortCut "$SMPROGRAMS\CruiseControl\ReportingApp.lnk" "http://localhost:8080/cruisecontrol" "" "" 0 "" "" "Reports"
  CreateShortCut "$SMPROGRAMS\CruiseControl\CruiseControl.lnk" "$INSTDIR\cruisecontrol.bat" "" "$INSTDIR\cruisecontrol.bat" 0 
  CreateShortCut "$SMPROGRAMS\CruiseControl\CruiseConfig.lnk" "http://cc-config.sf.net/release/cruisecontrol-gui.jnlp" "" "" 0 "" "" "CruiseControl Configuration"
  
SectionEnd

;--------------------------------

Section "Windows Service"
  ;
   SetOutPath $INSTDIR
   File wrapper.conf
   File  "..\JavaServiceWrapper\bin\wrapper.exe"
   SetOutPath $INSTDIR\lib\wrapper
   File /r /x .svn "..\JavaServiceWrapper\lib\*.*"
   MessageBox MB_OK "Start the CruiseControl service from the Services Control Panel"   

   Exec '"$INSTDIR\Wrapper.exe" -i "$INSTDIR\wrapper.conf"'
     
SectionEnd

; Uninstaller

Section "Uninstall"
  
  ; Remove registry keys
  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\CruiseControl"
  DeleteRegKey HKLM SOFTWARE\CruiseControl
  
  ;SetOutPath $TEMP
  CopyFiles $INSTDIR/logs $TEMP\CruiseControlArtifacts\logs
  CopyFiles $INSTDIR/artifacts $TEMP\CruiseControlArtifacts\artifacts
  MessageBox MB_OK "Your log files and artifacts are in $TEMP\CruiseControlArtifacts"

  ; remove service
  Exec "net stop cruisecontrol"
  Exec '"$INSTDIR\Wrapper.exe" -u "$INSTDIR\wrapper.conf"'
  
  RMDir /r $PROGRAMFILES\CruiseControl
  ; Remove shortcut
  RMDir /r $SMPROGRAMS\CruiseControl
  
SectionEnd
