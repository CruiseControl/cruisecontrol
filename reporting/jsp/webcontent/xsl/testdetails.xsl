<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
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
  <xsl:output method="html" encoding="UTF-8" indent="yes"/>
  <xsl:decimal-format decimal-separator="." grouping-separator="," />

  <!-- 
    Root template
  -->
  <xsl:template match="/">
        <script type="text/javascript" language="JavaScript">
          <!--
            Function show/hide given div
          -->
          function toggleDivVisibility(_div) {
            if (_div.style.display=="none") {
              _div.style.display="block";
            } else {
              _div.style.display="none";
            }
          }
        </script>
           
        <!-- Main table -->
        <table border="0" cellspacing="0" width="100%">
          <colgroup>
            <col width="10%"/>
            <col width="45%"/>
            <col width="25%"/>
            <col width="10%"/>
            <col width="10%"/>
          </colgroup>
          <tr valign="top" class="unittests-sectionheader" align="left">
            <th colspan="3">Name</th>
            <th>Status</th>
            <th nowrap="nowrap">Time(s)</th>
          </tr>

          <!-- display test suites -->
          <xsl:apply-templates select="//testsuite">
            <xsl:sort select="count(testcase/error)" data-type="number" order="descending"/>
            <xsl:sort select="count(testcase/failure)" data-type="number" order="descending"/>
            <xsl:sort select="@package"/>
            <xsl:sort select="@name"/>
          </xsl:apply-templates>

        </table>
    
  </xsl:template>
  
  <!--
    Test Suite Template
    Construct TestSuite section
  -->
  <xsl:template match="testsuite">
    <tr>
      <xsl:attribute name="class">
        <xsl:choose>
          <xsl:when test="testcase/error">unittests-error-title</xsl:when>
          <xsl:when test="testcase/failure">unittests-failure-title</xsl:when>
          <xsl:otherwise>unittests-title</xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
      <td colspan="5"><xsl:value-of select="concat(@package,'.',@name)"/></td>
    </tr>
    <!-- Display tests -->
    <xsl:apply-templates select="testcase"/>
    <!-- Display details links -->
    <xsl:apply-templates select="current()" mode="details"/>
  </xsl:template>
  
  <!--
    Testcase template
    Construct testcase section
  -->
  <xsl:template match="testcase">
    <tr>
      <xsl:attribute name="class">
        <xsl:choose>
          <xsl:when test="error">unittests-error</xsl:when>
          <xsl:when test="failure">unittests-failure</xsl:when>
          <xsl:otherwise>unittests-data</xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
      <xsl:if test="position() mod 2 = 0">
        <xsl:attribute name="bgcolor">#EEEEEE</xsl:attribute>
      </xsl:if>
      <td>&#xA0;</td>
      <td colspan="2">
        <xsl:value-of select="@name"/>
      </td>
      <td>
        <xsl:choose>
          <xsl:when test="error">
            <a>
              <xsl:attribute name="href">javascript:displayMessage('<xsl:value-of
                select="../@package"/>.<xsl:value-of select="../@name"/>.<xsl:value-of
                  select="@name"/>');</xsl:attribute> Error &#187; </a>
          </xsl:when>
          <xsl:when test="failure">
            <a>
              <xsl:attribute name="href">javascript:displayMessage('<xsl:value-of
                select="../@package"/>.<xsl:value-of select="../@name"/>.<xsl:value-of
                  select="@name"/>');</xsl:attribute> Failure &#187; </a>
          </xsl:when>
          <xsl:otherwise>Success</xsl:otherwise>
        </xsl:choose>
      </td>
      <xsl:choose>
        <xsl:when test="not(failure|error)">
          <td>
            <xsl:value-of select="format-number(@time,'0.000')"/>
          </td>
        </xsl:when>
        <xsl:otherwise>
          <td/>
        </xsl:otherwise>
      </xsl:choose>
    </tr>
  </xsl:template>
  
  <!--
    Display Properties and Output links
    and construct hidden div's with data
  -->
  <xsl:template match="testsuite" mode="details">
    <tr class="unittests-data">
      <td colspan="2">
        <xsl:if test="count(properties/property)&gt;0">
          <a href="javascript:void(0)" onClick="toggleDivVisibility(document.getElementById('{concat('properties.',@package,'.',@name)}'))">Properties &#187;</a>
        </xsl:if>&#xA0;
      </td>
      <td>
        <xsl:if test="system-out/text()">
          <a href="javascript:void(0)" onClick="toggleDivVisibility(document.getElementById('{concat('system_out.',@package,'.',@name)}'))">System.out &#187;</a>
        </xsl:if>&#xA0;
      </td>
      <td>
        <xsl:if test="system-err/text()">
          <a href="javascript:void(0)" onClick="toggleDivVisibility(document.getElementById('{concat('system_err.',@package,'.',@name)}'))">System.err &#187;</a>
        </xsl:if>&#xA0;
      </td>
      <td>&#xA0;</td>
    </tr>
    <tr>
      <td colspan="5">
        <!-- Construct details div's -->
        <!-- System Error -->
        <xsl:apply-templates select="system-err" mode="system-err-div">
          <xsl:with-param name="div-id" select="concat('system_err.',@package,'.',@name)"/>
        </xsl:apply-templates>
        <!-- System Output -->
        <xsl:apply-templates select="system-out" mode="system-out-div">
          <xsl:with-param name="div-id" select="concat('system_out.',@package,'.',@name)"/>
        </xsl:apply-templates>
        <!-- Properties -->
        <xsl:apply-templates select="properties" mode="properties-div">          
          <xsl:with-param name="div-id" select="concat('properties.',@package,'.',@name)"/>
        </xsl:apply-templates>
        &#xA0;
      </td>
    </tr>
  </xsl:template>
  
  <!--
    Create div with detailed system output
  -->
  <xsl:template match="system-out" mode="system-out-div" >
    <xsl:param name="div-id"/>
    <div id="{$div-id}" class="testresults-output-div" style="display: none;">
      <span style="font-weight:bold">System out:</span><br/>
      <xsl:apply-templates select="current()" mode="newline-to-br"/>
    </div>  
  </xsl:template>
  
  <!--
    Create div with detailed errors output
  -->
  <xsl:template match="system-err" mode="system-err-div" >
    <xsl:param name="div-id"/>
    <div id="{$div-id}" class="testresults-output-div" style="display: none;">
      <span style="font-weight:bold">System err:</span><br/>
      <xsl:apply-templates select="current()" mode="newline-to-br"/>
    </div>  
  </xsl:template>
  
  <!--
    Create div with properties
  -->
  <xsl:template match="properties" mode="properties-div" >
    <xsl:param name="div-id"/>
    <div id="{$div-id}" class="testresults-output-div" style="display: none;">
      <span style="font-weight:bold">Properties:</span><br/>
      <table>
        <tr>
          <th>Property</th>
          <th>Value</th>
        </tr>
        <xsl:for-each select="property">
          <xsl:sort select="@name"/>
          <tr>
            <td><xsl:value-of select="@name"/>&#xA0;</td>
            <td><xsl:value-of select="@value"/>&#xA0;</td>
          </tr>          
        </xsl:for-each>
      </table>
    </div>  
  </xsl:template>
  
  <!--
    Convert line brakes in given text into <br/>
  -->
  <xsl:template match="text()" mode="newline-to-br">    
    <xsl:value-of select="replace(current(), '(\n)|(\r)|(\r\n)', '&lt;br/&gt;')" disable-output-escaping="yes"/>          
  </xsl:template>   

</xsl:stylesheet>
