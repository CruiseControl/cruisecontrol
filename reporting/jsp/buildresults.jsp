<%@page contentType="text/html"%>
<%@ taglib uri="/WEB-INF/cruisecontrol-jsp12.tld" prefix="cruisecontrol"%>
<html>
<head>
  <title></title>
  <link type="text/css" rel="stylesheet" href="css/cruisecontrol.css"/>
</head>
<body background="images/bluebg.gif" topmargin="0" leftmargin="0" marginheight="0" marginwidth="0">
  <table border="0" align="center" cellpadding="0" cellspacing="0" width="98%">
    <tr>
      <td>&nbsp;</td>
      <td background="images/bluestripestop.gif"><img src="images/blank20.gif" border="0"></td>
    </tr>
    <tr>
      <td width="180" valign="top">
        <img src="images/continuousintegration.gif" border="0"><br>
        <table border="0" align="center" width="98%">
            <cruisecontrol:nav>
                <tr><td><a class="link" href="<%= url %>"><%= linktext %></a></td></tr>
            </cruisecontrol:nav>
        </table>
      </td>
      <td valign="top" bgcolor="#FFFFFF">
         <cruisecontrol:xsl xslFile="xsl/cruisecontrol.xsl"/>
      </td>
    </tr>
    <tr>
      <td>&nbsp;</td>
      <td background="images/bluestripesbottom.gif"><img src="images/blank20.gif" border="0"></td>
    </tr>
  </table>
</body>
</html>