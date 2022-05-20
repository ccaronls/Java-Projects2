package marcos.games.hexes.swing;

import java.io.File;
import java.util.LinkedList;

import javax.swing.JOptionPane;

import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.Justify;
import cc.lib.game.Utils;
import cc.lib.swing.AWTFrame;
import cc.lib.swing.AWTKeyboardAnimationApplet;
import cc.lib.utils.FileUtils;
import marcos.games.hexes.core.Board;
import marcos.games.hexes.core.Hexes;
import marcos.games.hexes.core.Piece;
import marcos.games.hexes.core.Player;
import marcos.games.hexes.core.Shape;

public class HexesApplet extends AWTKeyboardAnimationApplet {

	static HexesApplet instance = null;

	public static void main(String [] args) {
        Utils.setDebugEnabled();
        Utils.setRandomSeed(0);
        AWTFrame frame = new AWTFrame("Hexes Debug Mode");
        AWTKeyboardAnimationApplet app = new HexesApplet(frame);
        frame.add(app);
        frame.centerToScreen(640, 480);
        app.init();
        app.start();
        app.setMillisecondsPerFrame(200);
    }   

	final Hexes game = new Hexes() {

		@Override
		protected void onGameOver(final int winner) {
		    stopGame();
			new Thread() {
				public void run() {
        			int n = JOptionPane.showConfirmDialog(
        				    frame,
        				    "Player " + winner + " wins\nPlay Again?",
        				    "Game Over",
        				    JOptionPane.YES_NO_OPTION);
        			switch (n) {
        				case JOptionPane.YES_OPTION:
        					new Thread(new Runnable() {
        						public void run() {
        							showMainMenu();
        						}
        					}).start();
        					break;
        				case JOptionPane.CLOSED_OPTION:
        				case JOptionPane.NO_OPTION:
        					System.exit(0);
        					break;
        			}
				}
			}.start();
		}

		@Override
		protected void onPiecePlayed(int pIndex, int pts) {
			Piece p = getBoard().getPiece(pIndex);
			addMessage("Player %s played a %s for %d points", p.getPlayer(), p.getType(), pts);
		}
		
	};
	final AWTFrame frame;
	final File restoreFile;
	int highlightedPiece = -1;
	final LinkedList<String> messages = new LinkedList<String>();
	
	void addMessage(String msg, Object ... args) {
		synchronized (messages) {
			messages.add(String.format(msg, args));
			while (messages.size() > 3)
				messages.removeLast();
		}
	}
	
	public HexesApplet(AWTFrame frame) {
		instance = this;
		this.frame = frame;
		File settings = FileUtils.getOrCreateSettingsDirectory(getClass());
		frame.loadFromFile(new File(settings, "hexes.properties"));
		restoreFile = new File(settings, "hexes.txt");
		if (restoreFile.exists()) {
        	if (game.tryLoadFromFile(restoreFile)) {
        	    startGame();
        	    return;
            }
		}
        showHelpPopup();
	}

	private void showHelpPopup() {
		new Thread(() -> {
            String welcome = "Welcome to Hexes!\n\n"
                    + "The Object of the game is to gain the most points by placing triangular pieces to form hexagons, triangles and diamonds.\n"
                    + "";
            JOptionPane.showConfirmDialog(frame, welcome, "Welcome!", JOptionPane.INFORMATION_MESSAGE);
            showChooseColorMenu();
        }).start();
	}

	private void showChooseColorMenu() {
		new Thread() {
			public void run() {
        		int n = JOptionPane.showOptionDialog(frame,
        			    "Choose Your Color",
        			    "Choose",
        			    JOptionPane.YES_NO_OPTION,
        			    JOptionPane.PLAIN_MESSAGE,
        			    null,
        			    new String[] { "RED", "BLUE" },
        			    null);
        		if (n == 0) {
        			game.initPlayers(new SwingPlayer(), new Player());
        		} else if (n == 1) {
        			game.initPlayers(new Player(), new SwingPlayer());
        		}
        		game.newGame();
        		startGame();
			}
		}.start();
	}

