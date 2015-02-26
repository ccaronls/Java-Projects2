<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%--
@author ccaron
--%>

<%@ include  file="common/taglibs.jsp" %>


	<div id="formlist">
		<table class="formtable">
			<caption class="listcaption">
				<bean:message key="caption.joinleague"/>
			</caption>
			<tr class="listheader">
				<th width="20%"><a href=""><bean:message key="list.header.name"/></a></th>
				<th width="20%"><a href=""><bean:message key="list.header.franchise"/></a></th>
				<th width="15%"><a href=""><bean:message key="list.header.manager"/></a></th>
				<th width="15%"><a href=""><bean:message key="list.header.fee"/></a></th>
				<th width="15%"><a href=""><bean:message key="list.header.draftdate"/></a></th>
				<th width="15%"><a href=""><bean:message key="list.header.action"/></a></th>
			</tr>
			<c:set var="rowstyle" value="listrow2"/>			
	       	<c:forEach var="cur" items="${teamForm.leagues}">
       			<%@ include file="common/rowstyle.jsp" %>
				<tr class="${rowstyle}">
					<td><c:out value="${cur.name}"/></td>
					<td><c:out value="${teamForm.getFranchise(cur.franchiseId).name}"/></td>
					<td><c:out value="${teamForm.getUser(cur.userId).userName}"/></td>
					<td>FREE</td>
					<td><c:out value="${teamForm.getDateString(cur.draft)}"/></td>
					<td>
						<a href="javascript:goURL('league.edit.url', 'leagueId=${cur.id}')">
							<bean:message key="button.details"/>
						</a>
						<a href="javascript:goURL('team.edit.url', 'leagueId=${cur.id}')">
							<bean:message key="button.join"/>
						</a>
					</td>
				</tr>
			</c:forEach>
		</table>
	</div>
