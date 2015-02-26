package cc.fantasy.model;

import java.util.*;

import cc.fantasy.model.type.LeagueStatus;

/**
 * 
 * @author ccaron
 *
 * <pre>
 * TODO: Class Description
 * </pre>
 * 
 * @hibernate.class table="FAN_LEAGUE_T"
 */
public class League extends ModelBase {

    /**
     * @hibernate.id
     *   generator-class="increment"
     *   column="ID"
     * @hibernate.generator-param
     *   name="sequence"
     *   value="FAN_LEAGUE_ID"
     */
    public Integer getHibernateId() {
        return getId() < 0 ? null : getId();
    }
    
    /**
     * @hibernate.property column="MAX_PLAYERS"
     * @return
     */
    public int getMaxPlayers() {
        return maxPlayers;
    }
    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }
    /**
     * @hibernate.property column="CREATED"
     * @return
     */
    public Date getCreated() {
        return created;
    }
    public void setCreated(Date created) {
        this.created = created;
    }
    /**
     * @hibernate.property column="DRAFT"
     * @return
     */
    public Date getDraft() {
        return draft;
    }
    public void setDraft(Date draft) {
        this.draft = draft;
    }
    /**
     * @hibernate.property column="ENDING"
     * @return
     */
    public Date getEnding() {
        return ending;
    }
    public void setEnding(Date ending) {
        this.ending = ending;
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
     * @hibernate.property column="UPDATED"
     * @return
     */
    public Date getUpdated() {
        return updated;
    }
    public void setUpdated(Date updated) {
        this.updated = updated;
    }
    /**
     * @hibernate.many-to-one
     *    class="cc.fantasy.model.User"
     *    column="USER_ID"
     * @return
     */
    public int getUserId() {
        return userId;
    }
    public void setUserId(int userId) {
        this.userId = userId;
    }
    /**
     * @hibernate.many-to-one
     *    class="cc.fantasy.model.Franchise"
     *    column="FRANCHISE_ID"
     * @return
     */
    public int getFranchiseId() {
		return franchiseId;
	}
	public void setFranchiseId(int franchiseId) {
		this.franchiseId = franchiseId;
	}

    /**
     * @hibernate.set
     *    lazy="false"
     *    cascade="all-delete-orphan"
     *    inverse="true"
     *    
     * @hibernate.collection-key
     *    column="LEAGUE_ID"
     *    
     * @hibernate.collection-one-to-many
     *    class="cc.fantasy.model.Position"
     *    
     * @return
     */
    public List<LeaguePosition> getPositions() {
        if (positions == null)
            positions = new ArrayList();
        return positions;
    }
    
    public void setPositions(List<LeaguePosition> positions) {
    	this.positions = replaceList(getPositions(), positions);
    }
    
    public void addPosition(LeaguePosition position) {
    	addToList(position, getPositions());
		position.setStats(position.getStats());
    }
    
    public LeaguePosition getPosition(String position) {
    	Iterator<LeaguePosition> it = getPositions().iterator();
    	while (it.hasNext()) {
    		LeaguePosition p = it.next();
    		if (p.getPosition().equals(position))
    			return p;
    	}
    	return null;
    }

    public LeagueStatus getStatus() {
        Date now = new Date();
        if (now.compareTo(getDraft()) < 0) {
            return LeagueStatus.OPEN;
        } else if (now.compareTo(getEnding()) < 0) {
            return LeagueStatus.CLOSED; 
        }
        return LeagueStatus.DONE;
    }

    public Date getStatusDate() {
        LeagueStatus status = getStatus();
        switch (status) {
        case OPEN: return getDraft();
        case CLOSED: return getEnding();
        case DONE: return new Date();
        }
        throw new RuntimeException("Unhandled case [" + status + "]");
    }
    
    void setParent(Object o) {}
    
    // fields used for equals() (things that cant change)
    String name;
    int userId = -1;
    int franchiseId = -1;

    // fields ommited from equals() (things that can change)
    private List<LeaguePosition> positions;
    private int maxPlayers = 0;
    private Date created;
    private Date draft;
    private Date updated;
    private Date ending;

}
