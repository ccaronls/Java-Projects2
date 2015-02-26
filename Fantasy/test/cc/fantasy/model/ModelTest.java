package cc.fantasy.model;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

public class ModelTest extends TestCase {

	public void testFranchise() {
		
		Franchise f0 = new Franchise();
		Franchise f1 = f0;
		
		assertTrue(f0.equals(f1));
		f1 = new Franchise();
		assertTrue(f0.equals(f1));

		f0.setId(1);
		assertTrue(f0.equals(f1));
		
		f1.setId(1);
		assertTrue(f0.equals(f1));

		f1.setId(-1);
		f1.setName("franchise");
		assertFalse(f0.equals(f1));
		
		f0.setName("franchise");
		assertTrue(f0.equals(f1));		
		
		List<Franchise> list = new ArrayList();
		f0.addToList(f0, list);
		assertEquals(1, list.size());
		
		f0.addToList(f1, list);
		assertEquals(1, list.size());

		
	}
	
	public void testAddToList() {
		ArrayList<League> leagues = new ArrayList();
		League league = new League();
		league.setId(0);
		league.setName("A");
		league.addToList(league, leagues);
		assertEquals(1, leagues.size());
		league = new League();
		league.setName("A");
		league.addToList(league, leagues);
		assertEquals(1, leagues.size());
		league = new League();
		league.setName("B");
		league.addToList(league, leagues);
		assertEquals(2, leagues.size());
		
	}
	
	public void testReplaceList() {
		Team team = new Team();
		
		List<TeamPlayer> teams = new ArrayList();
		for (int i=0; i<10; i++) {
			TeamPlayer t = new TeamPlayer();
			t.setId(i);
			t.setPlayerId(i);
			team.addToList(t, teams);
			assertEquals(team, t.getTeam());
		}
		
		List<TeamPlayer> newTeams = new ArrayList();
		for (int i=0; i<5; i++) {
			TeamPlayer t = new TeamPlayer();
			t.setId(i);
			t.setPlayerId(i);
			newTeams.add(t);
		}
		
		teams = new Team().replaceList(teams, newTeams);
		assertEquals(5, teams.size());
		for (int i=0; i<teams.size(); i++)
			assertEquals(team, teams.get(i).getTeam());
		
		teams = new Team().replaceList(teams, new ArrayList());
		assertEquals(0, teams.size());
	}
	
	public void x_testLeagueEquals() {
		League l0 = new League();
		League l1 = l0;
		
		assertTrue(l0.equals(l1));

		l1 = new League();
		assertTrue(l0.equals(l1));

		l1.setName("x");
		assertFalse(l0.equals(l1));
		
		l0.setName("x");
		assertTrue(l0.equals(l1));

	}
	
}
