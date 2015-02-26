<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%-- 
	@author ccaron 
--%>

<%@ include  file="common/taglibs.jsp" %>

<html:form action="login" focus="userName">
		<div id="formfields">
			<span class="heading">
	      		<bean:message key="info.pleaselogin"/>
			</span>
			<br/><br/>
			<span>&nbsp;</span>
			<br/>
			<bean:message key="input.username"/>
			<html:text property="userName"/>
			<br/>
			<bean:message key="input.password"/>
			<html:password property="passWord"/>
		   	<br/><br/>
		   	<html:button property="unused" styleClass="formbutton" onclick="javascript:goURL('newuser.url')">
		   		<bean:message key='button.newuser'/>
		   	</html:button>
			<html:submit styleClass="formbutton">
				<bean:message key="button.login"/>
			</html:submit>
		</div>
</html:form>
