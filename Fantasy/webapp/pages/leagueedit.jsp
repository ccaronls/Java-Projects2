<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%--
@author ccaron
--%>

<%@ include  file="common/taglibs.jsp" %>

<c:set var="canedit" value="${leagueForm.userId == sessionScope.user.id || sessionScope.user.hasAdminAccess()}"/>

<html:form action="/saveLeague.do">
	<div id="formfields">
		<bean:message key="select.franchise"/>
		<html:select styleId="franchiseId" styleClass="formselect" property="franchiseId" onchange="javascript:fetchFranchisePositions('franchiseId')">
			<c:if test="${leagueForm.franchiseId lt 0}">
				<option value=""><bean:message key="select.option.choose"/></option>
			</c:if>
			<c:forEach var="cur" items="${leagueForm.franchises}">
				<html:option value="${cur.id}"><c:out value="${cur.name}"/></html:option>
			</c:forEach>
		</html:select>
		<c:if test="${leagueForm.franchiseId ge 0}">
			<br/>
			<bean:message key="input.leaguename"/>
			<html:text readonly="${!canedit}" styleClass="formtextinput" property="name"/>
			<br/>
			<bean:message key="input.draftdate"/>
			<html:text property="draftDate" readonly="true" styleId="pickdraftdate" maxlength="19" value="${leagueForm.draftDate}"/>
			<c:if test="${canedit}">
				<a href="javascript:NewCal('pickdraftdate','mmddyyyy',false,12)">
					<img src="images/cal.gif" width="16" height="16" border="0" alt="Pick a date"/>
				</a>
			</c:if>
			<br/>
			<bean:message key="input.enddate"/>						
			<html:text property="endDate" readonly="true" styleId="pickenddate" maxlength="19" value="${leagueForm.endDate}"/>
			<c:if test="${canedit}">
				<a href="javascript:NewCal('pickenddate','mmddyyyy',false,12)">
					<img src="images/cal.gif" width="16" height="16" border="0" alt="Pick a date"/>
				</a>
			</c:if>
			<br/>
			<bean:message key="select.maxplayers"/>
			<c:choose>
				<c:when test="${canedit}">	
					<html:select styleClass="formselect" property="maxPlayers">
						<option>2</option>						
						<option>3</option>						
						<option>4</option>						
						<option>5</option>						
						<option>6</option>						
						<option>7</option>						
						<option>8</option>						
						<option>9</option>						
						<option>10</option>						
						<option>11</option>						
					</html:select>
				</c:when>
				<c:otherwise>
					<c:out value="${leagueForm.maxPlayers}"/>
				</c:otherwise>
			</c:choose>
			<br/>
			<br/>
			<html:cancel styleClass="formbutton"><bean:message key="button.cancel"/></html:cancel>
			<c:if test="${canedit}">
				<html:submit styleClass="formbutton"><bean:message key="button.save"/></html:submit>
			</c:if>
			<br/>		
		</div>
		<div id="formlist">
			<c:if test="${leagueForm.leagueId ge 0}">
				<table class="formtable">
					<caption class="listcaption">
						<bean:message key="caption.leaguepositions" arg0="${leagueForm.franchiseName}"/>
					</caption>
					<tr class="listheader">
						<th width="10%"><a href=""><bean:message key="list.header.position"/></a></th>
						<th width="10%"><a href=""><bean:message key="list.header.count"/></a></th>
						<th width="80%"><a href=""><bean:message key="list.header.stats"/></a></th>
					</tr>
					<c:set var="rowstyle" value="listrow2"/>			
			       	<c:forEach var="cur" items="${leagueForm.positions}">
			       		<%@ include file="common/rowstyle.jsp" %>
						<tr class="${rowstyle}">
							<td><c:out value="${leagueForm.getPositionName(cur.position)}"/></td>
							<td>
								<select name="select${cur.position}" style="width:100%" onchange="javascript:setLeaguePositionCount('select${cur.position}', '${cur.position}')">
									<c:forTokens var="num" items="0,1,2,3,4" delims=",">
										<c:choose>
											<c:when test="${num == cur.num}">
												<option selected="selected">${num}</option>
											</c:when>
											<c:otherwise>
												<option>${num}</option>
											</c:otherwise>
										</c:choose>
									</c:forTokens>
								</select>
							</td>
							<td>
								<c:if test="${canedit}">
									<a href="javascript:goURL('league.position.edit.url', 'position=${cur.position}')">
										<c:out value="${leagueForm.getStatsString(cur)}"/>
									</a>
								</c:if>
							</td>
						</tr>
					</c:forEach>
				</table>
			</c:if>
		</c:if> 
	</div>
</html:form>