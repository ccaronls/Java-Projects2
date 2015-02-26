package cc.game.slappy;

import java.util.Vector;

import cc.lib.game.AGraphics;
import cc.lib.game.IImageFilter;
import cc.lib.game.Justify;
import cc.lib.game.Utils;
import cc.lib.swing.AWTGraphics;
import cc.lib.swing.EZFrame;
import cc.lib.swing.KeyboardAnimationApplet;

public class Main extends KeyboardAnimationApplet {

    public static void main(String [] args) {
        Utils.DEBUG_ENABLED = true;
        EZFrame frame = new EZFrame("Golf Card Game");
        KeyboardAnimationApplet app = new Main();
        frame.add(app);
        app.init();
        frame.centerToScreen(800, 600);
        app.start();
        app.setMillisecondsPerFrame(20);
    }   
    
	private final String [] negReplies = {
			"Ouch!",
			"Wait, Honey no!",
			"Please!",
			"Pleeeease!!!",
			"Stop Pummeling Me!"
	};
	
	private final String [] posReplies = {
			"Wow, honey!\nYou look so beautiful!",
			"I love you",
			"No honey.\nIll do the dishes",
			"You have had a\nlong day.  Let me\ndo that."
	};
	
	private final int BUBBLE_MIN_LIFE = 10;
	private final int SHADE_VARIANCE = 10;
	private final int NUM_SHADES = 255/SHADE_VARIANCE;
	private final int FADE = 3;
	
	private int faceId = -1;
	private int handId = -1;
	private int bubbleId = -1;
	private int [] slapId = new int[NUM_SHADES];
	private int lipsId = -1;
	private int [] kissId = new int[NUM_SHADES];
	
	private Vector<int []> slaps = new Vector<int[]>();
	private Vector<int []> kisses = new Vector<int[]>();
	
	private int bubbleX = 0;
	private int bubbleY = 0;
	private int bubbleEndFrame = 0;
	private String bubbleString = null;
	
	public void doInitialization() {
	}
	
	
	class AlphaFilter implements IImageFilter {
	    
	    private final int alpha;
	    
	    public AlphaFilter(int alpha) {
	        this.alpha = Utils.clamp(alpha, 0, 255); 
	    }
	    
	    public int filterRGBA(int x, int y, int rgb) 
	    {
	        int a = (rgb & 0xff000000) >> 24;
	        int r = (rgb & 0x00ff0000) >> 16;
	        int g = (rgb & 0x0000ff00) >> 8;
	        int b = rgb & 0x000000ff;
	        
	        if (a < alpha)
	            return rgb;
	        
	        if (r > 250 && g > 250 && b > 250)
	            return rgb;

	        return alpha<<24 | r<<16 | g<<8 | b;
	    }
	};
	void initImages(AGraphics g) {
	    if (faceId >= 0)
	        return;
		faceId = g.loadImage("images/face.jpg", null);
		handId = g.loadImage("images/hand.gif", g.WHITE);
		bubbleId = g.loadImage("images/bubble.gif", g.BLACK);
		lipsId = g.loadImage("images/lips1.gif", g.WHITE);
		for (int i=0; i<NUM_SHADES; i++) {
			IImageFilter filter = new AlphaFilter(255-i*SHADE_VARIANCE);
			slapId[i] = g.newTransformedImage(handId,filter);
			kissId[i] = g.newTransformedImage(lipsId, filter);			
		}
	}

	@Override
	public void onDimensionsChanged(AGraphics g, int width, int height) {
	    g.ortho();
	    initImages(g);
	}

	@Override
	public void drawFrame(AWTGraphics g) {
		
		final int width = getScreenWidth();
		final int height = getScreenHeight();
		
		this.clearScreen();
		g.drawImage(faceId, 0, 0, width, height);
		
		int hWidth = 128;
		int hHeight = 128;

		int mx = this.getMouseX() - hWidth/2;
		int my = this.getMouseY() - hHeight/2;

		// draw the slaps
		for (int i=0; i<slaps.size(); ){ 
			int [] slap = slaps.get(i);
			g.drawImage(slapId[slap[2]], slap[0], slap[1], hWidth, hHeight);
			
			if (getFrameNumber() % FADE == 0) {
				if (++slap[2] >= slapId.length) {
					slaps.remove(i);
					continue;
				} 
			}
			i++;
		}
		
		// draw the kisses
		for (int i=0; i<kisses.size(); ) {
			int[] kiss = kisses.get(i);
			g.drawImage(kissId[kiss[2]], kiss[0], kiss[1], hWidth, hHeight);
			
			if (getFrameNumber() % FADE == 0) { 
				if (++kiss[2] >= kissId.length) {
					kisses.remove(i);
					continue;
				} 
			}
			i++;
		}
		
		// draw any messages
		if (bubbleString != null) {
			g.drawImage(bubbleId, bubbleX-64, bubbleY-64, 128, 128);
			g.drawJustifiedString(bubbleX, bubbleY, Justify.CENTER, Justify.CENTER, bubbleString);
			if (bubbleEndFrame <= getFrameNumber())
				bubbleString = null;
		}
		
		// check input
		if (this.getMouseButtonClicked(0)) {
			int [] slap = { mx, my, slapId[0] };
			slaps.add(slap);
			bubbleX = Utils.randRange(100, width-100);
			bubbleY = Utils.randRange(100, height-100);
			bubbleString = this.negReplies[Utils.rand() % this.negReplies.length];
			bubbleEndFrame = getFrameNumber() + BUBBLE_MIN_LIFE + bubbleString.length();
		}
		
		if (this.getMouseButtonClicked(1)) {
			int [] kiss = { mx, my, kissId[0] };
			kisses.add(kiss);
			bubbleX = Utils.randRange(100, width-100);
			bubbleY = Utils.randRange(100, height-100);
			bubbleString = this.posReplies[Utils.rand() % this.posReplies.length];
			bubbleEndFrame = getFrameNumber() + BUBBLE_MIN_LIFE + bubbleString.length();
		}
	}
	
}
