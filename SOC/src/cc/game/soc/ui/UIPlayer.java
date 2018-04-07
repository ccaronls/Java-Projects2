package cc.game.soc.ui;

import java.util.Collection;
import java.util.List;

import cc.game.soc.core.BotNode;
import cc.game.soc.core.Card;
import cc.game.soc.core.Dice;
import cc.game.soc.core.MoveType;
import cc.game.soc.core.PlayerBot;
import cc.game.soc.core.Route;
import cc.game.soc.core.SOC;
import cc.game.soc.core.Trade;
import cc.game.soc.core.Vertex;
import cc.game.soc.core.VertexType;
import cc.lib.annotation.Keep;
import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.IVector2D;
import cc.lib.game.Utils;
import cc.lib.math.Vector2D;
import cc.lib.net.ClientConnection;
import cc.lib.net.GameCommand;

/**
 * 
 * @author Chris Caron
 *
 * Base player type to interact with GUI
 */
public class UIPlayer extends PlayerBot implements ClientConnection.Listener {

	static {
		addField(UIPlayer.class, "color");
	}

	@Omit
	ClientConnection connection = null; // this is set when game is in server mode and this object represents a remote player

    void connect(ClientConnection conn) {
        if (connection != null && connection.isConnected())
            throw new AssertionError("Connection already assigned");
        connection = conn;
        connection.addListener(this);
    }

	private GColor color = GColor.BLACK;

    public UIPlayer() {}

    public UIPlayer(GColor color) {
        this.color = color;
    }

	public void setColor(GColor color) {
		this.color = color;
	}
	
	public GColor getColor() {
		return color;
	}

    public String getName() {
        if (connection != null && connection.isConnected()) {
            return connection.getName();
        }
        return "Player " + getPlayerNum();
    }

    public boolean isInfoVisible() {
        return false;//UISOC.getInstance().getProps().getBooleanProperty(GUI.PROP_AI_TUNING_ENABLED, false);
    }

	private long getAnimTime() {
		return 1500;
	}
    
    void startStructureAnimation(final Vertex vertex, final VertexType type) {
        if (vertex == null)
            return;
        final UIBoardRenderer board = UISOC.getInstance().getUIBoard();
        board.addAnimation(new UIAnimation(getAnimTime()) {

            @Override
            public void draw(AGraphics g, float position, float dt) {
                g.setColor(getColor());
                g.pushMatrix();
                g.translate(vertex);
                g.translate(0, (1-position)*board.getStructureRadius()*5);
                g.scale(1, position);
                switch (type) {
                	case SETTLEMENT:
                		board.drawSettlement(g, Vector2D.ZERO, getPlayerNum(), false);
                		break;
                	case CITY:
                		board.drawCity(g, Vector2D.ZERO, getPlayerNum(), false);
                		break;
                	case WALLED_CITY:
                		board.drawWalledCity(g, Vector2D.ZERO, getPlayerNum(), false);
                		break;
                	case METROPOLIS_POLITICS:
                        board.drawMetropolisPolitics(g, Vector2D.ZERO, getPlayerNum(), false);
                        break;
                	case METROPOLIS_SCIENCE:
                        board.drawMetropolisScience(g, Vector2D.ZERO, getPlayerNum(), false);
                        break;
                	case METROPOLIS_TRADE:
                        board.drawMetropolisTrade(g, Vector2D.ZERO, getPlayerNum(), false);
                        break;
                }
                g.popMatrix();
            }
        }, true);
        
    }    

