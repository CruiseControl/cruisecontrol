# -*- coding: cp1252 -*-
from urllib import urlopen
from getopt import getopt, GetoptError
import socket
import wx, time, webbrowser, sys, ConfigParser
from    wx.lib import masked
import httplib
import  wx.lib.dialogs
import  wx.grid as gridlib
import urllib
import thread
import base64

# Global variables

oneSecond = 1000

# Basic AuthOpener, use this class, to open url, when user and password is required

class AuthOpener(urllib.FancyURLopener):
    user=''
    passw=''
    def __init__(self,user,passw,*args,**kwargs):
        urllib.FancyURLopener.__init__(self,*args,**kwargs)
        self.user=user
        self.passw=passw
    def prompt_user_passwd(self, host, realm):
                          
        return self.user, self.passw


# Class definitions for the GRID WINDOW
# Multiple Project SELECTION

class CustomDataTable(gridlib.PyGridTableBase):
    def __init__(self): 
        gridlib.PyGridTableBase.__init__(self) 
        self.colLabels = ['Available Projects', 'Observation active?'] 
        self.dataTypes = [gridlib.GRID_VALUE_STRING,
                          gridlib.GRID_VALUE_BOOL] 
        self.data = []
        

    def GetNumberRows(self): 
        return len(self.data) 
    def GetNumberCols(self): 
        return len(self.colLabels) 
    def IsEmptyCell(self, row, col): 
        try: 
            return not self.data[row][col] 
        except IndexError: 
            return True   
    def GetValue(self, row, col): 
        try: 
            return self.data[row][col] 
        except IndexError: 
            return '' 
    def SetValue(self, row, col, value): 
        try: 
            self.data[row][col] = value 
        except IndexError: 
            # add a new row 
            self.data.append([''] * self.GetNumberCols()) 
            self.SetValue(row, col, value) 
            # tell the grid we've added a row 
            #msg = gridlib.GridTableMessage(self,            # The table 
             #       gridlib.GRIDTABLE_NOTIFY_ROWS_APPENDED, # what we did to it 
             #       1                                       # how many 
            #) 
            #self.GetView().ProcessTableMessage(msg)
# We can only add Datums in the Grid at Startup, or, when connecting the first time to
# the cc-jmx server, so we do not need this function


    def GetColLabelValue(self, col): 
        return self.colLabels[col] 
    def GetTypeName(self, row, col): 
        return self.dataTypes[col] 
    def CanGetValueAs(self, row, col, typeName): 
        colType = self.dataTypes[col].split(':')[0] 
        if typeName == colType: 
            return True 
        else: 
            return False 
    def CanSetValueAs(self, row, col, typeName): 
        return self.CanGetValueAs(row, col, typeName) 
#--------------------------------------------------------------------------- 
class CustTableGrid(gridlib.Grid):
    #table = CustomDataTable()
    def __init__(self, parent,data): 
        gridlib.Grid.__init__(self, parent, -1)
        table=data
        self.SetTable(table, True) 
        self.SetRowLabelSize(40) 
        #self.SetMargins(0,0)
        self.AutoSize()
        self.AutoSizeColumns(True)
        self.EnableDragColSize(False)
        self.EnableDragGridSize(False)
        self.EnableDragRowSize(False)
        col = 0
        attr = gridlib.GridCellAttr() 
        attr.SetReadOnly(True)
        attr.SetTextColour(wx.BLUE)
        #attr.SetBackgroundColour(wx.LIGHT_GREY)
        self.SetColAttr(col,attr)
        gridlib.EVT_GRID_CELL_LEFT_DCLICK(self, self.OnGridLeftDClick)

    def OnGridLeftDClick(self, evt): 
        if self.CanEnableCellControl(): 
            self.EnableCellEditControl() 
#--------------------------------------------------------------------------- 
class GridFrame(wx.Frame):
    
    def __init__(self, parent,data): 
        wx.Frame.__init__(self, parent, -1, "Multiple Projectobservation",wx.DefaultPosition,wx.Size(400,480),wx.RESIZE_BORDER | wx.CAPTION | wx.CLOSE_BOX) 
        p = wx.Panel(self, -1, style=0) 
        self.grid = CustTableGrid(p,data)
        a = wx.Button(p, 1, "OK")
        b = wx.Button(p, 2, "Cancel")     
        a.SetDefault() 
        self.Bind(wx.EVT_BUTTON, parent.OnGridButton, a) 
        self.Bind(wx.EVT_BUTTON, parent.OnCloseGrid, b)
        self.Bind(wx.EVT_CLOSE, parent.OnCloseGrid)
        bs = wx.BoxSizer(wx.VERTICAL) 
        bs.Add(self.grid, 1, wx.TOP)
        bs.Add(a,0,wx.TOP,5)
        bs.Add(b,0,wx.TOP,2)
        p.SetSizer(bs)


# Config Class-Definition

class ConfigurationOptions:
    def __init__(self):
        
        self.configFile = "property.ini"
        self.saveConfig = False
        self.shouldShowStatusChangeDialog = True
        self.pollingDelay = oneSecond * 60
        self.cruiseUrl = "http://localhost:8080/cruisecontrol"
        self.cruiseRemotePort = 8000
        self.cruiseRemotePortActive = False
        self.buildFailedString = "failed"
        self.confFileExists = True
        self.multipleOn = False
        self.multipleProjects = ''
        self.useAuth = False
        self.userName = ''
        self.passwd = ''
        self.jmxUserName = ''
        self.jmxPassWord = ''
        self.jmxUseAuth = False
        return
      

# Frame Class-Definition

class CruiseWatchingTrayIconFrame(wx.Frame):

# Membervariables

    TBMENU_CONFIG = 1000
    TBMENU_CLOSE   = 1001
    TBMENU_REFRESH   = 1002
    TBMENU_INFO = 1003
    TBMENU_WEB = 1004
    TBMENU_DYNAMIC = 1010
    TBMENU_DYNAMIC2 = 2000
    TBMENU_DYNAMIC3 = 3000
    showPopupMenu = True
    indexFailedProject = ''
    lastIndexFailedProject=''
    projectNumber = -1
    menu = 0
    enableConfig = 1
    f = 0
    teUrl = 0
    teFaStr = 0
    teRef = 0
    teRe = 0
    rb = 0
    rbRe = 0
    rbSav = 0
    project = list()
    projectPos = list()
    buildIsBrokenStatus = list()
    configHelpText = """\
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

"""
    
    
        
# Cunstructor
    
    def __init__(self,configurationOptions):

        
        wx.Frame.__init__(self, None, -1, '', size = (1, 1),
            style=wx.FRAME_NO_TASKBAR|wx.NO_FULL_REPAINT_ON_RESIZE)
        self.buildIsBrokenStatus.append("unknown")
        self.configurationOptions = configurationOptions
        if self.configurationOptions.shouldShowStatusChangeDialog:
            self.dlg = None
        else:
            self.dlg = "don't show the status change dialog please"
        self.lastBuildChecked = "something"
        self.tbicon = wx.TaskBarIcon()
        icon = wx.Icon('question.ico', wx.BITMAP_TYPE_ICO)            
        self.tbicon.SetIcon(icon,"STATUS UNBEKANNT")
        self.data = CustomDataTable()
        

