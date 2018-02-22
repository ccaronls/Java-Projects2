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
import cc.lib.game.Renderer;
import cc.lib.math.Vector2D;

/**
 * 
 * @author Chris Caron
 *
 * Base player type to interact with GUI
 */
public abstract class UIPlayer extends PlayerBot {

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
	
    private boolean animationEnabled = true;
	
    public abstract boolean isInfoVisible();

	private long getAnimTime() {
		return 1500;//return GUI.instance.getProps().getIntProperty("anim.ms", 1500);
	}
    
    void startStructureAnimation(UIBoard b, final Vertex vertex, final VertexType type) {
        if (!animationEnabled)
            return;
        if (vertex == null)
            return;
        b.addAnimation(new UIBlockingAnimation(getAnimTime()) {

            @Override
            public void draw(AGraphics g, float position, float dt) {
                //vertex.setPlayer(0);
                g.setColor(getColor());
                g.pushMatrix();
                g.translate(vertex);
                g.translate(0, (1-position)*b.getStructureRadius()*5);
                g.scale(1, position);
                switch (type) {
                	case SETTLEMENT:
                		b.drawSettlement(g, Vector2D.ZERO, getPlayerNum(), false);
                		break;
                	case CITY:
                		b.drawCity(g, Vector2D.ZERO, getPlayerNum(), false);
                		break;
                	case WALLED_CITY:
                		b.drawWalledCity(g, Vector2D.ZERO, getPlayerNum(), false);
                		break;
                	case METROPOLIS_POLITICS:
                        b.drawMetropolisPolitics(g, Vector2D.ZERO, getPlayerNum(), false);
                        break;
                	case METROPOLIS_SCIENCE:
                        b.drawMetropolisScience(g, Vector2D.ZERO, getPlayerNum(), false);
                        break;
                	case METROPOLIS_TRADE:
                        b.drawMetropolisTrade(g, Vector2D.ZERO, getPlayerNum(), false);
                        break;
                }
                g.popMatrix();
            }
        });
        
    }    

    void startMoveShipAnimation(final UIBoard b, final Route source, final Route target, final SOC soc) {
    	if (!animationEnabled)
    		return;
    	if (source == null || target == null || soc == null)
    		return;
        b.addAnimation(new UIBlockingAnimation(getAnimTime()) {
			
			@Override
			public void draw(AGraphics g, float position, float dt) {
				g.setColor(getColor());
                g.pushMatrix();
                //render.translate(mp);
                //render.scale(1, position);
                Vector2D startV = b.getRouteMidpoint(source);
                Vector2D endV   = b.getRouteMidpoint(target);
                Vector2D curV   = startV.add(endV.sub(startV).scale(position));
                
                float startAng  = b.getEdgeAngle(source);
                float endAng    = b.getEdgeAngle(target);
                float curAng    = startAng + (endAng - startAng) * position;
                
                b.drawShip(g, curV, Math.round(curAng), false);
                g.popMatrix();
			}
		});
    }
    
    void startBuildShipAnimation(UIBoard b, final Route edge, final SOC soc) {
    	if (!animationEnabled)
    		return;
    	
    	if (edge == null || soc == null)
    		return;
    	
        b.addAnimation(new UIBlockingAnimation(getAnimTime()) {
			
			@Override
			public void draw(AGraphics g, float position, float dt) {
				g.setColor(getColor());
                g.pushMatrix();
                //render.translate(mp);
                g.scale(1, position);
                b.drawShip(g, edge, false);
                g.popMatrix();
			}
		});
    }
    
    void startUpgradeShipAnimation(final UIBoard b, final Route ship) {
    	if (!animationEnabled)
    		return;
    	
    	if (ship == null)
    		return;
    	b.addAnimation(new UIBlockingAnimation(2000) {
			
			@Override
			public void draw(AGraphics g, float position, float dt) {
				g.setColor(getColor());
                g.pushMatrix();
                //render.translate(mp);
                g.scale(1, 1.0f - position);
                b.drawShip(g, ship, false);
                g.popMatrix();
			
                g.pushMatrix();
                //render.translate(mp);
                g.scale(1, position);
                b.drawWarShip(g, ship, false);
                g.popMatrix();
			}
		});
    }
    
