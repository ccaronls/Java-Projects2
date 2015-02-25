package cc.game.soc.swing;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

import cc.game.soc.core.*;
import cc.lib.math.Vector2D;
import cc.lib.swing.AWTRenderer;
import cc.lib.swing.AWTUtils;
import cc.lib.utils.Reflector;

/**
 * 
 * @author Chris Caron
 *
 * Base player type to interact with GUI
 */
public class GUIPlayer extends PlayerBot {

	static {
		addField(GUIPlayer.class, "color", new Reflector.AArchiver() {
			
			@Override
			public Object parse(String value) throws Exception {
				return AWTUtils.stringToColor(value);
			}
			
			@Override
			public String getStringValue(Object obj) {
				return AWTUtils.colorToString((Color)obj);
			}
		});
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
	
	enum CardLoc {
	    CL_NONE,
	    CL_UPPER_LEFT,
	    CL_UPPER_RIGHT,
	    CL_MIDDLE_LEFT,
	    CL_MIDDLE_RIGHT,
	    CL_LOWER_RIGHT
	}
	
	List<Animation> cardAnimations = new ArrayList<Animation>();
	CardLoc loc = CardLoc.CL_NONE;
	
	private long roadAnimTime = GUI.instance.getProps().getIntProperty("anim.road.ms", 3000);
	private long structureAnimTime = GUI.instance.getProps().getIntProperty("anim.structure.ms", 3000);
	private long shipAnimTime = GUI.instance.getProps().getIntProperty("anim.ship.ms", 3000);
    private boolean animationEnabled = true;
	
    public boolean isInfoVisible() {
        return infoVisible;
    }

    void startCityAnimation(final Vertex vertex) {
        if (!animationEnabled)
            return;
        if (vertex == null)
            return;
        GUI.instance.getBoardComponent().addAnimation(new Animation(structureAnimTime, 0) {

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
                GUI.instance.getBoardComponent().drawCity(g, 0, 0, getPlayerNum(), false);
                render.popMatrix();
            }

            @Override
            void onDone() {
                //vertex.setPlayer(getPlayerNum());
                synchronized (this) {
                    notify();
                }
            }

            @Override
            void onStarted() { 
                synchronized (this) {
                    try {
                        wait(structureAnimTime);
                    } catch (Exception e) {}
                }
            }
            
        });
        
    }
    
    void startCityWallAnimation(final Vertex vertex) {
        if (!animationEnabled)
            return;
        if (vertex == null)
            return;
        GUI.instance.getBoardComponent().addAnimation(new Animation(structureAnimTime, 0) {

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
                GUI.instance.getBoardComponent().drawWalledCity(g, 0, 0, getPlayerNum(), false);
                render.popMatrix();
            }

            @Override
            void onDone() {
                //vertex.setPlayer(getPlayerNum());
                synchronized (this) {
                    notify();
                }
            }

            @Override
            void onStarted() { 
                synchronized (this) {
                    try {
                        wait(structureAnimTime);
                    } catch (Exception e) {}
                }
            }
            
        });
        
    }    
    
    void startShipAnimation(final Route edge, final SOC soc) {
    	if (!animationEnabled)
    		return;
    	
    	if (edge == null || soc == null)
    		return;
    	
    	final BoardComponent comp = GUI.instance.getBoardComponent();
    	final Vector2D mp = soc.getBoard().getRouteMidpoint(edge);
        comp.addAnimation(new Animation(shipAnimTime,0) {
			
			@Override
			void draw(Graphics g, float position, float dt) {
				g.setColor(getColor());
                AWTRenderer render = GUI.instance.getBoardComponent().render;
                render.pushMatrix();
                render.translate(mp);
                render.scale(1, position);
                GUI.instance.getBoardComponent().drawShip(g, Vector2D.ZERO, false);
                render.popMatrix();				
			}
			
			@Override
            void onDone() {
                synchronized (this) {
                    notify();
                }
            }

            @Override
            void onStarted() {
                synchronized (this) {
                    try {
                        wait(roadAnimTime);
                    } catch (Exception e) {}
                }
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
            comp.addAnimation(new Animation(roadAnimTime,0) {

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

                @Override
                void onDone() {
                    synchronized (this) {
                        notify();
                    }
                }

                @Override
                void onStarted() {
                    synchronized (this) {
                        try {
                            wait(roadAnimTime);
                        } catch (Exception e) {}
                    }
                }
                
            });
        }
    }
    
    void startSettlementAnimation(final Vertex vertex) {
        if (!animationEnabled)
            return;

        if (vertex == null)
            return;
        GUI.instance.getBoardComponent().addAnimation(new Animation(structureAnimTime, 0) {

            @Override
            void draw(Graphics g, float position, float dt) {
                g.setColor(getColor());
                AWTRenderer render = GUI.instance.getBoardComponent().render;
                render.pushMatrix();
                //render.translate(-vertex.getX(), -vertex.getY());
                render.translate(vertex);
                render.scale(position, position);
                //render.translate(-vertex.getX(), -vertex.getY());
                GUI.instance.getBoardComponent().drawSettlement(g, 0, 0, getPlayerNum(), false);
                render.popMatrix();
            }

            @Override
            void onDone() {
                synchronized (this) {
                    notify();
                }
            }

            @Override
            void onStarted() {
                synchronized (this) {
                    try {
                        wait(structureAnimTime);
                    } catch (Exception e) {}
                }
            }
            
        });
    }

	@Override
	public Vertex chooseVertex(SOC soc, List<Integer> vertexIndices, VertexChoice mode) {
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
		}
		return v;
	}

	

	@Override
	public Route chooseRoute(SOC soc, List<Integer> routeIndices, RouteChoice mode) {
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
		}
        
        return route;
	}

	@Override
	protected float evaluateSOC(SOC soc, Player p, Board b) {
		GUI.instance.getBoardComponent().repaint();
        try {
            synchronized (GUI.instance.getBoardComponent()) {
                GUI.instance.getBoardComponent().wait(50);
            }
        } catch (Exception e) {}
        synchronized (this) {
            notify(); // notify anyone waiting on this (see spinner)
        }
		return super.evaluateSOC(soc, p, b);
	}

}
