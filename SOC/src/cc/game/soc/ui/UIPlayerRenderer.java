package cc.game.soc.ui;

import cc.game.soc.core.Card;
import cc.game.soc.core.CardType;
import cc.game.soc.core.CommodityType;
import cc.game.soc.core.DevelopmentArea;
import cc.game.soc.core.Player;
import cc.game.soc.core.ResourceType;
import cc.game.soc.core.Route;
import cc.game.soc.core.SOC;
import cc.game.soc.core.SpecialVictoryType;
import cc.game.soc.core.Vertex;
import cc.game.soc.core.VertexType;
import cc.lib.game.AGraphics;
import cc.lib.game.APGraphics;
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
public final class UIPlayerRenderer implements UIRenderer {

	private GColor color = GColor.BLACK;
	
	public void setColor(GColor color) {
		this.color = color;
	}
	
	public GColor getColor() {
		return color;
	}
	
    private boolean animationEnabled = true;
	
	private long getAnimTime() {
		return 1500;//return GUI.instance.getProps().getIntProperty("anim.ms", 1500);
	}

	private UIPlayer player;
    private final UIComponent component;

    public UIPlayerRenderer(UIComponent component) {
        this.component = component;
        this.component.setRenderer(this);
    }

    public void setPlayer(UIPlayer player) {
        this.player = player;
    }

