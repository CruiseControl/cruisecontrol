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
    <xsl:variable name="get.tasklist" select="$tasklist[@name!='get']"/>
    
    <xsl:variable name="testsuite.list" select="build//testsuite"/>
    <xsl:variable name="testsuite.error.count" select="count($testsuite.list/error)"/>
    <xsl:variable name="testcase.list" select="$testsuite.list/testcase"/>
    <xsl:variable name="testcase.error.list" select="$testcase.list/error"/>
    <xsl:variable name="testcase.failure.list" select="$testcase.list/failure"/>
    <xsl:variable name="totalErrorsAndFailures" select="count($testcase.error.list) 
     + count($testcase.failure.list)"/>

    <xsl:variable name="modification.list" select="build/modifications/modification"/>
    <xsl:variable name="jar.tasklist" select="$tasklist[@name='Jar']"/>
    <xsl:variable name="war.tasklist" select="$tasklist[@name='War']"/>
    <xsl:variable name="dist.count" select="count($jar.tasklist) + count($war.tasklist)"/>
    
    <xsl:template match="/">
        <table align="center" cellpadding="2" cellspacing="0" border="0" width="98%"> 

            <xsl:if test="build/@error">
                <tr><td colspan="10"><font face="arial" size="3"><b>BUILD FAILED</b></font></td></tr>
                <tr>
                    <td colspan="10">
                        <font face="arial" size="2">
                            <b>Ant Error Message:&#160;</b>
                            <xsl:value-of select="build/@error"/>
                        </font>
                    </td>
                </tr>
            </xsl:if>   

            <xsl:if test="not (build/@error)">
                <tr><td colspan="10"><font face="arial" size="3"><b>BUILD COMPLETE&#160;-&#160;
                    <xsl:value-of select="build/label"/></b>      
                </font></td></tr>
            </xsl:if>

            <tr>
                <td colspan="10">
                    <font face="arial" size="2">
                        <b>Date of build:&#160;</b>
                        <xsl:value-of select="build/today"/>
                    </font>
                </td>
            </tr>   
            <tr>
                <td colspan="10">
                    <font face="arial" size="2">
                        <b>Time to build:&#160;</b>
                        <xsl:value-of select="build/@time"/>
                    </font>
                </td>
            </tr>
            <tr>
                <td colspan="10">
                    <font face="arial" size="2">
                        <b>Last changed:&#160;</b>
                        <xsl:value-of select="build/modifications/modification/date"/>
                    </font>
                </td>
            </tr>
            <tr>
                <td colspan="10">
                    <font face="arial" size="2">
                        <b>Last log entry:&#160;</b>
                        <xsl:value-of select="build/modifications/modification/comment"/>
                    </font>
                    <br/>
                    <br/>
                </td>
            </tr>

            <!-- Compilation Messages -->
            <xsl:variable name="javac.warn.messages" 
             select="$javac.tasklist/message[@priority='warn']"/>
            <xsl:variable name="ejbjar.warn.messages" 
             select="$ejbjar.tasklist/message[@priority='warn']"/>
            <xsl:variable name="total.errorMessage.count" 
             select="count($javac.warn.messages) + count($ejbjar.warn.messages)"/>
            
            <xsl:if test="$total.errorMessage.count > 0">
                <tr>
                <!-- NOTE: total.errorMessage.count is actually the number of lines of error 
                 messages. This accurately represents the number of errors ONLY if the Ant property
                 build.compiler.emacs is set to "true" -->
                    <td bgcolor="#000066" colspan="10">
                        <b><font face="arial" size="2" color="#FFFFFF">
                            &#160;Errors/Warnings: (<xsl:value-of select="$total.errorMessage.count"/>)
                        </font></b>
                    </td>
                </tr>

                <tr>
                    <td colspan="10">
                        <font color="red" face="arial" size="1">
                            <xsl:apply-templates select="$javac.warn.messages"/>
                        </font>
                    </td>
                </tr>
                <tr>
                    <td colspan="10">
                        <font color="red" face="arial" size="1">
                            <xsl:apply-templates select="$ejbjar.warn.messages"/>
                        </font>
                    </td>
                </tr>

                <tr><td colspan="10">&#160;</td></tr>
            </xsl:if>

            <!-- Unit Tests -->
            <tr>
                <td bgcolor="#000066" colspan="10">
                    <font face="arial" size="2" color="#FFFFFF">
                        &#160;Unit Tests: (<xsl:value-of select="count($testcase.list)"/>)
                    </font>
                </td>
            </tr>
                    
            <xsl:choose>
                <xsl:when test="count($testsuite.list) = 0">
                    <tr>
                        <td colspan="10">
                            <i><font face="arial" size="2">No Tests Run</font></i>
                        </td>
                    </tr>
                    <tr>
                        <td colspan="10">
                            <i><font color="red" face="arial" size="2">
                                This project doesn't have any tests
                            </font></i>
                        </td>
                    </tr>
                </xsl:when>

                <xsl:when test="$totalErrorsAndFailures = 0">
                    <tr>
                        <td colspan="10">
                            <i><font face="arial" size="2">All Tests Passed</font></i>
                        </td>
                    </tr>      
                </xsl:when>      
            </xsl:choose>

            <xsl:apply-templates select="$testcase.error.list"/>
            <xsl:apply-templates select="$testcase.failure.list"/>
            <tr/>
            <tr><td colspan="4">&#160;</td></tr>

            <xsl:if test="$totalErrorsAndFailures > 0">

              <tr>
                <td bgcolor="#000066" colspan="10">
                    <font face="arial" size="2" color="#FFFFFF">
                        &#160;Unit Test Error Details:&#160;(
                         <xsl:value-of select="$totalErrorsAndFailures"/>)
                    </font>
                </td>
              </tr>

              <!-- (PENDING) Why doesn't this work if set up as variables up top? -->
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
                <tr><td colspan="10">&#160;</td></tr>
                <td bgcolor="#000066" colspan="10">
                    <font face="arial" size="2" color="#FFFFFF">
                        &#160;Modifications since last build:&#160;(
                            <xsl:value-of select="count($modification.list)"/>)
                    </font>
                </td>
            </tr>
            
            <xsl:apply-templates select="$modification.list"/>

            <xsl:if test="$dist.count > 0">
                <tr>
                    <tr><td colspan="10">&#160;</td></tr>
                    <td bgcolor="#000066" colspan="10">
                        <font face="arial" size="2" color="#FFFFFF">
                            &#160;Deployments by this build:&#160;(
                             <xsl:value-of select="$dist.count"/>)
                        </font>
                    </td>
                </tr>
                <xsl:apply-templates select="$jar.tasklist | $war.tasklist" />
            </xsl:if>   

        </table>
    </xsl:template>

    <!-- UnitTest Errors -->
    <xsl:template match="error">
        <tr>
            <xsl:if test="position() mod 2 = 0">
                <xsl:attribute name="bgcolor">#CCCCCC</xsl:attribute>   
            </xsl:if>   

            <td colspan="5">
                <font size="1" face="arial">error</font>
            </td>
            <td colspan="5">
                <font size="1" face="arial"><xsl:value-of select="../@name"/></font>
            </td>
        </tr>
    </xsl:template>

    <!-- UnitTest Failures -->
    <xsl:template match="failure">
        <tr>
            <xsl:if test="($testsuite.error.count + position()) mod 2 = 0">
                <xsl:attribute name="bgcolor">#CCCCCC</xsl:attribute>   
            </xsl:if>   

            <td colspan="5">
                <font size="1" face="arial">failure</font>
            </td>
            <td colspan="5">
                <font size="1" face="arial"><xsl:value-of select="../@name"/></font>
            </td>
        </tr>
    </xsl:template>
    
    <!-- UnitTest Errors And Failures Detail Template -->
    <xsl:template name="testdetail">
      <xsl:param name="detailnodes"/>
      
      <xsl:for-each select="$detailnodes">
        <xsl:variable name="headercolor">#000000</xsl:variable>
        <xsl:variable name="detailcolor">#FF0000</xsl:variable>
        
        <tr>
            <td colspan="10">
                <font face="arial" size="1" color="{$headercolor}">
                    Test:&#160;<xsl:value-of select="@name"/>
                </font>
            </td>
        </tr>

        <xsl:if test="error">
            <tr>
                <td colspan="10">
                    <font face="arial" size="1" color="{$headercolor}">
                        Type: <xsl:value-of select="error/@type" />
                    </font>
                </td>
            </tr>
        <tr>
            <td colspan="10">
                <font face="arial" size="1" color="{$headercolor}">
                    Message: <xsl:value-of select="error/@message" />
                </font>
            </td>
        </tr>

        <tr/><tr/><tr/>

        <tr>
            <td colspan="10">
                <PRE>
                    <font face="arial" size="1" color="{$detailcolor}">
                        <xsl:value-of select="error" />
                    </font>
                </PRE>
            </td>
        </tr>
        </xsl:if>

        <xsl:if test="failure">
        <tr>
            <td colspan="10">
                <font face="arial" size="1" color="{$headercolor}">
                    Type: <xsl:value-of select="failure/@type" />
                </font>
            </td>
        </tr>
        <tr>
            <td colspan="10">
                <font face="arial" size="1" color="{$headercolor}">
                    Message: <xsl:value-of select="failure/@message" />
                </font>
            </td>
        </tr>

        <tr/><tr/><tr/>

        <tr>
            <td colspan="10">
                <PRE>
                    <font face="arial" size="1" color="{$detailcolor}">
                        <xsl:value-of select="failure" />
                    </font>
                </PRE>
            </td>
        </tr>
        </xsl:if>

        <tr/><tr/><tr/>
          
      </xsl:for-each>
    </xsl:template>

    <!-- Compilation Error Details -->
    <xsl:template match="message[@priority='warn']">
        <xsl:value-of select="text()"/><br/><br/>   
    </xsl:template>

    <!-- Modifications template -->
    <xsl:template match="modification">
        <tr>
            <xsl:if test="position() mod 2 = 0">
                <xsl:attribute name="bgcolor">#CCCCCC</xsl:attribute>   
            </xsl:if>

            <td colspan="2"><font size="1" face="arial"><xsl:value-of select="@type"/></font></td>
            <td colspan="2"><font size="1" face="arial"><xsl:value-of select="user"/></font></td>
            <td colspan="2"><font size="1" face="arial"><xsl:value-of select="project"/></font></td>
            <td colspan="2"><font size="1" face="arial"><xsl:value-of select="filename"/></font></td>
            <td colspan="2"><font size="1" face="arial"><xsl:value-of select="comment"/></font></td>
        </tr>
        
        <xsl:comment>Project: <xsl:value-of select="project"/></xsl:comment>        
    </xsl:template>

    <!-- jar and war template -->
    <xsl:template match="task[@name='Jar'] | task[@name='War']">
        <tr>
            <xsl:if test="position() mod 2 = 0">
                <xsl:attribute name="bgcolor">#CCCCCC</xsl:attribute>   
            </xsl:if>
            <td colspan="10">
                <nobr>
                    <font face="arial" size="1"><xsl:value-of select="message"/></font>
                </nobr>
            </td>
        </tr>
    </xsl:template>

</xsl:stylesheet>
