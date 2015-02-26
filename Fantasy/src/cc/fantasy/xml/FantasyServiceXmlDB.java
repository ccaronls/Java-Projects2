package cc.fantasy.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Category;

import com.thoughtworks.xstream.XStream;

import cc.fantasy.exception.ErrorCode;
import cc.fantasy.exception.FantasyException;
import cc.fantasy.model.Franchise;
import cc.fantasy.model.League;
import cc.fantasy.model.ModelBase;
import cc.fantasy.model.Player;
import cc.fantasy.model.Position;
import cc.fantasy.model.Team;
import cc.fantasy.model.User;
import cc.fantasy.model.type.LeagueStatus;
import cc.fantasy.service.FranchiseSearchKey;
import cc.fantasy.service.IFantasyService;
import cc.fantasy.service.LeagueSearchKey;
import cc.fantasy.service.Search;
import cc.fantasy.service.TeamSearchKey;
import cc.fantasy.service.UserSearchKey;
import cc.fantasy.util.Comparators;

public class FantasyServiceXmlDB implements IFantasyService, Comparator {

    private Category log = Category.getInstance(getClass());
    
    private XStream userStream = new UserXmlStream();
    private XStream franchiseStream = new FranchiseXmlStream();
    private XStream leagueStream = new LeagueXmlStream();
    private XStream teamStream = new TeamXmlStream();
    
    private List<User> users;
    private List<Franchise> franchises;
    private List<League> leagues;
    private List<Team> teams;
    
    private File userDB;
    private File franchiseDB;
    private File leagueDB;
    private File teamDB;
    
    protected File getUserDB() {
        return userDB;
    }
    
    protected File getFranchiseDB() {
        return franchiseDB;
    }
    
    protected File getLeagueDB() {
    	return leagueDB;
    }
    
    protected File getTeamDB() {
        return teamDB;
    }
    
    private List load(InputStream input, XStream stream) throws Exception {
        try {
            return (List)stream.fromXML(input);
        } finally {
            try {
                input.close();
            } catch (Exception e) {}
        }
    }
    
    protected String getFileExtension() {
    	return "xml";
    }
    
    public FantasyServiceXmlDB(String dbDir) {
    	File dir = new File(dbDir);
    	if (dir.isFile())
    		throw new FantasyException(ErrorCode.INVALID_DIRECTORY, dbDir);
    	if (!dir.exists() && !dir.mkdirs())
    		throw new FantasyException(ErrorCode.CANNOT_CREATE_DIRECTORY, dbDir);
        this.userDB = new File(dbDir + File.separator + "users." + getFileExtension());
        try {
            users = load(getUserDBInputStream(), userStream);
            log.info("Loaded " + users.size() + " users from " + userDB);            
        } catch (Exception e) {
            //throw new FantasyException("Failed to load db [" + userDBString + "]", e);
            log.error("Failed to load db [" + userDB + "], exception : " + e.getMessage());
            users = new ArrayList();
        }
        
        this.franchiseDB = new File(dbDir + File.separator + "franchises." + getFileExtension());
        try {
            franchises = load(getFranchiseDBInputStream(), franchiseStream);
            log.info("Loaded " + franchises.size() + " franchises from " + franchiseDB);            
            Iterator<Franchise> it = franchises.iterator();
            while (it.hasNext()) {
                Franchise f = it.next();
                f.setPlayers(f.getPlayers());
                f.setPositions(f.getPositions());
            }
        } catch (Exception e) {
            log.error("Failed to load db [" + franchiseDB + "], exception : " + e.getMessage());
            franchises = new ArrayList();
        }
        this.leagueDB = new File(dbDir + File.separator + "leagues." + getFileExtension());
        try {
        	leagues = load(getLeagueDBInputStream(), leagueStream);
            log.info("Loaded " + leagues.size() + " leagues from " + leagueDB);            
        	Iterator<League> it = leagues.iterator();
        	while (it.hasNext()) {
        		League l = it.next();
        		l.setPositions(l.getPositions());
        	}        		
        } catch (Exception e) {
            log.error("Failed to load db [" + leagueDB + "], exception : " + e.getMessage());
            leagues = new ArrayList();
        }
        this.teamDB = new File(dbDir + File.separator + "teams." + getFileExtension());
        try {
            teams = load(getTeamDBInputStream(), teamStream);
            log.info("Loaded " + teams.size() + " teams from " + teamDB);
            Iterator<Team> it = teams.iterator();
            while (it.hasNext()) {
                Team t = it.next();
                t.setPlayers(t.getPlayers());
            }
        } catch (Exception e) {
            log.error("Failed to load db [" + teamDB + "], exception : " + e.getMessage());
            teams = new ArrayList();
        }
    }
    
