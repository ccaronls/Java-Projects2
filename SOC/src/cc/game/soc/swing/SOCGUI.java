package cc.game.soc.swing;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import cc.game.soc.core.*;
import cc.lib.game.*;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;
import cc.lib.swing.*;
import cc.lib.utils.FileUtils;

public class SOCGUI extends SOC {

	protected final GUI gui;

	SOCGUI(GUI gui) {
		this.gui = gui;
	}
	
	public GUIPlayer getCurGuiPlayer() {
        return (GUIPlayer)getCurPlayer();
    }

	/*
	 *  (non-Javadoc)
	 * @see cc.game.soc.core.SOC#printinfo(int, java.lang.String)
	 */
    @Override
	public void printinfo(int playerNum, String msg) {
        Color color = Color.BLACK;
		GUIPlayer p = gui.getGUIPlayer(playerNum);
		if (p != null) {
			color = p.getColor();
        } 
		gui.getConsole().addText(color, msg);
		log.info("P(" + playerNum + ") " + msg);
	}

    @Override
    public boolean isDebugEnabled() {
        return true;
    }

    class SlerpAnim extends AAnimation<Graphics> {

        final Rectangle start, end;
        
        SlerpAnim(Rectangle begin, Rectangle end, long duration, int maxRepeats) {
            super(duration, maxRepeats);
            this.start = begin;
            this.end = end;
        }
        
        @Override
        public void draw(Graphics g, float position, float dt) {
            float dx = end.x - start.x;
            float dy = end.y - start.y;
            float dw = end.width - start.width;
            float dh = end.height - start.height;
            drawRectangle(g, 
                    Math.round(dx*position+start.x), 
                    Math.round(dy*position+start.y), 
                    Math.round(dw*position+start.width), 
                    Math.round(dh*position+start.height));
        }
        
        void drawRectangle(Graphics g, int x, int y, int w, int h) {
            
        }
        
    }
    

    void addCardAnimation(final int player, final String text) {
        
    	final PlayerInfoComponent comp = gui.playerComponents[player];
        final List<AAnimation<Graphics>> cardsList = comp.getCardAnimations();
        final BoardComponent board = GUI.instance.getBoardComponent();

        final int cardHeight = comp.getHeight()/5;
        final int cardWidth = cardHeight*2/3;

        final Point compPt = comp.getLocationOnScreen();
        final Point boardPt = board.getLocationOnScreen();

        final int dx = compPt.x - boardPt.x;
        final int dy = compPt.y - boardPt.y;
        
        final int y = board.getY() + dy; // duh
        final int W = cardsList.size() * cardWidth + (cardsList.size()+1) * cardWidth/3;
        final int x = board.getX() + dx > 0 ? board.getWidth() - W - cardWidth : W;

        final int animTime = GUI.instance.getProps().getIntProperty("anim.card.tm", 3000);
        
        gui.getBoardComponent().addAnimation(new GAnimation(animTime) {
            public void draw(Graphics g, float position, float dt) {
                drawCard(((GUIPlayer)getPlayerByPlayerNum(player)).getColor(), g, text, x, y, cardWidth, cardHeight);
            }

            @Override
            public void onDone() {
                //playerRowCardNum[player.getPlayerNum()]--;
                synchronized (cardsList) {
                    cardsList.remove(this);
                }
            }

            @Override
            public void onStarted() {
                synchronized (cardsList) {
                    cardsList.add(this);
                }
            }

        }, false);
    }
    
    private void drawCard(Color color, Graphics g, String txt, int x, int y, int cw, int ch) {
    	gui.getBoardComponent().images.drawImage(g, gui.getBoardComponent(), gui.getBoardComponent().cardFrameImage, x, y, cw, ch);
        g.setColor(color);
        AWTUtils.drawWrapJustifiedString(g, x+cw/2, y+ch/2, cw-6, Justify.CENTER, Justify.CENTER, txt);
    }

    @Override
    protected void onCardPicked(final int player, final Card card) {
        String txt = "";
        if (((GUIPlayer)getPlayerByPlayerNum(player)).isInfoVisible()) {
        	Pattern splitter = Pattern.compile("[A-Z][a-z0-9]*");
        	Matcher matcher = splitter.matcher(card.getName());
        	while (matcher.find()) {
        		if (txt.length() > 0) {
        			txt += " ";
        		}
        		txt += matcher.group();
        	}
        }        
        addCardAnimation(player, txt);
    }

    @Override
    protected void onDistributeResources(final int player, final ResourceType type, final int amount) {
        addCardAnimation(player, type.name() + "\nX " + amount);
    }

