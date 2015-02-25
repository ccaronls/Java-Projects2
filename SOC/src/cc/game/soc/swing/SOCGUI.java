package cc.game.soc.swing;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.game.soc.core.*;
import cc.lib.game.*;
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
    

    void addCardAnimation(final Player _player, final String text) {
        
        final GUIPlayer player = (GUIPlayer)_player;
        final List<Animation> cardsList = player.cardAnimations;

        final int cardWidth = 64;
        final int cardHeight = 96;

        int X = cardWidth/2 + cardWidth * cardsList.size(); 
        int Y = player.getPlayerNum() * 64 + 16;
        
        if (true) { //gui.useNewLayoutType) {
            switch (player.loc) {
                case CL_NONE:
                    return; // skip animations
                case CL_UPPER_LEFT:
                    X = cardWidth/2 + cardWidth * cardsList.size();
                    Y = cardHeight + 16;
                    break;
                case CL_MIDDLE_LEFT:
                    X = cardWidth/2 + cardWidth * cardsList.size();
                    Y = cardHeight*2 + 16;
                    break;
                case CL_UPPER_RIGHT:
                    X = gui.getBoardComponent().getWidth() - cardWidth - cardWidth/2 - cardWidth*cardsList.size();
                    Y = cardHeight + 16;
                    break;
                case CL_MIDDLE_RIGHT:
                    X = gui.getBoardComponent().getWidth() - cardWidth - cardWidth/2 - cardWidth*cardsList.size();
                    Y = cardHeight*2 + 16;
                    break;
                case CL_LOWER_RIGHT:
                    X = gui.getBoardComponent().getWidth() - cardWidth - cardWidth/2 - cardWidth*cardsList.size();
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
                drawCard(player.getColor(), g, text, x, y, cardWidth, cardHeight);
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
    protected void onMonopolyCardApplied(final Player taker, final Player giver, final ICardType type, final int amount) {
        addCardAnimation(giver, "Monopoly" + type.name() + "\n- " + amount);
        addCardAnimation(taker, "Monopoly" + type.name() + "\n+ " + amount);
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
	protected void onDiceRolled(int... dice) {
    	gui.spinDice();
        gui.waitForReturnValue(true);
        gui.setDice(dice);
	}

	@Override
	protected void onPlayerdiscoveredIsland(Player player, Island island) {
		// TODO Auto-generated method stub
		super.onPlayerdiscoveredIsland(player, island);
	}

	@Override
	protected void onDiscoverTerritory(Player player, Tile tile) {
		// TODO Auto-generated method stub
		super.onDiscoverTerritory(player, tile);
	}

	@Override
    protected Player instantiatePlayer(String className) throws Exception {
        Class<?> clazz = getClass().getClassLoader().loadClass(className);
        return (Player)clazz.getConstructor(GUI.class).newInstance(gui);
    }

    public GUIPlayer getCurGuiPlayer() {
        return (GUIPlayer)getCurPlayer();
    }

    
}