    void startRoadAnimation(final UIBoard b, final Route edge, final SOC soc) {
        if (!animationEnabled)
            return;

        if (edge == null || soc == null)
            return;
        if (edge != null) {
            b.addAnimation(new UIBlockingAnimation(getAnimTime()) {

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
                    g.drawLines(b.roadLineThickness);
                }
            });
        }
    }
    
    void startKnightAnimation(final UIBoard b, final Vertex vertex) {
    	if (!animationEnabled || vertex == null)
    		return;

    	b.addAnimation(new UIBlockingAnimation(getAnimTime()) {
			
			@Override
			public void draw(AGraphics g, float position, float dt) {
                g.setColor(getColor());
                g.pushMatrix();
                g.translate(vertex.getX(), position * (vertex.getY()));
                g.scale((2f-position) * (float)Math.cos((1-position)*20), (2f-position));
                b.drawKnight(g, Vector2D.ZERO, getPlayerNum(), 1, false, false);
                g.popMatrix();
			}
		});
    }
    
    void startMoveKnightAnimation(UIBoard b, final Vertex fromVertex, final Vertex toVertex) {
    	if (!animationEnabled || fromVertex == null || toVertex == null)
    		return;

    	b.addAnimation(new UIBlockingAnimation(getAnimTime()) {
			
			@Override
			public void draw(AGraphics g, float position, float dt) {
                g.setColor(getColor());
                g.pushMatrix();
                IVector2D pos = Vector2D.newTemp(fromVertex).add(Vector2D.newTemp(toVertex).sub(fromVertex).scale(position));
                g.translate(pos);
                b.drawKnight(g, Vector2D.ZERO, getPlayerNum(), fromVertex.getType().getKnightLevel(), fromVertex.getType().isKnightActive(), false);
                g.popMatrix();
			}
		});
    }

	@Override
	public Vertex chooseVertex(SOC soc, Collection<Integer> vertexIndices, VertexChoice mode, Vertex knightToMove) {
		Vertex v = super.chooseVertex(soc, vertexIndices, mode, knightToMove);
		doVertexAnimation((UIBoard)soc.getBoard(), mode, v, knightToMove);
		return v;
	}
	
	protected final void doVertexAnimation(UIBoard b, VertexChoice mode, Vertex v, Vertex v2) {
		if (v == null)
			return;
		switch (mode) {
			case CITY:
				startStructureAnimation(b, v, VertexType.CITY);
				break;
			case CITY_WALL:
				startStructureAnimation(b, v, VertexType.WALLED_CITY);
				break;
			case KNIGHT_DESERTER:
				break;
			case KNIGHT_DISPLACED:
			case KNIGHT_MOVE_POSITION:
				if (v2 != null)
					startMoveKnightAnimation(b, v2, v);
				break;
			case NEW_KNIGHT:
				startKnightAnimation(b, v);
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
				startStructureAnimation(b, v, VertexType.METROPOLIS_POLITICS);
				break;
			case SCIENCE_METROPOLIS:
				startStructureAnimation(b, v, VertexType.METROPOLIS_SCIENCE);
				break;
			case SETTLEMENT:
				startStructureAnimation(b, v, VertexType.SETTLEMENT);
				break;
			case TRADE_METROPOLIS:
				startStructureAnimation(b, v, VertexType.METROPOLIS_TRADE);
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
	    UIBoard b = (UIBoard)soc.getBoard();
		switch (mode)
		{
			case ROAD:
				startRoadAnimation(b, route, soc);
				break;
			case ROUTE_DIPLOMAT:
				break;
			case SHIP:
				if (moveShipSource != null) {
					startMoveShipAnimation(b, moveShipSource, route, soc);
				} else {
					startBuildShipAnimation(b, route, soc);
				}
				moveShipSource = null;
				break;
			case SHIP_TO_MOVE:
				moveShipSource = route;
				break;
			case UPGRADE_SHIP:
				startUpgradeShipAnimation(b, route);
				break;
			case OPPONENT_ROAD_TO_ATTACK:
			case OPPONENT_SHIP_TO_ATTACK:
				break;
		}
	}

	@Override
	protected abstract void onBoardChanged();

	/*{
		BoardComponent bc = GUI.instance.getBoardComponent();
		bc.repaint();
        try {
            synchronized (bc) {
                bc.wait(100);
            }
        } catch (Exception e) {}
        synchronized (this) {
            notify(); // notify anyone waiting on this (see spinner)
        }		
	}*/

	@Override
	protected abstract BotNode onOptimalPath(BotNode optimal, List<BotNode> leafs);

	/*{
		return GUI.instance.chooseOptimalPath(optimal, leafs);
	}*/
	
}
