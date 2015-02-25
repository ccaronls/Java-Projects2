package marcos.game.stackmate.swing;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.*;

import javax.swing.*;

import marcos.game.stackmate.core.*;
import cc.lib.game.*;
import cc.lib.swing.*;

public class StackMateApplet extends KeyboardAnimationApplet implements ActionListener {

	public static void main(String [] args) {
        Utils.DEBUG_ENABLED = true;
        StackMatePlayerAI.ENABLE_LOGGING = true;
        Utils.setRandomSeed(0);
        EZFrame frame = new EZFrame("StackMate Debug Mode");
        KeyboardAnimationApplet app = new StackMateApplet(frame);
        frame.add(app);
        frame.centerToScreen(640, 480);
        app.init();
        app.start();
        app.setMillisecondsPerFrame(200);
    }   

	final Container frame;
	final JPanel buttons = new JPanel();
	final StackMate game = new StackMate() {

		@Override
		public void onChipMoved(int source, int target, Chip color) {
			synchronized (messages) {
    			messages.addFirst("Player " + color + " moved chip from " + source + " to " + target);
    			if (messages.size() > 6)
    				messages.removeLast();
			}
		}

		@Override
		public void onGameOver(StackMate.Chip winnerColor) {
			synchronized (messages) {
    			messages.addFirst("Player " + winnerColor + " has WON");
    			if (messages.size() > 6)
    				messages.removeLast();
			}
			showMainMenu();
		}
		
	};
	static StackMateApplet instance;
	final File restoreFile = new File("stackMate.txt");
	final LinkedList<String> messages = new LinkedList<String>();
	
    public StackMateApplet(final EZFrame frame) {
    	instance = this;
    	this.frame = frame;
    	frame.add(buttons, BorderLayout.SOUTH);
    	buttons.setLayout(new GridLayout(1,  0));
    	game.newGame();
    	if (restoreFile.exists()) {
    		showMainMenu();
    	} else {
        	showChooseColorMenu();
        	new Thread(new Runnable() {
        		public void run() {
        			try {
        				Thread.sleep(1000);
        			} catch (Exception e) {}
                	showHelpPopup();
        		}
        	}).start();
    	}
	}

    enum Cmd {
    	NEW_GAME,
    	NEW_CUSTOM_GAME,
    	RESTORE,
    	CHOOSE_RED,
    	CHOOSE_BLACK,
    	SHOW_MAIN_MENU,
    	SHOW_HELP,
    }
    
    void showHelpPopup() {
    	String welcome = "Welcome to StackMate!\n\n"
			       + "The Object of the game is to control all of the 6 stacks with your own color.\n"
    			   + "A stack has control when your color chip is on top.\n"
			       + "Players take turns moving their own color chip from the top of any stack to any other stack.\n"
			       + "The game is over when a player cannot move (there is no stack with their own color on top.)\n"
			       + "Stack must have at least one piece.\n";
    	game.newGame();
    	JOptionPane.showMessageDialog(frame, welcome, "Welcome!", JOptionPane.INFORMATION_MESSAGE);    	
    }
    
    void showMainMenu() {
    	buttons.removeAll();
    	buttons.add(newButton("New Game", Cmd.NEW_GAME));
    	buttons.add(newButton("New Custom Game", Cmd.NEW_CUSTOM_GAME));
    	JButton restore;
    	buttons.add(restore = newButton("Restore", Cmd.RESTORE));
    	if (!restoreFile.exists())
    		restore.setEnabled(false);
    	buttons.add(newButton("Rules", Cmd.SHOW_HELP));
		frame.validate();
		frame.repaint();
    }
	protected void showChooseColorMenu() {
		buttons.removeAll();
		buttons.add(newButton("Red", Cmd.CHOOSE_RED));
		buttons.add(newButton("Black", Cmd.CHOOSE_BLACK));
		buttons.add(newButton("Back", Cmd.SHOW_MAIN_MENU));
		frame.validate();
		frame.repaint();
	}

	private void showGameMenu() {
		buttons.removeAll();
		buttons.add(newButton("Quit", Cmd.SHOW_MAIN_MENU));
    	buttons.add(newButton("Rules", Cmd.SHOW_HELP));
		frame.validate();
		frame.repaint();
	}

	boolean running = false;
	
