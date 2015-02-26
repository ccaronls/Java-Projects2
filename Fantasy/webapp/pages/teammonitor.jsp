<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%--
@author ccaron
--%>

<%@ include  file="common/taglibs.jsp" %>

	<div id="formfields">
		<br/><br/>
		<bean:message key="label.teamname"/> <c:out value="${teamForm.name}"/>
		<br/><br/>
		<bean:message key="lebel.teampoints"/> <c:out value="${teamForm.points}"/>
		<br/>
	</div>
	<div id="formlist">
		<table class="formtable">
			<caption class="listcaption">
				<bean:message key="caption.positions"/>
			</caption>
			<tr class="listheader">
				<th width=""><a href=""><bean:message key="list.header.position"/></a></th>
				<th width=""><a href=""><bean:message key="list.header.player"/></a></th>
				<th width=""><a href=""><bean:message key="list.header.stats"/></a></th>
				<th width=""><bean:message key="list.header.action"/></th>
			</tr>
			<c:set var="rowstyle" value="listrow2"/>			
	       	<c:forEach var="cur" items="${teamForm.players}">
       			<%@ include file="common/rowstyle.jsp" %>
				<tr class="listrow1">
					<td><c:out value="${cur.position}"/></td>
					<td><c:out value="${teamForm.getPlayerName(cur.playerId)}"/></td>
					<td><c:out value="${teamForm.getPlayerStats(cur.playerId, cur.position)}"/></td>
					<td>
						<c:choose>
							<c:when test="${teamForm.getLeagueStatus(teamForm.leagueId) eq 'CLOSED'}">
								<a href="javascript:Ajax.openChangePlayerPopup(${cur.position})">
									<bean:message key="button.changeplayer"/>
								</a>
							</c:when>
							<c:when test="${teamForm.getLeagueStatus() eq 'OPEN'}">
							</c:when>
						</c:choose>
					</td>
				</tr>
			</c:forEach>
		</table>
	</div>
