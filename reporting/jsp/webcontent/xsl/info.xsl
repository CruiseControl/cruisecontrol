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
    version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns="http://www.w3.org/TR/html4/strict.dtd" >

    <xsl:output method="html"/>

  <xsl:template match="/" mode="info">    
    Detailed info:
        <xsl:apply-templates select="/cruisecontrol/*" mode="info"/>
    <xsl:apply-templates select="//text()" mode="info"/>
  </xsl:template>

  <!-->xsl:template match="target" mode="info">
    <tr>
      <td>
       Target: <code><xsl:value-of select="@name"/></code><br/> Task: <code><xsl:value-of select="task/@name"/></code>        
      </td>
    </tr>
  </xsl:template-->
  <xsl:template match="message" mode="info">    
        <pre><xsl:value-of select="@priority"/>: <xsl:value-of select="text()"/></pre>
  </xsl:template>
  <xsl:template match="build" mode="info">
        <div style="padding-left:10px">
        <pre><xsl:value-of select="target/@name"/>, <xsl:value-of select="target/task/@name"/><br/>build time: <xsl:value-of select="@time"/></pre>
          <xsl:for-each select="*">
            <xsl:if test="self::target">
            <div style="padding-left:10px">
              <pre>
                <xsl:apply-templates select="task/message" mode="build"/>
              </pre>
            </div>
            </xsl:if>
            <xsl:if test="self::build">
              <xsl:apply-templates select="." mode="info"/>
            </xsl:if>
          </xsl:for-each>
        </div>
  </xsl:template>

  <xsl:template match="message" mode="build">
    <xsl:value-of select="@priority"/>: <xsl:value-of select="text()"/><br/>
  </xsl:template>

  <xsl:template match="/" >
    <xsl:apply-templates select="." mode="info"/>
  </xsl:template>
  
  <xsl:template match="text()" mode="info">
  </xsl:template>
        
</xsl:stylesheet>
