<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%--
@author ccaron
--%>

<%@ page  contentType="text/html;charset=UTF-8" language="java" %>
<%@ include  file="common/taglibs.jsp" %>
<%@ taglib uri="/tags/struts-tiles" prefix="tiles" %>

<html:html xhtml="true">

	<head>
	   <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
	   <link href="<html:rewrite page="/css/style.css"/>" rel="stylesheet" type="text/css" />
	   <script type="text/javascript" src="<html:rewrite page="/includes/jsprops.jsp" />"></script>      
	   <script type="text/javascript" src="<html:rewrite page="/includes/AjaxEngine.js" />"></script>
	   <script type="text/javascript" src="<html:rewrite page="/includes/Ajax.js" />"></script>
	   <script type="text/javascript" src="<html:rewrite page="/includes/datetimepicker.js" />"></script>      
	   <script type="text/javascript" src="<html:rewrite page="/includes/ticker.js" />"></script>      
	   <script type="text/javascript" src="<html:rewrite page="/includes/fantasy.js" />"></script>
	   <title>Fantasy Sports</title>
	</head>
	
	<c:set var="pageTitle">
		<tiles:getAsString name="pageName"/>
	</c:set>
	
	<body>
		<div id="container" align="center">
			<c:if test="${sessionScope.user != null}">
				<div id="logoutbar">
					<br/><bean:message key="label.welcome"/> <c:out value="${sessionScope.user.firstName}"/>
					<a href="javascript:goURL('logout.url')">
						<bean:message key="button.logout"/>
					</a>
				</div>
			</c:if>
		
			<div id="header">
				<h1><bean:message key="header.fantasysports"/></h1>
				<h2>&brvbar; <bean:message key="${pageTitle}"/> &brvbar;</h2>
			</div>
			
			<div id="content">
				
				<tiles:insert attribute="sidemenu"/>
				<div class="form">
					<div id="messages" style="visibility:hidden">
					</div>
					<div id="errorMessage">
	        			<logic:messagesPresent message="true">
			            	<html:messages id="msg" message="true">
	    		            	<bean:write name="msg"/> <br/>
	            		    </html:messages>
			            </logic:messagesPresent>                    
        		    </div>
				
					<tiles:insert attribute="content"/>
				</div>
	        	
			</div>
			<div id="footer">
				<p>&copy; 2009 Chris Caron </p>
			</div>
		</div>
	</body>
</html:html>