	void startGame() {
		if (!running) {
			synchronized (messages) {
				messages.clear();
			}
			showGameMenu();
			running = true;
			new Thread(new Runnable() {
				public void run() {
					try {
						Utils.println("Thread running");
						while (running && !game.isGameOver()) {
							//if (anims.size() == 0) {
								synchronized (game) {
									game.runGame();
									game.saveToFile(restoreFile);
								}
							//}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					running = false;
					Utils.println("Thread exiting");
				}
			}).start();
		}
	}
    private JButton newButton(String txt, Cmd actionCmd) {
    	return newButton(txt, actionCmd, true);
    }
    
    private JButton newButton(String txt, Cmd actionCmd, boolean enabled) {
    	JButton b = new JButton(txt);
    	b.setActionCommand(actionCmd.name());
    	b.addActionListener(this);
    	b.setEnabled(enabled);
    	return b;
    }
    
	@Override
	protected void doInitialization() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	protected void drawFrame(AWTGraphics g) {

		g.clearScreen(g.WHITE);
		stackChoice = -1;
		int n = game.getNumStacks();
		int numPerRow = 0;
		if (n<=4) {
			numPerRow = n;
		} else if (n <= 10) {
			numPerRow = n/2;
		} else {
			numPerRow = n/3;
		}
		int numRows = n/numPerRow;

		int stackVSpacing = numRows == 1 ? 30 : 20;
		int stackHeight = g.getViewportHeight()/numRows - (stackVSpacing*(numRows+1));
		int stackWidth = stackHeight / 4;
		
		int sy = stackVSpacing;
		int index = 0;
		for (int i=0; i<numRows-1; i++) {
			drawRow(g, sy, stackWidth, stackHeight, index, numPerRow);
			index += numPerRow;
			sy += stackHeight + stackVSpacing;
		}
		// draw remaining
		drawRow(g, sy, stackWidth, stackHeight, index, n-index);
	
		// draw messages
		int tx = 10;
		int ty = 10;
		g.setColor(g.BLACK);
		synchronized (messages) {
    		for (String msg : messages) {
    			g.drawString(msg, tx, ty);
    			ty += g.getTextHeight();
    		}
		}
	}

	private void drawRow(AGraphics g, int sy, int w, int h, int index, int num) {
		int hSpacing = w/5;
		int totW = w * num + (hSpacing) * (num-1);
		int sx = g.getViewportWidth()/2 - totW/2;
		for (int i=0; i<num; i++) {
			drawStack(g, sy, sx, w, h, index+i);
			sx += w+hSpacing;
		}
	}
	
	private void drawStack(AGraphics g, int sy, int sx, int w, int h, int i) {
		final int rx = sx-5;
		final int ry = sy-5;
		final int rw = w+10;
		final int rh = h+10;
		final int chipHeight = h/Math.max(12, game.getStackHeight(i));
		
		g.setColor(g.BLACK);
		g.drawJustifiedString(sx+w/2, sy+h+chipHeight+10, Justify.CENTER, Justify.TOP, String.valueOf(i));
		
		if (stackChoices != null && Arrays.binarySearch(stackChoices, i) >= 0) {
    		g.setColor(g.YELLOW);
    		if (Utils.isPointInsideRect(getMouseX(), getMouseY(), rx, ry, rw, rh)) {
    			g.setColor(g.GREEN);
    			stackChoice = i;
    		}
			
    		g.drawRect(rx, ry, rw, rh, 2);
		} else if (game.getSourceStack() == i) {
			if (Utils.isPointInsideRect(getMouseX(), getMouseY(), rx, ry, rw, rh)) {
				stackChoice = i;
			}
			g.setColor(g.BLUE);
    		g.drawRect(rx, ry, rw, rh, 2);
			
		}
		StackMate.Chip [] stack = game.getStack(i);
		if (stack.length > 12) {
			stack = Arrays.copyOfRange(stack, stack.length-12, 12);
		}
		for (StackMate.Chip c : stack) {
			AColor color, shadow;
			if (c == StackMate.Chip.RED) {
				color = g.RED;
				shadow = g.RED.darken(g, 0.5f);
			} else {
				color = g.BLACK;
				shadow = g.DARK_GRAY;
			}
			drawChip(g, sx, sy+h, w, chipHeight, color, shadow);
			sy -= chipHeight;
		}
	}
	
	private void drawChip(AGraphics g, int x, int y, int w, int h, AColor color, AColor shadow) {
		g.setColor(shadow);
		g.drawFilledOval(x, y+h/4, w, h);
		g.drawFilledRect(x, y+h/4, w, h/2);
		g.setColor(color);
		g.drawFilledOval(x, y-h/4, w, h);
	}
	
	@Override
	protected void onDimensionsChanged(AGraphics g, int width, int height) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		Cmd cmd = Cmd.valueOf(arg0.getActionCommand());
		switch (cmd) {
			case CHOOSE_BLACK:
				game.initPlayers(new StackMatePlayerSwingUser(), new StackMatePlayerAI());
				startGame();
				break;
			case CHOOSE_RED:
				game.initPlayers(new StackMatePlayerAI(), new StackMatePlayerSwingUser());
				startGame();
				break;
			case NEW_GAME:
				game.newGame();
				showChooseColorMenu();
				break;
			case NEW_CUSTOM_GAME: {
				Properties props = new Properties();
				try {
					InputStream in = new FileInputStream("stackmate.properties");
					try {
						props.load(in);
					} finally {
						in.close();
					}
					game.newGame(props);
					showChooseColorMenu();
				} catch (Exception e) {
					JOptionPane.showMessageDialog(frame, "Error reading stackmate.properties:\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
					game.newGame();
				}
				break;
			}
			case RESTORE: {
				try {
					StackMate g = new StackMate();
					g.loadFromFile(restoreFile);
					game.copyFrom(g);
					startGame();
				} catch (Exception e) {
					e.printStackTrace();
					JOptionPane.showMessageDialog(frame, "Error loading restore file: " + e.getMessage());
					restoreFile.delete();
				}
				break;
			}
			case SHOW_MAIN_MENU:
				stopGame();
				showMainMenu();
				break;
			case SHOW_HELP:
				showHelpPopup();
				break;
		}
		grabFocus();

	}
	
	void stopGame() {
		running = false;
		synchronized (this) {
			notify();
		}
	}
	
	@Override
	public void mouseClicked(MouseEvent evt) {
		super.mouseClicked(evt);
		synchronized (this) {
			notify();
		}
	}

	int [] stackChoices = null;
	int stackChoice = -1;
	
	int chooseStack(int [] choices) {
		stackChoices = choices;
		Arrays.sort(stackChoices);
		stackChoice = -1;
		synchronized (this) {
			try {
				wait();
			} catch (Exception e) {}
		}
		stackChoices = null;
		if (stackChoice == game.getSourceStack()) {
			game.cancel();
			return -1;
		}
		return stackChoice;
	}
}
