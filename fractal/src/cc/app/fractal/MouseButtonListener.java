package cc.app.fractal;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import cc.lib.game.Utils;

public abstract class MouseButtonListener implements MouseListener, Runnable {

	boolean entered = false;
	boolean running = true;
	
	@Override
	public void run() {
		while (running) {
			doAction();
            Utils.waitNoThrow(this, 200);
		}
		
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (entered) {
    		running = true;
    		new Thread(this).start();
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		running = false;
		synchronized (this) {
			notify();
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		entered = true;
	}

	@Override
	public void mouseExited(MouseEvent e) {
		entered = false;
		running = false;
		synchronized (this) {
			notify();
		}
	}

	protected abstract void doAction();
}
