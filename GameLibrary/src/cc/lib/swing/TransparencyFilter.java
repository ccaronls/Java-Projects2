package cc.lib.swing;

import java.awt.Color;
import java.awt.image.RGBImageFilter;

public class TransparencyFilter extends RGBImageFilter {

	int targetColor;
	
	public TransparencyFilter(Color color) {
		this(AWTUtils.colorToInt(color));
	}
	
	public TransparencyFilter(int color) {
		targetColor = color & 0x00ffffff;
	}	
	
	public int filterRGB(int x, int y, int rgb) {
	    int c = (rgb & 0x00ffffff);
		if (c == targetColor) {
			return 0;
		}
		return rgb;
	}
	
}
