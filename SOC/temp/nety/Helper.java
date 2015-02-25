package cc.game.soc.nety;

public class Helper {

	static String trimString(String in) {
		if (in.length() < 64)
			return in;
		return in.substring(0, 64) + "...";
	}
	
}