# Menu for TrayIcon
        
        self.menu = wx.Menu()        
        self.menu.Append(self.TBMENU_WEB, "Open Project Index Page")
        self.menu.Append(self.TBMENU_CONFIG, "Configuration")
        self.menu.Append(self.TBMENU_REFRESH, "Refresh")
        self.menu.AppendSeparator()
        self.tbicon.Bind(wx.EVT_MENU, self.EVT_MULTIPLE, id=1998)
        self.tbicon.Bind(wx.EVT_MENU, self.EVT_DMENU2, id=1999)
        
        self.submenu = wx.Menu()
        self.submenu2 = wx.Menu()
        self.submenu3 = wx.Menu()
        self.submenu2.Append(1998, "Multiple Projectobservation")
        self.submenu2.AppendSeparator()
        self.submenu2.Append(1999, "All Projects", "", wx.ITEM_CHECK)

# This menuelement can only be shown, if the cc-jmx-server is online
        
        if self.configurationOptions.cruiseRemotePortActive == True:
            self.DynamicMenu()
            
# if we are in single project mode
# Look the URL if it is directed to a ProjectPage Instead of the Index
# than the Menu Item should be checked if there is an Project

        if self.configurationOptions.multipleOn == False :
            str1 = self.configurationOptions.cruiseUrl.split('/')[(self.configurationOptions.cruiseUrl.split('/')).__len__()-1]
            try:
                in1 = self.project.index(str1)
                self.submenu2.Check(2000+in1,True)
                self.configurationOptions.buildFailedString = "BUILD FAILED"
                self.projectNumber = in1
                self.data.SetValue(in1,1,1)
            
            except ValueError:
                pass
        
            try:
                count = self.submenu.GetMenuItemCount()
                menulist=self.submenu.GetMenuItems()
                menulist3=self.submenu3.GetMenuItems()
                if self.projectNumber != -1:
                    id=self.submenu.FindItem(self.project[self.projectNumber])
                    while count>0:
                        count=count-1
                        if id != menulist[count].GetId():
                            self.submenu.Enable(menulist[count].GetId(),False)
                            self.submenu3.Enable(menulist3[count].GetId(),False)
            except IndexError:
                pass
            
        
# The rest of the Menu 
 
        self.menu.AppendMenu(1007,"Open Projectpage",self.submenu3)
        self.menu.AppendMenu(1008,"Projectobservation",self.submenu2)
        self.menu.AppendMenu(1009,"Force Build",self.submenu)
        self.menu.AppendSeparator()
        self.menu.Append(self.TBMENU_CLOSE,"Exit")
        self.menu.AppendSeparator()
        self.menu.Append(self.TBMENU_INFO,"About")

# EventHandler for the MenuItems
            
        wx.EVT_TASKBAR_LEFT_DCLICK(self.tbicon, self.OnTaskBarLeftDClick)
        wx.EVT_TASKBAR_LEFT_UP(self.tbicon, self.OnTaskBarLeftClick)
        self.tbicon.Bind(wx.EVT_TASKBAR_RIGHT_UP, self.OnTaskBarMenu)
        self.tbicon.Bind(wx.EVT_MENU, self.OnTaskBarActivate, id=self.TBMENU_CONFIG)
        self.tbicon.Bind(wx.EVT_MENU, self.OnTaskBarLeftDClick, id=self.TBMENU_WEB)
        self.tbicon.Bind(wx.EVT_MENU, self.OnTaskBarRightDClick, id=self.TBMENU_CLOSE)
        self.tbicon.Bind(wx.EVT_MENU, self.CheckBuild, id=self.TBMENU_REFRESH)
        self.tbicon.Bind(wx.EVT_MENU, self.OnInfo, id=self.TBMENU_INFO)

# if we are in multiple Project Mode
# check the Projects, which was saved in the configfile

        self.StartMultipleProjects()
        self.SetIconTimer()
        self.CheckBuild("whatever")
        
        self.Show(True)

        
        return

    def StartMultipleProjects(self):
        # If multipleOn true, then check all selected Projects to observe
        if self.configurationOptions.multipleOn :            
            pr=self.configurationOptions.multipleProjects.split(':')
            len = pr.__len__()
            len2= self.data.GetNumberRows()
            count = self.submenu.GetMenuItemCount()
            menulist=self.submenu.GetMenuItems()
            menulist2=self.submenu2.GetMenuItems()
            menulist3=self.submenu3.GetMenuItems()
            k=0
            self.projectPos=list()    
            try:

# uncheck and disable all elements
                while k<count :
                    self.submenu.Enable(menulist[k].GetId(),False)
                    self.submenu3.Enable(menulist3[k].GetId(),False)
                    self.menu.Check(menulist[k].GetId() + 990,False)
                    k=k+1
                k=0

# now check and enable all selected elements (projects)  
                while k<len :
                    j=0
                    while j<len2 :
                        if pr[k]==self.project[j] :
                            self.projectPos.append(j)
                            self.data.SetValue(j,1,1)
                            self.submenu.Enable(self.submenu.FindItem(self.project[j]),True)
                            self.submenu3.Enable(self.submenu.FindItem(self.project[j]) + 1990,True)
                            self.menu.Check(self.submenu.FindItem(self.project[j]) + 990,True)
                        j=j+1
                    k=k+1
            except IndexError:
                pass
        return

# show the menu projectpage of selected projects

    def OnTaskBarLeftClick(self,evt):
        lmenu = wx.Menu()
        subM = wx.Menu()
        self.icontimer.Stop()
        mList = self.submenu3.GetMenuItems()
        mCount = self.submenu3.GetMenuItemCount()
        k=0
        while k<mCount :
            if self.submenu3.IsEnabled(mList[k].GetId()) == True :
                subM.AppendItem(mList[k])
            k=k+1
        lmenu.AppendMenu(500,"Open Projectpage",subM)
        if self.showPopupMenu == True:
            self.tbicon.PopupMenu(lmenu)
        self.icontimer.Start(self.configurationOptions.pollingDelay)
        return        

    def OnGridButton(self, evt):

# click = ok        
        if evt.GetId()==1:
            k=0
            mTrue=0
            self.projectPos =list()
            id = list()            
            length = self.project.__len__()
            count = self.submenu.GetMenuItemCount()
            menulist=self.submenu.GetMenuItems()
            menulist3=self.submenu3.GetMenuItems()
            self.configurationOptions.multipleProjects=''

# if celleditor is not closed, the do it now            
            self.fenster.grid.SaveEditControlValue()
            self.fenster.grid.EnableCellEditControl(False)
# check the data
            while k<length:                
                if self.data.GetValue(k,1) == 1:
                    mTrue=mTrue+1
                    self.configurationOptions.multipleProjects=self.configurationOptions.multipleProjects + ':' + self.data.GetValue(k,0)  
                    self.projectPos.append(k)
                    id.append(self.submenu.FindItem(self.project[k]))                   
                k=k+1
# is more than one selected?
            if mTrue > 1:
                self.configurationOptions.multipleOn=True
            if mTrue == 1:
                self.configurationOptions.multipleOn=False
                self.projectNumber=self.projectPos[0]
# are all projects selected?
            if mTrue == count :
                evt.SetId(1999)                
                self.showPopupMenu=True
                self.fenster.Hide()
                self.EVT_DMENU2(evt)
                return