    void startMoveShipAnimation(final Route source, final Route target, final SOC soc) {
    	if (source == null || target == null || soc == null)
    		return;
    	final UIBoardRenderer comp = ((UISOC)soc).getUIBoard();
        comp.addAnimation(new UIAnimation(getAnimTime()) {
			
			@Override
			public void draw(AGraphics g, float position, float dt) {
				g.setColor(getColor());
                g.pushMatrix();
                //render.translate(mp);
                //render.scale(1, position);
                Vector2D startV = soc.getBoard().getRouteMidpoint(source);
                Vector2D endV   = soc.getBoard().getRouteMidpoint(target);
                Vector2D curV   = startV.add(endV.sub(startV).scaledBy(position));
                
                float startAng  = comp.getEdgeAngle(source);
                float endAng    = comp.getEdgeAngle(target);
                float curAng    = startAng + (endAng - startAng) * position;
                
                comp.drawShip(g, curV, Math.round(curAng), false);
                g.popMatrix();
			}
		}, true);
    }
    
    void startBuildShipAnimation(final Route edge, final SOC soc) {
    	if (edge == null || soc == null)
    		return;

        final UIBoardRenderer comp = ((UISOC)soc).getUIBoard();
        comp.addAnimation(new UIAnimation(getAnimTime()) {
			
			@Override
			public void draw(AGraphics g, float position, float dt) {
				g.setColor(getColor());
                g.pushMatrix();
                //render.translate(mp);
                g.scale(1, position);
                comp.drawShip(g, edge, false);
                g.popMatrix();
			}
		}, true);
    }
    
    void startUpgradeShipAnimation(final Route ship) {
    	if (ship == null)
    		return;
    	final UIBoardRenderer comp = UISOC.getInstance().getUIBoard();
    	comp.addAnimation(new UIAnimation(2000) {
			
			@Override
			public void draw(AGraphics g, float position, float dt) {
				g.setColor(getColor());
                g.pushMatrix();
                //render.translate(mp);
                g.scale(1, 1.0f - position);
                comp.drawShip(g, ship, false);
                g.popMatrix();
			
                g.pushMatrix();
                //render.translate(mp);
                g.scale(1, position);
                comp.drawWarShip(g, ship, false);
                g.popMatrix();
			}
		}, true);
    }

    void startRoadAnimation(final Route edge, final SOC soc) {
        if (edge == null || soc == null)
            return;
        final UIBoardRenderer comp = UISOC.getInstance().getUIBoard();
        if (edge != null) {
            comp.addAnimation(new UIAnimation(getAnimTime()) {

                final Vertex A = soc.getBoard().getVertex(edge.getFrom());
                final Vertex B = soc.getBoard().getVertex(edge.getTo());
                
                @Override
                public void draw(AGraphics g, float position, float dt) {
                    Vertex from, to;
                    if (A.getPlayer() == getPlayerNum() || soc.getBoard().isVertexAdjacentToPlayerRoad(edge.getFrom(), getPlayerNum())) {
                        from=A; to=B;
                    } else {
                        from=B; to=A;
                    }
                    float dx = (to.getX() - from.getX()) * position;
                    float dy = (to.getY() - from.getY()) * position;
                    g.begin();
                    g.vertex(from);
                    g.vertex(from.getX() + dx, from.getY() + dy);
                    g.setColor(getColor());
                    g.drawLines(RenderConstants.thickLineThickness);
                }
            }, true);
        }
    }
    
    void startKnightAnimation(final Vertex vertex) {
        final UIBoardRenderer comp = UISOC.getInstance().getUIBoard();
    	comp.addAnimation(new UIAnimation(getAnimTime()) {
			
			@Override
			public void draw(AGraphics g, float position, float dt) {
                g.setColor(getColor());
                g.pushMatrix();
                g.translate(vertex.getX(), position * (vertex.getY()));
                g.scale((2f-position) * (float)Math.cos((1-position)*20), (2f-position));
                comp.drawKnight(g, Vector2D.ZERO
                		, getPlayerNum(), 1, false, false);
                g.popMatrix();
			}
		}, true);
    }
    