    void startStructureAnimation(UIBoardRenderer b, final Vertex vertex, final VertexType type) {
        if (!animationEnabled)
            return;
        if (vertex == null)
            return;
        b.addAnimation(new UIAnimation(getAnimTime()) {

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
                		b.drawSettlement(g, Vector2D.ZERO, player.getPlayerNum(), false);
                		break;
                	case CITY:
                		b.drawCity(g, Vector2D.ZERO, player.getPlayerNum(), false);
                		break;
                	case WALLED_CITY:
                		b.drawWalledCity(g, Vector2D.ZERO, player.getPlayerNum(), false);
                		break;
                	case METROPOLIS_POLITICS:
                        b.drawMetropolisPolitics(g, Vector2D.ZERO, player.getPlayerNum(), false);
                        break;
                	case METROPOLIS_SCIENCE:
                        b.drawMetropolisScience(g, Vector2D.ZERO, player.getPlayerNum(), false);
                        break;
                	case METROPOLIS_TRADE:
                        b.drawMetropolisTrade(g, Vector2D.ZERO, player.getPlayerNum(), false);
                        break;
                }
                g.popMatrix();
            }
        }, true);
        
    }    

    void startMoveShipAnimation(final UIBoardRenderer b, final Route source, final Route target, final SOC soc) {
    	if (!animationEnabled)
    		return;
    	if (source == null || target == null || soc == null)
    		return;
        b.addAnimation(new UIAnimation(getAnimTime()) {
			
			@Override
			public void draw(AGraphics g, float position, float dt) {
				g.setColor(getColor());
                g.pushMatrix();
                //render.translate(mp);
                //render.scale(1, position);
                Vector2D startV = soc.getBoard().getRouteMidpoint(source);
                Vector2D endV   = soc.getBoard().getRouteMidpoint(target);
                Vector2D curV   = startV.add(endV.sub(startV).scaledBy(position));
                
                float startAng  = b.getEdgeAngle(source);
                float endAng    = b.getEdgeAngle(target);
                float curAng    = startAng + (endAng - startAng) * position;
                
                b.drawShip(g, curV, Math.round(curAng), false);
                g.popMatrix();
			}
		}, true);
    }
    
    void startBuildShipAnimation(UIBoardRenderer b, final Route edge, final SOC soc) {
    	if (!animationEnabled)
    		return;
    	
    	if (edge == null || soc == null)
    		return;
    	
        b.addAnimation(new UIAnimation(getAnimTime()) {
			
			@Override
			public void draw(AGraphics g, float position, float dt) {
				g.setColor(getColor());
                g.pushMatrix();
                //render.translate(mp);
                g.scale(1, position);
                b.drawShip(g, edge, false);
                g.popMatrix();
			}
		}, true);
    }
    
    void startUpgradeShipAnimation(final UIBoardRenderer b, final Route ship) {
    	if (!animationEnabled)
    		return;
    	
    	if (ship == null)
    		return;
    	b.addAnimation(new UIAnimation(2000) {
			
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
		}, true);
    }
    
    void startRoadAnimation(final UIBoardRenderer b, final Route edge, final SOC soc) {
        if (!animationEnabled)
            return;

        if (edge == null || soc == null)
            return;
        if (edge != null) {
            b.addAnimation(new UIAnimation(getAnimTime()) {

                final Vertex A = soc.getBoard().getVertex(edge.getFrom());
                final Vertex B = soc.getBoard().getVertex(edge.getTo());
                
                @Override
                public void draw(AGraphics g, float position, float dt) {
                    Vertex from, to;
                    if (A.getPlayer() == player.getPlayerNum() || soc.getBoard().isVertexAdjacentToPlayerRoad(edge.getFrom(), player.getPlayerNum())) {
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
            }, true);
        }
    }
    
    void startKnightAnimation(final UIBoardRenderer b, final Vertex vertex) {
    	if (!animationEnabled || vertex == null)
    		return;

    	b.addAnimation(new UIAnimation(getAnimTime()) {
			
			@Override
			public void draw(AGraphics g, float position, float dt) {
                g.setColor(getColor());
                g.pushMatrix();
                g.translate(vertex.getX(), position * (vertex.getY()));
                g.scale((2f-position) * (float)Math.cos((1-position)*20), (2f-position));
                b.drawKnight(g, Vector2D.ZERO, player.getPlayerNum(), 1, false, false);
                g.popMatrix();
			}
		}, true);
    }
    
    void startMoveKnightAnimation(UIBoardRenderer b, final Vertex fromVertex, final Vertex toVertex) {
    	if (!animationEnabled || fromVertex == null || toVertex == null)
    		return;

    	b.addAnimation(new UIAnimation(getAnimTime()) {
			
			@Override
			public void draw(AGraphics g, float position, float dt) {
                g.setColor(getColor());
                g.pushMatrix();
                IVector2D pos = Vector2D.newTemp(fromVertex).add(Vector2D.newTemp(toVertex).sub(fromVertex).scaledBy(position));
                g.translate(pos);
                b.drawKnight(g, Vector2D.ZERO, player.getPlayerNum(), fromVertex.getType().getKnightLevel(), fromVertex.getType().isKnightActive(), false);
                g.popMatrix();
			}
		}, true);
    }

	protected final void doVertexAnimation(UIBoardRenderer b, Player.VertexChoice mode, Vertex v, Vertex v2) {
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

	protected final void doRouteAnimation(SOC soc, UIBoardRenderer b, Player.RouteChoice mode, Route route) {
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
    public void draw(APGraphics g, int px, int py) {
        if (player == null)
            return;

        SOC soc = UISOC.getInstance();
        g.setTextStyles(AGraphics.TextStyle.BOLD);

        StringBuffer str = new StringBuffer();
        str.append(player.getName()).append(" ").append(player.getPoints()).append(" Points Cards ")
                .append(player.getTotalCardsLeftInHand()).append( "(").append(soc.getRules().getMaxSafeCardsForPlayer(player.getPlayerNum(), soc.getBoard()))
                .append(")\n");
        if (player.isInfoVisible()) {
            for (ResourceType t : ResourceType.values()) {
                int num = player.getCardCount(t);
                str.append(t.name()).append(" X ").append(num).append("\n");
            }
            if (soc.getRules().isEnableCitiesAndKnightsExpansion()) {
                for (CommodityType t : CommodityType.values()) {
                    int num = player.getCardCount(t);
                    str.append(t.name()).append(" X ").append(num).append("\n");
                }
                for (Card c : player.getCards(CardType.Progress)) {
                    str.append(c.getName()).append("(").append(c.getCardStatus()).append(")\n");
                }
            } else {
                for (Card c : player.getCards(CardType.Development)) {
                    str.append(c.getName()).append("(").append(c.getCardStatus()).append(")\n");
                }
            }
        } else {
            if (soc.getRules().isEnableCitiesAndKnightsExpansion()) {
                int num = player.getCardCount(CardType.Resource);
                num += player.getCardCount(CardType.Commodity);
                str.append("Materials X ").append(num).append("\n");
                num = player.getCardCount(CardType.Progress);
                str.append("Progress X ").append(num).append("\n");
            } else {
                int num = player.getUnusedCardCount();
                str.append("Card X ").append(num).append("\n");
            }
        }

        int numSettlements = soc.getBoard().getNumVertsOfType(player.getPlayerNum(), VertexType.SETTLEMENT);
        int numCities      = soc.getBoard().getNumVertsOfType(player.getPlayerNum(), VertexType.CITY, VertexType.WALLED_CITY);
        int numMetros      = soc.getBoard().getNumVertsOfType(player.getPlayerNum(), VertexType.METROPOLIS_POLITICS, VertexType.METROPOLIS_SCIENCE, VertexType.METROPOLIS_TRADE);
        int numKnights     = soc.getBoard().getNumKnightsForPlayer(player.getPlayerNum());
        int knightLevel    = soc.getBoard().getKnightLevelForPlayer(player.getPlayerNum(), true, false);
        int maxKnightLevel = soc.getBoard().getKnightLevelForPlayer(player.getPlayerNum(), true, true);
        int numDiscoveredTiles = player.getNumDiscoveredTerritories();
/*
        str.append(" S X ").append(numSettlements).append(" +").append(numSettlements * soc.getRules().getPointsPerSettlement())
           .append(" C X ").append(numCities).append(" +").append(numCities * soc.getRules().getPointsPerCity())
           .append(" KL X ").append(knightLevel).append("/").append(maxKnightLevel);

        if (soc.getRules().isEnableCitiesAndKnightsExpansion()) {
        	str.append(" M X ").append(numMetros).append(" +").append(numMetros * soc.getRules().getPointsPerMetropolis());
        }
        str.append("\n");
  */
        int size = player.getArmySize();
        if (size > 0) {
            str.append("Army X ").append(size).append("\n");
        }
        if (numKnights > 0) {
            str.append("Knights ").append(knightLevel).append("/").append(maxKnightLevel).append("\n");
        }
        str.append("Road Length ").append(player.getRoadLength());
        str.append("\n");
        for (SpecialVictoryType sv : SpecialVictoryType.values()) {
            int num = player.getCardCount(sv);
            if (num > 0) {
                str.append(sv.name());
                if (sv.points != 0)
                    str.append(Utils.getSignedString(num*sv.points));
                str.append("\n");
            }
        }

        for (DevelopmentArea d : DevelopmentArea.values()) {
            if (player.getCityDevelopment(d) > 0) {
                str.append(d.name()).append(" ").append(d.levelName[player.getCityDevelopment(d)]).append(" (").append(player.getCityDevelopment(d)).append(") ");
                if (soc.getMetropolisPlayer(d) == player.getPlayerNum()) {
                    str.append(" Metro +").append(soc.getRules().getPointsPerMetropolis());
                }
                str.append("\n");
            }
        }
        if (player.getMerchantFleetTradable() != null) {
            str.append("Merchant Fleet ").append(player.getMerchantFleetTradable().getName()).append("\n");
        }

        {
            int num = soc.getBoard().getNumDiscoveredIslands(player.getPlayerNum());
            if (num > 0) {
                str.append("Discovered Islands X ").append(num).append(" +").append(num * soc.getRules().getPointsIslandDiscovery()).append("\n");
            }
        }

        if (numDiscoveredTiles > 0) {
            str.append("Discovered Tiles X " + numDiscoveredTiles).append("\n");
        }

        g.setColor(player.getColor());
        float height = g.drawWrapString(5, 5, component.getWidth(), str.toString());



        //Rectangle r = AWTUtils.drawWrapString(g, 5, 5, getWidth(), str.toString());
        //int h = r.y + r.height + 10;
        //int w = getWidth() - 1;
        if (isCurrentPlayer()) {
            g.drawRect(0, 0, component.getWidth(), component.getHeight(), 3);
        }
        //setPreferredSize(new Dimension(w, h));
        component.setMinSize(component.getWidth(), Math.round(15 + height));
    }

    final boolean isCurrentPlayer() {
        return player.getPlayerNum() == UISOC.getInstance().getCurPlayerNum();
    }

    @Override
    public void doClick() {

    }
}
