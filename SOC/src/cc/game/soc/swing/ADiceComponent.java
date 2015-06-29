package cc.game.soc.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import cc.game.soc.core.DiceEvent;
import cc.game.soc.core.DiceType;
import cc.lib.swing.ImageColorFilter;
import cc.lib.swing.ImageMgr;

public final class ADiceComponent extends JComponent implements ChangeListener, MouseListener {

	public static interface DiceChangedListener {
		void onDiceChanged(int numDieNum);
	}
	
	private int die=0;
	private DiceChangedListener listener;
	private boolean highlighted = false;
	private DiceType type;
    private final int dotSize; 
	
	private static int shipImageId = -1;
	private static int redCityImageId = -1;
	private static int greenCityImageId = -1;
	private static int blueCityImageId = -1;

	private static void initImages() {
		if (shipImageId < 0) {
			ImageMgr images = GUI.instance.images;
			shipImageId = images.loadImage("dicesideship2.GIF");
			int cityId = images.loadImage("dicesidecity2.GIF");
			redCityImageId = images.addImage(images.transform(images.getImage(cityId), new ImageColorFilter(Color.WHITE, Color.RED)));
			greenCityImageId = images.addImage(images.transform(images.getImage(cityId), new ImageColorFilter(Color.WHITE, Color.GREEN)));
			blueCityImageId = images.addImage(images.transform(images.getImage(cityId), new ImageColorFilter(Color.WHITE, Color.BLUE)));
			images.deleteImage(cityId);
		}
	}

	
	ADiceComponent() {
		setMinimumSize(new Dimension(30,30));
		setPreferredSize(new Dimension(60, 30));
		this.type = DiceType.None;
        this.dotSize  = GUI.instance.getProps().getIntProperty("dice.dotsize", 4);
	}
	
	void setDie(int die) {
		this.die = die;
		repaint();
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
	public void paint(Graphics g) {
		int w = getWidth();
		int h = getHeight();

		final int spacing = 5;
		final int dim = h - spacing;
		int x = w/2 - spacing/2 - dim;
		int y = 0;
		
		if (die != 0) {
			drawDie(g, x, y, dim);
		}

		if (highlighted) {
			g.setColor(Color.CYAN);
			g.drawRect(x, y, dim, dim);
		}
	}

	public void drawDie(Graphics g, int x, int y, int dim) {
		switch (type) {
			case Event:
				drawEventDie(g, x, y, dim, getDie());
				break;
			case RedYellow:
				drawDie(g, x, y, dim, dotSize, Color.RED, Color.YELLOW, getDie());
				break;
			case WhiteBlack:
				drawDie(g, x, y, dim, dotSize, Color.WHITE, Color.BLACK, getDie());
				break;
			case YellowRed:
				drawDie(g, x, y, dim, dotSize, Color.YELLOW, Color.RED, getDie());
				break;
			case None:
				break;
		}
	}

	@Override
	public final void stateChanged(ChangeEvent arg) {
		JSpinner spinner = (JSpinner)arg.getSource();
		this.die = (Integer)spinner.getValue();
		repaint();
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		die = (die+1)%6+1;
		repaint();
	}

	public DiceType getType() {
		return this.type;
	}
	
	@Override
	public void mousePressed(MouseEvent e) {}

	@Override
	public void mouseReleased(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {
		highlighted = true;
		repaint();
	}

	@Override
	public void mouseExited(MouseEvent e) {
		highlighted = true;
		repaint();
	}
	
	public final void setDicePickerEnabled(boolean enabled) {
		if (enabled) {
			addMouseListener(this);
		} else {
			removeMouseListener(this);
		}
	}
	
	private static void drawDot(Graphics g, int x, int y, int dotSize) {
		g.fillOval(x-dotSize/2,y-dotSize/2,dotSize,dotSize);
	}

	public static void drawEventDie(Graphics g, int x, int y, int dim, int dieNum) {
		initImages();
		g.setColor(Color.WHITE);
		int arc = dim/4;
	    g.fillRoundRect(x, y, dim, dim, arc, arc);
	    ImageMgr images = GUI.instance.images;
	    switch (DiceEvent.fromDieNum(dieNum)) {
			case AdvanceBarbarianShip:
	    		images.drawImage(g, shipImageId, x, y, dim, dim);
				break;
			case PoliticsCard:
				images.drawImage(g, blueCityImageId, x, y, dim, dim);
				break;
			case ScienceCard:
				images.drawImage(g, greenCityImageId, x, y, dim, dim);
				break;
			case TradeCard:
	    		images.drawImage(g, redCityImageId, x, y, dim, dim);
				break;
	    }
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
