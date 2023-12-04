package cc.lib.game;

import java.util.ArrayList;

import cc.lib.reflector.Reflector;

public final class Figures extends Reflector<Figures> {
	
	static {
		addAllFields(Figures.class);
	}

	private ArrayList<Figure2> figures = new ArrayList<Figure2>();
	
	public void add(Figure2 figure) {
		figures.add(figure);
	}
	
	public Figure2 getByName(String name) {
		for (Figure2 f : figures) {
			if (f.getName().equals(name))
				return f;
		}
		return null;
	}
	
	public int getCount() {
		return figures.size();
	}
	
	public Figure2 get(int index) {
		return figures.get(index);
	}
}
