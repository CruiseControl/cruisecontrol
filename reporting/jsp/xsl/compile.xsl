<?xml version="1.0"?>
<!--********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 500
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
 ********************************************************************************-->
<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
    xmlns:lxslt="http://xml.apache.org/xslt">

    <xsl:output method="html"/>
    <xsl:variable name="tasklist" select="//target/task"/>
    <xsl:variable name="javac.tasklist" select="$tasklist[@name='Javac']"/>
    <xsl:variable name="ejbjar.tasklist" select="$tasklist[@name='EjbJar']"/>

    <xsl:template match="/">
        <table align="center" cellpadding="2" cellspacing="0" border="0" width="98%">

            <xsl:variable name="javac.error.messages" select="$javac.tasklist/message[@priority='error']"/>
            <xsl:variable name="javac.warn.messages" select="$javac.tasklist/message[@priority='warn']"/>
            <xsl:variable name="ejbjar.error.messages" select="$ejbjar.tasklist/message[@priority='error']"/>
            <xsl:variable name="ejbjar.warn.messages" select="$ejbjar.tasklist/message[@priority='warn']"/>
            <xsl:variable name="total.errorMessage.count" select="count($javac.warn.messages) + count($ejbjar.warn.messages) + count($javac.error.messages) + count($ejbjar.error.messages)"/>

            <xsl:if test="$total.errorMessage.count > 0">
                <tr>
                <!-- NOTE: total.errorMessage.count is actually the number of lines of error
                 messages. This accurately represents the number of errors ONLY if the Ant property
                 build.compiler.emacs is set to "true" -->
                    <td class="compile-sectionheader">
                       &#160;Errors/Warnings: (<xsl:value-of select="$total.errorMessage.count"/>)
                    </td>
                </tr>
                <xsl:apply-templates select="$javac.error.messages"/>
                <xsl:apply-templates select="$javac.warn.messages"/>
                <xsl:apply-templates select="$ejbjar.error.messages"/>
                <xsl:apply-templates select="$ejbjar.warn.messages"/>
            </xsl:if>

        </table>
    </xsl:template>

    <xsl:template match="message[@priority='error']">
        <tr><td class="compile-data"><xsl:value-of select="text()"/></td></tr>
    </xsl:template>

    <xsl:template match="message[@priority='warn']">
        <tr><td class="compile-data"><xsl:value-of select="text()"/></td></tr>
    </xsl:template>

</xsl:stylesheet>
