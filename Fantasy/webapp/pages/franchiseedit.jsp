<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%--
@author ccaron
--%>

<%@ include  file="common/taglibs.jsp" %>

<html:form action="/saveFranchise.do">
		<div id="formfields">
		
			<bean:message key="input.franchisename"/>  <html:text property="name"/><br/><br/>
			<bean:message key="input.franchisecategory"/>  <html:text styleId="cat" property="category"/><br/><br/>
            <select class="formselect" id="selcat" onchange="javascript:copyValue('selcat', 'cat')">
				<option value=""><bean:message key="select.option.choose"/></option>
                <c:forEach var="cur" items="${franchiseForm.categories}">
					<option>${cur}</option>
                </c:forEach>
            </select>			
			<br/><br/>
			<bean:message key="select.enabled"/>
			<html:select styleClass="formselect" property="active">
				<html:option value="true" key="select.option.true"/>
				<html:option value="false" key="select.option.false"/>
			</html:select>
			<br/><br/>
			
			<html:submit styleClass="formbutton">
				<bean:message key="button.save"/>
			</html:submit>
			<html:cancel styleClass="formbutton">
				<bean:message key='button.cancel'/>
			</html:cancel>

			<c:if test="${franchiseForm.franchiseId ge 0}">
				<html:button property="unused" styleClass="formbutton" onclick="javascript:goURL('franchise.position.edit', 'franchiseId=${franchiseForm.franchiseId}')">
					<bean:message key="button.import"/>
				</html:button>						
				<div id="formlist">
					<table class="formtable">
						<caption class="listcaption">
							<bean:message key="caption.positions"/>
							<%--
							<div id="pagination-top" class="pagination">
								<a href="">&lt; Previous &nbsp;</a>
								&nbsp;&nbsp;|
								<a href="">2 &nbsp;</a>|
								<a href="">3 &nbsp;</a>|
								4 &nbsp;|
								<a href="">&nbsp;&nbsp;Next &gt;</a>
							</div>					
							--%>
						</caption>
						<tr class="listheader">
							<th width="30%"><a href=""><bean:message key="list.header.name"/></a></th>
							<th width="60%"><a href=""><bean:message key="list.header.stats"/></a></th>
							<th width="10%"><bean:message key="list.header.action"/></th>
						</tr>
						<c:set var="rowstyle" value="listrow2"/>
						<c:forEach var="cur" items="${franchiseForm.positions}">
			        		<%@ include file="common/rowstyle.jsp" %>	        		
		               		<tr class="${rowstyle}">
			            		<td>${cur.longName}</td>
			            		<td>${franchiseForm.getStatsString(cur)}</td>
			            		<td>
			            			<a href="javascript:goURL('franchise.position.edit', 'franchiseId=${franchiseForm.franchiseId}&position=${cur.name}')">
		            					<bean:message key="button.edit"/>
		            				</a>
		            			</td>
							</tr>				
						</c:forEach>
					</table>		
		        </div>
		    </c:if>
	    </div>

</html:form>
      