package cc.game.soc.ui;

import java.util.ArrayList;
import java.util.List;

import cc.game.soc.core.SOC;
import cc.lib.game.APGraphics;
import cc.lib.game.GColor;

public final class UIBarbarianRenderer implements UIRenderer {

	int imageDefault;
	int [] images;
    float border;
    final UIComponent component;

	public UIBarbarianRenderer(UIComponent component) { //}, SOC soc, int defaultImage, int [] images, float border) {
	    this.component = component;
	    component.setRenderer(this);
	}

	public void initAssets(int imageDefault, int tile0, int tile1, int tile2, int tile3, int tile4, int tile5, int tile6, int tile7, float border) {
	    this.imageDefault = imageDefault;
	    this.images = new int [] {
            tile1, tile2, tile3, tile4, tile5, tile6, tile7
        };
	    this.border = border;
    }

	@Override
	public final void draw(APGraphics g, int pickX, int pickY) {

        UISOC soc = UISOC.getInstance();

        int numStepsAway = soc.getBarbarianDistance();
		int image = imageDefault;
		if (numStepsAway >= 0 && numStepsAway < images.length) {
			image = images[numStepsAway];
		}
		g.drawImage(image, 0, 0, component.getWidth(), component.getHeight());

		int barbStr = SOC.computeBarbarianStrength(soc, soc.getBoard());
		int catanStr = SOC.computeCatanStrength(soc, soc.getBoard());
		
		String text = "Barbarians: " + barbStr
				    + "\nCatan: " + catanStr;

		List<String> lines = new ArrayList<>();
		float maxWidth  = g.generateWrappedText(text, component.getWidth(), lines, null);

        g.setColor(GColor.TRANSLUSCENT_BLACK);
		g.drawFilledRect(0, 0, component.getWidth(), Math.round(maxWidth));

        if (catanStr >= barbStr)
            g.setColor(GColor.GREEN);
        else
            g.setColor(GColor.RED);

        float y = 3;
		for (String line : lines) {
		    g.drawString(line, 3, y);
		    y += g.getTextHeight();
        }
	}

    @Override
    public void doClick() {

    }
}
