package cc.game.soc.ui;

import java.util.ArrayList;
import java.util.Arrays;

import cc.game.soc.android.R;
import cc.game.soc.core.EventCard;
import cc.game.soc.core.EventCardType;
import cc.game.soc.core.Rules;
import cc.lib.game.AGraphics;
import cc.lib.game.APGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GDimension;
import cc.lib.game.Justify;

public final class UIEventCardRenderer extends UIRenderer {

	//private float minCardWidth = 0;
	final UIDiceRenderer diceComps;
	private EventCard eventCard = new EventCard(EventCardType.NoEvent, 8);

	public UIEventCardRenderer(UIComponent component) {
		super(component);
        this.diceComps = new UIDiceRenderer(component, false);
	}
	
	@Override
	public synchronized void draw(APGraphics g, int px, int py) {
        g.setTextHeight(RenderConstants.textSizeSmall);
        g.setTextStyles(AGraphics.TextStyle.NORMAL);

		float padding = RenderConstants.thickLineThickness;
		String cardText = getString(R.string.event_card_new_year);
		String helpText = getString(R.string.event_card_card_shuffled_on_next);
		int production = 0;
        UISOC soc = UISOC.getInstance();
        if (eventCard != null) {
            cardText = eventCard.getType().getName(this);
            helpText = eventCard.getHelpText(soc != null ? soc.getRules() : new Rules(), this);
            production = eventCard.getProduction();
        }

        final float ch = component.getHeight();//cw*3/2;
		final float cw = ch*2/3;
		final float fh = g.getTextHeight();
		final float tw = g.getViewportWidth() - cw - 3*padding;
		GDimension r = g.getTextDimension(helpText, tw);
		float cx = g.getViewportWidth() - cw;
		float cy = 0;
		float arc = Math.min(cw,  ch) / 5;
		g.setColor(GColor.BLUE);
		g.drawFilledRoundedRect(cx, cy, cw, ch, arc);
		g.setColor(GColor.YELLOW);
		g.drawFilledRoundedRect(cx+padding, cy+padding, cw-2*padding, ch-2*padding, arc-2);
		if (production > 0) {
		    g.setTextStyles(AGraphics.TextStyle.BOLD);
    		g.setColor(GColor.BLUE);
    		float ovalThickness = RenderConstants.thinLineThickness;
    		float ovalWidth = fh*2;
    		float ovalHeight = fh+ovalThickness+RenderConstants.textMargin;
    		g.drawFilledOval(cx+cw/2-ovalWidth/2, cy+padding*2, ovalWidth, ovalHeight);
    		g.setColor(GColor.YELLOW);
    		g.drawFilledOval(cx+cw/2-ovalWidth/2+ovalThickness, cy+padding*2+ovalThickness, ovalWidth-ovalThickness*2, ovalHeight-ovalThickness*2);
    		g.setColor(GColor.BLACK);
    		g.drawJustifiedString(cx+cw/2, cy+padding*2+ovalHeight/2, Justify.CENTER, Justify.CENTER, String.valueOf(production));
		}
		g.setColor(GColor.BLACK);
		g.drawWrapString(cx+cw/2, cy+ch/3, cw, Justify.CENTER, Justify.TOP, cardText);

		g.drawWrapString(padding, padding, tw, helpText);

		float dieDim = cw/2-4*padding;
		float dx = cx;//+component.getWidth()-cw;//+cw/2-dieDim-padding;
		float dy = cy+component.getHeight()-2*padding-dieDim;

        if (diceComps != null) {
            g.pushMatrix();
            g.translate(dx, dy);
            diceComps.setDiceRect(new GDimension(cw, dieDim));
            diceComps.draw(g, px, py);
            diceComps.setDiceRect(null);
            g.popMatrix();
        }
	}

	public final void setEventCard(EventCard card) {
	    this.eventCard = card;
	    component.redraw();
    }

}
