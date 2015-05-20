package cc.game.soc.swing;

import java.awt.Color;
import java.awt.Graphics;

import cc.game.soc.core.DiceEvent;
import cc.lib.swing.ImageColorFilter;
import cc.lib.swing.ImageMgr;

public class EventDiceComponent extends ADiceComponent {

	final int shipImageId;
	final int redCityImageId;
	final int greenCityImageId;
	final int blueCityImageId;

	EventDiceComponent() {
		ImageMgr images = GUI.instance.images;
		shipImageId = images.loadImage("dicesideship2.GIF");
		int cityId = images.loadImage("dicesidecity2.GIF");
		redCityImageId = images.addImage(images.transform(images.getImage(cityId), new ImageColorFilter(Color.WHITE, Color.RED)));
		greenCityImageId = images.addImage(images.transform(images.getImage(cityId), new ImageColorFilter(Color.WHITE, Color.GREEN)));
		blueCityImageId = images.addImage(images.transform(images.getImage(cityId), new ImageColorFilter(Color.WHITE, Color.BLUE)));
		images.deleteImage(cityId);
	}
	
	
	@Override
	void drawDie(Graphics g, int x, int y, int dim) {
		g.setColor(Color.WHITE);
		int arc = dim/4;
	    g.fillRoundRect(x, y, dim, dim, arc, arc);
	    ImageMgr images = GUI.instance.images;
	    switch (DiceEvent.fromDieNum(getDie())) {
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

	
	
}
