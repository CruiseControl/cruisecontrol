<?xml version="1.0"?>

<!--*****************************************************************************
 * CruiseControl, a Continuous Integration Toolkit                              *
 * Copyright (C) 2001  ThoughtWorks, Inc.                                       *
 * 651 W Washington Ave. Suite 500                                              *
 * Chicago, IL 60661 USA                                                        *
 *                                                                              *
 * This program is free software; you can redistribute it and/or                *
 * modify it under the terms of the GNU General Public License                  *
 * as published by the Free Software Foundation; either version 2               *
 * of the License, or (at your option) any later version.                       *
 *                                                                              *
 * This program is distributed in the hope that it will be useful,              *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of               *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                *
 * GNU General Public License for more details.                                 *
 *                                                                              *
 * You should have received a copy of the GNU General Public License            *
 * along with this program; if not, write to the Free Software                  *
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.  *
 *****************************************************************************-->
 
<xsl:stylesheet
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
	xmlns:lxslt="http://xml.apache.org/xslt">

<xsl:output method="html"/>

<xsl:template match="/">

<table align="center" cellpadding="0" cellspacing="0" border="0" width="98%"> 

   <xsl:if test="build/@error">
      <tr><td colspan="4"><font face="arial" size="3"><b>BUILD FAILED</b></font></td></tr>
   </xsl:if>   
   <xsl:if test="not (build/@error)">
      <tr><td colspan="4"><font face="arial" size="3"><b>BUILD COMPLETE&#160;-&#160;
      <xsl:value-of select="build/label"/></b>      
      </font></td></tr>
   </xsl:if>

   <tr><td colspan="4"><font face="arial" size="2"><b>Date:&#160;</b><xsl:value-of select="build/today"/></font></td></tr>   
   <tr><td colspan="4"><font face="arial" size="2"><b>Time:&#160;</b><xsl:value-of select="build/@time"/></font><br/><br/></td></tr>


   <!-- Compilation Errors -->
   <xsl:if test="(count(build//target/task[@name='Javac']/message[@priority='warn']) + count(build//target/task[@name='EjbJar']/message[@priority='warn'])) > 0">

     <tr><td bgcolor="#000066" colspan="4"><font face="arial" size="2" color="#FFFFFF">&#160;Errors:&#160;(<xsl:value-of select="count(build//target/task[(@name='Javac' or @name='EjbJar') and message[@priority='warn']])"/>)</font></td></tr>

     <tr><td colspan="4"><font face="arial" size="1"><xsl:apply-templates select="build//target/task[@name='Javac']/message[@priority='warn']"/></font></td></tr><tr><td colspan="4">&#160;</td></tr>
     <tr><td colspan="4"><font face="arial" size="1"><xsl:apply-templates select="build//target/task[@name='EjbJar']/message[@priority='warn']"/></font></td></tr><tr><td colspan="4">&#160;</td></tr>
   </xsl:if>



   <!-- Unit Tests -->
   <tr>
     <td bgcolor="#000066" colspan="4"><font face="arial" size="2" color="#FFFFFF">&#160;Unit Tests: (<xsl:value-of select="count(build//testsuite/testcase)"/>)</font></td>
   </tr>
   
   
   <xsl:choose>
      <xsl:when test="count(build//testsuite/testcase) = 0">
         <tr><td colspan="4"><i><font face="arial" size="2">No Tests Run</font></i></td></tr>
      </xsl:when>
      <xsl:when test="(count(build//testsuite/testcase/error) + count(build//testsuite/testcase/failure)) = 0">
         <tr><td colspan="4"><i><font face="arial" size="2">All Tests Passed</font></i></td></tr>      
      </xsl:when>      
   </xsl:choose>
   
      
   <xsl:apply-templates select="build//testsuite/testcase//error"/>
   <xsl:apply-templates select="build//testsuite/testcase//failure"/>
   <tr/>
   <tr><td colspan="4">&#160;</td></tr>
   
   <xsl:if test="count(build//testsuite/testcase[error]) + count(build//testsuite/testcase[failure]) > 0">
    
      <tr><td bgcolor="#000066" colspan="4"><font face="arial" size="2" color="#FFFFFF">&#160;Unit Test Error Details:&#160;(<xsl:value-of select="count(build//testsuite/testcase[error]) + count(build//testsuite/testcase[failure])"/>)</font></td></tr>

      <xsl:call-template name="testdetail">
        <xsl:with-param name="detailnodes" select="build//testsuite/testcase[.//error]"/>
      </xsl:call-template>

      <xsl:call-template name="testdetail">
        <xsl:with-param name="detailnodes" select="build//testsuite/testcase[.//failure]"/>
      </xsl:call-template>
      
      <tr><td colspan="4">&#160;</td></tr>
   </xsl:if>


   <!-- Modifications -->
   <tr>
   <td bgcolor="#000066" colspan="4"><font face="arial" size="2" color="#FFFFFF">&#160;Modifications:&#160;(<xsl:value-of select="count(build/modifications/modification)"/>)</font></td>
   </tr>

   <xsl:apply-templates select="build/modifications/modification"/>
   <tr><td colspan="4">&#160;</td></tr>

