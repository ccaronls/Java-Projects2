package cc.game.soc.ui;

import java.util.ArrayList;
import java.util.Arrays;

import cc.game.soc.core.EventCard;
import cc.game.soc.core.EventCardType;
import cc.lib.game.AGraphics;
import cc.lib.game.APGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GDimension;
import cc.lib.game.Justify;

public final class UIEventCardRenderer implements UIRenderer {

	float minCardWidth = 0;
	final UIDiceRenderer diceComps;
	final UIComponent component;

	public UIEventCardRenderer(UIComponent component, UIDiceRenderer diceComps) {
		this.diceComps = diceComps;
		this.component = component;
		this.component.setRenderer(this);
	}
	
	@Override
	public synchronized void draw(APGraphics g, int px, int py) {
		g.setTextStyles(AGraphics.TextStyle.BOLD);
		final int padding = 5;
	
		if (minCardWidth == 0) {
			ArrayList<String> all = new ArrayList<String>();
			for (EventCardType e : EventCardType.values()) {
				all.addAll(Arrays.asList(e.getNiceString().split("\n")));
			}
			
			for (String s : all) {
				float width = g.getTextWidth(s);
				if (width > minCardWidth) {
					minCardWidth = width;
				}
			}
			
			minCardWidth += 2*padding;
		}
		
		String cardText = "New Year";
		String helpText = "Event cards wil be shuffled on next event card drawn.";
		int production = 0;
		{
			EventCard card = UISOC.getInstance().getTopEventCard();
			if (card != null) {
				cardText = card.getType().getNiceString();
				helpText = card.getHelpText(UISOC.getInstance().getRules());
				production = card.getProduction();
			}
		}
		
		final float cw = minCardWidth;
		final float ch = cw*3/2;
		final float fh = g.getTextHeight();
		final float tw = Math.max(g.getViewportWidth() - cw - 3*padding, 100);
		GDimension r = g.getTextDimension(helpText, tw);
		float cx = g.getViewportWidth() - padding - cw;
		float cy = padding;
		float arc = Math.min(cw,  ch) / 5;
		g.setColor(GColor.BLUE);
		g.drawFilledRoundedRect(cx, cy, cw, ch, arc);
		g.setColor(GColor.YELLOW);
		g.drawFilledRoundedRect(cx+padding, cy+padding, cw-2*padding, ch-2*padding, arc-2);
		if (production > 0) {
    		g.setColor(GColor.BLUE);
    		float ovalThickness = 2;
    		float ovalWidth = fh*2;
    		float ovalHeight = fh+ovalThickness+6;
    		g.drawFilledOval(cx+cw/2-ovalWidth/2, cy+padding*2, ovalWidth, ovalHeight);
    		g.setColor(GColor.YELLOW);
    		g.drawFilledOval(cx+cw/2-ovalWidth/2+ovalThickness, cy+padding*2+ovalThickness, ovalWidth-ovalThickness*2, ovalHeight-ovalThickness*2);
    		g.setColor(GColor.BLACK);
    		g.drawJustifiedString(cx+cw/2, cy+padding*2+ovalHeight/2, Justify.CENTER, Justify.CENTER, String.valueOf(production));
		}
		g.setColor(GColor.BLACK);
		g.drawWrapString(cx+cw/2, cy+ch/3, minCardWidth, Justify.CENTER, Justify.TOP, cardText);
		
		float dieDim = cw/2-4*padding;
		float dx = cx+cw/2-dieDim-padding;
		float dy = cy+ch-2*padding-dieDim;

		g.pushMatrix();
		g.translate(dx, dy);
		diceComps.setDiceRect(new GDimension(minCardWidth, dieDim));
        diceComps.draw(g, px, py);
        diceComps.setDiceRect(null);
		g.popMatrix();
	}

    @Override
    public void doClick() {

    }

    @Override
    public void startDrag(float x, float y) {

    }

    @Override
    public void endDrag() {

    }

}