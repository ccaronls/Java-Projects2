package cc.fantasy.struts.form;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cc.fantasy.model.League;
import cc.fantasy.model.LeaguePosition;
import cc.fantasy.model.Player;
import cc.fantasy.model.Team;
import cc.fantasy.model.TeamPlayer;
import cc.fantasy.struts.util.Common;

public class TeamForm extends BaseForm{

	// team list
    private List<Team> teams = new ArrayList();

	// team edit
    private List<League> leagues = new ArrayList();
    private int teamId = -1;
    private String name;
    private int leagueId = -1;
    private int franchiseId = -1; 
    private int points = 0;
    private List<LeaguePosition> positions = new ArrayList();
    private String leagueName;
    private List<TeamPlayer> players = new ArrayList();
	
    // team rank
	private String positionToRank;
	private List<TeamPlayer> playersToRank = new ArrayList();    
    
	public List<Team> getTeams() {
        return teams;
    }
    public void setTeams(List<Team> teams) {
        this.teams = teams;
    }
    public int getLeagueId() {
		return leagueId;
	}
	public void setLeagueId(int leagueId) {
		this.leagueId = leagueId;
	}
	public List<League> getLeagues() {
		return leagues;
	}
	public void setLeagues(List<League> leagues) {
		this.leagues = leagues;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public List<TeamPlayer> getPlayers() {
		return players;
	}
	public void setPlayers(List<TeamPlayer> players) {
		this.players = players;
	}
	public String getPositionToRank() {
		return positionToRank;
	}
	public void setPositionToRank(String positionToRank) {
		this.positionToRank = positionToRank;
	}
	public String getLeagueName() {
		return leagueName;
	}
	public void setLeagueName(String leagueName) {
		this.leagueName = leagueName;
	}
	public int getTeamId() {
		return teamId;
	}
	public void setTeamId(int teamId) {
		this.teamId = teamId;
	}
    public String getPositionName(String position) {
    	return getService().getFranchise(franchiseId).getPosition(position).getLongName();
    }
    public int getFranchiseId() {
		return franchiseId;
	}
	public void setFranchiseId(int franchiseId) {
		this.franchiseId = franchiseId;
	}
    public int getPoints() {
        return points;
    }
    public void setPoints(int points) {
        this.points = points;
    }
    public List getPositions() {
		return positions;
	}
	public void setPositions(List<LeaguePosition> positions) {
		this.positions = positions;
	}
	public List<TeamPlayer> getPlayersToRank() {
		return playersToRank;
	}
	public void setPlayersToRank(List<TeamPlayer> playersToRank) {
		this.playersToRank = playersToRank;
	}
	public String getTopChoicePlayer(String position) {
    	Iterator<TeamPlayer> it = players.iterator();
    	while (it.hasNext()) {
    		TeamPlayer player = it.next();
    		if (player.getPosition().equals(position) && player.getRank() == 1) {
    			Player fPlayer = getService().getFranchise(franchiseId).getPlayer(player.getPlayerId());
    			return Common.makePlayerNameString(fPlayer);
    		}
    	}
    	return null;
    }
    public String getPlayerName(int playerId) {
    	Player player = getService().getFranchise(franchiseId).getPlayer(playerId);
    	return player.getFirstName() + " " + player.getLastName();
    }
    public League getLeague(int leagueId) {
    	return getService().getLeague(leagueId);
    }
    public int getTeamRank(Team team) {
    	return getService().getTeamRank(team);
    }
    public String getLeagueLeader(int leagueId) {
    	return Team.getName(getService().getLeagueLeader(leagueId));
    }
}
