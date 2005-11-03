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
<%@page errorPage="/error.jsp"%>
<%@ taglib uri="webwork" prefix="ww"%>
<html>
<head>
<title>CVS Configuration</title>
<link type="text/css" rel="stylesheet" href="css/cruisecontrol.css" />
</head>
<body>
<ww:form action="cvs" id="cvs" name="cvs" method="post">
    <table xmlns="http://www.w3.org/TR/html4/strict.dtd" width="98%" border="0" cellspacing="0" cellpadding="2" align="center">
        <thead>
            <tr><td>
                <ul>
                    <li><a href="config.action">Return to project configuration</a></li>
                </ul>
            </td></tr>
            <ww:if test="false">
                <tr><td class='config-resultmsg'></td></tr>
            </ww:if>
            <tr><td class="config-sectionheader" colspan="2">
                <p>CVS Configuration</p>
            </td></tr>
        </thead>
        <tbody>
            <tr>
                <td><label for="localWorkingCopy">Local working copy:</label></td>
                <td>
                    <input type="text" id="localWorkingCopy"
                        name="localWorkingCopy" size="64"
                        value="<ww:property value="localWorkingCopy"/>"/>
                </td>
            </tr>
            <tr>
                <td><label for="cvsroot">CVSROOT:</label></td>
                <td>
                    <input type="text" id="cvsroot"
                        name="cvsroot" size="64"
                        value="<ww:property value="cvsroot"/>"/>
                </td>
            </tr>
            <tr>
                <td><label for="module">Module:</label></td>
                <td>
                    <input type="text" id="module"
                        name="module" size="64"
                        value="<ww:property value="module"/>"/>
                </td>
            </tr>
            <tr>
                <td><label for="tag">Tag or revision number:</label></td>
                <td>
                    <input type="text" id="tag"
                        name="tag" size="64"
                        value="<ww:property value="tag"/>"/>
                </td>
            </tr>
            <tr>
                <td><label for="property">Property:</label></td>
                <td>
                    <input type="text" id="property"
                        name="property" size="64"
                        value="<ww:property value="property"/>"/>
                </td>
            </tr>
            <tr>
                <td><label for="propertyOnDelete">Property to set on delete:</label></td>
                <td>
                    <input type="text" id="propertyOnDelete"
                        name="propertyOnDelete" size="64"
                        value="<ww:property value="propertyOnDelete"/>"/>
                </td>
            </tr>
        </tbody>
        <tfoot>
            <tr><td><input type="submit" name="configure" value="Configure" /></td></tr>
        </tfoot>
    </table>
</ww:form>
</body>
</html>
