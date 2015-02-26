package marcos.games.hexes.swing;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import marcos.games.hexes.core.Board;
import marcos.games.hexes.core.Hexes;
import marcos.games.hexes.core.Piece;
import marcos.games.hexes.core.Player;
import marcos.games.hexes.core.Shape;
import marcos.games.hexes.swing.MultiPlayerClient.*;
import cc.lib.game.AGraphics;
import cc.lib.game.Justify;
import cc.lib.game.Utils;
import cc.lib.swing.AWTGraphics;
import cc.lib.swing.EZButton;
import cc.lib.swing.EZFrame;
import cc.lib.swing.KeyboardAnimationApplet;

public class HexesApplet extends KeyboardAnimationApplet implements MultiPlayerClient.Listener {

	static HexesApplet instance = null;

	public static void main(String [] args) {
        Utils.DEBUG_ENABLED = true;
        Utils.setRandomSeed(0);
        EZFrame frame = new EZFrame("Hexes Debug Mode");
        KeyboardAnimationApplet app = new HexesApplet(frame);
        frame.add(app);
        frame.centerToScreen(640, 480);
        app.init();
        app.start();
        app.setMillisecondsPerFrame(200);
    }   

	MultiPlayerClient mpClient = null;
	final Hexes game = new Hexes() {

		@Override
		protected void onGameOver(final int winner) {
			if (mpClient != null) {
				mpClient.sendCommand("gameState", game);
				mpClient.disconnect();
			}
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
		protected void onPiecePlayed(int pIndex) {
			Piece p = getBoard().getPiece(pIndex);
			addMessage("Player " + p.getPlayer() + " played a " + p.getType());
			if (mpClient != null) {
				mpClient.sendCommand("gameState", game);
			}
		}
		
	};
	final Frame frame;
	final File restoreFile = new File("hexes.txt");
	final Properties props = new Properties();
	int highlightedPiece = -1;
	final LinkedList<String> messages = new LinkedList<String>();
	
	void addMessage(String msg) {
		synchronized (messages) {
			messages.add(msg);
			while (messages.size() > 6)
				messages.removeLast();
		}
	}
	
	public HexesApplet(EZFrame frame) {
		instance = this;
		this.frame = frame;
		try {
			FileInputStream in = new FileInputStream("hexes.properties");
			try {
				props.load(in);
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				in.close();
			}
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
		showMainMenu();
		if (restoreFile.exists()) {
        	new Thread() {
        		public void run() {
        			try {
        				Thread.sleep(1000);
        			} catch (Exception e) {}
                	showHelpPopup();
        		}
        	}.start();

		}
	}

	private void showHelpPopup() {
		
		String welcome = "Welcome to Hexes!\n\n"
		       + "The Object of the game is to gain the most points by placing triangular pieces to form hexagons, triangles and diamonds.\n"
			   + "";
		JOptionPane.showMessageDialog(frame, welcome, "Welcome!", JOptionPane.INFORMATION_MESSAGE); 
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
        			    new String[] { "NEW", "RESTORE", "CUSTOM", "AUTO", "MULTI PLAYER" },
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
        				
        			case 4: { // MULTIPLAYER
        				mpClient = new MultiPlayerClient(HexesApplet.this);
        				connectMultiplayer();
        				break;
        			}
        			
        		}
			}
		}.start();
	}
	
	private void endMultiplayer() {
		if (mpClient != null) {
			mpClient.disconnect();
		}
		mpClient = null;
		showMainMenu();
	}
	
	private void connectMultiplayer() {
		if (props.getProperty("username") == null) {
			showUsernamePasswordDialog("Enter User name and Password");
			return;
		}

		final JDialog dlg = showProgressDialog("Connecting", new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				mpClient.disconnect();
			}
		});
		final Callback<Void> connectCB = new Callback<Void>() {
			
			@Override
			public void complete(ResultStatus status, String statusMsg, Void... params) {
				switch (status) {
					default:
					case STATUS_FAILED:
						dlg.setVisible(false);
						int n = JOptionPane.showOptionDialog(frame, "Connect Failed\n" + statusMsg + "\nTry Again?", "Error", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE, null, null, null);
						switch (n) {
							case JOptionPane.YES_OPTION:
								connectMultiplayer();
								break;
							default:
								endMultiplayer();
								break;
						}
						break;
					case STATUS_OK: {
						User user = mpClient.chooseRandomUserForNewGame();
						if (user == null) {
							JOptionPane.showMessageDialog(frame, "There are no games ready yet.  Once another player becomes available you will be connected");
						} else {
							if (Utils.flipCoin()) {
								game.initPlayers(new SwingPlayer(), new RemotePlayer(user, mpClient));
							} else {
								game.initPlayers(new RemotePlayer(user, mpClient), new SwingPlayer());
							}
							startGame();
						}
						break;
					}
					case STATUS_CANCELED:
						dlg.setVisible(false);
						endMultiplayer();
						break;
					
				}
			}
		};
		mpClient.connect(props.getProperty("username"), props.getProperty("password"), connectCB);
	}
	
	private void showUsernamePasswordDialog(final String message) {
		new Thread() {
			public void run() {
				final JDialog d = new JDialog(frame, message, true);
				d.setLayout(new GridLayout(0, 3));
				final JTextField userName = new JTextField(props.getProperty("username"), 32);
				final JTextField passWord = new JPasswordField(props.getProperty("password"), 32);
				d.add(new JLabel("User Name"));
				d.add(userName);
				d.add(new JLabel("Password"));
				d.add(passWord);
				d.add(new EZButton("Cancel", new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent arg0) {
						d.setVisible(false);
						endMultiplayer();
					}
				}));
				d.add(new EZButton("Connect", new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						props.setProperty("username", userName.getText());
						props.setProperty("password", passWord.getText());
					}
				}));
			}
		}.start();
	}
	
	private JDialog showProgressDialog(String msg, final ActionListener cancelListener) {
		final JDialog dlg = new JDialog(frame, msg, true);
		new Thread() {
			public void run() {
        	    JProgressBar dpb = new JProgressBar();
        	    dpb.setIndeterminate(true);
        	    dlg.add(BorderLayout.CENTER, dpb);
        	    dlg.add(BorderLayout.NORTH, new JLabel("Progress..."));
        	    JButton button = new JButton("Cancel");
        	    button.addActionListener(cancelListener);
        	    button.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						dlg.setVisible(false);
					}
				});
        	    dlg.add(BorderLayout.SOUTH, button);
        	    dlg.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        	    dlg.setSize(300, 75);
        	    dlg.setLocationRelativeTo(frame);
				dlg.setVisible(true);
			}
	    }.start();
	    return dlg;
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
	protected void drawFrame(AWTGraphics g) {
		g.clearScreen(g.LIGHT_GRAY);
		Board b = game.getBoard();
		b.setHighlighted(getMouseX(), getMouseY());
		highlightedPiece = b.draw(g);
		g.ortho();
		g.setColor(g.RED);
		if (game.getNumPlayers() > 1) {
    		String redTxt = "Points     " + game.getPlayer(1).getScore() + "\n"
    				      + "Hexagons   " + game.getPlayer(1).getShapeCount(Shape.HEXAGON) + "\n"
    				      + "Triangles  " + game.getPlayer(1).getShapeCount(Shape.TRIANGLE) + "\n"
    				      + "Diamonds   " + game.getPlayer(1).getShapeCount(Shape.DIAMOND);
    		
    		g.drawJustifiedString(10, 10, Justify.LEFT, redTxt);
    		g.setColor(g.BLUE);
    		String blueTxt = "Points     " + game.getPlayer(2).getScore() + "\n"
    				       + "Hexagons   " + game.getPlayer(2).getShapeCount(Shape.HEXAGON) + "\n"
    				       + "Triangles  " + game.getPlayer(2).getShapeCount(Shape.TRIANGLE) + "\n"
    				       + "Diamonds   " + game.getPlayer(2).getShapeCount(Shape.DIAMOND);
    		
    		g.drawJustifiedString(g.getViewportWidth()-10, 10, Justify.RIGHT, blueTxt);
		}
		// draw messages
		int tx = g.getScreenWidth()/2;
		int ty = 10;
		g.setColor(g.BLACK);
		synchronized (messages) {
    		for (String msg : messages) {
    			g.drawJustifiedString(tx, ty, Justify.CENTER, msg);
    			ty += g.getTextHeight();
    		}
		}

	}
	
	@Override
	public boolean onCommandReceived(String cmd, Object... params) {
		if (cmd.equals("gameState")) {
			game.copyFrom((Hexes)params[0]);
		}

		return false;
	}

	@Override
	protected void onDimensionsChanged(AGraphics g, int width, int height) {
		
	}

}
