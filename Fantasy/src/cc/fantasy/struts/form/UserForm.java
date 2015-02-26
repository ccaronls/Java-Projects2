package cc.fantasy.struts.form;

import java.util.ArrayList;
import java.util.List;

import cc.fantasy.model.User;

public class UserForm extends BaseForm{
    
    private String userName;
    private String passWord;
    private String passWordConfirm;
    private String firstName;
    private String lastName;
    private String email;
    private int numUsers;
    private String access;
    private boolean active = true;
    private int userId = -1;
    private List<User> users = new ArrayList();
    
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getFirstName() {
        return firstName;
    }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    public String getLastName() {
        return lastName;
    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    public int getNumUsers() {
        return numUsers;
    }
    public void setNumUsers(int numUsers) {
        this.numUsers = numUsers;
    }
    public String getPassWord() {
        return passWord;
    }
    public void setPassWord(String passWord) {
        this.passWord = passWord;
    }
    public String getPassWordConfirm() {
        return passWordConfirm;
    }
    public void setPassWordConfirm(String passWordConfirm) {
        this.passWordConfirm = passWordConfirm;
    }
    public String getUserName() {
        return userName;
    }
    public void setUserName(String userName) {
        this.userName = userName;
    }
    public String getAccess() {
        return access;
    }
    public void setAccess(String access) {
        this.access = access;
    }
    public boolean getActive() {
        return active;
    }
    public void setActive(boolean active) {
        this.active = active;
    }
	public int getUserId() {
		return userId;
	}
	public void setUserId(int userId) {
		this.userId = userId;
	}
	public List<User> getUsers() {
		return users;
	}
	public void setUsers(List<User> users) {
		this.users = users;
	}
    public int getNumTeams(User user) {
    	return getService().getTeamsCount(user);
    }
    public int getNumLeagues(User user) {
    	return getService().getLeaguesCount(user);
    }
}
