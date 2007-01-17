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
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

  <xsl:output method="html"/>

  <xsl:param name="viewcvs.url"/>
  <xsl:param name="cvstagdiff.success.show" select="false"/>

  <xsl:template match="/" mode="cvstagdiff">
    <xsl:apply-templates select="/cruisecontrol/tagdiff" mode="cvstagdiff"/>
  </xsl:template>

  <xsl:template match="tagdiff" mode="cvstagdiff">
    <xsl:variable name="broken" select="/cruisecontrol/build/@error"/>
    <xsl:if test="$cvstagdiff.success.show!='false' or $broken">
      <xsl:variable name="difference.list" select="entry"/>

      <table align="center" cellpadding="2" cellspacing="1" border="0" width="98%">
        <tr>
          <td class="differences-sectionheader" colspan="2">
           Differences since last good build: 
          (<xsl:value-of select="count($difference.list)"/>)
          </td>

          <td class="differences-sectionheader">Last Working Version</td>
          <td class="differences-sectionheader">Current Version</td>

        </tr>

       <xsl:apply-templates select="$difference.list" mode="cvstagdiff">
         <xsl:sort select="filename" order="descending" data-type="text" />
       </xsl:apply-templates>

      </table>

      <hr/>
   </xsl:if>
  </xsl:template>

  <!-- Differences template -->
 
  <xsl:template match="entry" mode="cvstagdiff">
    <xsl:variable name="package" select="../@package"/>
    <xsl:variable name="filename" select="file/name"/>
    <xsl:variable name="prevrevision" select="file/prevrevision"/>
    <xsl:variable name="revision" select="file/revision"/>

    <tr>
      <xsl:if test="position() mod 2=0">
        <xsl:attribute name="class">differences-oddrow</xsl:attribute>
      </xsl:if>
      <xsl:if test="position() mod 2!=0">
        <xsl:attribute name="class">differences-evenrow</xsl:attribute>
      </xsl:if>

      <xsl:choose>
          
        <!-- modified files -->
        <xsl:when test="$prevrevision and $revision">
          <td class="differences-data"> modified </td>
          <td class="differences-data">
            <a href="{$viewcvs.url}/{$package}/{$filename}.diff?r1={$prevrevision}&amp;r2={$revision}">
              <i><xsl:value-of select="$filename"/></i>
            </a>
          </td>
          <td class="differences-data">
            <a href="{$viewcvs.url}/{$package}/{$filename}?r1={$prevrevision}#rev{$prevrevision}">
              <xsl:value-of select="$prevrevision"/>
            </a>
          </td>
          <td class="differences-data">
            <a href="{$viewcvs.url}/{$package}/{$filename}?r1={$revision}#rev{$revision}">
              <xsl:value-of select="$revision"/>
            </a>
          </td>
        </xsl:when>

        <!-- deleted files -->
        <xsl:when test="$prevrevision and not ($revision)">
          <td class="differences-data"> deleted </td>
          <td class="differences-data">
            <a href="{$viewcvs.url}/{$package}/{$filename}">
              <i><xsl:value-of select="$filename"/></i>
            </a>
          </td>
          <td class="differences-data"> </td>
          <td class="differences-data"> </td>
        </xsl:when>

        <!-- added files -->
        <xsl:otherwise><!-- test="not ($prevrevision) and $revision" -->
          <td class="differences-data"> added </td>
          <td class="differences-data">
            <a href="{$viewcvs.url}/{$package}/{$filename}?rev={$revision}&amp;content-type=text/vnd.viewcvs-markup">
              <i><xsl:value-of select="$filename"/></i>
            </a>
          </td>
          <td class="differences-data"> </td>
          <td class="differences-data">
            <a href="{$viewcvs.url}/{$package}/{$filename}?r1={$revision}#rev{$revision}">
              <xsl:value-of select="$revision"/>
            </a>
          </td>
        </xsl:otherwise>
      </xsl:choose>

    </tr>
  </xsl:template>

  <xsl:template match="/">
     <xsl:apply-templates select="." mode="cvstagdiff"/>
  </xsl:template>
</xsl:stylesheet>