    @Override
    protected void onDistributeCommodity(int player, final CommodityType type, final int amount) {
        addCardAnimation(player, type.name() + "\nX " + amount);
    }
    
    @Override
	protected void onProgressCardDistributed(int player, ProgressCardType type) {
        String txt = type.name();
        if (!((GUIPlayer)getPlayerByPlayerNum(player)).isInfoVisible()) {
            txt = "Progress";
        }
    	addCardAnimation(player, txt);
	}

	@Override
	protected void onSpecialVictoryCard(int player, SpecialVictoryType type) {
    	addCardAnimation(player, type.name());
	}

	@SuppressWarnings("serial")
    @Override
    protected void onGameOver(final int winner) {
        PopupButton button = new PopupButton("OK") {
            public boolean doAction() {
                gui.quitToMainMenu();
                synchronized (this) {
                    notify();
                }
                return true;
            }
        };
        gui.showPopup("A WINNER!", "Player " + winner + "\n Wins!", button);
        try {
            synchronized (button) {
                button.wait();
            }        
        } catch (Exception e) {
            //gui.quitToMainMenu();
        }
    }

    @Override
    protected void onLargestArmyPlayerUpdated(final int oldPlayer, final int newPlayer, final int armySize) {
    	if (newPlayer > 0)
    		addCardAnimation(newPlayer, "Largest Army");
    	if (oldPlayer > 0)
    		addCardAnimation(oldPlayer, "Largest Army Lost!");
    }

    @Override
    protected void onLongestRoadPlayerUpdated(final int oldPlayer, final int newPlayer, final int maxRoadLen) {
    	if (newPlayer > 0)
    		addCardAnimation(newPlayer, "Longest Road");
    	if (oldPlayer > 0)
    		addCardAnimation(oldPlayer, "Longest Road Lost!");
    }

    @Override
	protected void onHarborMasterPlayerUpdated(int oldPlayer, int newPlayer, int harborPts) {
    	if (newPlayer > 0)
    		addCardAnimation(newPlayer, "Harbor Master");
    	if (oldPlayer > 0)
    		addCardAnimation(oldPlayer, "Harbor Master Lost!");
	}

	@Override
    protected void onMonopolyCardApplied(final int taker, final int giver, final ICardType<?> type, final int amount) {
        addCardAnimation(giver, type.name() + "\n- " + amount);
        addCardAnimation(taker, type.name() + "\n+ " + amount);
    }

    @Override
    protected void onPlayerPointsChanged(final int player, final int changeAmount) {
    }

    @Override
    protected void onTakeOpponentCard(final int taker, final int giver, final Card card) {
        addCardAnimation(giver, card.getName() + "\n-1");
        addCardAnimation(taker, card.getName() + "\n+1");
    }

    @Override
    protected void onPlayerRoadLengthChanged(int p, int oldLen, int newLen) {
    	if (oldLen > newLen)
    		addCardAnimation(p, "Route Reduced!\n" + "-" + (oldLen - newLen));
    	else
    		addCardAnimation(p, "Route Increased!\n" + "+" + (newLen - oldLen));
    }

    @Override
    protected void onTradeCompleted(int player, Trade trade) {
        addCardAnimation(player, "Trade\n" + trade.getType() + "\n -" + trade.getAmount());
    }
    
    @Override
	protected void onBarbariansAdvanced(int distanceAway) {
	}

	@Override
	protected void onBarbariansAttack(int catanStrength, int barbarianStrength, String [] playerStatus) {
		gui.frame.repaint();
    	StringBuffer str = new StringBuffer("Barbarian Attack!\n\nBarbarian Strength ")
    		.append(barbarianStrength)
    		.append("\nCatan Strength ")
    		.append(catanStrength)
    		.append("\n");
		for (Player p : getPlayers()) {
			str.append(p.getName()).append(" ").append(playerStatus[p.getPlayerNum()]).append("\n");
		}
    	JOptionPane.showMessageDialog(gui.frame,
    		    str,
    		    "Barbarian Attack",
    		    JOptionPane.PLAIN_MESSAGE);
	}

	@Override
	protected void onDiceRolled(Dice...  dice) {
    	gui.spinDice(dice);
        gui.setDice(dice);
        /*
        try {
			FileUtils.backupFile(gui.saveGameFile.getAbsolutePath(), 10);
			save(gui.saveGameFile.getAbsolutePath());
		} catch (Exception e) {
			e.printStackTrace();
		}*/
	}

	
	@Override
	protected void onEventCardDealt(EventCard card) {
	}

