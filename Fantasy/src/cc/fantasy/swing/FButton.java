package cc.fantasy.swing;

import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.swing.JButton;

import cc.fantasy.model.Franchise;
import cc.fantasy.model.League;
import cc.fantasy.model.LeaguePosition;
import cc.fantasy.model.Player;
import cc.fantasy.model.Position;
import cc.fantasy.model.Stat;
import cc.fantasy.model.Team;
import cc.fantasy.model.User;

class FButton extends JButton implements KeyListener {
    
    HashMap<Class<?>, Object> map;
    
    private HashMap<Class<?>, Object> getMap() {
        if (map == null)
            map = new HashMap();
        return map;
    }
    
    FButton set(Object o) {
        if (o != null)
            return set(o.getClass(), o);
        return this;
    }

    FButton set(Class clazz, Object o) {   
        getMap().put(clazz, o);
        return this;
    }

    <T> T get(Class<T> clazz) {
        return (T)getMap().get(clazz);
    }
    
    FButton(String txt) {
        this(txt, txt);
    }

    FButton(String txt, ActionListener listener) {
        this(txt, txt, listener);
    }
    
    FButton(String txt, String cmd) {
        this(txt, cmd, Fantasy.instance);
    }
    
    FButton(String txt, String cmd, ActionListener listener) {
        super(txt);
        addKeyListener(this);
        setActionCommand(cmd);
        addActionListener(listener);
        setFrame(FFrame.getTopFrame());
    }

    FButton(Command cmd) {
        this(cmd.getText(), cmd.name());
    }
    
    FButton(String text, Command cmd) {
    	this(text, cmd.name());
    }

    FButton setFrame(FFrame frame) {
        return set(FFrame.class, frame);
    }

    FFrame getFrame() {
        return get(FFrame.class);
    }

    FButton setId(int id) {
        return set(Integer.class, id);
    }

    int getId() {
        return get(Integer.class);
    }

    FButton setActive(boolean active) {
        return set(Boolean.class, active);
    }

    boolean getActive() {
    	return get(Boolean.class);
    }

    Franchise getFranchise() {
        return get(Franchise.class);
    }

    FButton setFranchise(Franchise franchise) {
        return set(franchise);
    }

    Player getPlayer() {
        return get(Player.class);
    }

    FButton setPlayer(Player player) {
        return set(player);
    }

    Position getPosition() {
        return get(Position.class);
    }

    FButton setPosition(Position position) {
        return set(position);
    }

    
    League getLeague() {
        return get(League.class);
    }

    FButton setLeague(League league) {
        return set(league);
    }
    
    Stat getStat() {
        return get(Stat.class);
    }
    
    FButton setStat(Stat stat) {
        return set(stat);
    }

    User getUser() {
        return get(User.class);
    }
    
    FButton setUser(User user) {
        return set(user);
    }

    Team getTeam() {
    	return get(Team.class);
    }
    
    FButton setTeam(Team team) {
        return set(team);
    }
    
    FButton setList(List list) {
        return set(List.class, list);
    }
    
    List getList() {
        return get(List.class);    	
    }
    
    public LeaguePosition getLeaguePosition() {
		return get(LeaguePosition.class);
	}

	public FButton setLeaguePosition(LeaguePosition leaguePosition) {
		return set(leaguePosition);
	}

    FButton setArray(Object [] array) {
        return set(Arrays.class, array);
    }
    
    Object [] getArray() {
        Object [] array = (Object[])getMap().get(Arrays.class);
        if (array == null)
            return new Object[0];
        return array;
    }
    
	public void keyPressed(KeyEvent key) {
        if (key.getKeyCode() == KeyEvent.VK_ENTER)
            this.doClick();
    }

    public void keyReleased(KeyEvent arg0) {}
    public void keyTyped(KeyEvent arg0) {}
    
    
};
