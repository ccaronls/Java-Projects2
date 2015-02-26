<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%--
@author ccaron
--%>

<%@ include  file="common/taglibs.jsp" %>

<html:form action="/saveTeam.do">

	<div id="formfields">
		<br/><br/>
		<html:button styleClass="formbutton" property="unused" onclick="javascript:goURL('join.league.url');">
			<bean:message key="button.joinleague"/>
		</html:button>
	</div>
	<div id="formlist">
		<table class="formtable">
			<caption class="listcaption">
				<bean:message key="caption.teams"/>
			</caption>
			<tr class="listheader">
				<th width="20%"><a href=""><bean:message key="list.header.name"/></a></th>
				<th width="10%"><a href=""><bean:message key="list.header.league"/></a></th>
				<th width="10%"><a href=""><bean:message key="list.header.points"/></a></th>
				<th width="10%"><a href=""><bean:message key="list.header.rank"/></a></th>
				<th width="10%"><a href=""><bean:message key="list.header.leader"/></a></th>
				<th width="10%"><a href=""><bean:message key="list.header.status"/></a></th>
				<th width="10%"><a href=""><bean:message key="list.header.date"/></a></th>
				<th width="20%"><a href=""><bean:message key="list.header.action"/></a></th>
			</tr>
			<c:set var="rowstyle" value="listrow2"/>			
	       	<c:forEach var="cur" items="${teamForm.teams}">
	       		<%@ include file="common/rowstyle.jsp" %>
				<tr class="${rowstyle}">
					<td><c:out value="${cur.name}"/></td>
					<td><c:out value="${teamForm.getLeague(cur.leagueId).name}"/></td>
					<td><c:out value="${cur.points}"/></td>
					<td><c:out value="${teamForm.getTeamRank(cur)}"/></td>
					<td>
						<c:if test="${teamForm.getLeague(cur.leagueId).getStatus() ne OPEN}">
							<c:out value="${teamForm.getLeagueLeader(cur.leagueId)}"/>
						</c:if>
					</td>
					<td><c:out value="${teamForm.getLeague(cur.leagueId).getStatus()}"/></td>
					<td><c:out value="${teamForm.getDateString(teamForm.getLeague(cur.leagueId).getStatusDate())}"/></td>
					<td>
						<c:choose>
							<c:when test="${teamForm.getLeagueStatus(cur.leagueId) eq OPEN}">
								<a href="javascript:goURL('team.edit.url', 'teamId=${cur.id}')">
									<bean:message key="button.edit"/>
								</a>
							</c:when>
							<c:when test="${teamForm.getLeagueStatus(cur.leagueId) eq CLOSED}">
								<a href="javascript:goURL('team.monitor.url', 'teamId=${cur.id}')">
									<bean:message key="button.monitor"/>
								</a>
							</c:when>
							<c:when test="${teamForm.getLeagueStatus(cur.leagueId) eq DONE}">
								<a href="javascript:goURL('team.edit.url', 'teamId=${cur.id}')">
									<bean:message key="button.details"/>
								</a>
							</c:when>
						</c:choose>							
					</td>
				</tr>
			</c:forEach>
		</table>
	</div>

</html:form>