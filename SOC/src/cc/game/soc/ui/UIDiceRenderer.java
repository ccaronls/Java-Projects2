package cc.game.soc.ui;

import cc.game.soc.core.DiceEvent;
import cc.game.soc.core.DiceType;
import cc.game.soc.swing.GUI;
import cc.lib.game.AGraphics;
import cc.lib.game.APGraphics;
import cc.lib.game.GColor;
import cc.lib.game.Utils;
import cc.lib.swing.ImageColorFilter;
import cc.lib.swing.ImageMgr;

public final class UIDiceRenderer implements UIRenderer {

	public interface DiceChangedListener {
		void onDiceChanged(int numDieNum);
	}
	
	private int die=0;
	private DiceChangedListener listener;
	private boolean highlighted = false;
	private DiceType type;
    private final int dotSize;
	
	private int shipImageId = -1;
	private int redCityImageId = -1;
	private int greenCityImageId = -1;
	private int blueCityImageId = -1;

	public void initImages(int shipImageId, int redcityImageId, int greenCityImageId, int blueCityImageId) {
	    this.shipImageId = shipImageId;
	    this.redCityImageId = redcityImageId;
	    this.greenCityImageId = greenCityImageId;
	    this.blueCityImageId = blueCityImageId;
    }

	private final UIComponent component;

	public UIDiceRenderer(UIComponent component) {
		this.type = DiceType.None;
        this.dotSize  = 4;
        this.component = component;
        this.component.setRenderer(this);
	}
	
	void setDie(int die) {
		this.die = die;
		component.redraw();
		if (listener != null)
			listener.onDiceChanged(die);
	}

	int getDie() {
		return die;
	}

	public void setType(DiceType type) {
		this.type = type;
	}

	public void setListener(DiceChangedListener listener) {
		this.listener = listener;
	}
	
	@Override
	public void draw(APGraphics g, int pickX, int pickY) {
		int w = g.getViewportHeight();
		int h = g.getViewportHeight();

		final int spacing = 5;
		final int dim = h - spacing;
		int x = w/2 - spacing/2 - dim;
		int y = 0;
		
		if (die != 0) {
			drawDie(g, x, y, dim);
		}

		if (highlighted) {
			g.setColor(GColor.CYAN);
			g.drawRect(x, y, dim, dim);
		}

        highlighted = Utils.isPointInsideRect(pickX, pickY, 0, 0, w, h);
    }

	public void drawDie(AGraphics g, float x, float y, float dim) {
		switch (type) {
			case Event:
				drawEventDie(g, x, y, dim, getDie());
				break;
			case RedYellow:
				drawDie(g, x, y, dim, dotSize, GColor.RED, GColor.YELLOW, getDie());
				break;
			case WhiteBlack:
				drawDie(g, x, y, dim, dotSize, GColor.WHITE, GColor.BLACK, getDie());
				break;
			case YellowRed:
				drawDie(g, x, y, dim, dotSize, GColor.YELLOW, GColor.RED, getDie());
				break;
		}
	}

    public void doClick() {
	    if (pickerEnbled) {
            if (++die > 6)
                die = 1;
            component.redraw();
        }
	}

	public DiceType getType() {
		return this.type;
	}

	private boolean pickerEnbled = false;

	public void setDicePickerEnabled(boolean enabled) {
	    this.pickerEnbled = enabled;
    }
	
	private void drawDot(AGraphics g, float x, float y, float dotSize) {
		g.drawFilledOval(x-dotSize/2,y-dotSize/2,dotSize,dotSize);
	}

	public void drawEventDie(AGraphics g, float x, float y, float dim, int dieNum) {
		g.setColor(GColor.WHITE);
		float arc = dim/4;
	    g.drawFilledRoundedRect(x, y, dim, dim, arc);
	    switch (DiceEvent.fromDieNum(dieNum)) {
			case AdvanceBarbarianShip:
	    		g.drawImage(shipImageId, x, y, dim, dim);
				break;
			case PoliticsCard:
				g.drawImage(blueCityImageId, x, y, dim, dim);
				break;
			case ScienceCard:
				g.drawImage(greenCityImageId, x, y, dim, dim);
				break;
			case TradeCard:
	    		g.drawImage(redCityImageId, x, y, dim, dim);
				break;
	    }
	}
	
	public void drawDie(AGraphics g, float x, float y, float dim, int dotSize, GColor dieColor, GColor dotColor, int numDots) {
	    g.setColor(dieColor);
	    float arc = dim/4;
	    g.drawFilledRoundedRect(x, y, dim, dim, arc);
	    g.setColor(dotColor);
	    float dd2 = dim/2;
	    float dd4 = dim/4;
	    float dd34 = (dim*3)/4;
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
	    g.drawFilledRoundedRect(x, y, dim, dim, arc);
	}

	void setBounds(float x, float y, float w, float h) {
	    component.setBounds(x, y, w, h);
    }

}
