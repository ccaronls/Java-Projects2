package cc.lib.swing;

import java.awt.Color;

import cc.lib.game.AColor;

final class AWTColor extends AColor {

    Color color;
    
    public AWTColor(Color color) {
        this.color = color;
    }

    @Override
    public final float getRed() {
        return (float)color.getRed() / 255.0f;
    }

    @Override
    public final float getGreen() {
        return (float)color.getGreen() / 255.0f;
    }

    @Override
    public final float getBlue() {
        return (float)color.getBlue() / 255.0f;
    }

    @Override
    public final float getAlpha() {
        return (float)color.getAlpha() / 255.0f;
    }

    @Override
    public final AColor darkened(float amount) {
        return new AWTColor(AWTUtils.darken(color, amount));
    }

    @Override
    public final AColor lightened(float amount) {
        return new AWTColor(AWTUtils.lighten(color, amount));
    }

	@Override
	public AColor setAlpha(float alpha) {
		if (alpha == getAlpha())
			return this;
		return new AWTColor(new Color(getRed(), getGreen(), getBlue(), alpha));
	}

    
    
}
