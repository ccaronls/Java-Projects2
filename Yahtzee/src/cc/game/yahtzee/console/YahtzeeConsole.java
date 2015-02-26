package cc.game.yahtzee.console;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import cc.game.yahtzee.core.Yahtzee;
import cc.game.yahtzee.core.YahtzeeRules;
import cc.game.yahtzee.core.YahtzeeSlot;
import cc.lib.game.Utils;

public class YahtzeeConsole extends Yahtzee {

	public static void main(String [] args) {

		Utils.DEBUG_ENABLED = true;
		
		final File restoreFile = new File("yahtzee.sav");
		try {
			
			YahtzeeConsole yc = new YahtzeeConsole();
			do {
				System.out.println("N>  New game\n" + 
								   "R>  Restore Game\n" + 
								   "A>  New Alternate Game\n\n>");

				String input = in.readLine();
				if (input.length() == 0) {
					continue;
				}

				switch (input.toLowerCase().charAt(0)) {
				case 'n':
					break;
				case 'r':
					if (restoreFile.exists()) {
						try {
							yc.loadFromFile(restoreFile);
						} catch (Exception e) {
							e.printStackTrace();
							continue;
						}
					} else {
						System.err.println("Restore file " + restoreFile + " not found");
						continue;
					}
					break;
				case 'a':
					YahtzeeRules rules = new YahtzeeRules();
					rules.setEnableAlternateVersion(true);
					yc.reset(rules);
					break;
				default:
					System.err.println("Invalid entry\n\n");
					continue;
				}
			} while (false);
				
			
			
			while (yc.isRunning()) {
				yc.draw();
				yc.runGame();
				yc.saveToFile(restoreFile);
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	static BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
	boolean isRunning = true;
	
	private void checkQuit(String input) {
		input = input.trim();if (input.length() > 0) {
			if (input.toLowerCase().charAt(0) == 'q') {
				try {
					in.close();
				} catch (Exception e) {}
				in = null;
				isRunning = false;
				System.exit(0);
			}
		}
	}
	
	private void draw() {
		// Draw a divider
		System.out.println("\n\n\n-----------------------------------------------------------------\n");
		
		// draw roll count
		System.out.println("Roll " + getRollCount() + " of " + getRules().getNumRollsPerRound());
		// draw the roll
		final int [] dice = getDiceRoll();
		final boolean [] keepers = getKeepers();
		drawDice(dice);
		System.out.println();
		// draw the keepers
		for (boolean keep : keepers) {
			System.out.print(String.format("%-" + DICE_SPACING + "s", keep ? " KEEP" : ""));
		}
		System.out.println();
		// draw the slots
		int index = 1;
		for (YahtzeeSlot slot : getAllSlots()) {
			System.out.println(String.format("%-2d %-20s %6s : %d", index++, slot.name(), isSlotUsed(slot) ? "CLOSED" : "", isSlotUsed(slot) ? getSlotScore(slot) : slot.getScore(getRules(), dice)));
		}
		System.out.println();
		// draw the score
		System.out.println(String.format(
						   "Yahtzees     %-5d\n" + 
						   "Upper Points %-5d\n" + 
						   "Bonus Points %-5d\n" +
						   "Total        %-5d\n" +
						   "Top Score    %-5d", getNumYahtzees(), getUpperPoints(), getBonusPoints(), getTotalPoints(), getTopScore()));

	}	

	private boolean isRunning() {
		return this.isRunning;
	}

	@Override
	public boolean onChooseKeepers(boolean[] keeprs) {
		
		System.out.print("\n\nChoose die nums to toggle seperated by a space or enter to continue\n> ");
		try {
			String line = in.readLine();
			if (line == null) {
				isRunning = false;
				return false;
			}
			
			if (line.length() == 0) {
				return true;
			}

			checkQuit(line);
			
			String [] parts = line.split(" ");
			for (int i=0; i<parts.length; i++) {
				if (parts[i].toLowerCase().charAt(0) == 'a') {
					Arrays.fill(keeprs, true);
					return true;
				}
				int num = Integer.parseInt(parts[i]);
				if (num > 0 && num <= keeprs.length) {
					keeprs[num-1] = ! keeprs[num-1];
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}

	@Override
	protected void onGameOver() {
		try {

			System.out.print("G A M E    O V E R\nPress enter to start a new game or q to exit\n> ");
			
			String line = in.readLine();
			if (line == null) {
				System.exit(1);
			}
			
			checkQuit(line);
			reset();
			
 		} catch (Exception e) {
 			e.printStackTrace();
 		}
	}
	
	@Override
	protected YahtzeeSlot onChooseSlotAssignment(List<YahtzeeSlot> choices) {
		System.out.print("\n\nChoose slot num to assign\n> ");
		
		try {
			
			String line = in.readLine();
			if (line == null) {
				isRunning= false;
				return null;
			}
			
			if (line.length() == 0)
				return null;

			checkQuit(line);
			Integer num = Integer.parseInt(line.trim());
			return getAllSlots().get(num-1);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}


	static final int DICE_SPACING = 10;
	static final String diceEdge = "+-----+";
	static final String [][] diceMiddle = {
			{ "|     |",
			  "|     |",
			  "|     |",
			},
			{ "|     |",
			  "|  o  |",
			  "|     |",
			},
			{ "|o    |",
			  "|     |",
			  "|    o|",
			},
			{ "|o    |",
			  "|  o  |",
			  "|    o|",
			},
			{ "|o   o|",
			  "|     |",
			  "|o   o|",
			},
			{ "|o   o|",
			  "|  o  |",
			  "|o   o|",
			},
			{ "|o   o|",
			  "|o   o|",
			  "|o   o|",
			},
			{ "|o   o|",
			  "|o o o|",
			  "|o   o|",
			},
			{ "|o o o|",
			  "|o   o|",
			  "|o o o|",
			},
			{ "|o o o|",
			  "|o o o|",
			  "|o o o|",
			},
			
	};
	
	static void drawDice(int ... roll) {
		final String diceSpacing = String.format("%" + (DICE_SPACING - diceEdge.length()) + "s", " ");
		for (int i=0; i<roll.length; i++) {
			System.out.print(String.format("%-" + DICE_SPACING + "s", "  [" + (i+1) + "]"));
		}
		System.out.println();
		for (int i=0; i<roll.length; i++) {
			System.out.print(diceEdge + diceSpacing);
		}
		System.out.println();
		for (int ii=0; ii<3; ii++) {
			for (int i=0; i<roll.length; i++) {
				System.out.print(diceMiddle[roll[i]][ii] + diceSpacing);
			}
			System.out.println();
		}
		for (int i=0; i<roll.length; i++) {
			System.out.print(diceEdge + diceSpacing);
		}
	}
	
}
