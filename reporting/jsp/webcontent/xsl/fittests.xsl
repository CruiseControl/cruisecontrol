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
    <xsl:variable name="fitresults.list" select="//testResults"/>
    <xsl:variable name="fitresults.names" select="//testResults/result/relativePageName"/>
    <xsl:variable name="fitresults.right.count" select="sum($fitresults.list/finalCounts/right)"/>
    <xsl:variable name="fitresults.wrong.count" select="sum($fitresults.list/finalCounts/wrong)"/>
    <xsl:variable name="fitresults.ignores.count" select="sum($fitresults.list/finalCounts/ignores)"/>
    <xsl:variable name="fitresults.exceptions.count" select="sum($fitresults.list/finalCounts/exceptions)"/>
    <xsl:variable name="fitresults.totalErrors" select="$fitresults.wrong.count + $fitresults.exceptions.count"/>
    <xsl:variable name="fitresults.totalTests" select="$fitresults.right.count + $fitresults.wrong.count + $fitresults.ignores.count + $fitresults.exceptions.count"/>
<!--
***********************************************************************************
*
* This first section is based mostly on the unittests.xls that ships with
* CruiseControl. We modify it a bit to cover cppunit.
*
***********************************************************************************
-->
    <xsl:template match="/" mode="fittests">
      <xsl:if test="count($fitresults.list) > 0">
        <table align="center" cellpadding="2" cellspacing="0" border="0" width="98%">
            <tr>
                <td class="unittests-sectionheader" colspan="4">
                    &#160;Fit Tests: (<xsl:value-of select="$fitresults.totalTests"/> tests, <xsl:value-of select="$fitresults.right.count"/> successful, <xsl:value-of select="$fitresults.ignores.count"/> ignored, <xsl:value-of select="$fitresults.totalErrors"/> failed)
                </td>
            </tr>
    <xsl:choose>
        <xsl:when test="$fitresults.totalTests = 0">
            <tr>
                <td colspan="2" class="unittests-data">
                    No Fit Tests Run
                </td>
            </tr>
            <tr>
                <td colspan="2" class="unittests-error">
                    This project doesn't have any Fit tests
                </td>
            </tr>
        </xsl:when>
        <xsl:when test="$fitresults.totalErrors = 0">
            <tr>
                <td colspan="2" class="unittests-data">
                    All Fit Tests Passed
                </td>
            </tr>
        </xsl:when>
    </xsl:choose>
            <tr>
                <td>
                    <table align="center" cellpadding="2" cellspacing="0" border="0" width="98%">
                        <xsl:apply-templates select="$fitresults.names" mode="fittests"/>
                    </table>
                </td>
            </tr>
            <tr/>
            <tr><td colspan="2">&#160;</td></tr>
        </table>
      </xsl:if>

    </xsl:template>
<!--
***********************************************************************************
* Format the pass/fail cases, coloring appropriately.
***********************************************************************************
-->
    <xsl:template match="relativePageName" mode="fittests">
        <xsl:variable name="right" select="../counts/right"/>
        <xsl:variable name="wrong" select="../counts/wrong"/>
        <xsl:variable name="ignored" select="../counts/ignores"/>
        <xsl:variable name="exceptions" select="../counts/exceptions"/>
        <xsl:variable name="failures" select="$wrong + $exceptions"/>
        <tr>
    <xsl:choose>
        <xsl:when test="$failures != 0">
            <td class="unittests-data"><FONT COLOR="FF0000"><xsl:apply-templates mode="fittests"/></FONT></td>
            <td class="unittests-data"><FONT COLOR="FF0000">(<xsl:value-of select="$right"/> right, <xsl:value-of select="$wrong"/> wrong, <xsl:value-of select="$ignored"/> ignored, <xsl:value-of select="$exceptions"/> exceptions)</FONT></td>
        </xsl:when>
        <xsl:otherwise>
            <td class="unittests-data"><FONT COLOR="009900"><xsl:apply-templates mode="fittests"/></FONT></td>
            <td class="unittests-data"><FONT COLOR="009900">(<xsl:value-of select="$right"/> right, <xsl:value-of select="$wrong"/> wrong, <xsl:value-of select="$ignored"/> ignored, <xsl:value-of select="$exceptions"/> exceptions)</FONT></td>
        </xsl:otherwise>
    </xsl:choose>
        </tr>
    </xsl:template>
</xsl:stylesheet>
