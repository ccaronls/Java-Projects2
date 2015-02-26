package marcos.games.hexes.swing;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.*;

import javax.swing.JOptionPane;

import marcos.games.hexes.core.*;

public class SwingPlayer extends Player implements MouseListener {

	public SwingPlayer() {
		HexesApplet.instance.addMouseListener(this);
	}

	@Override
	public int choosePiece(Hexes hexes, List<Integer> choices) {
		try {
			synchronized (this) {
				wait();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return HexesApplet.instance.highlightedPiece;
	}

	@Override
	public Shape chooseShape(Hexes hexes, Shape [] choices) {
		String [] opts = new String[choices.length];
		for (int i=0; i<opts.length; i++)
			opts[i] = choices[i].name() + "(" + getShapeCount(choices[i]) + ")";
		int n = JOptionPane.showOptionDialog(HexesApplet.instance.frame,
			"Choose Piece to Play",
		    "Choose",
		    JOptionPane.YES_NO_CANCEL_OPTION,
		    JOptionPane.PLAIN_MESSAGE,
		    null,
		    opts,
		    null);
		
		if (n == JOptionPane.CLOSED_OPTION) {
			hexes.cancel();
			return null;
		}
		
		return choices[n];
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
		
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		synchronized (this) {
			notify();
		}
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	
}
