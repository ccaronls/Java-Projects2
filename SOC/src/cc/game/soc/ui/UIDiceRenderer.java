package cc.game.soc.ui;

import javax.rmi.CORBA.Util;

import cc.game.soc.core.Dice;
import cc.game.soc.core.DiceEvent;
import cc.game.soc.core.DiceType;
import cc.game.soc.swing.GUI;
import cc.lib.game.AAnimation;
import cc.lib.game.AGraphics;
import cc.lib.game.APGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GDimension;
import cc.lib.game.GRectangle;
import cc.lib.game.Utils;
import cc.lib.swing.ImageColorFilter;
import cc.lib.swing.ImageMgr;

public final class UIDiceRenderer implements UIRenderer {

	private int shipImageId = -1;
	private int redCityImageId = -1;
	private int greenCityImageId = -1;
	private int blueCityImageId = -1;
	private float dotSize = 4;
	private GDimension diceRect = null;

	private Dice [] dice = new Dice[0];
    private final UIComponent component;
    private int picked = -1;
    private int pickable = 0;

	public void initImages(int shipImageId, int redcityImageId, int greenCityImageId, int blueCityImageId) {
	    this.shipImageId = shipImageId;
	    this.redCityImageId = redcityImageId;
	    this.greenCityImageId = greenCityImageId;
	    this.blueCityImageId = blueCityImageId;
    }
    
    public void initStyle(float dotSize) {
	    this.dotSize = dotSize;
    }


	public UIDiceRenderer(UIComponent component) {
        this.component = component;
        this.component.setRenderer(this);
	}

	public void setDice(Dice ... dice) {
	    this.dice = dice;
	    component.redraw();
    }

    public void setPickableDice(int num) {
        this.pickable = num;
        component.redraw();
    }

    public int [] getPickedDiceNums() {
	    int [] nums = new int[pickable];
	    for (int i=0; i<pickable; i++) {
	        nums[i] = dice[i].getNum();
        }
        return nums;
    }

	public void spinDice(long spinTimeMs, final Dice ... which) {
	    spinAnim = new AAnimation<APGraphics>(spinTimeMs) {

	        long delay = 10;

            @Override
            protected void draw(APGraphics g, float position, float dt) {

                for (Dice d : which) {
                    d.setNum(1 + Utils.rand()%6);
                }
                drawPrivate(g, 0, 0, which);

                new Thread() {
                    public void run() {
                        try {
                            synchronized (this) {
                                wait(delay);
                            }
                        } catch (Exception e) {
                        }
                        delay += 20;
                        component.redraw();
                    }
                }.start();
            }

            @Override
            public void onDone() {
                synchronized (this) {
                    notifyAll();
                }
            }
        }.start();
	    component.redraw();
	    try {
	        synchronized (spinAnim) {
	            spinAnim.wait(spinTimeMs+500);
            }
        } catch (Exception e) {}
    }

    private AAnimation<APGraphics> spinAnim = null;

    public void setDiceRect(GDimension rect) {
	    this.diceRect = rect;
    }

    private GDimension getDim() {
	    return diceRect != null ? diceRect : new GDimension(component.getWidth(), component.getHeight());
    }

	@Override
	public void draw(APGraphics g, int pickX, int pickY) {
        picked = -1;
        if (Utils.isEmpty(dice))
            return;

        if (spinAnim != null) {
            if (spinAnim.isDone()) {
                spinAnim = null;
            } else {
                spinAnim.update(g);
                return;
            }
        }

        drawPrivate(g, pickX, pickY, dice);
    }

