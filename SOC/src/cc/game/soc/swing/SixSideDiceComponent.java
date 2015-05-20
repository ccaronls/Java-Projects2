package cc.game.soc.swing;

import java.awt.Color;
import java.awt.Graphics;


@SuppressWarnings("serial")
public class SixSideDiceComponent extends ADiceComponent {

    private final Color dieColor, dotColor;
    private final int dotSize; 
    
    SixSideDiceComponent(Color dieColor, Color dotColor) {
        this.dieColor = dieColor;
        this.dotColor = dotColor;
        this.dotSize  = GUI.instance.getProps().getIntProperty("dice.dotsize", 4);
    }
    
	@Override
	void drawDie(Graphics g, int x, int y, int dim)
	{
		drawDie(g, x, y, dim, dotSize, dieColor, dotColor, getDie());
	}

	private static void drawDot(Graphics g, int x, int y, int dotSize) {
		g.fillOval(x-dotSize/2,y-dotSize/2,dotSize,dotSize);
	}

	public static void drawDie(Graphics g, int x, int y, int dim, int dotSize, Color dieColor, Color dotColor, int numDots) {
	    g.setColor(dieColor);
	    int arc = dim/4;
	    g.fillRoundRect(x, y, dim, dim, arc, arc);
	    g.setColor(dotColor);
	    int dd2 = dim/2;
	    int dd4 = dim/4;
	    int dd34 = (dim*3)/4;
	    switch (numDots) {
	    case 1:	    	
	        drawDot(g, x+dd2, y+dd2, dotSize);	    	
	        break;
	    case 2:
	        drawDot(g, x+dd4, y+dd4, dotSize);
	        drawDot(g, x+dd34, y+dd34, dotSize);
	        break;
	    case 3:
	        drawDot(g, x+dd4, y+dd4, dotSize);
	        drawDot(g, x+dd2, y+dd2, dotSize);
	        drawDot(g, x+dd34, y+dd34, dotSize);
	        break;
	    case 4:
	        drawDot(g, x+dd4, y+dd4, dotSize);
	        drawDot(g, x+dd34, y+dd34, dotSize);
	        drawDot(g, x+dd4, y+dd34, dotSize);
	        drawDot(g, x+dd34, y+dd4, dotSize);
	        break;
	    case 5:
	        drawDot(g, x+dd4, y+dd4, dotSize);
	        drawDot(g, x+dd34, y+dd34, dotSize);
	        drawDot(g, x+dd4, y+dd34, dotSize);
	        drawDot(g, x+dd34, y+dd4, dotSize);
	        drawDot(g, x+dd2, y+dd2, dotSize);
	        break;
	    case 6:
	        drawDot(g, x+dd4, y+dd4, dotSize);
	        drawDot(g, x+dd34, y+dd34, dotSize);
	        drawDot(g, x+dd4, y+dd34, dotSize);
	        drawDot(g, x+dd34, y+dd4, dotSize);
	        drawDot(g, x+dd4, y+dd2, dotSize);
	        drawDot(g, x+dd34, y+dd2, dotSize);
	        break;
	    default:
	        assert(false);// && "Invalid die");
	        break;
	    }
	    g.drawRoundRect(x, y, dim, dim, arc, arc);
	}

	

}
