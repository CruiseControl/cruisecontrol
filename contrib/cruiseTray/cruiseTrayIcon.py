from urllib import urlopen
from getopt import getopt, GetoptError
import wx, time, webbrowser, sys
#import random

oneSecond = 1000

class ConfigurationOptions:
    def __init__(self):
        self.shouldShowStatusChangeDialog = True
        self.pollingDelay = oneSecond * 60
        self.cruiseUrl = "http://localhost:8080/scratch/cruise"
        self.buildFailedString = "BUILD FAILED"

class CruiseWatchingTrayIconFrame(wx.Frame):
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
        
    def OnTaskBarRightDClick(self, evt):
        self.Close(True)
        wx.GetApp().ProcessIdle()
        
    def SetIconTimer(self):
        wxId = wx.NewId()
        self.icontimer = wx.Timer(self, wxId)
        wx.EVT_TIMER(self, wxId, self.CheckBuild)
        self.icontimer.Start(self.configurationOptions.pollingDelay)

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
        if o == "-h":
            usage()
            sys.exit()
    CruiseWatchingTrayIconApp(0, configurationOptions).MainLoop()

if __name__ == '__main__':
    main()
