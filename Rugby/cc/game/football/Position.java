package cc.game.football;

public enum Position {
	
	// defensive positions
	POS_DEF_END("DE", false, 2), // defensive ends line up on the outside of the line
	POS_DEF_TACKLE("DT", false, 2), // line up inside the ends
	POS_DEF_NOSE("DN", false, 1), // nose guard lines up opposite the off center
	POS_DEF_BACK("DB", false, 4), // lines up behind the line, can rush, play zone, man-to-man ect.
	POS_DEF_CORNER("DC", false, 2), // generally cover wide receivers
	POS_DEF_SAFTY("DS", false, 2), // last line of defense (this encapsulates safty and strong safty)

	// offense player positions
	POS_OFF_QB("QB", true, 1), // quarter back
	POS_OFF_CENTER("CTR", true, 1), // center
	POS_OFF_RB("RB", true, 2), // running back (the encapsulates both rb and fullback positions)
	POS_OFF_WR("WR", true, 4), // wide reciever
	POS_OFF_TE("TE", true, 2), // tight end
	POS_OFF_GUARD("GD", true, 4), // offensiver guard, lines up on either side of center
	POS_OFF_TACKLE("TKL", true, 2); // tackles line up on either side of the guards

	private Position(String id, boolean offense, int max) {
		this.id = id;
		this.offense = offense;
		this.max = max;
	}
	
	public final String id;
	public final boolean offense;
	public final int max;
}
