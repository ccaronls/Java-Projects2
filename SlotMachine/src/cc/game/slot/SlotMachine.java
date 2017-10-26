package cc.game.slot;

import cc.lib.swing.AWTGraphics;
import cc.lib.swing.EZFrame;
import cc.lib.swing.KeyboardAnimationApplet;
import cc.lib.game.AGraphics;
import cc.lib.game.Justify;
import cc.lib.game.Utils;

public class SlotMachine extends KeyboardAnimationApplet {

    public static void main(String[] args) {
        Utils.DEBUG_ENABLED = true;
        EZFrame frame = new EZFrame("SlotMachine DEBUG");
        KeyboardAnimationApplet app = new SlotMachine();
        frame.add(app);
        frame.centerToScreen(600, 400);
        app.init();
        app.start();
        app.focusGained(null);
    }

	
	Wheel wheelA = new Wheel();
	Wheel wheelB = new Wheel();
	Wheel wheelC = new Wheel();
	
	final int STATE_STOPPED = 0;
	final int STATE_SPINNING = 1;
	
	int state = STATE_STOPPED;
	
	@Override
	protected void doInitialization() {
		// TODO Auto-generated method stub
		String [] cards = {
			"BAR", "Cherry", "Orange", "Pirate", "Lime"	
		};
		
		wheelA.generate(cards, 20);
		wheelB.generate(cards, 20);
		wheelC.generate(cards, 20);
	}

	static long lastTime = System.currentTimeMillis();
	
	@Override
	protected void drawFrame(AWTGraphics g) {

		//g.setFont(Font.getFont("Arial"));
		this.clearScreen();
		
		final int maxX = this.getScreenWidth();
		final int maxY = this.getScreenHeight();
		
		g.setColor(g.WHITE);
		final int x0 = 50;
		final int w0 = 50;
		final int y0 = 150;
		final int h0 = 150;
		wheelA.draw(g, x0, y0-50, w0, h0+100);
		wheelB.draw(g, x0+w0, y0-50, w0, h0+100);
		wheelC.draw(g, x0+w0*2, y0-50, w0, h0+100);
		
		g.setColor(g.CYAN);
		g.drawFilledRect(0, 0, maxX, y0);
		g.drawFilledRect(0, y0 + h0, maxX, 300);

		int cx = maxX / 2;
		int cy = maxY / 2;
		
		if (Utils.DEBUG_ENABLED && this.getKeyboard('q'))
			System.exit(0);
		
		switch (state) {
		case STATE_STOPPED:
			g.setColor(g.WHITE);
			g.drawJustifiedString(maxX - 20, cy, Justify.RIGHT, Justify.CENTER, "Press spacebar to spin");
			
			final int randFactor = 200;
			
			if (this.getKeyboardReset(' ')) {
				wheelA.setVelocity(randFactor + Utils.randFloat(randFactor));
				wheelB.setVelocity(-randFactor - Utils.randFloat(randFactor));
				wheelC.setVelocity(randFactor + Utils.randFloat(randFactor));
				state = STATE_SPINNING;
			}
			
			g.setColor(g.CYAN);
			g.drawLine(0, y0 + h0/2, maxX, y0 + h0/2, 1);
			g.drawJustifiedString(cx, y0+h0/3, Justify.LEFT, Justify.CENTER, wheelA.getCenterCardAt());
			g.drawJustifiedString(cx, y0+h0/2, Justify.LEFT, Justify.CENTER, wheelB.getCenterCardAt());
			g.drawJustifiedString(cx, y0+h0*2/3, Justify.LEFT, Justify.CENTER, wheelC.getCenterCardAt());
			
			break;
			
		case STATE_SPINNING:
			
			long currentTime = System.currentTimeMillis();
			float delta = (currentTime - lastTime);
			lastTime = currentTime;
			delta *= 0.001f; // convert from miliseconds to seconds
			
			wheelA.spin(delta);
			wheelB.spin(delta);
			wheelC.spin(delta);			
			
			if (wheelA.isStopped() &&
				wheelB.isStopped() &&
				wheelC.isStopped()) {
				state = STATE_STOPPED;
				}
			break;
		}
		
		
	}

	@Override
	protected void onDimensionsChanged(AGraphics g, int width, int height) {
		g.ortho();
	}

	
	
}
