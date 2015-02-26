package cc.fantasy.swing;


import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.apache.log4j.Category;

import cc.fantasy.exception.ErrorCode;
import cc.fantasy.exception.FantasyException;
import cc.fantasy.model.*;
import cc.fantasy.model.type.LeagueStatus;
import cc.fantasy.model.type.UserAccess;
import cc.fantasy.service.FranchiseSearchKey;
import cc.fantasy.service.IFantasyService;
import cc.fantasy.service.LeagueSearchKey;
import cc.fantasy.service.Search;
import cc.fantasy.service.TeamSearchKey;
import cc.fantasy.service.UserSearchKey;
import cc.fantasy.update.DraftUpdate;
import cc.fantasy.update.FranchiseUpdate;
import cc.fantasy.util.CommandLineParser;
import cc.fantasy.xml.FantasyServiceXmlDB;
import cc.fantasy.xml.FantasyServiceXmlDBEncrypted;

/**
 * 
 * @author ccaron
 *
 * <pre>
 * Swing based Front End
 * </pre>
 */
public class Fantasy implements ActionListener, ComponentListener  {

    static Category log = Category.getInstance(Fantasy.class);    
    static Fantasy instance = null;

    FFrame currentFrame = null;
    FFrame popupFrame = null;
    File fileChooserPath = null;
    IFantasyService service;
    User user;
    boolean encrypted = false;
    Config config;