# check and enable the selected projects 
            try:
                length = id.__len__()
                k=0
                self.menu.Check(1999,False)
                while k<count :
                    self.submenu.Enable(menulist[k].GetId(),False)
                    self.submenu3.Enable(menulist3[k].GetId(),False)
                    self.menu.Check(menulist[k].GetId() + 990,False)
                    k=k+1
                k=0
                while k<length :
                    self.submenu.Enable(id[k],True)
                    self.submenu3.Enable(id[k] + 1990,True)
                    self.menu.Check(id[k] + 990,True)
                    k=k+1
            except IndexError:
                pass            
            self.showPopupMenu=True
            self.fenster.Hide()
            self.CheckBuild("Whatever")
            self.icontimer.Start(self.configurationOptions.pollingDelay)
            return
            
    def OnCloseGrid(self,evt):

        self.fenster.grid.EnableCellEditControl(False)
        self.showPopupMenu=True
        self.fenster.Hide()
        self.icontimer.Start(self.configurationOptions.pollingDelay)
        return        
       
# Memberfunction to look at the Buildstatus
# The decision if a Build is broken or not
# depends on a specified String!
# Better would be to make a request to CC and
# get information about the Buildstatus
        
    def buildStatus(self):
        try:
            if self.configurationOptions.useAuth :
                istream = AuthOpener(self.configurationOptions.userName,self.configurationOptions.passwd)
                cruisePage = istream.open(self.configurationOptions.cruiseUrl).read()
            else :                
                cruisePage = urlopen(self.configurationOptions.cruiseUrl).read()
            i = cruisePage.count(self.configurationOptions.buildFailedString)/2
            k = 0
            
# Find the names of the broken builds, only for the indexpage!

            if self.projectNumber == -1:
                try:
                    start = cruisePage.index("<tr>")
                    while True:
                        try:
                            end = cruisePage.index("</tr>",start)
                            test = cruisePage.index(self.configurationOptions.buildFailedString,start,end)
                        
                       # if self.project.__len__() > 0:
                       #     for a in self.project:                            
                       #         if cruisePage.find(a,start,end)!=-1:
                       #             self.indexFailedProject = self.indexFailedProject + a + ';'
                       #             k = k + 1                                
                       #             break
                       # else:
                            start2 = cruisePage.index("<a href",start,end)
                            end2 = cruisePage.index("</a>",start,end)
                            start2 = cruisePage.index(">",start2,end2)+1
                            distance = end2 - start2
                            t=0
                            pname=''
                            while distance > t:
                                pname = pname + cruisePage[start2+t]
                                t=t+1
                            self.indexFailedProject = self.indexFailedProject +" ["+ pname+"] " 
                            k = k + 1
                            if i == k:
                                break
                            start = cruisePage.index("<tr>",end)
                        except ValueError:
                       
                            if i == k:
                                break
                            start = cruisePage.index("<tr>",end)                            
                except ValueError:
                    pass
            #if i == 0: return "unknown"    
            return cruisePage.count(self.configurationOptions.buildFailedString)
        except IOError:
            return "unknown"
        except EOFError:
            return "unknown"
        return


# Here i have mixed up a code, which finds the project, running on CruiseControl
# If in newer Version of CC the communication between browser and CC-RemoteAPP
# changes, than this have to be changed, or it doesent works!!!
# In CC are no commandos to get Infos about the project via NETWORK!!! 


    def DynamicMenu(self):
        if self.submenu.GetMenuItemCount()==0:        
        #find project
            try:
# Read Project-Collections
                response=''
                headers = {"Content-type": "application/x-www-form-urlencoded","Accept": "text/plain"}
                conn = httplib.HTTPConnection((self.configurationOptions.cruiseUrl.split(':')[1]).split("//")[1]+':'+ str(self.configurationOptions.cruiseRemotePort))
                conn.request("POST", "/getattribute", "objectname=CruiseControl%20Manager:id=unique&attribute=Projects&format=collection&template=viewcollection",headers)
                response = conn.getresponse()
                conn.close()
                if response.status == 401:
                    if self.configurationOptions.jmxUseAuth == False:
                        dlg = wx.MessageDialog(self, "JMXUsername and JMXPassword required\nSet it in Config Window!", 'Cruise Control Message', wx.OK | wx.ICON_ERROR | wx.STAY_ON_TOP)
                        dlg.ShowModal()
                        dlg.Destroy()
                    else:
                        conn.connect()
                        headers = {"Content-type": "application/x-www-form-urlencoded","Accept": "text/plain","Authorization": "Basic "+ base64.standard_b64encode(self.configurationOptions.jmxUserName +":"+ self.configurationOptions.jmxPassWord)}
                        conn.request("POST", "/getattribute", "objectname=CruiseControl%20Manager:id=unique&attribute=Projects&format=collection&template=viewcollection",headers)
                        response = conn.getresponse()
                        conn.close()
                if response.status == 403:
                    dlg = wx.MessageDialog(self, "Wrong JMXUsername or JMXPassword\nSet it in Config Window!", 'Cruise Control Message', wx.OK | wx.ICON_ERROR | wx.STAY_ON_TOP)
                    dlg.ShowModal()
                    dlg.Destroy()
                    
                data = response.read()
                k = 0
                i = data.count("Project")/2

# Menu Project Menu dynamically for this session!
            
                try:
                    start = data.index("Project")+8
                    
                    while i > k:
                        try:                    
                            end = data.index(":",start)
                            loop = end - start
                            pname=''
                            t = 0
                            j = 0
                            while loop > t:
                                pname = pname + data[start+t]
                                t=t+1
                            self.submenu.Append(self.TBMENU_DYNAMIC + k, pname)
                            self.submenu2.Append(self.TBMENU_DYNAMIC2 + k, pname, "", wx.ITEM_CHECK)
                            self.submenu3.Append(self.TBMENU_DYNAMIC3 + k, pname)
                            self.buildIsBrokenStatus.append("unknown")
                            self.tbicon.Bind(wx.EVT_MENU, self.EVT_DMENU, id=self.TBMENU_DYNAMIC + k)
                            self.tbicon.Bind(wx.EVT_MENU, self.EVT_DMENU2, id=self.TBMENU_DYNAMIC2 + k)
                            self.tbicon.Bind(wx.EVT_MENU, self.EVT_DMENU3, id=self.TBMENU_DYNAMIC3 + k)
                            self.project.append(pname)
                            self.data.SetValue(k,0,pname)
                            end = data.index("Project",end)+7
                            start = data.index("Project",end)+8
                        except ValueError:
                            pass
                        k=k+1                
                except ValueError:
                    pass
            except IOError:
                dlg = wx.MessageDialog(self, "Internal Application Failure!", 'Cruise Control Message', wx.OK | wx.ICON_ERROR | wx.STAY_ON_TOP)
                dlg.ShowModal()
                dlg.Destroy()
            except socket.error:
                self.configurationOptions.cruiseRemotePortActive = False
                self.configurationOptions.jmxUseAuth = False
                dlg = wx.MessageDialog(self, "Connection refused!", 'Cruise Control Message', wx.OK | wx.ICON_ERROR | wx.STAY_ON_TOP)
                dlg.ShowModal()
                dlg.Destroy()    
            self.fenster = GridFrame(self,self.data)                
        return


# Opens the URL in a Webbrowser
# Note: There must be a registered Standard Webbrowser!!!

    def OnTaskBarLeftDClick(self, evt):
        h = self.configurationOptions.cruiseUrl.split('/')
        indexUrl=h[0]+"//"+h[2]+'/'+h[3]
        thread.start_new(webbrowser.open,(indexUrl,))
        return

