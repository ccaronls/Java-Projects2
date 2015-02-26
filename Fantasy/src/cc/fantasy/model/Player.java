package cc.fantasy.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @hibernate.class table="FAN_PLAYER_T"
 * @author ccaron
 *
 * <pre>
 * TODO: Class Description
 * </pre>
 */
public class Player extends  ModelBase {

    /**
     * @hibernate.id
     *    generator-class="increment"
     *    column="ID"
     * @hibernate.generator-param
     *    name="sequence"
     *    value="FAN_PLAYER_ID"
     */
    public Integer getHibernateId() {
        return getId() < 0 ? null : getId();
    }
    
    /**
     * @hibernate.property column="FIRST_NAME"
     * @return
     */
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    /**
     * @hibernate.property column="INFO"
     * @return
     */
    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    /**
     * @hibernate.property column="LAST_NAME"
     * @return
     */
    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * @hibernate.property 
     *     column="POSITIONS"
     * @return
     */
    public String getHibernatePositions() {
        String pos = getPositions().toString();
        return pos.substring(1, pos.length()-1);
    }
    
    public void setHibernatePositions(String posStr) {
        positions = Arrays.asList(posStr.split("[, ]+"));
    }
    
    public List<String> getPositions() {
    	if (positions == null)
    		positions = new ArrayList();
        return positions;
    }

    public void setPositions(List<String> positions) {
        this.positions = positions;
    }

    public void addPosition(String position) {
    	if (!getPositions().contains(position))
    		getPositions().add(position);
    }
    
    /**
     * @hibernate.property column="STATUS"
     * @return
     */
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * @hibernate.many-to-one
     *    class="cc.fantasy.model.Franchise"
     *    column="FRANCHISE_ID"
     * @return
     */
    public Franchise getFranchise() {
        return franchise;
    }

    public void setFranchise(Franchise franchise) {
        this.franchise = franchise;
    }
    
    void setParent(Object o) {}
    
    // fields used for equals
    String firstName;
    String lastName;
    
    // fields ommited from equals
    private String info;
    private String status;    
    private List<String> positions;    
    private Franchise franchise;
    
}
