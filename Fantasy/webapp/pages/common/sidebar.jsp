<%@taglib   uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib   uri="http://jakarta.apache.org/struts/tags-html" prefix="html"%>
<%@taglib   uri="http://jakarta.apache.org/struts/tags-bean" prefix="bean"%>
<%@taglib   uri="http://jakarta.apache.org/struts/tags-logic" prefix="logic"%>

<div id="sideMenu">
	<html:link page="/">
		<bean:message key="sidebutton.home"/>
	</html:link>
	<br/>
	<c:if test="${sessionScope.user.hasAdminAccess()}">
		<html:link page="/searchFranchises.do">
			<bean:message key="sidebutton.franchises"/>
		</html:link>
		<br/>
		<html:link page="/searchLeagues.do">
			<bean:message key="sidebutton.leagues"/>
		</html:link>
		<br/>
		<html:link page="/searchTeams.do">
			<bean:message key="sidebutton.teams"/>
		</html:link>
		<br/>
		<html:link page="/searchUsers.do">
			<bean:message key="sidebutton.users"/>
		</html:link>
		<br/>
    </c:if>
	<c:if test="${sessionScope.user.hasLeagueAccess()}">
        <html:link page="/searchMyLeagues.do?userId=${sessionScope.user.id}">
        	<bean:message key="sidebutton.myleagues"/>
        </html:link>
        <br/>
    </c:if>
    <html:link page="/searchMyTeams.do?userId=${sessionScope.user.id}">
    	<bean:message key="sidebutton.myteams"/>
    </html:link>
    <br/>
</div>
