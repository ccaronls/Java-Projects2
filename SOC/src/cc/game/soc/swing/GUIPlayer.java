package cc.game.soc.swing;

import java.awt.Color;
import java.awt.Graphics;
import java.util.*;

import cc.game.soc.core.*;
import cc.lib.game.IVector2D;
import cc.lib.game.Renderer;
import cc.lib.math.CMath;
import cc.lib.math.Vector2D;
import cc.lib.swing.AWTUtils;

/**
 * 
 * @author Chris Caron
 *
 * Base player type to interact with GUI
 */
public class GUIPlayer extends PlayerBot {

	static {
		addField(GUIPlayer.class, "color", AWTUtils.COLOR_ARCHIVER);
	}
	
	private Color color = Color.BLACK;
	
	public void setColor(Color color) {
		this.color = color;
	}
	
	public Color getColor() {
		return color;
	}
	
    private boolean animationEnabled = true;
	
    public boolean isInfoVisible() {
        return GUI.instance.getProps().getBooleanProperty(GUI.PROP_AI_TUNING_ENABLED, false);
    }

	private long getAnimTime() {
		return GUI.instance.getProps().getIntProperty("anim.ms", 1500);		
	}
    
    void startStructureAnimation(final Vertex vertex, final VertexType type) {
        if (!animationEnabled)
            return;
        if (vertex == null)
            return;
        GUI.instance.getBoardComponent().addAnimation(new BlockingAnimation(getAnimTime()) {

            @Override
            public void draw(Graphics g, float position, float dt) {
                //vertex.setPlayer(0);
                g.setColor(getColor());
                Renderer render = GUI.instance.getBoardComponent().render;
                render.pushMatrix();
                render.translate(vertex);
                render.translate(0, (1-position)*GUI.instance.getBoardComponent().getStructureRadius()*5);
                render.scale(1, position);
                switch (type) {
                	case SETTLEMENT:
                		GUI.instance.getBoardComponent().drawSettlement(g, Vector2D.ZERO, getPlayerNum(), false);
                		break;
                	case CITY:
                		GUI.instance.getBoardComponent().drawCity(g, Vector2D.ZERO, getPlayerNum(), false);
                		break;
                	case WALLED_CITY:
                		GUI.instance.getBoardComponent().drawWalledCity(g, Vector2D.ZERO, getPlayerNum(), false);
                		break;
                	case METROPOLIS_POLITICS:
                        GUI.instance.getBoardComponent().drawMetropolisPolitics(g, Vector2D.ZERO, getPlayerNum(), false);
                        break;
                	case METROPOLIS_SCIENCE:
                        GUI.instance.getBoardComponent().drawMetropolisScience(g, Vector2D.ZERO, getPlayerNum(), false);
                        break;
                	case METROPOLIS_TRADE:
                        GUI.instance.getBoardComponent().drawMetropolisTrade(g, Vector2D.ZERO, getPlayerNum(), false);
                        break;
                }
                render.popMatrix();
            }
        });
        
    }    

    void startMoveShipAnimation(final Route source, final Route target, final SOC soc) {
    	if (!animationEnabled)
    		return;
    	if (source == null || target == null || soc == null)
    		return;
    	final BoardComponent comp = GUI.instance.getBoardComponent();
        comp.addAnimation(new BlockingAnimation(getAnimTime()) {
			
			@Override
			public void draw(Graphics g, float position, float dt) {
				g.setColor(getColor());
                Renderer render = comp.render;
                render.pushMatrix();
                //render.translate(mp);
                //render.scale(1, position);
                Vector2D startV = comp.getBoard().getRouteMidpoint(source);
                Vector2D endV   = comp.getBoard().getRouteMidpoint(target);
                Vector2D curV   = startV.add(endV.sub(startV).scale(position));
                
                float startAng  = comp.getEdgeAngle(source);
                float endAng    = comp.getEdgeAngle(target);
                float curAng    = startAng + (endAng - startAng) * position;
                
                GUI.instance.getBoardComponent().drawShip(g, curV, Math.round(curAng), false);
                render.popMatrix();				
			}
		});
    }
    
