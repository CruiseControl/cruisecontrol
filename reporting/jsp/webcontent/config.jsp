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
<ww:if test="actionMessages != null">
    <ul>
        <ww:iterator value="actionMessages">
            <li class='config-resultmsg'><ww:property/></li>
        </ww:iterator>
    </ul>
    <hr/>
</ww:if>
<table>
    <tr>
        <td width="50%">
            <a href="plugins.jspa?project=<ww:property value="project"/>&pluginType=listener"
               onclick="loadPlugins(this.href, 'listenersID'); return false;">
                <img id="listenersID-icon" src="images/plus_nolines.gif"/>
                Listeners</a>

            <div id="listenersID" style="display: none; margin-left: 15%"></div>
            <br/>
            <a href="plugins.jspa?project=<ww:property value="project"/>&pluginType=bootstrapper"
               onclick="loadPlugins(this.href, 'bootstrappersID'); return false;">
                <img id="bootstrappersID-icon" src="images/plus_nolines.gif"/>
                Bootstrappers</a>

            <div id="bootstrappersID" style="display: none; margin-left: 15%"></div>
            <br/>
            <a href="plugins.jspa?project=<ww:property value="project"/>&pluginType=sourcecontrol"
               onclick="loadPlugins(this.href, 'sourceControlID'); return false;">
                <img id="sourceControlID-icon" src="images/plus_nolines.gif"/>
                Source Controls</a>

            <div id="sourceControlID" style="display: none; margin-left: 15%"></div>
            <br/>
            <a href="plugins.jspa?project=<ww:property value="project"/>&pluginType=builder"
               onclick="loadPlugins(this.href, 'builderID'); return false;">
                <img id="builderID-icon" src="images/plus_nolines.gif"/>
                Schedule</a>

            <div id="builderID" style="display: none; margin-left: 15%"></div>
            <br/>
            <a href="plugins.jspa?project=<ww:property value="project"/>&pluginType=logger"
               onclick="loadPlugins(this.href, 'loggerID'); return false;">
                <img id="loggerID-icon" src="images/plus_nolines.gif"/>
                Log</a>

            <div id="loggerID" style="display: none; margin-left: 15%"></div>
            <br/>
            <a href="plugins.jspa?project=<ww:property value="project"/>&pluginType=publisher"
               onclick="loadPlugins(this.href, 'publishersID'); return false;">
                <img id="publishersID-icon" src="images/plus_nolines.gif"/>
                Publishers</a>

            <div id="publishersID" style="display: none; margin-left: 15%"></div>
        </td>
        <td width="50%"><div id="plugin-details"></div></td>
    </tr>
    <tr><td colspan="2">
        <ww:form action="config" id="commons-math-config"
                 name="commons-math-config" method="post">
            <ww:textarea name="contents" rows="24" cols="80"/>
            <ww:submit value="Configure"/>
        </ww:form>
    </td></tr>
</table>
</body>
</html>