    protected InputStream getUserDBInputStream() throws Exception {
        return new FileInputStream(userDB);
    }
    
    protected OutputStream getUserDBOutputStream() throws Exception {
        return new FileOutputStream(userDB);
    }
    
    protected InputStream getFranchiseDBInputStream() throws Exception {
        return new FileInputStream(franchiseDB);
    }
    
    protected OutputStream getFranchiseOutputStream() throws Exception {
        return new FileOutputStream(franchiseDB);
    }
    
    protected InputStream getLeagueDBInputStream() throws Exception {
        return new FileInputStream(leagueDB);
    }
    
    protected OutputStream getLeagueOutputStream() throws Exception {
        return new FileOutputStream(leagueDB);
    }
    
    protected InputStream getTeamDBInputStream() throws Exception {
        return new FileInputStream(teamDB);
    }
    
    protected OutputStream getTeamDBOutputStream() throws Exception {
        return new FileOutputStream(teamDB);
    }
    
    private <T extends ModelBase> void save(OutputStream output, XStream stream, List<T> list) throws Exception {
    	int id = 0;
    	Iterator<T> it = list.iterator();
    	while (it.hasNext()) {
    		it.next().setId(id++);
    	}    	
        try {
            String xml = stream.toXML(list);
            output.write(xml.getBytes());
        } finally {
            output.close();
        }
    }
    
    private List reduceList(List list, Search search, Comparator comp) {
        List resultList = list;
        if (search != null) {
            Object [] array = list.toArray(new Object[list.size()]);
            if (comp != null) {
                Arrays.sort(array, comp);
            }
            if (search.isDescending()) {
                int start = 0;
                int end = array.length-1;
                while (start < end) {
                    Object t = array[start];
                    array[start] = array[end];
                    array[end] = t;
                    start ++;
                    end --;
                }
            }
            resultList = new ArrayList();
            for (int i=0; i<search.getMax() && i+search.getOffset()<array.length; i++)
                resultList.add(array[i+search.getOffset()]);
            comp = null;
        }
        
        log.debug("Returning list length [" + resultList.size() + "]");
        
        return resultList;
    }
    
    ////////////////////////////////////////////////////////////////
    // USER MGMT
    ////////////////////////////////////////////////////////////////

    public User getUser(int id) {
        if (id < 0 || id >= users.size())
            throw new FantasyException(ErrorCode.INVALID_ID, "user [" + id + "]");
        User user = users.get(id);
        if (user.getId() != id)
            throw new AssertionError("Expected id [" + id + "] for User at index [" + id + "] instead found [" + user.getId() + "]");
        return user;
    }

    public static final int USERNAME_MIN_CHARS = 5;
    public static final int PASSWORD_MIN_CHARS = 4;
    
    public void addOrUpdateUser(User user) {
        
    	checkField(user.getUserName(), "userName");
    	checkField(user.getPassWord(), "passWord");
    	
        if (user.getUserName().length()<USERNAME_MIN_CHARS)
            throw new FantasyException(ErrorCode.USERNAME_FIELD_MIN_CHARS_NOT_MET, String.valueOf(USERNAME_MIN_CHARS));
        if (user.getPassWord().length()<PASSWORD_MIN_CHARS)
            throw new FantasyException(ErrorCode.PASSWORD_FIELD_MIN_CHARS_NOT_MET, String.valueOf(PASSWORD_MIN_CHARS));
        
        if (user.getId() == -1) {
            if (getUser(user.getUserName()) != null)
                throw new FantasyException(ErrorCode.USERNAME_ALREADY_IN_USE, user.getUserName());
            user.setId(users.size());
            new User().addToList(user, users);
        }
        
        try {
            this.save(getUserDBOutputStream(), userStream,users);
        } catch (Exception e) {
            log.error("Failed to write to [" + userDB + "], exception : " + e.getMessage());
        }
    }

