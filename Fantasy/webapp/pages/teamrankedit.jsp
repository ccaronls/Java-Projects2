<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%--
@author ccaron
--%>

<%@ include  file="common/taglibs.jsp" %>


<html:form action="/saveTeam.do">
		<div id="formfields">
			<html:button property="unused" styleClass="formbutton" onclick="javascript:Ajax.submitTeamRank(${teamForm.playersToRank.size()})">
				<bean:message key="button.save"/>
			</html:button>
			<html:cancel styleClass="formbutton">
				<bean:message key="button.cancel"/>
			</html:cancel>						
		</div>
		<div id="formlist">
			<table class="formtable">
				<caption class="listcaption">
					<bean:message key="caption.rankplayers"/> <c:out value="${teamForm.positionToRank}"/>
				</caption>
				<tr class="listheader">
					<th width="10%"><bean:message key="list.header.rank"/></th>
					<th width="60%"><bean:message key="list.header.player"/></th>
					<th width="30%"><bean:message key="list.header.action"/></th>
				</tr>
				<c:set var="rowstyle" value="listrow2"/>			
				<c:set var="rank" value="1"/>			
		       	<c:forEach var="cur" items="${teamForm.playersToRank}">
		       		<%@ include file="common/rowstyle.jsp" %>
					<tr class="${rowstyle}">
						<td><c:out value="${rank}"/></td>
						<td id="${rank}"><c:out value="${teamForm.getPlayerName(cur.playerId)}"/></td>
						<html:hidden property="unused" styleId="id${rank}" value="${cur.playerId}"/>
						<td>
						<c:choose>
							<c:when test="${rank == 1}">
								<html:button property="unused" onclick="javascript:moveRank(${rank}, ${rank+1})">
									<bean:message key="button.down"/>
								</html:button>
							</c:when>
							<c:when test="${rank == teamForm.players.size()-1}">
								<html:button property="unused" onclick="javascript:moveRank(${rank}, 1)">
									<bean:message key="button.top"/>
								</html:button>
								<html:button property="unused" onclick="javascript:moveRank(${rank}, ${rank-1})">
									<bean:message key="button.up"/>
								</html:button>
							</c:when>
							<c:otherwise>
								<html:button property="unused" onclick="javascript:moveRank(${rank}, 1)">
									<bean:message key="button.top"/>
								</html:button>
								<html:button property="unused" onclick="javascript:moveRank(${rank}, ${rank-1})">
									<bean:message key="button.up"/>
								</html:button>
								<html:button property="unused" onclick="javascript:moveRank(${rank}, ${rank+1})">
									<bean:message key="button.down"/>
								</html:button>
							</c:otherwise>
						</c:choose>
					</tr>
					<c:set var="rank" value="${rank + 1}"/>
				</c:forEach>
			</table>
		</div>
</html:form>