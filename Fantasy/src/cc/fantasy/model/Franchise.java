package cc.fantasy.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * 
 * @author ccaron
 *
 * <pre>
 * TODO: Class Description
 * </pre>
 * 
 * @hibernate.class table="FAN_FRANCHISE_T"
 */
public class Franchise extends ModelBase {

    /**
     * @hibernate.id
     *   generator-class="increment"
     *   column="ID"
     * @hibernate.generator-param
     *   name="sequence"
     *   value="FAN_FRANCHISE_ID"
     */
    public Integer getHibernateId() {
        return getId() < 0 ? null : getId();
    }
    
    /**
     * @hibernate.property column="NAME"
     *         
     * @return
     */
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @hibernate.property column="CATEGORY"
     * @return
     */
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * @hibernate.property column="ACTIVE"
     * 
     * @return
     */
	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

    /**
     * @hibernate.list
     *      table="FAN_PLAYER_T"
     *      cascade="all"
     *      
     * @hibernate.collection-key
     *      column="FRANCHISE_ID"
     *      
     * @hibernate.collection-one-to-many
     *      class="cc.fantasy.model.Player"
     *
     * @hibernate.collection-index
     *      column="ID"
     * 
     * @return
     */
    public List<Player> getPlayers() {
    	if (players == null)
    		players = new ArrayList();
        return players;
    }
    
    public void setPlayers(List<Player> players) {
        this.players = replaceList(getPlayers(), players); 
    }
    
    public void addPlayer(Player player) {
        player.setFranchise(this);
        addToList(player, getPlayers());
    }
    
    /**
     * @hibernate.list
     *      name="positions"
     *      table="FAN_POSITION_T"
     *      cascade="all"
     *
     * @hibernate.collection-index
     *      column="ID"
     *      
     * @hibernate.collection-key
     *      column="FRANCHISE_ID"
     *      
     * @hibernate.collection-one-to-many
     *      class="cc.fantasy.model.Position"
     * 
     * @return
     */    
    public List<Position> getPositions() {
    	if (positions == null)
    		positions = new ArrayList();
        return positions;
    }
    
    public void setPositions(List<Position> positions) {
    	this.positions = replaceList(getPositions(), positions);
    }
    
    public void addPosition(Position position) {
    	addToList(position, getPositions());
        position.setStats(position.getStats());
    }
    
    public Player getPlayer(int id) {
    	return players.get(id);
    }
    
    public boolean isTeamPositionComplete(Team team, String position) {
    	Iterator<Player> it = getPlayers().iterator();
    	while (it.hasNext()) {
    		Player player = it.next();
            if (!player.getPositions().contains(position))
                continue;
    		TeamPlayer tPlayer = team.getPlayer(player.getId(), position);
    		if (tPlayer == null || tPlayer.getRank() == 0)
    			return false;
    	}
    	return true;
    }

    public Player getPlayer(String firstName, String lastName) {
    	Iterator<Player> it = getPlayers().iterator();
    	while (it.hasNext()) {
    		Player player = it.next();
    		if (player.getFirstName().equalsIgnoreCase(firstName) && player.getLastName().equalsIgnoreCase(lastName))
    			return player;
    	}
    	return null;
    }
    
    public Position getPosition(String name) {
    	Iterator<Position> it = getPositions().iterator();
    	while (it.hasNext()) {
    		Position pos = it.next();
    		if (pos.getName().equals(name))
    			return pos;
    	}
    	return null;
    }
    
    public void populateLeaguePositions(League league, float initialStatMultiplier, int initialPositionCount) {
    	if (league.getFranchiseId() != getId())
    		league.getPositions().clear();
    	Iterator<Position> it = getPositions().iterator();
    	while (it.hasNext()) {
    		Position p = it.next();
    		LeaguePosition pos = league.getPosition(p.getName());
    		if (pos == null) {
    			pos = new LeaguePosition();
    			pos.setPosition(p.getName());
    			pos.setNum(initialPositionCount);
    			league.addPosition(pos);
    			Iterator<Stat> sit = p.getStats().iterator();
    			while (sit.hasNext()) {
    				Stat stat = sit.next();
    				LeagueStat newStat = new LeagueStat();
    				newStat.setName(stat.getName());
    				newStat.setMultiplier(initialStatMultiplier);
    				pos.addStat(newStat);
    			}
    		}
    	}
    }
    
    public void populateTeamPlayers(League league, Team team) {
    	// add all the players to the team with
    	if (team.getLeagueId() != league.getId())
    		team.getPlayers().clear();
		Iterator<LeaguePosition> pit = league.getPositions().iterator();
		while (pit.hasNext()) {
			LeaguePosition pos = pit.next();
            if (pos.getNum() <= 0)
                continue;
            ArrayList<TeamPlayer> assigned = new ArrayList();
	    	Iterator<Player> it = getPlayers().iterator();
	    	while (it.hasNext()) {
	    		Player player = it.next();
	    		if (player.getPositions().contains(pos.getPosition())) {
	    			TeamPlayer tp = team.getPlayer(player.getId(), pos.getPosition());
	    			if (tp == null) {
	    				tp = new TeamPlayer();
	    				tp.setPlayerId(player.getId());
	    				tp.setPosition(pos.getPosition());
	    				team.addPlayer(tp);
	    			} 
                    assigned.add(tp);
	    		}
	    	}

            int rank = 1;
            Iterator<TeamPlayer> tit = assigned.iterator();
            while (tit.hasNext())
                tit.next().setRank(rank++);
		}
    }
    
    public List<Player> getAvailablePlayers(List<Team> teams, String position) {
        HashSet<Integer> used = new HashSet(); 
        Iterator<Team> tit = teams.iterator();
        while (tit.hasNext()) {
            Iterator<TeamPlayer> pit = tit.next().getPlayersForPosition(position).iterator();
            while (pit.hasNext()) {
                used.add(pit.next().getPlayerId());
            }
        }
        ArrayList<Player> availablePlayers = new ArrayList();
        Iterator<Player> pit = getPlayers().iterator();
        while (pit.hasNext()) {
            Player player = pit.next();
            if (used.contains(player.getId()))
                continue;
            if (!player.getPositions().contains(position))
                continue;
            availablePlayers.add(player);
        }        
        return availablePlayers;
    }

    void setParent(Object o) {}
    
    // fields used for equals
    String name;
    String category;
    boolean active;
    
    // fields omitted from equals
    private List<Position> positions;
    private List<Player> players;

}