    public User getUser(String userName, String passWord) {
        Iterator<User> it = users.iterator();
        while (it.hasNext()) {
            User user = it.next();
            if (user.getUserName().equals(userName)) {
                if (user.getPassWord().equals(passWord))
                    return user;
                throw new FantasyException(ErrorCode.PASSWORD_NOT_VALID_FOR_USER, user.getUserName());
            }
        }
        throw new FantasyException(ErrorCode.USERNAME_NOT_FOUND, userName);
    }
    
    public User getUser(String userName) {
        Iterator<User> it = users.iterator();
        while (it.hasNext()) {
            User user = it.next();
            if (user.getUserName().equals(userName)) {
                return user;
            }
        }
        return null;
    }

    public int getUserCount() {
        // TODO Auto-generated method stub
        return users.size();
    }

    enum Comparor {
        COMP_USER_LEAGUE_COUNT,
        COMP_USER_TEAM_COUNT,
        COMP_FRANCHISE_LEAGUE_COUNT,
        COMP_LEAGUE_FRANCHISE_NAME,
        COMP_LEAGUE_LEADER_NAME,
        COMP_LEAGUE_NUM_TEAMS,
        COMP_LEAGUE_STATUS,
        COMP_TEAM_LEAGUE_NAME,
        COMP_TEAM_FRANCHISE_NAME,
        COMP_TEAM_RANK,
        COMP_TEAM_LEAGUE_LEADER,
        COMP_TEAM_LEAGUE_STATUS,
        COMP_TEAM_LEAGUE_STATUS_DATE,
        COMP_TEAM_OWNER_NAME,
    };
    
    public int compare(Object o1, Object o2) {
        switch (comp) {
            case COMP_USER_LEAGUE_COUNT:
            	return getLeagues(((User)o1), null).size() - getLeagues(((User)o2), null).size();
            case COMP_USER_TEAM_COUNT:
                return getTeams(((User)o1), null).size() - getTeams(((User)o2), null).size();
            case COMP_FRANCHISE_LEAGUE_COUNT:
                return getLeaguesCount((Franchise)o1) - getLeaguesCount((Franchise)o2);
            case COMP_LEAGUE_FRANCHISE_NAME:
                return getFranchise(((League)o1).getFranchiseId()).getName().compareToIgnoreCase(getFranchise(((League)o2).getFranchiseId()).getName());
            case COMP_LEAGUE_LEADER_NAME:
                return Team.getName(getLeagueLeader((League)o1)).compareToIgnoreCase(Team.getName(getLeagueLeader((League)o2)));
            case COMP_LEAGUE_NUM_TEAMS:
                return getTeamsCount((League)o1) - getTeamsCount((League)o2);
            case COMP_LEAGUE_STATUS:
                return ((League)o1).getStatus().compareTo(((League)o2).getStatus());
            case COMP_TEAM_LEAGUE_NAME:
                return getLeague(((Team)o1).getLeagueId()).getName().compareToIgnoreCase(getLeague(((Team)o2).getLeagueId()).getName()); 
            case COMP_TEAM_FRANCHISE_NAME:
                return getFranchise(getLeague(((Team)o1).getLeagueId()).getFranchiseId()).getName().compareToIgnoreCase(getFranchise(getLeague(((Team)o2).getLeagueId()).getFranchiseId()).getName());
            case COMP_TEAM_RANK:
                return getTeamRank((Team)o1) - getTeamRank((Team)o2);
            case COMP_TEAM_LEAGUE_LEADER:
                return Team.getName(getLeagueLeader(((Team)o1).getLeagueId())).compareToIgnoreCase(Team.getName(getLeagueLeader(((Team)o2).getLeagueId())));
            case COMP_TEAM_LEAGUE_STATUS:
                return getLeague(((Team)o1).getLeagueId()).getStatus().compareTo(getLeague(((Team)o2).getLeagueId()).getStatus());  
            case COMP_TEAM_LEAGUE_STATUS_DATE:
                return getLeague(((Team)o1).getLeagueId()).getStatusDate().compareTo(getLeague(((Team)o2).getLeagueId()).getStatusDate());
            case COMP_TEAM_OWNER_NAME:
                return getUser(((Team)o1).getUserId()).getUserName().compareToIgnoreCase(getUser(((Team)o2).getUserId()).getUserName());
            default:
                log.error("Unhandled key : " + comp); 
        }
        return 0;
    }
    
