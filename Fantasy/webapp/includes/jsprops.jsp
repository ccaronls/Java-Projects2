<%@ page language="java" session="true" contentType="text/javascript; charset=UTF-8" %>

<%@taglib   uri="http://jakarta.apache.org/struts/tags-bean"    prefix="bean"%>
<%@taglib   uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

function jsMap(){
	this.set = function(key,value) {this[key] = value;};
	this.get = function(key) {return this[key];};
}

var jsprops = new jsMap();

jsprops.set("logout.url",						"<c:url value='/logout.do'/>						");
jsprops.set("newuser.url",						"<c:url value='/newUser.do'/>						");
jsprops.set("user.edit.url",					"<c:url value="/editUser.do"/>						");
jsprops.set("user.setactive.url",				"<c:url value="/setUserActive.do"/>					");

jsprops.set("franchise.edit.url", 				"<c:url value='/editFranchise.do'/>					");
jsprops.set("franchise.positions.url",			"<c:url value='/getFranchisePositions.do'/>			");
jsprops.set("franchise.position.edit",			"<c:url value='/franchisePositionEdit.do'/>			");
jsprops.set("franchise.position.stat.delete",	"<c:url value='/franchisePositionStatDelete.do'/>	");

jsprops.set("league.edit.url",					"<c:url value='/editLeague.do'/>					");
jsprops.set("league.position.count.set.url",	"<c:url value='/setLeaguePosition.do'/>				");
jsprops.set("league.position.edit.url",			"<c:url value='/editLeaguePosition.do'/>			");
jsprops.set("league.position.stats.submit.url",	"<c:url value='/saveLeaguePositionStats.do'/>		");
jsprops.set("league.position.stats.done.url",	"<c:url value='/saveLeaguePositionStatsDone.do'/>	");
jsprops.set("join.league.url",					"<c:url value='/joinLeague.do'/>					");
jsprops.set("league.rundraft.url",				"<c:url value="/leagueDraftRun.do"/>				");

jsprops.set("team.edit.url",					"<c:url value='/editTeam.do'/>						");
jsprops.set("team.rank.edit.url",				"<c:url value='/teamRankEdit.do'/>					");
jsprops.set("team.rank.submit.url",				"<c:url value='/teamRankSubmit.do'/>				");
jsprops.set("team.ranks.save.done.url",			"<c:url value="/teamRankSaveDone.do"/>				");
jsprops.set("team.monitor.url",					"<c:url value="/teamMonitor.do"/>					");

jsprops.set("button.activate",					"<bean:message key="button.activate"/>				");
jsprops.set("button.deactivate",				"<bean:message key="button.deactivate"/>			");
jsprops.set("message.user.activated",			"<bean:message key="message.user.activated"/>		");
jsprops.set("message.user.deactivated",			"<bean:message key="message.user.deactivated"/>		");