# Closes the Application
# Save the Configfile if needed
        
    def OnTaskBarRightDClick(self, evt):
        if self.f!=0: self.f.Close(True)

        if self.configurationOptions.saveConfig == True:
            ptr = file(self.configurationOptions.configFile,'w')            
            ptr.write(self.configHelpText)
            cfg1 = ConfigParser.ConfigParser()
            cfg1.add_section("trayprop")
            cfg1.set("trayprop","url",self.configurationOptions.cruiseUrl)
            cfg1.set("trayprop","failString",self.configurationOptions.buildFailedString)
            cfg1.set("trayprop","refreshTime",str(self.configurationOptions.pollingDelay))
            cfg1.set("trayprop","showDialog",str(int(self.configurationOptions.shouldShowStatusChangeDialog)))
            cfg1.set("trayprop","saveConfig",str(int(self.configurationOptions.saveConfig)))
            cfg1.set("trayprop","remotePortActive",str(int(self.configurationOptions.cruiseRemotePortActive)))
            cfg1.set("trayprop","remotePort",str(self.configurationOptions.cruiseRemotePort))
            cfg1.set("trayprop","multipleOn",str(int(self.configurationOptions.multipleOn)))
            cfg1.set("trayprop","multipleProjects",str(self.configurationOptions.multipleProjects))
            cfg1.set("trayprop","useAuth",str(int(self.configurationOptions.useAuth)))
            cfg1.set("trayprop","userName",str(self.configurationOptions.userName))
            cfg1.set("trayprop","passwd",str(self.configurationOptions.passwd))
            cfg1.set("trayprop","jmxUseAuth",str(int(self.configurationOptions.jmxUseAuth)))
            cfg1.set("trayprop","jmxUserName",str(self.configurationOptions.jmxUserName))
            cfg1.set("trayprop","jmxPassWord",str(self.configurationOptions.jmxPassWord))
            cfg1.write(ptr)
            ptr.flush()
            ptr.close()

        else:
            if self.configurationOptions.confFileExists == True:
                cfg1 = ConfigParser.ConfigParser()
                cfg1.read(self.configurationOptions.configFile)
                ptr = file(self.configurationOptions.configFile,'w')
                ptr.write(self.configHelpText)
                cfg1.set("trayprop","saveConfig",str(int(self.configurationOptions.saveConfig)))
                cfg1.write(ptr)
                ptr.flush()
                ptr.close()

        self.tbicon.RemoveIcon()        
        self.Close(True)
        wx.GetApp().ProcessIdle()
        wx.Exit()    
        return

# Shows the ABOUT window

    def OnInfo(self,evt):
        try:
            f = open("README.txt", "r")
            msg = "CruiseControl Tray Icon made by CC-CONTRIB, modified by Ali Eyertas (eyertas@gmx.de) 2005\n\n" + f.read()
            f.close()
        except IOError:
            dlg3 = wx.MessageDialog(self, "Error reading file 'README.txt'!", 'Cruise Control Message', wx.OK | wx.ICON_ERROR | wx.STAY_ON_TOP)
            dlg3.ShowModal()
            dlg3.Destroy()
        dlg3 = wx.lib.dialogs.ScrolledMessageDialog(self, msg, "Info")
        dlg3.ShowModal()
        dlg3.Destroy()
        return

# Set the Configwindow to the original size

    def OnResize(self,evt):
        self.f.SetSize(wx.Size(400,520))
        return

