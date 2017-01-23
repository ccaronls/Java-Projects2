package cc.game.soc.ui;

public interface UIWidget {

	void repaint();

	int getWidth();

	int getHeight();

	void setUIEventHandler(UIEventHandler handler);

	void setBounds(int x, int y, int w, int h);

	void setSize(int width, int height);
}
