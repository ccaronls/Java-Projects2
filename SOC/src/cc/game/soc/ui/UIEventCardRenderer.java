package cc.game.soc.ui;

import cc.game.soc.android.R;
import cc.game.soc.core.EventCard;
import cc.game.soc.core.EventCardType;
import cc.game.soc.core.Rules;
import cc.lib.game.AAnimation;
import cc.lib.game.AGraphics;
import cc.lib.game.APGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GDimension;
import cc.lib.game.Justify;
import cc.lib.game.Utils;
import cc.lib.ui.UIComponent;

public final class UIEventCardRenderer extends UIRenderer {

	//private float minCardWidth = 0;
	final UIDiceRenderer diceComps;
	private EventCard eventCard = new EventCard(EventCardType.NoEvent, 8);

	public UIEventCardRenderer(UIComponent component) {
		super(component);
        this.diceComps = new UIDiceRenderer(component, false);
	}

	private AAnimation<AGraphics> dealAnim = null;

	float cw, ch, arc;
    float padding = RenderConstants.thickLineThickness;

	@Override
	public synchronized void draw(APGraphics g, int px, int py) {
        g.setTextHeight(RenderConstants.textSizeSmall);
        g.setTextStyles(AGraphics.TextStyle.NORMAL);

		String cardText = getString(R.string.event_card_new_year);
		String helpText = getString(R.string.event_card_card_shuffled_on_next);
		int production = 0;
        UISOC soc = UISOC.getInstance();
        if (eventCard != null) {
            cardText = eventCard.getType().getName(this);
            helpText = eventCard.getHelpText(soc != null ? soc.getRules() : new Rules(), this);
            production = eventCard.getProduction();
        }

        ch = getComponent().getHeight();
		cw = ch*2/3;

		if (cw > getComponent().getWidth()/2) {
		    cw = getComponent().getWidth()/2;
            ch = cw*3/2;
        }

		final float tw = g.getViewportWidth() - cw - 3*padding;
		GDimension r = g.getTextDimension(helpText, tw);
		float cx = g.getViewportWidth() - cw;
		float cy = g.getViewportHeight() - ch;
		arc = Math.min(cw,  ch) / 5;
		g.setColor(GColor.WHITE);
        g.setTextStyles(AGraphics.TextStyle.NORMAL);
		g.drawWrapString(padding, cy + padding, tw, helpText);

		float dieDim = cw/2-4*padding;
		float dx = cx;
		float dy = cy + ch-2*padding-dieDim;
		g.pushMatrix();
		g.translate(cx, cy);
		if (dealAnim != null) {
		    dealAnim.update(g);
		    if (dealAnim.isDone()) {
		        dealAnim = null;
            }
            getComponent().redraw();
        } else {
		    drawCard(g, production, cardText);
        }
        g.popMatrix();

        if (diceComps != null) {
            g.pushMatrix();
            g.translate(dx, dy);
            diceComps.setDiceRect(new GDimension(cw, dieDim));
            diceComps.draw(g, px, py);
            diceComps.setDiceRect(null);
            g.popMatrix();
        }
	}

	private void drawCard(AGraphics g, int production, String txt) {
        final float fh = g.getTextHeight();
        g.setColor(GColor.BLUE);
        g.drawFilledRoundedRect(0, 0, cw, ch, arc);
        g.setColor(GColor.YELLOW);
        g.drawFilledRoundedRect(padding, padding, cw-2*padding, ch-2*padding, arc-2);
        g.setColor(GColor.BLACK);
        g.drawWrapString(cw/2, ch/3, cw, Justify.CENTER, Justify.TOP, txt);

        if (production > 0) {
            g.setTextStyles(AGraphics.TextStyle.BOLD);
            g.setColor(GColor.BLUE);
            float ovalThickness = RenderConstants.thinLineThickness;
            float ovalWidth = fh*2;
            float ovalHeight = fh+ovalThickness+RenderConstants.textMargin;
            g.drawFilledOval(cw/2-ovalWidth/2, padding*2, ovalWidth, ovalHeight);
            g.setColor(GColor.YELLOW);
            g.drawFilledOval(cw/2-ovalWidth/2+ovalThickness, padding*2+ovalThickness, ovalWidth-ovalThickness*2, ovalHeight-ovalThickness*2);
            g.setColor(GColor.BLACK);
            g.drawJustifiedString(cw/2, padding*2+ovalHeight/2, Justify.CENTER, Justify.CENTER, String.valueOf(production));
        }

    }

	public final void setEventCard(final EventCard card) {
	    if (card == null)
	        return;
	    if (eventCard == null) {
	        eventCard = card;
	        getComponent().redraw();
	        return;
        }
	    final int productionIn = eventCard.getProduction();
	    final int productionOut = card.getProduction();
	    final String txtIn = eventCard.getName(UISOC.getInstance());
        final String txtOut = card.getName(UISOC.getInstance());
	    dealAnim = new AAnimation<AGraphics>(500, 1, true) {
            @Override
            protected void draw(AGraphics g, float position, float dt) {
                g.pushMatrix();
                g.translate(cw / 2, 0);
                g.scale(1f - position, 1);
                g.translate(-cw / 2, 0);
                if (getRepeat() == 0)
                    drawCard(g, productionIn, txtIn);
                else
                    drawCard(g, productionOut, txtOut);
                g.popMatrix();
            }

            @Override
            public void onDone() {
                eventCard = card;
                synchronized (this) {
                    notify();
                }
            }
        }.start();

	    getComponent().redraw();
        Utils.waitNoThrow(dealAnim, dealAnim.getDuration()+100);
    }

}
