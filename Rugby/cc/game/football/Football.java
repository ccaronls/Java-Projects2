package cc.game.football;

import java.io.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import cc.lib.game.AGraphics;
import cc.lib.game.Justify;
import cc.lib.game.Utils;
import cc.lib.swing.AWTGraphics;
import cc.lib.swing.AWTFrame;
import cc.lib.swing.AWTKeyboardAnimationApplet;

public class Football extends AWTKeyboardAnimationApplet implements ActionListener {

    public static void main(String[] args) {
        Utils.DEBUG_ENABLED = true;
        AWTFrame frame = new AWTFrame("Football DEBUG");
        AWTKeyboardAnimationApplet app = new Football();
        frame.add(app);
        frame.centerToScreen(600, 400);
        app.init();
        app.start();
        app.focusGained(null);
    }

    
	boolean isMouseInside(int x, int y, int w, int h) {
		int mx = getMouseX();
		int my = getMouseY();
		return mx>x && mx<x+w && my>y && my<y+h;
	}
	
	boolean drawButton(AGraphics g, int x, int y, int w, int h, String text) {
		boolean inside = isMouseInside(x, y, w, h);
		if (inside)
			g.setColor(g.CYAN);
		else
			g.setColor(g.BLUE);
		g.drawRect(x, y, w, h);
		g.setColor(g.YELLOW);
		g.drawString(text, x, y+h);
		return inside;
	}
	
	void drawChooseSide(AGraphics g) {
		int cx = this.getScreenWidth()/2;
		int cy = this.getScreenHeight()/2;
		int buttonHeight = 20;		
		int buttonWidth = getScreenWidth()/6;
		int x = cx - buttonWidth / 2;
		int y = cy - buttonHeight + 10;
		if (drawButton(g, x, y, buttonWidth, buttonHeight, "OFFENSE") &&
				this.getMouseButtonClicked(0)) {
			playerOffense = true;
			state = STATE_CHOOSE_OFFENSE_FORMATION;
		}
		
		y += buttonHeight + 10;
		
		if (drawButton(g, x, y, buttonWidth, buttonHeight, "DEFENSE") &&
				this.getMouseButtonClicked(0)) {
			playerOffense = false;
			state = STATE_CHOOSE_DEFENSE_FORMATION;
		}
		
	}
	
	void drawPlayers(AGraphics g, Player [] players) {
		
	}
	
	Player [] loadFormation(File file) {
		try {
			ObjectInputStream input = new ObjectInputStream(
					new FileInputStream(file));
			String name = input.readUTF();
			int numPlayers = input.readInt();
			Player [] players = new Player[numPlayers];
			for (int i=0; i<numPlayers; i++) {
				players[i] = (Player)input.readObject();
			}
			input.close();
			return players;
		} catch (Exception e) {
			setError(e);
			return null;
		}
	}
	
	File lastFormation = null;
	Player [] formationPlayers;
	
	void drawFormation(AGraphics g, File formation) {
		if (formation != lastFormation) {
			formationPlayers = loadFormation(formation);
		}
		drawPlayers(g, formationPlayers);
	}
	
	File drawChooseFormation(AGraphics g, File [] formations, int newFormationState) {
		int buttonHeight = 20;		
		int buttonWidth = getScreenWidth()/6;
		int x = 5;
		int y = 5;
		for (int i=0; i<formations.length; i++) {
			if (drawButton(g, x, y, buttonWidth, buttonHeight, formations[i].getName())) {
				if (this.getMouseButtonClicked(0)) {
					return formations[i];
				} else {
					drawFormation(g, formations[i]);
				}
			}
			
			y += buttonHeight + 5;
			if (y + buttonHeight + 5 > this.getScreenHeight()) {
				x += buttonWidth + 5;
				y = 5;
			}
		}
		if (drawButton(g, x, y, buttonWidth, buttonHeight, "NEW")
				&& this.getMouseButtonClicked(0)) {
			state = newFormationState;
		}
		return null;
	}

	void drawChooseOffenseFormation(AGraphics g) {
		File [] formations = new File("offense_formations").listFiles();
		if (formations.length == 0) {
			state = STATE_CREATE_NEW_OFFENSE_FORMATION;
		} else {
			File formation;
			if (null != (formation = drawChooseFormation(g, formations, STATE_CREATE_NEW_OFFENSE_FORMATION))) {
				offense = loadFormation(formation);
				state = playerOffense ? STATE_CHOOSE_DEFENSE_FORMATION : STATE_SNAP_THE_BALL;
			}
		}
	}
	
