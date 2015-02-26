package marcos.games.hexes.swing;

import java.util.Arrays;
import java.util.List;

import marcos.games.hexes.core.Hexes;
import marcos.games.hexes.core.Player;
import marcos.games.hexes.core.Shape;
import marcos.games.hexes.swing.MultiPlayerClient.Listener;
import marcos.games.hexes.swing.MultiPlayerClient.User;

public class RemotePlayer extends Player implements Listener {

	final User user;
	final MultiPlayerClient cl;
	
	RemotePlayer(User user, MultiPlayerClient cl) {
		this.user = user;
		this.cl = cl;
		cl.addListener(this);
	}

	int choosePieceResponse = -1;
	
	@Override
	public int choosePiece(Hexes hexes, List<Integer> choices) {
		try {
			choosePieceResponse = -1;
			cl.sendCommand("choosePiece", choices);
			synchronized (this) {
				wait();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return choosePieceResponse;
	}

	Shape chooseShapeResponse = null;
	
	@Override
	public Shape chooseShape(Hexes hexes, Shape[] choices) {
		try {
			chooseShapeResponse = null;
			cl.sendCommand("chooseShape", Arrays.asList(choices));
			synchronized (this) {
				wait();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return chooseShapeResponse;
	}

	@Override
	public boolean onCommandReceived(String cmd, Object... params) {
		boolean handled = false;
		if (cmd.equals("pieceChoosen")) {
			choosePieceResponse = (Integer)params[0];
			handled = true;
		} else if (cmd.equals("shapeChoosen")) {
			chooseShapeResponse = (Shape)params[0];
			handled = true;
		}
		
		synchronized (this) {
			notify();
		}

		return handled;
	}

	
	
}
