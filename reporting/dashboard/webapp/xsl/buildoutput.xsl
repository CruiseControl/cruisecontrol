<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="html" indent="yes" encoding="US-ASCII"/>
  <!--
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

-->

  <!--

  The purpose have this XSL is to provide a nice way to look at the output
  from the Ant XmlLogger (ie: ant -listener org.apache.tools.ant.XmlLogger )
  or the Exec builder output

  @author <a href="mailto:sbailliez@apache.org">Stephane Bailliez</a>

-->
  <xsl:decimal-format decimal-separator="." grouping-separator="," />

  <xsl:template match="/">
         <style type="text/css">
           #build_output {
           max-height: 650px;
           }

           #build_output_container .bannercell {
           border: 0px;
           padding: 0px;
           }

           #build_output_container {
           font-family: verdana;
           font-size: 13px;
           background-color:#FFFFFF;
           color:#000000;
           }

           #build_output_container table.status {
           background-color:#525D76;
           color:#ffffff;
           }

           #build_output_container table.log {

           }

           #build_output_container table.log tr td, tr th {

           }

           #build_output_container .error {
           color:red;
           }

           #build_output_container .warn {
           color:brown;
           }

           #build_output_container .info {
           color:gray;
           }

           #build_output_container .debug{
           color:gray;
           }

           #build_output_container .failed {
           background-color: red;
           color:#FFFFFF;
           font-weight: bold
           }

           #build_output_container .complete {
           background-color: #525D76;
           color:#FFFFFF;
           font-weight: bold
           }

           #build_output_container .a td {
           background: #f7f7f7;
           }

           #build_output_container .b td {
           background: #fff;
           }

           #build_output_container th, td {
           text-align: left;
           vertical-align: top;
           }

           #build_output_container th {
           background: #ccc;
           color: black;
           }

           #build_output_container table, th, td {
           border-color: #fff;
           border-width: 0 0 0 0;
           border-style: none;
           padding: 2px;
           }

           #build_output_container h3 {
           font: bold 13px verdana;
           background: #525D76;
           color: white;
           text-decoration: none;
           padding: 5px;
           margin-right: 0;
           margin-left: 0;
           margin-bottom: 0;
           margin-top: 10px;
           }
         </style>

        <div id="build_output_container">

        <xsl:apply-templates select="cruisecontrol/build"/>
        </div>
  </xsl:template>

  <xsl:template match="build">
    <!-- build status -->
    <table width="100%">
      <xsl:attribute name="class">
        <xsl:if test="@error">failed</xsl:if>
        <xsl:if test="not(@error)">complete</xsl:if>
      </xsl:attribute>
      <tr>
        <xsl:if test="@error">
          <td nowrap="yes">Build Failed</td>
        </xsl:if>
        <xsl:if test="not(@error)">
          <td nowrap="yes">Build Complete</td>
        </xsl:if>
        <td style="text-align:right" nowrap="yes">
          Total Time: <xsl:value-of select="@time"/>
        </td>
      </tr>
      <tr>
        <td colspan="2">
          <xsl:if test="@error">
            <tt>
              <xsl:value-of select="@error"/>
            </tt>
            <br/>
            <i style="font-size:80%">
              See the <a href="#stacktrace" alt="Click for details">stacktrace</a>.
            </i>
          </xsl:if>
        </td>
      </tr>

    </table>
    <!-- build information -->
    <h3>Build events</h3>
    <table class="log" border="1" cellspacing="2" cellpadding="3" width="100%">
      <tr>
        <th nowrap="yes" align="left" width="1%">target</th>
        <th nowrap="yes" align="left" width="1%">task</th>
        <th nowrap="yes" align="left">message</th>
      </tr>
      <xsl:apply-templates select=".//message[@priority != 'debug']"/>
    </table>
    <p>
      <!-- stacktrace -->
      <xsl:if test="stacktrace">
        <a name="stacktrace"/>
        <h3>Error details</h3>
        <table width="100%">
          <tr>
            <td>
              <pre>
                <xsl:value-of select="stacktrace"/>
              </pre>
            </td>
          </tr>
        </table>
      </xsl:if>
    </p>
  </xsl:template>

  <!-- report every message but those with debug priority -->
  <xsl:template match="message[@priority!='debug']">
    <tr valign="top">
      <!-- alternated row style -->
      <xsl:attribute name="class">
        <xsl:if test="position() mod 2 = 1">a</xsl:if>
        <xsl:if test="position() mod 2 = 0">b</xsl:if>
      </xsl:attribute>
      <td nowrap="yes" width="1%">
        <xsl:value-of select="../../@name"/>
      </td>
      <td nowrap="yes" style="text-align:right" width="1%">
        [ <xsl:value-of select="../@name"/> ]
      </td>
      <td class="{@priority}" nowrap="yes">
        <xsl:value-of select="text()"/>
      </td>
    </tr>
  </xsl:template>

</xsl:stylesheet>
