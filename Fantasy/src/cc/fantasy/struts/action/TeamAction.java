package cc.fantasy.struts.action;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import com.thoughtworks.xstream.XStream;

import cc.fantasy.model.League;
import cc.fantasy.model.Team;
import cc.fantasy.model.TeamPlayer;
import cc.fantasy.model.User;
import cc.fantasy.model.type.LeagueStatus;
import cc.fantasy.service.Search;
import cc.fantasy.struts.form.TeamForm;
import cc.fantasy.util.Comparators;

public class TeamAction extends BaseAction {

    public ActionForward searchTeamsAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        TeamForm teamForm = (TeamForm)form;
        Search search = null;
        String userId = request.getParameter("userId");
        if (userId == null) {
            teamForm.setTeams(getService().getTeams(null, search));
        } else {
            User user = getService().getUser(Integer.parseInt(userId));
            teamForm.setTeams(getService().getTeams(user, search));
        }
        teamForm.setFranchiseId(-1);
        teamForm.setLeagueId(-1);
        teamForm.setName(null);
        teamForm.setTeamId(-1);
        
        return mapping.findForward(SUCCESS);
    }

    public ActionForward joinLeagueAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        TeamForm teamForm = (TeamForm)form;
        
        teamForm.setLeagues(getService().getLeaguesByStatus(LeagueStatus.OPEN, null));
        
        return mapping.findForward(SUCCESS);
    }

    public ActionForward editTeamAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        
        TeamForm teamForm = (TeamForm)form;
    	String teamIdStr = request.getParameter("teamId");

    	Team team = null;
    	League league = null;
    	String forward = null;
    	
    	if (teamIdStr == null) {    		
    		// creating a new team, better have leagueId specified
        	int leagueId = Integer.parseInt(request.getParameter("leagueId"));
        	league = getService().getLeague(leagueId);
        	team = new Team();
        	team.setLeagueId(leagueId);
        	getService().getFranchise(league.getFranchiseId()).populateTeamPlayers(league, team);
        	forward = "new";
    	} else {
            int teamId = Integer.parseInt(teamIdStr);
            team = getService().getTeam(teamId);
            league = getService().getLeague(team.getLeagueId());
            forward = "edit";
    	}

        teamForm.setLeagueId		(team.getLeagueId());
        teamForm.setLeagueName		(league.getName());
        teamForm.setFranchiseId		(league.getFranchiseId());
        teamForm.setTeamId			(team.getId());
        teamForm.setPlayers			(team.getPlayers());
        teamForm.setPositions		(league.getPositions());   
        teamForm.setName			(team.getName());
        teamForm.setPoints			(team.getPoints());
    	
    	return mapping.findForward(forward);
    	
    }    
    
    public ActionForward editTeamPositionRankAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        TeamForm teamForm = (TeamForm)form;

        String position = request.getParameter("position");
        Team team = null;
        if (teamForm.getTeamId() < 0) {
            team = new Team();
        } else {
            team = getService().getTeam(teamForm.getTeamId());
        }
        League league = getService().getLeague(teamForm.getLeagueId());
        getService().getFranchise(teamForm.getFranchiseId()).populateTeamPlayers(league, team);
        teamForm.setPlayersToRank(team.getPlayersForPosition(position));
        Collections.sort(teamForm.getPlayersToRank(), Comparators.getTeamPlayerRankComparator());
        teamForm.setPositionToRank(position);
        
        return mapping.findForward(SUCCESS);
    }
    
    TeamPlayer getPlayer(List<TeamPlayer> players, int playerId) {
        Iterator<TeamPlayer> it = players.iterator();
        while (it.hasNext()) {
            TeamPlayer player = it.next();
            if (player.getPlayerId() == playerId)
                return player;
        }
        return null;
    }

    
    public ActionForward teamRankSubmitAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException {
        TeamForm teamForm = (TeamForm)form;

        XStream stream = new XStream();
        
        List<Integer> newPlayerRanks = (List)readXml(request, stream);

        // merge the new ranks in
        int rank = 1;
        Iterator<Integer> it = newPlayerRanks.iterator();
        while (it.hasNext()) {
            getPlayer(teamForm.getPlayersToRank(), it.next()).setRank(rank ++);
        }
        
        teamForm.setPlayersToRank(new ArrayList());
        return mapping.findForward(SUCCESS);
    }
    
    public ActionForward saveTeamAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        TeamForm teamForm = (TeamForm)form;
        
        Team team = null;
        if (teamForm.getTeamId() < 0)
            team = new Team();
        else
            team = getService().getTeam(teamForm.getTeamId());

        try {
            team.setLeagueId(teamForm.getLeagueId());
            team.setName(teamForm.getName());
            team.setPlayers(teamForm.getPlayers());
            team.setUserId(getUser(request).getId());
            getService().addOrUpdateTeam(team);
            teamForm.setTeams(getService().getTeams(getUser(request), null));
        } catch (Exception e) {
            this.setErrorMessage(request, e);
            return mapping.findForward(FAILED);
        }
        return mapping.findForward(SUCCESS);
    }
    
    public ActionForward teamMonitorAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        TeamForm teamForm = (TeamForm)form;

        int teamId = Integer.parseInt(request.getParameter("teamId"));
        Team team = getService().getTeam(teamId);
        
        teamForm.setName(team.getName());
        teamForm.setPoints(team.getPoints());
        teamForm.setLeagueId(team.getLeagueId());
        teamForm.setPlayers(team.getPlayers());
        teamForm.setPositions(getService().getLeague(team.getLeagueId()).getPositions());
        teamForm.setTeamId(teamId);
        
        return mapping.findForward(SUCCESS);
    }
    /*
    public ActionForward searchTeamsAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        return mapping.findForward(SUCCESS);
    }
    public ActionForward searchTeamsAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        return mapping.findForward(SUCCESS);
    }
    public ActionForward searchTeamsAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        return mapping.findForward(SUCCESS);
    }
    public ActionForward searchTeamsAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        return mapping.findForward(SUCCESS);
    }
    public ActionForward searchTeamsAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        return mapping.findForward(SUCCESS);
    }
    */
    
    
}
