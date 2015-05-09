package cc.game.soc.swing;

public enum MenuOp {

	// common
	BACK("Back", null),
	EXIT("Exit", "Exit the Application"),
	NEW("New Game", "Start a new game"),
	RESTORE("Restore", "Restore previously saved game"),
	CONFIG_BOARD("Configure Board", "Open configure board mode"),
	CONFIG_SETTINGS("Config Settings", "Configure game settings"),
	CHOOSE_NUM_PLAYERS("--", null),
	CHOOSE_COLOR("--", null),
	START("Start", "Start the game"),
	GEN_HEX_BOARD("New Hexagon", "Generate a hexagon shaped board"),
	GEN_HEX_BOARD_SMALL("Small", "Generate a small hexagon shaped board"),
	GEN_HEX_BOARD_MEDIUM("Medium", "Generate a medium hexagon shaped board"),
	GEN_HEX_BOARD_LARGE("Large", "Generate a large hexagon shaped board"),
	GEN_RECT_BOARD("New Rectangle", "Generate a rectangular shaped board"),
	GEN_RECT_BOARD_SMALL("Small", "Generate a small rectangular shaped board"),
	GEN_RECT_BOARD_MEDIUM("Medium", "Generate a medium rectangular shaped board"),
	GEN_RECT_BOARD_LARGE("Large", "Generate a large shaped board"),
	FINALIZE_BOARD("Finalize Board", "Commit board"),
	SAVE_BOARD_AS_DEFAULT("Save as Default", "Save current board as default board"),
	LOAD_DEFAULT("Load Default", "Load the default board"),
	SAVE_BOARD("Save", "Overwrite board changes"),
	SAVE_BOARD_AS("Save as New", "Save as a new board"),
	LOAD_BOARD("Load Board", "Load a board"),
	SET_PICKMODE("--", null),
    POPUPBUTTON("--", null),
    SAVE_SCENARIO("Save Scenario", "Save the current board and game configuration as a scenario"),
    LOAD_SCENARIO("Load Scenario", "Load a current scenario board and game configuration"),
    
	// in game options
	QUIT("Quit", "Quit current game"),
	CANCEL("Cancel", "Cancel current operation"),
	CHOOSE_MOVE("--", null),
	CHOOSE_GIVEUP_CARD("--", null),
	CHOOSE_PLAYER("--", null),
	CHOOSE_CARD("--", null),
	CHOOSE_TRADE("--", null),
	BUILDABLES_POPUP("Buildables", "Show the buildables popup"), 
	DEBUG_BOARD("Debug Board", "Open board debugging screen"),
	RESET_BOARD("Reset Board", "Clear current board of structures and routes"),
	RESET_BOARD_ISLANDS("Reset Islands", "Remove island "),
	CHOOSE_SHIP("Ships", "Show ship choices"),
	CHOOSE_ROAD("Roads", "Show road choices"),
	SET_DICE("Set Dice", null),
    REWIND_GAME("Rewind Game", "Rewind the game to previous state"),
	
	;
	
	private MenuOp(String txt, String toolTipText) {
		this.txt = txt;
		this.toolTipText = toolTipText;
	}
	
	final String txt;
	final String toolTipText;
}
