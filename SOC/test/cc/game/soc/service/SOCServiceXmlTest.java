package cc.game.soc.service;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import cc.game.soc.base.SOCTestBase;
import cc.game.soc.model.Game;
import cc.game.soc.model.GameState;
import cc.game.soc.model.User;

public class SOCServiceXmlTest extends SOCTestBase {

	Category log = Logger.getLogger(getClass().getSimpleName());
	
	public void test() {
		
		log.info("starting " + log.getName());
		
		User user = service.getUser("admin");
		assertNotNull(user);
		
		System.out.println(user);
		
		Game game = service.getGame(0);
		if (game == null) {
			game = new Game();
			game.setUser(user);
			game.setState(GameState.JOINING);
			service.createOrUpdateGame(game);
			game = service.getGame(0);
		}

		assertNotNull(game);
		System.out.println(game);
	}
	
	
}
