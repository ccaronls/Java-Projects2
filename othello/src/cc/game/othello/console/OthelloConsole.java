package cc.game.othello.console;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import cc.game.othello.ai.AiOthelloPlayer;
import cc.game.othello.core.Othello;
import cc.game.othello.core.OthelloBoard;
import cc.game.othello.core.OthelloPlayer;

public class OthelloConsole {

	public static class ConsolePlayer extends OthelloPlayer {

		@Override
		public boolean chooseCell(OthelloBoard board, int[] rowColCell) {
			String cmd = getCommand();

			if (cmd.length() > 1) {
				rowColCell[0] = cmd.toLowerCase().charAt(0) - 'a';
				rowColCell[1] = cmd.toLowerCase().charAt(1) - 'a';
				return true;
			}
			
			return false;
		}
	}
	
	public static void main(String [] args) {
		Othello game = new Othello();
		
		ConsolePlayer consolePlayer = new ConsolePlayer();
		AiOthelloPlayer aiPlayer = new AiOthelloPlayer();
		
		new Thread(new Runnable() {
			public void run() {
				try {
					
					BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
					while (true) {
						String line = input.readLine();
						if (command != null) {
							synchronized (writeLock) {
								writeLock.wait();
							}
						}
						command = line;
						synchronized (readLock) {
							readLock.notify();
						}
					}
					
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
			}
		}).start();
		
		try {
			File file = new File("othello.txt");
			if (file.exists())
				game.loadFromFile(file);
			else {
				game.intiPlayers(consolePlayer, aiPlayer);
				game.newGame();
			}
			
			while (!game.isGameOver()) {
				drawGame(game);
				game.runGame();
				game.saveToFile(file);
			}
			
			System.out.println("Game Over");
			
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		System.exit(0);
	}
	
	static final Object readLock = new Object();
	static final Object writeLock = new Object();
	static String command = null;
	
	public static String getCommand() {
		if (command == null) {
			synchronized (readLock) {
				try {
					readLock.wait();
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
			}
		}
		String s = command;
		command = null;
		synchronized (writeLock) {
			writeLock.notify();
		}
		return s;
	}

	final static String [][] PC = {
		{
			"     ",
			"     ",
			"     "
		},
		{
			".   .",
			"     ",
			".   ."
		},
		{
			"/---\\",
			"|   |",
			"\\___/"
		},
		{
			"#####",
			"#####",
			"#####"
		}
	};
	
	
	static void drawGame(Othello game) {
		OthelloBoard b = game.getBoard();
		StringBuffer s = new StringBuffer("  ");
		for (int c=0; c<game.getBoard().getNumCols(); c++) {
			s.append("+-----");
		}
		s.append("+\n");
		for (int r=0; r<b.getNumRows(); r++) {
			s.append("" + (char)('A' + r) + " ");
			for (int i=0; i<3; i++) {
				for (int c=0; c<game.getBoard().getNumCols(); c++) {
				int p = b.get(r,c);
					s.append("|").append(PC[p][i]);
				}
				s.append("|\n  ");
			}
			//s.append("  ");
			for (int c=0; c<game.getBoard().getNumCols(); c++) {
				s.append("+-----");
			}
			s.append("+\n");
		}
		s.append("  ");
		for (int c=0; c<game.getBoard().getNumCols(); c++) {
			s.append("   " + (char)('A' + c) + "  ");
		}
		s.append("\n");
		
		s.append("\n\nWHITE = " + b.getCellCount(OthelloBoard.CELL_WHITE) + "\nBLACK = " + b.getCellCount(OthelloBoard.CELL_BLACK) + "\n\n>");
		System.out.println(s.toString());
	}

}

