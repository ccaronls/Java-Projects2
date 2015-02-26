<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%--
@author ccaron
--%>

<%@ include  file="common/taglibs.jsp" %>

	<div id="formfields">
		<html:button property="unused" styleClass="formbutton" onclick="javascript:goURL('franchise.edit.url');">
			<bean:message key='button.newfranchise'/>
		</html:button>
	</div>
	<div id="formlist">
		<table class="formtable">
			<caption class="listcaption">
				<bean:message key="caption.franchises"/>
			</caption>
			<tr class="listheader">
				<th width="20%"><a href=""><bean:message key="list.header.name"/></a></th>
				<th width="20%"><a href=""><bean:message key="list.header.category"/></a></th>
				<th width="10%"><a href=""><bean:message key="list.header.leagues"/></a></th>
				<th width="10%"><a href=""><bean:message key="list.header.active"/></a></th>
				<th width="40%"><a href=""><bean:message key="list.header.action"/></a></th>	
			</tr>
			<c:set var="rowstyle" value="listrow2"/>			
	       	<c:forEach var="cur" items="${franchiseForm.franchises}">
	       		<%@ include file="common/rowstyle.jsp" %>
				<tr class="${rowstyle}">
					<td><c:out value="${cur.name}"/></td>
		            <td><c:out value="${cur.category}"/></td>
		            <td><c:out value="${franchiseForm.getNumLeagues(cur)}"/></td>
		            <td><c:out value="${cur.active}"/></td>
		            <td>
		            	<a href="javascript:goURL('franchise.edit.url', 'franchiseId=${cur.id}')">
		            		<bean:message key="button.edit"/>
		                </a>
			            <c:choose>
			            	<c:when test="${cur.active}">
			                <%-- Ajax here --%>
			             	</c:when>
			                <c:otherwise>
			                <%-- Ajax here --%>
			                </c:otherwise>
			            </c:choose>
			        </td>
			    </tr>	        	
	        </c:forEach>

		</table>
	</div>
        	