    private Comparor comp = null;
    
    private Comparator setComparor(Comparor comp) {
        this.comp = comp;
        return this;
    }
    
    public List<User> getUsers(Search<UserSearchKey> search) {
        Comparator comp = null;
        if (search != null && search.getKey() != null) {
            UserSearchKey key = search.getKey();
            switch (key) {
            case ID:
            case FIRSTNAME:
            case LASTNAME:
            case USERNAME:
            case ACTIVE:
            case ACCESS:
            case LASTLOGIN:
                comp = Comparators.getObjectFieldComparator(key.name());
                break;
            case NUM_LEAGUES:
                comp = setComparor(Comparor.COMP_USER_LEAGUE_COUNT);
                break;
            case NUM_TEAMS:
                comp = setComparor(Comparor.COMP_USER_TEAM_COUNT);
                break;
            default:
                log.error("Unhandled key : " + key); 
            }
        }
        return reduceList(users, search, comp); 
    }

    ////////////////////////////////////////////////////////////////
    // FRANCHISE MGMT
    ////////////////////////////////////////////////////////////////
    
    public List<Franchise> getFranchises(Search<FranchiseSearchKey> search) {
        Comparator comp = null;
        if (search != null) {
            FranchiseSearchKey key = search.getKey();
            if (key != null) {
                switch (key) {
                case ID:
                case NAME:
                case CATEGORY:
                case ACTIVE:
                    comp = Comparators.getObjectFieldComparator(key.name());
                    break;
                case NUM_LEAGUES:
                    comp = setComparor(Comparor.COMP_FRANCHISE_LEAGUE_COUNT);
                    break;
                default:
                    log.error("Unhandled key : " + key); 
                }
            }
        }
        return reduceList(franchises, search, comp);
    }

    private void saveFranchises() {
        try {
            // cleaup ids
            for (int f=0; f<franchises.size(); f++) {
                List<Position> positions = franchises.get(f).getPositions();
                for (int p=0; p<positions.size(); p++) {
                    positions.get(p).setId(p);                    
                }
                List<Player> players = franchises.get(f).getPlayers();
                for (int p=0; p<players.size(); p++) {
                    players.get(p).setId(p);
                }
            }

            save(getFranchiseOutputStream(), franchiseStream, franchises);

        } catch (Exception e) {
            e.printStackTrace();
            //throw new FantasyException("Failed to write to [" + franchiseDB + "]", e);
            log.error("Failed to write to [" + franchiseDB + "], exception : " +e.getMessage());
        }
    }

    private void checkField(Object field, String name) {
    	if (field == null || field.toString().length() == 0)
    		throw new FantasyException(ErrorCode.EMPTY_FIELD, name);
    }
    
    private void checkId(int value, String name) {
    	if (value < 0)
    		throw new FantasyException(ErrorCode.INVALID_ID, name + "[" + value + "]");
    }
    
    public void addOrUpdateFranchise(Franchise franchise) {
    	
    	checkField(franchise.getName(), "name");
    	checkField(franchise.getCategory(), "category");

    	new Franchise().addToList(franchise, franchises);
        saveFranchises();
    }

    public int getFranchiseCount() {
        return franchises.size();
    }

	public void deleteFranchise(Franchise f) {
	    if (!franchises.contains(f))
            throw new FantasyException(ErrorCode.FRANCHISE_NOT_FOUND, f.getName());
        franchises.remove(f);
        saveFranchises();
    }


    public Franchise getFranchise(int id) {
		if (id < 0 || id >= franchises.size())
			throw new FantasyException(ErrorCode.INVALID_ID, "franchise [" + id + "]");
		Franchise franchise = franchises.get(id);
		if (franchise.getId() != id)
			throw new AssertionError("Expected id [" + id + "] for Franchise at index [" + id + "] instead found [" + franchise.getId() + "]");
		return franchise;
	}