    private void drawPrivate(APGraphics g, int pickX, int pickY, Dice [] dice) {

	    g.pushMatrix();
        {
            GDimension dim = getDim();

            float dieDim = dim.height;
            if (dieDim*dice.length + (dieDim/4*(dice.length+1)) > dim.width) {
                dieDim = (4*dim.width) / (5*dice.length + 1);
                g.translate(0, dim.height/2 - dieDim/2);
            }
            float spacing = dieDim/4;

            g.translate(spacing, 0);

            g.pushMatrix();
            {
                g.begin();
                for (int index = 0; index<dice.length; index++) {
                    g.setName(index);
                    g.vertex(0, 0);
                    g.vertex(dieDim, dieDim);
                    g.translate(dieDim + spacing, 0);
                }
            }
            g.popMatrix();
            picked = g.pickRects(pickX, pickY);

            int index = 0;
            for (Dice d : dice) {
                drawDie(g, dieDim, d.getType(), d.getNum());
                if (index == picked) {
                    g.setColor(GColor.RED);
                    g.pushMatrix();
                    g.scale(1.1f, 1.1f);
                    g.drawRoundedRect(0, 0, dieDim, dieDim, 1, dieDim/4);
                    g.popMatrix();
                }
                g.translate(dieDim+ spacing, 0);
            }
        }
        g.popMatrix();
    }

	public void drawDie(AGraphics g, float dim, DiceType type, int num) {
		switch (type) {
			case Event:
				drawEventDie(g, dim, num);
				break;
			case RedYellow:
				drawDie(g, dim, GColor.RED, GColor.YELLOW, num);
				break;
			case WhiteBlack:
				drawDie(g, dim, GColor.WHITE, GColor.BLACK, num);
				break;
			case YellowRed:
				drawDie(g, dim, GColor.YELLOW, GColor.RED, num);
				break;
		}
	}

    public void doClick() {
	    if (dice != null && picked >= 0 && picked < dice.length) {
	        int num = dice[picked].getNum();
	        if (++num > 6) {
	            num = 1;
            }
            dice[picked].setNum(num);
        }
        component.redraw();
	}

	public void drawEventDie(AGraphics g, float dim, int dieNum) {
		g.setColor(GColor.WHITE);
		float arc = dim/4;
	    g.drawFilledRoundedRect(0, 0, dim, dim, arc);
	    switch (DiceEvent.fromDieNum(dieNum)) {
			case AdvanceBarbarianShip:
	    		g.drawImage(shipImageId, 0, 0, dim, dim);
				break;
			case PoliticsCard:
				g.drawImage(blueCityImageId, 0, 0, dim, dim);
				break;
			case ScienceCard:
				g.drawImage(greenCityImageId, 0, 0, dim, dim);
				break;
			case TradeCard:
	    		g.drawImage(redCityImageId, 0, 0, dim, dim);
				break;
	    }
	}
	
	public void drawDie(AGraphics g, float dim, GColor dieColor, GColor dotColor, int numDots) {
	    g.setColor(dieColor);
	    float arc = dim/4;
	    g.drawFilledRoundedRect(0, 0, dim, dim, arc);
	    g.setColor(dotColor);
	    float dd2 = dim/2;
	    float dd4 = dim/4;
	    float dd34 = (dim*3)/4;
	    float oldDotSize = g.setPointSize(dotSize);
	    g.begin();
	    switch (numDots) {
	    case 1:	    	
	        g.vertex(dd2, dd2);
	        break;
	    case 2:
	        g.vertex(dd4, dd4);
	        g.vertex(dd34, dd34);
	        break;
	    case 3:
	        g.vertex(dd4, dd4);
	        g.vertex(dd2, dd2);
	        g.vertex(dd34, dd34);
	        break;
	    case 4:
	        g.vertex(dd4, dd4);
	        g.vertex(dd34, dd34);
	        g.vertex(dd4, dd34);
	        g.vertex(dd34, dd4);
	        break;
	    case 5:
	        g.vertex(dd4, dd4);
	        g.vertex(dd34, dd34);
	        g.vertex(dd4, dd34);
	        g.vertex(dd34, dd4);
	        g.vertex(dd2, dd2);
	        break;
	    case 6:
	        g.vertex(dd4, dd4);
	        g.vertex(dd34, dd34);
	        g.vertex(dd4, dd34);
	        g.vertex(dd34, dd4);
	        g.vertex(dd4, dd2);
	        g.vertex(dd34, dd2);
	        break;
	    default:
	        assert(false);// && "Invalid die");
	        break;
	    }
	    g.drawPoints();
	    g.setPointSize(oldDotSize);
	}

    @Override
    public void startDrag(float x, float y) {

    }

    @Override
    public void endDrag() {

    }

}