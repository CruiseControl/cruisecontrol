# -*- coding: cp1252 -*-
from urllib import urlopen
from getopt import getopt, GetoptError
import wx, time, webbrowser, sys, ConfigParser
import  wx.lib.maskednumctrl    as mnum
#import random

oneSecond = 1000


class ConfigurationOptions:
    def __init__(self):
        self.saveConfig = False
        self.shouldShowStatusChangeDialog = True
        self.pollingDelay = oneSecond * 60
        self.cruiseUrl = "http://localhost:8080/cruisecontrol"
        self.buildFailedString = "failed"
        self.confFileExists = True

class CruiseWatchingTrayIconFrame(wx.Frame):

    TBMENU_CONFIG = 1000
    TBMENU_CLOSE   = 1001
    TBMENU_REFRESH   = 1002
    TBMENU_INFO = 1003
    enableConfig = 1
    f = 0
    teUrl = 0
    teFaStr = 0
    teRef = 0
    rb = 0
    rbSav = 0
    
    
    
    def __init__(self, configurationOptions):

        
        wx.Frame.__init__(self, None, -1, '', size = (1, 1),
            style=wx.FRAME_NO_TASKBAR|wx.NO_FULL_REPAINT_ON_RESIZE)
        self.buildIsBrokenStatus = "unknown"
        self.configurationOptions = configurationOptions
        if self.configurationOptions.shouldShowStatusChangeDialog:
            self.dlg = None
        else:
            self.dlg = "don't show the status change dialog please"
        self.lastBuildChecked = "something"
        self.tbicon = wx.TaskBarIcon()
        self.CheckBuild("whatever")
        
        
        wx.EVT_TASKBAR_LEFT_DCLICK(self.tbicon, self.OnTaskBarLeftDClick)
        wx.EVT_TASKBAR_RIGHT_DCLICK(self.tbicon, self.OnTaskBarRightDClick)
        self.tbicon.Bind(wx.EVT_TASKBAR_RIGHT_UP, self.OnTaskBarMenu)
        self.tbicon.Bind(wx.EVT_MENU, self.OnTaskBarActivate, id=self.TBMENU_CONFIG)
        self.tbicon.Bind(wx.EVT_MENU, self.OnTaskBarRightDClick, id=self.TBMENU_CLOSE)
        self.tbicon.Bind(wx.EVT_MENU, self.CheckBuild, id=self.TBMENU_REFRESH)
        self.tbicon.Bind(wx.EVT_MENU, self.OnInfo, id=self.TBMENU_INFO)
        self.SetIconTimer()
        self.Show(True)
       
        
    def buildStatus(self):
        try:
            cruisePage = urlopen(self.configurationOptions.cruiseUrl).read()
            return cruisePage.count(self.configurationOptions.buildFailedString)
        except IOError:
            return True
        #return random.randint(0,1)

    def OnTaskBarLeftDClick(self, evt):
        webbrowser.open(self.configurationOptions.cruiseUrl)
        return
        
    def OnTaskBarRightDClick(self, evt):
        if self.f!=0: self.f.Close(True)

        if self.configurationOptions.saveConfig == True:
            ptr = file("property.ini",'w')
            text = """\
# Optionsname must be lowercase !
# The value of the 'url' option must start with
# 'http://', or the Programm will not work correctly
# refreshtime must be >= 20000  (value in msec)
# showDialog can only be '1' for True or '0' for False
# saveConfig can only be '1' for True or '0' for False
# If this File exists, all options has to be declared
# Here is the list of all options
#
# url
# failString
# refreshTime
# showDialog
# saveConfig
#
# author: Ali Eyertas eyertas@gmx.de 2004

"""
            
            ptr.write(text)
            cfg1 = ConfigParser.ConfigParser()
            cfg1.add_section("trayprop")
            cfg1.set("trayprop","url",self.configurationOptions.cruiseUrl)
            cfg1.set("trayprop","failString",self.configurationOptions.buildFailedString)
            cfg1.set("trayprop","refreshTime",str(self.configurationOptions.pollingDelay))
            cfg1.set("trayprop","showDialog",str(int(self.configurationOptions.shouldShowStatusChangeDialog)))
            cfg1.set("trayprop","saveConfig",str(int(self.configurationOptions.saveConfig)))
            cfg1.write(ptr)
            ptr.flush()
            ptr.close()

        else:
            if self.configurationOptions.confFileExists == True:
                cfg1 = ConfigParser.ConfigParser()
                cfg1.read("property.ini")
                ptr = file("property.ini",'w')
                text = """\
# Optionsname must be lowercase !
# The value of the 'url' option must start with
# 'http://', or the Programm will not work correctly
# refreshtime must be >= 20000  (value in msec)
# showDialog can only be '1' for True or '0' for False
# saveConfig can only be '1' for True or '0' for False
# If this File exists, all options has to be declared
# Here is the list of all options
#
# url
# failString
# refreshTime
# showDialog
# saveConfig
#
# author: Ali Eyertas eyertas@gmx.de 2004

"""            
                ptr.write(text)
                cfg1.set("trayprop","saveConfig",str(int(self.configurationOptions.saveConfig)))
                cfg1.write(ptr)
                ptr.flush()
                ptr.close()

            else:
                pass
        
        self.Close(True)        
        wx.GetApp().ProcessIdle()
        return

    def OnInfo(self,evt):
        dlg1 = wx.MessageDialog(self, 'CruiseControl Tray Icon made by CC-CONTRIB, modified by Ali Eyertas (eyertas@gmx.de) 2004','About', wx.OK | wx.ICON_INFORMATION)
                          #wxYES_NO | wxNO_DEFAULT | wxCANCEL | wxICON_INFORMATION)
        dlg1.ShowModal()
        dlg1.Destroy()
        return

    def OnResize(self,evt):
        self.f.SetSize(wx.Size(400,240))
        return
        
    def OnTaskBarActivate(self, evt):
        
        self.enableConfig = 0
        self.f= wx.Frame(None,-1,"Configuration",wx.Point(400,200),wx.Size(400,240))
        self.f.SetBackgroundColour(wx.Colour(255,255,255))
        self.f.SetIcon(wx.Icon("py.ico",wx.BITMAP_TYPE_ICO))
        self.f.Fit()
        self.f.Bind(wx.EVT_SIZE,self.OnResize)
        
        url = wx.StaticText( self.f, 1, "URL:",wx.Point(10,10))
        fUrl = url.GetFont()
        fUrl.SetPointSize(12)
        url.SetFont(fUrl)       

        failString = wx.StaticText( self.f, 2, "Fail String:",wx.Point(10,40))
        ffailString = failString.GetFont()
        ffailString.SetPointSize(12)
        failString.SetFont(fUrl)        
        
        ref = wx.StaticText( self.f, 3, "Refresh Time:" ,wx.Point(10,70) )
        fref = ref.GetFont()
        fref.SetPointSize(12)
        ref.SetFont(fUrl)
       

        shDiag = wx.StaticText( self.f, 4, "Show Dialog on Change:", wx.Point(10,100) )
        fshDiag = shDiag.GetFont()
        fshDiag.SetPointSize(12)
        shDiag.SetFont(fUrl)

        saveConfig = wx.StaticText( self.f, 5, "Save Config to File:", wx.Point(10,140) )
        fsaveConfig = saveConfig.GetFont()
        fsaveConfig.SetPointSize(12)
        saveConfig.SetFont(fUrl)

        uTip = wx.ToolTip("URL to Index or to Project Page")        
        uTip.Enable(True)
        fTip = wx.ToolTip("String, that indicates a broken build. For Index Page 'failed', for Project Page 'BUILD FAILED'")        
        fTip.Enable(True)
        rTip = wx.ToolTip("Icon Refresh Time in sec")        
        rTip.Enable(True)

        self.teUrl = wx.TextCtrl(self.f,6,self.configurationOptions.cruiseUrl,wx.Point(190,10),wx.Size(195,20))
        self.teUrl.SetToolTip(uTip)
        self.teFaStr = wx.TextCtrl(self.f,7,self.configurationOptions.buildFailedString,wx.Point(190,40),wx.Size(195,20))
        self.teFaStr.SetToolTip(fTip)
        self.teRef = mnum.MaskedNumCtrl(self.f, value=self.configurationOptions.pollingDelay/1000, integerWidth=5, allowNegative=False)
        self.teRef.SetPosition(wx.Point(190,70))
        self.teRef.SetSize(wx.Size(90,20))
        secondLabel = wx.StaticText( self.f, 9, "seconds",wx.Point(285,75))
        self.teRef.SetToolTip(rTip)

        btOk = wx.Button(self.f,10,"OK",wx.Point(190,175))
        btCa = wx.Button(self.f,11,"Abbrechen",wx.Point(310,175))

        btOk.Bind(wx.EVT_LEFT_UP, self.OnConfOk)
        btCa.Bind(wx.EVT_LEFT_UP, self.OnCloseConfig)

        sampleList = ['Yes', 'No']        
        self.rb = wx.RadioBox(self.f,12, "", wx.Point(190,90), wx.DefaultSize, sampleList, 2, wx.RA_SPECIFY_COLS | wx.NO_BORDER )        
        self.rb.SetToolTip(wx.ToolTip("Show DialogBox when Buildstaus changes?"))
        if self.configurationOptions.shouldShowStatusChangeDialog == True:
            self.rb.SetSelection(0)
        else:
            self.rb.SetSelection(1)

        self.rbSav = wx.RadioBox(self.f,13, "", wx.Point(190,130), wx.DefaultSize, sampleList, 2, wx.RA_SPECIFY_COLS | wx.NO_BORDER )        
        self.rbSav.SetToolTip(wx.ToolTip("Save configuration to 'property.ini' on EXIT ?"))
        if self.configurationOptions.saveConfig == True:
            self.rbSav.SetSelection(0)
        else:
            self.rbSav.SetSelection(1)
        
        self.f.Bind(wx.EVT_CLOSE, self.OnCloseConfig)
        self.f.Show()
        return
        

    def OnConfOk(self,evt):


        if self.teUrl.GetValue().__len__()== 0:
            dlg2 = wx.MessageDialog(self.f, "You have not typed any URL!", 'Cruise Control Message', wx.OK | wx.ICON_ERROR | wx.STAY_ON_TOP)
            dlg2.ShowModal()
            dlg2.Destroy()
            self.teUrl.SetValue(self.configurationOptions.cruiseUrl)
            return
        if self.teUrl.GetValue().isspace() == True:
            dlg2 = wx.MessageDialog(self.f, "You have not typed any URL!", 'Cruise Control Message', wx.OK | wx.ICON_ERROR | wx.STAY_ON_TOP)
            dlg2.ShowModal()
            dlg2.Destroy()
            self.teUrl.SetValue(self.configurationOptions.cruiseUrl)
            return
        if self.teUrl.GetValue().find("http://") != 0: 
            dlg2 = wx.MessageDialog(self.f, "The URL musst start with 'http://' !", 'Cruise Control Message', wx.OK | wx.ICON_ERROR | wx.STAY_ON_TOP)
            dlg2.ShowModal()
            dlg2.Destroy()
            self.teUrl.SetValue(self.configurationOptions.cruiseUrl)
            return
        if self.teUrl.GetValue().__len__()<= 7:
            dlg2 = wx.MessageDialog(self.f, "The URL is not complete!", 'Cruise Control Message', wx.OK | wx.ICON_ERROR | wx.STAY_ON_TOP)
            dlg2.ShowModal()
            dlg2.Destroy()
            self.teUrl.SetValue(self.configurationOptions.cruiseUrl)
            return
        self.configurationOptions.cruiseUrl = self.teUrl.GetValue()
        
        
        if self.teFaStr.GetValue().__len__()== 0:
            dlg2 = wx.MessageDialog(self.f, "You have not typed any FAILSTRING!", 'Cruise Control Message', wx.OK | wx.ICON_ERROR | wx.STAY_ON_TOP)
            dlg2.ShowModal()
            dlg2.Destroy()
            self.teFaStr.SetValue(self.configurationOptions.buildFailedString)
            return
        if self.teFaStr.GetValue().isspace() == True:
            dlg2 = wx.MessageDialog(self.f, "You have not typed any FAILSTRING!", 'Cruise Control Message', wx.OK | wx.ICON_ERROR | wx.STAY_ON_TOP)
            dlg2.ShowModal()
            dlg2.Destroy()
            self.teFaStr.SetValue(self.configurationOptions.buildFailedString)
            return
        self.configurationOptions.buildFailedString = self.teFaStr.GetValue()
        if self.teRef.GetValue()*1000 < 20000:
            dlg2 = wx.MessageDialog(self.f, "Refresh Time too short! Minimum 20 seconds!", 'Cruise Control Message', wx.OK | wx.ICON_ERROR | wx.STAY_ON_TOP)
            dlg2.ShowModal()
            dlg2.Destroy()
            self.teRef.SetValue(self.configurationOptions.pollingDelay/1000)
            return
        self.configurationOptions.pollingDelay = self.teRef.GetValue()*1000

        if self.rb.GetSelection() == 0:
            self.configurationOptions.shouldShowStatusChangeDialog = True
            self.dlg = None
        else:
            self.configurationOptions.shouldShowStatusChangeDialog = False
            self.dlg = "No Dialog"

        if self.rbSav.GetSelection() == 0:
            self.configurationOptions.saveConfig = True
            
        else:
            self.configurationOptions.saveConfig = False
        
        self.enableConfig = 1 
        self.f.Destroy()
        self.f = 0
        return


    def OnCloseConfig(self, evt):        
        
        self.enableConfig = 1 
        self.f.Destroy()
        self.f = 0
        return
        
        
    def OnTaskBarMenu(self, evt):

        menu = wx.Menu()        
        menu.Append(self.TBMENU_CONFIG, "Configuration")
        menu.Append(self.TBMENU_REFRESH, "Refresh")
        menu.Append(self.TBMENU_CLOSE,   "Close")
        menu.AppendSeparator()
        menu.Append(self.TBMENU_INFO,   "About")
        
        if self.enableConfig == 1:
            menu.Enable(self.TBMENU_CONFIG,True)
        else:
            menu.Enable(self.TBMENU_CONFIG,False)

        self.tbicon.PopupMenu(menu)
        menu.Destroy()
        return
  
        
    def SetIconTimer(self):
        wxId = wx.NewId()
        self.icontimer = wx.Timer(self, wxId)
        wx.EVT_TIMER(self, wxId, self.CheckBuild)
        self.icontimer.Start(self.configurationOptions.pollingDelay)
        return

    def CheckBuild(self, evt):

        
        previousIsBrokenStatus = self.buildIsBrokenStatus
    	self.buildIsBrokenStatus = self.buildStatus()
    	if self.buildIsBrokenStatus:
            icon = wx.Icon('red.ico', wx.BITMAP_TYPE_ICO)
            self.tbicon.SetIcon(icon, 'Build is broken')
        else:
            icon = wx.Icon('green.ico', wx.BITMAP_TYPE_ICO)
            self.tbicon.SetIcon(icon, 'Build is OK')

        if previousIsBrokenStatus == "unknown":
            return
    	if previousIsBrokenStatus and self.buildIsBrokenStatus:
            return self.stillBroken()
        if previousIsBrokenStatus and not self.buildIsBrokenStatus:
            return self.buildFixed()
        if not previousIsBrokenStatus and self.buildIsBrokenStatus:
            return self.buildBroken()
        if not previousIsBrokenStatus and not self.buildIsBrokenStatus:
            return self.stillWorking()
        

    def showMessage(self, message):
        if self.dlg != None:
            return
        
        self.dlg = wx.MessageDialog(self, message, 'Cruise Control Message', wx.OK | wx.ICON_INFORMATION | wx.STAY_ON_TOP)
        
        self.dlg.ShowModal()
        self.dlg.Destroy()
        self.dlg = None

    def stillBroken(self):
        #self.showMessage("Build is still broken")
        pass

    def buildFixed(self):
        self.showMessage("Build has been fixed")

    def buildBroken(self):
        self.showMessage("Build has just been broken")

    def stillWorking(self):
        #self.showMessage("Build is still working")
        pass
    
