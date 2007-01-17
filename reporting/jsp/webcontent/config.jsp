<%--********************************************************************************
* CruiseControl, a Continuous Integration Toolkit
* Copyright (c) 2005 ThoughtWorks, Inc.
* 200 E. Randolph, 25th Floor
* Chicago, IL 60601 USA
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
<table width="100%"><tr><td align="center">Ready for <em>Alpha</em>-level testing</td></tr></table>

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
<table width="100%">
    <tr>
        <td width="1%">
            <form action="<ww:url value="reload.jspa"/>" id="reload-configuration"
                     name="reload-configuration" method="post">
                <input type="submit" value="Reload from server" class="config-button"/>
            </form>
        </td><td width="1%">
            <form action="<ww:url value="save.jspa"/>" id="save-configuration"
                     name="save-configuration" method="post">
                <input type="submit" value="Save to server" class="config-button"/>
            </form>
        </td>
        <td align="right">
            <a href="<ww:url value="contents.jspa"/>">
                <img border="0" src="images/xml.png" alt="Edit raw XML configuration" title="Edit raw XML configuration"/>
            </a>
        </td>
    </tr>
</table>
<table width="100%">
    <tr valign="top">
        <td width="50%" style="border-right:thin solid dimgray;">
            <a class="config-link" href="plugins.jspa?pluginType=listener"
               onclick="loadPlugins(this.href, 'listeners'); return false;">
                <img id="listeners-tree-icon" src="images/plus_nolines.gif"/></a>
            <a class="config-link" href="<ww:url value="load-details.jspa?pluginType=listeners&pluginName=listeners"/>"
               onclick="loadPlugin(this.href); return false;">Listeners</a>
            <div id="listeners" style="display: none; margin-left: 15%"></div>

            <br/>

            <a class="config-link" href="plugins.jspa?pluginType=bootstrapper"
               onclick="loadPlugins(this.href, 'bootstrappers'); return false;">
                <img id="bootstrappers-tree-icon" src="images/plus_nolines.gif"/></a>
            <a class="config-link" href="<ww:url value="load-details.jspa?pluginType=bootstrappers&pluginName=bootstrappers"/>"
               onclick="loadPlugin(this.href); return false;">Bootstrappers</a>
            <div id="bootstrappers" style="display: none; margin-left: 15%"></div>

            <br/>

            <a class="config-link" href="plugins.jspa?pluginType=sourcecontrol"
               onclick="loadPlugins(this.href, 'sourcecontrols'); return false;">
                <img id="sourcecontrols-tree-icon" src="images/plus_nolines.gif"/></a>
            <a class="config-link" href="<ww:url value="load-details.jspa?pluginType=modificationset&pluginName=modificationset"/>"
               onclick="loadPlugin(this.href); return false;">Source Controls</a>
            <div id="sourcecontrols" style="display: none; margin-left: 15%"></div>

            <br/>

            <a class="config-link" href="plugins.jspa?pluginType=builder"
               onclick="loadPlugins(this.href, 'builders'); return false;">
                <img id="builders-tree-icon" src="images/plus_nolines.gif"/></a>
            <a class="config-link" href="<ww:url value="load-details.jspa?pluginType=schedule&pluginName=schedule"/>"
               onclick="loadPlugin(this.href); return false;">Schedule</a>
            <div id="builders" style="display: none; margin-left: 15%"></div>

            <br/>

            <a class="config-link" href="plugins.jspa?pluginType=logger"
               onclick="loadPlugins(this.href, 'loggers'); return false;">
                <img id="loggers-tree-icon" src="images/plus_nolines.gif"/></a>
            <a class="config-link" href="<ww:url value="load-details.jspa?pluginType=log&pluginName=log"/>"
               onclick="loadPlugin(this.href); return false;">Log</a>
            <div id="loggers" style="display: none; margin-left: 15%"></div>

            <br/>

            <a class="config-link" href="plugins.jspa?pluginType=publisher"
               onclick="loadPlugins(this.href, 'publishers'); return false;">
                <img id="publishers-tree-icon" src="images/plus_nolines.gif"/></a>
            <a class="config-link" href="<ww:url value="load-details.jspa?pluginType=publishers&pluginName=publishers"/>"
               onclick="loadPlugin(this.href); return false;">Publishers</a>
            <div id="publishers" style="display: none; margin-left: 15%"></div>
        </td>
        <td width="50%" valign="top">
            <iframe src="load-details.jspa?pluginType=listeners&pluginName=listeners"
                frameborder="0" height="100%" width="100%" id="plugin-details"></iframe>
        </td>
    </tr>
</table>
</body>
</html>
