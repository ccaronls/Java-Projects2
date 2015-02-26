<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%--
@author ccaron
--%>

<%@ include  file="common/taglibs.jsp" %>


	<div id="formfields">
		<html:button styleClass="formbutton" property="unused" onclick="javascript:goURL('league.edit.url')">
			<bean:message key="button.newleague"/>
		</html:button>
	</div>
	<div id="formlist">
		<table class="formtable">
			<caption class="listcaption">
				<bean:message key="caption.leagues"/>
			</caption>
			<tr class="listheader">
				<th width="15%"><a href=""><bean:message key="list.header.league"/></a></th>
				<th width="15%"><a href=""><bean:message key="list.header.franchise"/></a></th>
				<th width="3%"><a href=""><bean:message key="list.header.teams"/></a></th>
				<th width="17%"><a href=""><bean:message key="list.header.status"/></a></th>
				<th width="15%"><a href=""><bean:message key="list.header.leader"/></a></th>
				<th width="35%"><a href=""><bean:message key="list.header.action"/></a></th>
			</tr>
			<c:set var="rowstyle" value="listrow2"/>			
	       	<c:forEach var="cur" items="${leagueForm.leagues}">
       			<%@ include file="common/rowstyle.jsp" %>
				<tr class="${rowstyle}">
					<td><c:out value="${cur.name}"/></td>
					<td><c:out value="${cur.getFranchise().name}"/></td>
					<td><c:out value="${leagueForm.getTeams(cur.id).size()}"/></td>
					<td><c:out value="${cur.getStatus()}"/></td>
					<td><c:out value="${legaueForm.getUser(cur.userId).userName}"/></td>
					<td>
						<c:if test="${sessionScope.user.hasAdminAccess() && cur.getStatus() eq OPEN}">
							<a href="javascript:goURL('league.rundraft.url', 'leagueId=${cur.id}')">
								<bean:message key="button.rundraft"/>
							</a>
						</c:if>
						<a href="javascript:goURL('league.edit.url', 'leagueId=${cur.id}')">
							<c:if test="${cur.userId eq sessionScope.user.id}">
								<bean:message key="button.edit"/>
							</c:if>
							<c:if test="${cur.userId ne sessionScope.user.id}">
								<bean:message key="button.details"/>
							</c:if>
						</a>
					</td>
				</tr>
			</c:forEach>
		</table>
	</div>
