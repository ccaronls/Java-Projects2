<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%--
@author ccaron
--%>

<%@ include  file="common/taglibs.jsp" %>

<html:form action="/saveTeam.do">

	<div id="formfields">
		<br/><br/>
		<bean:message key="label.league"/><c:out value="${teamForm.leagueName}"/>
		<br/><br/>
		<bean:message key="input.teamname"/>
		<html:text property="name" styleClass="formtextinput"/>
		<br/><br/>
		<html:submit styleClass="formbutton">
			<bean:message key="button.submit"/>
		</html:submit>
		<html:cancel styleClass="formbutton">
			<bean:message key="button.cancel"/>
		</html:cancel>
	</div>
	<c:if test="${teamForm.teamId ge 0}">
		<div id="formlist">
			<table class="formtable">
				<caption class="listcaption">
					<bean:message key="caption.rankplayers"/>
				</caption>
				<tr class="listheader">
					<th width="30%"><a href=""><bean:message key="list.header.position"/></a></th>
					<th width="60%"><a href=""><bean:message key="list.header.toppick"/></a></th>
					<th width="10%"><bean:message key="list.header.action"/></th>
				</tr>
				<c:set var="rowstyle" value="listrow2"/>			
		       	<c:forEach var="cur" items="${teamForm.positions}">
	       			<%@ include file="common/rowstyle.jsp" %>
					<tr class="${rowstyle}">
						<td><c:out value="${teamForm.getPositionName(cur.position)}"/></td>
						<td><c:out value="${teamForm.getTopChoicePlayer(cur.position)}"/></td>
						<td>
							<a href="javascript:goURL('team.rank.edit.url', 'position=${cur.position}')">
								<bean:message key="button.edit"/>
							</a>
						</td>
					</tr>
				</c:forEach>
			</table>
		</div>
	</c:if>
	
</html:form>					