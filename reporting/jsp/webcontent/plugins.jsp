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
    <title><ww:property value="project"/> <ww:property value="pluginType"/></title>
    <link type="text/css" rel="stylesheet" href="css/cruisecontrol.css"/>
    <script type="text/javascript" language="JavaScript" src="prototype.js"></script>
    <script type="text/javascript" language="JavaScript" src="cc-config.js"></script>
</head>

<body>
<p class="config-sectionheader"><ww:property value="listType"/></p>
<ul class="config-plugins">
    <ww:iterator value="configuredPlugins">
        <li class="config-plugin"><a class="config-link"
            href="<ww:url value="load-details.jspa?pluginName=%{name}"/>"
            onclick="loadPlugin(this.href); return false;"><ww:property value="name" /></a></li>
    </ww:iterator>
</ul>

<form name="load-<ww:property value="pluginType"/>" action="load-details.jspa" method="post"
    onsubmit="loadPlugin(buildUrlForForm(this, '<ww:url value="load-details.jspa"/>')); return false;">
  <input type="hidden" name="pluginType" value="<ww:property value="pluginType"/>"/>
  <label for="select-<ww:property value="pluginType"/>" class="label">Add <ww:property value="pluginType"/>:</label>
  <select id="select-<ww:property value="pluginType"/>" name="pluginName">
    <ww:iterator value="availablePlugins">
      <option value="<ww:property value="name"/>"><ww:property value="name"/></option>
    </ww:iterator>
  </select>
  <br/>
  <input type="submit" name="add-<ww:property value="pluginType"/>" value="Add" class="config-button"/>
</form>
</body>
</html>
