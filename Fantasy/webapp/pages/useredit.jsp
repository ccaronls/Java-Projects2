<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%@ include  file="common/taglibs.jsp" %>

<%--
@author ccaron
--%>

<html:form action="/saveUser.do">
		<div id="formfields">
			<br/><br/>
			<bean:message key="input.firstname"/>  <html:text styleClass="formtextinput" property="firstName"/>
			<br/><br/>
			<bean:message key="input.lastname"/>  <html:text styleClass="formtextinput" property="lastName"/>
			<br/><br/>
			<c:set var="readonly" value="${userForm.userId ge 0}"/>
			<bean:message key="input.username"/>  <html:text styleClass="formtextinput" readonly="${readonly}" property="userName"/>
			<br/><br/>
			<bean:message key="input.password"/>  <html:password styleClass="formtextinput" property="passWord"/>
			<br/><br/>
			<bean:message key="input.passwordconfirm"/>  <html:password styleClass="formtextinput" property="passWordConfirm"/>
			<br/><br/>
			<bean:message key="input.email"/> <html:text styleClass="formtextinput" property="email"/>
			<br/><br/>
			<c:if test="${sessionScope.user != null && sessionScope.user.hasAdminAccess() && sessionScope.user.id ne userForm.userId}">
				<bean:message key="select.active"/>
				<html:select styleClass="formselect" property="active">			
					<html:option value="true" key="select.option.true"/>
					<html:option value="false" key="select.option.false"/>
				</html:select>
				<br/><br/>
			</c:if>
			<c:choose>
				<c:when test="${userForm.numUsers eq 0 && userForm.userId lt 0}">
					<html:hidden property="access" value="ADMIN"/>
					<html:submit styleClass="formbutton">
						<bean:message key="button.create"/>
					</html:submit>
				</c:when>
				<c:when test="${userForm.userId lt 0}">
					<html:hidden property="access" value="TEAM"/>
					<html:submit styleClass="formbutton">
						<bean:message key="button.create"/>
					</html:submit>
				</c:when>
				<c:when test="${sessionScope.user != null && sessionScope.user.hasAdminAccess()}">
					<bean:message key="select.access"/>
					<html:select styleClass="formselect" property="access">
						<html:option value="TEAM" key="select.option.team"/>
						<html:option value="LEAGUE" key="select.option.league"/>
						<html:option value="ADMIN"  key="select.option.admin"/>
					</html:select>
					<br/><br/>
					<html:submit styleClass="formbutton">
						<bean:message key="button.save"/>
					</html:submit>
				</c:when>
				<html:cancel styleClass="formbutton" >
					<bean:message key="button.cancel"/>
				</html:cancel>
			</c:choose>
		</div>
</html:form>
