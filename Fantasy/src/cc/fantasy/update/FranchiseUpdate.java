package cc.fantasy.update;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Category;

import cc.fantasy.model.Franchise;
import cc.fantasy.model.League;
import cc.fantasy.model.LeaguePosition;
import cc.fantasy.model.LeagueStat;
import cc.fantasy.model.Player;
import cc.fantasy.model.Position;
import cc.fantasy.model.Stat;
import cc.fantasy.model.Team;
import cc.fantasy.model.TeamPlayer;
import cc.fantasy.service.IFantasyService;

public class FranchiseUpdate {

	Category log = Category.getInstance(getClass());
	
	private HashMap<Team, Integer> teamPoints = new HashMap();
    private IFantasyService service;
	
    public FranchiseUpdate(IFantasyService service) {
        this.service = service;
    }
    
	public void importData(Franchise franchise, Position position, List<League> leagues) throws IOException {

		File spreadSheet = new File(position.getSpreadSheetFile());
		String playerColumn = position.getPlayerColumn();
		List<String> omitColumns = position.getOmitStats();
		
		log.info("importing '" + position.getName() + "' from '" + spreadSheet + "'");
		
        BufferedReader input = null;
    	int playerCol = -1;
        try {
    		input = new BufferedReader(new FileReader(spreadSheet));
    		String line = input.readLine();
    		if (line == null)
    			throw new IOException("Empty file [" + spreadSheet + "]");
    		String [] header = line.split("[\t]");
            boolean [] skip = new boolean[header.length];
            
    		for (int i=0; i<header.length; i++) {
    			if (header[i].equals(playerColumn)) {
    				playerCol = i;
    				skip[i] = true;
    			} else if (omitColumns != null && omitColumns.contains(header[i])) {
    				skip[i] = true;
    			}
    		}
            
    		if (playerCol == -1)
    			throw new IOException("Failed to find column header '" + playerColumn + "'");
    		
    		log.debug("Player column index = " + playerCol);
    		
            // traverse the header and add stats that have not been explicitly omitted.
            for (int i=0; i<header.length; i++) {
                if (skip[i])
                    continue;
                String statName = header[i].trim();
                log.info("Adding stat '" + statName + "'");
                position.addStat(new Stat(statName));
            }
            
            // now parse all the player stats
            int numPlayersParsed = 0;
            while (true) {
            	line = input.readLine();
            	if (line == null)
            		break;
            	
            	String [] cells = line.split("[\t]");
            	if (cells == null || cells.length != header.length)
            		continue;
       			String firstName = parseFirstName(cells[playerCol]);
       			String lastName  = parseLastName(cells[playerCol]);
       			log.info("Parsing player '" + firstName + " " + lastName + "'");
       			Player player = franchise.getPlayer(firstName, lastName);
       			if (player == null) {
       				player = new Player();
       				player.setFirstName(firstName);
       				player.setLastName(lastName);
       				franchise.addPlayer(player);
       			}
       			player.addPosition(position.getName());
            	
       			if (leagues != null) {
	       			for (int i=0; i<cells.length; i++) {
	       				if (skip[i])
	       					continue;
	       				Matcher matcher = numberPattern.matcher(cells[i]);
	       				if (matcher.find()) {
	       					float statValue = Float.parseFloat(matcher.group());
	       					updateTeams(leagues, player, position, header[i].trim(), statValue);
	       				}
	       				
	       			}
       			}
            	numPlayersParsed ++;
            }
            
            log.info("Successfully parsed data for " + numPlayersParsed + " players");            
            
        } finally {
            try {
                input.close();
            } catch (Exception e) {}
        }
		
	}
	
	public void updateTeamPoints() {
		Iterator<Team> it = teamPoints.keySet().iterator();
		while (it.hasNext()) {
			Team t = it.next();
			int newPoints = teamPoints.get(t);
			log.debug("Setting team '" + t.getName() + "' points from " + t.getPoints() + " too " + newPoints);
			t.setPoints(newPoints);
		}
	}
	
	void updateTeams(List<League> leagues, Player player, Position position, String stat, float statValue) {
		Iterator<League> lit = leagues.iterator();
		while (lit.hasNext()) {
			League league = lit.next();
			LeaguePosition lPos = league.getPosition(position.getName());
			if (lPos == null)
				continue;
			LeagueStat lStat = lPos.getStat(stat);
			if (lStat == null || lStat.getMultiplier() == 0)
				continue;			
			Iterator<Team> tit = service.getTeamsByLeague(league.getId(), null).iterator();
			while (tit.hasNext()) {
				Team team = tit.next();
				TeamPlayer tPlayer = team.getPlayer(player.getId(), position.getName());
				if (tPlayer == null || tPlayer.getRank() == 0) 
					continue;
				int pointsToAdd = Math.round(statValue * lStat.getMultiplier());
				log.info("Team " + team.getName() + " gets " + pointsToAdd + " for stat " + lStat.getName());
				addPoints(team, pointsToAdd);
			}
		}
	}
	
	void addPoints(Team team, int points) {
		if (!teamPoints.containsKey(team)) {
			teamPoints.put(team, points);
		} else {
			int curPoints = teamPoints.get(team);
			curPoints += points;
			teamPoints.put(team, curPoints);
		}
	}
	
	String parseFirstName(String fullName) {
		int comma = fullName.indexOf(',');
		if (comma > 0) {
			return fullName.substring(comma+1).trim();
		}
		return fullName.split("[ ]")[0].trim();
	}
	
	String parseLastName(String fullName) {
		int comma = fullName.indexOf(',');
		if (comma > 0) {
			return fullName.substring(0, comma-1).trim();
		}
		String [] parts = fullName.split("[ ]");
		String name = parts[1].trim();
		for (int i=2; i<parts.length; i++) {
			name += " " + parts[i];
		}
		return name;
	}
	
	
	Pattern numberPattern = Pattern.compile("([1-9][0-9]*(\\.[0-9]+)?)|(0\\.[0-9]+)");
}