# Create and shows the Configwindow

        
    def OnTaskBarActivate(self, evt):
        self.icontimer.Stop()
        self.enableConfig = 0
        self.f= wx.Dialog(self,1050,"Configuration",wx.Point(400,200),wx.Size(400,520))
        self.f.SetBackgroundColour(wx.Colour(255,255,255))
        self.f.SetIcon(wx.Icon("py.ico",wx.BITMAP_TYPE_ICO))
        self.f.Fit()
        self.f.Bind(wx.EVT_SIZE,self.OnResize)
        
        url = wx.StaticText( self.f, 1, "URL:",wx.Point(10,10))
        fUrl = url.GetFont()
        fUrl.SetPointSize(12)
        url.SetFont(fUrl)       

        failString = wx.StaticText( self.f, 2, "Fail String:",wx.Point(10,40))
        failString.SetFont(fUrl)        
        
        ref = wx.StaticText( self.f, 3, "Refresh Time:" ,wx.Point(10,70))
        ref.SetFont(fUrl)

        port = wx.StaticText( self.f, 4, "Remote Port active?:" ,wx.Point(10,100))
        port.SetFont(fUrl)

        jmxauth = wx.StaticText( self.f, 5, "Use JMXAuthentication?:" ,wx.Point(10,140))
        jmxauth.SetFont(fUrl)

        jmxuser = wx.StaticText( self.f, 6, "JMX-Username:" ,wx.Point(10,180))
        jmxuser.SetFont(fUrl)

        jmxpwd = wx.StaticText( self.f, 7, "JMX-Password:" ,wx.Point(10,220))
        jmxpwd.SetFont(fUrl)         

        auth = wx.StaticText( self.f, 8, "Use WEBAuthentication?:" ,wx.Point(10,260))
        auth.SetFont(fUrl)

        user = wx.StaticText( self.f, 9, "WEB-Username:" ,wx.Point(10,300))
        user.SetFont(fUrl)

        pwd = wx.StaticText( self.f, 10, "WEB-Password:" ,wx.Point(10,340))
        pwd.SetFont(fUrl)        
       

        shDiag = wx.StaticText( self.f, 11, "Show Dialog on Change:", wx.Point(10,380))
        shDiag.SetFont(fUrl)

        saveConfig = wx.StaticText( self.f, 12, "Save Config to File:", wx.Point(10,420))
        saveConfig.SetFont(fUrl)

        uTip = wx.ToolTip("URL to Index or to Project Page")        
        uTip.Enable(True)
        fTip = wx.ToolTip("String, that indicates a broken build. For Index Page 'failed', for Project Page 'BUILD FAILED'")        
        fTip.Enable(True)
        rTip = wx.ToolTip("Icon Refresh Time in sec")        
        rTip.Enable(True)
        reTip = wx.ToolTip("Remote Port! Default: 8000")        
        reTip.Enable(True)
        userTip = wx.ToolTip("Username for Webserver Authentication")        
        userTip.Enable(True)
        pwdTip = wx.ToolTip("Password for Webserver Authentication")        
        pwdTip.Enable(True)
        jmxuserTip = wx.ToolTip("Username for JMX-Server Authentication")        
        jmxuserTip.Enable(True)
        jmxpwdTip = wx.ToolTip("Password for JMX-Server Authentication")        
        jmxpwdTip.Enable(True)
                

        self.teUrl = wx.TextCtrl(self.f,13,self.configurationOptions.cruiseUrl,wx.Point(195,10),wx.Size(195,20))
        self.teUrl.SetToolTip(uTip)
        self.teFaStr = wx.TextCtrl(self.f,14,self.configurationOptions.buildFailedString,wx.Point(195,40),wx.Size(195,20))
        self.teFaStr.SetToolTip(fTip)
        self.teRef = masked.NumCtrl(self.f, value=self.configurationOptions.pollingDelay/1000, integerWidth=5, allowNegative=False)
        self.teRef.SetPosition(wx.Point(195,70))
        self.teRef.SetSize(wx.Size(90,20))
        secondLabel = wx.StaticText( self.f, 15, "seconds",wx.Point(290,75))
        self.teRef.SetToolTip(rTip)
        self.teJUser = wx.TextCtrl(self.f,16,self.configurationOptions.jmxUserName,wx.Point(195,180),wx.Size(195,20))
        self.teJUser.SetToolTip(jmxuserTip)
        self.teJPwd = wx.TextCtrl(self.f,17,self.configurationOptions.jmxPassWord,wx.Point(195,220),wx.Size(195,20))
        self.teJPwd.SetToolTip(jmxpwdTip)
        if self.configurationOptions.jmxUseAuth == False:
            self.teJUser.Enable(False)
            self.teJPwd.Enable(False)        
        self.teUser = wx.TextCtrl(self.f,18,self.configurationOptions.userName,wx.Point(195,300),wx.Size(195,20))
        self.teUser.SetToolTip(userTip)
        self.tePwd = wx.TextCtrl(self.f,19,self.configurationOptions.passwd,wx.Point(195,340),wx.Size(195,20))
        self.tePwd.SetToolTip(pwdTip)
        if self.configurationOptions.useAuth == False:
            self.teUser.Enable(False)
            self.tePwd.Enable(False)

        self.teRe = masked.NumCtrl(self.f, value=self.configurationOptions.cruiseRemotePort, integerWidth=5, allowNegative=False)
        self.teRe.SetPosition(wx.Point(290,100))
        self.teRe.SetSize(wx.Size(100,20))
        self.teRe.SetMax(65535)
        self.teRe.SetToolTip(reTip)
        if self.configurationOptions.cruiseRemotePortActive == False:
            self.teRe.Enable(False)

        btOk = wx.Button(self.f,20,"OK",wx.Point(195,460))
        btCa = wx.Button(self.f,21,"Cancel",wx.Point(315,460))

        btOk.Bind(wx.EVT_LEFT_UP, self.OnConfOk)
        btCa.Bind(wx.EVT_LEFT_UP, self.OnCloseConfig)

        sampleList = ['Yes', 'No']        

        self.rbRe = wx.RadioBox(self.f,22, "", wx.Point(195,90), wx.DefaultSize, sampleList, 2, wx.RA_SPECIFY_COLS | wx.NO_BORDER )        
        self.rbRe.SetToolTip(wx.ToolTip("Is Remote Control active?"))
        if self.configurationOptions.cruiseRemotePortActive == True:
            self.rbRe.SetSelection(0)
        else:
            self.rbRe.SetSelection(1)

        self.rbRe.Bind(wx.EVT_RADIOBOX, self.EvtRadioBoxRBRE)

        self.rbJAu = wx.RadioBox(self.f,23, "", wx.Point(195,130), wx.DefaultSize, sampleList, 2, wx.RA_SPECIFY_COLS | wx.NO_BORDER )        
        self.rbJAu.SetToolTip(wx.ToolTip("Need Authentication on JMX-Server"))
        if self.configurationOptions.jmxUseAuth == True:
            self.rbJAu.SetSelection(0)
        else:
            self.rbJAu.SetSelection(1)
        if self.configurationOptions.cruiseRemotePortActive == False:
            self.rbJAu.Enable(False)
        

        self.rbJAu.Bind(wx.EVT_RADIOBOX, self.EvtRadioBoxRBJAU)        

        self.rbAu = wx.RadioBox(self.f,24, "", wx.Point(195,250), wx.DefaultSize, sampleList, 2, wx.RA_SPECIFY_COLS | wx.NO_BORDER )        
        self.rbAu.SetToolTip(wx.ToolTip("Need Authentication on Webserver"))
        if self.configurationOptions.useAuth == True:
            self.rbAu.SetSelection(0)
        else:
            self.rbAu.SetSelection(1)

        self.rbAu.Bind(wx.EVT_RADIOBOX, self.EvtRadioBoxRBAU)        

        self.rb = wx.RadioBox(self.f,25, "", wx.Point(195,370), wx.DefaultSize, sampleList, 2, wx.RA_SPECIFY_COLS | wx.NO_BORDER )        
        self.rb.SetToolTip(wx.ToolTip("Show DialogBox when Buildstaus changes?"))
        if self.configurationOptions.shouldShowStatusChangeDialog == True:
            self.rb.SetSelection(0)
        else:
            self.rb.SetSelection(1)

        self.rbSav = wx.RadioBox(self.f,26, "", wx.Point(195,410), wx.DefaultSize, sampleList, 2, wx.RA_SPECIFY_COLS | wx.NO_BORDER )        
        self.rbSav.SetToolTip(wx.ToolTip("Save configuration to 'property.ini' on EXIT ?"))
        if self.configurationOptions.saveConfig == True:
            self.rbSav.SetSelection(0)
        else:
            self.rbSav.SetSelection(1)
        
        self.f.Bind(wx.EVT_CLOSE, self.OnCloseConfig)
        self.f.MakeModal(True)
        self.f.ShowModal()        
        return
        
# Make some test on the Textfields

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
            dlg2 = wx.MessageDialog(self.f, "The URL musst start with 'http://'", 'Cruise Control Message', wx.OK | wx.ICON_ERROR | wx.STAY_ON_TOP)
            dlg2.ShowModal()
            dlg2.Destroy()
            self.teUrl.SetValue(self.configurationOptions.cruiseUrl)
            return
        if self.teUrl.GetValue().__len__()<= 7:
            dlg2 = wx.MessageDialog(self.f, "The URL is incomplete!", 'Cruise Control Message', wx.OK | wx.ICON_ERROR | wx.STAY_ON_TOP)
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
        #self.icontimer.Stop()
        self.icontimer.Start(self.configurationOptions.pollingDelay)

        if self.rbRe.GetSelection() == 0:
            self.configurationOptions.cruiseRemotePortActive = True
            self.configurationOptions.cruiseRemotePort = self.teRe.GetValue()
            if self.rbJAu.GetSelection() == 0:
                self.configurationOptions.jmxUseAuth = True
                if self.teJUser.GetValue().__len__()== 0:
                    dlg2 = wx.MessageDialog(self.f, "JMX-Username not typed!", 'Cruise Control Message', wx.OK | wx.ICON_ERROR | wx.STAY_ON_TOP)
                    dlg2.ShowModal()
                    dlg2.Destroy()
                    self.teJUser.SetValue(self.configurationOptions.jmxUserName)
                    return
                if self.teJUser.GetValue().isspace() == True:
                    dlg2 = wx.MessageDialog(self.f, "JMX-Username not typed!", 'Cruise Control Message', wx.OK | wx.ICON_ERROR | wx.STAY_ON_TOP)
                    dlg2.ShowModal()
                    dlg2.Destroy()
                    self.teJUser.SetValue(self.configurationOptions.jmxUserName)
                    return
                if self.teJPwd.GetValue().__len__()== 0:
                    dlg2 = wx.MessageDialog(self.f, "JMX-Password not typed!", 'Cruise Control Message', wx.OK | wx.ICON_ERROR | wx.STAY_ON_TOP)
                    dlg2.ShowModal()
                    dlg2.Destroy()
                    self.teJPwd.SetValue(self.configurationOptions.jmxPassWord)
                    return
                if self.teJPwd.GetValue().isspace() == True:
                    dlg2 = wx.MessageDialog(self.f, "JMX-Password not typed!", 'Cruise Control Message', wx.OK | wx.ICON_ERROR | wx.STAY_ON_TOP)
                    dlg2.ShowModal()
                    dlg2.Destroy()
                    self.teJPwd.SetValue(self.configurationOptions.jmxPassWord)
                    return
                self.configurationOptions.jmxUserName = self.teJUser.GetValue()
                self.configurationOptions.jmxPassWord = self.teJPwd.GetValue()
            else:
                self.configurationOptions.jmxUseAuth = False
            self.DynamicMenu()
            self.StartMultipleProjects()
        else:
            self.configurationOptions.cruiseRemotePortActive = False

        if self.rbAu.GetSelection() == 0:
            self.configurationOptions.useAuth = True
            if self.teUser.GetValue().__len__()== 0:
                dlg2 = wx.MessageDialog(self.f, "Web-Username not typed!", 'Cruise Control Message', wx.OK | wx.ICON_ERROR | wx.STAY_ON_TOP)
                dlg2.ShowModal()
                dlg2.Destroy()
                self.teUser.SetValue(self.configurationOptions.userName)
                return
            if self.teUser.GetValue().isspace() == True:
                dlg2 = wx.MessageDialog(self.f, "Web-Username not typed!", 'Cruise Control Message', wx.OK | wx.ICON_ERROR | wx.STAY_ON_TOP)
                dlg2.ShowModal()
                dlg2.Destroy()
                self.teUser.SetValue(self.configurationOptions.userName)
                return
            if self.tePwd.GetValue().__len__()== 0:
                dlg2 = wx.MessageDialog(self.f, "Web-Password not typed!", 'Cruise Control Message', wx.OK | wx.ICON_ERROR | wx.STAY_ON_TOP)
                dlg2.ShowModal()
                dlg2.Destroy()
                self.tePwd.SetValue(self.configurationOptions.passwd)
                return
            if self.tePwd.GetValue().isspace() == True:
                dlg2 = wx.MessageDialog(self.f, "Web-Password not typed!", 'Cruise Control Message', wx.OK | wx.ICON_ERROR | wx.STAY_ON_TOP)
                dlg2.ShowModal()
                dlg2.Destroy()
                self.tePwd.SetValue(self.configurationOptions.passwd)
                return
            self.configurationOptions.userName = self.teUser.GetValue()
            self.configurationOptions.passwd = self.tePwd.GetValue()
            
        else:
            self.configurationOptions.useAuth = False            
     

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

