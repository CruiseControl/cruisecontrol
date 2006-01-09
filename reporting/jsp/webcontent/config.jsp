<%--********************************************************************************
* CruiseControl, a Continuous Integration Toolkit
* Copyright (c) 2005 ThoughtWorks, Inc.
* 651 W Washington Ave. Suite 600
* Chicago, IL 60661 USA
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions
* are met:
*
*     + Redistributions of source code must retain the above copyright
*       notice, this list of conditions and the following disclaimer.
*
*     + Redistributions in binary form must reproduce the above
*       copyright notice, this list of conditions and the following
*       disclaimer in the documentation and/or other materials provided
*       with the distribution.
*
*     + Neither the name of ThoughtWorks, Inc., CruiseControl, nor the
*       names of its contributors may be used to endorse or promote
*       products derived from this software without specific prior
*       written permission.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
* "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
* LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
* A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
* CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
* EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
* PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
* PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
* LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
* NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
* SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
********************************************************************************--%>
<%@ page errorPage="/error.jsp" %>
<%@ taglib uri="webwork" prefix="ww" %>
<html>
<head>
    <title><ww:property value="project"/> Configuration</title>
    <link type="text/css" rel="stylesheet" href="css/cruisecontrol.css"/>
    <script type="text/javascript" language="JavaScript" src="prototype.js"></script>
    <script type="text/javascript" language="JavaScript" src="cc-config.js"></script>
</head>

<body>
<ww:if test="hasActionMessages()">
    <div id="result-messages" class="config-result-messages">
        <ul>
            <ww:iterator value="actionMessages">
                <li class="config-result-message"><ww:property/></li>
            </ww:iterator>
        </ul>
        <hr/>
    </div>
</ww:if>
<table>
    <tr>
        <td colspan="2">
            <ww:form action="reload" id="reload-configuration"
                     name="reload-configuration" method="post">
                <ww:submit value="Reload" cssClass="config-button"/>
            </ww:form>
        </td>
    </tr>
    <tr valign="top">
        <td width="50%">
            <a class="config-link" href="configured.jspa?pluginType=listener"
               onclick="loadConfiguredPlugins(this.href, 'listeners'); return false;">
                <img id="listeners-tree-icon" src="images/plus_nolines.gif"/></a>
            <a class="config-link" href="<ww:url value="details!default.jspa?pluginType=listeners&pluginName=listeners"/>"
               onclick="loadPlugin(this.href); return false;">Listeners</a>
            <a class="config-link" href="available.jspa?pluginType=listener"
               onclick="loadAvailablePlugins(this.href, 'listeners'); return false;">Add Listener</a>
            <div id="available-listeners" style="display: none; margin-left: 15%"></div>
            <hr id="listeners-hr" style="display: none;"/>
            <div id="listeners" style="display: none; margin-left: 15%"></div>

            <br/>

            <a class="config-link" href="configured.jspa?pluginType=bootstrapper"
               onclick="loadConfiguredPlugins(this.href, 'bootstrappers'); return false;">
                <img id="bootstrappers-tree-icon" src="images/plus_nolines.gif"/></a>
            <a class="config-link" href="<ww:url value="details!default.jspa?pluginType=bootstrappers&pluginName=bootstrappers"/>"
               onclick="loadPlugin(this.href); return false;">Bootstrappers</a>
            <a class="config-link" href="available.jspa?pluginType=bootstrapper"
               onclick="loadAvailablePlugins(this.href, 'bootstrappers'); return false;">Add Bootstrapper</a>
            <div id="available-bootstrappers" style="display: none; margin-left: 15%"></div>
            <hr id="bootstrappers-hr" style="display: none;"/>
            <div id="bootstrappers" style="display: none; margin-left: 15%"></div>

            <br/>

            <a class="config-link" href="configured.jspa?pluginType=sourcecontrol"
               onclick="loadConfiguredPlugins(this.href, 'sourcecontrols'); return false;">
                <img id="sourcecontrols-tree-icon" src="images/plus_nolines.gif"/></a>
            <a class="config-link" href="<ww:url value="details!default.jspa?pluginType=modificationset&pluginName=modificationset"/>"
               onclick="loadPlugin(this.href); return false;">Source Controls</a>
            <a class="config-link" href="available.jspa?pluginType=sourcecontrol"
               onclick="loadAvailablePlugins(this.href, 'sourcecontrols'); return false;">Add Source Control</a>
            <div id="available-sourcecontrols" style="display: none; margin-left: 15%"></div>
            <hr id="sourcecontrols-hr" style="display: none;"/>
            <div id="sourcecontrols" style="display: none; margin-left: 15%"></div>

            <br/>

            <a class="config-link" href="configured.jspa?pluginType=builder"
               onclick="loadConfiguredPlugins(this.href, 'builders'); return false;">
                <img id="builders-tree-icon" src="images/plus_nolines.gif"/></a>
            <a class="config-link" href="<ww:url value="details!default.jspa?pluginType=schedule&pluginName=schedule"/>"
               onclick="loadPlugin(this.href); return false;">Schedule</a>
            <a class="config-link" href="available.jspa?pluginType=builder"
               onclick="loadAvailablePlugins(this.href, 'builders'); return false;">Add Schedule</a>
            <div id="available-builders" style="display: none; margin-left: 15%"></div>
            <hr id="builders-hr" style="display: none;"/>
            <div id="builders" style="display: none; margin-left: 15%"></div>

            <br/>

            <a class="config-link" href="configured.jspa?pluginType=logger"
               onclick="loadConfiguredPlugins(this.href, 'loggers'); return false;">
                <img id="loggers-tree-icon" src="images/plus_nolines.gif"/></a>
            <a class="config-link" href="<ww:url value="details!default.jspa?pluginType=log&pluginName=log"/>"
               onclick="loadPlugin(this.href); return false;">Log</a>
            <a class="config-link" href="available.jspa?pluginType=logger"
               onclick="loadAvailablePlugins(this.href, 'loggers'); return false;">Add Log</a>
            <div id="available-loggers" style="display: none; margin-left: 15%"></div>
            <hr id="loggers-hr" style="display: none;"/>
            <div id="loggers" style="display: none; margin-left: 15%"></div>

            <br/>

            <a class="config-link" href="configured.jspa?pluginType=publisher"
               onclick="loadConfiguredPlugins(this.href, 'publishers'); return false;">
                <img id="publishers-tree-icon" src="images/plus_nolines.gif"/></a>
            <a class="config-link" href="<ww:url value="details!default.jspa?pluginType=publishers&pluginName=publishers"/>"
               onclick="loadPlugin(this.href); return false;">Publishers</a>
            <a class="config-link" href="available.jspa?pluginType=publisher"
               onclick="loadAvailablePlugins(this.href, 'publishers'); return false;">Add Publishers</a>
            <div id="available-publishers" style="display: none; margin-left: 15%"></div>
            <hr id="publishers-hr" style="display: none;"/>
            <div id="publishers" style="display: none; margin-left: 15%"></div>
        </td>
        <td width="50%">
            <iframe src="details!default.jspa?pluginType=listeners&pluginName=listeners"
                frameborder="0" height="100%" width="100%" id="plugin-details"></iframe>
        </td>
    </tr>
    <tr><td colspan="2">
        <ww:form action="save" id="project-config"
                 name="project-config" method="post">
            <ww:textarea name="contents" rows="24" cols="80"/>
            <ww:submit value="Save" cssClass="config-button"/>
        </ww:form>
    </td></tr>
</table>
</body>
</html>
