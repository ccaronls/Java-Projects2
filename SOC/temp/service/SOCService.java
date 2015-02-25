package cc.game.soc.service;

import java.util.List;

import cc.game.soc.model.Game;
import cc.game.soc.model.GameState;
import cc.game.soc.model.User;

/**
 * 
 * @author ccaron
 *
 * 
 */
public interface SOCService {

	// User management
	List<User>	listUsers(Search search);
	User		getUser(int id);
	User		getUser(String userName);
	
	void		createOrUpdateUser(User user);

	// Game Management
	List<Game>	listGames(User user, Search search);
	List<Game>	listGamesByState(GameState state);
	List<Game>	listOpenGames(User user, Search search);
	
	Game		getGame(int id);
	
	void		createOrUpdateGame(Game game);
	void		deleteGame(int id);
		
}
