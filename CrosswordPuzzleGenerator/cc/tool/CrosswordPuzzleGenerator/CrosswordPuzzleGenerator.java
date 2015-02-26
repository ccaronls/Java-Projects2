package cc.tool.CrosswordPuzzleGenerator;

//CWGen.java

import java.io.*;
import java.util.*;

/**
 * 
 * @author Chris Caron
 *
 */
public class CrosswordPuzzleGenerator {

	/**
	 * 
	 * @param args
	 */
	public static void main(String [] args) {
		
		if (args.length < 3) {
			String usage = "Not Enough args, fuond [" + args.length + "] expected 3\nUSAGE: CWGen <wordlistfile> <width> <height>";
			System.out.println(usage);
			System.exit(1);
		}
		
		try {
			
			File file = new File(args[0]);
			int width = Integer.parseInt(args[1]);
			int height = Integer.parseInt(args[2]);
			BufferedReader reader = new BufferedReader(new FileReader(file));
			CrosswordPuzzleGenerator c = new CrosswordPuzzleGenerator(reader, width, height);
			Grid g = c.generate();
			g.display(System.out);
			
		} catch (IOException e) {
			System.err.println("Initialization error [" + e.getMessage() + "] invalid usage");
			System.exit(1);
		} catch (InvalidWordException e) {
			System.err.println(e.getMessage());
			System.exit(1);
		} catch (CantGeneratePuzzleException e) {
			System.err.println(e.getMessage());
			System.exit(1);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
		
	/*
	 * 
	 */
	CrosswordPuzzleGenerator(BufferedReader reader, int width, int height) throws Exception {
		ArrayList words = new ArrayList();
		while (true) {
			String word = reader.readLine();
			if (word == null)
				break;
			if (word.length() <= 0)
				continue;
			word = word.trim().toLowerCase();
			validateWord(word);
			debug("Adding word [" + word + "]");
			words.add(word);
		}
		reader.close();
		
		if (words.size() <= 0)
			throw new IOException("No words found");			
		
		this.width = width;
		this.height = height;
		this.words = (String [])words.toArray(new String[words.size()]);
	}
	
	/*
	 * 
	 */
	private void validateWord(String word) throws InvalidWordException {
		for (int i=0; i<word.length(); i++) {
			char c = word.charAt(i);
			if (c < 'a' || c > 'z')
				throw new InvalidWordException(word);
		}
	}

	/**
	 * 
	 * @return
	 * @throws CantGeneratePuzzleException
	 */
	public Grid generate() throws CantGeneratePuzzleException {
		Grid g = new Grid(width, height);
		if (generateR(g,0)) {
			g.fillEmpty();
			return g;
		}
		throw new CantGeneratePuzzleException();
	}
	
	/*
	 * 
	 */
	private boolean generateR(Grid g, int wordIndex) {
		
		if (wordIndex >= words.length)
			return true;

		Grid copy = new Grid(width, height);

		// iterate over the grid and try to place the word
		for (int i=0; i<width; i++) {
			for (int j=0; j<height; j++) {
				// copy the grid
				copy.copy(g);
		
				if (placeWord(copy, words[wordIndex], i, j, false)) {
					if (generateR(copy, wordIndex+1)) {
						g.copy(copy);
						return true;
					}
				}
				
				copy.copy(g);
				
				if (placeWord(copy, words[wordIndex], i, j, true)) {
					if (generateR(copy, wordIndex+1)) {
						g.copy(copy);
						return true;
					}					
				}
			}
		}
		return false;
	}
	
	/*
	 * 
	 */
	private boolean placeWord(Grid g, String word, int x, int y, boolean horz) {
		debug("placeWord [" + word + "] x [" + x + "] y [" + y + "] horz [" + horz + "]");
		if (g.get(x,y) == 0 && !g.canPlaceChar('#', x, y, Grid.FLAG_ALL))
			return false;
		// place the first char
		if (!g.canPlaceChar(word.charAt(0), x, y, 0))
			return false;
		g.set(x,y,word.charAt(0));
		
		if (horz) {
			if (word.length() > width-x)
				return false;
			int i;
			for (i=1; i<word.length()-1; i++) {
				if (g.canPlaceChar(word.charAt(i), x+i, y, Grid.FLAG_HORZ)) {
					g.set(x+i, y, word.charAt(i));
				} else {
					return false;
				}
			}
			// the last char cannot have a trailing char
			if (g.canPlaceChar(word.charAt(i), x+i, y, Grid.FLAG_HORZ | Grid.FLAG_RIGHT)) {
				g.set(x+i, y, word.charAt(i));
			} else {
				return false;
			}		
		} else {
			if (word.length() > height-y)
				return false;
			int i;
			for (i=1; i<word.length()-1; i++) {
				if (g.canPlaceChar(word.charAt(i), x, y+i, Grid.FLAG_VERT)) {
					g.set(x, y+i, word.charAt(i));
				} else {
					return false;
				}
			}
			// the last char cannot have a trailing char
			if (g.canPlaceChar(word.charAt(i), x, y+i, Grid.FLAG_VERT | Grid.FLAG_BOTTOM)) {
				g.set(x, y+i, word.charAt(i));
			} else {
				return false;
			}		
		}
		return true;
	}
	
	/*
	 * 
	 *
	private void printBar(PrintStream out) {
		for (int i=0; i<width*2+1; i++) {
			out.print("-");
		}
		out.println();
	}
	
	/*
	 * 
	 */
	private void debug(String msg) {
		//System.out.println(msg);
		//System.out.print(".");
	}

	/**
	 * 
	 * @author Chris Caron
	 *
	 */
	public static class InvalidWordException extends Exception {
		public InvalidWordException(String word) {
			super("Invalid word [" + word + "] word can contain only letters");
		}
	}

	/**
	 * 
	 * @author Chris Caron
	 *
	 */
	public static class CantGeneratePuzzleException extends Exception {
		public CantGeneratePuzzleException() {
			super("Failed to generate puzzle from inputs");
		}
	}
	
	/**
	 * 
	 * @author Chris Caron
	 *
	 */
	public static class Grid {
		private char [][] grid;
		
		public Grid(int width, int height) {
			grid = new char[width][];
			for (int i=0; i<grid.length; i++)
				grid[i] = new char[height];
		}
		
		public char get(int x, int y) {
			if (x<0 || y<0 || x>= grid.length || y>=grid[x].length)
				return 0;
			return grid[x][y];
		}
		
		public void set(int x, int y, char c) {
			grid[x][y] = c;
		}
		
		public void copy(Grid g) {
			for (int i=0; i<grid.length; i++) {
				for (int j=0; j<grid[i].length; j++) {
					set(i,j,g.get(i,j));
				}
			}
		}
		
		public void display(PrintStream out) {
			//printBar(out);
			for (int i=0; i<grid.length; i++) {
				out.print(FILL_CHAR);
				for (int j=0; j<grid[i].length; j++) {
					out.print(grid[i][j] + String.valueOf(FILL_CHAR));
				}
				out.println();
				//printBar(out);
			}
		}
		
		final static int FLAG_LEFT 		= 1<<3;
		final static int FLAG_RIGHT 	= 1<<2;
		final static int FLAG_TOP 		= 1<<1;
		final static int FLAG_BOTTOM 	= 1<<0;
		final static int FLAG_HORZ 		= FLAG_TOP | FLAG_BOTTOM;
		final static int FLAG_VERT 		= FLAG_LEFT | FLAG_RIGHT;
		final static int FLAG_ALL 		= FLAG_HORZ | FLAG_VERT;
		
		final char FILL_CHAR = ' ';
		
		public boolean canPlaceChar(char c, int x, int y, int adjacencyFlag) {
			if (get(x,y) == c)
				return true;
			if ((adjacencyFlag & 1<<3) != 0 && get(x-1, y)!=0)
				return false;
			if ((adjacencyFlag & 1<<2) != 0 && get(x+1, y)!=0)
				return false;
			if ((adjacencyFlag & 1<<1) != 0 && get(x, y-1)!=0)
				return false;
			if ((adjacencyFlag & 1<<0) != 0 && get(x,y+1)!=0)
				return false;
			if (get(x,y)==0)
				return true;
			return false;
		}
		
		public void fillEmpty() {
			for (int i=0; i<grid.length; i++) {
				for (int j=0; j<grid[i].length; j++) {
					if (get(i,j)==0)
						set(i,j,FILL_CHAR);
				}
			}
		}
		
	}
	
	private int width, height;
	private String [] words;
}
