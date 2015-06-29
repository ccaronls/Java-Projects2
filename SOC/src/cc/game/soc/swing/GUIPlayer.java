package cc.game.soc.swing;

import java.awt.Color;
import java.awt.Graphics;
import java.util.*;

import cc.game.soc.core.*;
import cc.lib.math.Vector2D;
import cc.lib.swing.AWTRenderer;
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
	
	private boolean infoVisible = true;
	
	void setInfoVisible(boolean visible) {
	    this.infoVisible = visible;
	}
	
    private boolean animationEnabled = true;
	
    public boolean isInfoVisible() {
        return infoVisible;
    }

	private long getAnimTime() {
		return GUI.instance.getProps().getIntProperty("anim.ms", 1500);		
	}
	
    void startCityAnimation(final Vertex vertex) {
        if (!animationEnabled)
            return;
        if (vertex == null)
            return;
        GUI.instance.getBoardComponent().addAnimation(new BlockingAnimation(getAnimTime()) {

            @Override
            void draw(Graphics g, float position, float dt) {
                //vertex.setPlayer(0);
                g.setColor(getColor());
                AWTRenderer render = GUI.instance.getBoardComponent().render;
                render.pushMatrix();
                //render.translate(-vertex.getX(), -vertex.getY());
                render.translate(vertex);
                render.scale(position, position);
                //render.translate(-vertex.getX(), -vertex.getY());
                GUI.instance.getBoardComponent().drawCity(g, Vector2D.ZERO, getPlayerNum(), false);
                render.popMatrix();
            }
        });
        
    }
    
    void startCityWallAnimation(final Vertex vertex) {
        if (!animationEnabled)
            return;
        if (vertex == null)
            return;
        GUI.instance.getBoardComponent().addAnimation(new BlockingAnimation(getAnimTime()) {

            @Override
            void draw(Graphics g, float position, float dt) {
                //vertex.setPlayer(0);
                g.setColor(getColor());
                AWTRenderer render = GUI.instance.getBoardComponent().render;
                render.pushMatrix();
                //render.translate(-vertex.getX(), -vertex.getY());
                render.translate(vertex);
                //render.translate(0, -4*position);
                render.scale(1, position);
                //render.translate(-vertex.getX(), -vertex.getY());
                GUI.instance.getBoardComponent().drawWalledCity(g, Vector2D.ZERO, getPlayerNum(), false);
                render.popMatrix();
            }
            
        });
        
    }    
    
    void startShipAnimation(final Route edge, final SOC soc) {
    	if (!animationEnabled)
    		return;
    	
    	if (edge == null || soc == null)
    		return;
    	
    	final BoardComponent comp = GUI.instance.getBoardComponent();
        comp.addAnimation(new BlockingAnimation(getAnimTime()) {
			
			@Override
			void draw(Graphics g, float position, float dt) {
				g.setColor(getColor());
                AWTRenderer render = GUI.instance.getBoardComponent().render;
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
			void draw(Graphics g, float position, float dt) {
				g.setColor(getColor());
                AWTRenderer render = GUI.instance.getBoardComponent().render;
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
                void draw(Graphics g, float position, float dt) {
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
    
    void startSettlementAnimation(final Vertex vertex) {
        if (!animationEnabled)
            return;

        if (vertex == null)
            return;
        GUI.instance.getBoardComponent().addAnimation(new BlockingAnimation(getAnimTime()) {

            @Override
            void draw(Graphics g, float position, float dt) {
                g.setColor(getColor());
                AWTRenderer render = GUI.instance.getBoardComponent().render;
                render.pushMatrix();
                //render.translate(-vertex.getX(), -vertex.getY());
                render.translate(vertex);
                render.scale(position, position);
                //render.translate(-vertex.getX(), -vertex.getY());
                GUI.instance.getBoardComponent().drawSettlement(g, Vector2D.ZERO, getPlayerNum(), false);
                render.popMatrix();
            }
        });
    }
    
    void startKnightAnimation(final Vertex vertex) {
    	if (!animationEnabled || vertex == null)
    		return;

    	GUI.instance.getBoardComponent().addAnimation(new BlockingAnimation(getAnimTime()) {
			
			@Override
			void draw(Graphics g, float position, float dt) {
                g.setColor(getColor());
                AWTRenderer render = GUI.instance.getBoardComponent().render;
                render.pushMatrix();
                //render.translate(-vertex.getX(), -vertex.getY());
                render.translate(vertex);
                render.scale(position, 1);
                //render.translate(-vertex.getX(), -vertex.getY());
                GUI.instance.getBoardComponent().drawKnight(g, Vector2D.ZERO, getPlayerNum(), 1, false, false);
                render.popMatrix();
			}
		});
    }

	@Override
	public Vertex chooseVertex(SOC soc, Collection<Integer> vertexIndices, VertexChoice mode) {
		Vertex v = super.chooseVertex(soc, vertexIndices, mode);
		switch (mode) {
			case CITY:
				startCityAnimation(v);
				break;
			case CITY_WALL:
				break;
			case KNIGHT_DESERTER:
				break;
			case KNIGHT_DISPLACED:
				break;
			case KNIGHT_MOVE_POSITION:
				break;
			case KNIGHT_TO_ACTIVATE:
				break;
			case KNIGHT_TO_MOVE:
				break;
			case KNIGHT_TO_PROMOTE:
				break;
			case NEW_KNIGHT:
				break;
			case OPPONENT_KNIGHT_TO_DISPLACE:
				break;
			case POLITICS_METROPOLIS:
				break;
			case SCIENCE_METROPOLIS:
				break;
			case SETTLEMENT:
				startSettlementAnimation(v);
				break;
			case TRADE_METROPOLIS:
				break;
			case PIRATE_FORTRESS:
				break;
		}
		return v;
	}

	

	@Override
	public Route chooseRoute(SOC soc, Collection<Integer> routeIndices, RouteChoice mode) {
		Route route = super.chooseRoute(soc, routeIndices, mode);
		switch (mode)
		{
			case ROAD:
				startRoadAnimation(route, soc);
				break;
			case ROUTE_DIPLOMAT:
				break;
			case SHIP:
				startShipAnimation(route, soc);
				break;
			case SHIP_TO_MOVE:
				break;
			case UPGRADE_SHIP:
				break;
			case OPPONENT_ROAD_TO_ATTACK:
				break;
		}
        
        return route;
	}

	@Override
	protected void onBoardChanged() {
		BoardComponent bc = GUI.instance.getBoardComponent();
		bc.repaint();
        try {
            synchronized (bc) {
                bc.wait(50);
            }
        } catch (Exception e) {}
        synchronized (this) {
            notify(); // notify anyone waiting on this (see spinner)
        }		
	}
}
