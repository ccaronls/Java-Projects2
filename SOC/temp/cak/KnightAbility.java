package cc.game.soc.core.cak;

public enum KnightAbility {

	Move, // knight can move to any unoccupied vertex connected to current location by a road or ship
	Displace, // knight can occupy position of opponent knight of lesser rank, forcing the opponent to perform a move.  If there are no moves then the knight is lost.
	Chase // place knight or vertex adjacent to pirate or robber, then player can place the pirate or robber like a soldier card
	
}
