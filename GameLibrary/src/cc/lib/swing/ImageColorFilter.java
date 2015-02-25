package cc.lib.swing;

import java.awt.image.RGBImageFilter;
import java.awt.Color;

/**
 * This class can be used to transform all of a specific color to another color with variation.
 * 
 * Example:
 * 
 * Image transformed = ImageMgr.this.transform(srcImage, new ImageColorFilter(Color.RED, Color.BLUE, 0));
 * 
 * @author ccaron
 *
 */
public class ImageColorFilter extends RGBImageFilter {

	private int oldColor;
	private int newColor;
	private int variance;
	
	/**
	 * 
	 * @param oldColor
	 * @param newColor
	 * @param variance
	 */
	public ImageColorFilter(int oldColor, int newColor, int variance) {
		this.oldColor = oldColor;
		this.newColor = newColor;
		this.variance = variance;
	}
	
	/**
	 * 
	 * @param oldColor
	 * @param newColor
	 * @param variance
	 */
	public ImageColorFilter(Color oldColor, int newColor, int variance) {
		this(AWTUtils.colorToInt(oldColor), newColor, variance);
	}
	
	/**
	 * 
	 * @param oldColor
	 * @param newColor
	 * @param variance
	 */
	public ImageColorFilter(Color oldColor, Color newColor, int variance) {
		this(AWTUtils.colorToInt(oldColor), AWTUtils.colorToInt(newColor), variance);
	}
	
	/**
	 * 
	 * @param oldcolor
	 * @param newColor
	 */
	public ImageColorFilter(Color oldcolor, Color newColor) {
		this(oldcolor, newColor, 0);
	}

	@Override
	public final int filterRGB(int x, int y, int rgb) 
	{
		int a = (rgb & 0xff000000) >> 24;
		int r = rgb & 0x00ff0000;
		int g = rgb & 0x0000ff00;
		int b = rgb & 0x000000ff;
		
		if (a == 0)
			return rgb;
		
		int dr = Math.abs(r - (oldColor & 0x00ff0000));
		int dg = Math.abs(g - (oldColor & 0x0000ff00));
		int db = Math.abs(b - (oldColor & 0x000000ff));
		
		if ( dr <= variance && dg <= variance && db <= variance )
		{
//			System.out.println(String.format("Converting color %x", rgb));
			return newColor;
		}
		else 
		{
//			System.out.println(String.format("Not Converting color %x", rgb));
			return rgb;
		}
	}
	
}