# Close the Configwindow

    def OnCloseConfig(self, evt):        
        
        self.enableConfig = 1 
        self.f.Destroy()
        self.f = 0
        self.icontimer.Start(self.configurationOptions.pollingDelay)
        return
        
# Shows the Popupmenu
        
    def OnTaskBarMenu(self, evt):

        subM = wx.Menu()
        subM3 = wx.Menu()
        self.icontimer.Stop()
        mList = self.submenu.GetMenuItems()
        mList3 = self.submenu3.GetMenuItems()
        mCount = self.submenu3.GetMenuItemCount()
        k=0
        self.menu.Remove(1007)
        self.menu.Remove(1009)   
        while k<mCount :
            if self.submenu.IsEnabled(mList[k].GetId()) == True :
                subM.AppendItem(mList[k])
                subM3.AppendItem(mList3[k])
            k=k+1
                    
        self.menu.InsertMenu(4,1007,"Open Projectpage",subM3)
        self.menu.InsertMenu(6,1009,"ForceBuild",subM)

        if self.configurationOptions.cruiseRemotePortActive == True:
            self.menu.Enable(1009,True)
            self.menu.Enable(1008,True)
            self.menu.Enable(1007,True)
        else:
            self.menu.Enable(1009,False)
            self.menu.Enable(1008,False)
            self.menu.Enable(1007,False)
        
        if self.enableConfig == 1:
            self.menu.Enable(self.TBMENU_CONFIG,True)
        else:
            self.menu.Enable(self.TBMENU_CONFIG,False)
        self.icontimer.Start(self.configurationOptions.pollingDelay)
        if self.showPopupMenu == True:
            self.tbicon.PopupMenu(self.menu)
        #self.icontimer.Start(self.configurationOptions.pollingDelay)
        #menu.Destroy()
        return

    def EvtRadioBoxRBRE(self,evt):
        if evt.GetInt() == 0:
            self.teRe.Enable(True)
            self.rbJAu.Enable(True)
            if self.rbJAu.GetSelection() == 0 :
                self.teJUser.Enable(True)
                self.teJPwd.Enable(True)
            else:
                self.teJUser.Enable(False)
                self.teJPwd.Enable(False)
        else:
            self.teRe.Enable(False)
            self.rbJAu.Enable(False)
            self.teJUser.Enable(False)
            self.teJPwd.Enable(False)
        return
    
    def EvtRadioBoxRBJAU(self,evt):
        if evt.GetInt() == 0:
            self.teJUser.Enable(True)
            self.teJPwd.Enable(True)
        else:
            self.teJUser.Enable(False)
            self.teJPwd.Enable(False)
        return   

    def EvtRadioBoxRBAU(self,evt):
        if evt.GetInt() == 0:
            self.teUser.Enable(True)
            self.tePwd.Enable(True)
        else:
            self.teUser.Enable(False)
            self.tePwd.Enable(False)
        return    
    
  
# Eventhandler for the dynamically created menuitems  (force build)
    def EVT_MULTIPLE(self,evt):
        self.icontimer.Stop()
        self.showPopupMenu=False
        if self.configurationOptions.multipleOn == False :
            len=self.data.GetNumberRows()
            k=0
            while k<len :
                self.data.SetValue(k,1,0)
                k=k+1
            if self.projectNumber != -1 :
                self.data.SetValue(self.projectNumber,1,1)
        self.fenster.Show(True)
        if not self.fenster.AcceptsFocus():
            self.fenster.Enable(True)
        return                


    


    def EVT_DMENU(self,evt):
        try:            
            headers = {"Content-type": "application/x-www-form-urlencoded","Accept": "text/plain"}
            conn = httplib.HTTPConnection((self.configurationOptions.cruiseUrl.split(':')[1]).split("//")[1]+':'+ str(self.configurationOptions.cruiseRemotePort))
            conn.request("POST", "/invoke", "operation=build&objectname=CruiseControl+Project%3Aname%3D"+self.project[evt.GetId()-1010],headers)
            response = conn.getresponse()
            conn.close()
            if response.status == 401:
                if self.configurationOptions.jmxUseAuth == False:
                    dlg = wx.MessageDialog(self, "JMXUsername and JMXPassword required\nSet it in Config Window!", 'Cruise Control Message', wx.OK | wx.ICON_ERROR | wx.STAY_ON_TOP)
                    dlg.ShowModal()
                    dlg.Destroy()
                    return
                else:
                    conn.connect()
                    headers = {"Content-type": "application/x-www-form-urlencoded","Accept": "text/plain","Authorization": "Basic "+ base64.standard_b64encode(self.configurationOptions.jmxUserName +":"+ self.configurationOptions.jmxPassWord)}
                    conn.request("POST", "/invoke", "operation=build&objectname=CruiseControl+Project%3Aname%3D"+self.project[evt.GetId()-1010],headers)
                    response = conn.getresponse()
                    conn.close()
            elif response.status == 403:
                dlg = wx.MessageDialog(self, "Wrong JMXUsername or JMXPassword\nISet it in Config Window!", 'Cruise Control Message', wx.OK | wx.ICON_ERROR | wx.STAY_ON_TOP)
                dlg.ShowModal()
                dlg.Destroy()
                return
            data = response.read()
            self.showMessage("Build wird erstellt!")
        except IOError:
            dlg = wx.MessageDialog(self, "Internal Application Failure!", 'Cruise Control Message', wx.OK | wx.ICON_ERROR | wx.STAY_ON_TOP)
            dlg.ShowModal()
            dlg.Destroy()
        except socket.error:
            self.configurationOptions.cruiseRemotePortActive = False
            dlg = wx.MessageDialog(self, "Connection refused!", 'Cruise Control Message', wx.OK | wx.ICON_ERROR | wx.STAY_ON_TOP)
            dlg.ShowModal()
            dlg.Destroy()
        return