	void drawChooseDefenseFormation(AGraphics g) {
		File [] formations = new File("defense_formations").listFiles();
		if (formations.length == 0) {
			state = STATE_CREATE_NEW_DEFENSE_FORMATION;
		} else {
			File formation;
			if (null != (formation = drawChooseFormation(g, formations, STATE_CREATE_NEW_DEFENSE_FORMATION))) {
				offense = loadFormation(formation);
				state = !playerOffense ? STATE_CHOOSE_OFFENSE_FORMATION : STATE_SNAP_THE_BALL;
			}
		}
	}

	void drawPlayer(AGraphics g, Player p) {
		int r = 10;
		int x = Math.round(bsx + p.px);
		int y = Math.round(bsy + p.py);
		if (p.position.offense) {
			g.drawLine(x-r, y-r, x+r, y+r);
			g.drawLine(x-r, y+r, x+r, y-r);
		} else {
			g.drawOval(x-r, y-r, r*2, r*2);
		}		
	}
	
	Player [] initDefaultOffenseFormation() {
		Player [] players = new Player[11];
		players[0] = new Player(0, 10, Position.POS_OFF_CENTER);
		players[1] = new Player(20, 10, Position.POS_OFF_GUARD);
		players[2] = new Player(-20, 10, Position.POS_OFF_GUARD);
		players[3] = new Player(40, 10, Position.POS_OFF_TACKLE);
		players[4] = new Player(-40, 10, Position.POS_OFF_TACKLE);
		players[5] = new Player(60, 10, Position.POS_OFF_TE);
		players[6] = new Player(-60, 10, Position.POS_OFF_TE);
		players[7] = new Player(80, 10, Position.POS_OFF_WR);
		players[8] = new Player(-80, 10, Position.POS_OFF_WR);
		players[9] = new Player(0, 30, Position.POS_OFF_QB);
		players[10] = new Player(0, 50, Position.POS_OFF_RB);
		return players;		        
	}
	
	void drawCreateNewOffenseFormation(AGraphics g) {
		int cx = this.getScreenWidth()/2;
		int cy = this.getScreenHeight()/2;
		
		if (formationPlayers == null)
		    formationPlayers = this.initDefaultOffenseFormation();
		
		Player picked = null;
		for (Player p : formationPlayers) {
			int x = Math.round(p.px) + cx;
			int y = Math.round(p.py) + cy;
			int r = 10;
			if (isMouseInside(x-r, y-r, r*2, r*2)) {
				g.setColor(g.YELLOW);
				picked = p;
			} else {
				g.setColor(g.RED);
			}
			g.drawLine(x-r, y-r, x+r, y+r);
			g.drawLine(x-r, y+r, x+r, y-r);
			g.setColor(g.WHITE);
			g.drawJustifiedString(x, y, Justify.CENTER, Justify.CENTER, p.position.id);
		}
		
		if (picked != null) {
			if (this.getMouseButtonPressed(0)) {
				picked.px = this.getMouseX() - cx;
				picked.py = this.getMouseY() - cy;
			}
		}
	}

	void drawCreateNewDefenseFormation(AGraphics g) {
		
	}
	
	void drawError(AGraphics g) {
		int cx = this.getScreenWidth();
		int cy = this.getScreenHeight();
		int swid = (int)g.getTextWidth(errorString);
		int x = cx - swid / 2;
		int y = cy;
		g.drawString(errorString, x, y);
	}

	void setError(Exception e) {
		state = STATE_ERROR;
		errorString = e.getMessage();
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////
	
	
	
	@Override
	protected void drawFrame(AWTGraphics g) {
		// TODO Auto-generated method stub
		this.clearScreen();
		switch (state) {
		case STATE_ERROR:
			drawError(g);
			break;			
		case STATE_INIT:
			state = STATE_CHOOSE_SIDE;
			break;
		case STATE_CHOOSE_SIDE:
			drawChooseSide(g);
			break;
		case STATE_CHOOSE_OFFENSE_FORMATION:
			drawChooseOffenseFormation(g);
			break;
		case STATE_CHOOSE_DEFENSE_FORMATION:
			drawChooseDefenseFormation(g);
			break;
		case STATE_CREATE_NEW_OFFENSE_FORMATION:
			drawCreateNewOffenseFormation(g);
			break;
		case STATE_CREATE_NEW_DEFENSE_FORMATION:
			drawCreateNewDefenseFormation(g);
			break;
		case STATE_SNAP_THE_BALL:
			break;
			
			/*
		case STATE_PRE_SNAP:
		case STATE_POST_SNAP:
		case STATE_END_OF_PLAY:
		*/			
		}
	}

	@Override
	protected void doInitialization() {
		// TODO Auto-generated method stub
		
	}


	@Override
	protected void onDimensionsChanged(AGraphics g, int width, int height) {
		// TODO Auto-generated method stub
		
	}


	protected void setDimension(int width, int height) {
		// TODO Auto-generated method stub
		
	}

	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub
		
	}	

