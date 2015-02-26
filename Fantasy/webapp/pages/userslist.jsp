<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%--
@author ccaron
--%>

<%@ include  file="common/taglibs.jsp" %>

<div id=formfields">

	<html:form action="/editUser.do">
		<html:submit styleClass="formbutton" ><bean:message key="button.newuser"/></html:submit>
	</html:form>	
	<br/>
	<br/>		

	<div id="formlist">		
		<table class="formtable">
			<caption class="listcaption">
				<bean:message key="caption.users"/>
			</caption>
			<tr class="listheader">
				<th width="15%"><a href=""><bean:message key="list.header.name"/></a></th>
				<th width="15%"><a href=""><bean:message key="list.header.username"/></a></th>
				<th width="10%"><a href=""><bean:message key="list.header.access"/></a></th>
				<th width="5%"><a href=""><bean:message key="list.header.teams"/></a></th>
				<th width="5%"><a href=""><bean:message key="list.header.leagues"/></a></th>
				<th width="20%"><a href=""><bean:message key="list.header.lastlogin"/></a></th>
				<th width="10%"><a href=""><bean:message key="list.header.active"/></a></th>
				<th width="20%"><bean:message key="list.header.action"/></th>
			</tr>
			<c:set var="rowstyle" value="listrow2"/>			
	       	<c:forEach var="cur" items="${userForm.users}">
	       		<%@ include file="common/rowstyle.jsp" %>
	       		<tr class="${rowstyle}">
					<td>${cur.lastName}, ${cur.firstName}</td>
					<td>${cur.userName}</td>
					<td>${cur.access}</td>
					<td>${userForm.getNumTeams(cur)}</td>
					<td>${userForm.getNumLeagues(cur)}</td>
					<td>${userForm.getDateString(cur.lastLogin)}</td>
					<td>${cur.active}</td>
					<td>
						<c:if test="${sessionScope.user.id ne cur.id}">
							<c:choose>
								<c:when test="${cur.active}">
									<a href="javascript:setUserActive('${cur.id}', 'false')"><bean:message key="button.deactivate"/></a>
								</c:when>
								<c:otherwise>
									<a href="javascript:setUserActive('${cur.id}', 'true')"><bean:message key="button.activate"/></a>
								</c:otherwise>
							</c:choose>
						</c:if>
						<a href="javascript:goURL('user.edit.url', 'userId=${cur.id}')">
							<bean:message key="button.edit"/>
						</a>
					</td>
				</tr>
			</c:forEach>
		</table>
	</div>

</div>