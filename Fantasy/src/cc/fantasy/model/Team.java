package cc.fantasy.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @hibernate.class table="FAN_TEAM_T"
 * @author ccaron
 *
 * <pre>
 * TODO: Class Description
 * </pre>
 */
public class Team extends ModelBase {

    
    /**
     * @hibernate.id
     *      generator-class="increment"
     *      column="ID"
     * @hibernate.generator-param
     *      name="sequence"
     *      value="FAN_TEAM_ID"
     */
    public Integer getHibernateId() {
        return getId() < 0 ? null : getId();
    }
    
    /**
     * @hibernate.property column="NAME"
     * @return
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * @hibernate.set
     *      lazy="false"
     *      cascade="all-delete-orphan"
     *      inverse="true"
     *      
     * @hibernate.collection-key
     *      column="PLAYER_ID"
     *      
     * @hibernate.collection-one-to-many
     *      class="cc.fantasy.model.TeamPlayer"
     * 
     * @hibernate.collection-cache
     *      usage="nonstrict-read-write"
     * 
     * @return
     */
    public List<TeamPlayer> getPlayers() {
    	if (players == null)
    		players = new ArrayList();
        return players;
    }

    public void setPlayers(List<TeamPlayer> players) {
    	this.players = replaceList(getPlayers(), players);
    }
    
    public void addPlayer(TeamPlayer player) {
    	addToList(player, getPlayers());
    }

    /**
     * @hibernate.property column="USER_ID"
     * @return
     */
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    /**
     * @hibernate.property column="POINTS"
     * @return
     */
	public int getPoints() {
		return points;
	}

	public void setPoints(int points) {
		this.points = points;
	}
    
    /**
     * @hibernate.property column="LEAGUE_ID"
     * @return
     */    
    public int getLeagueId() {
        return leagueId;
    }

    public void setLeagueId(int leagueId) {
        this.leagueId = leagueId;
    }

    public TeamPlayer getTopChoicePlayer(String position) {
        Iterator<TeamPlayer> it = getPlayers().iterator();
        while (it.hasNext()) {
            TeamPlayer player = it.next();
            if (player.getPosition().equals(position) && player.getRank() == 1) {
                return player;
            }
        }
        return null;
    }
    
    public TeamPlayer getPlayer(int playerId, String position) {
        Iterator<TeamPlayer> it = getPlayers().iterator();
        while (it.hasNext()) {
        	TeamPlayer player = it.next();
            if (player.getPosition().equals(position) && player.getPlayerId() == playerId) {
                return player;
            }
        }
        return null;
    }
    
    public List<TeamPlayer> getPlayersForPosition(String position) {
        List<TeamPlayer> players = new ArrayList();
        Iterator<TeamPlayer> it = getPlayers().iterator();
        while (it.hasNext()) {
            TeamPlayer player = it.next();
            if (player.getPosition().equals(position)) {
                players.add(player);
            }
        }
        return players;        
    }
    
    public static String getName(Team t) {
        return t == null ? "--" : t.getName();
    }

    void setParent(Object o) {}
    
    // fields used for equals
    String name;
    int userId = -1;
    int leagueId = -1;

    // fields to omit from equals()
    private int points;
    private List<TeamPlayer> players;
    


}