	private void showMainMenu() {
		new Thread() {
			public void run() {
        		int n = JOptionPane.showOptionDialog(frame,
        			    "Choose Game",
        			    "Choose",
        			    JOptionPane.YES_NO_OPTION,
        			    JOptionPane.PLAIN_MESSAGE,
        			    null,
        			    new String[] { "NEW", "RESTORE", "CUSTOM", "AUTO" },
        			    null);
        		switch (n) {
        			case 0:
        				showChooseColorMenu(); 
        				break;
        				
        			case 1:
        				try {
        					Hexes g = new Hexes();
        					g.loadFromFile(restoreFile);
        					game.copyFrom(g);
        					startGame();
        				} catch (Exception e) {
        					e.printStackTrace();
        					JOptionPane.showMessageDialog(frame, "Error loading restore file: " + e.getMessage());
        					restoreFile.delete();
        				}
        				break;
        				
        			case 2: // TODO: CUSTOM
        				break;
        				
        			case 3: // AUTO (computer vs. computer)
        				game.initPlayers(new Player(), new Player());
        				game.newGame();
        				startGame();
        				break;
        				

        		}
			}
		}.start();
	}
	

	void stopGame() {
	    running = false;
    }

	boolean running = false;
	void startGame() {
		if (!running) {
			grabFocus();
			synchronized (messages) {
				messages.clear();
			}
			running = true;
			new Thread() {
				public void run() {
					try {
						Utils.println("Thread running");
						while (running && !game.isGameOver()) {
							synchronized (game) {
								game.runGame();
								game.saveToFile(restoreFile);
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					Utils.println("Thread exiting running=" + running + " gameover=" + game.isGameOver());
					running = false;
				}
			}.start();
		}
	}
	
	@Override
	protected void doInitialization() {
	}

	@Override
	protected void drawFrame(AGraphics g) {
		g.clearScreen(GColor.LIGHT_GRAY);
		Board b = game.getBoard();
		b.setHighlighted(getMouseX(), getMouseY());
		highlightedPiece = b.draw(g);
		g.ortho();
		g.setColor(GColor.RED);
		if (game.getNumPlayers() > 1) {
    		String redTxt = "Points     " + game.getPlayer(1).getScore() + "\n"
    				      + "Hexagons   " + game.getPlayer(1).getShapeCount(Shape.HEXAGON) + "\n"
    				      + "Triangles  " + game.getPlayer(1).getShapeCount(Shape.TRIANGLE) + "\n"
    				      + "Diamonds   " + game.getPlayer(1).getShapeCount(Shape.DIAMOND);
    		
    		g.drawJustifiedString(10, 10, Justify.LEFT, redTxt);
    		g.setColor(GColor.BLUE);
    		String blueTxt = "Points     " + game.getPlayer(2).getScore() + "\n"
    				       + "Hexagons   " + game.getPlayer(2).getShapeCount(Shape.HEXAGON) + "\n"
    				       + "Triangles  " + game.getPlayer(2).getShapeCount(Shape.TRIANGLE) + "\n"
    				       + "Diamonds   " + game.getPlayer(2).getShapeCount(Shape.DIAMOND);
    		
    		g.drawJustifiedString(g.getViewportWidth()-10, 10, Justify.RIGHT, blueTxt);
		}
		// draw messages
		int tx = g.getViewportWidth()/2;
		int ty = 10;
		g.setColor(GColor.BLACK);
		synchronized (messages) {
    		for (String msg : messages) {
    			g.drawJustifiedString(tx, ty, Justify.CENTER, msg);
    			ty += g.getTextHeight();
    		}
		}

	}
	
	@Override
	protected void onDimensionsChanged(AGraphics g, int width, int height) {
		
	}

}
