package cc.fantasy.update;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cc.fantasy.model.Franchise;
import cc.fantasy.model.League;
import cc.fantasy.model.Position;
import cc.fantasy.model.Team;
import cc.fantasy.service.FantasyContext;
import cc.fantasy.service.IFantasyService;
import junit.framework.TestCase;

public class FranchiseUpdateTest extends TestCase {

    private IFantasyService getService() {
        return FantasyContext.getService();
    }
    
    
	public void test() throws Exception {

		Map<String, String> positions = new HashMap();
		
		positions.put("QB", "testdata/QUARTERBACK.xls");
		positions.put("RB", "testdata/RUNNING_BACK.xls");
		
		Franchise franchise = new Franchise();
		
		int numPlayers = 0;
		int numPositions = 0;
		
		Iterator<String> it = positions.keySet().iterator();
		while (it.hasNext()) {
			String key = it.next();
			String fileName = positions.get(key);
			Position position = new Position();
			position.setName(key);
			franchise.addPosition(position);
			position.setId(numPositions++);
			position.setPlayerColumn("Player");
			position.setSpreadSheetFile(fileName);
			
			new FranchiseUpdate(getService()).importData(franchise, position, new ArrayList());
		}
		
		for (int i=0; i<franchise.getPlayers().size(); i++) {
			franchise.getPlayers().get(i).setId(i);
		}
		
		numPlayers = franchise.getPlayers().size();

		// see what happens with leagues
		
		League league = new League();
		league.setName("testLeague");
		league.setFranchiseId(franchise.getId());
		franchise.populateLeaguePositions(league, 1, 1);
		
		Team team = new Team();
		team.setName("testTeam");
		franchise.populateTeamPlayers(league, team);

		List<League> leagues = new ArrayList();
		leagues.add(league);
		
		// update again and make sure the positions and players count doesnt change
		it = positions.keySet().iterator();
		FranchiseUpdate update = new FranchiseUpdate(getService());
		while (it.hasNext()) {
			String key = it.next();
			Position position = franchise.getPosition(key);			
			update.importData(franchise, position, leagues);
		}
		update.updateTeamPoints();
		
		assertEquals(numPlayers, franchise.getPlayers().size());
		assertEquals(numPositions, franchise.getPositions().size());
		
		int teamPoints = team.getPoints();
		assertTrue(teamPoints > 0);

		it = positions.keySet().iterator();
		update = new FranchiseUpdate(getService());
		while (it.hasNext()) {
			String key = it.next();
			Position position = franchise.getPosition(key);
			
			new FranchiseUpdate(getService()).importData(franchise, position, leagues);
		}
		update.updateTeamPoints();
		assertEquals(teamPoints, team.getPoints());
		
		System.out.println("Franchise = \n" + franchise);
		
	}
}
