package cc.game.soc.ui;

import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JComponent;

import cc.game.soc.core.DiceType;
import cc.game.soc.core.EventCard;
import cc.game.soc.core.EventCardType;
import cc.game.soc.core.SOC;
import cc.lib.game.AGraphics;
import cc.lib.game.Justify;
import cc.lib.swing.AWTUtils;

public class UIEventCardController implements AUIDiceComtroller.DiceChangedListener {

	int minCardWidth = 0;
	final AUIDiceComtroller [] diceComps;
	final UIWidget widget;
	final SOC soc;
	
	UIEventCardController(SOC soc, UIWidget widget, AUIDiceComtroller ... diceComps) {
		this.diceComps = diceComps;
		this.widget = widget;
		this.soc = soc;
		for (AUIDiceComtroller d : diceComps) {
			d.setListener(this);
		}
	}
	
	@Override
	public synchronized void onDiceChanged(int numDieNum) {
		widget.repaint();
	}

	public synchronized void paint(AGraphics g) {
		final int padding = 5;
	
		if (minCardWidth == 0) {
			ArrayList<String> all = new ArrayList<String>();
			for (EventCardType e : EventCardType.values()) {
				all.addAll(Arrays.asList(e.getNiceString().split("\n")));
			}
			
			for (String s : all) {
				int width = (int)g.getTextWidth(s);
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
			EventCard card = soc.getTopEventCard();
			if (card != null) {
				cardText = card.getType().getNiceString();
				helpText = card.getHelpText(soc.getRules());
				production = card.getProduction();
			}
		}
		
		final int cw = minCardWidth;
		final int ch = cw*3/2;
		final int fh = g.getTextHeight();//.getFontMetrics().getHeight();
		final int tw = Math.max(widget.getWidth() - cw - 3*padding, 100);
		//final float dy = 
		final int ny = (int)g.drawWrapString(padding, padding, tw, helpText);
		//Rectangle r = AWTUtils.drawWrapString(g, padding, padding, tw, helpText);
		int cx = widget.getWidth() - padding - cw;
		int cy = padding;
		int arc = Math.min(cw,  ch) / 5;
		g.setColor(g.BLUE);
		g.drawFilledRoundedRect(cx, cy, cw, ch, arc);
		g.setColor(g.YELLOW);
		g.drawFilledRoundedRect(cx+padding, cy+padding, cw-2*padding, ch-2*padding, arc-2);
		if (production > 0) {
    		g.setColor(g.BLUE);
    		int ovalThickness = 2;
    		int ovalWidth = fh*2;
    		int ovalHeight = fh+ovalThickness+6;
    		g.drawFilledOval(cx+cw/2-ovalWidth/2, cy+padding*2, ovalWidth, ovalHeight);
    		g.setColor(g.YELLOW);
    		g.drawFilledOval(cx+cw/2-ovalWidth/2+ovalThickness, cy+padding*2+ovalThickness, ovalWidth-ovalThickness*2, ovalHeight-ovalThickness*2);
    		g.setColor(g.BLACK);
    		g.drawJustifiedString(cx+cw/2, cy+padding*2+ovalHeight/2, Justify.CENTER, Justify.CENTER, String.valueOf(production));
		}
		g.setColor(g.BLACK);
		g.drawWrapString(cx+cw/2, cy+ch/3, minCardWidth, Justify.CENTER, Justify.CENTER, cardText);
		
		int dieDim = cw/2-4*padding;
		int dx = cx+cw/2-dieDim-padding;
		int dy = cy+ch-2*padding-dieDim;
		
		for (AUIDiceComtroller d : diceComps) {
			d.widget.setBounds(dx, dy, dieDim, dieDim);
			if (d.getDie() > 0)
				d.drawDie(g, dx, dy, dieDim);
			dx += dieDim+padding*2;
		}

		int dimx = Math.max(widget.getWidth()-1, tw+3*padding+cw);
		int dimy = Math.max(ny, ch)+2*padding;
		widget.setSize(dimx, dimy);
	}
}
