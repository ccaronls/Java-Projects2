<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%--
@author ccaron
--%>

<%@ include  file="common/taglibs.jsp" %>


<html:form action="/importFranchisePosition.do" enctype="multipart/form-data">
		<div id="formfields">
            <br/>
			<bean:message key="input.position.abbrev"/>  <html:text property="position" styleClass="formtextinput"/><br/><br/>
			<bean:message key="input.position.longname"/>  <html:text property="positionNameLong" styleClass="formtextinput"/><br/><br/>
			<bean:message key="input.spreadsheet.file"/>  <html:text property="spreadSheetFile" styleClass="formtextinput"/><br/><br/>
			<bean:message key="input.position.playercol"/>  <html:text property="playerColumn" styleClass="formtextinput" value="Player"/><br/><br/>
			<html:submit styleClass="formbutton">
				<bean:message key="button.import"/>
			</html:submit>
			<html:button property="unused" styleClass="formbutton" onclick="javascript:goURL('franchise.edit.url', 'franchiseId=${franchiseForm.franchiseId}')">
				<bean:message key="button.done"/>
			</html:button>						
		</div>
		<div id="formlist">
			<table class="formtable">
				<caption class="listcaption">
					<bean:message key="caption.statstrkd"/>
				</caption>
				<tr class="listheader">
					<th width="20%"><a href=""><bean:message key="list.header.name"/></a></th>
					<th width="50%"><a href=""><bean:message key="list.header.desc"/></a></th>
					<th width="30%"><bean:message key="list.header.action"/></th>
				</tr>
				<c:set var="rowstyle" value="listrow2"/>
	        	<c:forEach var="cur" items="${franchiseForm.stats}">
	        		<%@ include file="common/rowstyle.jsp" %>	        		
               		<tr class="${rowstyle}">	        		
               			<td>${cur.name}</td>
						<td>${cur.description}</td>
						<td>
							<a href="javascript:goURL('franchise.position.stat.delete', 'stat=${cur.name}')">
								<bean:message key="button.delete"/>
							</a>							
							<a href="">
								<bean:message key="button.save"/>
							</a>
						</td>
					</tr>
				</c:forEach>
			</table>
		</div>
</html:form>
	
