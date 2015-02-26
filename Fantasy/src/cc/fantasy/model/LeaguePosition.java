package cc.fantasy.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @hibernate.class table="FAN_LEAGUE_POSITION_T"
 * 
 * @author ccaron
 *
 * <pre>
 * TODO: Class Description
 * </pre>
 */
public class LeaguePosition extends ModelBase {

    
    /**
     * @hibernate.id
     *      generator-class="increment"
     *      column="ID"
     * @hibernate.generator-param
     *      name="sequence"
     *      value="FAN_LEAGUE_POSITION_ID"
     */
    public Integer getHibernateId() {
        return id < 0 ? null : id;
    }
    
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    /**
     * @hibernate.many-to-one 
     *    class="cc.fantasy.model.League"
     *    column="LEAGUE_ID"
     * @return
     */
    public League getLeague() {
		return league;
	}
    public void setLeague(League league) {
        this.league = league;
    }

    /**
     * @hibernate.property column="POSITION_ID"
     * @return
     */
	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position;
	}
	
    /**
     * @hibernate.property column="NUM"
     * @return
     */
	public int getNum() {
		return num;
	}

	public void setNum(int num) {
		this.num = num;
	}

    /**
     * @hibernate.set
     *      table="FAN_LEAGUEPOSITION_LEAGUESTAT_T"
     *      lazy="false"
     *      cascade="all-delete-orphan"
     *      inverse="true"
     *      
     * @hibernate.collection-key
     *      column="LEAGUEPOSITION_ID"
     *      
     * @hibernate.collection-one-to-many
     *      class="cc.fantasy.model.LeagueStat"
     *      
     * @hibernate.collection-cache
     *      usage="nonstrict-read-write"
     *      
     * @return
     */
	public List<LeagueStat> getStats() {
		if (stats == null)
			stats = new ArrayList();
		return stats;
	}
	
	public void setStats(List<LeagueStat> stats) {
		this.stats = replaceList(getStats(), stats);
	}
	
	public void addStat(LeagueStat stat) {
		addToList(stat, getStats());
	}

	public LeagueStat getStat(String name) {
		Iterator<LeagueStat> it = getStats().iterator();
		while (it.hasNext()) {
			LeagueStat stat = it.next();
			if (stat.getName().equals(name)) {
				return stat;
			}
		}
		return null;
	}

	void setParent(Object o) { league = (League)o; }
	
    // the fields used for equals
    String position;

    // fields ommited from equals
    private int id = -1;
    private int num;    
    private List<LeagueStat> stats; 
    private League league;

}