	public List<String> getAllFranchiseCategories() {
		HashSet<String> categories = new HashSet();
		Iterator<Franchise> it = franchises.iterator();
		while (it.hasNext()) {
			Franchise f = it.next();
			categories.add(f.getCategory());
		}
		return new ArrayList(categories);
	}

    ////////////////////////////////////////////////////////////////
    // LEAGUE MGMT
    ////////////////////////////////////////////////////////////////

    private void saveLeagues() {
        try {
            save(getLeagueOutputStream(), leagueStream, leagues);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Failed to write to [" + leagueDB + "], exception : " +e.getMessage());
        }
    }

    public void addOrUpdateLeague(League league) {
    	
    	checkField(league.getName(), "name");
    	checkField(league.getCreated(), "created date");
    	checkField(league.getDraft(), "draft date");
    	checkField(league.getEnding(), "end date");
    	checkId(league.getFranchiseId(), "franchise id");
    	checkId(league.getUserId(), "user id");

    	new League().addToList(league, leagues);
    	saveLeagues();
	}

    private Comparator getLeagueSearchKeyComparator(Search<LeagueSearchKey> search) {
        Comparator comp = null;
        if (search != null && search.getKey() != null) {
            LeagueSearchKey key = search.getKey();
            switch (key) {
            case ID:
            case NAME:
            case CREATEDDATE:
            case DRAFTDATE:
            case ENDDATE:
            case UPDATEDDATE:
            case MAX_PLAYERS:
                comp = Comparators.getObjectFieldComparator(key.name());
                break;
            case FRANCHISE_NAME:
                comp = setComparor(Comparor.COMP_LEAGUE_FRANCHISE_NAME);
                break;
            case LEADER_NAME:
                comp = setComparor(Comparor.COMP_LEAGUE_LEADER_NAME);
                break;
            case NUM_TEAMS:
                comp = setComparor(Comparor.COMP_LEAGUE_NUM_TEAMS);
                break;
            case STATUS:
                comp = setComparor(Comparor.COMP_LEAGUE_STATUS);
                break;
            default:
                log.error("Unhandled key : " + key); 
            }
        }
        return comp;
    }
    
	public int getLeaguesCount(User user) {
		return getLeagues(user, null).size();
	}

	public List<League> getLeagues(Search<LeagueSearchKey> search) {
        return reduceList(leagues, search, getLeagueSearchKeyComparator(search));
    }

    public List<League> getLeagues(User user, Search<LeagueSearchKey> search) {
		List<League> list = leagues;
		if (user != null) {
			list = new ArrayList();
			Iterator<League> it = leagues.iterator();
			while (it.hasNext()) {
				League l = it.next();
				if (l.getUserId() == user.getId())
					list.add(l);
			}
		}
		return reduceList(list, search, getLeagueSearchKeyComparator(search));
 	}

	public List<League> getLeagues(Franchise franchise, Search<LeagueSearchKey> search) {
		List<League> list = new ArrayList();
		Iterator<League> it = leagues.iterator();
		while (it.hasNext()) {
			League league = it.next();
			if (league.getFranchiseId() == franchise.getId()) {
				list.add(league);
			}
		}
		return reduceList(list, search, getLeagueSearchKeyComparator(search));
	}

	public List<League> getLeaguesByStatus(LeagueStatus status, Search<LeagueSearchKey> search) {
        List<League> result = new ArrayList();
        Iterator<League> it = leagues.iterator();
        while (it.hasNext()) {
            League league = it.next();
            if (league.getStatus() == status)
                result.add(league);
        }
        return reduceList(result, search, getLeagueSearchKeyComparator(search));
    }

    public int getLeaguesCount(Franchise franchise) {
		return getLeagues(franchise, null).size();
	}

	
	public void addOrUpdateLeagues(List<League> leagues) {
		this.leagues = new League().replaceList(this.leagues, leagues);
		saveLeagues();
	}

	public League getLeague(int id) {
		if (id < 0 || id >= leagues.size())
			throw new FantasyException(ErrorCode.INVALID_ID, "League [" + id + "]");		
		League league = leagues.get(id);
		if (league.getId() != id)
			throw new AssertionError("Invalid League, extected id '" + id + "' found '" + league + "'");
		return league;
	}

    ////////////////////////////////////////////////////////////////
    // TEAM MGMT
    ////////////////////////////////////////////////////////////////

