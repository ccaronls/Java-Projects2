package cc.game.othello.swing;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import cc.game.othello.ai.AiOthelloPlayer;
import cc.game.othello.core.Othello;
import cc.game.othello.core.OthelloBoard;
import cc.lib.game.AAnimation;
import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.Utils;
import cc.lib.swing.AWTFrame;
import cc.lib.swing.AWTKeyboardAnimationApplet;
import cc.lib.utils.FileUtils;

public class OthelloApplet extends AWTKeyboardAnimationApplet implements ActionListener {

    public static void main(String [] args) {
        Utils.setDebugEnabled();
        Utils.setRandomSeed(0);
        AWTFrame frame = new AWTFrame("JavaRoids Debug Mode");
        AWTKeyboardAnimationApplet app = new OthelloApplet(frame);
        frame.add(app);
        File settings = FileUtils.getOrCreateSettingsDirectory(OthelloApplet.class);
        if (!frame.loadFromFile(new File(settings, "gui.properties")))
            frame.centerToScreen(640, 480);
        app.init();
        app.start();
//        app.setMillisecondsPerFrame(20);
    }   

	public static OthelloApplet getInstance() {
		return instance;
	}

	class FlipAnimation extends AAnimation<AGraphics> {

		final int row, col, fromColor, toColor;
		
		public FlipAnimation(int row, int col, int fromColor, int toColor) {
			super(1000,0);
			this.row = row;
			this.col = col;
			this.fromColor = fromColor;
			this.toColor = toColor;
		}

		@Override
		public void draw(AGraphics g, float position, float dt) {
			float scale = 0;
			if (position < 0.5f) {
				g.setColor(fromColor == OthelloBoard.CELL_WHITE ? GColor.WHITE : GColor.BLACK);
				scale = 1f - (position*2);
			} else {
				g.setColor(toColor == OthelloBoard.CELL_WHITE ? GColor.WHITE : GColor.BLACK);
				scale = (position-0.5f) * 2;
			}
			g.pushMatrix();
//			g.translate(p/2, 0);
			g.scale(scale, 1);
			g.drawFilledCircle(0, 0, 1f);
			g.popMatrix();
		}

	}
	
	static OthelloApplet instance;
    final Container frame;
    final JPanel buttons = new JPanel();
    final Othello game = new Othello() {

		@Override
		public void onCellChanged(int row, int col, int oldColor, int newColor) {
			FlipAnimation anim = new FlipAnimation(row,col,oldColor,newColor);
			anim.start(anims.size() * 500);
			anims.add(anim);
		}
    	
    };
    final File gameFile = new File(FileUtils.getOrCreateSettingsDirectory(getClass()), "othello.txt");;
    final List<FlipAnimation> anims = new LinkedList<FlipAnimation>();
    
    OthelloApplet(Container frame) {
    	this.frame = frame;
    	instance = this;
        frame.add(buttons, BorderLayout.SOUTH);
        buttons.setLayout(new GridLayout(1, 0));
    	showMainMenu();
    }
    
    enum Cmd {
    	NEW_GAME,
    	RESTORE,
    	CHOOSE_WHITE,
    	CHOOSE_BLACK,
    	SHOW_MAIN_MENU,
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
    
	private void showMainMenu() {
		buttons.removeAll();
		buttons.add(newButton("New Game", Cmd.NEW_GAME));
		buttons.add(newButton("Restore", Cmd.RESTORE, gameFile.exists()));
	}

	protected void showChooseColorMenu() {
		buttons.removeAll();
		buttons.add(newButton("White", Cmd.CHOOSE_WHITE));
		buttons.add(newButton("Black", Cmd.CHOOSE_BLACK));
		buttons.add(newButton("Back", Cmd.SHOW_MAIN_MENU));
	}

	private void showGameMenu() {
		buttons.removeAll();
		buttons.add(newButton("Quit", Cmd.SHOW_MAIN_MENU));
		buttons.add(new JLabel("White: " + game.getBoard().getCellCount(OthelloBoard.CELL_WHITE)));
		buttons.add(new JLabel("Black: " + game.getBoard().getCellCount(OthelloBoard.CELL_BLACK)));
		frame.validate();
		frame.repaint();
	}

	boolean running = false;
	
