package cc.app.umleditor.swing;

import cc.app.umleditor.api.UMLEditor;
import cc.lib.game.AGraphics;
import cc.lib.swing.AWTGraphics;
import cc.lib.swing.AWTFrame;
import cc.lib.swing.AWTKeyboardAnimationApplet;

public class Main extends AWTKeyboardAnimationApplet {

	public static void main(String [] args) {
		//AGraphics.DEBUG_ENABLED = true;
        //Utils.DEBUG_ENABLED = true;
        //Golf.DEBUG_ENABLED = true;
        //PlayerBot.DEBUG_ENABLED = true;
        AWTFrame frame = new AWTFrame("Kaiser");
        AWTKeyboardAnimationApplet app = new Main();
        frame.add(app);
        app.init();
        frame.centerToScreen(800, 600);
        app.start();
	}
	
	UMLEditor editor;
	
	@Override
	protected void doInitialization() {
		// TODO Auto-generated method stub
		//editor = new UMLEditor();
	}

	final StringBuffer textBuffer = new StringBuffer();
	
	@Override
	protected void drawFrame(AWTGraphics g) {
		// TODO Auto-generated method stub
		if (getMouseButtonClicked(1)) {
			editor.highlightAt(getMouseX(), getMouseY());
		}

		for (int i=0; i<255; i++) {
			if (getKeyboardReset((char)i)) {
				switch ((char)i) {
				case 'a': case 'b' : case 'c':
				}
			}
		}

		editor.draw(g);
	}

	@Override
	protected void onDimensionsChanged(AGraphics g, int width, int height) {
		// TODO Auto-generated method stub
		
	}

	
	
	
}
