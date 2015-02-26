package cc.fantasy.swing;

import cc.fantasy.service.FranchiseSearchKey;
import cc.fantasy.service.LeagueSearchKey;
import cc.fantasy.service.Search;
import cc.fantasy.service.TeamSearchKey;
import cc.fantasy.service.UserSearchKey;

public class FSortButton extends FButton {

	FSortButton(String label, Command cmd, Search search) {
		super(label, cmd);
		this.setFont(FLabel.getHeader1Font(getFont()));
		this.setForeground(FLabel.H1_TXT_COLOR);
		this.setBackground(FLabel.H1_BK_COLOR);
		this.setBorderPainted(false);
		set(search);
	}
	
    public FSortButton setFranchiseKey(FranchiseSearchKey franchiseKey) {
        return (FSortButton)set(franchiseKey);
    }

    public FSortButton setLeagueKey(LeagueSearchKey leagueKey) {
        return (FSortButton)set(leagueKey);
    }

    public FSortButton setTeamKey(TeamSearchKey teamKey) {
        return (FSortButton)set(teamKey);
    }

    public FSortButton setUserKey(UserSearchKey userKey) {
        return (FSortButton)set(userKey);
    }
	
    
}
