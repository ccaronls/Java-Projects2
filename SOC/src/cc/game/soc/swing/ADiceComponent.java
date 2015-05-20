package cc.game.soc.swing;

import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public abstract class ADiceComponent extends JComponent implements ChangeListener {

	public static interface DiceChangedListener {
		void onDiceChanged(int numDieNum);
	}
	
	private int die=0;
	private DiceChangedListener listener;
	
	ADiceComponent() {
		setMinimumSize(new Dimension(30,30));
		setPreferredSize(new Dimension(60, 30));
	}
	
	void setDie(int die) {
		this.die = die;
		repaint();
		if (listener != null)
			listener.onDiceChanged(die);
	}

	int getDie() {
		return die;
	}
	
	public void setListener(DiceChangedListener listener) {
		this.listener = listener;
	}
	
	@Override
	public void paint(Graphics g) {
		int w = getWidth();
		int h = getHeight();

		final int spacing = 5;
		final int dim = h - spacing;
		int x = w/2 - spacing/2 - dim;
		int y = 0;
		
		if (die != 0) {
			drawDie(g, x, y, dim);
		}
	}

	abstract void drawDie(Graphics g, int x, int y, int dim);

	@Override
	public void stateChanged(ChangeEvent arg) {
		JSpinner spinner = (JSpinner)arg.getSource();
		this.die = (Integer)spinner.getValue();
		repaint();
	}
	
	
}
