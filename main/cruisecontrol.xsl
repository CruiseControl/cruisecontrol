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
    <table align="center" cellpadding="2" cellspacing="0" border="0" width="98%"> 

        <xsl:if test="build/@error">
        <tr><td colspan="5"><font face="arial" size="3"><b>BUILD FAILED</b></font></td></tr>
        </xsl:if>   
        <xsl:if test="not (build/@error)">
        <tr><td colspan="5"><font face="arial" size="3"><b>BUILD COMPLETE&#160;-&#160;
                <xsl:value-of select="build/label"/></b>      
        </font></td></tr>
        </xsl:if>

        <tr><td colspan="5"><font face="arial" size="2"><b>Date of build:&#160;</b><xsl:value-of select="build/today"/></font></td></tr>   
        <tr><td colspan="5"><font face="arial" size="2"><b>Time to build:&#160;</b><xsl:value-of select="build/@time"/></font></td></tr>
        <!--
        <tr><td colspan="5"><font face="arial" size="2"><b>Codeline:&#160;</b><xsl:value-of select="build/modifications/modification/project"/></font></td></tr>
        -->
        <tr><td colspan="5"><font face="arial" size="2"><b>Last changed:&#160;</b><xsl:value-of select="build/modifications/modification/date"/></font></td></tr>
        <tr><td colspan="5"><font face="arial" size="2"><b>Description:&#160;</b><xsl:value-of select="build/modifications/modification/comment"/></font><br/><br/></td></tr>

        <xsl:if test="(count(//target/task[@name='Javac']/message[@priority='warn']) + count(//target/task[@name='EjbJar']/message[@priority='warn'])) > 0">
        <tr><td bgcolor="#000066" colspan="5"><b><font face="arial" size="2" color="#FFFFFF">&#160;Errors:&#160;(<xsl:value-of select="count(//target/task[@name!='get']/message[@priority='warn'])"/>)</font></b></td></tr>

        <tr><td colspan="5"><font color="red" face="arial" size="1"><xsl:apply-templates select="//target/task[@name='Javac']/message[@priority='warn']"/></font></td></tr>
        <tr><td colspan="5"><font color="red" face="arial" size="1"><xsl:apply-templates select="//target/task[@name='EjbJar']/message[@priority='warn']"/></font></td></tr>

        <tr><td colspan="5">&#160;</td></tr>
        </xsl:if>
        <tr>
        <td bgcolor="#000066" colspan="5"><font face="arial" size="2" color="#FFFFFF">&#160;Unit Tests: (<xsl:value-of select="count(build//testsuite/testcase)"/>)</font></td>
        </tr>


        <xsl:choose>
        <xsl:when test="count(build//testsuite) = 0">
            <tr><td colspan="5"><i><font face="arial" size="2">No Tests Run</font></i></td></tr>
            <tr><td colspan="5"><i><font color="red" face="arial" size="2">This project doesn't have any tests</font></i></td></tr>
        </xsl:when>
        <xsl:when test="(count(build//testsuite/testcase/error) + count(build//testsuite/testcase/failure)) = 0">
            <tr><td colspan="5"><i><font face="arial" size="2">All Tests Passed</font></i></td></tr>      
        </xsl:when>      
        </xsl:choose>


        <xsl:apply-templates select="build//testsuite/testcase/error"/>
        <xsl:apply-templates select="build//testsuite/testcase/failure"/>
        <tr>
        <tr><td colspan="5">&#160;</td></tr>
        <td bgcolor="#000066" colspan="5"><font face="arial" size="2" color="#FFFFFF">&#160;Modifications since last build:&#160;(<xsl:value-of select="count(build/modifications/modification)"/>)</font></td>
        </tr>
        <xsl:apply-templates select="build/modifications/modification"/>

        <xsl:if test="count(//task[@name='Jar']) + count(//task[@name='War']) > 0">
            <tr>
                <tr><td colspan="5">&#160;</td></tr>
                <td bgcolor="#000066" colspan="5"><font face="arial" size="2" color="#FFFFFF">&#160;Deployments by this build:&#160;(<xsl:value-of select="count(//task[@name='Jar']) + count(//task[@name='War'])"/>)</font></td>
            </tr>
            <xsl:apply-templates select="//task[@name='Jar'] | //task[@name='War']" />
        </xsl:if>   


    </table>
    </xsl:template>

    <xsl:template match="error">
    <tr>
        <xsl:if test="position() mod 2 = 0">
        <xsl:attribute name="bgcolor">#CCCCCC</xsl:attribute>   
        </xsl:if>   

        <td><font size="1" face="arial">error</font></td><td colspan="3"><font size="1" face="arial"><xsl:value-of select="../@name"/></font></td></tr>
    </xsl:template>

    <xsl:template match="failure">
    <tr>
        <xsl:if test="(count(build//testsuite/error) + position()) mod 2 = 0">
        <xsl:attribute name="bgcolor">#CCCCCC</xsl:attribute>   
        </xsl:if>   

        <td><font size="1" face="arial">failure</font></td><td colspan="3"><font size="1" face="arial"><xsl:value-of select="../@name"/></font></td></tr>
    </xsl:template>




    <xsl:template match="message[@priority='warn']">
    <xsl:value-of select="text()"/><br/><br/>   
    </xsl:template>

    <xsl:template match="modification">


    <tr>
        <xsl:if test="position() mod 2 = 0">
        <xsl:attribute name="bgcolor">#CCCCCC</xsl:attribute>   
        </xsl:if>


        <td><font size="1" face="arial"><xsl:value-of select="@type"/></font></td>
        <!--
        <td><font size="1" face="arial"><xsl:value-of select="date"/></font></td>
        -->
        <td><font size="1" face="arial">&#160;</font></td>
        <td><font size="1" face="arial"><xsl:value-of select="user"/></font></td>
        <td><font size="1" face="arial"><xsl:value-of select="project"/></font></td>
        <td><font size="1" face="arial"><xsl:value-of select="filename"/></font></td>
    </tr>
    <xsl:comment>Project: <xsl:value-of select="project"/></xsl:comment>
    <xsl:comment>Project: <xsl:value-of select="project"/></xsl:comment>

    </xsl:template>

    <xsl:template match="task[@name='Jar'] | task[@name='War']">
        <tr>
            <xsl:if test="position() mod 2 = 0">
                <xsl:attribute name="bgcolor">#CCCCCC</xsl:attribute>   
            </xsl:if>
            <td colspan="5">
                <nobr>
                    <font face="arial" size="1"><xsl:value-of select="message"/></font>
                </nobr>
            </td>
        </tr>
    </xsl:template>

</xsl:stylesheet>