	void startGame() {
		if (!running) {
			running = true;
			new Thread(new Runnable() {
				public void run() {
					try {
						Utils.println("Thread running");
						while (running && !game.isGameOver()) {
							if (anims.size() == 0) {
								synchronized (game) {
									game.runGame();
									game.saveToFile(gameFile);
								}
								showGameMenu();
							}
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
	
	@Override
	protected void doInitialization() {
		
	}

	@Override
	protected void drawFrame(AGraphics g) {
        float w = g.getViewportWidth();
        float h = g.getViewportHeight();
        float radius = 10;
        float padding = 10;
        float thickness = 5;

        g.setColor(GColor.GREEN);
        g.drawFilledRoundedRect(padding, padding, w-2*padding, h-padding*2, radius);

        g.setColor(GColor.WHITE);
        g.drawRoundedRect(padding, padding, w-2*padding, h-padding*2, thickness, radius);

        OthelloBoard b = game.getBoard();

        float bw = g.getViewportWidth() - padding*2 - radius*2;
        float bh = g.getViewportHeight() - padding*2 - radius*2;

        float cw = bw/b.getNumCols();
        float ch = bh/b.getNumRows();

        float x0 = radius+padding;
        float y0 = radius+padding;

        pickedRow = pickedCol = -1;

        for (int r=0; r<b.getNumRows(); r++) {
            for (int c=0; c<b.getNumCols(); c++) {
                float cx = x0 + cw/2 + cw*c;
                float cy = y0 + ch/2 + ch*r;

                int cell = b.get(r, c);

                float cx0 = cx-cw/2+1;
                float cy0 = cy-ch/2+1;

                float pcRad = Math.min(cw, ch)/2-3;

                g.setColor(GColor.WHITE);
                switch (cell) {
                case OthelloBoard.CELL_AVAILABLE:
                    if (Utils.isPointInsideRect(getMouseX(), getMouseY(), cx0, cy0, cw-2, ch-2)) {
                        g.setColor(GColor.RED);
                        pickedRow = r;
                        pickedCol = c;
                    }
                case OthelloBoard.CELL_UNUSED:
                    g.drawRect(cx-cw/2+1, cy-ch/2+1, cw-2, ch-2, 2);
                    continue;
                }

                g.setColor(GColor.WHITE);
                g.drawRect(cx-cw/2+1, cy-ch/2+1, cw-2, ch-2, 2);

                FlipAnimation f= getAnimationAt(r, c);

                if (f != null) {
                    g.pushMatrix();
                    g.translate(cx, cy);
                    g.scale(pcRad,pcRad);
                    f.update(g);
                    if (f.isDone())
                        anims.remove(f);
                    g.popMatrix();
                } else {
                    g.setColor(b.get(r, c) == OthelloBoard.CELL_BLACK ? GColor.BLACK : GColor.WHITE);
                    g.drawFilledCircle(cx, cy, pcRad);
                }
            }
        }
    }

	FlipAnimation getAnimationAt(int r, int c) {
		for (FlipAnimation f : anims) {
			if (f.row == r && f.col == c)
				return f;
		}
		return null;
	}
	
	int pickedRow = 0;
	int pickedCol = 0;

	@Override
	protected void onDimensionsChanged(AGraphics g, int width, int height) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void actionPerformed(ActionEvent ev) {
		try {
			switch (Cmd.valueOf(ev.getActionCommand())) {
			case CHOOSE_BLACK:
				game.intiPlayers(new AiOthelloPlayer(), new SwingOthelloPlayer());
				game.newGame();
				startGame();
				break;
			case CHOOSE_WHITE:
				game.intiPlayers(new SwingOthelloPlayer(), new AiOthelloPlayer());
				game.newGame();
				startGame();
				break;
			case NEW_GAME:
				showChooseColorMenu();
				break;
			case RESTORE:
				game.loadFromFile(gameFile);
				startGame();
				break;
			case SHOW_MAIN_MENU:
				pickedRow = pickedCol = -1;
				running = false;
				synchronized (this) {
					notify();
				}
				showMainMenu();
				break;
			default:
				break;
				
			}
			frame.validate();
			frame.repaint();
			getRootPane().grabFocus();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean pickCell(int[] rowColCell) {
		try {
			synchronized (this) {
				wait();
			}

			rowColCell[0] = pickedRow;
			rowColCell[1] = pickedCol;
			return true;
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public void mouseClicked(MouseEvent evt) {
		super.mouseClicked(evt);
		synchronized (this) {
			notify();
		}
	}

	

}
