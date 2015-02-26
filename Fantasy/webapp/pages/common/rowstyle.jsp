<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<c:choose>
	<c:when test="${rowstyle == 'listrow1'}">
		<c:set var="rowstyle" value="listrow2"/>
	</c:when>
	<c:otherwise>
		<c:set var="rowstyle" value="listrow1"/>
	</c:otherwise>
</c:choose>
