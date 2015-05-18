package cc.game.soc.swing;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import cc.game.soc.core.*;
import cc.lib.game.*;
import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;
import cc.lib.swing.*;

public class SOCGUI extends SOC {

	protected final GUI gui;

	SOCGUI(GUI gui) {
		this.gui = gui;
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
		gui.logInfo("P(" + playerNum + ") " + msg);		
	}

    @Override
    public boolean isDebugEnabled() {
        return true;
    }

    @Override
    public void logDebug(String msg) {
        gui.logDebug(msg);
    }

    @Override
    public void logError(String msg) {
        gui.logError(msg);
    }

    class SlerpAnim extends Animation {

        final Rectangle start, end;
        
        SlerpAnim(Rectangle begin, Rectangle end, long duration, int maxRepeats) {
            super(duration, maxRepeats);
            this.start = begin;
            this.end = end;
        }
        
        @Override
        void draw(Graphics g, float position, float dt) {
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
    

    void addCardAnimation(final Player player, final String text) {
        
    	PlayerInfoComponent comp = gui.playerComponents[player.getPlayerNum()];
        final List<Animation> cardsList = comp.getCardAnimations();

        final int cardWidth = 64;
        final int cardHeight = 96;

        int X = cardWidth/2 + cardWidth * cardsList.size(); 
        int Y = player.getPlayerNum() * 64 + 16;
        
        if (true) { //gui.useNewLayoutType) {
            switch (comp.getCardLoc()) {
                case CL_NONE:
                    return; // skip animations
                case CL_UPPER_LEFT:
                    X = cardWidth/2 + cardWidth*2/3 * cardsList.size();
                    Y = cardHeight + 16;
                    break;
                case CL_MIDDLE_LEFT:
                    X = cardWidth/2 + cardWidth*2/3 * cardsList.size();
                    Y = cardHeight*2 + 16;
                    break;
                case CL_UPPER_RIGHT:
                    X = gui.getBoardComponent().getWidth() - cardWidth - cardWidth/2 - cardWidth*2/3*cardsList.size();
                    Y = cardHeight + 16;
                    break;
                case CL_MIDDLE_RIGHT:
                    X = gui.getBoardComponent().getWidth() - cardWidth - cardWidth/2 - cardWidth*2/3*cardsList.size();
                    Y = cardHeight*2 + 16;
                    break;
                case CL_LOWER_RIGHT:
                    X = gui.getBoardComponent().getWidth() - cardWidth - cardWidth/2 - cardWidth*2/3*cardsList.size();
                    Y = cardHeight*3 + 16;
                    break;
                default:
                    assert(false);
                
            }
        }

        final int x = X;
        final int y = Y;
        final int animTime = GUI.instance.getProps().getIntProperty("anim.card.tm", 3000);
        
        gui.getBoardComponent().addAnimation(new Animation(animTime, 0) {
            void draw(Graphics g, float position, float dt) {
                drawCard(((GUIPlayer)player).getColor(), g, text, x, y, cardWidth, cardHeight);
            }

            @Override
            void onDone() {
                //playerRowCardNum[player.getPlayerNum()]--;
                synchronized (cardsList) {
                    cardsList.remove(this);
                }
            }

            @Override
            void onStarted() {
                synchronized (cardsList) {
                    cardsList.add(this);
                }
            }
            
            
            
            
        });
    }
    
    private void drawCard(Color color, Graphics g, String txt, int x, int y, int cw, int ch) {
        int fontHeight = AWTUtils.getFontHeight(g);
    	gui.getBoardComponent().images.drawImage(g, gui.getBoardComponent().cardFrameImage, x, y, cw, ch);
        g.setColor(color);
        String [] lines = AWTUtils.generateWrappedLines(g, txt, cw-2);
        y = y+ch/2 - (lines.length-1)*fontHeight;
        for (String line : lines) {
        	AWTUtils.drawJustifiedString(g, x+cw/2, y, Justify.CENTER, Justify.CENTER, line);
        	y += fontHeight;
        }
    }

    @Override
    protected void onCardPicked(final Player player, final Card card) {
        String txt = "";
        if (((GUIPlayer)player).isInfoVisible()) {
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
    protected void onDistributeResources(final Player player, final ResourceType type, final int amount) {
        addCardAnimation(player, type.name() + "\nX " + amount);
    }

    @Override
    protected void onDistributeCommodity(final Player player, final CommodityType type, final int amount) {
        addCardAnimation(player, type.name() + "\nX " + amount);
    }
    
    @Override
	protected void onProgressCardDistributed(Player player, ProgressCardType type) {
    	addCardAnimation(player, type.name());
	}

	@Override
	protected void onSpecialVictoryCard(Player player, SpecialVictoryType type) {
    	addCardAnimation(player, type.name());
	}

	@SuppressWarnings("serial")
    @Override
    protected void onGameOver(final Player winner) {
        PopupButton button = new PopupButton("OK") {
            public boolean doAction() {
                gui.quitToMainMenu();
                synchronized (this) {
                    notify();
                }
                return true;
            }
        };
        gui.showPopup("A WINNER!", "Player " + winner.getPlayerNum() + "\n Wins!", button);
        try {
            synchronized (button) {
                button.wait();
            }        
        } catch (Exception e) {
            gui.quitToMainMenu();
        }
    }

    @Override
    protected void onLargestArmyPlayerUpdated(final Player oldPlayer, final Player newPlayer, final int armySize) {
        addCardAnimation(newPlayer, "Largest Army");
    }

    @Override
    protected void onLongestRoadPlayerUpdated(final Player oldPlayer, final Player newPlayer, final int maxRoadLen) {
    	if (newPlayer != null)
    		addCardAnimation(newPlayer, "Longest Road");
    }

    @Override
    protected void onMonopolyCardApplied(final Player taker, final Player giver, final ICardType<?> type, final int amount) {
        addCardAnimation(giver, type.name() + "\n- " + amount);
        addCardAnimation(taker, type.name() + "\n+ " + amount);
    }

    @Override
    protected void onPlayerPointsChanged(final Player player, final int changeAmount) {
    }

    @Override
    protected void onTakeOpponentCard(final Player taker, final Player giver, final Card card) {
        addCardAnimation(giver, card.getName() + "\n-1");
        addCardAnimation(taker, card.getName() + "\n+1");
    }

    @Override
    protected void onPlayerRoadBlocked(Player player, Route road) {
    }

    @Override
    protected void onTradeCompleted(Player player, Trade trade) {
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
	protected void onDiceRolled(int... dice) {
    	gui.spinDice(dice);
        gui.setDice(dice);
	}

	
	@Override
	protected void onEventCardDealt(EventCard card) {
		gui.setDice(0, card.getCakEvent());
	}

	@Override
	protected void onPlayerdiscoveredIsland(Player player, Island island) {
		addCardAnimation(player, "Island " + island.getNum() + "\nDiscovered!");
	}

	@Override
	protected void onDiscoverTerritory(Player player, Tile tile) {
		addCardAnimation(player, "Territory\nDiscovered");
	}
	
	
	@Override
	protected void onMetropolisStolen(Player loser, Player stealer, DevelopmentArea area) {
		addCardAnimation(loser, "Metropolis\n" + area.name() + "\nLost!");
		addCardAnimation(stealer, "Metropolis\n" + area.name() + "\nStolen!");
	}

	@Override
	protected void onTilesInvented(Player player, final Tile tile0, final Tile tile1) {
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
		final AWTRenderer render = gui.getBoardComponent().render;
		gui.getBoardComponent().addAnimation(new Animation(3000, 0) {
			
			void drawChit(Graphics g, Vector2D [] pts, float position, int num) {
				int index0 = (int)(position*28);
				int index1 = index0+1;
				float delta = ((position*28)-index0);
				assert(position >= 0 && position <= 1);
				Vector2D pos = pts[index0].scale(1.0f-delta).add(pts[index1].scale(delta));
				Vector2D t = render.transformXY(pos);
				int fh = AWTUtils.getFontHeight(g);
				int x = Math.round(t.getX() - fh);
				int y = Math.round(t.getY() - fh);
				gui.getBoardComponent().drawCellProductionValue(g, x, y, num);
			}
			
			@Override
			void draw(Graphics g, float position, float dt) {
				drawChit(g, pts0, position, t1);
				drawChit(g, pts1, position, t0);
			}

			@Override
			void onDone() {
				synchronized (SOCGUI.this) {
					SOCGUI.this.notify();
				}
			}

			@Override
			void onStarted() {
				tile0.setDieNum(0);
				tile1.setDieNum(0);
			}
			
		});
		
		// block until animation completes
		synchronized (this) {
			try {
				wait(4000);
			} catch (Exception e) {}
		}
		tile0.setDieNum(t0);
		tile1.setDieNum(t1);
	}

	public GUIPlayer getCurGuiPlayer() {
        return (GUIPlayer)getCurPlayer();
    }

    
}
