package cc.game.slot;

import java.util.ArrayList;

import cc.lib.game.AGraphics;
import cc.lib.game.Justify;

class Wheel {

	private ArrayList<String> cards = new ArrayList<String>();
	// this is a linear physics system, only need 3 vars
	private float position = 0;
	private float velocity = 0;
	// rate of deacceleration
	private float friction = 0.96f;
	// units of height per card
	private int   cardHeight = 25;
	
	private int   maxPosition = 0;
	
	void generate(String [] cards, int num) {
		for (int i=0; i<num; i++) {
			int index = i % cards.length;
			this.cards.add(cards[index]);
		}
		maxPosition = (cardHeight * this.cards.size()) - 1;
	}

	void spin(float dt) {
		position += velocity * dt;
		velocity *= friction;
		while (position < 0)
			position += maxPosition;
		while (position >= maxPosition)
			position -= maxPosition;		
	}
	
	void setVelocity(float vel) {
		velocity = vel;
	}
	
	boolean isStopped() {
		return velocity < 0.2f;
	}
	
	void draw(AGraphics g, int x, int y, int w, int h) {

		int start = Math.round(position);

		int index = start / cardHeight;
		int dy    = start % cardHeight; 
		
		int y0 = y + h + dy;
		
		while (y0 > y) {
			
			g.drawRect(x, y0 - cardHeight/2, w, cardHeight);
			
			//String txt = "(" + index + ") " + cards.get(index);
			String txt = cards.get(index);
			g.drawJustifiedString(x+w/2, y0, Justify.CENTER, Justify.CENTER, txt);
			
			y0 -= cardHeight;
			index = (index+1) % cards.size();
			
			//int max = cardHeight * cards.size();
		}
		
	}
	
	
	String getCenterCardAt() {
		
		int index = Math.round(position / cardHeight) % cards.size();
		return cards.get(index);
	}
}