class CruiseWatchingTrayIconApp(wx.App):
    def __init__(self, someNumber, configurationOptions):
        self.configurationOptions = configurationOptions
        wx.App.__init__(self, someNumber)
        
    def OnInit(self):
        frame = CruiseWatchingTrayIconFrame(self.configurationOptions)
        frame.Center(wx.BOTH)
        frame.Show(False)
        return True

def usage():
    print """available options are:
            -u urlOfCruiseWebPage (including http://)
            -q (quiet = no dialog box on status change)
            -d pollingDelayInSeconds (how long it waits between polling cruise)
            -b buildFailedString (existence on cruise page means build broken)
            -s saveConfig to 'property.ini'
            -h (help - show this message)"""
    
def main():
    try:
        opts, noArgsExpected = getopt(sys.argv[1:], "qhd:u:b:", [])
    except GetoptError:
        usage()
        sys.exit(2)
    configurationOptions = ConfigurationOptions()
    for o, a in opts:
        if o == "-q":
            configurationOptions.shouldShowStatusChangeDialog = False
        if o == "-d":
            configurationOptions.pollingDelay = int(a) * oneSecond
        if o == "-u":
            configurationOptions.cruiseUrl = a
        if o == "-b":
            configurationOptions.buildFailedString = a
        if o == "-s":
            configurationOptions.saveConfig = True
        if o == "-h":
            usage()
            sys.exit()
    
    try:
        ptr = file("property.ini",'r')
        ptr.close()
        cfg = ConfigParser.ConfigParser()
        cfg.read("property.ini")
        configurationOptions.cruiseUrl = cfg.get("trayprop","url")
        configurationOptions.buildFailedString = cfg.get("trayprop","failString")
        configurationOptions.pollingDelay = cfg.getint("trayprop","refreshTime")
        configurationOptions.shouldShowStatusChangeDialog = cfg.getboolean("trayprop","showDialog")
        configurationOptions.saveConfig = cfg.getboolean("trayprop","saveConfig")
        
    except IOError:
        configurationOptions.confFileExists = False
        #pass 
    
    CruiseWatchingTrayIconApp(0, configurationOptions).MainLoop()
    

if __name__ == '__main__':
    main()
