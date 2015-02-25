package cc.game.soc.netx;

import cc.game.soc.core.*;

public class NetGame extends SOC implements Runnable {

	@Override
	public void printinfo(int playerNum, String txt) {
		// TODO Auto-generated method stub
		super.printinfo(playerNum, txt);
	}

	public void spinDice() {
		// TODO Auto-generated method stub
		
	}

	public void run() {
	    runGame();
	}
	
	void broadcast(Command cmd) {
	}

    public void setNumPlayers(int num) {
        for (int i=0; i<num; i++)
            addPlayer(new NetPlayer());
    }

    NetPlayer getNetPlayer(int num) {
        return (NetPlayer)getPlayerByPlayerNum(num);
    }
    
    public int findEmptySlot() {
        for (int i=1; i<=getNumPlayers(); i++) {
            if (!getNetPlayer(i).isConnected())
                return i;
        }
        return 0;
    }
}
