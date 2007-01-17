<?xml version="1.0"?>
<!--********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2003, ThoughtWorks, Inc.
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
    version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns="http://www.w3.org/TR/html4/strict.dtd" >

    <xsl:output method="html"/>

    <xsl:variable name="task" select="/cruisecontrol/build//buildresults//task"/>

    <xsl:template match="/" mode="nant">
	
        <xsl:variable name="nant.messages" select="$task/message"/>
        <xsl:variable name="nant.error.messages" select="$task/message[@priority='Error']"/>
        <xsl:variable name="nant.warn.messages" select="$task/message[@priority='Warn']"/>
        <xsl:variable name="nant.info.messages" select="$task/message[@level='Info']"/>

        <xsl:if test="count($nant.messages) > 0">
            <table align="center" cellpadding="2" cellspacing="0" border="0" width="98%">
                 <!-- Style download notifications first -->
                 <tr class="compile-sectionheader">
                     <td>Initial Messages</td>
                 </tr>
                 <tr>
                     <td>
                         <xsl:apply-templates select="cruisecontrol/buildresults/message" mode="nant"/>
                     </td>
                 </tr>
                 <xsl:apply-templates select="$task" mode="nant"/>
            </table>
        </xsl:if>
    </xsl:template>

    <xsl:template match="task" mode="nant">
       <tr class="compile-sectionheader">
       		<td>
            	<xsl:value-of select="@name"/>
            </td>
       </tr>
       <tr>
       		<td>
            	<xsl:apply-templates select="./message" mode="nant"/>
            </td>
       </tr>
    </xsl:template>

    <xsl:template match="message[@priority='Error']" mode="nant">
    	  <span class="compile-error-data">
        <xsl:value-of select="text()"/><xsl:text disable-output-escaping="yes"><![CDATA[<br/>]]></xsl:text>
        </span>
    </xsl:template>

    <xsl:template match="message[@priority='Warn']" mode="nant">
    	  <span class="compile-data">
        <xsl:value-of select="text()"/><xsl:text disable-output-escaping="yes"><![CDATA[<br/>]]></xsl:text>
        </span>
    </xsl:template>

    <xsl:template match="message[@priority='Info']" mode="nant">
    	  <span class="compile-data">
        <xsl:value-of select="text()"/><xsl:text disable-output-escaping="yes"><![CDATA[<br/>]]></xsl:text>
        </span>
    </xsl:template>

    <xsl:template match="/">
       <xsl:apply-templates select="." mode="nant"/>
    </xsl:template>
</xsl:stylesheet>