</table>
</xsl:template>



<!-- UnitTest Errors And Failures Detail Template -->
<xsl:template name="testdetail">
  <xsl:param name="detailnodes"/>
  
  <xsl:for-each select="$detailnodes">
    <xsl:variable name="headercolor">#000000</xsl:variable>
    <xsl:variable name="detailcolor">#FF0000</xsl:variable>
    
      <tr>
      <td colspan="4"><font face="arial" size="1" color="{$headercolor}">Test: <xsl:value-of select="@name" /></font></td></tr>
    
      <xsl:if test="error">
        <tr ><td colspan="4"><font face="arial" size="1" color="{$headercolor}">Type: <xsl:value-of select="error/@type" /></font></td></tr>
        <tr ><td colspan="4"><font face="arial" size="1" color="{$headercolor}">Message: <xsl:value-of select="error/@message" /></font></td></tr>
        <tr/><tr/><tr/>
        <tr><td colspan="4"><PRE><font face="arial" size="1" color="{$detailcolor}"><xsl:value-of select="error" /></font></PRE></td></tr>
      </xsl:if>

      <xsl:if test="failure">
        <tr><td colspan="4"><font face="arial" size="1" color="{$headercolor}">Type: <xsl:value-of select="failure/@type" /></font></td></tr>
        <tr><td colspan="4"><font face="arial" size="1" color="{$headercolor}">Message: <xsl:value-of select="failure/@message" /></font></td></tr>
        <tr/><tr/><tr/>
        <tr><td colspan="4"><PRE><font face="arial" size="1" color="{$detailcolor}"><xsl:value-of select="failure" /></font></PRE></td></tr>
      </xsl:if>

      <tr/><tr/><tr/>
  </xsl:for-each>
</xsl:template>



<!-- UnitTest Errors -->
<xsl:template match="error">
  <tr>
  <xsl:if test="position() mod 2 = 0">
    <xsl:attribute name="bgcolor">#CCCCCC</xsl:attribute>   
  </xsl:if>   
   
  <td><font size="1" face="arial">error</font></td><td colspan="3"><font size="1" face="arial"><xsl:value-of select="../@name"/></font></td></tr>
</xsl:template>



<!-- UnitTest Failures -->
<xsl:template match="failure">
  <tr>
  
  <xsl:if test="(count(build//testsuite//error) + position()) mod 2 = 0">
    <xsl:attribute name="bgcolor">#CCCCCC</xsl:attribute>   
  </xsl:if>   
   
  <td><font size="1" face="arial">failure</font></td><td colspan="3"><font size="1" face="arial"><xsl:value-of select="../@name"/></font></td></tr>
</xsl:template>



<!-- Compilation Error Details -->
<xsl:template match="message[@priority='warn']">
  <xsl:value-of select="text()"/><br/>
  
</xsl:template>



<!-- Modifications template -->
<xsl:template match="modification">
  <tr>
    <xsl:if test="position() mod 2 = 0">
      <xsl:attribute name="bgcolor">#CCCCCC</xsl:attribute>   
    </xsl:if>


    <td><font size="1" face="arial"><xsl:value-of select="@type"/></font></td>
    <td width="175"><font size="1" face="arial"><xsl:value-of select="date"/></font></td>
    <td width="80"><font size="1" face="arial"><xsl:value-of select="user"/></font></td>
    <td><font size="1" face="arial"><xsl:value-of select="filename"/></font></td>
  </tr>
    
  <xsl:comment>Project: <xsl:value-of select="project"/></xsl:comment>
  <xsl:comment>Comment: <xsl:value-of select="comment"/></xsl:comment>
</xsl:template>


</xsl:stylesheet>
