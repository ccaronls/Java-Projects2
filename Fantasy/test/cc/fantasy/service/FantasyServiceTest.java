package cc.fantasy.service;

import java.util.Date;

import org.apache.log4j.Category;

import cc.fantasy.exception.FantasyException;
import cc.fantasy.model.Franchise;
import cc.fantasy.model.Player;
import cc.fantasy.model.Position;
import cc.fantasy.model.User;
import cc.fantasy.model.type.UserAccess;
import junit.framework.TestCase;

public abstract class FantasyServiceTest extends TestCase {

    Category log = Category.getInstance(getClass());
	
	protected abstract IFantasyService getService();
	
	public void testUsers() throws Exception {
		IFantasyService service = getService();
		User user = null;
		try {
			user = service.getUser(1);
		} catch (FantasyException e) {
			log.info("Creating new user");
			user = new User();
			user.setFirstName("a");
			user.setLastName("b");
			user.setUserName("abcde");
			user.setPassWord("1234");
            user.setAccess(UserAccess.ADMIN);
            user.setLastLogin(new Date());
            user.setActive(true);
		}
		service.addOrUpdateUser(user);
		assertTrue(service.getUserCount()==1);
		assertTrue(user.getId() == 1);
        assertTrue(user.hasAdminAccess());
		user = service.getUser(user.getId());
		assertEquals(user.getUserName(), "abcde");
		log.info("Users:\n" + service.getUsers(null));
        
        user = getService().getUser("abcde", "1234");
        assertEquals(user.getUserName(), "abcde");
       
	}
	
	public void testFranchise() throws Exception {
		IFantasyService service = getService();
		
		Franchise franchise = null;
		try {
			franchise = service.getFranchise(1);
		} catch (FantasyException e) {
			franchise = new Franchise();
			franchise.setName("test");	
            Player player = new Player();
            player.setFirstName("Adam");
            player.setLastName("West");
            franchise.addPlayer(player);
            
            Position position = new Position();
            position.setName("QB");
            position.setLongName("Quarter Back");
            franchise.addPosition(position);
            
            position = new Position();
            position.setName("RB");
            position.setLongName("Running Back");
            franchise.addPosition(position);
		}
		service.addOrUpdateFranchise(franchise);
		assertTrue(service.getFranchiseCount() == 1);
        log.info("Franchises:\n" + service.getFranchises(null));

        franchise = service.getFranchise(1);
        assertTrue(franchise.getPlayers().size() == 1);
        assertTrue(franchise.getPositions().size() == 2);
        log.info("Franchises:\n" + service.getFranchises(null));
        
        franchise.getPositions().remove(0);
		service.addOrUpdateFranchise(franchise);
		assertTrue(franchise.getPositions().size() == 1);
	}
	
	public void x_testLeagues() throws Exception {
		IFantasyService service = getService();
		
	}
	
}
