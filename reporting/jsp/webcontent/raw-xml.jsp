<%@ taglib uri="webwork" prefix="ww" %>
<html>
<head>
    <title>CruiseControl Raw XML Configuration</title>
    <link type="text/css" rel="stylesheet" href="css/cruisecontrol.css"/>    
</head>

<body>

    <ww:if test="hasActionMessages()">
        <div id="result-messages" class="config-result-messages">
            <ul>
                <ww:iterator value="actionMessages">
                    <li class="config-result-message"><ww:property/></li>
                </ww:iterator>
            </ul>
            <hr/>
        </div>
    </ww:if>

    <ww:form action="saveContents" id="project-configuration"
             name="project-configuration" method="post">
        <ww:textarea name="contents" rows="24" cols="80"/>
        <ww:submit value="Save" cssClass="config-button"/>
    </ww:form>

    <a href="<ww:url value="config.jspa"/>">Return to configuration editor</a>
</body></html>