# Eventhandler for the dynamically created menuitems (choose Project)

    def EVT_DMENU2(self,evt):
        h = self.configurationOptions.cruiseUrl.split('/')
        indexUrl=h[0]+"//"+h[2]+'/'+h[3]
        if evt.GetId() == 1999:
            self.configurationOptions.cruiseUrl = indexUrl
            self.configurationOptions.buildFailedString = "failed"
            self.configurationOptions.multipleOn = False
            self.menu.Check(1999, True)
            self.projectNumber = -1
            
            try:
                count = self.submenu.GetMenuItemCount()
                menulist=self.submenu.GetMenuItems()
                count2 = self.submenu2.GetMenuItemCount()
                menulist2=self.submenu2.GetMenuItems()
                menulist3=self.submenu3.GetMenuItems()
                while count>0:
                    count=count-1
                    count2=count2-1
                    self.submenu.Enable(menulist[count].GetId(),True)
                    self.submenu3.Enable(menulist3[count].GetId(),True)
                    self.menu.Check(menulist2[count2].GetId(),False)
            except IndexError:
                pass
            self.CheckBuild("Whatever")
        else:
            try:
                self.configurationOptions.cruiseUrl = indexUrl + "/buildresults/" + self.project[evt.GetId()-2000]
                self.configurationOptions.buildFailedString = "BUILD FAILED"
                self.configurationOptions.multipleOn = False
                self.menu.Check(evt.GetId(), True)
                self.menu.Check(1999, False)
                self.projectNumber = evt.GetId()-2000
                try:
                    count = self.submenu.GetMenuItemCount()
                    menulist=self.submenu.GetMenuItems()
                    count2 = self.submenu2.GetMenuItemCount()
                    menulist2=self.submenu2.GetMenuItems()
                    menulist3=self.submenu3.GetMenuItems()
                    if self.projectNumber != -1:
                        id=self.submenu.FindItem(self.project[self.projectNumber])
                        self.submenu.Enable(id,True)
                        id2=self.submenu3.FindItem(self.project[self.projectNumber])
                        self.submenu3.Enable(id2,True)
                        while count>0:
                            count=count-1
                            count2=count2-1
                            if id != menulist[count].GetId():
                                self.submenu.Enable(menulist[count].GetId(),False)
                                self.submenu3.Enable(menulist3[count].GetId(),False)
                                self.menu.Check(menulist2[count2].GetId(),False)
                except IndexError:
                    pass
                self.CheckBuild("Whatever")
            except IndexError:
                dlg = wx.MessageDialog(self, "Internal Application Failure!", 'Cruise Control Message', wx.OK | wx.ICON_ERROR | wx.STAY_ON_TOP)
                dlg.ShowModal()
                dlg.Destroy()
        return

# Eventhandler for the dynamically created menuitems (open projekt webpage)

    def EVT_DMENU3(self,evt):
        try:
            h = self.configurationOptions.cruiseUrl.split('/')
            indexUrl=h[0]+"//"+h[2]+'/'+h[3] + "/buildresults/" + self.project[evt.GetId()-3000]
            thread.start_new(webbrowser.open,(indexUrl,))
        except IndexError:
            dlg = wx.MessageDialog(self, "Internal Application Failurer!", 'Cruise Control Message', wx.OK | wx.ICON_ERROR | wx.STAY_ON_TOP)
            dlg.ShowModal()
            dlg.Destroy()
        return
        

        
    def SetIconTimer(self):
        wxId = wx.NewId()
        self.icontimer = wx.Timer(self, wxId)
        wx.EVT_TIMER(self, wxId, self.CheckBuild)
        self.icontimer.Start(self.configurationOptions.pollingDelay)
        return

    def CheckBuild(self, evt):
        self.icontimer.Stop()
        text=''      
        test=False
        previousIsBrokenStatus=False
        
        if self.configurationOptions.multipleOn == False:
            
            if self.projectNumber == -1:
                previousIsBrokenStatus = self.buildIsBrokenStatus[0]
                if previousIsBrokenStatus == "unknown":
                    text = "INDEX PAGE: "+self.indexFailedProject
                    icon = wx.Icon('question.ico', wx.BITMAP_TYPE_ICO)            
                    self.tbicon.SetIcon(icon,"STATUS of "+text+" is UNKNOWN")                    
                self.buildIsBrokenStatus[0] = self.buildStatus()
                test = self.buildIsBrokenStatus[0]
                text = "INDEX PAGE: "+self.indexFailedProject
            else:
                text = self.project[self.projectNumber]
                previousIsBrokenStatus = self.buildIsBrokenStatus[self.projectNumber+1]
                if previousIsBrokenStatus == "unknown":
                    text = "INDEX PAGE: "+self.indexFailedProject
                    icon = wx.Icon('question.ico', wx.BITMAP_TYPE_ICO)            
                    self.tbicon.SetIcon(icon,"STATUS of "+text+" is UNKNOWN")
                self.buildIsBrokenStatus[self.projectNumber+1] = self.buildStatus()
                test = self.buildIsBrokenStatus[self.projectNumber+1]        
        
            if test:
                icon = wx.Icon('red.ico', wx.BITMAP_TYPE_ICO)            
                self.tbicon.SetIcon(icon,"Status of\n'"+text+"'\nis FAILURE")
                self.lastIndexFailedProject = self.indexFailedProject
            else:
                if self.projectNumber==-1:
                    text=text+self.lastIndexFailedProject
                icon = wx.Icon('green.ico', wx.BITMAP_TYPE_ICO)
                self.tbicon.SetIcon(icon,"Status of\n'"+text+"'\nis OK")        
            self.indexFailedProject = ''
            self.icontimer.Start(self.configurationOptions.pollingDelay)
            if previousIsBrokenStatus == "unknown":
                return
            if previousIsBrokenStatus and test:
                return self.stillBroken(text)
            if previousIsBrokenStatus and not test:
                return self.buildFixed(text)
            if not previousIsBrokenStatus and test:
                return self.buildBroken(text)
            if not previousIsBrokenStatus and not test:
                return self.stillWorking(text)
            return
        else :
            count = self.projectPos.__len__()
            k=0
            previousIsBrokenStatus=False
            while k<count :
                try:
                    h = self.configurationOptions.cruiseUrl.split('/')
                    indexUrl=h[0]+"//"+h[2]+'/'+h[3]
                    self.configurationOptions.cruiseUrl = indexUrl + "/buildresults/" + self.project[self.projectPos[k]]
                    self.configurationOptions.buildFailedString = "BUILD FAILED"
                except IndexError:
                    dlg = wx.MessageDialog(self, "Internal Application Failure!", 'Cruise Control Message', wx.OK | wx.ICON_ERROR | wx.STAY_ON_TOP)
                    dlg.ShowModal()
                    dlg.Destroy()
                    
                text = self.project[self.projectPos[k]]
                previousIsBrokenStatus = self.buildIsBrokenStatus[self.projectPos[k]+1]
                self.buildIsBrokenStatus[self.projectPos[k]+1] = self.buildStatus()
                test = self.buildIsBrokenStatus[self.projectPos[k]+1]
        
                if previousIsBrokenStatus == "unknown":
                    icon = wx.Icon('question.ico', wx.BITMAP_TYPE_ICO)            
                    self.tbicon.SetIcon(icon,"STATUS of "+text+" is UNKNOWN")
                    k=k+1
                    continue
                if previousIsBrokenStatus and test:
                    self.stillBroken(text)
                if previousIsBrokenStatus and not test:
                    self.buildFixed(text)
                if not previousIsBrokenStatus and test:
                    self.buildBroken(text)
                if not previousIsBrokenStatus and not test:
                    self.stillWorking(text)
                k=k+1

            k=0
            text=''
            brokenText=''
            test=False
            while k<count :
                if self.buildIsBrokenStatus[self.projectPos[k]+1] :
                    brokenText = brokenText +'|'+ self.project[self.projectPos[k]] + "=Faild"
                else :
                    text= text +'|'+ self.project[self.projectPos[k]] + '=OK'
                test = test or self.buildIsBrokenStatus[self.projectPos[k]+1]
                k=k+1                
            brokenText=brokenText + '|'    
            text=text + '|'
            if test:
                icon = wx.Icon('red.ico', wx.BITMAP_TYPE_ICO)            
                self.tbicon.SetIcon(icon,brokenText)
            else:
                icon = wx.Icon('green.ico', wx.BITMAP_TYPE_ICO)
                self.tbicon.SetIcon(icon,text)
            self.icontimer.Start(self.configurationOptions.pollingDelay)
            return           
        

    def showMessage(self, message):
        if self.dlg != None:
            return
        
        self.dlg = wx.MessageDialog(self, message, 'Cruise Control Message', wx.OK | wx.ICON_INFORMATION | wx.STAY_ON_TOP)
        
        self.dlg.ShowModal()
        self.dlg.Destroy()
        self.dlg = None
        return

    def stillBroken(self, message):
        #self.showMessage("Project "+message+": Build is still broken")
        pass

    def buildFixed(self, message):
        self.showMessage("Project "+message+": Build has been fixed")
        return
    
    def buildBroken(self, message):
        self.showMessage("Project "+message+": Build has been fixed")
        return
    
    def stillWorking(self, message):
        #self.showMessage("Project "+message+": Build is still working")
        pass
    
