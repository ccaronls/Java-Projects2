package cc.fantasy.model;

/**
 * @hibernate.class table="FAN_STAT_T"
 * 
 * @author ccaron
 *
 * <pre>
 * TODO: Class Description
 * </pre>
 */
public class Stat extends ModelBase {

    public Stat() {}
    
    public Stat(String name) {
    	this.name = name;
    }
   
    /**
     * @hibernate.id
     *      generator-class="increment"
     *      column="ID"
     * @hibernate.generator-param
     *      name="sequence"
     *      value="FAN_STAT_ID"
     */
    public Integer getHibernateId() {
        return getId() < 0 ? null : getId();
    }
    
    /**
     * @hibernate.property column="DESCRIPTION"
     * @return
     */
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
     * @hibernate.many-to-one
     *    class="cc.fantasy.model.Position"
     *    column="POSITION_ID"
     * @return
     */
    public Position getPosition() {
        return position;
    }
    public void setPosition(Position position) {
        this.position = position;
    }

    void setParent(Object o) {
    	position = (Position)o;
    }

    // fields used for equals
    String name = "";

    // fields ommited from equals
    private String description = "";    
    private Position position;

}
