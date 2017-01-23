package cc.game.soc.ui;

public interface UIEventHandler {

	void onTouch(int x, int y);
	
	void onUntouched();
	
	void onPressed(int x, int y);
}
