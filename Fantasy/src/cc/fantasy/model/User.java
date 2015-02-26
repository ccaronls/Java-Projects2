package cc.fantasy.model;

import java.util.Date;

import cc.fantasy.model.type.UserAccess;

/**
 * @hibernate.class table="FAN_USER_T"
 * @author ccaron
 *
 * <pre>
 * TODO: Class Description
 * </pre>
 */
public class User extends ModelBase {

    /**
     * @hibernate.id
     *      generator-class="increment"
     *      column="ID"
     * @hibernate.generator-param
     *      name="sequence"
     *      value="FAN_USER_ID"
     */
    public Integer getHibernateId() {
        return getId() < 0 ? null : getId();
    }
	
	/**
	 * @hibernate.property 
	 *    column="ACCESS"
     *    type="cc.fantasy.hibernate.UserAccessUserType"
     *    
	 * @return
	 */
    public UserAccess getAccess() {
        return access;
    }
    public void setAccess(UserAccess access) {
        this.access = access;
    }
    /**
     * @hibernate.property column="EMAIL"
     * @return
     */
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
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
     * @hibernate.property column="PASSWORD"
     * @return
     */
    public String getPassWord() {
        return passWord;
    }
    public void setPassWord(String passWord) {
        this.passWord = passWord;
    }
    /**
     * @hibernate.property column="USER_NAME"
     * @return
     */
    public String getUserName() {
        return userName;
    }
    public void setUserName(String userName) {
        this.userName = userName;
    }
    /**
     * @hibernate.property column="ACTIVE"
     * @return
     */
    public boolean isActive() {
        return active;
    }
    public void setActive(boolean active) {
        this.active = active;
    }
    /**
     * @hibernate.property 
     *    column="LAST_LOGIN"
     *    class="java.sql.Date"
     * @return
     */
    public Date getLastLogin() {
        return lastLogin;
    }
    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
    }
    public boolean hasTeamAccess() {
        return access.ordinal() >= UserAccess.TEAM.ordinal();
    }
    public boolean hasLeagueAccess() {
        return access.ordinal() >= UserAccess.LEAGUE.ordinal();
    }
    public boolean hasAdminAccess() {
        return access.ordinal() >= UserAccess.ADMIN.ordinal();
    }

    void setParent(Object o) {}
    
    // fields used for equals
    String userName;
    
    // fields ommited from equals
    private String firstName = "";
    private String lastName = "";
    private String email = "";
    private String passWord;
    private UserAccess access=UserAccess.NONE;
    private boolean active=false;
    private Date lastLogin;

}