	String errorString;
	
	final int STATE_ERROR = -1;
	final int STATE_INIT = 0;
	final int STATE_CHOOSE_SIDE = 1;
	final int STATE_CHOOSE_OFFENSE_FORMATION = 2;
	final int STATE_CHOOSE_DEFENSE_FORMATION = 3;
	final int STATE_CREATE_NEW_OFFENSE_FORMATION = 4;
	final int STATE_CREATE_NEW_DEFENSE_FORMATION = 5;
	final int STATE_SNAP_THE_BALL = 6;

	int state = STATE_INIT;
	boolean playerOffense; // flag to determine which side the player is on

	float bsx, bsy; // initial position of ball
	float bx, by; // position of the ball in absolute coords
	float bdx, bdy; // ball motion delta
	
	Player offense [];
	Player defense [];
	
	Player user; // what player the user is contolling at the moment
	
	int yardLine; // what yard line we are at
	int firstDownLine; // what line need to get to for first down
	int downs; // how many downs we have used
	
	void snapTheBall() {
		
	}
	
	void updateUserPresnap(Player p) {
		switch (p.position) {
		case POS_OFF_QB: break;
		case POS_OFF_CENTER:
			if (this.getMouseButtonClicked(0)) {
				snapTheBall();
			}
			break;
		case POS_OFF_RB: break;
		case POS_OFF_WR: break;
		case POS_OFF_TE: break;
		case POS_OFF_GUARD: break;
		case POS_OFF_TACKLE: break;
		
		// defensive positions
		case POS_DEF_END: break;
		case POS_DEF_TACKLE: break;
		case POS_DEF_NOSE: break;
		case POS_DEF_BACK: break;
		case POS_DEF_CORNER: break;
		case POS_DEF_SAFTY: break;

		}
		
	}
	
	void updateUserPostsnap(Player p) {
		switch (p.position) {
		case POS_OFF_QB: break;
		case POS_OFF_CENTER: break;
		case POS_OFF_RB: break;
		case POS_OFF_WR: break;
		case POS_OFF_TE: break;
		case POS_OFF_GUARD: break;
		case POS_OFF_TACKLE: break;
		
		// defensive positions
		case POS_DEF_END: break;
		case POS_DEF_TACKLE: break;
		case POS_DEF_NOSE: break;
		case POS_DEF_BACK: break;
		case POS_DEF_CORNER: break;
		case POS_DEF_SAFTY: break;

		}
	}
	
	void updateAIPreSnap(Player p)
	{
		switch (p.position) {
		case POS_OFF_QB:
			// here we can audible the play on some commands
			break;
		case POS_OFF_CENTER:
			// snap the ball on mouse button
			break;
		case POS_OFF_RB: break;
		case POS_OFF_WR: break;
		case POS_OFF_TE: break;
		case POS_OFF_GUARD: break;
		case POS_OFF_TACKLE: break;
		
		// defensive positions
		case POS_DEF_END: break;
		case POS_DEF_TACKLE: break;
		case POS_DEF_NOSE: break;
		case POS_DEF_BACK: break;
		case POS_DEF_CORNER: break;
		case POS_DEF_SAFTY: break;

		}
	}
	
	void updateAIPostSnap(Player p)
	{
		switch (p.position) {
		case POS_OFF_QB: break;
		case POS_OFF_CENTER: break;
		case POS_OFF_RB: break;
		case POS_OFF_WR: break;
		case POS_OFF_TE: break;
		case POS_OFF_GUARD: break;
		case POS_OFF_TACKLE: break;
		
		// defensive positions
		case POS_DEF_END: break;
		case POS_DEF_TACKLE: break;
		case POS_DEF_NOSE: break;
		case POS_DEF_BACK: break;
		case POS_DEF_CORNER: break;
		case POS_DEF_SAFTY: break;

		}
	}
	
	
}