class CruiseWatchingTrayIconApp(wx.App):
    def __init__(self, someNumber, configurationOptions):
        self.configurationOptions = configurationOptions
        wx.App.__init__(self, someNumber)
        return
        
    def OnInit(self):
        frame = CruiseWatchingTrayIconFrame(self.configurationOptions)
        frame.Center(wx.BOTH)
        frame.Show(False)
        return True

def usage():
    print """available options are:
            -u urlOfCruiseWebPage (including http://) (std:http://localhost:8080/cruisecontrol)
            -p CruiseControl Remote Port (std: notActive)
            -q (quiet = no dialog box on status change)
            -d pollingDelayInSeconds (how long it waits between polling cruise) (std:60)
            -b buildFailedString (existence on cruise page means build broken) (std:failed)
            -c loadConfig from configfile which is given as parameter
            -s saveConfig to 'property.ini'
            -h (help - show this message)"""
    return
    
def main():
    try:
        opts, noArgsExpected = getopt(sys.argv[1:], "qhsp:c:d:u:b:", [])
    except GetoptError:
        usage()
        sys.exit(2)
    configurationOptions = ConfigurationOptions()
    for o, a in opts:
        if o == "-q":
            configurationOptions.shouldShowStatusChangeDialog = False
        if o == "-p":
            configurationOptions.cruiseRemotePortActive = True 
            configurationOptions.cruiseRemotePort = int(a)
        if o == "-d":
            configurationOptions.pollingDelay = int(a) * oneSecond
        if o == "-u":
            configurationOptions.cruiseUrl = a
        if o == "-b":
            configurationOptions.buildFailedString = a
        if o == "-c":
            configurationOptions.configFile = a
            try:
                ptr = file(configurationOptions.configFile,'r')
                ptr.close()
                cfg = ConfigParser.ConfigParser()
                cfg.read(configurationOptions.configFile)
                configurationOptions.cruiseUrl = cfg.get("trayprop","url")
                configurationOptions.buildFailedString = cfg.get("trayprop","failString")
                configurationOptions.pollingDelay = cfg.getint("trayprop","refreshTime")
                configurationOptions.shouldShowStatusChangeDialog = cfg.getboolean("trayprop","showDialog")
                configurationOptions.cruiseRemotePortActive = cfg.getboolean("trayprop","remotePortActive")
                if configurationOptions.cruiseRemotePortActive :
                    configurationOptions.cruiseRemotePort = cfg.getint("trayprop","remotePort")
                configurationOptions.multipleOn = cfg.getboolean("trayprop","multipleOn")
                if configurationOptions.multipleOn :
                    configurationOptions.multipleProjects = cfg.get("trayprop","multipleProjects")
                configurationOptions.useAuth = cfg.getboolean("trayprop","useAuth")
                if configurationOptions.useAuth :
                    configurationOptions.userName = cfg.get("trayprop","userName")
                    configurationOptions.passwd = cfg.get("trayprop","passwd")
                configurationOptions.jmxUseAuth = cfg.getboolean("trayprop","jmxUseAuth")
                if configurationOptions.jmxUseAuth :
                    configurationOptions.jmxUserName = cfg.get("trayprop","jmxUserName")
                    configurationOptions.jmxPassWord = cfg.get("trayprop","jmxPassWord")
                configurationOptions.saveConfig = cfg.getboolean("trayprop","saveConfig")
                CruiseWatchingTrayIconApp(0, configurationOptions).MainLoop()
                sys.exit()
            except IOError:
                pass 
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
        configurationOptions.cruiseRemotePortActive = cfg.getboolean("trayprop","remotePortActive")
        if configurationOptions.cruiseRemotePortActive :
            configurationOptions.cruiseRemotePort = cfg.getint("trayprop","remotePort")
        configurationOptions.multipleOn = cfg.getboolean("trayprop","multipleOn")
        if configurationOptions.multipleOn :
            configurationOptions.multipleProjects = cfg.get("trayprop","multipleProjects")
        configurationOptions.useAuth = cfg.getboolean("trayprop","useAuth")
        if configurationOptions.useAuth :
            configurationOptions.userName = cfg.get("trayprop","userName")
            configurationOptions.passwd = cfg.get("trayprop","passwd")
        configurationOptions.jmxUseAuth = cfg.getboolean("trayprop","jmxUseAuth")
        if configurationOptions.jmxUseAuth :
            configurationOptions.jmxUserName = cfg.get("trayprop","jmxUserName")
            configurationOptions.jmxPassWord = cfg.get("trayprop","jmxPassWord")
        configurationOptions.shouldShowStatusChangeDialog = cfg.getboolean("trayprop","showDialog")
        configurationOptions.saveConfig = cfg.getboolean("trayprop","saveConfig")
        
    except IOError:
        configurationOptions.confFileExists = False
        #pass 
    
    CruiseWatchingTrayIconApp(0, configurationOptions).MainLoop()
    

if __name__ == '__main__':
    main()