    void startMoveKnightAnimation(final Vertex fromVertex, final Vertex toVertex) {
        final UIBoardRenderer comp = UISOC.getInstance().getUIBoard();
    	comp.addAnimation(new UIAnimation(getAnimTime()) {
			
			@Override
			public void draw(AGraphics g, float position, float dt) {
                g.setColor(getColor());
                g.pushMatrix();
                IVector2D pos = Vector2D.newTemp(fromVertex).add(Vector2D.newTemp(toVertex).sub(fromVertex).scaledBy(position));
                g.translate(pos);
                comp.drawKnight(g, Vector2D.ZERO, getPlayerNum(), fromVertex.getType().getKnightLevel(), fromVertex.getType().isKnightActive(), false);
                g.popMatrix();
			}
		}, true);
    }

	@Override
    @Keep
	public Integer chooseVertex(SOC soc, Collection<Integer> vertexIndices, VertexChoice mode, Integer knightToMove) {
		Integer vIndex = null;
		if (connection != null && connection.isConnected()) {
		    vIndex = connection.executeOnRemote(NetCommon.USER_ID, soc, vertexIndices, mode, knightToMove);
        } else {
		    vIndex = super.chooseVertex(soc, vertexIndices, mode, knightToMove);
        }
		if (vIndex != null) {
            doVertexAnimation(soc, mode, soc.getBoard().getVertex(vIndex), knightToMove);
        }
		return vIndex;
	}

    @Override
    @Keep
    public MoveType chooseMove(SOC soc, Collection<MoveType> moves) {
	    MoveType mv = null;
	    if (connection != null && connection.isConnected()) {
	        mv = connection.executeOnRemote(NetCommon.USER_ID,soc, moves);
        } else {
            mv = super.chooseMove(soc, moves);
        }
        return mv;
    }

    @Override
    @Keep
    public RouteChoiceType chooseRouteType(SOC soc) {
	    RouteChoiceType rt = null;
        if (connection != null && connection.isConnected()) {
            rt = connection.executeOnRemote(NetCommon.USER_ID, soc);
        } else {
            rt = super.chooseRouteType(soc);
        }
        return rt;
    }

    @Override
    @Keep
    public Integer chooseTile(SOC soc, Collection<Integer> tileIndices, TileChoice mode) {
	    Integer tIndex = null;
        if (connection != null && connection.isConnected()) {
            tIndex = connection.executeOnRemote(NetCommon.USER_ID, soc, tileIndices, mode);
        } else {
            tIndex = super.chooseTile(soc, tileIndices, mode);
        }
        return tIndex;
    }

    @Override
    @Keep
    public Trade chooseTradeOption(SOC soc, Collection<Trade> trades) {
	    Trade trade = null;
        if (connection != null && connection.isConnected()) {
            trade = connection.executeOnRemote(NetCommon.USER_ID, soc, trades);
        } else {
            trade = super.chooseTradeOption(soc, trades);
        }
        return trade;
    }

    @Override
    @Keep
    public Integer choosePlayer(SOC soc, Collection<Integer> playerOptions, PlayerChoice mode) {
	    Integer player = null;
        if (connection != null && connection.isConnected()) {
            player = connection.executeOnRemote(NetCommon.USER_ID, soc, playerOptions, mode);
        } else {
            player = super.choosePlayer(soc, playerOptions, mode);
        }
        return player;
    }

    @Override
    @Keep
    public Card chooseCard(SOC soc, Collection<Card> cards, CardChoice mode) {
	    Card card = null;
        if (connection != null && connection.isConnected()) {
            card = connection.executeOnRemote(NetCommon.USER_ID, soc, cards, mode);
        } else {
            card = super.chooseCard(soc, cards, mode);
        }
        return card;
    }

    @Override
    @Keep
    public <T extends Enum<T>> T chooseEnum(SOC soc, EnumChoice mode, T[] values) {
	    T e = null;
        if (connection != null && connection.isConnected()) {
            e = connection.executeOnRemote(NetCommon.USER_ID, soc, mode, values);
        } else {
            e = super.chooseEnum(soc, mode, values);
        }
        return e;
    }

