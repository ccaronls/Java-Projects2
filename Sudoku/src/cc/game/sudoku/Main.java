package cc.game.sudoku;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import cc.lib.game.GColor;
import cc.lib.swing.AWTGraphics;
import cc.lib.swing.AWTButton;
import cc.lib.swing.AWTFrame;
import cc.lib.swing.AWTKeyboardAnimationApplet;
import cc.lib.game.AGraphics;
import cc.lib.game.Utils;

public class Main extends AWTKeyboardAnimationApplet implements ActionListener {

	
	Sudoku sudoku = new Sudoku(new Random(System.currentTimeMillis()));
	boolean showHints = false;
	int curRow = -1;
	int curCol = -1;
	int curNum = -1;

	@Override
	protected void onDimensionsChanged(AGraphics g, int width, int height) {
	    g.ortho(0, width, 0, height);
	}
	
	protected void doInitialization() {
		sudoku.generate(Sudoku.MEDIUM);
	}
	
	void drawRect(AGraphics g, int x, int y, int w, int h, int thickness) {
		int start = -thickness/2;
		int end = thickness/2;
		for (int i=start; i<=end; i++)
			g.drawRect(x+i,y+i,w-2*i,h-2*i);
	}
	
	boolean isInside(int px, int py, int x, int y, int w, int h) {
		return px>x && px<x+w && py>y && py<y+h;
	}
	
	boolean isMouseInside(int x, int y, int w, int h) {
		return isInside(getMouseX(), getMouseY(), x, y, w, h);
	}
	
	@Override
	protected void drawFrame(AGraphics g) {
		
		final int width = getScreenWidth();
		final int height = getScreenHeight();
		
        // clear screen
        g.setColor(GColor.BLACK);
        g.drawFilledRect(0,0,width,height);

		curRow = curCol = curNum -1;
		boolean solved = sudoku.isSolved();
        
		int dw = width/Sudoku.SUDOKU_DIM;
		int dh = height/Sudoku.SUDOKU_DIM;
		
		// draw the grid
		if (solved)
			g.setColor(new GColor(sudoku.rand()%256,sudoku.rand()%256,sudoku.rand()%256));
		else
			g.setColor(GColor.LIGHT_GRAY);
		for (int i=0; i<=Sudoku.SUDOKU_DIM; i++) {
			g.drawLine(dw*i, 0, dw*i, height);
			g.drawLine(0, dh*i, width, dh*i);
		}
		
		// draw the sub-cells
		g.setColor(GColor.CYAN);
		for (int i=0; i<=Sudoku.SUDOKU_DIM/3; i++) {
			for (int t=-1; t<=1; t++) {
				g.drawLine(dw*3*i+t, 0, dw*3*i+t, height);
				g.drawLine(0, dh*3*i+t, width, dh*3*i+t);
			}
		}

		boolean leftPressed = getMouseButtonClicked(0);
		
		// draw the numbers
		for (int i=0; i<Sudoku.SUDOKU_DIM; i++) {
			for (int j=0; j<Sudoku.SUDOKU_DIM; j++) {
				int x = i*dw;
				int y = j*dh;
				final boolean inside = isMouseInside(x,y,dw,dh);
				final boolean canEdit = sudoku.canEdit(i,j);
				final int num = sudoku.getBoard(i,j);
				
				if (inside && canEdit) {
					// highlight this square in red
					g.setColor(GColor.RED);
					drawRect(g,x,y,dw,dh,3);
					curRow = i;
					curCol = j;
				}
				
				if (num > 0) {
					if (canEdit) {
						g.setColor(GColor.YELLOW);
					} else {
						g.setColor(GColor.WHITE);
					}
					drawNumber(g, num, x, y, dw, dh);
					if (inside && leftPressed && canEdit) {
						sudoku.setBoard(i,j,0);
						leftPressed = false;
					}
					
				} else if (inside && canEdit && showHints) {
					curNum = -1;
					drawHints(g,x,y,dw,dh);
					if (leftPressed && curNum >= 0) {
						sudoku.setBoard(i,j,curNum);
						leftPressed = false;
					}
				}
			}
		}
	}
	
	void drawNumber(AGraphics g, int num, int x, int y, int w, int h) {
		int size = g.getTextHeight();//g.getFont().getSize();
		int tx = x + w/2 - size/2;
		int ty = y + h/2 + size/2;
		g.drawString(String.valueOf(num), tx, ty);
	}
	
