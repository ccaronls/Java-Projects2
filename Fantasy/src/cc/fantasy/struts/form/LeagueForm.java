package cc.fantasy.struts.form;

import java.util.Iterator;
import java.util.List;

import cc.fantasy.model.Franchise;
import cc.fantasy.model.LeaguePosition;
import cc.fantasy.model.LeagueStat;
import cc.fantasy.model.Team;
import cc.fantasy.struts.util.Common;

public class LeagueForm extends BaseForm{

    // league search
    private List leagues;
    
    // league edit
    private int leagueId = -1;
    private int userId = -1;
    private String name;
    private int    franchiseId = -1;
    private String draftDate;
    private String endDate;
    private int maxPlayers;
    private List<Franchise> franchises;
    private String franchiseName;
    private List positions;
    private int positionId = -1;
    
    // league position edit
    private String position;
    private List<LeagueStat> stats;
    
    public int getLeagueId() {
        return leagueId;
    }

    public void setLeagueId(int leagueId) {
        this.leagueId = leagueId;
    }
    
    public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public List getLeagues() {
        return leagues;
    }

    public void setLeagues(List leagues) {
        this.leagues = leagues;
    }

    public String getDraftDate() {
        return draftDate;
    }

    public void setDraftDate(String draftDate) {
        this.draftDate = draftDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public int getFranchiseId() {
        return franchiseId;
    }

    public void setFranchiseId(int franchiseId) {
        this.franchiseId = franchiseId;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFranchiseName() {
        return franchiseName;
    }

    public void setFranchiseName(String franchiseName) {
        this.franchiseName = franchiseName;
    }

    public List getFranchises() {
        return franchises;
    }

    public void setFranchises(List franchises) {
        this.franchises = franchises;
    }

    public List<LeaguePosition> getPositions() {
        return positions;
    }

    public void setPositions(List<LeaguePosition> positions) {
        this.positions = positions;
    }
    
    public String getPositionName(String position) {
        return getService().getFranchise(franchiseId).getPosition(position).getLongName();
    }

    public String getStatsString(LeaguePosition position) {
        return Common.makeLeagueStatsString(position.getStats());
    }
    
    public int getPositionId() {
		return positionId;
	}

	public void setPositionId(int positionId) {
		this.positionId = positionId;
	}

	public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public List<LeagueStat> getStats() {
        return stats;
    }

    public void setStats(List<LeagueStat> stats) {
        this.stats = stats;
    }
    
    public LeaguePosition getPosition(String position) {
        Iterator<LeaguePosition> it = getPositions().iterator();
        while (it.hasNext()) {
            LeaguePosition pos = it.next();
            if (pos.getPosition().equals(position)) {
                return pos;
            }
        }
        return null;
    }

    public List<Team> getTeams(int leagueId) {
    	return getService().getTeamsByLeague(leagueId, null);
    }
}
