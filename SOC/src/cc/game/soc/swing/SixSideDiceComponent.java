package cc.game.soc.swing;

import java.awt.Color;
import java.awt.Graphics;


@SuppressWarnings("serial")
public class SixSideDiceComponent extends ADiceComponent {

    private final Color dieColor, dotColor;
    private final int dotSize; 
    
    SixSideDiceComponent(Color dieColor, Color dotColor) {
//        this.bkColor  = GUI.instance.getProps().getColorProperty("dice.bkcolor", Color.LIGHT_GRAY);
        this.dieColor = dieColor;
        this.dotColor = dotColor;
        this.dotSize  = GUI.instance.getProps().getIntProperty("dice.dotsize", 4);
    }
    
	private void drawDot(Graphics g, int x, int y) {
		g.fillOval(x-dotSize/2,y-dotSize/2,dotSize,dotSize);
	}
	

	@Override
	void drawDie(Graphics g, int x, int y, int dim)
	{
	    g.setColor(dieColor);
	    g.fillRect(x, y, dim, dim);
	    g.setColor(dotColor);
	    int dd2 = dim/2;
	    int dd4 = dim/4;
	    int dd34 = (dim*3)/4;
	    switch (getDie()) {
	    case 1:	    	
	        drawDot(g, x+dd2, y+dd2);	    	
	        break;
	    case 2:
	        drawDot(g, x+dd4, y+dd4);
	        drawDot(g, x+dd34, y+dd34);
	        break;
	    case 3:
	        drawDot(g, x+dd4, y+dd4);
	        drawDot(g, x+dd2, y+dd2);
	        drawDot(g, x+dd34, y+dd34);
	        break;
	    case 4:
	        drawDot(g, x+dd4, y+dd4);
	        drawDot(g, x+dd34, y+dd34);
	        drawDot(g, x+dd4, y+dd34);
	        drawDot(g, x+dd34, y+dd4);
	        break;
	    case 5:
	        drawDot(g, x+dd4, y+dd4);
	        drawDot(g, x+dd34, y+dd34);
	        drawDot(g, x+dd4, y+dd34);
	        drawDot(g, x+dd34, y+dd4);
	        drawDot(g, x+dd2, y+dd2);
	        break;
	    case 6:
	        drawDot(g, x+dd4, y+dd4);
	        drawDot(g, x+dd34, y+dd34);
	        drawDot(g, x+dd4, y+dd34);
	        drawDot(g, x+dd34, y+dd4);
	        drawDot(g, x+dd4, y+dd2);
	        drawDot(g, x+dd34, y+dd2);
	        break;
	    default:
	        assert(false);// && "Invalid die");
	        break;
	    }
	    g.drawRect(x, y, dim, dim);
	}

	

}
