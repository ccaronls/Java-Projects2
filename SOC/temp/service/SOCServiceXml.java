package cc.game.soc.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;

import cc.game.soc.model.Game;
import cc.game.soc.model.GameState;
import cc.game.soc.model.User;

public class SOCServiceXml extends AbstractSingleValueConverter implements SOCService {

	private final static Category log = Logger.getLogger(SOCServiceXml.class);
	
	private XStream userStream;
	private XStream gameStream;
	
	private List<User> users = new ArrayList<User>();
	private List<Game> games = new ArrayList<Game>();
	
	private File usersFile;
	private File gamesFile;
	
	SOCServiceXml(String dbDirName) {
		userStream = new XStream();
		userStream.alias("users", List.class);
		userStream.alias("User", User.class);
		userStream.setMode(XStream.NO_REFERENCES);

		gameStream = new XStream();
		gameStream.alias("games", List.class);
		gameStream.alias("Game", Game.class);
		gameStream.omitField(Game.class, "soc");
		gameStream.registerConverter(this);
		gameStream.setMode(XStream.NO_REFERENCES);

		File dbDir = new File(dbDirName);
		dbDir.mkdirs();
		
		usersFile = new File(dbDir, "users.xml");
		gamesFile = new File(dbDir, "games.xml");
		
		users = loadXml(userStream, usersFile); 
		games = loadXml(gameStream, gamesFile);
		
		log.debug("loaded '" + users.size() + "' users");
		log.debug("loaded '" + games.size() + "' games");
	}
	
	@SuppressWarnings("unchecked")
	private <T> List<T> loadXml(XStream x, File file) {
		log.debug("loading xml from '" + file + "'");
		InputStream is = null;
		try {
			is = new FileInputStream(file);
			return (List<T>)x.fromXML(is);
		} catch (Exception e) {
			log.error(e);
			e.printStackTrace();
			return new ArrayList<T>();
		} finally {
			try { is.close(); } catch (Exception e) {}
		}
		
	}
	
	private <T> void saveXml(XStream x, List<T> list, File file) {
		OutputStream out = null;
		try {
			out = new FileOutputStream(file);
			x.toXML(list, out);
		} catch (Exception e) {
			log.error(e);
			e.printStackTrace();
		} finally {
			try { out.close(); } catch (Exception e) {}
		}
	}
	
	private HashMap<String, Comparator<?>> objectFieldComparators = new HashMap<String, Comparator<?>>();
	private HashMap<String, Comparator<?>> objectMethodComparators = new HashMap<String, Comparator<?>>();
	
	private Field findField(Object o, String field) {
		Field [] fields = o.getClass().getDeclaredFields();
		for (int i=0; i<fields.length; i++) {
			if (fields[i].getName().equalsIgnoreCase(field))
				return fields[i];
		}
		throw new RuntimeException("Cannot find field '" + field + "' for class " + o.getClass());
	}
	