    public static void main(String [] args) {
        
        try {
        	
        	CommandLineParser parser = new CommandLineParser(
        			"h\"this help\"" +
        			"e\"enable data encryption (default: disable)\"" + 
        			"d$s\"db dir (default: ./db)\""
        			);
        	parser.parse(args, 
        			"S\"username\"" + 
        			"S\"password\""
        			);
        	
        	if (parser.getParamSpecified('h')) {
        		System.out.println(parser.getUsage("Fantasy"));
        		System.exit(0);
        	}        

        	if (parser.getNumArgs() != 0 && parser.getNumArgs() != 2) {
        		System.err.println(parser.getUsage("Fantasy"));
        		System.err.println("Either both UserName and Password are required or neither");
        		System.exit(1);
        	}

        	String baseDir = null;
        	if (parser.getParamSpecified('d')) {
        		baseDir = parser.getParamValue('d');
        	} else {
        		baseDir = "./db";
        	}

        	boolean encrypt = false;
        	if (parser.getParamSpecified('e')) {
                log.info("Encryption enabled");
        		encrypt = true;
            }
        	
        	if (args.length == 2) {
                new Fantasy(encrypt, baseDir, parser.getArg(0), parser.getArg(1));
            } else {            
                new Fantasy(encrypt, baseDir);
            }
        } catch (NullPointerException e) {
        	e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        	log.error(e.getMessage());
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
    
    boolean init(boolean encrypt, String baseDir) {
    	if (instance == null) {
    		encrypted = encrypt;
    		instance = this;
    		if (encrypted)
    			service = new FantasyServiceXmlDBEncrypted(baseDir + File.separator + "bin");
    		else
    			service = new FantasyServiceXmlDB(baseDir + File.separator + "xml");
        	config = new Config();
        	config.load(new File("fantasy.cfg"));
            return true;
    	}
    	return false;
    }
    
    
    Fantasy(boolean encrypt, String baseDir, String userName, String passWord) {
    	if (init(encrypt, baseDir)) {
            try {
                User user = service.getUser(userName, passWord); 
                login(user);
            } catch (FantasyException e) {
                log.error("Login Failed for [" + userName + "], error : " + e.getMessage());
                openLogin();
            }
    	}
    }
    
    Fantasy(boolean encrypt, String baseDir) {
    	if (init(encrypt, baseDir)) {
    		String userName = config.getString("user.name", null);
    		String passWord = config.getString("user.pw", null);
    		if (userName != null && passWord != null) {
                try {
                    User user = service.getUser(userName, passWord); 
                    login(user);
                    currentFrame.finalizeFrame();
                } catch (FantasyException e) {
                    log.error("Login Failed for [" + userName + "], error : " + e.getMessage());
                    openLogin();
                }
    		} else {
    			openLogin();
    		}
    	}
    }
    
    void openLogin() {
        if (service.getUserCount() == 0) {
            pushEditUser(null);
        } else {
            // login screen
            pushLogin();
        }
    }
    
    void login(User user) {
        if (!user.isActive())
            throw new FantasyException(ErrorCode.USER_ACCOUNT_NOT_ACTIVE, user.getUserName());
        user.setLastLogin(new Date());
        service.addOrUpdateUser(user);
    	this.user = user;
        switch (user.getAccess()) {
        case ADMIN: showFranchiseManagement(null); break;
        case TEAM: showTeamManagement(user, null); break;
        case LEAGUE: showLeagueManagement(user, null); break;
        default: 
            unhandledCase(user.getAccess());                            
        }

    }
    
    void pushLogin() {
    	final FFrame frame = newPopupFrame("LOGIN");
        FGridPanel panel = new FGridPanel(2);
        frame.add(panel);
        final FTextField userName = new FTextField();
        final JTextField passWord = new JPasswordField();
        panel.add(new FLabel("UserName"));
        panel.add(userName);
        panel.add(new FLabel("Password"));
        panel.add(passWord);
        frame.addFooter(new FButton(Command.LOGIN).setArray(new JTextField[]{userName, passWord}));
        frame.addFooter(new FButton(Command.NEW_USER));
        frame.finalizeFrame();
    }
    
    void showUsers(Search<UserSearchKey> search) {
        FFrame frame = getMainFrame();
        frame.addHeader("USER MANAGEMENT");
        FGridPanel panel = new FGridPanel(7);
        frame.add(panel);
        
        panel.add(new FSortButton("Name", Command.SORT_USER_LIST, search).setUserKey(UserSearchKey.LASTNAME));
        panel.add(new FSortButton("User Name", Command.SORT_USER_LIST, search).setUserKey(UserSearchKey.USERNAME));
        panel.add(new FSortButton("Access", Command.SORT_USER_LIST, search).setUserKey(UserSearchKey.ACCESS));
        panel.add(new FSortButton("Teams", Command.SORT_USER_LIST, search).setUserKey(UserSearchKey.NUM_TEAMS));
        panel.add(new FSortButton("Leagues", Command.SORT_USER_LIST, search).setUserKey(UserSearchKey.NUM_LEAGUES));
        panel.add(new FSortButton("Last Login", Command.SORT_USER_LIST, search).setUserKey(UserSearchKey.LASTLOGIN));
        panel.add(new FSortButton("Active", Command.SORT_USER_LIST, search).setUserKey(UserSearchKey.ACTIVE));
                
        Iterator<User> it = service.getUsers(search).iterator();
        while (it.hasNext()) {
            User user = it.next();
            panel.add(user.getFirstName() + " " + user.getLastName());
            panel.add(user.getUserName());
            panel.add(user.getAccess().name());
            panel.add(service.getTeamsCount(user));
            panel.add(service.getLeaguesCount(user));
            panel.add(user.getLastLogin());
            panel.add(new FToggleButton(user.isActive() ? "YES" : "NO", Command.TOGGLE_USER_ACTIVE, user.isActive()).setUser(user));
        }
        
    }

    void showLeagueManagement(User user, Search<LeagueSearchKey> search) {
    	final FFrame frame = getMainFrame();
        frame.addHeader("League Management");

        // create the league matrix
        List<League> leagues = service.getLeagues(user, search);
        FGridPanel panel = new FGridPanel(6); 
    	frame.add(panel);
    	panel.add(new FSortButton("League", Command.SORT_LEAGUE_LIST, search).setLeagueKey(LeagueSearchKey.NAME).setUser(user));
        panel.add(new FSortButton("Franchise", Command.SORT_LEAGUE_LIST, search).setLeagueKey(LeagueSearchKey.FRANCHISE_NAME).setUser(user));
        panel.add(new FSortButton("Teams", Command.SORT_LEAGUE_LIST, search).setLeagueKey(LeagueSearchKey.NUM_TEAMS).setUser(user));
        panel.add(new FSortButton("Status", Command.SORT_LEAGUE_LIST, search).setLeagueKey(LeagueSearchKey.STATUS).setUser(user));
        panel.add(new FSortButton("Leader", Command.SORT_LEAGUE_LIST, search).setLeagueKey(LeagueSearchKey.LEADER_NAME).setUser(user));
    	panel.addHeader("Action");
    	
    	Iterator<League> lit = leagues.iterator();
    	while (lit.hasNext()) {
    		League league = lit.next();
    		panel.add(new FLabel(league.getName()));
    		Franchise franchise = service.getFranchise(league.getFranchiseId());
    		panel.add(new FLabel(franchise.getName()));
    		panel.add(new FLabel(service.getTeamsCount(league)));
    		//Date now = new Date();
    		JPanel buttons = new JPanel();
    		buttons.setLayout(new FlowLayout());
    		buttons.add(new FButton(Command.EDIT_LEAGUE).setLeague(league));
            LeagueStatus status = league.getStatus();
            switch (status) {
                case OPEN:
                    panel.add(new FLabel("OPEN"));
                    panel.add(new FLabel("--"));
                    if (service.getTeamsCount(league)>1)
                        buttons.add(new FButton(Command.RUN_LEAGUE_DRAFT).setLeague(league).setFranchise(franchise));
                    break;
                case CLOSED: {
                    panel.add(new FLabel("CLOSED"));
                    panel.add(new FLabel(Team.getName(service.getLeagueLeader(league.getId()))));
                    break;
                }
                case DONE: {
                    panel.add(new FLabel("DONE"));
                    panel.add(new FLabel(Team.getName(service.getLeagueLeader(league.getId()))));
                    break;
                }
            }
            
			panel.add(buttons);
    	}
        
        // create the back, cancel, ... buttons on the bottom
        frame.addFooter(new FButton(Command.NEW_LEAGUE));
        
    }
    
    void showTeams(Search<TeamSearchKey> search) {
        FFrame frame = getMainFrame();
        frame.addHeader("Team Management");
        FGridPanel panel = new FGridPanel(10);
        frame.add(panel);
        panel.add(new FSortButton("Team", Command.SORT_TEAM_LIST, search).setTeamKey(TeamSearchKey.NAME));
        panel.add(new FSortButton("Owner", Command.SORT_TEAM_LIST, search).setTeamKey(TeamSearchKey.OWNER_NAME));
        panel.add(new FSortButton("League", Command.SORT_TEAM_LIST, search).setTeamKey(TeamSearchKey.LEAGUE_NAME));
        panel.add(new FSortButton("Franchise", Command.SORT_TEAM_LIST, search).setTeamKey(TeamSearchKey.FRANCHISE_NAME));
        panel.add(new FSortButton("Points", Command.SORT_TEAM_LIST, search).setTeamKey(TeamSearchKey.POINTS));
        panel.add(new FSortButton("Rank", Command.SORT_TEAM_LIST, search).setTeamKey(TeamSearchKey.RANK));
        panel.add(new FSortButton("Leader", Command.SORT_TEAM_LIST, search).setTeamKey(TeamSearchKey.LEADER));
        panel.add(new FSortButton("Status", Command.SORT_TEAM_LIST, search).setTeamKey(TeamSearchKey.LEAGUE_STATUS));
        panel.add(new FSortButton("Date", Command.SORT_TEAM_LIST, search).setTeamKey(TeamSearchKey.LEAGUE_STATUS_DATE));
        panel.addHeader("Action");
        List<Team> teams = service.getTeams(user, search);
        Iterator<Team> it = teams.iterator();
        while (it.hasNext()) {
        	Team t = it.next();
        	panel.add(new FLabel(t.getName()));
            User u = service.getUser(t.getUserId());
            panel.add(new FLabel(u.getUserName()));
            League league = service.getLeague(t.getLeagueId());
        	panel.add(new FLabel(league.getName()));
            panel.add(new FLabel(service.getFranchise(league.getFranchiseId()).getName()));
        	panel.add(new FLabel(t.getPoints()));
        	panel.add(new FLabel(service.getTeamRank(t)));
            Team leader = service.getLeagueLeader(league.getId());
        	panel.add(new FLabel(leader == null ? "--" : leader.getName()));
        	LeagueStatus status = league.getStatus();
        	panel.add(new FLabel(status.name()));
        	JPanel buttons = new JPanel();
        	buttons.setLayout(new FlowLayout());
        	switch (status) {
        	case OPEN: 
        		panel.add(new FLabel(league.getDraft()));
        		buttons.add(new FButton(Command.EDIT_TEAM).setLeague(league).setTeam(t));
        		break;
        	case CLOSED: 
        		panel.add(new FLabel(league.getEnding())); 
        		break;
        	default: 
        		unhandledCase(status); // fallthrough
        	case DONE: 
        		panel.add(new FLabel("--")); 
        		break;        		
        	}
        	panel.add(buttons);
        }
    }
    
    void showTeamManagement(User user, Search<TeamSearchKey> search) {
        FFrame frame = getMainFrame();
        frame.addHeader("Team Management");
        FGridPanel panel = new FGridPanel(8);
        frame.add(panel);
        panel.add(new FSortButton("Team", Command.SORT_TEAM_LIST, search).setTeamKey(TeamSearchKey.NAME).setUser(user));
        panel.add(new FSortButton("League", Command.SORT_TEAM_LIST, search).setTeamKey(TeamSearchKey.LEAGUE_NAME).setUser(user));
        panel.add(new FSortButton("Points", Command.SORT_TEAM_LIST, search).setTeamKey(TeamSearchKey.POINTS).setUser(user));
        panel.add(new FSortButton("Rank", Command.SORT_TEAM_LIST, search).setTeamKey(TeamSearchKey.RANK).setUser(user));
        panel.add(new FSortButton("Leader", Command.SORT_TEAM_LIST, search).setTeamKey(TeamSearchKey.LEADER).setUser(user));
        panel.add(new FSortButton("Status", Command.SORT_TEAM_LIST, search).setTeamKey(TeamSearchKey.LEAGUE_STATUS).setUser(user));
        panel.add(new FSortButton("Date", Command.SORT_TEAM_LIST, search).setTeamKey(TeamSearchKey.LEAGUE_STATUS_DATE).setUser(user));
        panel.addHeader("Action");
        List<Team> teams = service.getTeams(user, search);
        Iterator<Team> it = teams.iterator();
        while (it.hasNext()) {
        	Team t = it.next();
            League league = service.getLeague(t.getLeagueId());
        	panel.add(new FLabel(t.getName()));
        	panel.add(new FLabel(league.getName()));
        	panel.add(new FLabel(t.getPoints()));
        	panel.add(new FLabel(service.getTeamRank(t)));
        	panel.add(new FLabel(Team.getName(service.getLeagueLeader(league.getId()))));
        	LeagueStatus status = league.getStatus();
        	panel.add(new FLabel(status.name()));
        	JPanel buttons = new JPanel();
        	buttons.setLayout(new FlowLayout());
        	switch (status) {
        	case OPEN: 
        		panel.add(new FLabel(league.getDraft()));
        		buttons.add(new FButton(Command.EDIT_TEAM).setLeague(league).setTeam(t));
        		break;
        	case CLOSED: 
        		panel.add(new FLabel(league.getEnding()));
                buttons.add(new FButton(Command.MONITOR_TEAM).setLeague(league).setTeam(t));
        		break;
        	default: 
        		unhandledCase(status); // fallthrough
        	case DONE: 
        		panel.add(new FLabel("--")); 
        		break;        		
        	}
        	panel.add(buttons);
        }
        frame.addFooter(new FButton(Command.JOIN_LEAGUE));
    }
    
    void showMonitorTeam(League league, Team team) {
        FFrame frame = getMainFrame();
        frame.addHeader("MONITOR TEAM " + team.getName());
        Iterator<TeamPlayer> it = team.getPlayers().iterator();
        FGridPanel panel = new FGridPanel(4);
        frame.add(panel);
        panel.addHeader("Position");
        panel.addHeader("Player");
        panel.addHeader("Stats");
        panel.addHeader("Action");
        Franchise franchise = service.getFranchise(league.getFranchiseId());
        while (it.hasNext()) {
            TeamPlayer player = it.next();
            panel.add(player.getPosition());
            Player fPlayer = franchise.getPlayer(player.getPlayerId());
            FLabel label = new FLabel(makePlayerNameString(fPlayer));
            panel.add(label);
            List<LeagueStat> stats = league.getPosition(player.getPosition()).getStats();
            panel.add(this.makeLeagueStatsString(stats));
            panel.add(new FButton(Command.CHANGE_TEAM_PLAYER).setTeam(team).setLeague(league).set(player).set(label));
        }
        frame.addFooter(new FButton("BACK", Command.VIEW_TEAMS).setUser(user));
    }
    
    void showLeagueDetails(League league) {
    	FFrame frame = getMainFrame();
    	frame.addHeader("League " + league.getName() + " Details");
    	HashSet<String> allStats = new HashSet(); 
    	Iterator<LeaguePosition> pit = league.getPositions().iterator();
    	while (pit.hasNext()) {
    		LeaguePosition pos = pit.next();
    		Iterator<LeagueStat> sit = pos.getStats().iterator();
    		while (sit.hasNext()) {
    			LeagueStat stat = sit.next();
    			allStats.add(stat.getName());
    		}
    	}
    	FGridPanel panel = new FGridPanel(allStats.size() + 1);
    	frame.add(panel);
    	panel.addHeader("Position");
    	Iterator<String> it = allStats.iterator();
    	while (it.hasNext()) {
    		panel.addHeader(it.next());
    	}
		Franchise franchise = service.getFranchise(league.getFranchiseId());
    	pit = league.getPositions().iterator();
    	while (pit.hasNext()) {
    		LeaguePosition pos = pit.next();
    		Position p = franchise.getPosition(pos.getPosition());
    		panel.add(p.getName() + "(" + pos.getNum() + ")");
    		Iterator<String> sit = allStats.iterator();
    		while (sit.hasNext()) {
    			LeagueStat stat = pos.getStat(sit.next());
    			if (stat != null) {
    				panel.add(stat.getMultiplier());
    			} else {
    				panel.add("--");    				
    			}
    		}
    	}
    	frame.addFooter(new FButton("BACK", Command.JOIN_LEAGUE));
    }
    
    void showJoinLeague() {
    	FFrame frame = getMainFrame();
    	frame.addHeader("Join League");
        FGridPanel panel = new FGridPanel(6);
    	frame.add(panel);
    	panel.addHeader("League");
    	panel.addHeader("Franchise");
    	panel.addHeader("Manager");
    	panel.addHeader("Fee");
    	panel.addHeader("Draft Date");
    	panel.addHeader("Action");
        Iterator<League> it = service.getLeaguesByStatus(LeagueStatus.OPEN, null).iterator();
    	while (it.hasNext()) {
    		League l = it.next();
    		if (l.getStatus() == LeagueStatus.OPEN) {
    			panel.add(new FLabel(l.getName()));
    			panel.add(new FLabel(service.getFranchise(l.getFranchiseId()).getName()));
    			panel.add(new FLabel(service.getUser(l.getUserId()).getUserName()));
    			panel.add(new FLabel("--"));
    			panel.add(new FLabel(l.getDraft()));
    			JPanel buttons = new JPanel();
    			buttons.setLayout(new FlowLayout());
    			buttons.add(new FButton(Command.LEAGUE_DETAILS).setLeague(l));
    			buttons.add(new FButton(Command.NEW_TEAM).setLeague(l));
    			panel.add(buttons);
    		}
    	}
   		frame.addFooter(new FButton("BACK", Command.VIEW_USER_TEAMS));
    }
    
    void showEditTeam(League league, Team team) {
        FFrame frame = getMainFrame();
        Franchise franchise = service.getFranchise(league.getFranchiseId());
        if (team == null) {
            frame.addHeader("Create a New Team");
            team = new Team();
            team.setUserId(user.getId());
            team.setLeagueId(league.getId());
            franchise.populateTeamPlayers(league, team);
        } else {
            frame.addHeader("Edit a team");
            franchise.populateTeamPlayers(league, team);
        }
        
        FGridPanel panel = new FGridPanel(3);
        frame.add(panel);
        panel.add(new FLabel("League: "));
        panel.add(new FLabel(league.getName()));
        panel.add(new FLabel());
        panel.add(new FLabel("Name"));
        FTextField name = new FTextField(team.getName());
        panel.add(name);
        panel.add(new FLabel());
        panel.add(new FLabel("Position"));
        panel.add(new FLabel("Top Choice"));
        panel.add(new FLabel("Completed"));
        Iterator<LeaguePosition> it = league.getPositions().iterator();
        while(it.hasNext()) {
            LeaguePosition p = it.next();
            if (p.getNum() == 0)
            	continue;
            Position pos = franchise.getPosition(p.getPosition());
            panel.add(new FButton(pos.getName(), Command.EDIT_TEAM_POSITION).setLeaguePosition(p).setTeam(team));
            TeamPlayer topChoice = team.getTopChoicePlayer(p.getPosition());
            panel.add(new FLabel(topChoice == null ? "--" : 
            	makePlayerNameString(franchise.getPlayer(topChoice.getPlayerId()))));
            panel.add(new FLabel(franchise.isTeamPositionComplete(team, pos.getName()) ? "[X]" : "[ ]"));
        }
        
        frame.addFooter(new FButton(Command.SAVE_TEAM).setTeam(team).setLeague(league).setArray(new FTextField[] { name }));
        frame.addFooter(new FButton(Command.EDIT_TEAM_DONE));
    }
    
    void showEditTeamPosition(Team team, LeaguePosition pos) {
    	FFrame frame = getMainFrame();
        League league = service.getLeague(team.getLeagueId());
    	Franchise franchise = service.getFranchise(league.getFranchiseId());
    	Position position = franchise.getPosition(pos.getPosition());
        frame.addHeader("Edit " + team.getName() + " position " + makePositionString(position));
    	frame.add(new FLabel("Edit rankings for " + position.getLongName()));
    	FRankPlayersPanel panel = new FRankPlayersPanel(position, team);
    	frame.add(panel);
    	frame.addFooter(new FButton("CANCEL", Command.EDIT_TEAM).setTeam(team).setLeague(pos.getLeague()));
    	frame.addFooter(new FButton("SAVE", Command.SAVE_TEAM_PLAYER_RANKINGS)
    			.set(panel).setLeague(pos.getLeague()).setTeam(team));
    }
    
    String makePositionString(Position pos) {
        return pos.getLongName() + "(" + pos.getName() + ")";
    }
    
    void showFranchiseManagement(Search<FranchiseSearchKey> search) {
    	FFrame frame = getMainFrame();
        frame.addHeader("Franchise Management");
        FGridPanel panel = new FGridPanel(5);
        frame.add(panel);
        List<Franchise> franchises = service.getFranchises(search);
        panel.add(new FSortButton("Name", Command.SORT_FRANCHISE_LIST, search).setFranchiseKey(FranchiseSearchKey.NAME));
        panel.add(new FSortButton("Category", Command.SORT_FRANCHISE_LIST, search).setFranchiseKey(FranchiseSearchKey.CATEGORY));
        panel.add(new FSortButton("Leagues", Command.SORT_FRANCHISE_LIST, search).setFranchiseKey(FranchiseSearchKey.NUM_LEAGUES));
        panel.add(new FSortButton("Active", Command.SORT_FRANCHISE_LIST, search).setFranchiseKey(FranchiseSearchKey.ACTIVE));
        panel.addHeader("Action");
        Iterator<Franchise> it = franchises.iterator();
        while (it.hasNext()) {
            Franchise f = it.next();
            panel.add(new FLabel(f.getName()));
            panel.add(new FLabel(f.getCategory()));
            int numLeagues = service.getLeaguesCount(f);
            panel.add(new FLabel(numLeagues));
            panel.add(new FLabel(String.valueOf(f.isActive())));
            //List<FButton> buttons = new ArrayList(2);
            JPanel buttons = new JPanel();
            buttons.setLayout(new FlowLayout());
            buttons.add(new FButton(Command.VIEW_FRANCHISE_DETAILS).setFranchise(f));
            if (numLeagues == 0)
                buttons.add(new FButton(Command.DELETE_FRANCHISE_CONFIRM).setFranchise(f));
            else
            	buttons.add(new FButton(Command.UPDATE_FRANCHISE).setFranchise(f));
            panel.add(buttons);
        }

        frame.addFooter(new FButton(Command.NEW_FRANCHISE));
        frame.setVisible(true);
    }
    
    void showEditFranchise(Franchise f) {
        final FFrame frame = getMainFrame();
        if (f == null) {
            // this is a new franchise
            f = new Franchise();
            frame.addHeader("New Franchise");
        } else {
            frame.addHeader("Edit Franchise");
        }
        
        //JPanel center = new JPanel();
        FGridPanel center = new FGridPanel(1);
        frame.add(center);
        //center.setLayout(new FGridLayout(3, 1));
        //Container panel = new JPanel();
        //JPanel info = new JPanel();
        FGridPanel info = new FGridPanel(2);
        //info.setLayout(new FGridLayout(3, 2));
        center.add(info);
        
        FGridPanel positions = new FGridPanel(4);
        center.add(positions);
        FGridPanel players = new FGridPanel(4);
        center.add(players);
        
        FTextField name = new FTextField(f.getName());
        FTextField category = new FTextField(f.getCategory());
        FButton categoryChooser = new FButton(Command.CHOOSE_FRANCHISE_CATEGORY).set(category);
        
        info.add(new FLabel("Name"));
        info.add(name);
        JPanel panel = new JPanel();
        panel.setLayout(new FGridLayout(1, 2));
        panel.add(category);
        panel.add(categoryChooser);
        info.add(new FLabel("Category"));
        info.add(panel);
        info.add(new FLabel("Active"));
        FToggleButton active = new FToggleButton(f.isActive() ? "ACTIVE" : "INACTIVE", Command.TOGGLE_FRANCHISE_ACTIVE, f.isActive());
        info.add(active);
        
        positions.add(new FLabel("Positon").setHeader2());
        positions.add(new FLabel("Name").setHeader2());
        positions.add(new FLabel("Stats").setHeader2());
        positions.add(new FLabel("Action").setHeader2());
        
        {
            Iterator<Position> pit = f.getPositions().iterator();
            while (pit.hasNext()) {
                Position p = pit.next();
                positions.add(new FLabel(p.getName()));
                positions.add(new FLabel(p.getLongName()));
                positions.add(new FLabel(makeStatsString(p.getStats())));
                JPanel buttons = new JPanel();
                buttons.setLayout(new FlowLayout());
                buttons.add(new FButton(Command.EDIT_POSITION).setPosition(p).setFranchise(f));
                int numLeagues = service.getLeaguesCount(f);
                if (numLeagues == 0) {
                	buttons.add(new FButton(Command.DELETE_POSITION).setPosition(p).setFranchise(f));
                }
                positions.add(buttons);                
            }
        }

        players.add(new FLabel("Player").setHeader2());
        players.add(new FLabel("Positions").setHeader2());
        players.add(new FLabel("Actions").setHeader2());
        players.add(new FLabel().setHeader2());
        
        {
            Iterator<Player> it = f.getPlayers().iterator();
            while (it.hasNext()) {
                Player p = it.next();
                players.add(new FLabel(makePlayerNameString(p)));
                players.add(new FLabel(makePositionsString(f, p.getPositions())));
                JPanel buttons = new JPanel();
                buttons.setLayout(new FlowLayout());
                buttons.add(new FButton(Command.EDIT_PLAYER).setPlayer(p).setFranchise(f));
                int numLeagues = service.getLeaguesCount(f);
                if (numLeagues == 0)
                    buttons.add(new FButton(Command.DELETE_FRANCHISE_PLAYER).setPlayer(p));
                players.add(buttons);
                players.add(new FLabel());
            }
        }
        
        Object [] fields = { name, category, active };
        frame.addFooter(new FButton(Command.SAVE_FRANCHISE).setFranchise(f).setArray(fields));
        frame.addFooter(new FButton(Command.NEW_POSITION).setFranchise(f));
        frame.addFooter(new FButton(Command.NEW_PLAYER).setFranchise(f));
        
    }
    
    void showEditFranchisePlayer(final Franchise f, Player p) {
        final FFrame frame = getMainFrame();
        
        if (p == null) {
            frame.addHeader("New Franchise Player");
            p = new Player();
        } else {
            frame.addHeader("Edit Franchise Player");
        }
        
        FGridPanel panel = new FGridPanel(2);
        frame.add(panel);        
        
        final FTextField fName = new FTextField(p.getFirstName());
        final FTextField lName = new FTextField(p.getLastName());
        final FTextField status= new FTextField(p.getStatus());
        final FTextField info  = new FTextField(p.getInfo());
        
        panel.add(new FLabel("First Name"));
        panel.add(fName);
        panel.add(new FLabel("Last Name"));
        panel.add(lName);
        panel.add(new FLabel("Info"));
        panel.add(info);
        panel.add(new FLabel("Status"));
        panel.add(status);
        panel.add(new FLabel("Positons"));
        panel.add(new FButton(makePositionsString(f, p.getPositions()), 
        		Command.EDIT_FRANCHISE_PLAYER_POSITIONS).setFranchise(f).setPlayer(p));
        
        FTextField [] fields = {
        		fName, lName, info, status
        };
        
        frame.addFooter(new FButton(Command.CANCEL_EDIT_FRANCHISE_PLAYER).setFranchise(f));
        frame.addFooter(new FButton(Command.SAVE_FRANCHISE_PLAYER).setPlayer(p).setArray(fields).setFranchise(f));

    }
    
    void showImportPositionData(Position p) {
    	FFrame frame = getMainFrame();
    	frame.addHeader("Import Position Data");
    	FGridPanel panel = new FGridPanel(2);
    	frame.add(panel);
    	panel.add(new FLabel("Spread Sheet"));
    	FButton file = new FButton(Command.CHOOSE_SPREADSHEET_FILE);
    	panel.add(file);
    	panel.add(new FLabel("Player\nColumn"));
    	FTextField playerCol = new FTextField("Player");
    	panel.add(playerCol);
    	frame.addFooter(new FButton(Command.CANCEL_EDIT_FRANCHISE_POSITION).setFranchise(p.getFranchise()));
    	//JComponent [] array = { file, playerCol };
    	frame.addFooter(new FButton(Command.IMPORT_POSITION_DATA)
                .set(file).set(playerCol).setPosition(p));
    }
    
    void pushFileChooserDialog(String ext, FButton field) {
    	currentFrame.setEnabled(false);
    	JFileChooser chooser = null;
    	if (fileChooserPath == null)
    		chooser = new JFileChooser(".");
    	else
    		chooser = new JFileChooser(fileChooserPath);
    	chooser.setFileFilter(new FileExtensionFilter("xls"));
    	int status = chooser.showOpenDialog(null);
    	switch (status) {
    	case JFileChooser.APPROVE_OPTION:
    		fileChooserPath = chooser.getSelectedFile().getParentFile();
    		field.setText(chooser.getSelectedFile().getAbsolutePath());
    		// fallthrough
    	case JFileChooser.CANCEL_OPTION:
    		currentFrame.setEnabled(true);
    		break;
    	default:
    		unhandledCase(status);
    	}
    }
    
    void showEditFranchisePosition(Franchise f, Position p) {
        final FFrame frame = getMainFrame();
        if (p == null) {
            frame.addHeader("New Franchise Position");
            p = new Position();
        } else {
            frame.addHeader("Edit Franchise Position");
        }
        
        FGridPanel panel = new FGridPanel(2);
        
        final FTextField name = new FTextField(p.getName());
        final FTextField longName = new FTextField(p.getLongName());
        
        panel.add(new FLabel("Name"));
        panel.add(name);
        panel.add(new FLabel("Long Name"));
        panel.add(longName);
        panel.add(new FLabel("Stats"));
        panel.add(new FButton(makeStatsString(p.getStats()), Command.EDIT_FRANCHISE_POSITION_STATS)
                .setFranchise(f).setPosition(p));
        frame.add(panel);
        
        FTextField [] fields = { name, longName };
        
        frame.addFooter(new FButton(Command.SAVE_FRANCHISE_POSITION).setFranchise(f).setPosition(p).setArray(fields));
        frame.addFooter(new FButton(Command.CANCEL_EDIT_FRANCHISE_POSITION).setFranchise(f));
        if (p.getFranchise() != null)
        	frame.addFooter(new FButton(Command.SHOW_IMPORT_POSITION).setPosition(p));
    }
    
    void showEditFranchisePositionStats(Franchise f, Position position) {
        FFrame frame = getMainFrame();
        frame.addHeader("Edit " + f.getName() + " Stats for " + makePositionString(position));
        List<Stat> stats = position.getStats();
        FGridPanel panel = new FGridPanel(4);
        frame.add(panel);
        panel.add(new FLabel("Name").setHeader2());
        panel.add(new FLabel("Description").setHeader2());
        panel.add(new FLabel("Action").setHeader2(), 2);
        Iterator<Stat> it = stats.iterator();

        while (it.hasNext()) {
            Stat stat = it.next();
            FTextField name = new FTextField(stat.getName());
            FTextField desc = new FTextField(stat.getDescription());
            panel.add(name);
            panel.add(desc);
            panel.add(new FButton(Command.SAVE_STAT).setStat(stat).setArray(new FTextField[] { name, desc}));
            panel.add(new FButton(Command.DELETE_STAT).setStat(stat).setPosition(position));
        }

        frame.addFooter(new FButton("DONE", Command.EDIT_POSITION).setFranchise(f).setPosition(position));
        frame.addFooter(new FButton(Command.NEW_FRANCHISE_POSITION_STAT).setFranchise(f).setPosition(position));
    }
    
    void showEditFranchisePlayerPositions(Franchise franchise, final Player p) {
        FFrame frame = getMainFrame();
        frame.addHeader("Edit Positions for Player " + makePlayerNameString(p));
        FGridPanel panel = new FGridPanel(1);
        frame.add(panel);
        panel.add(new FLabel("Position"));
        List<Position> positions = franchise.getPositions();
        Iterator<Position> it = positions.iterator();
        while (it.hasNext()) {
            Position pos = it.next();
            if (pos.getId() >= 0) {
                boolean enabled = p.getPositions().contains(pos.getId());
                panel.add(new FToggleButton(this.makePositionString(pos), 
                        Command.TOGGLE_PLAYER_POSITION_ENABLED, enabled).setPosition(pos));
            }
        }
        
        frame.addFooter(new FButton("Done", Command.EDIT_PLAYER).setPlayer(p).setFranchise(franchise));
    }
    
    String makeStatsString(List<Stat> stats) {
        if (stats.size() == 0)
            return "";
        StringBuffer buf = new StringBuffer(stats.get(0).getName());
        int i = 1;
        for ( ; i<stats.size() && i<4; i++)
            buf.append(",").append(stats.get(i).getName());
        if (i<stats.size())
        	buf.append(",...");        	
        return buf.toString();
    }

    String makeLeagueStatsString(List<LeagueStat> stats) {
        if (stats.size() == 0)
            return "";
        StringBuffer buf = new StringBuffer();
        for (int i=0; i<stats.size(); i++)
        	if (stats.get(i).getMultiplier() != 0) {
        		if (buf.length() == 0)
        			buf.append(stats.get(i).getName());
        		else
        			buf.append(",").append(stats.get(i).getName());
        	}
        if (buf.length() > 20) {
        	return buf.substring(0, 20) + "...";
        }
        return buf.toString();
    }

    String makePlayerNameString(Player p) {
        String nm = "";
        if (p.getFirstName() != null && p.getFirstName().length() > 0)
            nm += p.getFirstName().charAt(0) + ". ";
        if (p.getLastName() != null)
            nm += p.getLastName();
        return nm;
    }
    
    String makePositionsString(Franchise f, List<String> positions) {
        if (positions.size() == 0)
            return "";
        StringBuffer buf = new StringBuffer(positions.get(0));
        int i = 1;
        for ( ; i<positions.size(); i++) {
            buf.append(",").append(positions.get(i));
        }     
        if (i<positions.size())
        	buf.append(",...");        	
        return buf.toString();
    }
    
    String getDateString(Date date) {
    	return DateFormat.getDateInstance().format(date);
    }

    void showEditLeague(League l) {
        final FFrame frame = getMainFrame();
    	if (l == null) {
            frame.addHeader("New League");
            l = new League();
            l.setUserId(user.getId());
    	} else {
            frame.addHeader("Edit League");
    	} 
        FGridPanel panel = new FGridPanel(2);
        
        frame.add(panel);
        final FTextField name = new FTextField(l.getName());
        final FButton editPositionsButton = new FButton(Command.EDIT_LEAGUE_POSITIONS).setLeague(l);
        final FButton franchise = new FButton(Command.CHOOSE_FRANCHISE).set(editPositionsButton).setLeague(l);
        try {
        	Franchise f = service.getFranchise(l.getFranchiseId());
        	franchise.setText(f.getName());
        	franchise.setFranchise(f);
        	franchise.setEnabled(false);
        } catch (FantasyException e) {
        	editPositionsButton.setEnabled(false);        
        }
        final FTextField maxPlayers = new FTextField(String.valueOf(l.getMaxPlayers()));
        final FDatePicker draftDate = new FDatePicker(l.getDraft());
        final FDatePicker endDate = new FDatePicker(l.getEnding());
        panel.add(new FLabel("Name"));
        panel.add(name);
        panel.add(new FLabel("Franchise"));
        panel.add(franchise);
        panel.add(new FLabel("Draft Date"));
        panel.add(draftDate);
        panel.add(new FLabel("End Date"));
        panel.add(endDate);
        panel.add(new FLabel("Max Players"));
        panel.add(maxPlayers);
        JComponent [] buttons = {
        	name, draftDate, endDate, maxPlayers, franchise
        };
        frame.addFooter(new FButton(Command.SAVE_LEAGUE).setArray(buttons).setLeague(l));
        frame.addFooter(editPositionsButton);
        frame.addFooter(new FButton("BACK", Command.VIEW_USER_LEAGUES));
        
    }
    
    void showEditLeaguePositions(League league) {
    	FFrame frame = getMainFrame();
    	frame.addHeader("Edit League Positions");
        FGridPanel panel = new FGridPanel(3);
        frame.add(panel);
    	panel.addHeader("Position");
    	panel.addHeader("Num");
    	panel.addHeader("Stats");
    	// show all the possible positions as specified by the franchise
    	// positions marked as num[0] are excluded from the league
    	Franchise f = service.getFranchise(league.getFranchiseId());
    	//f.populateNewLeague(league, 0, 0);
    	FTextField [] numFields = new FTextField[f.getPositions().size()];
    	Iterator<Position> it = f.getPositions().iterator();
    	int index = 0;
    	while (it.hasNext()) {
    		Position p = it.next();
    		panel.add(new FLabel(p.getName()));
    		LeaguePosition pos = league.getPosition(p.getName());
    		numFields[index] = new FTextField(String.valueOf(pos.getNum()));
    		panel.add(numFields[index]);
    		index++;
    		panel.add(new FButton(this.makeLeagueStatsString(pos.getStats()), Command.EDIT_LEAGUE_POSITION_STATS).setLeaguePosition(pos).setPosition(p));
    	}
    	
    	frame.addFooter(new FButton(Command.EDIT_LEAGUE_POSITIONS_DONE).setArray(numFields).setLeague(league));
    }
    
    void showEditLeaguePositionStats(Position fPos, LeaguePosition lPos) {
    	FFrame frame = getMainFrame();
    	frame.addHeader("Edit " + lPos.getLeague().getName() + " Stats for " + fPos.getName());
        
        FGridPanel panel = new FGridPanel(3);
    	frame.add(panel);
    	panel.add(new FLabel("Stat").setHeader2());
    	panel.add(new FLabel("Multiplier").setHeader2(), 2);
    	FTextField [] multField = new FTextField[lPos.getStats().size()];
    	Iterator<LeagueStat> it = lPos.getStats().iterator();
    	int index = 0;
    	while (it.hasNext()) {
    		LeagueStat stat = it.next();
    		panel.add(new FLabel(stat.getName()));
    		multField[index] = new FTextField(String.valueOf(stat.getMultiplier()));
    		panel.add(multField[index]);
            JPanel buttons = new JPanel();
            buttons.setLayout(new GridLayout(2, 1));
            buttons.add(new FButton("+1", Command.INCREMENT_MULTIPLIER).setId(1).set(multField[index]));
            buttons.add(new FButton("-1", Command.INCREMENT_MULTIPLIER).setId(-1).set(multField[index]));
            panel.add(buttons);
            index++;
    	}
    	frame.addFooter(new FButton(Command.EDIT_LEAGUE_POSITION_STATS_DONE)
    			.setArray(multField).setLeaguePosition(lPos).setLeague(lPos.getLeague()));
    }
    
    void pushEditUser(User u) {
    	
    	final FFrame frame = newPopupFrame("");
        
        boolean canEdit = true;
        if (user != null) {
            canEdit = user.hasAdminAccess() || user == u;
        }
        
        if (u == null) {        
        	frame.addHeader("New User");
        	u = new User();
            u.setActive(true);
        } else if (canEdit) {
        	frame.addHeader("Edit User " + u.getUserName());
        } else {
            frame.addHeader("View User " + u.getUserName());
        }
    	
        // check if the userlist is empty, if it is load a custom page
        // to create the admin
        final FTextField firstName = new FTextField(u.getFirstName(), canEdit);
        final FTextField lastName = new FTextField(u.getLastName(), canEdit);
        final FTextField userName = new FTextField(u.getUserName(), canEdit);
        final JTextField passWord = new JPasswordField(u.getPassWord());
        final JTextField passWordConfirm = new JPasswordField(u.getPassWord());
        final FTextField email = new FTextField(u.getEmail(), canEdit);
        
        FGridPanel panel = new FGridPanel(2);
        
        frame.add(panel);
        
        panel.add(new FLabel("First Name"));
        panel.add(firstName);
        panel.add(new FLabel("Last Name"));
        panel.add(lastName);
        panel.add(new FLabel("UserName"));
        panel.add(userName);
        if (canEdit) {
            panel.add(new FLabel("Password"));
            panel.add(passWord);
            panel.add(new FLabel("Confirm Password"));
            panel.add(passWordConfirm);
        }
        panel.add(new FLabel("email"));
        panel.add(email);
        
        JTextField [] fields = {
        		firstName, lastName, userName, passWord, passWordConfirm, email
        };
        
        if (canEdit) {
        	if (u.getAccess() == UserAccess.NONE) {
		        if (service.getUserCount() == 0) {
		        	frame.addFooter(new FButton(Command.SAVE_USER_ADMIN).setArray(fields).setUser(u).setId(UserAccess.ADMIN.ordinal()));
		        } else {
		        	frame.addFooter(new FButton(Command.SAVE_USER_BASIC).setArray(fields).setUser(u).setId(UserAccess.TEAM.ordinal()));
		        	frame.addFooter(new FButton(Command.SAVE_USER_LEAGUEMGR).setArray(fields).setUser(u).setId(UserAccess.LEAGUE.ordinal()));
		        	frame.addFooter(new FButton(Command.CANCEL_EDIT_USER));
		        }
        	} else {
        		frame.addFooter(new FButton(Command.SAVE_USER).setArray(fields).setUser(u).setId(user.getAccess().ordinal()));
	        	frame.addFooter(new FButton(Command.CANCEL_EDIT_USER));
        	}
        }        
    }    
    
    void pushOkPopup(String label, String msg) {
        FFrame frame = newPopupFrame(label);        
        frame.add(new FWrapLabel(msg, Justify.CENTER, Justify.CENTER));
        frame.addFooter(new FButton(Command.OK));
    }
    
    void pushYesNoPopup(String label, String msg, Command yesCmd, Object yesExtra, Command noCmd, Object noExtra) {
        FFrame frame = newPopupFrame(label);
        frame.add(new FLabel(msg));
        frame.addFooter(new FButton("YES", yesCmd.name()).set(yesExtra));
        frame.addFooter(new FButton("NO", noCmd.name()).set(noExtra));
    }
    
    void pushChooseFranchisePopup(FButton button, FButton editPositionsButton, League league) {
        FFrame frame = newPopupFrame("Choose Franchise");
        List<Franchise> franchises = service.getFranchises(null);
        FGridPanel panel = new FGridPanel(1);
        Iterator<Franchise> it = franchises.iterator();
        while (it.hasNext()) {
            Franchise f = it.next();
            panel.add(new FButton(f.getName(), Command.SET_FRANCHISE)
            		.setFranchise(f)
            		.setFrame(frame)
            		.setLeague(league)
            		.setArray(new FButton[] { button, editPositionsButton }));
        }
        frame.add(panel);
        frame.addFooter(new FButton(Command.CANCEL));
    }
    
    void pushChooseFranchiseCategory(FButton button, FTextField field) {
    	FFrame frame = newPopupFrame("Choose Category");
        List<String> allCategories = service.getAllFranchiseCategories();
    	FGridPanel panel = new FGridPanel(1);
    	Iterator<String> cit = allCategories.iterator();
    	while (cit.hasNext()) {
    		panel.add(new FButton(cit.next(), Command.SET_FRANCHISE_CATEGORY).set(field));
    	}
        frame.add(panel);
        frame.addFooter(new FButton(Command.CANCEL));
    }
    
    void pushChooseNewTeamPlayerDialog(League league, TeamPlayer playerToEdit, FLabel label) {
        FFrame frame = newPopupFrame("Choose New Player");
        
        List<Team> teams = service.getTeamsByLeague(league.getId(), null);
        Iterator<Player> pit = service.getFranchise(league.getFranchiseId())
            .getAvailablePlayers(teams, playerToEdit.getPosition()).iterator();
        
        FGridPanel panel = new FGridPanel(1);
        frame.add(panel);
        // now add all the unused players TODO: Move this to the service
        while (pit.hasNext()) {
            Player player = pit.next();
            panel.add(new FButton(player.getFirstName() + " " + player.getLastName(), Command.SET_TEAM_PLAYER)
                    .set(playerToEdit).setPlayer(player).set(label));
        }
        frame.addFooter(new FButton(Command.CANCEL));
    }

    
    private void unhandledCase(int value) {
        log.error("Unhandled case [" + value + "]");
    }

    private void unhandledCase(Enum value) {
        log.error("Unhandled case [" + value + "]");
    }
    
	public void actionPerformed(ActionEvent ev) {
        Command cmd = null;
        try {
    		cmd = Command.valueOf(ev.getActionCommand());
    		FButton button = (FButton)ev.getSource();		
            log.debug("Processing cmd [" + cmd + "]");
            
    		switch (cmd) {
            case NEW_USER: 
                button.getFrame().close();
            	pushEditUser(null); 
            	break;
            case SAVE_USER:
            case SAVE_USER_BASIC: 
            case SAVE_USER_LEAGUEMGR: 
            case SAVE_USER_ADMIN: 
                try {
                    JTextField [] fields = (JTextField[])button.getArray();
                    User user = button.getUser();
                    user.setFirstName(fields[0].getText());
                    user.setLastName(fields[1].getText());
                    user.setUserName(fields[2].getText());
                    user.setPassWord(fields[3].getText());
                    if (!fields[3].getText().equals(fields[4].getText()))
                        throw new FantasyException(ErrorCode.PASSWORD_MISMATCH);
                    user.setEmail(fields[5].getText());
                    user.setAccess(UserAccess.values()[button.getId()]);
                    service.addOrUpdateUser(user);
                    button.getFrame().close();
                    if (instance.user == null) {
                    	login(user);
                    }
                } catch (FantasyException e) {
                    pushOkPopup("ERROR", e.getMessage());
                }    
            	break;
            case TOGGLE_USER_ACTIVE: {
                User user = button.getUser();
                if (user.isActive()) {
                    user.setActive(false);
                    button.setText("NO");
                } else {
                    user.setActive(true);
                    button.setText("YES");
                }
                service.addOrUpdateUser(user);
                break;
            }
            case CANCEL_EDIT_USER: 
                button.getFrame().close();
                if (this.user == null)
                    pushLogin();
                break;
            case LOGIN: {
                JTextField [] fields = (JTextField[])button.getArray();
                String userName = fields[0].getText();
                String passWord = fields[1].getText();
                login(service.getUser(userName, passWord));
                config.setString("user.name", userName);
                config.setString("user.pw", passWord);
                button.getFrame().close();
                break;
            }
                
            case LOGOUT: 
            	user = null; 
                config.setString("user.name", null);
                config.setString("user.pw", null);
            	pushLogin();
                currentFrame.close();
                currentFrame = null;
            	break;
            case VIEW_USER:
            	pushEditUser(button.getUser());
            	break;
            case NEW_FRANCHISE: 
            	showEditFranchise(null); 
            	break;
    		case VIEW_FRANCHISES: 
    			showFranchiseManagement(null); 
    			break;
    		case SET_FRANCHISE: {
    			FButton [] affected = (FButton[])button.getArray(); 
    			FButton callee = affected[0];
    			FButton editPositionsButton = affected[1];
    			callee.setFranchise(button.getFranchise());
    			callee.setText(button.getText());
    			editPositionsButton.setEnabled(true);
    			button.getFranchise().populateLeaguePositions(button.getLeague(), 0, 0);
    			button.getFrame().close();
    			break;
    		}
    		case TOGGLE_FRANCHISE_ACTIVE: {
    			if (button.getActive()) {
    				button.setActive(false);
    				button.setText("ACTIVE");
    			} else {
    				button.setActive(true);
    				button.setText("INACTIVE");
    			}
    			break;
    		}
    		case VIEW_LEAGUES:
                showLeagueManagement(null, null);
                break;
    		case VIEW_TEAMS:
                showTeams(null);
                break;
    		case VIEW_USERS:
                showUsers(null);
                break;
            case VIEW_USER_LEAGUES: 
            	showLeagueManagement(user, null); 
            	break;
            case VIEW_USER_TEAMS: 
            	showTeamManagement(user, null); 
            	break;
            case VIEW_FRANCHISE_DETAILS: 
            	showEditFranchise(button.getFranchise()); 
            	break;
            case DELETE_FRANCHISE_CONFIRM: {
                pushYesNoPopup("Confirm Delete", "Are you sure you want to delete the franchise " + button.getFranchise().getName(), 
                        Command.DELETE_FRANCHISE, button.getFranchise(), Command.CANCEL, null);
                break;
            }
            case DELETE_FRANCHISE: 
                button.getFrame().close();
                service.deleteFranchise(button.getFranchise());
                showFranchiseManagement(null);
                break;                
                
            case SAVE_FRANCHISE: {
            	Object [] fields = button.getArray();
            	FTextField name = (FTextField)fields[0];
            	FTextField category = (FTextField)fields[1];
            	FToggleButton active = (FToggleButton)fields[2];
                button.getFranchise().setName(name.getText());
                button.getFranchise().setCategory(category.getText());
                button.getFranchise().setActive(active.getActive());
                service.addOrUpdateFranchise(button.getFranchise());
                break;
            }
            case NEW_POSITION: 
            	showEditFranchisePosition(button.getFranchise(), null); 
            	break;
            case EDIT_POSITION: 
            	showEditFranchisePosition(button.getFranchise(), button.getPosition()); 
            	break;
            case NEW_PLAYER: 
            	showEditFranchisePlayer(button.getFranchise(), null); 
            	break;
            case EDIT_PLAYER: 
            	showEditFranchisePlayer(button.getFranchise(), button.getPlayer()); 
            	break;
            case SAVE_FRANCHISE_PLAYER: {        	
            	FTextField [] field = (FTextField[])button.getArray();
            	Player player = button.getPlayer();
                player.setFirstName(field[0].getText());
                player.setLastName(field[1].getText());
                player.setInfo(field[2].getText());
                player.setStatus(field[3].getText());
                button.getFranchise().addPlayer(player);
                showEditFranchise(button.getFranchise());
            	break;
            }
            case SAVE_FRANCHISE_POSITION: {
                Position p = button.getPosition();
                FTextField [] fields = (FTextField[])button.getArray();
                p.setName(fields[0].getText());
                p.setLongName(fields[1].getText());
                button.getFranchise().addPosition(p);
                //service.addOrUpdateFranchisePosition(button.getFranchise(), p);
                showEditFranchisePosition(button.getFranchise(), button.getPosition());
                break;
            }
            case CHOOSE_SPREADSHEET_FILE:
            	this.pushFileChooserDialog("xls", button);
            	break;
            case SHOW_IMPORT_POSITION: {
            	showImportPositionData(button.getPosition());
            	break;
            }
            case IMPORT_POSITION_DATA: {
                String file = button.get(FButton.class).getText();
                String playerColumn = button.get(FTextField.class).getText();
            	Position position = button.getPosition();
            	position.setSpreadSheetFile(file);
            	position.setPlayerColumn(playerColumn);
                service.addOrUpdateFranchise(position.getFranchise());
            	new FranchiseUpdate(service).importData(position.getFranchise(), position, null); 
                service.addOrUpdateFranchise(position.getFranchise());
            	this.showEditFranchise(position.getFranchise());
            	break;
            }
            case CANCEL_EDIT_FRANCHISE_POSITION:
            case CANCEL_EDIT_FRANCHISE_PLAYER:
                showEditFranchise(button.getFranchise());
            	break;
            case DELETE_FRANCHISE_PLAYER: {
                button.getFrame().close();
                Franchise f = (button.getPlayer()).getFranchise();
                f.getPlayers().remove(button.getPlayer());
                this.showEditFranchise(f);
                break;
            }
            case EDIT_FRANCHISE_POSITION_STATS: 
            	showEditFranchisePositionStats(button.getFranchise(), button.getPosition()); 
            	break;
            case NEW_FRANCHISE_POSITION_STAT:
                button.getPosition().addStat(new Stat());
                showEditFranchisePositionStats(button.getFranchise(), button.getPosition()); 
                break;
            case EDIT_FRANCHISE_PLAYER_POSITIONS: 
            	showEditFranchisePlayerPositions(button.getFranchise(), button.getPlayer()); 
            	break;
            case REMOVE_PLAYER_POSITION: 
                button.getPlayer().getPositions().remove(button.getId());
                showEditFranchisePlayerPositions(button.getFranchise(), button.getPlayer()); 
                break;
            case UPDATE_FRANCHISE: {
            	// clear team points       
            	List<League> leagues = service.getLeagues(button.getFranchise(), null);
            	FranchiseUpdate update = new FranchiseUpdate(service);
            	Iterator<Position> it = button.getFranchise().getPositions().iterator();
            	while (it.hasNext()) {
            		Position pos = it.next();
            		update.importData(pos.getFranchise(), pos, leagues);
            	}
            	update.updateTeamPoints();
            	service.addOrUpdateLeagues(leagues);
            	break;
            }
            case TOGGLE_PLAYER_POSITION_ENABLED: {
            	Player p = button.getPlayer();
            	Position pos = button.getPosition();
                if (p.getPositions().contains(pos.getName()))
                    p.getPositions().remove(pos.getName());
                else
                    p.getPositions().add(pos.getName());
            	break;
            }
            case SAVE_STAT: {
                FTextField [] fields = (FTextField[])button.getArray();
                Stat s = button.getStat();
                s.setName(fields[0].getText());
                s.setDescription(fields[1].getText());
            	break;
            }
            case DELETE_STAT: {
                Stat s = button.getStat();
                button.getPosition().getStats().remove(s);
                button.getPosition().addOmitStat(s.getName());
                showEditFranchisePositionStats(button.getPosition().getFranchise(), button.getPosition()); 
                break;
            }
            
            case NEW_LEAGUE: 
            	showEditLeague(null); 
            	break;
    		case LEAGUE_DETAILS:
    			showLeagueDetails(button.getLeague());
    			break;
            case OK: 
            case CANCEL: 
                button.getFrame().close(); 
                break;
            case CHOOSE_FRANCHISE: {
            	FButton editPositionsButton = button.get(FButton.class);
                pushChooseFranchisePopup(button, editPositionsButton, button.getLeague()); 
                break;
            }
            case CHOOSE_FRANCHISE_CATEGORY: 
            	pushChooseFranchiseCategory(button, button.get(FTextField.class));
            	break;
            case SET_FRANCHISE_CATEGORY: {
            	FTextField field = button.get(FTextField.class);
            	field.setText(button.getText());
                button.getFrame().close();
            	break;
            }
            case EDIT_LEAGUE:
            	this.showEditLeague(button.getLeague());
            	break;                
            case EDIT_LEAGUE_POSITIONS: 
            	showEditLeaguePositions(button.getLeague()); 
            	break;
                
            case EDIT_LEAGUE_POSITION_STATS: 
            	this.showEditLeaguePositionStats(button.getPosition(), button.getLeaguePosition());
            	break;
            
            case EDIT_LEAGUE_POSITIONS_DONE: {
            	FTextField [] numFields = (FTextField[])button.getArray();
            	League league = button.getLeague();
            	Franchise franchise = service.getFranchise(league.getFranchiseId());
            	Iterator<Position> it = franchise.getPositions().iterator();
            	int index = 0;
            	while (it.hasNext()) {
            		Position p = it.next();
            		LeaguePosition pos = league.getPosition(p.getName());
            		pos.setNum(Integer.parseInt(numFields[index++].getText()));
            	}
            	showEditLeague(league);
            	break;
            }
            
            case EDIT_LEAGUE_POSITION_STATS_DONE: {
            	FTextField [] multFields = (FTextField[])button.getArray(); 
            	LeaguePosition pos = button.getLeaguePosition();
            	Iterator<LeagueStat> it = pos.getStats().iterator();
            	int index = 0;
            	while (it.hasNext()) {
            		LeagueStat stat = it.next();
            		stat.setMultiplier(Float.parseFloat(multFields[index++].getText()));
            	}
            	this.showEditLeaguePositions(button.getLeague());
            	break;
            }
            
            case INCREMENT_MULTIPLIER: {
                float amount = button.getId();
                FTextField field = button.get(FTextField.class);
                float curVal = Float.parseFloat(field.getText());
                float newVal = curVal + amount;
                field.setText(String.valueOf(newVal));
                break;
            }
            
            case SAVE_LEAGUE: {                
            	JComponent [] buttons = (JComponent [])button.getArray();
            	FTextField name = (FTextField)buttons[0];
            	FDatePicker draftDate = (FDatePicker)buttons[1];
            	FDatePicker endDate = (FDatePicker)buttons[2];
            	FTextField maxPlayers = (FTextField)buttons[3];
            	FButton franchise = (FButton)buttons[4];
            	if (franchise.getFranchise() == null)
            		throw new FantasyException(ErrorCode.EMPTY_FIELD, "franchise");
            	League league = button.getLeague();
                league.setName(name.getText());
                if (league.getCreated() == null)
                	league.setCreated(new Date());
                league.setDraft(draftDate.getDate());
                league.setEnding(endDate.getDate());
                league.setMaxPlayers(Integer.parseInt(maxPlayers.getText()));
                league.setFranchiseId(franchise.getFranchise().getId());
                service.addOrUpdateLeague(league);
            	break;
            }
                
            case EDIT_TEAM: 
            	showEditTeam(button.getLeague(), button.getTeam()); 
            	break;
            case NEW_TEAM: 
            	showEditTeam(button.getLeague(), null); 
            	break;
            case SAVE_TEAM: {
            	FTextField [] field = (FTextField[])button.getArray();
            	button.getTeam().setName(field[0].getText());
                service.addOrUpdateTeam(button.getTeam());
            	break;
            }
            case EDIT_TEAM_DONE: 
            	showTeamManagement(user, null); 
            	break;   			
            case JOIN_LEAGUE: 
            	showJoinLeague(); 
            	break;	
            case EDIT_TEAM_POSITION: 
            	showEditTeamPosition(button.getTeam(), button.getLeaguePosition()); 
            	break;
            case SAVE_TEAM_PLAYER_RANKINGS: {
                button.get(FRankPlayersPanel.class).commitChanges();
            	showEditTeam(button.getLeague(), button.getTeam());
            	break;
            }
            
            case RUN_LEAGUE_DRAFT: {
                new DraftUpdate(service).runDraft(button.getFranchise(), button.getLeague());
                List<Team> teamsToUpdate = service.getTeamsByLeague(button.getLeague().getId(), null);
                service.addOrUpdateTeams(teamsToUpdate);
                service.addOrUpdateLeague(button.getLeague());
                this.showLeagueManagement(user, null);
                break;
            }	

            case MONITOR_TEAM:
                showMonitorTeam(button.getLeague(), button.getTeam());
                break;
                
            case CHANGE_TEAM_PLAYER:
                this.pushChooseNewTeamPlayerDialog(button.getLeague(), button.get(TeamPlayer.class), button.get(FLabel.class));
                break;
                
            case SET_TEAM_PLAYER: {
                TeamPlayer playerToChange = button.get(TeamPlayer.class);
                Player newPlayer = button.getPlayer();
                playerToChange.setPlayerId(newPlayer.getId());
                FLabel label = button.get(FLabel.class);
                String newPlayerString = makePlayerNameString(newPlayer);
                log.debug("Changing player from [" + label.getText() + "] to [" + newPlayerString + "]");
                service.addOrUpdateTeam(playerToChange.getTeam());
                label.setText(newPlayerString);
                button.getFrame().close();
                break;
            }
            
            // show prob keep search instances for all the lists instead of putting in the
            // button so when returning to pages we have preserved our search.  Should 
            // prob also pass in the list to the show method so we can call custom fetches
            // as needed.  OR we can simply have all the data IN the model objects ... hmmmm ....
            case SORT_USER_LIST: {
            	Search search = processSearch(button, button.get(UserSearchKey.class));
            	showUsers(search);
            	break;
            }

            case SORT_FRANCHISE_LIST:{
                Search search = processSearch(button, button.get(FranchiseSearchKey.class));
            	showFranchiseManagement(search);
            	break;
            }

            case SORT_TEAM_LIST: {
            	Search search = processSearch(button, button.get(TeamSearchKey.class));
            	if (button.getUser() == null)
            		showTeams(search);
            	else
            		showTeamManagement(button.getUser(), search);
            	break;
            }

            case SORT_LEAGUE_LIST: {
                Search search = processSearch(button, button.get(LeagueSearchKey.class));
            	showLeagueManagement(button.getUser(), search);
            	break;
            }
    	    default: 
                unhandledCase(cmd); 
    		}

    		if (popupFrame != null) {
                popupFrame.finalizeFrame();
                popupFrame.center(currentFrame);
                popupFrame = null;
    		} else if (currentFrame != null){
                currentFrame.setVisible(true);
        		currentFrame.setEnabled(true);
                currentFrame.validate();
            }
        } catch (NumberFormatException e) {
        	pushOkPopup("ERROR", cmd.name() + "\n\nERROR\nNot a number\n\n[ " + e.getMessage() + "]");
        } catch (FantasyException e) {
            pushOkPopup("ERROR", cmd.name() + "\n\nFailed, error: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        if (popupFrame != null) {
            popupFrame.finalizeFrame();
            popupFrame.center(currentFrame);
            popupFrame = null;
        }
	}
	
	public <T> Search<T> processSearch(FButton sortButton, T key) {
    	Search<T> search = sortButton.get(Search.class);
    	if (search == null)
    		search = new Search<T>();

        if (search.getKey() != null && search.getKey().equals(key)) {
            search.setDescending(!search.isDescending());
        } else {
            search.setKey(key);
        }
    	return search;
	}
    
    public void componentMoved(ComponentEvent e) {
        Point newLoc = e.getComponent().getLocation();
        config.setInt("window.x", newLoc.x);
        config.setInt("window.y", newLoc.y);
    }

    public void componentResized(ComponentEvent e) {
        Dimension newSize = e.getComponent().getSize();
        config.setInt("window.w", newSize.width);
        config.setInt("window.h", newSize.height);
    }

    FFrame getMainFrame() {
    	if (currentFrame == null) {
    		currentFrame = new FFrame();
    		currentFrame.setTitle("Fantasy Sports");
    		int x = config.getInt("window.x", 300);
    		int y = config.getInt("window.y", 200);
    		int w = config.getInt("window.w", 600);
    		int h = config.getInt("window.h", 500);
    		currentFrame.setLocation(x, y);
    		currentFrame.setSize(w, h);
    		currentFrame.addSideBar();
            currentFrame.addLogout();
            currentFrame.addComponentListener(this);
    	} else {
    		currentFrame.clearContent();
    	}
    	return currentFrame;
    }
	
    FFrame newPopupFrame(String header) {
    	FFrame frame = new FFrame();
    	frame.addHeader(header);
    	frame.setSize(300, 200);
    	frame.setLocation(400, 300);
    	frame.addHeader(header);
        frame.setResizable(false);
        frame.setPopup();
        popupFrame = frame;
    	return frame;
    }
    
    void exit() {
    	config.save();
    	if (currentFrame != null)
    		currentFrame.close();
        System.exit(0);
    }
    
    public void componentHidden(ComponentEvent e) {}
    public void componentShown(ComponentEvent e) {}


}
