package cc.game.soc.ui;

import java.util.Collection;
import java.util.List;

import cc.game.soc.core.BotNode;
import cc.game.soc.core.PlayerBot;
import cc.game.soc.core.Route;
import cc.game.soc.core.SOC;
import cc.game.soc.core.Vertex;
import cc.game.soc.core.VertexType;
import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.IVector2D;
import cc.lib.game.Utils;
import cc.lib.math.Vector2D;

/**
 * 
 * @author Chris Caron
 *
 * Base player type to interact with GUI
 */
public class UIPlayer extends PlayerBot {

	static {
		addField(UIPlayer.class, "color");
	}
	
	private GColor color = GColor.BLACK;
	
	public void setColor(GColor color) {
		this.color = color;
	}
	
	public GColor getColor() {
		return color;
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
                    g.drawLines(comp.roadLineThickness);
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
	public Vertex chooseVertex(SOC soc, Collection<Integer> vertexIndices, VertexChoice mode, Vertex knightToMove) {
		Vertex v = super.chooseVertex(soc, vertexIndices, mode, knightToMove);
		doVertexAnimation(soc, mode, v, knightToMove);
		return v;
	}
	
	protected final void doVertexAnimation(SOC soc, VertexChoice mode, Vertex v, Vertex v2) {
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
					startMoveKnightAnimation(v2, v);
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
	public Route chooseRoute(SOC soc, Collection<Integer> routeIndices, RouteChoice mode) {
		Route route = super.chooseRoute(soc, routeIndices, mode);
		doRouteAnimation(soc, mode, route);
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
        Utils.waitNoThrow(bc, 100);
        synchronized (this) {
            notify(); // notify anyone waiting on this (see spinner)
        }		
	}

	@Override
	protected BotNode onOptimalPath(BotNode optimal, List<BotNode> leafs) {
		return UISOC.getInstance().chooseOptimalPath(optimal, leafs);
	}



}