    void startBuildShipAnimation(final Route edge, final SOC soc) {
    	if (!animationEnabled)
    		return;
    	
    	if (edge == null || soc == null)
    		return;
    	
    	final BoardComponent comp = GUI.instance.getBoardComponent();
        comp.addAnimation(new BlockingAnimation(getAnimTime()) {
			
			@Override
			public void draw(Graphics g, float position, float dt) {
				g.setColor(getColor());
                Renderer render = GUI.instance.getBoardComponent().render;
                render.pushMatrix();
                //render.translate(mp);
                render.scale(1, position);
                GUI.instance.getBoardComponent().drawShip(g, edge, false);
                render.popMatrix();				
			}
		});
    }
    
    void startUpgradeShipAnimation(final Route ship) {
    	if (!animationEnabled)
    		return;
    	
    	if (ship == null)
    		return;
    	final BoardComponent comp = GUI.instance.getBoardComponent();
    	comp.addAnimation(new BlockingAnimation(2000) {
			
			@Override
			public void draw(Graphics g, float position, float dt) {
				g.setColor(getColor());
                Renderer render = GUI.instance.getBoardComponent().render;
                render.pushMatrix();
                //render.translate(mp);
                render.scale(1, 1.0f - position);
                GUI.instance.getBoardComponent().drawShip(g, ship, false);
                render.popMatrix();				
			
                render.pushMatrix();
                //render.translate(mp);
                render.scale(1, position);
                GUI.instance.getBoardComponent().drawWarShip(g, ship, false);
                render.popMatrix();
			}
		});
    }
    
    void startRoadAnimation(final Route edge, final SOC soc) {
        if (!animationEnabled)
            return;

        if (edge == null || soc == null)
            return;
        final BoardComponent comp = GUI.instance.getBoardComponent();
        if (edge != null) {
            comp.addAnimation(new BlockingAnimation(getAnimTime()) {

                final Vertex A = soc.getBoard().getVertex(edge.getFrom());
                final Vertex B = soc.getBoard().getVertex(edge.getTo());
                
                @Override
                public void draw(Graphics g, float position, float dt) {
                    Vertex from, to;
                    if (A.getPlayer() == getPlayerNum() || soc.getBoard().isVertexAdjacentToPlayerRoad(edge.getFrom(), getPlayerNum())) {
                        from=A; to=B;
                    } else {
                        from=B; to=A;
                    }
                    float dx = (to.getX() - from.getX()) * position;
                    float dy = (to.getY() - from.getY()) * position;
                    comp.render.clearVerts();
                    comp.render.addVertex(from);
                    comp.render.addVertex(from.getX() + dx, from.getY() + dy);
                    g.setColor(getColor());
                    comp.render.drawLines(g, comp.roadLineThickness);
                }
            });
        }
    }
    
    void startKnightAnimation(final Vertex vertex) {
    	if (!animationEnabled || vertex == null)
    		return;

    	GUI.instance.getBoardComponent().addAnimation(new BlockingAnimation(getAnimTime()) {
			
			@Override
			public void draw(Graphics g, float position, float dt) {
                g.setColor(getColor());
                Renderer render = GUI.instance.getBoardComponent().render;
                render.pushMatrix();
                render.translate(vertex.getX(), position * (vertex.getY()));
                render.scale((2f-position) * (float)Math.cos((1-position)*20), (2f-position));
                GUI.instance.getBoardComponent().drawKnight(g, Vector2D.ZERO
                		, getPlayerNum(), 1, false, false);
                render.popMatrix();
			}
		});
    }
    
    void startMoveKnightAnimation(final Vertex fromVertex, final Vertex toVertex) {
    	if (!animationEnabled || fromVertex == null || toVertex == null)
    		return;

    	GUI.instance.getBoardComponent().addAnimation(new BlockingAnimation(getAnimTime()) {
			
			@Override
			public void draw(Graphics g, float position, float dt) {
                g.setColor(getColor());
                Renderer render = GUI.instance.getBoardComponent().render;
                render.pushMatrix();
                IVector2D pos = Vector2D.newTemp(fromVertex).add(Vector2D.newTemp(toVertex).sub(fromVertex).scale(position));
                render.translate(pos);
                GUI.instance.getBoardComponent().drawKnight(g, Vector2D.ZERO, getPlayerNum(), fromVertex.getType().getKnightLevel(), fromVertex.getType().isKnightActive(), false);
                render.popMatrix();
			}
		});
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
	}

	@Override
	protected BotNode onOptimalPath(BotNode optimal, List<BotNode> leafs) {
		return GUI.instance.chooseOptimalPath(optimal, leafs);
	}
	
}