	@Override
	protected void onPlayerDiscoveredIsland(int player, int island) {
		addCardAnimation(player, "Island " + island + "\nDiscovered!");
	}

	@Override
	protected void onDiscoverTerritory(int player, int tile) {
		addCardAnimation(player, "Territory\nDiscovered");
	}
	
	
	@Override
	protected void onMetropolisStolen(int loser, int stealer, DevelopmentArea area) {
		addCardAnimation(loser, "Metropolis\n" + area.name() + "\nLost!");
		addCardAnimation(stealer, "Metropolis\n" + area.name() + "\nStolen!");
	}

	@Override
	protected void onTilesInvented(int player, final int tileIndex0, final int tileIndex1) {
        final Tile tile0 = getBoard().getTile(tileIndex0);
        final Tile tile1 = getBoard().getTile(tileIndex1);
		final int t1 = tile1.getDieNum();
		final int t0 = tile0.getDieNum();
		final Vector2D [] pts0 = new Vector2D[31];
		final Vector2D [] pts1 = new Vector2D[31];
		final Vector2D mid = Vector2D.newTemp(tile0).add(tile1).scaleEq(0.5f);
		final Vector2D d = Vector2D.newTemp(mid).sub(tile0).scaleEq(0.5f);
		final Vector2D mid0 = Vector2D.newTemp(tile0).add(d);
		final Vector2D mid1 = Vector2D.newTemp(tile1).sub(d);
		final MutableVector2D dv = d.norm().scaleEq(0.7f);
		Utils.computeBezierCurvePoints(pts0, 30, tile0, mid0.add(dv), mid1.add(dv), tile1);
		pts0[30]=pts0[29];
		Utils.computeBezierCurvePoints(pts1, 30, tile1, mid1.sub(dv), mid0.sub(dv), tile0);
		pts1[30]=pts1[29];
		final Renderer render = gui.getBoardComponent().render;
		gui.getBoardComponent().addAnimation(new GAnimation(3000) {
			
			void drawChit(Graphics g, Vector2D [] pts, float position, int num) {
				int index0 = (int)(position*28);
				int index1 = index0+1;
				float delta = ((position*28)-index0);
				assert(position >= 0 && position <= 1);
				Vector2D pos = pts[index0].scaledBy(1.0f-delta).add(pts[index1].scaledBy(delta));
				Vector2D t = render.transformXY(pos);
				int fh = AWTUtils.getFontHeight(g);
				int x = Math.round(t.getX() - fh);
				int y = Math.round(t.getY() - fh);
				gui.getBoardComponent().drawCellProductionValue(g, x, y, num, BoardComponent.TILE_CELL_NUM_RADIUS);
			}
			
			@Override
			public void draw(Graphics g, float position, float dt) {
				drawChit(g, pts0, position, t1);
				drawChit(g, pts1, position, t0);
			}

			@Override
            public void onStarted() {
				tile0.setDieNum(0);
				tile1.setDieNum(0);
			}
			
		}, true);
		tile0.setDieNum(t0);
		tile1.setDieNum(t1);
	}

	@Override
	protected void onPlayerShipUpgraded(int p, int r) {
	}

	@Override
	protected void onPirateSailing(final int fromTile, final int toTile) {
		gui.getBoardComponent().addAnimation(new GAnimation(800) {
			
			@Override
            public void draw(Graphics g, float position, float dt) {
				Vector2D v = Vector2D.newTemp(getBoard().getTile(fromTile)).scaledBy(1-position).add(Vector2D.newTemp(getBoard().getTile(toTile)).scaledBy(position));
				gui.getBoardComponent().drawPirate(g, v);
			}
			
		}, true);
	}

	@Override
	protected void onCardLost(int p, Card c) {
		addCardAnimation(p, c.getName() + "\n-1");
	}

	@Override
	protected void onPirateAttack(int p, int playerStrength, int pirateStrength) {
		StringBuffer str = new StringBuffer("Pirates attack " + getPlayerByPlayerNum(p).getName())
			.append("\nPlayer Strength " + playerStrength)
			.append("\nPirate Stength " + pirateStrength)
			.append("\n");
        	JOptionPane.showMessageDialog(gui.frame,
        		    str,
        		    "Pirate Attack",
        		    JOptionPane.PLAIN_MESSAGE);	
        	
	}

	@Override
	protected void onPlayerConqueredPirateFortress(int p, int v) {
		StringBuffer str = new StringBuffer("Player " + getPlayerByPlayerNum(p).getName() + " has conquered the fortress!");
    	JOptionPane.showMessageDialog(gui.frame,
    		    str,
    		    "Pirate Attack",
    		    JOptionPane.PLAIN_MESSAGE);	
	}

