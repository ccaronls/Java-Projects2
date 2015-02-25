package cc.game.soc.nety;

import cc.game.soc.core.DevelopmentCardType;
import cc.game.soc.core.SOCPlayer;
import cc.game.soc.core.ResourceType;

/**
 * Player type intended base class for all networked players. 
 * 
 * @author Chris Caron
 *
 */
public abstract class PlayerNet extends SOCPlayer {

	private SOCServer server;
	
	protected PlayerNet(SOCServer server) {
		this.server = server;
	}
	
	SOCServer getServer() {
		return server;
	}
	
    @Override
    public void incrementResource(ResourceType type, int amount) {
        // TODO Auto-generated method stub
        super.incrementResource(type, amount);
        server.broadcastCommand(new Command(CommandType.CMD_SET_RESOURCE_COUNT, 
                Protocol.encodeResourceCount(type, this.getResourceCount(type))), getPlayerNum());
    }

	/* (non-Javadoc)
	 * @see cc.game.soc.core.Player#addDevelopmentCard(cc.game.soc.core.DevelopmentCardType, int)
	 */
	public void addDevelopmentCard(DevelopmentCardType type, int flag) {
		super.addDevelopmentCard(type, flag);
		server.broadcastCommand(new Command(CommandType.CMD_EDIT_DEVEL_CARD, 
                Protocol.encodeDevelCard(type, flag, true)), getPlayerNum());
	}

	/* (non-Javadoc)
	 * @see cc.game.soc.core.Player#removeDevelopmentCard(cc.game.soc.core.DevelopmentCardType, int)
	 */
	public void removeDevelopmentCard(DevelopmentCardType type, int flag) {
		// TODO Auto-generated method stub
		super.removeDevelopmentCard(type, flag);
		server.broadcastCommand(new Command(CommandType.CMD_EDIT_DEVEL_CARD, 
                Protocol.encodeDevelCard(type, flag, false)), getPlayerNum());
	}

	/* (non-Javadoc)
	 * @see cc.game.soc.core.Player#setRoadLength(int)
	 */
	public void setRoadLength(int len) {
		// TODO Auto-generated method stub
	    if (len != getRoadLength()) {
    		super.setRoadLength(len);
    		server.broadcastCommand(new Command(CommandType.CMD_SET_ROAD_LENGTH, len), getPlayerNum());
	    }
	}

	/*  (non-Javadoc)
	 * @see cc.game.soc.core.Player#setPoints(int)
	 */
	public void setPoints(int points) {
	    if (points != getPoints()) {
    		super.setPoints(points);
    		server.broadcastCommand(new Command(CommandType.CMD_SET_POINTS, points), getPlayerNum());
	    }
	}
}
