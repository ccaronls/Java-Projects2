<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%--
@author ccaron
--%>

<%@ include  file="common/taglibs.jsp" %>

<html:form action="/saveLeaguePositionStats.do">

	<div id="info" class="info">
		<bean:message key="info.leaguepositionedit"/>
	</div>
	<div id="formfields">
		<br/><br/>
		<html:button styleClass="formbutton" property="unused" onclick="javascript:Ajax.saveLeaguePositions('${leagueForm.position}', '${leagueForm.stats.size()}')">
			<bean:message key="button.save"/>
		</html:button>
		<html:cancel styleClass="formbutton">
			<bean:message key="button.cancel"/>
		</html:cancel>
	</div>
	<div id="formlist">
		<table class="formtable">
			<caption class="listcaption">
				<bean:message key="caption.leaguepositionstats" arg0="${leagueForm.name}" arg1="${leagueForm.position}"/>
			</caption>
			<tr class="listheader">
				<th width="20%"><bean:message key="list.header.stat"/></th>
				<th width="80%"><bean:message key="list.header.multiplier"/></th>
			</tr>
			<c:set var="index" value="0"/>
			<c:set var="rowstyle" value="listrow2"/>			
	       	<c:forEach var="cur" items="${leagueForm.stats}">
       			<%@ include file="common/rowstyle.jsp" %>
				<tr class="${rowstyle}">
					<td id="${index}"><c:out value="${cur.name}"/></td>
					<td>
						<html:text property="unused" styleId="${cur.name}" styleClass="width:80%" value="${cur.multiplier}"/>
						<html:button property="unused" value="+1" onclick="javascript:increment('${cur.name}', 1);"/>
						<html:button property="unused" value="-1" onclick="javascript:increment('${cur.name}', -1);"/>
						<html:button property="unused" value="zero" onclick="javascript:document.getElementById('${cur.name}').value='0.0'"/>
					</td>
				</tr>
				<c:set var="index" value="${index + 1}"/>
			</c:forEach>
		</table>
	</div>

</html:form>