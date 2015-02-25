package cc.game.soc.swing;

public enum MenuOp {

	// common
	BACK("Back"),
	EXIT("Exit"),
	NEW("New Game"),
	RESTORE("Restore"),
	CONFIG_BOARD("Configure Board"),
	CONFIG_SETTINGS("Config Settings"),
	CHOOSE_NUM_PLAYERS("--"),
	CHOOSE_COLOR("--"),
	START("Start"),
	GEN_HEX_BOARD("New Hexagon"),
	GEN_HEX_BOARD_SMALL("Small"),
	GEN_HEX_BOARD_MEDIUM("Medium"),
	GEN_HEX_BOARD_LARGE("Large"),
	GEN_RECT_BOARD("New Rectangle"),
	GEN_RECT_BOARD_SMALL("Small"),
	GEN_RECT_BOARD_MEDIUM("Medium"),
	GEN_RECT_BOARD_LARGE("Large"),
	FINALIZE_BOARD("Finalize Board"),
	SAVE_BOARD_AS_DEFAULT("Save as Default"),
	LOAD_DEFAULT("Load Default"),
	SAVE_BOARD("Save"),
	SAVE_BOARD_AS("Save as New"),
	LOAD_BOARD("Load Board"),
	SET_PICKMODE("--"),
    POPUPBUTTON("--"),
    SAVE_SCENARIO("Save Scenario"),
    LOAD_SCENARIO("Load Scenario"),
    
	// in game options
	QUIT("Quit"),
	CANCEL("Cancel"),
	CHOOSE_MOVE("--"),
	CHOOSE_GIVEUP_CARD("--"),
	CHOOSE_PLAYER("--"),
	CHOOSE_CARD("--"),
	CHOOSE_TRADE("--"),
	BUILDABLES_POPUP("Buildables"), 
	DEBUG_BOARD("Debug Board"),
	RESET_BOARD("Reset Board"),
	RESET_BOARD_ISLANDS("Reset Islands"),
	CHOOSE_SHIP("Ships"),
	CHOOSE_ROAD("Roads"),
	SET_DICE("Set Dice")
	
	;
	
	private MenuOp(String txt) {
		this.txt = txt;
	}
	
	final String txt;
}
