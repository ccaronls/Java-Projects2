package cc.fantasy.struts.action;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import cc.fantasy.model.Franchise;
import cc.fantasy.model.League;
import cc.fantasy.model.LeaguePosition;
import cc.fantasy.model.LeagueStat;
import cc.fantasy.model.Position;
import cc.fantasy.model.User;
import cc.fantasy.service.Search;
import cc.fantasy.struts.form.LeagueForm;
import cc.fantasy.struts.util.Common;
import cc.fantasy.update.DraftUpdate;
import cc.fantasy.xml.LeagueXmlStream;

public class LeagueAction extends BaseAction {

    public ActionForward searchLeaguesAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        LeagueForm leagueForm = (LeagueForm)form;
        Search search = null;
        String userId = request.getParameter("userId");
        if (userId == null) {
            leagueForm.setLeagues(getService().getLeagues(search));
        } else {
            User user = getService().getUser(Integer.parseInt(userId));
            leagueForm.setLeagues(getService().getLeagues(user, search));
        }
        return mapping.findForward(SUCCESS);
    }

    public ActionForward editLeagueAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        LeagueForm leagueForm = (LeagueForm)form;
        leagueForm.setFranchises(getService().getFranchises(null));
        
        String leagueId = request.getParameter("leagueId");
        
        // TODO: Move to a 'reset' method in the form and figure out when to call that function
        leagueForm.setPosition(null);
        leagueForm.setPositionId(-1);
        leagueForm.setPositions(new ArrayList());
        leagueForm.setStats(new ArrayList());

        if (leagueId == null) {
            // new league
            leagueForm.setLeagueId(-1);
            leagueForm.setDraftDate(null);
            leagueForm.setEndDate(null);
            leagueForm.setFranchiseId(-1);
            leagueForm.setMaxPlayers(0);
            leagueForm.setName(null);
            leagueForm.setUserId(getUser(request).getId());
        } else {
            // existing league
            int id = Integer.parseInt(leagueId);
            League league = getService().getLeague(id);
            leagueForm.setLeagueId(id);
            leagueForm.setDraftDate(Common.dateToString(league.getDraft(), request.getLocale()));
            leagueForm.setEndDate(Common.dateToString(league.getEnding(), request.getLocale()));
            leagueForm.setFranchiseId(league.getFranchiseId());
            leagueForm.setMaxPlayers(league.getMaxPlayers());
            leagueForm.setName(league.getName());
            leagueForm.setPositions(league.getPositions());
            leagueForm.setUserId(league.getUserId());
        }
        
        return mapping.findForward(SUCCESS);
    }

    public ActionForward saveLeagueAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        
        LeagueForm leagueForm = (LeagueForm)form;
        
        try {
            League league = null;
            if (leagueForm.getLeagueId() < 0) {
                league = new League();
            }
            else {
                league = getService().getLeague(leagueForm.getLeagueId());
            }
            
            league.setCreated(new Date());
            league.setDraft(Common.stringToDate(leagueForm.getDraftDate(), request.getLocale()));
            league.setEnding(Common.stringToDate(leagueForm.getEndDate(), request.getLocale()));
            league.setFranchiseId(leagueForm.getFranchiseId());
            league.setMaxPlayers(leagueForm.getMaxPlayers());
            league.setName(leagueForm.getName());
            league.setPositions(leagueForm.getPositions());
            league.setUserId(getUser(request).getId());
            
            getService().addOrUpdateLeague(league);
            leagueForm.setLeagueId(league.getId());
            
            return mapping.findForward(SUCCESS);
        } catch (Exception e) {
            this.setErrorMessage(request, e);
            return mapping.findForward(FAILED);
        }
    }
    
    public ActionForward getFranchisePositionsAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        LeagueForm leagueForm = (LeagueForm)form;

        int franchiseId = Integer.parseInt(request.getParameter("franchiseId"));
        Franchise franchise = getService().getFranchise(franchiseId);
        League league = new League();
        league.setPositions(new ArrayList(leagueForm.getPositions()));
        
        franchise.populateLeaguePositions(league, 1, 1);        
        leagueForm.setPositions(league.getPositions());
        leagueForm.setFranchiseId(franchiseId);
        leagueForm.setFranchiseName(franchise.getName());
        
        return mapping.findForward(SUCCESS);
    }
        
    public ActionForward setLeaguePositionAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        LeagueForm leagueForm = (LeagueForm)form;
        
        int num = Integer.parseInt(request.getParameter("num"));
        String position = request.getParameter("position");
        
        LeaguePosition pos = leagueForm.getPosition(position);
        pos.setNum(num);
        
        return mapping.findForward(SUCCESS);
    }
    
    
    public ActionForward editLeaguePositionAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        LeagueForm leagueForm = (LeagueForm)form;

        String positionId = request.getParameter("position");
        Franchise franchise = getService().getFranchise(leagueForm.getFranchiseId());
        Position position = franchise.getPosition(positionId);
        
        leagueForm.setPosition(position.getName());
        leagueForm.setPosition(positionId);

        LeaguePosition pos = leagueForm.getPosition(positionId);
        leagueForm.setStats(pos.getStats());        
        
        return mapping.findForward(SUCCESS);
    }
    
    
    public ActionForward saveLeaguePositionStatsAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException {
        LeagueForm leagueForm = (LeagueForm)form;

        LeagueXmlStream stream = new LeagueXmlStream();
        stream.alias("stats", List.class);
        List<LeagueStat> stats = (List)readXml(request, stream);
        String positionId = request.getParameter("position");
        
        leagueForm.getPosition(positionId).setStats(stats);
        
        return mapping.findForward("cancel");
    }

    public ActionForward leagueDraftRunAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
    	
    	int leagueId = Integer.parseInt(request.getParameter("leagueId"));
    	
    	DraftUpdate update = new DraftUpdate(getService());
    	
    	League league = getService().getLeague(leagueId);
    	Franchise franchise = getService().getFranchise(league.getFranchiseId());
    	
    	update.runDraft(franchise, league);
    	
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
    */

}