	@Override
	protected void onPlayerAttacksPirateFortress(int p, int playerHealth, int pirateHealth) {
		String result = null;
		if (playerHealth > pirateHealth)
			result = "Player damages the fortress";
		else if (playerHealth < pirateHealth)
			result = "Player loses battle and 2 ships";
		else
			result = "Battle is a draw.  Player lost a ship";
		StringBuffer str = new StringBuffer(getPlayerByPlayerNum(p).getName() + " attackes the pirate fortress!")
		.append("\nPlayer Strength " + playerHealth)
		.append("\nPirate Stength " + pirateHealth)
		.append("\n")
		.append("Result: " + result + "\n");
    	JOptionPane.showMessageDialog(gui.frame,
    		    str,
    		    "Pirate Attack",
    		    JOptionPane.PLAIN_MESSAGE);	
	}

	@Override
	protected void onAqueduct(int playerNum) {
		addCardAnimation(playerNum, "Aqueduct Ability!");
	}

	@Override
	protected void onPlayerAttackingOpponent(int attackerIndex, int victimIndex, String attackingWhat, int attackerScore, int victimScore) {
        Player attacker = getPlayerByPlayerNum(attackerIndex);
        Player victim = getPlayerByPlayerNum(victimIndex);
		String message = attacker.getName() + " is attacking " + victim.getName() + "'s " + attackingWhat + "\n"
				       + attacker.getName() + "'s score : " + attackerScore + "\n"
				       + victim.getName() + "'s score : " + victimScore;
		JOptionPane.showMessageDialog(gui.frame, message, "Player Attack!", JOptionPane.PLAIN_MESSAGE);
	}

	@Override
	protected void onRoadDestroyed(int r, int destroyer, int victim) {
		addCardAnimation(victim, "Road Destroyed!");
	}

	@Override
	protected void onStructureDemoted(int v, VertexType newType, int destroyer, int victim) {
		addCardAnimation(victim, getBoard().getVertex(v).getType().getNiceName() + " Reduced to " + newType.getNiceName());
	}
    
	@Override
	protected void onShouldSaveGame() {
		FileUtils.backupFile(gui.saveGameFile.getAbsolutePath(), 10);
		save(gui.saveGameFile.getAbsolutePath());
	}

	@Override
	protected void onExplorerPlayerUpdated(int oldPlayer, int newPlayer, int harborPts) {
		if (oldPlayer > 0)
            addCardAnimation(oldPlayer, "Explorer Lost!");
		addCardAnimation(newPlayer, "Explorer Gained!");
	}

	@Override
	protected void onPlayerKnightDestroyed(int player, int knight) {
		addFloatingTextAnimation((GUIPlayer)getPlayerByPlayerNum(player), getBoard().getVertex(knight), "Knight\nDestroyed");
	}

	@Override
	protected void onPlayerKnightDemoted(int player, int knightIndex) {
        Vertex knight = getBoard().getVertex(knightIndex);
		addFloatingTextAnimation((GUIPlayer)getPlayerByPlayerNum(player), knight, "Demoted to\n" + knight.getType().getNiceName());
	}

	void addFloatingTextAnimation(final GUIPlayer player, final IVector2D v, final String txt) {
		GUI.instance.getBoardComponent().addAnimation(new GAnimation(2000) {
			
			@Override
            public void draw(Graphics g, float position, float dt) {
				g.setColor(player.getColor());
				Renderer r = GUI.instance.getBoardComponent().render;
				r.pushMatrix();
				 r.translate(v);
				 r.translate(0, -GUI.instance.getBoardComponent().getKnightRadius()*5);
				 r.translate(0, -GUI.instance.getBoardComponent().getKnightRadius()*10*position);
				 MutableVector2D mv = new MutableVector2D();
				 r.transformXY(mv);
				 AWTUtils.drawJustifiedString(g, mv.Xi(), mv.Yi(), Justify.CENTER, Justify.CENTER, txt);
				r.popMatrix();
			}
		}, true);
		
	}
	
	@Override
	protected void onPlayerKnightPromoted(int player, final int knightIndex) {
        Vertex knight = getBoard().getVertex(knightIndex);
		addFloatingTextAnimation((GUIPlayer)getPlayerByPlayerNum(player), knight, "Promoted to\n" + knight.getType().getNiceName());
	}

	@Override
	protected void onPlayerCityDeveloped(int p, DevelopmentArea area) {
		addCardAnimation(p, area.name() + "\n\n" + area.levelName[getPlayerByPlayerNum(p).getCityDevelopment(area)]);
	}
	
	
}
