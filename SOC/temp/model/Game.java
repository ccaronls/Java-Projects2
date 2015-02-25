package cc.game.soc.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Game extends AModel {

	int maxPlayers = 0;
	int minRank;
	String archive;
	String boardType = "default"; // can also be custom
	GameState state = GameState.JOINING;
	Date created;
	Date completed;
	User user; // user who created this game
	int numJoined;
	List<Integer> joined = new ArrayList<Integer>();
	
	public String toString() {
		return String.format("%-5d %2d %-5d %-5d %-10s %-10s %-20s %-20s", getId(), user == null ? null : user.getId(), minRank, maxPlayers, boardType, state, created, completed);
	}
	public User getUser() {
		return user;
	}
	public void setUser(User user) {
		this.user = user;
	}
	public int getMaxPlayers() {
		return maxPlayers;
	}
	public void setMaxPlayers(int maxPlayers) {
		this.maxPlayers = maxPlayers;
	}
	public String getArchive() {
		return archive;
	}
	public void setArchive(String archive) {
		this.archive = archive;
	}
	public List<Integer> getJoined() {
		return joined;
	}
	public void setJoined(List<Integer> joined) {
		this.joined = joined;
	}
	public void setNumJoined(int numJoined) {
		this.numJoined = numJoined;
	}
	public String getBoardType() {
		return boardType;
	}
	public void setBoardType(String boardType) {
		this.boardType = boardType;
	}
	public GameState getState() {
		return state;
	}
	public void setState(GameState state) {
		this.state = state;
	}
	public Date getCreated() {
		return created;
	}
	public void setCreated(Date created) {
		this.created = created;
	}
	public Date getCompleted() {
		return completed;
	}
	public void setCompleted(Date completed) {
		this.completed = completed;
	}	
	public int getMinRank() {
		return minRank;
	}
	public void setMinRank(int minRank) {
		this.minRank = minRank;
	}
	//public List<Integer> getJoined() {
	//	return joined;
	//}
	//public void setJoined(List<Integer> joined) {
	//	this.joined = joined;
	//}
	public void addJoinedUser(User user) {
		joined.add(user.getId());
		numJoined ++;
	}
	public int getNumJoined() {
		return numJoined;
	}
}
