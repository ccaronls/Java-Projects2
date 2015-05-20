package cc.game.soc.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JComponent;

import cc.game.soc.core.EventCard;
import cc.game.soc.core.EventCardType;
import cc.game.soc.swing.ADiceComponent.DiceChangedListener;
import cc.lib.game.Justify;
import cc.lib.swing.AWTUtils;

public class EventCardComponent extends JComponent implements DiceChangedListener {

	int minCardWidth = 0;
	Font BOLD = null;
	final ADiceComponent [] diceComps;
	
	EventCardComponent(ADiceComponent ... diceComps) {
		this.diceComps = diceComps;
		for (ADiceComponent d : diceComps) {
			d.setListener(this);
		}
	}
	
	@Override
	public void onDiceChanged(int numDieNum) {
		repaint();
	}

	@Override
	public void paint(Graphics g) {
		if (BOLD == null) {
			BOLD = g.getFont().deriveFont(Font.BOLD);
		}
		g.setFont(BOLD);
		final int padding = 5;
	
		if (minCardWidth == 0) {
			ArrayList<String> all = new ArrayList<String>();
			for (EventCardType e : EventCardType.values()) {
				all.addAll(Arrays.asList(e.getNiceString().split("\n")));
			}
			
			for (String s : all) {
				int width = AWTUtils.getStringWidth(g, s);
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
			EventCard card = GUI.instance.getSOC().getTopEventCard();
			if (card != null) {
				cardText = card.getType().getNiceString();
				helpText = card.getHelpText();
				production = card.getProduction();
			}
		}
		
		final int cw = minCardWidth;
		final int ch = cw*3/2;
		final int fh = g.getFontMetrics().getHeight();
		final int tw = Math.max(getWidth() - cw - 3*padding, 100);
		Rectangle r = AWTUtils.drawWrapString(g, padding, padding, tw, helpText);
		int cx = getWidth() - padding - cw;
		int cy = padding;
		int arc = Math.min(cw,  ch) / 5;
		g.setColor(Color.BLUE);
		g.fillRoundRect(cx, cy, cw, ch, arc, arc);
		g.setColor(Color.YELLOW);
		g.fillRoundRect(cx+padding, cy+padding, cw-2*padding, ch-2*padding, arc-2, arc-2);
		if (production > 0) {
    		g.setColor(Color.BLUE);
    		int ovalThickness = 2;
    		int ovalWidth = fh*2;
    		int ovalHeight = fh+ovalThickness+6;
    		g.fillOval(cx+cw/2-ovalWidth/2, cy+padding*2, ovalWidth, ovalHeight);
    		g.setColor(Color.YELLOW);
    		g.fillOval(cx+cw/2-ovalWidth/2+ovalThickness, cy+padding*2+ovalThickness, ovalWidth-ovalThickness*2, ovalHeight-ovalThickness*2);
    		g.setColor(Color.BLACK);
    		AWTUtils.drawJustifiedString(g, cx+cw/2, cy+padding*2+ovalHeight/2, Justify.CENTER, Justify.CENTER, String.valueOf(production));
		}
		g.setColor(Color.BLACK);
		AWTUtils.drawWrapJustifiedString(g, cx+cw/2, cy+ch/3, minCardWidth, Justify.CENTER, cardText);
		
		int dieDim = cw/2-4*padding;
		int dx = cx+cw/2-dieDim-padding;
		int dy = cy+ch-2*padding-dieDim;
		
		for (ADiceComponent d : diceComps) {
			d.setBounds(dx, dy, dieDim, dieDim);
			if (d.getDie() > 0)
				d.drawDie(g, dx, dy, dieDim);
			dx += dieDim+padding*2;
		}
		
		Dimension dim = new Dimension(Math.max(getWidth()-1, tw+3*padding+cw), Math.max(r.y + r.height, ch)+2*padding);
		Dimension pDim = getPreferredSize();
		if (pDim == null || pDim.width < dim.width || pDim.height < dim.height) {
			setPreferredSize(dim);
			invalidate();
		}
	}
}