    @Override
    @Keep
    public boolean setDice(SOC soc, Dice[] die, int num) {
	    if (connection != null && connection.isConnected()) {
	        Dice [] result = connection.executeOnRemote(NetCommon.USER_ID, soc, die, num);
	        if (result != null) {
	            Utils.copyElems(die, result);
	            return true;
            }
            return false;
        }
        return super.setDice(soc, die, num);
    }

    protected final void doVertexAnimation(SOC soc, VertexChoice mode, Vertex v, Integer v2) {
		if (v == null)
			return;
		switch (mode) {
			case CITY:
				startStructureAnimation(v, VertexType.CITY);
				break;
			case CITY_WALL:
				startStructureAnimation(v, VertexType.WALLED_CITY);
				break;
			case KNIGHT_DESERTER:
				break;
			case KNIGHT_DISPLACED:
			case KNIGHT_MOVE_POSITION:
				if (v2 != null)
					startMoveKnightAnimation(soc.getBoard().getVertex(v2), v);
				break;
			case NEW_KNIGHT:
				startKnightAnimation(v);
				break;
			case KNIGHT_TO_ACTIVATE:
				break;
			case KNIGHT_TO_MOVE:
				break;
			case KNIGHT_TO_PROMOTE:
				break;
			case OPPONENT_KNIGHT_TO_DISPLACE:
				break;
			case POLITICS_METROPOLIS:
				startStructureAnimation(v, VertexType.METROPOLIS_POLITICS);
				break;
			case SCIENCE_METROPOLIS:
				startStructureAnimation(v, VertexType.METROPOLIS_SCIENCE);
				break;
			case SETTLEMENT:
				startStructureAnimation(v, VertexType.SETTLEMENT);
				break;
			case TRADE_METROPOLIS:
				startStructureAnimation(v, VertexType.METROPOLIS_TRADE);
				break;
			case PIRATE_FORTRESS:
				break;
			case OPPONENT_STRUCTURE_TO_ATTACK:
				break;
		}
	}

	private Route moveShipSource = null;

	@Override
    @Keep
	public Integer chooseRoute(SOC soc, Collection<Integer> routeIndices, RouteChoice mode) {
	    Integer route = null;
	    if (connection != null && connection.isConnected()) {
	        route = connection.executeOnRemote(NetCommon.USER_ID, soc, routeIndices, mode);
        } else {
            route = super.chooseRoute(soc, routeIndices, mode);
        }
		if (route != null) {
            doRouteAnimation(soc, mode, soc.getBoard().getRoute(route));
        }
		return route;
	}
	
	protected final void doRouteAnimation(SOC soc, RouteChoice mode, Route route) {
		switch (mode)
		{
			case ROAD:
				startRoadAnimation(route, soc);
				break;
			case ROUTE_DIPLOMAT:
				break;
			case SHIP:
				if (moveShipSource != null) {
					startMoveShipAnimation(moveShipSource, route, soc);
				} else {
					startBuildShipAnimation(route, soc);
				}
				moveShipSource = null;
				break;
			case SHIP_TO_MOVE:
				moveShipSource = route;
				break;
			case UPGRADE_SHIP:
				startUpgradeShipAnimation(route);
				break;
			case OPPONENT_ROAD_TO_ATTACK:
			case OPPONENT_SHIP_TO_ATTACK:
				break;
		}
	}

	@Override
	protected void onBoardChanged() {
        final UIBoardRenderer bc = UISOC.getInstance().getUIBoard();
		bc.component.redraw();
        //Utils.waitNoThrow(bc, 100);
        //synchronized (this) {
        //    notify(); // notify anyone waiting on this (see spinner)
        //}
	}

	@Override
	protected BotNode onOptimalPath(BotNode optimal, List<BotNode> leafs) {
		return UISOC.getInstance().chooseOptimalPath(optimal, leafs);
	}

    @Override
    public void onCommand(ClientConnection c, GameCommand cmd) {

    }

    @Override
    public void onDisconnected(ClientConnection c, String reason) {

    }

    @Override
    public void onConnected(ClientConnection c) {

    }
}