	void drawHints(AGraphics g, int x, int y, int w, int h) {
		
		final int startX = x;
		
		int dw = w/3;
		int dh = h/3;
		
		int numHints = 0;
		for (int i=1; i<=9; i++)
		    if (sudoku.canSetSquare(i, curRow, curCol))
		        numHints++;
		
		int numDrawn = 0;
		for (int i=1; i<=9; i++) {
			if (sudoku.canSetSquare(i, curRow, curCol)) {
				if (numHints == 1 || isMouseInside(x,y,dw,dh)) {
					curNum = i;
					g.setColor(GColor.YELLOW);
				} else {
					g.setColor(GColor.GREEN);
				}
				drawNumber(g, i, x, y, dw, dh);
				if (++numDrawn >= 3) {
					numDrawn = 0;
					x = startX;
					y += dh;
				} else {
					x += dw;
				}
			}
		}
		
	}
	
	public void actionPerformed(ActionEvent ev) {
		
		String cmd = ev.getActionCommand();
		
		if (cmd.equals("Easy")) {
			sudoku.generate(Sudoku.EASY);
		} else if (cmd.equals("Medium")) {
			sudoku.generate(Sudoku.MEDIUM);
		} else if (cmd.equals("Hard")) {
			sudoku.generate(Sudoku.HARD);
		} else if (cmd.equals("Solve")) {
			sudoku.solve();
		} else if (cmd.equals("Enable")) {
			showHints = true;
		} else if (cmd.equals("Disable")) {
			showHints = false;
		} else if (cmd.equals("Quit")) {
			System.exit(0);
		} else {
			System.err.println("ERROR: Unhandled cmd [" + cmd + "]");
		}
	}
	
	public void showNewMenu() {
		final AWTFrame frame = new AWTFrame("NEW") {
			public void onClosing() { hide(); }
		};
		frame.setLayout(new GridLayout(3,1));
		ActionListener listener = new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				String cmd = ev.getActionCommand();
				if (cmd.equals("EASY")) {
					sudoku.generate(Sudoku.EASY);
				} else if (cmd.equals("MEDIUM")) {
					sudoku.generate(Sudoku.MEDIUM);					
				} else if (cmd.equals("HARD")) {
					sudoku.generate(Sudoku.HARD);
				} else {
					System.err.println("Unhandled cmd [" + cmd + "]");
				}
				
				frame.hide();
			}
		};
		frame.add(new AWTButton("EASY", listener));
		frame.add(new AWTButton("MEDIUM", listener));
		frame.add(new AWTButton("HARD", listener));
		frame.centerToScreen();
	}
	
	public void keyPressed(KeyEvent ev) {
		
		if (ev.getKeyChar() == 's' || ev.getKeyChar() == 'S') {
			sudoku.solve();
		} else if (ev.getKeyChar() == 'h' || ev.getKeyChar() == 'H') {
			showHints = !showHints;
		} else if (ev.getKeyChar() == 'n' || ev.getKeyChar() == 'N') {
			showNewMenu();
		} else if (ev.getKeyChar() >= '0' && ev.getKeyChar() <= '9' && curRow >= 0 && curCol >= 0) {
			int num = ev.getKeyChar() - '0';
			sudoku.setBoard(curRow, curCol, num);
		}
	}

	public static void main(String [] args) {
		Utils.DEBUG_ENABLED = true;
		AWTFrame frame = new AWTFrame();
		
		Main main = new Main();
		MenuBar menuBar = new MenuBar();
		
		Menu newMenu = new Menu("NEW");
		Menu hintsMenu = new Menu("HINTS");
		Menu gameMenu = new Menu("GAME");
		
		newMenu.add("Easy");
		newMenu.add("Medium");
		newMenu.add("Hard");
		newMenu.addActionListener(main);
		
		hintsMenu.add("Enable");
		hintsMenu.add("Disable");
		hintsMenu.addActionListener(main);
		
		gameMenu = new Menu("GAME");
		gameMenu.add(newMenu);
		gameMenu.add(hintsMenu);
		gameMenu.add("Solve");
		gameMenu.add("Quit");
		gameMenu.addActionListener(main);
		menuBar.add(gameMenu);		
		frame.setMenuBar(menuBar);
        frame.add(main);
        frame.centerToScreen(640, 480);
        
        main.start();
        main.init();
    }
	
}