	@SuppressWarnings({ "unchecked" })
	private Comparator getObjectFieldComparator(String _fieldName) {
		final String fieldName = _fieldName.toLowerCase();
		if (!objectFieldComparators.containsKey(fieldName)) {
			objectFieldComparators.put(fieldName, new Comparator() {
				public int compare(Object arg0, Object arg1) {					
					Field field = findField(arg0, fieldName);
					try {
						field.setAccessible(true);
						Comparable v0 = (Comparable)field.get(arg0);
						Comparable v1 = (Comparable)field.get(arg1);
						if (v0 == null && v1 == null)
							return 0;
						if (v1 == null)
							return -1;
						if (v0 == null)
							return 1;
						return v0.compareTo(v1);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			});
		}
		return objectFieldComparators.get(fieldName);
	}

	@SuppressWarnings({ "unchecked" })
	private Comparator getObjectMethodComparator(final String methodName) {
		if (!objectMethodComparators.containsKey(methodName)) {
			objectMethodComparators.put(methodName, new Comparator() {
				public int compare(Object arg0, Object arg1) {
					Class [] params = new Class[] { SOCService.class };
					Object [] args = new Object[] { SOCServiceXml.this };
					try {
						Method method = arg0.getClass().getMethod(methodName, params);
						Comparable v0 = (Comparable)method.invoke(arg0, args);
						Comparable v1 = (Comparable)method.invoke(arg1, args);
						if (v0 == null && v1 == null)
							return 0;
						if (v1 == null)
							return -1;
						if (v0 == null)
							return 1;
						return v0.compareTo(v1);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			});
		}
		return objectMethodComparators.get(methodName);
	}

	
	@SuppressWarnings({ "unchecked" })
	private <T> List<T> reduceList(List list, Search search) {
    	if (search == null)
    		return list;
    	// set the total
    	search.setTotal(list.size());
    	// now reduce
    	Object [] array = list.toArray(new Object[list.size()]);
    	if (search.getOrderByField() != null) {
    		Comparator comp = getObjectFieldComparator(search.getOrderByField());
    		Arrays.sort(array, comp);
    	} else if (search.getOrderByMethod() != null) {
    		Comparator comp = getObjectMethodComparator(search.getOrderByMethod());
    		Arrays.sort(array, comp);
    	}
    	if (search.isDescending()) {
    		// reverse order
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
    	List newList = new ArrayList();
    	for (int i=search.getOffset(); i<search.getMax() && i<array.length; i++)
    		newList.add(array[i]);
    	return newList;
    }
	
	@SuppressWarnings("unchecked")
    @Override
	public boolean canConvert(Class arg0) {
		return arg0.equals(User.class);
	}

	@Override
	public Object fromString(String arg0) {
		int id = Integer.parseInt(arg0);
		User user = getUser(id);
		return user;
	}

	@Override
	public String toString(Object obj) {
		return String.valueOf(((User)obj).getId());
	}

	@Override
	public List<User> listUsers(Search search) {
		return reduceList(users, search);
	}

	@Override
	public User getUser(int id) {
		Iterator<User> it = users.iterator();
		while (it.hasNext()) {
			User u = it.next();
			if (u.getId() == id)
				return u;
		}
		return null;
	}

	@Override
	public User getUser(String userName) {
		Iterator<User> it = users.iterator();
		while (it.hasNext()) {
			User u = it.next();
			if (u.getUserName().equals(userName))
				return u;
		}
		return null;
	}

	@Override
	public void createOrUpdateUser(User user) {
		if (user.getId() >= 0) {
			log.info("update user: " + user);
			User u = getUser(user.getUserName());
			if (u != null) {
				users.remove(u);
			}
		} else {
			log.info("create user: " + user);
			if (getUser(user.getUserName()) != null)
				throw new DataException("User already exists '" + user.getUserName() + "'");
			user.setId(users.size());
		}
		users.add(user);
		saveXml(userStream, users, usersFile);
		log.debug("create user SUCCESS");
	}

	@Override
	public List<Game> listGames(User user, Search search) {
		List<Game> list = games;
		if (user != null) {
			list = new ArrayList<Game>();
			Iterator<Game> it = games.iterator();
			while (it.hasNext()) {
				Game g = it.next();
				if (g.getUser().getId() == user.getId())list.add(g);
			}
		}
		return reduceList(list, search);
	}

	@Override
	public List<Game> listGamesByState(GameState state) {
		List<Game> result = new ArrayList<Game>();
		Iterator<Game> it = games.iterator();
		while (it.hasNext()) {
			Game g = it.next();
			if (g.getState() == state)
				result.add(g);
		}
		return result;
	}
	
	@Override
	public List<Game> listOpenGames(User user, Search search) {
		List<Game> result = new ArrayList<Game>();
		Iterator<Game> it = games.iterator();
		while (it.hasNext()) {
			Game g = it.next();
			if (g.getState() == GameState.JOINING && g.getMinRank() <= user.getRank())
				result.add(g);
		}
		return reduceList(result, search);
	}

	@Override
	public Game getGame(int id) {
		Iterator<Game> it = games.iterator();
		while (it.hasNext()) {
			Game g = it.next();
			if (g.getId() == id)
				return g;
		}
		return null;
	}

	@Override
	public void createOrUpdateGame(Game game) {
		if (game.getId() >= 0) {
			Game g = getGame(game.getId());
			games.remove(g);
		} else {
			game.setId(games.size());
		}
		games.add(game);
		saveXml(gameStream, games, gamesFile);
	}

	@Override
	public void deleteGame(int id) {
		Game g = getGame(id);
		if (g != null) {
			g.setId(-1);
			saveXml(gameStream, games, gamesFile);
		}
	}

	
	
}
