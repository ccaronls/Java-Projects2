package cc.game.soc.model;

public class User extends AModel {

	String userName;
	String passWord;
	String firstName;
	String lastName;
	String email;
	int rank;
	UserAccess access;

	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getPassWord() {
		return passWord;
	}
	public void setPassWord(String passWord) {
		this.passWord = passWord;
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
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public int getRank() {
		return rank;
	}
	public void setRank(int rank) {
		this.rank = rank;
	}
	public UserAccess getAccess() {
		return access;
	}
	public void setAccess(UserAccess access) {
		this.access = access;
	}
	public String toString() {
		return String.format("%-5d %2d %-10s %-10s %-10s %4s %1s", getId(), rank, userName, firstName, lastName, access, email);		
	}
	
}
