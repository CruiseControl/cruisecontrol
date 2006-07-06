<%--********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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
<%@page import="java.net.*,java.io.*"%>
<%@ taglib uri="/WEB-INF/cruisecontrol-jsp11.tld" prefix="cruisecontrol"%>
<%
    String hostname = "";
    try
    {
    	hostname = InetAddress.getLocalHost().getCanonicalHostName(); 
    }
    catch(IOException e)
    {
	    try
    	{
        	hostname = InetAddress.getLocalHost().getHostName();
    	}
    	catch(IOException e)
    	{
        	hostname = "localhost";
    	}
    }
    String port = System.getProperty("cruisecontrol.jmxport");
    String webXmlPort = application.getInitParameter("cruisecontrol.jmxport");
    if (port == null && webXmlPort != null) {
        port = webXmlPort;
    } else if (port == null) {
        port = "8000";
    }


    String jmxURL = "http://" + hostname+ ":"+ port + "/mbean?objectname=CruiseControl+Project%3Aname%3D" +
            request.getPathInfo().substring(1);
%>
<p>
<table width="100%" align="center" cellpadding="0" cellspacing="0">
    <tr>
        <td align="center">
            <h2>JMX Control Panel</h2>
        </td>
    </tr>
    <tr>
        <td align="center">
            <iframe name="controlPanelFrame" id="controlPanelFrame" height="520" marginheight="0" frameborder="1"
                marginwidth="0" src="<%= jmxURL %>" height="600" width="90%">
            </iframe>
        </td>
    </tr>
</table>
</p>
