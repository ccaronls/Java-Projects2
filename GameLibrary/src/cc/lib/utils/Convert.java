package cc.lib.utils;

public class Convert {
	
	public final static float degreesToCelcius(float degrees) {
		return 5f * (degrees - 32) / 9;
	}
	
	public final static int celciusToDegrees(float celcius) {
		return Math.round(celcius * 9f/5 + 32);
	}
}
