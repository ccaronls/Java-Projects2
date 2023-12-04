package marcos.games.hexes.core;

import java.util.LinkedList;

import cc.lib.reflector.Reflector;

public class IdGenerator extends Reflector<IdGenerator> {

	static {
		addAllFields(IdGenerator.class);
	}
	
	private LinkedList<Integer> removed = new LinkedList<Integer>();
	private int counter = 1;
	
	int nextId() {
		if (removed.isEmpty())
			return counter++;
		return removed.remove();
	}
	
	void putBack(int id) {
		if (id > 0 && !removed.contains(id))
			removed.add(id);
	}
	
}
