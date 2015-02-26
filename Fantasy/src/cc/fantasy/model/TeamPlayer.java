package cc.fantasy.model;

/**
 * @hibernate.class table="FAN_TEAM_PLAYER_T"
 * @author ccaron
 *
 * <pre>
 * TODO: Class Description
 * </pre>
 */
public class TeamPlayer extends ModelBase {

    /**
     * @hibernate.id
     *      generator-class="increment"
     *      column="ID"
     * @hibernate.generator-param
     *      name="sequence"
     *      value="FAN_TEAM_PLAYER_ID"
     */
    public Integer getHibernateId() {
        return getId() < 0 ? null : getId();
    }
    
    /**
     * @hibernate.property column="PLAYER_ID"
     * @return
     */
    public int getPlayerId() {
        return playerId;
    }

    public void setPlayerId(int playerId) {
        this.playerId = playerId;
    }

    /**
     * @hibernate.property column="RANK"
     * @return
     */
    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    /**
     * @hibernate.many-to-one
     *    class="cc.fantasy.model.Team"
     *    column="TEAM_ID"
     * @return
     */
    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    /**
     * @hibernate.property column="POSITION"
     * @return
     */
    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }
    
    void setParent(Object o) {
    	team = (Team)o;
    }
    
    // fields used for equals
    int playerId = -1;
    String position;
    
    // fields ommited from equals
    private int rank = 0;    
    private Team team;

    
}
