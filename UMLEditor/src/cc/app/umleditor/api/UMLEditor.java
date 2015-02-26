package cc.app.umleditor.api;

import java.util.ArrayList;
import java.util.Collection;

import cc.lib.game.AGraphics;

public class UMLEditor {

	Collection<UMLBubble> bubbles = new ArrayList<UMLBubble>();
	
	public void load(String fileName) throws Exception {
		
	}
	
	public void save(String fileName) throws Exception {
		
	}
	
	public void draw(AGraphics g) {
		
	}

	public enum Event {
		EVENT_MOVE_RIGHT,
		EVENT_MOVE_UP,
		EVENT_MOVE_LEFT,
		EVENT_MOVE_DOWN,
		
		EVENT_TOGGLE_SHAPE,
	}
	
	public boolean fireEvent(Event ev) {
		
		return false;
	}

	public UMLBubble getHighlightedBubble() {
		
		return null;
	}

	public void highlightAt(int mouseX, int mouseY) {
		// TODO Auto-generated method stub
		
	}
}