    private Comparator getTeamSearchKeyComparator(Search<TeamSearchKey> search) {
        Comparator comp = null;
        if (search != null && search.getKey() != null) {
            TeamSearchKey key = search.getKey();
            switch (key) {
            case NAME:
            case POINTS:
                comp = Comparators.getObjectFieldComparator(key.name());
                break;
            case LEAGUE_NAME:
                comp = setComparor(Comparor.COMP_TEAM_LEAGUE_NAME);
                break;
            case FRANCHISE_NAME:
                comp = setComparor(Comparor.COMP_TEAM_FRANCHISE_NAME);
                break;
            case LEAGUE_STATUS:
                comp = setComparor(Comparor.COMP_TEAM_LEAGUE_STATUS);
                break;
            case LEAGUE_STATUS_DATE:
                comp = setComparor(Comparor.COMP_TEAM_LEAGUE_STATUS_DATE);
                break;
            case OWNER_NAME:
                comp = setComparor(Comparor.COMP_TEAM_OWNER_NAME);
                break;
            case RANK:
                comp = setComparor(Comparor.COMP_TEAM_RANK);
                break;
            case LEADER:
                comp = setComparor(Comparor.COMP_TEAM_LEAGUE_LEADER);
                break;
            default:
                log.error("Unhandled key : " + key); 
           }
        }
        return comp;
    }
    
	public List<Team> getTeams(User user, Search<TeamSearchKey> search) {
		List<Team> teams = this.teams;
        if (user != null) {
            teams = new ArrayList();
            Iterator<Team> it = this.teams.iterator();
            while (it.hasNext()) {
                Team team = it.next();
                if (team.getUserId() == user.getId())
                    teams.add(team);
            }
        }
        Comparator comp = getTeamSearchKeyComparator(search);
		return reduceList(teams, search, comp);
	}

    private void saveTeams() {
        try {
            save(getTeamDBOutputStream(), teamStream, teams);
        } catch (Exception e) {
            log.error("Failed to save teams : " + e.getMessage());
        }
    }
    
    public void addOrUpdateTeam(Team team) {
        
        checkField(team.getName(), "name");
        checkId(team.getLeagueId(), "leagueId");
        new Team().addToList(team, teams);
        team.setId(team.find(teams));
        saveTeams();
    }

    public void addOrUpdateTeams(List<Team> teams) {
    	this.teams = new Team().replaceList(this.teams, teams);
        saveTeams();
    }

    public Team getLeagueLeader(League league) {
        return getLeagueLeader(league.getId());
    }

    public Team getTeam(int id) {
        if (id < 0 || id >= teams.size())
            throw new FantasyException(ErrorCode.TEAM_NOT_FOUND, String.valueOf(id));
        Team team = teams.get(id);
        if (team.getId() != id)
            throw new AssertionError("Id not consistent");
        return team;
    }

    public int getTeamRank(Team team) {
        List<Team> teams = getTeamsByLeague(team.getLeagueId(), null);
        Team [] array = teams.toArray(new Team[teams.size()]);
        Arrays.sort(array, Comparators.getTeamRankComparator());
        return Arrays.binarySearch(array, team, Comparators.getTeamRankComparator());
    }

    public List<Team> getTeamsByLeague(int leagueId, Search<TeamSearchKey> search) {
        ArrayList result = new ArrayList();
        Iterator<Team> it = teams.iterator();
        while (it.hasNext()) {
            Team team = it.next();
            if (team.getLeagueId() == leagueId)
                result.add(team);
        }
        return reduceList(result, search, getTeamSearchKeyComparator(search));
    }

    public int getTeamsCount(League league) {
        if (league == null)
            return teams.size();
        return getTeamsByLeague(league.getId(), null).size();
    }

    public int getTeamsCount(User user) {
        if (user == null)
            return teams.size();
        return getTeams(user, null).size();
    }

    public Team getLeagueLeader(int leagueId) {
        Team leader = null;
        Iterator<Team> it = getTeamsByLeague(leagueId, null).iterator();
        while (it.hasNext()) {
            Team t = it.next();            
            if (leader == null || leader.getPoints() < t.getPoints())
                leader = t;         
        }
        return leader;    }


	
}
