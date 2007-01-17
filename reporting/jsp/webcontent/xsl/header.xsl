<?xml version="1.0"?>
<!--********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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
 ********************************************************************************-->
<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
    xmlns:lxslt="http://xml.apache.org/xslt">

    <xsl:output method="html"/>

    <xsl:template match="/" mode="header">
        <xsl:variable name="modification.list" select="cruisecontrol/modifications/modification"/>

        <table align="center" cellpadding="2" cellspacing="0" border="0" class="header" width="98%">

            <xsl:if test="cruisecontrol/build/@error">
                <tr><th class="big" colspan="2">BUILD FAILED</th></tr>
                <tr>
                    <th>Ant Error Message:</th>
                    <td><xsl:value-of select="cruisecontrol/build/@error"/></td>
                </tr>
            </xsl:if>

            <xsl:if test="not (cruisecontrol/build/@error)">
                <tr><th class="big" colspan="2">BUILD COMPLETE&#160;-&#160;
                    <xsl:value-of select="cruisecontrol/info/property[@name='label']/@value"/>
                </th></tr>
            </xsl:if>

            <tr>
                <th>Date of build:</th>
                <td><xsl:value-of select="cruisecontrol/info/property[@name='builddate']/@value"/></td>
            </tr>
            <tr>
                <th>Time to build:</th>
                <td><xsl:value-of select="cruisecontrol/build/@time"/></td>
            </tr>
            <xsl:apply-templates select="$modification.list" mode="header">
                <xsl:sort select="date" order="descending" data-type="text" />
            </xsl:apply-templates>
        </table>
    </xsl:template>

    <!-- Last Modification template -->
    <xsl:template match="modification" mode="header">
        <xsl:if test="position() = 1">
            <tr>
                <th>Last changed:</th>
                <td><xsl:value-of select="date"/></td>
            </tr>
            <tr>
                <th>Last log entry:</th>
                <td><xsl:value-of select="comment"/></td>
            </tr>
        </xsl:if>
    </xsl:template>

    <xsl:template match="/">
        <xsl:apply-templates select="." mode="header"/>
    </xsl:template>
</xsl:stylesheet>
