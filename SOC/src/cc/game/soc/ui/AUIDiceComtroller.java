package cc.game.soc.ui;

import cc.game.soc.core.DiceEvent;
import cc.game.soc.core.DiceType;
import cc.lib.game.AColor;
import cc.lib.game.AGraphics;
import cc.lib.game.ImageColorFilter;

public final class AUIDiceComtroller implements UIEventHandler {

	public static interface DiceChangedListener {
		void onDiceChanged(int numDieNum);
	}
	
	private int die=0;
	private DiceChangedListener listener;
	private boolean highlighted = false;
	private DiceType type = DiceType.WhiteBlack;
	
	private static int shipImageId = -1;
	private static int redCityImageId = -1;
	private static int greenCityImageId = -1;
	private static int blueCityImageId = -1;

	final UIWidget widget;
	
	public AUIDiceComtroller(UIWidget widget) {
		this.widget = widget;
		widget.setUIEventHandler(this);
	}
	
	private static void initImages(AGraphics g) {
		if (shipImageId < 0) {
			shipImageId = g.loadImage("dicesideship2.GIF");
			int cityId = g.loadImage("dicesidecity2.GIF");
			redCityImageId = g.newTransformedImage(cityId, new ImageColorFilter(g.WHITE, g.RED, 10));
			greenCityImageId = g.newTransformedImage(cityId, new ImageColorFilter(g.WHITE, g.GREEN, 10));
			blueCityImageId = g.newTransformedImage(cityId, new ImageColorFilter(g.WHITE, g.BLUE, 10));
			g.deleteImage(cityId);
		}
	}

	public void setDie(int die) {
		this.die = die;
		widget.repaint();
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
	
	public void paint(AGraphics g) {
		int w = widget.getWidth();
		int h = widget.getHeight();

		final int spacing = 5;
		final int dim = h - spacing;
		int x = w/2 - spacing/2 - dim;
		int y = 0;
		
		if (die != 0) {
			drawDie(g, x, y, dim);
		}

		if (highlighted) {
			g.setColor(g.CYAN);
			g.drawRect(x, y, dim, dim);
		}
	}

	public void drawDie(AGraphics g, int x, int y, int dim) {
		int dotSize = Math.min(widget.getWidth(), widget.getHeight()) / 8;
		switch (type) {
			case Event:
				drawEventDie(g, x, y, dim, getDie());
				break;
			case RedYellow:
				drawDie(g, x, y, dim, dotSize, g.RED, g.YELLOW, getDie());
				break;
			case WhiteBlack:
				drawDie(g, x, y, dim, dotSize, g.WHITE, g.BLACK, getDie());
				break;
			case YellowRed:
				drawDie(g, x, y, dim, dotSize, g.YELLOW, g.RED, getDie());
				break;
		}
	}

	/*
	@Override
	public final void stateChanged(ChangeEvent arg) {
		JSpinner spinner = (JSpinner)arg.getSource();
		this.die = (Integer)spinner.getValue();
		repaint();
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (++die > 6)
			die = 1;
		repaint();
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		highlighted = true;
		widget.repaint();
	}

	@Override
	public void mouseExited(MouseEvent e) {
		highlighted = true;
		widget.repaint();
	}*/

	public DiceType getType() {
		return this.type;
	}

	boolean pickEnabled = false;
	
	public final void setDicePickerEnabled(boolean enabled) {
		pickEnabled = enabled;
	}
	
	@Override
	public void onTouch(int x, int y) {
		highlighted = true;
		widget.repaint();
	}
	
	@Override
	public void onUntouched() {
		highlighted = false;
		widget.repaint();
	}

	@Override
	public void onPressed(int x, int y) {
		if (pickEnabled) {
			if (++die > 6)
				die = 1;
			widget.repaint();
		}
	}

	private static void drawDot(AGraphics g, int x, int y, int dotSize) {
		g.drawFilledOval(x-dotSize/2,y-dotSize/2,dotSize,dotSize);
	}

	public static void drawEventDie(AGraphics g, int x, int y, int dim, int dieNum) {
		initImages(g);
		g.setColor(g.WHITE);
		int arc = dim/4;
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
	
	public static void drawDie(AGraphics g, int x, int y, int dim, int dotSize, AColor dieColor, AColor dotColor, int numDots) {
	    g.setColor(dieColor);
	    int arc = dim/4;
	    g.drawFilledRoundedRect(x, y, dim, dim, arc);
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
	    g.drawRoundedRect(x, y, dim,  dim,  1,  arc);
	}

}
