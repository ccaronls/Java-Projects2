package cc.fantasy.update;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Category;

import cc.fantasy.model.Franchise;
import cc.fantasy.model.League;
import cc.fantasy.model.LeaguePosition;
import cc.fantasy.model.Player;
import cc.fantasy.model.Position;
import cc.fantasy.model.Team;
import cc.fantasy.model.TeamPlayer;
import cc.fantasy.service.IFantasyService;
import cc.fantasy.util.Comparators;

public class DraftUpdate {

	private Category log = Category.getInstance(getClass());
	private Random random;
    private IFantasyService service;
	
	// For JUnit tests
	DraftUpdate(IFantasyService service, long seed) {
		random = new Random(seed);
        this.service = service;
	}
	
    public DraftUpdate(IFantasyService service) {
        this(service, System.currentTimeMillis());
    }
    
    
	void checkTeamsReadiness(Franchise franchise, League league, List<Team> teams) {
		Iterator<Team> tit = teams.iterator();
		while (tit.hasNext()) {
			Team t = tit.next();
			log.info("Checking team " + t.getName() + " ...");
			Iterator<LeaguePosition> lit = league.getPositions().iterator();
			while (lit.hasNext()) {
				LeaguePosition p = lit.next();
				if (p.getNum() > 0) {
					if (!franchise.isTeamPositionComplete(t, p.getPosition())) {
						Position pos = franchise.getPosition(p.getPosition());
						log.info(pos.getName() + " is incomplete.  Populating remaining slots with default players");
						franchise.populateTeamPlayers(league, t);
						List<TeamPlayer> list = t.getPlayersForPosition(p.getPosition());
						for (int i=0; i<list.size(); i++) {
							TeamPlayer tp = list.get(i);
							Player player = franchise.getPlayer(tp.getPlayerId());
							tp.setRank(i+1);
							log.info("Assigning rank [" + tp.getRank() + "] to player '" + player.getFirstName() + " " + player.getLastName() + "'");
						}
					}
				}
			}
		}
	}
	
	public void runDraft(Franchise franchise, League league) {
		
        List<Team> teamsList = service.getTeamsByLeague(league.getId(), null);
		checkTeamsReadiness(franchise, league, teamsList);
		
        Date oldDate = league.getDraft();
		league.setDraft(new Date()); // draft is now closed
		HashSet<Integer> used = new HashSet();
		List<TeamPlayer> [] keepers = new List[teamsList.size()];
		for (int i=0; i<teamsList.size(); i++)
			keepers[i] = new ArrayList();
		
        try {
    		Iterator<LeaguePosition> it = league.getPositions().iterator();
    		while (it.hasNext()) {
    			LeaguePosition position = it.next();
    			
    			Position pos = franchise.getPosition(position.getPosition());
    			
    			log.info("Running draft for position " + pos.getLongName() + " (" + pos.getName() + ")");
    			
    			// Should I shuffle the pick order for each position or just once at the beginning?
    			log.info("Shuffling the pick order ...");
    			Team [] teams = teamsList.toArray(new Team[teamsList.size()]);
    			shuffle(teams);
    			
    			for (int n=0; n<position.getNum(); n++) {
    			
	    			for (int i=0; i<teams.length; i++) {
	    				
	    				log.info("Round [" + (i+1) + "] is team " + teams[i].getName());
	    				
	    				List<TeamPlayer> tList = teams[i].getPlayersForPosition(pos.getName());
	    				TeamPlayer [] players = tList.toArray(new TeamPlayer[tList.size()]);
	    				Arrays.sort(players, Comparators.getTeamPlayerRankComparator());
	    				
	    				int assignedPlayer = -1;
	    				for (int ii=0; ii<players.length; ii++) {
	    					if (!used.contains(players[ii].getPlayerId())) {
	    						assignedPlayer = ii;
	    						break;
	    					}
	    				}
	    				
	    				if (assignedPlayer == -1)
	    					throw new RuntimeException("Failed to find an unused player to fill position : " + position + "\n, this prob means there are too many teams and not enough players");
	    				
	    				Player p = franchise.getPlayer(players[assignedPlayer].getPlayerId());				
	    				log.info("Assigning player '" + p.getFirstName() + " " + p.getLastName() + "'");
	    				
	    				TeamPlayer keeper = players[assignedPlayer];
	    				keepers[i].add(keeper);
	    				used.add(keeper.getPlayerId());
	    			}
    			}
    		}
    		
    		// assign the keepers
    		for (int i=0; i<teamsList.size(); i++) {
    			Team team = teamsList.get(i);
    			team.getPlayers().clear();
    			team.setPlayers(keepers[i]);
    		}
    		
        } catch (RuntimeException e) {
            // restore the draft date
            league.setDraft(oldDate);
            throw e;
        }
	}
	
	void shuffle(Object [] array) {
		for (int i=0; i<10000; i++) {
			int i0 = Math.abs(random.nextInt()) % array.length;
			int i1 = Math.abs(random.nextInt()) % array.length;
			
			Object t = array[i0];
			array[i0] = array[i1];
			array[i1] = t;
		}
	}
	
}
