package cc.fantasy.model;

/**
 * @hibernate.class table="FAN_LEAGUE_STAT_T"
 * @author ccaron
 *
 * <pre>
 * TODO: Class Description
 * </pre>
 */
public class LeagueStat extends ModelBase {

    /**
     * @hibernate.id
     *      generator-class="increment"
     *      column="ID"
     * @hibernate.generator-param
     *      name="sequence"
     *      value="FAN_LEAGUE_STAT_ID"
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
     *    class="cc.fantasy.model.LeaguePosition"
     *    column="LEAGUE_POSITION_ID"
     * @return
     */
    public LeaguePosition getPosition() {
		return position;
	}
	
    public void setPosition(LeaguePosition position) {
        this.position = position;
    }

    /**
     * @hibernate.property column="MULTIPLIER"
     * @return
     */
	public float getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(float multiplier) {
        this.multiplier = multiplier;
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
    
    void setParent(Object o) { position = (LeaguePosition)o; }
    
    // fields used for equals
    String name; 

    // fields ommited from equals
    private int id = -1;
    private float multiplier = 0;    
    private LeaguePosition position;
    
}
