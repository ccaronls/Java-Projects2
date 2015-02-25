package cc.game.soc.core;

import java.io.*;
import java.util.*;

import cc.game.soc.core.Board.IVisitor;
import cc.game.soc.core.Player.CardChoice;
import cc.game.soc.core.Player.EnumChoice;
import cc.game.soc.core.Player.PlayerChoice;
import cc.game.soc.core.Player.RouteChoice;
import cc.game.soc.core.Player.RouteChoiceType;
import cc.game.soc.core.Player.TileChoice;
import cc.game.soc.core.Player.VertexChoice;
import cc.lib.game.Utils;
import cc.lib.math.CMath;
import cc.lib.utils.Reflector;

/**
 * SOC Core business logic
 * 
 * Add player instances then call runGame() until isGameOver()
 * Make sure to initialize the board otherwise a compltly random one will get generated
 * 
 * @author Chris Caron
 * 
 */
public class SOC extends Reflector<SOC> {
	
	public static final int MAX_PLAYERS = 8;
	
	public static final int	NUM_RESOURCE_TYPES			= ResourceType.values().length;
	public static final int	NUM_DEVELOPMENT_CARD_TYPES	= DevelopmentCardType.values().length;
	public static final int NUM_PROGRESS_CARD_TYPES		= ProgressCardType.values().length;
	public static final int NUM_COMMODITY_TYPES			= CommodityType.values().length;
	public static final int NUM_DEVELOPMENT_AREAS		= DevelopmentArea.values().length;
	
    private interface UndoAction {
        void undo();
    }
    
    static {
    	addField(StackItem.class, "state");
    	//addField(StackItem.class, "data");
    }
    
    public static class StackItem extends Reflector<StackItem> {
        
    	public StackItem() {
    		this(null, null);
    	}

    	public StackItem(State state, UndoAction action, Object data) {
    		this.state = state;
    		this.action = action;
    		this.data = data;
    	}

    	public StackItem(State state, UndoAction action) {
            this(state, action, 0);
        }

        final State state;
        final UndoAction action;
        final Object data;
        
        public String toString() {
        	return state.name() + (data == null ? "" : "(" + data + ")");
        }
    };
    
    static {
    	
    	addAllFields(SOC.class);
    	removeField(SOC.class, "mOptions");
    	/*
    	, "mPlayers");
    	addField(SOC.class, "mCurrentPlayer");
    	addField(SOC.class, "mNumPlayers");
    	addField(SOC.class, "mDie1");
    	addField(SOC.class, "mDie2");
    	addField(SOC.class, "mLongestRoadPlayer");
    	addField(SOC.class, "mLargestArmyPlayer");
    	addField(SOC.class, "mStateStack");
    	addField(SOC.class, "mDeck");
    	addField(SOC.class, "mBoard");
    	addField(SOC.class, "mRules");
    	addField(SOC.class, "mDice");*/
    }
    
	private final Player[]   	mPlayers = new Player[MAX_PLAYERS];
	private int					mCurrentPlayer;
	private int					mNumPlayers;
	private int []				mDice;
	private int				    mLongestRoadPlayer;
	private int				    mLargestArmyPlayer;
	private Stack<StackItem>	mStateStack = new Stack<StackItem>();
	private List<Card>			mDevelopmentCards = new ArrayList<Card>();
	private List<Card>[]		mProgressCards;
	private Board				mBoard;
	private Rules				mRules;
	private int					mBarbarianDistance; // CAK
	
    @SuppressWarnings("rawtypes")
    private List				mOptions;	
	//private Trade            	mSaveTrade;
	
	public final State getState() {
		return mStateStack.peek().state;
	}
	
	private final Object getStateData() {
		return mStateStack.peek().data;
	}
	
	private UndoAction getUndoAction() {
	    return mStateStack.peek().action;
	}
	
	/**
	 * Get the playernum with the longest road
	 * @return
	 */
	public int getLongestRoadPlayerNum() {
		return mLongestRoadPlayer;
	}

	/**
	 * Get the playernum with the largest ary
	 * @return
	 */
	public int getLargestArmyPlayerNum() {
		return mLargestArmyPlayer;
	}

	/**
	 * Get number of attached players
	 * @return
	 */
	public int getNumPlayers() {
		return mNumPlayers;
	}

	/**
	 * Get the current player number.  If game not in progress then return 0.
	 * @return
	 */
	public int getCurPlayerNum() {
		if (mCurrentPlayer < 0)
			return 0;
		
		return mPlayers[mCurrentPlayer].getPlayerNum();
	}
	
	/**
	 * 
	 * @param playerNum
	 */
	public void setCurrentPlayer(int playerNum) {
		for (int i=0; i<mNumPlayers; i++) {
			if (mPlayers[i].getPlayerNum() == playerNum) {
				mCurrentPlayer = i;
				break;
			}
		}
	}

	/**
	 * Get value of first die
	 * @return
	 */
	public int [] getDice() {
		return mDice;
	}

	/**
	 * Get sum of both 6 sided die 
	 * @return
	 */
	public int getDiceNum() {
		return mDice[0] + mDice[1];
	}

	/**
	 * 
	 * @return
	 */
	public Board getBoard() {
		return mBoard;
	}

	/**
	 * 
	 * @return
	 */
	public Player getCurPlayer() {
		return mPlayers[mCurrentPlayer];
	}

	/**
	 * 
	 * @param className
	 * @return
	 * @throws Exception
	 */
	protected Player instantiatePlayer(String className) throws Exception {
	    return (Player)getClass().getClassLoader().loadClass(className).newInstance();
	}

	/**
	 * 
	 * @return
	 */
	public Tile getRobberCell() {
		return getBoard().getTile(getBoard().getRobberTile());
	}
	
	/**
	 * 
	 */
	public SOC() {
	    mBoard = new Board();
	    mBoard.generateDefaultBoard();
	    mRules = new Rules();
	}

	/**
	 * Get the interface for getting game settings.  Never returns null  
	 * @return
	 */
	public final Rules getRules() {
		return mRules;
    }
	
	/**
	 * 
	 * @param rules
	 */
	public final void setRules(Rules rules) {
		this.mRules = rules;
	}
    
	/**
	 * Resets game but keeps the players
	 */
	public void reset() {
		mLongestRoadPlayer = -1;
		mLargestArmyPlayer = -1;
//		Utils.fillArray(mDice, 0);
		mBoard.reset();
		mStateStack.clear();
		resetOptions();
	}
	
	/**
	 * Resets and removes all the players
	 */
	public void clear() {
		reset();
	    Arrays.fill(mPlayers,  null);
	    mNumPlayers = 0;
        mCurrentPlayer = -1;
	}
	
	private void resetOptions() {
		mOptions = null;
	}

	@SuppressWarnings("unchecked")
	private void initDeck() {
		mDevelopmentCards.clear();
		if (mRules.isEnableCitiesAndKnightsExpansion()) {
			mProgressCards = new List[NUM_DEVELOPMENT_AREAS];
			for (ProgressCardType p :  ProgressCardType.values()) {
				mProgressCards[p.ordinal()] = new ArrayList<Card>();
				for (int i=0; i<p.deckOccurances; i++) {
					mProgressCards[p.type.ordinal()].add(new Card(p, CardStatus.USABLE));
				}
			}
			for (int i=0; i<mProgressCards.length; i++)
				Utils.shuffle(mProgressCards);
		} else {
			for (DevelopmentCardType d : DevelopmentCardType.values()) {
				for (int i=0; i<d.deckOccurances; i++)
					mDevelopmentCards.add(new Card(d, CardStatus.USABLE));
			}
			Utils.shuffle(mDevelopmentCards);
		}
	}
	
	/**
	 * 
	 * @param board
	 */
	public void setBoard(Board board) {
		mBoard = board;
	}

	/**
	 * 
	 * @param playerNum range is [1-numPlayers] inclusive
	 * @return null if player num out of range, the player with num otherwise
	 */
	public Player getPlayerByPlayerNum(int playerNum) {
		if (playerNum < 1 || playerNum > MAX_PLAYERS)
			return null;
		return mPlayers[playerNum-1];
	}
	
	// package access for unit tests
    void pushStateFront(State state) {
        pushStateFront(state, null, null);
    }

    private void pushStateFront(State state, Object data, UndoAction action) {
    	if (mStateStack.size() != 0) {
    		assert(state.canCancel || !canCancel()); // we dont want to push a non-cancel state on top of a cancel state...that would be weird
    	}
	    mStateStack.add(new StackItem(state, action, data));
	}

	/**
	 * Log an error.  Base version writes to stderr
	 * @param msg
	 */
	public void logError(String msg) {
	    System.err.println("ERROR: " + msg);
	}
	
	/**
	 * Log a debug.  Base version writes to stdout.
	 * @param msg
	 */
	public void logDebug(String msg) {
	    System.out.println("DEBUG: " + msg);
	}
	
	/**
	 * Override to enable/disable some logging.  Base version always returns true.
	 * @return
	 */
	public boolean isDebugEnabled() {
	    return true;
	}
	
	/**
	 * 
	 * @param player
	 */
	public void addPlayer(Player player) {
		if (mNumPlayers == MAX_PLAYERS)
			throw new RuntimeException("Too many players");
		
		mPlayers[mNumPlayers++] = player;
		
		if (player.getPlayerNum() == 0)
			player.setPlayerNum(mNumPlayers);
        logDebug("AddPlayer num = " + player.getPlayerNum() + " " + player.getClass().getSimpleName());
	}

	private void incrementCurPlayer(int num) {
		int nextPlayer = (mCurrentPlayer + mNumPlayers + num) % mNumPlayers;
		logDebug("Increment player [" + num + "] positions, was " + getCurPlayer().getPlayerNum() + ", now " + mPlayers[nextPlayer].getPlayerNum());
		mCurrentPlayer = nextPlayer;
	}

	/*
	 * 
	 */
	private void popState() {
		logDebug("Popping state " + getState());
		assert (mStateStack.size() > 0);
		mStateStack.pop();
		logDebug("Setting state to " + (getState()));
	}

	// package access for unit tests
	void setDice(int [] dice) {
		Utils.copyElems(mDice, dice);
	}
	
	/**
	 * 
	 * @param r
	 */
	public void rollDice() {
		for (int i=0; i<mDice.length; i++)
			mDice[i] = Utils.rand() % 6 + 1;
		onDiceRolled(mDice);
		if (mDice.length == 2) {
			printinfo("Die roll: " + mDice[0] + ", " + mDice[1]);
		} else {
			printinfo("Die roll: " + mDice[0] + ", " + mDice[1] + ", " + DiceEvent.fromDieNum(mDice[2]));
		}
	}
/*
	private void distributeCards(int dieRoll) {
		printinfo("Dealing cards ...");
		final boolean cak = getRules().isEnableCitiesAndKnightsExpansion();
		int [] playersOnGold = new int[getNumPlayers()+1];
		for (Tile t : mBoard.getTiles()) {
			if (!t.isDistributionTile())
				continue;
			
			if (dieRoll != 0 && t.getDieNum() != dieRoll)
				continue;
			
			for (Vertex v : mBoard.getTileVertices(t)) {
				if (v.getPlayer() == 0)
					continue;
				if (t.getType() == TileType.GOLD) {
					playersOnGold[v.getPlayer()] ++;
					continue;
				}
				
				Player player = getPlayerByPlayerNum(v.getPlayer());
				
				switch (v.getType()) {
					case CITY:
					case WALLED_CITY:
					case METROPOLIS_POLITICS:
					case METROPOLIS_SCIENCE:
					case METROPOLIS_TRADE:
						if (cak && t.getCommodity() != null) {
							player.addCard(t.getCommodity());
						} else {
							player.addCard(t.getResource());
						}
					case SETTLEMENT:
						player.addCard(t.getResource());
						break;
					default:
						// ok
				}
			}
		}
		
		
		
		
		for (int i=0; i<mNumPlayers; i++) {

			Player cur = mPlayers[i];
			String msg = "Player " + cur.getName() + " gets";
			for (ResourceType r : ResourceType.values()) {
				int num = cur.getCardCount(r);
				if (num > 0) {
					msg += " " + num + " X " + r;
					this.onDistributeResources(cur, r, num);
				}
			}
			
			for (CommodityType c : CommodityType.values()) {
				int num = cur.getCardCount(c);
				if (num > 0) {
					msg += " " + num + " X " + c;
					this.onDistributeCommodity(cur, c, num);
				}
			}
			
			printinfo(msg);
		} 
	}

	/**
	 * Called for Every resource bundle a player recieves.  
	 * Called once for each player, for each resource.  
	 * default method does nothing.
	 * @param player
	 * @param type
	 * @param amount
	 */
	protected void onDistributeResources(Player player, ResourceType type, int amount) {}
	protected void onDistributeCommodity(Player player, CommodityType type, int amount) {}
	private void distributeResources(int diceRoll) {
		// collect info to be displayed at the end
		int [][] resourceInfo = new int[NUM_RESOURCE_TYPES][];
		for (int i = 0; i < resourceInfo.length; i++) {
			resourceInfo[i] = new int[getNumPlayers()+1];
		}
		int [][] commodityInfo = new int[NUM_COMMODITY_TYPES][]; 
		for (int i=0; i<commodityInfo.length; i++) {
			commodityInfo[i] = new int[getNumPlayers()+1];
		}
		if (diceRoll > 0)
			printinfo("Distributing resources for num " + diceRoll);

		// visit all the cells with dice as their num
		for (int i = 0; i < mBoard.getNumTiles(); i++) {

			Tile cell = mBoard.getTile(i);
			if (!cell.isDistributionTile())
				continue;
			assert(cell.getDieNum() != 0);
			if (mBoard.getRobberTile() == i)
				continue; // apply the robber

			if (diceRoll > 0 && cell.getDieNum()!= diceRoll)
				continue;

			// visit each of the adjacent verts to this cell and
			// add to any player at the vertex, some resource of
			// type cell.resource
			for (int vIndex: cell.getAdjVerts()) {
				Vertex vertex = mBoard.getVertex(vIndex);
				if (vertex.getPlayer() > 0) {
					Player p = getPlayerByPlayerNum(vertex.getPlayer()); 
					int num = vertex.isCity() ? mRules.getNumResourcesForCity() : mRules.getNumResourcesForSettlement();
					if (cell.getType() == TileType.GOLD) {
						// set to original player
						pushStateFront(State.SET_PLAYER, getCurPlayerNum(), null);
						for (int ii=0; ii<num; ii++) {
							if (mRules.isEnableCitiesAndKnightsExpansion()) {
								pushStateFront(State.DRAW_RESOURCE_OR_COMMODITY_NOCANCEL);
							} else {
								pushStateFront(State.DRAW_RESOURCE_NOCANCEL);
							}
						}
						// set to player that needs choose a resource
						pushStateFront(State.SET_PLAYER, vertex.getPlayer(), null);
					} else if (mRules.isEnableCitiesAndKnightsExpansion()) {
						
						if (cell.getCommodity() != null) {
							commodityInfo[cell.getCommodity().ordinal()][vertex.getPlayer()] += 1;
							resourceInfo[cell.getResource().ordinal()][vertex.getPlayer()] += 1;
							p.incrementResource(cell.getResource(), 1);
							p.incrementResource(cell.getCommodity(), 1);
						} else {
							resourceInfo[cell.getResource().ordinal()][vertex.getPlayer()] += 2;
							p.incrementResource(cell.getResource(), 2);
						}
						
					} else {
						resourceInfo[cell.getResource().ordinal()][vertex.getPlayer()] += num;
						p.incrementResource(cell.getResource(), num);
					}
				}
			}
		}

		for (Player p : getPlayers()) {
	        String msg = "";
	        for (ResourceType r : ResourceType.values()) {
			    int amount = resourceInfo[r.ordinal()][p.getPlayerNum()];
				if (amount > 0) {
					msg += (msg.length() == 0 ? "" : ", ") + amount + " X " + r;
					this.onDistributeResources(p, r, amount);
				}
			}
	        
	        for (CommodityType c : CommodityType.values()) {
	        	int amount = commodityInfo[c.ordinal()][p.getPlayerNum()];
	        	if (amount > 0) {
	        		msg += (msg.length() == 0 ? "" : ", ") + amount + " X " + c;
	        		this.onDistributeCommodity(p, c, amount);
	        	}
	        }

			if (msg.length() > 0) {
				printinfo("Player " + p.getName() + " gets " + msg);
			}
		}
	}

	/**
	 * Print a game info.  Base version writes to stdout.  
	 * All messages originate from @see runRame 
	 * @param playerNum
	 * @param txt
	 */
    public void printinfo(int playerNum, String txt) {
        System.out.println("Player " + playerNum + ": " + txt);
    }
    
	private void printinfo(String txt) {
		printinfo(getCurPlayerNum(), txt);
	}

	private void computePointsForPlayer(Player p) {
	    int newPoints = computePointsForPlayer(p, mBoard, this);
	    if (newPoints != p.getPoints()) {
	        this.onPlayerPointsChanged(p, newPoints-p.getPoints());
            p.setPoints(newPoints);
	    }
	}
	
	/**
	 * Compute the point the player should have based on the board and relevant SOC values.
	 * The player's point field is not changed. 
	 * @param player
	 * @return
	 */
	static public int computePointsForPlayer(Player player, Board board, SOC soc) {
	    int longestRoadPlayer = soc.getLongestRoadPlayerNum();
	    int largestArmyPlayer = soc.getLargestArmyPlayerNum();
		int numPts = 0;
		// count cities and settlements
		boolean [] islands = new boolean[board.getNumIslands()+1];
		for (int i = 0; i < board.getNumVerts(); i++) {
			Vertex vertex = board.getVertex(i);
			if (vertex.getPlayer() == player.getPlayerNum()) {
				numPts += vertex.getPointsValue(soc.getRules());
				for (Tile t : board.getTilesAdjacentToVertex(vertex)) {
					if (t.getIslandNum() > 0) {
						islands[t.getIslandNum()] = true;
					}
				}
			}
		}

		if (player.getPlayerNum() == longestRoadPlayer)
			numPts += soc.mRules.getPointsLongestRoad();

		if (player.getPlayerNum() == largestArmyPlayer)
			numPts += soc.mRules.getPointsLargestArmy();

		for (boolean b: islands) {
			if (b) {
				numPts += soc.getRules().getPointsIslandDiscovery();
			}
		}

		if (soc.getRules().isEnableCitiesAndKnightsExpansion()) {
			numPts += player.getCardCount(CardType.SpecialVictory);
		} else {
    		int victoryPts = player.getUsableCardCount(DevelopmentCardType.Victory);
    
    		if (numPts + victoryPts >= soc.getRules().getPointsForWinGame()) {
    			numPts += victoryPts;
    		}
		}
		
		return numPts;
	}
	
	/**
	 * Called when a players point change (for better or worse).  default method does nothing.
	 * @param player
	 * @param changeAmount
	 */
	protected void onPlayerPointsChanged(Player player, int changeAmount) {}
	
	private void updatePlayerPoints() {
		for (int i=0; i<mNumPlayers; i++) {
			computePointsForPlayer(mPlayers[i]);
		}
	}

	protected void onDiceRolled(int ... dice) {}
	
	/**
	 * Called when a player picks a development card from the deck.
	 * default method does nothing.
	 * @param player
	 * @param card
	 */
	protected void onCardPicked(Player player, Card card) {}
	private void pickDevelopmentCardFromDeck() {
		// add up the total chance
		if (mDevelopmentCards.size() <= 0) {
			initDeck();
		}
		Card picked = mDevelopmentCards.remove(0);
		picked.setUsable(false);
		getCurPlayer().addCard(picked);
		printinfo("Player " + getCurPlayer().getName() + " picked a " + picked + " card");
		this.onCardPicked(getCurPlayer(), picked);
	}

	/**
	 * Called when a player takes a card from another due to soldier.  default method does nothing.
	 * @param taker
	 * @param giver
	 * @param card
	 */
	protected void onTakeOpponentCard(Player taker, Player giver, Card card) {}
	
	private void takeOpponentCard(Player taker, Player giver) {
		assert (giver != taker);
		Card taken = giver.removeRandomUnusedCard();
		taker.addCard(taken);
		printinfo("Player " + taker.getName() + " taking a " + taken.getName() + " card from Player " + giver.getName());
		onTakeOpponentCard(taker, giver, taken);
	}
    
    public void initGame() {
        // setup
        assert (mNumPlayers > 1);
        assert (mPlayers != null);
        assert (mBoard != null);
        
        if (mRules.isEnableCitiesAndKnightsExpansion()) {
        	mDice = new int[3];
        	mBarbarianDistance = mRules.getBarbarianStepsToAttack();
        } else {
        	mDice = new int[2];
        }

        mLongestRoadPlayer = -1;
        mLargestArmyPlayer = -1;
        mStateStack.clear();
        mBoard.reset();
        mCurrentPlayer = 0;
        for (int i=0; i<getNumPlayers(); i++) {
            mPlayers[i].reset();
        }
        resetOptions();
        initDeck();

        pushStateFront(State.START_ROUND);
        pushStateFront(State.DEAL_CARDS);
        
        // first player picks last
        pushStateFront(State.POSITION_ROAD_OR_SHIP_NOCANCEL);
        if (mRules.isEnableCitiesAndKnightsExpansion())
        	pushStateFront(State.POSITION_CITY_NOCANCEL);
        else
        	pushStateFront(State.POSITION_SETTLEMENT_NOCANCEL);

        // player picks in reverse order
        for (int i=mNumPlayers-1; i > 0; i--) {
            pushStateFront(State.PREV_PLAYER);
            pushStateFront(State.POSITION_ROAD_OR_SHIP_NOCANCEL);
            if (mRules.isEnableCitiesAndKnightsExpansion())
            	pushStateFront(State.POSITION_CITY_NOCANCEL);
            else
            	pushStateFront(State.POSITION_SETTLEMENT_NOCANCEL);
        }

        // the last player picks again
        pushStateFront(State.POSITION_ROAD_OR_SHIP_NOCANCEL);
        if (mRules.isEnableCitiesAndKnightsExpansion())
        	pushStateFront(State.POSITION_CITY_NOCANCEL);
        else
        	pushStateFront(State.POSITION_SETTLEMENT_NOCANCEL);

        // players pick in order
        for (int i=0; i<mNumPlayers-1; i++) {
            pushStateFront(State.NEXT_PLAYER);
            pushStateFront(State.POSITION_ROAD_OR_SHIP_NOCANCEL);
            pushStateFront(State.POSITION_SETTLEMENT_NOCANCEL);
        }
    }

    /**
     * 
     * @return
     */
    public boolean isGameOver() {
        for (int i=1; i<=getNumPlayers(); i++) {
            Player player = getPlayerByPlayerNum(i);
            int pts = player.getPoints();
            assert(pts == player.getPoints());
            if (player.getPoints() >= mRules.getPointsForWinGame()) {
                onGameOver(player);
                return true;
            }
        }
        return false;
    }
    
    public static List<Card> createCards(ICardType [] values, CardStatus status) {
    	List<Card> cards = new ArrayList<Card>();
    	for (ICardType c : values) {
    		cards.add(new Card(c, status));
    	}
    	return cards;
    }
    
    public static List<Player> computeHarborTradePlayers(Player trader, SOC soc) {
    	List<Player> players = new ArrayList<Player>();
    	int num = trader.getCardCount(CardType.Resource);
    	for (Player p : soc.getPlayers()) {
    		if (num == 0)
    			break;
    		if (p == trader)
    			continue;
    		int numCommodity = p.getCardCount(CardType.Commodity);
    		if (numCommodity > 0) {
    			players.add(p);
    			num -= 1;
    		}
    	}
    	return players;
    }
    
    /**
     * Find all knights that are NOT owned by playerNum but ARE on a route of playerNum
     * 
     * @param playerNum
     * @param b
     * @return
     */
    public static List<Integer> computeIntrigueKnightsVertexIndices(int playerNum, Board b) {
    	List<Integer> verts = new ArrayList<Integer>();
		for (int vIndex=0; vIndex<b.getNumVerts(); vIndex++) {
			Vertex v = b.getVertex(vIndex);
			if (v.isKnight() && v.getPlayer() != playerNum && b.isVertexAdjacentToPlayerRoute(vIndex, playerNum)) {
				verts.add(vIndex);
			}
		}    	
		return verts;
    }
    
    public static List<Integer> computeInventorTileIndices(Board b) {
    	int [] values = { 3,4,5,9,10,11 };
		List<Integer> tiles = new ArrayList<Integer>();
		for (int tIndex=0; tIndex<b.getNumTiles(); tIndex++) {
			Tile t = b.getTile(tIndex);
			if (Arrays.binarySearch(values, t.getDieNum()) >= 0) {
				tiles.add(tIndex);
			}
		}    	
		return tiles;
    }
    
    /**
     * Return true when it is valid to call run()
     * @return
     */
    public boolean canRun() {
        return this.mStateStack.size() > 0;
    }
    
    private boolean runGameCheck() {
		if (mBoard == null) {
			throw new RuntimeException("No board, cannot run game");
		}

		if (!mBoard.isFinalized()) {
			throw new RuntimeException("Board not initialized, cannot run game");
		}

		if (mNumPlayers < 2) {
			throw new RuntimeException("Not enought players, cannot run game");
		}
		
		int i;
		
		// test that the players are numbered correctly
		for (i=1; i<=mNumPlayers; i++) {
			if (getPlayerByPlayerNum(i) == null)
				throw new RuntimeException("Cannot find player '" + i + "' of '" + mNumPlayers + "' cannot run game");
		}
		
		if (mStateStack.isEmpty())
			initGame();
		
		assert(!mStateStack.isEmpty());
		if (isDebugEnabled()) {
			List<StackItem> stack = new LinkedList<StackItem>(mStateStack);
			Collections.reverse(stack);			
			logDebug("Stack Before: " + stack);
		}
		
    	updatePlayerPoints();
		if (isGameOver()) {
		    return false;
		}
		
		return true;
    }
    
    /**
     * A game processing step.  Typically this method is called from a unique thread.  
     *   
     * 
     * @return true if run is valid, false otherwise
     */
	@SuppressWarnings("unchecked")
    public void runGame() {
		
		Vertex knightVertex = null;
		
		if (!runGameCheck())
			return;
		
		try {

			switch (getState()) {

				case DEAL_CARDS: // transition state
					//dealCards();
					distributeResources(0);
					popState();
					break;

				case POSITION_SETTLEMENT_CANCEL: // wait state
				case POSITION_SETTLEMENT_NOCANCEL: { // wait state
					if (mOptions == null) {
						printinfo("Player " + getCurPlayer().getName() + " place settlement");
						mOptions = computeSettlementVertexIndices(this, getCurPlayerNum(), mBoard);
					}

					assert (mOptions != null);
					Vertex v = getCurPlayer().chooseVertex(this, mOptions, VertexChoice.SETTLEMENT);

					if (v != null) {
						int vIndex = mBoard.getVertexIndex(v);
						printinfo("Player " + getCurPlayer().getName() + " placed a settlement on vertex " + vIndex);
						assert (v.getPlayer() == 0);
						v.setPlayer(getCurPlayerNum());
						v.setType(VertexType.SETTLEMENT);
						updatePlayerRoadsBlocked(getBoard(), vIndex);

						// need to re-eval the road lengths for all players that the new settlement
						// may have affected.  Get the players to update first to avoid dups.
						boolean [] playerNumsToCompute = new boolean[getNumPlayers()+1];
						for (int ii=0; ii<v.getNumAdjacent(); ii++) {
							int eIndex = mBoard.getRouteIndex(vIndex, v.getAdjacent()[ii]);
							if (eIndex >= 0) {
								Route e = mBoard.getRoute(eIndex);
								if (e.getPlayer() > 0)
									playerNumsToCompute[e.getPlayer()] = true;
							}
						}

						for (int ii=1; ii<playerNumsToCompute.length; ii++) {
							if (playerNumsToCompute[ii]) {
								Player p = getPlayerByPlayerNum(ii);
								int len = mBoard.computeMaxRouteLengthForPlayer(ii, mRules.isEnableRoadBlock());
								p.setRoadLength(len);
							}
						}

						updateLongestRoutePlayer();
						checkForDiscoveredIsland(vIndex);
						resetOptions();
						popState();
					}
					break;
				}

				case POSITION_ROAD_OR_SHIP_CANCEL:
				case POSITION_ROAD_OR_SHIP_NOCANCEL: {
					
					if (mRules.isEnableSeafarersExpansion()) {
					
    					// this state reserved for choosing between roads or ships to place
    					List<Integer> shipOptions = computeShipRouteIndices(getCurPlayerNum(), mBoard);
    					List<Integer> roadOptions = computeRoadRouteIndices(getCurPlayerNum(), mBoard);
    					if (shipOptions.size() > 0 && roadOptions.size() > 0) {
    						RouteChoiceType type = getCurPlayer().chooseRouteType(this);
    						final State saveState = getState();
    						popState();
    						if (type != null) {
    							// allow player to back out to this menu to switch their choice if they want
    							switch (type) {
									case ROAD_CHOICE:
										mOptions = roadOptions;
										pushStateFront(State.POSITION_ROAD_CANCEL, null , new UndoAction() {
											@Override
											public void undo() {
												pushStateFront(saveState);
											}
										});
										break;
									case SHIP_CHOICE:
										mOptions = shipOptions;
										pushStateFront(State.POSITION_SHIP_CANCEL, null, new UndoAction() {
											
											@Override
											public void undo() {
												pushStateFront(saveState);
											}
										});
										break;
    							}
    						}
    					} else if (shipOptions.size() > 0) {
    						popState();
    						if (canCancel()) {
    							pushStateFront(State.POSITION_SHIP_CANCEL);
    						} else {
    							pushStateFront(State.POSITION_SHIP_NOCANCEL);
    						}
    					} else if (roadOptions.size() > 0) {
    						popState();
    						if (canCancel()) {
    							pushStateFront(State.POSITION_ROAD_CANCEL);
    						} else {
    							pushStateFront(State.POSITION_ROAD_NOCANCEL);
    						}
    					} else {
    						popState(); // no road or ship choices?  hmmmmmmmm
    					}
					} else {
						popState();
						pushStateFront(State.POSITION_ROAD_NOCANCEL);
					}
					break;
				}
				
				/*
				case POSITION_ROAD_OR_SHIP_NOCANCEL:
				case POSITION_ROAD_OR_SHIP_CANCEL:
					if (getRules().isEnableSeafarersExpansion()) {
						if (mOptions == null) {
							mOptions = computeRoadOptions(getCurPlayerNum(), mBoard);
						}
						//List<Integer> shipOptions = computeShipOptions(getCurPlayerNum(), mBoard);
						Route edge = getCurPlayer().chooseRoute(this, mOptions, shipOptions, RouteChoice.ROAD_OR_SHIP);
						if (edge != null) {
							assert(edge.getPlayer() == 0);
							int eIndex = mBoard.getRouteIndex(edge);
							getBoard().setPlayerForRoute(edge, getCurPlayerNum());
							if (edge.isShip()) {
								printinfo("Player " + getCurPlayer().getName() + " placing a ship on edge " + eIndex);
							} else {
								printinfo("Player " + getCurPlayer().getName() + " placing a road on edge " + eIndex);
							}
							Player p = getCurPlayer();
							int len = mBoard.computeMaxRouteLengthForPlayer(p.getPlayerNum(), mRules.isEnableRoadBlock());
							p.setRoadLength(len);
							updateLongestRoutePlayer();
							checkForDiscoveredNewTerritory(edge.getFrom());
							checkForDiscoveredNewTerritory(edge.getTo());
							resetOptions();
							popState();
						}
						break;
					} // else fallthrough to roads
*/
				case POSITION_ROAD_NOCANCEL: // wait state
				case POSITION_ROAD_CANCEL: {// wait state

					if (mOptions == null) {
						printinfo("Player " + getCurPlayer().getName() + " place road");
						mOptions = computeRoadRouteIndices(getCurPlayerNum(), mBoard);
					}

					assert (mOptions != null && mOptions.size() > 0);

					Route edge = getCurPlayer().chooseRoute(this, mOptions, RouteChoice.ROAD);

					if (edge != null) {
						assert(!edge.isShip());
						assert(edge.getPlayer()==0);
						assert(edge.isAdjacentToLand());
						//assert (edge.player <= 0);
						int eIndex = mBoard.getRouteIndex(edge);
						printinfo("Player " + getCurPlayer().getName() + " placing a road on edge " + eIndex);
						//edge.player = getCurPlayerNum();
						getBoard().setPlayerForRoute(edge, getCurPlayerNum());
						Player p = getCurPlayer();
						int len = mBoard.computeMaxRouteLengthForPlayer(p.getPlayerNum(), mRules.isEnableRoadBlock());
						p.setRoadLength(len);
						updateLongestRoutePlayer();
						checkForDiscoveredNewTerritory(edge.getFrom());
						checkForDiscoveredNewTerritory(edge.getTo());
						resetOptions();
						popState();
					}
					break;
				}

				case POSITION_SHIP_NOCANCEL:
				case POSITION_SHIP_CANCEL: {
					if (mOptions == null) {
						printinfo("Player " + getCurPlayer().getName() + " place ship");
						mOptions = computeShipRouteIndices(getCurPlayerNum(), mBoard);
					}

					assert(mOptions != null);
					Route edge = getCurPlayer().chooseRoute(this, mOptions, RouteChoice.SHIP);
					if (edge != null) {
						assert(edge.getPlayer()==0);
						assert(edge.isAdjacentToWater());
						edge.setShip(true);
						edge.setLocked(true);
						int eIndex = mBoard.getRouteIndex(edge);
						printinfo("Player " + getCurPlayer().getName() + " placing a ship on edge " + eIndex);
						getBoard().setPlayerForRoute(edge, getCurPlayerNum());
						Player p = getCurPlayer();
						int len = mBoard.computeMaxRouteLengthForPlayer(p.getPlayerNum(), mRules.isEnableRoadBlock());
						p.setRoadLength(len);
						updateLongestRoutePlayer();
						checkForDiscoveredNewTerritory(edge.getFrom());
						checkForDiscoveredNewTerritory(edge.getTo());
						resetOptions();
						popState();
					}
					break;
				}

				case CHOOSE_SHIP_TO_MOVE:
					if (mOptions == null) {
						printinfo("Player " + getCurPlayer().getName() + " choose ship to move");
						mOptions = computeOpenRouteIndices(getCurPlayerNum(), mBoard, false, true);
					}

					assert(mOptions != null);
					final Route ship = getCurPlayer().chooseRoute(this, mOptions, RouteChoice.SHIP_TO_MOVE);
					if (ship != null) {
						assert(ship.isShip());
						assert(ship.getPlayer() == getCurPlayerNum());
						mBoard.setPlayerForRoute(ship, 0);
						ship.setShip(false);
						popState();
						for (Route toLock : mBoard.getShipRoutesForPlayer(getCurPlayerNum())) {
							toLock.setLocked(true);
						}
						pushStateFront(State.POSITION_SHIP_CANCEL, null, new UndoAction() {
							@Override
							public void undo() {
								mBoard.setPlayerForRoute(ship, getCurPlayerNum());
								ship.setShip(true);
								for (Route toLock : mBoard.getShipRoutesForPlayer(getCurPlayerNum()))
									toLock.setLocked(false);
							}
						});
						resetOptions();
					}
					break;

				case POSITION_CITY_NOCANCEL: 
					if (mOptions == null) {
						printinfo("Player " + getCurPlayer().getName() + " place city");
						mOptions = computeSettlementVertexIndices(this, getCurPlayerNum(), mBoard);
					}
				case POSITION_CITY_CANCEL: { // wait state
					if (mOptions == null) {
						printinfo("Player " + getCurPlayer().getName() + " place city");
						mOptions = computeCityVertxIndices(getCurPlayerNum(), mBoard);
					}

					assert (mOptions != null);
					Vertex v = getCurPlayer().chooseVertex(this, mOptions, VertexChoice.CITY);

					if (v != null) {
						assert (v.getPlayer() == getCurPlayerNum());
						int vIndex = mBoard.getVertexIndex(v);
						printinfo("Player getCurPlayer().getName() placing a city at vertex " + vIndex);
						assert (v.isCity() == false);
						v.setType(VertexType.CITY);
						computePointsForPlayer(getCurPlayer());
						resetOptions();
						popState();
					}
					break;
				}
				
				case CHOOSE_CITY_FOR_WALL: {
					if (mOptions == null) {
						printinfo("Player " + getCurPlayer().getName() + " choose city to protect with wall");
						mOptions = computeCityWallVertexIndices(getCurPlayerNum(), mBoard);
					}

					assert (mOptions != null);
					Vertex v = getCurPlayer().chooseVertex(this, mOptions, VertexChoice.CITY_WALL);

					if (v != null) {
						assert (v.getPlayer() == getCurPlayerNum());
						int vIndex = mBoard.getVertexIndex(v);
						printinfo("Player " + getCurPlayer().getName() + " placing a city at vertex " + vIndex);
						assert (v.isCity() == false);
						v.setType(VertexType.WALLED_CITY);
						resetOptions();
						popState();
					}
					break;

				}

				case CHOOSE_METROPOLIS: {
					if (mOptions == null) {
						printinfo("Player " + getCurPlayer().getName() + " choose city to upgrade to Metropolis");
						mOptions = computeMetropolisVertexIndices(getCurPlayerNum(), mBoard);
					}
					
					assert(mOptions != null && mOptions.size() > 0);
					DevelopmentArea area = (DevelopmentArea)getStateData();
					assert(area != null);
					Vertex v = getCurPlayer().chooseVertex(this, mOptions, area.choice);
					if (v != null) {
						assert(v.getPlayer() == getCurPlayerNum());
						assert(v.isCity());
						printinfo("Player " + getCurPlayer().getName() + " is building a " + area + " Metrololis");
						v.setType(area.vertexType);
						resetOptions();
						popState();
					}
					break;
				}

				case NEXT_PLAYER: // transition state
					incrementCurPlayer(1);
					popState();
					break;

				case PREV_PLAYER: // transition state
					incrementCurPlayer(-1);
					popState();
					break;

				case START_ROUND: // transition state
					assert(mStateStack.size() == 1); // this should always be the start
					Arrays.fill(mDice, 0);
					if (getCurPlayer().getCardCount(ProgressCardType.Alchemist) > 0) {
						mOptions = Utils.asList(MoveType.ALCHEMIST_CARD, MoveType.ROLL_DICE);
					} else {
						mOptions = Utils.asList(MoveType.ROLL_DICE);
					}

					pushStateFront(State.PLAYER_TURN_NOCANCEL);
					break;

				case INIT_PLAYER_TURN: { // transition state
					// update any unusable cards in that players hand to be usable
					if (getCurPlayer().getMerchantFleetTradable() != null) {
						getCurPlayer().setMerchantFleetTradable(null);
						putCardBackInDeck(getCurPlayer().removeCard(ProgressCardType.MerchantFleet));
					}
					getCurPlayer().setCardsUsable(CardType.Development, true);
					// unlock the players routes
					for (Route r : mBoard.getRoutesForPlayer(getCurPlayerNum())) {
						r.setLocked(false);
					}
					for (int vIndex : mBoard.getVertsOfType(getCurPlayerNum(), VertexType.STRONG_KNIGHT_ACTIVE, VertexType.STRONG_KNIGHT_INACTIVE)) {
						mBoard.getVertex(vIndex).setPromotedKnight(false);
					}
					popState();
					pushStateFront(State.PLAYER_TURN_NOCANCEL);
					break;
				}

				case PLAYER_TURN_CANCEL:
				case PLAYER_TURN_NOCANCEL: // wait state
					if (mOptions == null) {
						printinfo("Player " + getCurPlayer().getName() + " choose move");
						mOptions = computeMoves(getCurPlayer(), mBoard, this);
					}
					assert (mOptions != null);
					assert (mOptions.size() > 0);
					MoveType move = getCurPlayer().chooseMove(this, mOptions);
					if (move != null) {
						processMove(move);
						resetOptions();
					}
					break;

				case SHOW_TRADE_OPTIONS: { // wait state
					if (mOptions == null) {
						printinfo("Player " + getCurPlayer().getName() + " select trade option");
						mOptions = computeTrades(getCurPlayer(), mBoard);
					}
					assert (mOptions != null);
					final Trade trade = getCurPlayer().chooseTradeOption(this, mOptions);
					if (trade != null) {
						printinfo("Player " + getCurPlayer().getName() + " trades " + trade.getType() + " X " + trade.getAmount());
						getCurPlayer().incrementResource(trade.getType(), -trade.getAmount());
						popState();
						//this.mSaveTrade = trade;
						//pushStateFront(State.TRADE_COMPLETED, trade, null);
						UndoAction action = new UndoAction() {
							public void undo() {
								getCurPlayer().incrementResource(trade.getType(), trade.getAmount());
								//popState();
							}
						};
						if (mRules.isEnableCitiesAndKnightsExpansion()) {
							pushStateFront(State.DRAW_RESOURCE_OR_COMMODITY_CANCEL, null, action);
						} else {
							pushStateFront(State.DRAW_RESOURCE_CANCEL, null, action);
						}
						resetOptions();
					}
					break;
				}

				case POSITION_ROBBER_OR_PIRATE_CANCEL:
				case POSITION_ROBBER_OR_PIRATE_NOCANCEL:
					if (mOptions == null) {
						printinfo("Player " + getCurPlayer().getName() + " place robber or pirate");
						mOptions = computeRobberTileIndices(this, mBoard);
						mOptions.addAll(computePirateTileIndices(this, mBoard));
					}
				case POSITION_ROBBER_CANCEL: // wait state
				case POSITION_ROBBER_NOCANCEL: // wait state
					if (mOptions == null) {
						printinfo("Player " + getCurPlayer().getName() + " place robber");
						mOptions = computeRobberTileIndices(this, mBoard);
					}
					Tile cell = getCurPlayer().chooseTile(this, mOptions, TileChoice.ROBBER);

					if (cell != null) {
						popState();
						int cellIndex = mBoard.getTileIndex(cell);
						if (cell.isWater()) {
							printinfo("Player " + getCurPlayer().getName() + " placing pirate on cell " + cellIndex);
							mBoard.setPirate(cellIndex);
							pushStateFront(State.TAKE_OPPONENT_CARD, 1, null);
						} else {
							printinfo("Player " + getCurPlayer().getName() + " placing robber on cell " + cellIndex);
							mBoard.setRobber(cellIndex);
							pushStateFront(State.TAKE_OPPONENT_CARD, 0, null);
						}
						resetOptions();
					}
					break;

				case TAKE_OPPONENT_CARD: // wait state
					if (mOptions == null) {
						int data = (Integer)getStateData();
						mOptions = computeTakeOpponentCardPlayers(getAllPlayers(), getCurPlayer(), mBoard, data == 0 ? false : true);
					}
					assert (mOptions != null);
					if (mOptions.size() == 0) {
						popState();
						resetOptions();
					} else {
						Player player = getCurPlayer().choosePlayer(this, mOptions, PlayerChoice.PLAYER_TO_TAKE_CARD_FROM);
						if (player != null) {
							assert (player != getCurPlayer());
							assert (player.getPlayerNum() > 0);
							takeOpponentCard(getCurPlayer(), player);
							popState();
							resetOptions();
						}
					}
					break;

				case CHOOSE_RESOURCE_MONOPOLY: { // wait state
					ResourceType type = getCurPlayer().chooseEnum(this, EnumChoice.MONOPOLY, ResourceType.values());
					if (type != null) {
						processMonopoly(type);
						popState();
						resetOptions();
					}
					break;
				}

				case GIVE_UP_CARD: { // wait state
					if (mOptions == null) {
						mOptions = computeGiveUpCards(getCurPlayer());
					}
					assert (mOptions != null);
					Card card = getCurPlayer().chooseCard(this, mOptions, CardChoice.GIVEUP_CARD);
					if (card != null) {
						getCurPlayer().removeCard(card);
						resetOptions();
						popState();
					}
					break;
				}

				case DRAW_RESOURCE_OR_COMMODITY_NOCANCEL:
				case DRAW_RESOURCE_OR_COMMODITY_CANCEL:
					if (mOptions == null) {
						mOptions = new ArrayList<Card>();
						for (ResourceType t : ResourceType.values()) {
							mOptions.add(new Card(t, CardStatus.USABLE));
						}
						for (CommodityType c : CommodityType.values()) {
							mOptions.add(new Card(c, CardStatus.USABLE));
						}
					}
				case DRAW_RESOURCE_NOCANCEL:
				case DRAW_RESOURCE_CANCEL: { // wait state
					if (mOptions == null) {
						mOptions = new ArrayList<Card>();
						for (ResourceType t : ResourceType.values()) {
							mOptions.add(new Card(t, CardStatus.USABLE));
						}
					}
					//Card card = getCurPlayer().chooseCard(this, createCards(ResourceType.values(), CardStatus.USABLE), CardChoice.RESOURCE_CARD);
					//ResourceType type = getCurPlayer().chooseEnum(this, EnumChoice.DRAW_RESOURCE, ResourceType.values());
					Card card = getCurPlayer().chooseCard(this, mOptions, CardChoice.RESOURCE_OR_COMMODITY);
					if (card != null) {
						//Card card = new Card(type, CardStatus.USABLE);
						printinfo("Player " + getCurPlayer().getName() + " draws a " + card.getName() + " resource card");
						getCurPlayer().addCard(card);
						popState();
						resetOptions();
					}
					break;
				}

/*				case TRADE_COMPLETED:
					onTradeCompleted(getCurPlayer(), (Trade)getStateData());
					popState();
					break;
*/
				case SET_PLAYER:
					setCurrentPlayer((Integer)getStateData());
					popState();
					break;

				case CHOOSE_KNIGHT_TO_ACTIVATE: {
					if (mOptions == null) {
						mOptions = mBoard.getVertsOfType(getCurPlayerNum(), VertexType.BASIC_KNIGHT_INACTIVE, VertexType.STRONG_KNIGHT_INACTIVE, VertexType.MIGHTY_KNIGHT_INACTIVE);
					}
					Vertex v = getCurPlayer().chooseVertex(this, mOptions, VertexChoice.KNIGHT_TO_ACTIVATE);
					if (v != null) {
						switch (v.getType()) {
							case BASIC_KNIGHT_INACTIVE:
								v.setType(VertexType.BASIC_KNIGHT_ACTIVE);
								break;
							case MIGHTY_KNIGHT_INACTIVE:
								v.setType(VertexType.MIGHTY_KNIGHT_ACTIVE);
								break;
							case STRONG_KNIGHT_INACTIVE:
								v.setType(VertexType.STRONG_KNIGHT_ACTIVE);
								break;
							default:
								throw new RuntimeException("Vertex '" + v + "' is not an inactive knight");
							
						}
						resetOptions();
						popState();
					}
					break;
				}

				case CHOOSE_KNIGHT_TO_PROMOTE: {
					if (mOptions == null) {
						mOptions = computePromoteKnightVertexIndices(getCurPlayer(), mBoard);
					}
					Vertex v = getCurPlayer().chooseVertex(this, mOptions, VertexChoice.KNIGHT_TO_PROMOTE);
					if (v != null) {
						assert(v.isKnight());
						assert(!v.isPromotedKnight());
						switch (v.getType()) {
							case BASIC_KNIGHT_INACTIVE:
								v.setType(VertexType.STRONG_KNIGHT_INACTIVE);
								break;
							case BASIC_KNIGHT_ACTIVE:
								v.setType(VertexType.STRONG_KNIGHT_ACTIVE);
								break;
							case STRONG_KNIGHT_INACTIVE:
								v.setType(VertexType.MIGHTY_KNIGHT_INACTIVE);
								break;
							case STRONG_KNIGHT_ACTIVE:
								v.setType(VertexType.MIGHTY_KNIGHT_ACTIVE);
								break;
							default:
								throw new RuntimeException("Vertex '" + v + "' is not an inactive knight");
							
						}
						v.setPromotedKnight(true);
						resetOptions();
						popState();
					}
					break;
				}
				
				case CHOOSE_KNIGHT_TO_MOVE: {
					if (mOptions == null) {
						mOptions = computeMovableKnightVertexIndices(getCurPlayerNum(), mBoard);
					}
					assert(mOptions.size() > 0);
					final Vertex v = getCurPlayer().chooseVertex(this, mOptions, VertexChoice.KNIGHT_TO_MOVE);
					if (v != null) {
						v.setPlayer(0);
						v.deactivateKnight();
						pushStateFront(State.POSITION_KNIGHT_CANCEL, v.getType(), new UndoAction() {
							@Override
							public void undo() {
								v.setPlayer(getCurPlayerNum());
								v.activateKnight();
							}
						});
						mOptions = computeKnightMoveVertexIndices(mBoard.getVertexIndex(v), mBoard);
					}
					break;
				}
				
				case POSITION_DISPLACED_KNIGHT: 
					knightVertex = (Vertex)getStateData();
					// fallthrought
				case POSITION_KNIGHT_CANCEL: {
					assert(mOptions != null && mOptions.size() > 0);
					VertexType curKnight = null;
					if (knightVertex == null) {
						curKnight = (VertexType)getStateData();
						knightVertex = getCurPlayer().chooseVertex(this, mOptions, VertexChoice.KNIGHT_MOVE_POSITION);
					} else {
						curKnight = knightVertex.getType();
					}
					Vertex v = knightVertex;
					if (v != null) {
						popState();

						// see if we displaced another player
						if (v.getPlayer() != 0) {
							assert(curKnight.getKnightLevel() > v.getType().getKnightLevel());
							if (computeKnightMoveVertexIndices(mBoard.getVertexIndex(v), mBoard).size() > 0) {
								printinfo("Player " + getCurPlayer().getName() + " has displaced " + getPlayerByPlayerNum(v.getPlayer()).getName() + "'s Knight");
								pushStateFront(State.SET_PLAYER, getCurPlayerNum(), null);
								pushStateFront(State.POSITION_DISPLACED_KNIGHT, v, null);
								pushStateFront(State.SET_PLAYER, v.getPlayer(), null);
							}
						}

						// see if we are chasing away the robber
						for (int i=0; i<v.getNumTiles(); i++) {
							int tIndex = v.getTile(i);
							if (tIndex == mBoard.getRobberTile()) {
								printinfo("Player " + getCurPlayer().getName() + " has chased away the robber!");
								pushStateFront(State.POSITION_ROBBER_NOCANCEL);
								break;
							}
						}
						
						resetOptions();
					}
					break;
				}
				
				case CHOOSE_PROGRESS_CARD_TYPE: {
					DevelopmentArea area = getCurPlayer().chooseEnum(this, EnumChoice.DRAW_PROGRESS_CARD, DevelopmentArea.values());
					if (area != null) {
						Card dealt = mProgressCards[area.ordinal()].remove(0);
						getCurPlayer().addCard(dealt);
						popState();
					}
					break;
				}

				// Crane Card
				case CHOOSE_CITY_IMPROVEMENT: {
					assert(mOptions != null && mOptions.size() > 0);
					DevelopmentArea area = getCurPlayer().chooseEnum(this, EnumChoice.IMPROVE_DEVELOPMENT_AREA, (DevelopmentArea[])mOptions.toArray(new DevelopmentArea[mOptions.size()]));
					if (area != null) {
						int improvement = getCurPlayer().getCityDevelopment(area);
						getCurPlayer().incrementResource(area.commodity, improvement);
						getCurPlayer().setCityDevelopment(area, improvement+1);
						popState();
						resetOptions();
					}
					break;
				}
				
				case CHOOSE_KNIGHT_TO_DESERT: {
					assert(mOptions != null && mOptions.size() > 0);
					Vertex v = getCurPlayer().chooseVertex(this, mOptions, VertexChoice.KNIGHT_DESERTER);
					if (v != null) {
						int newPlayerNum = (Integer)getStateData();
						v.setPlayer(newPlayerNum);
						popState();
						resetOptions();
					}
					break;
				}
					
				case CHOOSE_PLAYER_FOR_DESERTION: {
					assert(mOptions != null && mOptions.size() > 0);
					Player p = getCurPlayer().choosePlayer(this, mOptions, PlayerChoice.PLAYER_FOR_DESERTION);
					if (p != null) {
						popState();
						List<Integer> knights = (List<Integer>)getStateData();
						if (knights.size() == 1) {
							mBoard.getVertex(knights.get(0)).setPlayer(getCurPlayerNum());
						} else if (knights.size() > 1) {
							mOptions = knights;
							pushStateFront(State.SET_PLAYER, getCurPlayerNum(), null);
							pushStateFront(State.CHOOSE_KNIGHT_TO_DESERT, getCurPlayerNum(), null);
							pushStateFront(State.SET_PLAYER, p.getPlayerNum(), null);
						}
					}
					break;
				}
				
				case CHOOSE_DIPLOMAT_ROUTE: {
					assert(mOptions != null && mOptions.size() > 0);
					final Route r = getCurPlayer().chooseRoute(this, mOptions, RouteChoice.ROUTE_DIPLOMAT);
					if (r != null) {
						popState();
						if (r.getPlayer() == getCurPlayerNum()) {
							mBoard.setPlayerForRoute(r, 0);
							mOptions = computeRoadRouteIndices(getCurPlayerNum(), mBoard);
							mOptions.remove(mBoard.getRouteIndex(r));
							final Card card = getCurPlayer().removeCard(ProgressCardType.Diplomat);
							putCardBackInDeck(card);
							pushStateFront(State.POSITION_ROAD_CANCEL, null, new UndoAction() {
								@Override
								public void undo() {
									mBoard.setPlayerForRoute(r, getCurPlayerNum());
									mProgressCards[ProgressCardType.Diplomat.type.ordinal()].remove(card);
									getCurPlayer().addCard(card);
								}
							});
						}
					}
					break;
				}
				
				case CHOOSE_HARBOR_RESOURCE: {
					Card card = getCurPlayer().chooseCard(this, getCurPlayer().getCards(CardType.Resource), CardChoice.EXCHANGE_CARD);
					if (card != null) {
						Player exchanging = (Player)getStateData();
						exchanging.addCard(card);
						pushStateFront(State.SET_PLAYER, getCurPlayerNum(), null);
						mOptions = exchanging.getCards(CardType.Commodity);
						pushStateFront(State.EXCHANGE_CARD, getCurPlayer(), null);
						pushStateFront(State.SET_PLAYER, exchanging.getPlayerNum(), null);
						popState();
					}
					break;
				}
				
				case EXCHANGE_CARD: {
					Card card = getCurPlayer().chooseCard(this, mOptions, CardChoice.EXCHANGE_CARD);
					if (card != null) {
						Player exchanging = (Player)getStateData();
						exchanging.addCard(card);
						popState();
						resetOptions();
					}
					break;
				}
				
				case CHOOSE_OPPONENT_KNIGHT_TO_DISPLACE: {
					assert(mOptions != null && mOptions.size() > 0);
					Vertex v = getCurPlayer().chooseVertex(this, mOptions, VertexChoice.OPPONENT_KNIGHT_TO_DISPLACE);
					if (v != null) {
						int vIndex = mBoard.getVertexIndex(v);
						mOptions = computeDisplacedKnightVertexIndices(vIndex, mBoard);
						mOptions.remove(vIndex);
						putCardBackInDeck(getCurPlayer().removeCard(ProgressCardType.Intrigue));
						popState();
						pushStateFront(State.SET_PLAYER, getCurPlayerNum(), null);
						pushStateFront(State.POSITION_DISPLACED_KNIGHT, v, null);
						pushStateFront(State.SET_PLAYER, v.getPlayer(), null);
					}
					break;
				}
				
				case CHOOSE_TILE_INVENTOR: {
					assert(mOptions != null && mOptions.size() > 0);
					Tile tile = getCurPlayer().chooseTile(this, mOptions, TileChoice.INVENTOR);
					if (tile != null) {
						Tile firstTile = (Tile)getStateData();
						popState();
						if (firstTile == null) {
							mOptions.remove(mBoard.getTileIndex(tile));
							pushStateFront(State.CHOOSE_TILE_INVENTOR, firstTile, null);
						} else {
							// swap em
							int t = firstTile.getDieNum();
							firstTile.setDieNum(tile.getDieNum());
							tile.setDieNum(t);
							putCardBackInDeck(getCurPlayer().removeCard(ProgressCardType.Inventor));
						}
					}
					break;
				}
				
				case CHOOSE_PLAYER_MASTER_MERCHANT: {
					assert(mOptions != null && mOptions.size() > 0);
					Player p = null;
					if (mOptions.size() == 1) {
						p = getPlayerByPlayerNum((Integer)mOptions.get(0));
					} else {
						p = getCurPlayer().choosePlayer(this, mOptions, PlayerChoice.PLAYER_TO_TAKE_CARD_FROM);
					}
					if (p != null) {
						putCardBackInDeck(getCurPlayer().removeCard(ProgressCardType.MasterMerchant));
						List<Card> cards = p.getCards(CardType.Commodity);
						cards.addAll(p.getCards(CardType.Resource));
						int num = cards.size();
						if (num <= 2) {
							// just take em all
							for (Card c : cards) {
								p.removeCard(c);
								getCurPlayer().addCard(c);
								onTakeOpponentCard(getCurPlayer(), p, c);
							}
						} else {
        					if (num > 2)
        						num = 2;
        					popState();
        					resetOptions();
        					pushStateFront(State.TAKE_CARD_FROM_OPPONENT, p, null);
        					pushStateFront(State.TAKE_CARD_FROM_OPPONENT, p, null);
						}
					}
					break;
				}
					
				case TAKE_CARD_FROM_OPPONENT: {
					Player p = (Player)getStateData();
					List<Card> cards = p.getCards(CardType.Commodity);
					cards.addAll(p.getCards(CardType.Resource));
					Card c = getCurPlayer().chooseCard(this, cards, CardChoice.OPPONENT_CARD);
					if (c != null) {
						p.removeCard(c);
						getCurPlayer().addCard(c);
						onTakeOpponentCard(getCurPlayer(), p, c);
						popState();
					}
					break;
				}
				
				case POSITION_MERCHANT: {
					if (mOptions == null) {
						mOptions = computeMerchantTileIndices(this, getCurPlayerNum(), mBoard);
					}
					assert(mOptions.size() > 0);
					Tile t = getCurPlayer().chooseTile(this, mOptions, TileChoice.MERCHANT);
					if (t != null) {
						putCardBackInDeck(getCurPlayer().removeCard(ProgressCardType.Merchant));
						mBoard.setMerchant(mBoard.getTileIndex(t), getCurPlayerNum());
						popState();
					}
					break;
				}
				
				case CHOOSE_RESOURCE_FLEET: {
					assert(mOptions.size() > 0);
					Card c = getCurPlayer().chooseCard(this, mOptions, CardChoice.FLEET_TRADE);
					if (c != null) {
						getCurPlayer().getCard(ProgressCardType.MerchantFleet).setUsed(true);
						getCurPlayer().setMerchantFleetTradable(c);
						popState();
					}
					break;
				}
				
				case CHOOSE_PLAYER_TO_SPY_ON: {
					assert(mOptions.size() > 0);
					Player p = getCurPlayer().choosePlayer(this, mOptions, PlayerChoice.PLAYER_TO_TAKE_CARD_FROM);
					if (p != null) {
						putCardBackInDeck(getCurPlayer().removeCard(ProgressCardType.Spy));
						popState();
						List<Card> cards = p.getUnusedCards(CardType.Progress);
						if (cards.size() > 0) {
							pushStateFront(State.CHOOSE_OPPONENT_CARD, p, null);
							mOptions = cards;
						}
					}
					break;
				}
				
				case CHOOSE_OPPONENT_CARD: {
					Card c = getCurPlayer().chooseCard(this, mOptions, CardChoice.OPPONENT_CARD);
					if (c != null) {
						((Player)getStateData()).removeCard(c);
						getCurPlayer().addCard(c);
						popState();
					}
					break;
				}
				
				case CHOOSE_TRADE_MONOPOLY: {
					CommodityType c = getCurPlayer().chooseEnum(this, EnumChoice.MONOPOLY, CommodityType.values());
					if (c != null) {
						putCardBackInDeck(getCurPlayer().removeCard(ProgressCardType.TradeMonopoly));
						for (Player p : getPlayers()) {
							if (p != getCurPlayer()) {
								int num = p.getCardCount(c);
								if (num > 0) {
									p.removeCards(c, 1);
									getCurPlayer().incrementResource(c, 1);
									onTakeOpponentCard(getCurPlayer(), p, new Card(c, CardStatus.USABLE));
								}
							}
						}
					}
					break;
				}
				
				case CHOOSE_GIFT_CARD: {
					Card c = getCurPlayer().chooseCard(this, computeGiftCards(this, getCurPlayer()), CardChoice.GIVEUP_CARD);
					if (c != null) {
						Player taker = (Player)getStateData();
						getCurPlayer().removeCard(c);
						taker.addCard(c);
						onTakeOpponentCard(taker, getCurPlayer(), c);
						popState();
					}
					break;
				}
			}
    			
    		if (isDebugEnabled()) {
    			List<StackItem> stack = new LinkedList<StackItem>(mStateStack);
    			Collections.reverse(stack);
    			logDebug("Stack After: " + stack);
    		}
	    } finally {
	        //if (Profiler.ENABLED) Profiler.pop("SOC::runGame[" + state + "]");
            //if (Profiler.ENABLED) Profiler.pop("SOC::runGame");
	    }
	}

	/**
	 * Not recommended to use as this function modifies player and this data.
	 * call runGame until returns true to process
	 * @param move
	 */
	private final void processMove(MoveType move) {
	    
        printinfo("Player " + getCurPlayer().getName() + " choose move " + move);
        switch (move) {
        	case BUILD_ROAD:
        		getCurPlayer().adjustResourcesForBuildable(BuildableType.Road, -1);
        		pushStateFront(State.POSITION_ROAD_CANCEL, null, new UndoAction() {
        			public void undo() {
        				getCurPlayer().adjustResourcesForBuildable(BuildableType.Road, 1);
        			}
        		});
        		break;

        	case BUILD_SHIP:
        		getCurPlayer().adjustResourcesForBuildable(BuildableType.Ship, -1);
        		pushStateFront(State.POSITION_SHIP_CANCEL, null, new UndoAction() {
        			public void undo() {
        				getCurPlayer().adjustResourcesForBuildable(BuildableType.Ship, 1);
        			}
        		});
        		break;

        	case MOVE_SHIP:
        		pushStateFront(State.CHOOSE_SHIP_TO_MOVE);
        		break;

        	case BUILD_SETTLEMENT:
        		getCurPlayer().adjustResourcesForBuildable(BuildableType.Settlement, -1);
        		pushStateFront(State.POSITION_SETTLEMENT_CANCEL, null, new UndoAction() {
        			public void undo() {
        				getCurPlayer().adjustResourcesForBuildable(BuildableType.Settlement, 1);
        			}
        		});
        		break;

        	case BUILD_CITY:
        		getCurPlayer().adjustResourcesForBuildable(BuildableType.City, -1);
        		pushStateFront(State.POSITION_CITY_CANCEL, null, new UndoAction() {
        			public void undo() {
        				getCurPlayer().adjustResourcesForBuildable(BuildableType.City, 1);
        			}
        		});
        		break;

        	case DRAW_DEVELOPMENT:
        		getCurPlayer().adjustResourcesForBuildable(BuildableType.Development, -1);
        		pickDevelopmentCardFromDeck();
        		break;

        	case MONOPOLY_CARD: {
        		final Card removed = getCurPlayer().removeUsableCard(DevelopmentCardType.Monopoly);
        		putCardBackInDeck(removed);
        		pushStateFront(State.CHOOSE_RESOURCE_MONOPOLY, null, new UndoAction() {
        			public void undo() {
        				getCurPlayer().addCard(removed);
        				removeCardFromDeck(removed);
        			}
        		});
        		break;
        	}

        	case YEAR_OF_PLENTY_CARD: {
        		final Card removed = getCurPlayer().removeUsableCard(DevelopmentCardType.YearOfPlenty);
        		putCardBackInDeck(removed);
        		mDevelopmentCards.add(removed);
        		pushStateFront(State.DRAW_RESOURCE_NOCANCEL);
        		getCurPlayer().setCardsUsable(CardType.Development, false);
        		pushStateFront(State.DRAW_RESOURCE_CANCEL, null, new UndoAction() {
        			public void undo() {
        				getCurPlayer().addCard(removed);
        				removeCardFromDeck(removed);
        				getCurPlayer().setCardsUsable(CardType.Development, true);
        				popState();                    
        			}
        		});
        		break;
        	}

        	case ROAD_BUILDING_CARD: {
        		final Card removed = getCurPlayer().removeUsableCard(DevelopmentCardType.RoadBuilding);
        		putCardBackInDeck(removed);
        		pushStateFront(State.POSITION_ROAD_OR_SHIP_NOCANCEL);
        		getCurPlayer().setCardsUsable(CardType.Development, false);
        		pushStateFront(State.POSITION_ROAD_OR_SHIP_CANCEL, null, new UndoAction() {
        			public void undo() {
        				getCurPlayer().addCard(removed);
        				removeCardFromDeck(removed);
        				getCurPlayer().setCardsUsable(CardType.Development, true);
        				popState(); // pop an extra state since we push the NOCANCEL
        			}
        		});
        		break;
        	}

        	case SOLDIER_CARD: {
        		//final Card removed = getCurPlayer().removeCard(DevelopmentCardType.Soldier);
        		final Card used = getCurPlayer().getUsableCard(DevelopmentCardType.Soldier);
        		used.setUsed(true);
        		updateLargestArmyPlayer();
        		getCurPlayer().setCardsUsable(CardType.Development, false);
        		pushStateFront(mRules.isEnableSeafarersExpansion() ? State.POSITION_ROBBER_OR_PIRATE_CANCEL : State.POSITION_ROBBER_CANCEL, null, new UndoAction() {
        			public void undo() {
        				used.setUsed(false);
        				updateLargestArmyPlayer();
        				getCurPlayer().setCardsUsable(CardType.Development, true);
        				popState();
        			}
        		});
        		break;
        	}

        	case ROLL_DICE:
        		rollDice();
        		processDice();
        		break;
        		
        	case ALCHEMIST_CARD: {
        		if (getCurPlayer().setDice(mDice, 2)) {
        			mDice[0] = Utils.clamp(mDice[0], 1, 6);
        			mDice[1] = Utils.clamp(mDice[1], 1, 6);
        			mDice[3] = Utils.rand() % 6 + 1;
        			onDiceRolled(mDice);
        			printinfo("Player " + getCurPlayer().getName() + " applied Alchemist card on dice " +  mDice[0] + ", " + mDice[1] + ", " + DiceEvent.fromDieNum(mDice[2]));
            		processDice();
        		}
        		break;
        	}

        	case TRADE:
        		pushStateFront(State.SHOW_TRADE_OPTIONS);
        		break;

        	case BUILD_CITY_WALL: {
        		getCurPlayer().adjustResourcesForBuildable(BuildableType.CityWall, -1);
        		pushStateFront(State.CHOOSE_CITY_FOR_WALL, null, new UndoAction() {
					@Override
					public void undo() {
						getCurPlayer().adjustResourcesForBuildable(BuildableType.CityWall, 1);
					}
				});
        		break;
        	}

        	case ACTIVATE_KNIGHT: {
        		getCurPlayer().adjustResourcesForBuildable(BuildableType.ActivateKnight, -1);
        		pushStateFront(State.CHOOSE_KNIGHT_TO_ACTIVATE, null, new UndoAction() {
					@Override
					public void undo() {
						getCurPlayer().adjustResourcesForBuildable(BuildableType.ActivateKnight, 1);
					}
				});
        		break;
        	}
        	case HIRE_KNIGHT:{
        		getCurPlayer().adjustResourcesForBuildable(BuildableType.Knight, -1);
        		pushStateFront(State.POSITION_KNIGHT_CANCEL, VertexType.BASIC_KNIGHT_INACTIVE, new UndoAction() {
					@Override
					public void undo() {
						getCurPlayer().adjustResourcesForBuildable(BuildableType.Knight, 1);
					}
				});
        		mOptions = computeNewKnightVertexIndices(getCurPlayerNum(), mBoard);
        		break;
        	}
        	case MOVE_KNIGHT: 
        		popState();
        		pushStateFront(State.CHOOSE_KNIGHT_TO_MOVE);
        		break;
        		
        	case PROMOTE_KNIGHT:{
        		getCurPlayer().adjustResourcesForBuildable(BuildableType.PromoteKnight, -1);
        		pushStateFront(State.CHOOSE_KNIGHT_TO_PROMOTE, null, new UndoAction() {
					@Override
					public void undo() {
						getCurPlayer().adjustResourcesForBuildable(BuildableType.PromoteKnight, 1);
					}
				});
        		break;
        	}

        	case IMPROVE_CITY_POLITICS: {
        		popState();
        		processCityImprovement(getCurPlayer(), DevelopmentArea.Politics);
        		break;
        	}
        	
        	case IMPROVE_CITY_SCIENCE:{
        		popState();
        		processCityImprovement(getCurPlayer(), DevelopmentArea.Science);
        		break;
        	}
        	
        	case IMPROVE_CITY_TRADE:{
        		popState();
        		processCityImprovement(getCurPlayer(), DevelopmentArea.Trade);
        		break;
        	}

        	case CONTINUE:
        		popState();
        		break;
        		
			case CRANE_CARD: {
				// build a city improvement for 1 commodity less than normal
				ArrayList<MoveType> options = new ArrayList<MoveType>();
				DevelopmentArea areaSaved = null;
				for (DevelopmentArea area : DevelopmentArea.values()) {
					int devel = getCurPlayer().getCityDevelopment(area);
					int numCommodity = getCurPlayer().getCardCount(area.commodity);
					if (numCommodity >= devel) {
						options.add(area.move);
						areaSaved = area;
					}
				}
				popState();
				if (options.size() == 1) {
					// just apply it automatically
					processCityImprovement(getCurPlayer(), areaSaved);
				} else if (options.size() > 1) {
					mOptions = options;
					pushStateFront(State.CHOOSE_CITY_IMPROVEMENT);
				}
				break;
			}
				
			case DESERTER_CARD:{
				// replace an opponents knight with one of your own
				@SuppressWarnings("unchecked")
				List<Integer> [] playerKnight = new List[getNumPlayers()+1];
				List<Integer> players = new ArrayList<Integer>();
				for (int i=1; i<=getNumPlayers(); i++) {
					if (i == getCurPlayerNum())
						continue;
					playerKnight[i] = mBoard.getVertsOfType(i, VertexType.BASIC_KNIGHT_ACTIVE, VertexType.BASIC_KNIGHT_INACTIVE, VertexType.STRONG_KNIGHT_ACTIVE, VertexType.STRONG_KNIGHT_INACTIVE, VertexType.MIGHTY_KNIGHT_ACTIVE, VertexType.MIGHTY_KNIGHT_INACTIVE);
					if (playerKnight[i].size() > 0) {
						players.add(i);
					}
				}
				popState();
				if (players.size() == 1) {
					putCardBackInDeck(getCurPlayer().removeCard(ProgressCardType.Deserter));
					// automatically pick
					int player = players.get(0);
					if (playerKnight[player].size() == 1) {
						// automatically take the knight
						Vertex knight = mBoard.getVertex(playerKnight[player].get(0));
						knight.setPlayer(getCurPlayerNum());
					} else {
						pushStateFront(State.SET_PLAYER, getCurPlayerNum(), null);
						pushStateFront(State.CHOOSE_KNIGHT_TO_DESERT, getCurPlayerNum(), null);
						pushStateFront(State.SET_PLAYER, player, null);
						mOptions = playerKnight[player];
					}
				} else if (players.size() > 1) {
					mOptions = players;
					pushStateFront(State.CHOOSE_PLAYER_FOR_DESERTION);
				}
				break;
			}
			case DIPLOMAT_CARD: {
				List<Integer> allOpenRoutes = computeDiplomatOpenRouteIndices(this, mBoard);
				popState();
				if (allOpenRoutes.size() > 0) {
					mOptions = allOpenRoutes;
					pushStateFront(State.CHOOSE_DIPLOMAT_ROUTE);
				}
				break;
			}
			case ENGINEER_CARD: {
				// build a city wall for free
				List<Integer> cities = SOC.computeCityWallVertexIndices(getCurPlayerNum(), mBoard);
				if (cities.size() > 0) {
					final Card card = getCurPlayer().removeCard(ProgressCardType.Engineer);
					putCardBackInDeck(card);
					pushStateFront(State.CHOOSE_CITY_FOR_WALL, null, new UndoAction() {
						@Override
						public void undo() {
							getCurPlayer().addCard(card);
							removeCardFromDeck(card);
						}
					});
				}
				break;
			}
			case HARBOR_CARD: {
				// You may force each of the other players to make a special trade. 
				// You may offer each opponent any 1 Resource Card from your hand. He must exchange it for any
				// 1 Commodity Card from his hand of his choice, if he has any.
				// You must have a resource card to trade and they must have a commodity card to trade
				// You can skip a player if you wish
				for (Player p : computeHarborTradePlayers(getCurPlayer(), this)) {
					pushStateFront(State.CHOOSE_HARBOR_RESOURCE, p, null);
				}
				break;
			}
			case INTRIGUE_CARD: {
				// displace an opponents knight that is on your road without moving your own knight
				List<Integer> verts = computeIntrigueKnightsVertexIndices(getCurPlayerNum(), mBoard);
				if (verts.size() > 0) {
					mOptions = verts;
					pushStateFront(State.CHOOSE_OPPONENT_KNIGHT_TO_DISPLACE);
				}				
				break;
			}
			case INVENTOR_CARD: {
				// switch tile tokens of users choice
				List<Integer> tiles = computeInventorTileIndices(mBoard);
				if (tiles.size() == 2) {
					// just switch em
					Tile t0 = mBoard.getTile(tiles.get(0));
					Tile t1 = mBoard.getTile(tiles.get(1));
					int t = t0.getDieNum();
					t0.setDieNum(t1.getDieNum());
					t1.setDieNum(t);
					putCardBackInDeck(getCurPlayer().removeCard(ProgressCardType.Inventor));
				} else if (tiles.size() > 2) {
					pushStateFront(State.CHOOSE_TILE_INVENTOR);
				}				
				break;
			}
			case IRRIGATION_CARD: {
				// player gets 2 wheat for each structure on a field
				int numGained = 2 * computeNumStructuresAdjacentToTileType(getCurPlayerNum(), mBoard, TileType.FIELDS);
				if (numGained > 0) {
					getCurPlayer().incrementResource(ResourceType.Wheat, numGained);
					onDistributeResources(getCurPlayer(), ResourceType.Wheat, numGained);
					putCardBackInDeck(getCurPlayer().removeCard(ProgressCardType.Irrigation));
				}
				break;
			}
			case MASTER_MERCHANT_CARD: {
				// take 2 resource or commodity cards from another player who has more victory pts than you
				List<Integer> players = new ArrayList<Integer>();
				for (Player p :getPlayers()) {
					if (p == getCurPlayer())
						continue;
					if (p.getPoints() > getCurPlayer().getPoints()) {
						players.add(p.getPlayerNum());
					}
				}
				if (players.size() > 0) {
					mOptions = players; 
					pushStateFront(State.CHOOSE_PLAYER_MASTER_MERCHANT);
				}
				break;
			}
			case MEDICINE_CARD: {
				// upgrade to city for cheaper
				if (getCurPlayer().getCardCount(ResourceType.Ore) >= 2 && getCurPlayer().getCardCount(ResourceType.Wheat) >= 1) {
					List<Integer> settlements = mBoard.getSettlementsForPlayer(getCurPlayerNum());
					if (settlements.size() > 0) {
						mOptions = settlements;
						getCurPlayer().incrementResource(ResourceType.Ore, -2);
						getCurPlayer().incrementResource(ResourceType.Wheat, -1);
						final Card card = getCurPlayer().removeCard(ProgressCardType.Medicine);
						putCardBackInDeck(card);
						pushStateFront(State.POSITION_CITY_CANCEL, null, new UndoAction() {
							@Override
							public void undo() {
								removeCardFromDeck(card);
								getCurPlayer().addCard(card);
								getCurPlayer().incrementResource(ResourceType.Ore, 2);
								getCurPlayer().incrementResource(ResourceType.Wheat, 1);
							}
						});
					}
				}
				break;
			}
			case MERCHANT_CARD: {
				pushStateFront(State.POSITION_MERCHANT);
				break;
			}
			case MERCHANT_FLEET_CARD: {
				List<Card> tradableCards = new ArrayList<Card>();
				for (ResourceType t : ResourceType.values()) {
					List<Card> cards = getCurPlayer().getUsableCards(t);
					if (cards.size() >= 2) {
						tradableCards.addAll(cards);
					}
				}

				for (CommodityType t : CommodityType.values()) {
					List<Card> cards = getCurPlayer().getUsableCards(t);
					if (cards.size() >= 2) {
						tradableCards.addAll(cards);
					}
				}
				
				if (tradableCards.size() > 0) {
					mOptions = tradableCards;
					pushStateFront(State.CHOOSE_RESOURCE_FLEET);
				}
				break;
			}
			case MINING_CARD: {
				// player gets 2 ore for each structure on a field
				int numGained = 2 * computeNumStructuresAdjacentToTileType(getCurPlayerNum(), mBoard, TileType.MOUNTAINS);
				if (numGained > 0) {
					onDistributeResources(getCurPlayer(), ResourceType.Ore, numGained);
					putCardBackInDeck(getCurPlayer().removeCard(ProgressCardType.Mining));
				}
				break;
			}
			case RESOURCE_MONOPOLY_CARD: {
				final Card remove = getCurPlayer().removeCard(ProgressCardType.ResourceMonopoly);
				putCardBackInDeck(remove);
				pushStateFront(State.CHOOSE_RESOURCE_MONOPOLY, null, new UndoAction() {
					@Override
					public void undo() {
						removeCardFromDeck(remove);
						getCurPlayer().addCard(remove);
					}
				});
				break;
			}
			case SABOTEUR_CARD: {
				boolean done = false;
				for (Player p : getPlayers()) {
					if (p == getCurPlayer())
						continue;
					if (p.getPoints() < getCurPlayer().getPoints())
						continue;
					int num = (1+p.getUnusedCardCount()) / 2;
					if (num > 0) {
    					for (int i=0; i<num; i++) {
    						if (!done) {
    							pushStateFront(State.SET_PLAYER, getCurPlayerNum(), null);
        						done = true;
    						}
    						pushStateFront(State.GIVE_UP_CARD);
    					}
    					pushStateFront(State.SET_PLAYER, p.getPlayerNum(), null);
					}
				}
				if (done) {
					putCardBackInDeck(getCurPlayer().removeCard(ProgressCardType.Saboteur));
				}
				break;
			}
			case SMITH_CARD: {
				// promote 2 knights for free
				final List<Integer> knights = computePromoteKnightVertexIndices(getCurPlayer(), mBoard);
				if (knights.size() > 0) {
					final Card removed = getCurPlayer().removeCard(ProgressCardType.Smith); 
					putCardBackInDeck(removed);
    				if (knights.size() <= 2) {
    					// just promote em
    					for (int kIndex : knights) {
    						mBoard.getVertex(kIndex).promoteKnight();
    					}
    				} else {
    					pushStateFront(State.CHOOSE_KNIGHT_TO_PROMOTE, null, new UndoAction() {
							
							@Override
							public void undo() {
								for (int kIndex : knights) {
									mBoard.getVertex(kIndex).demoteKnight();
								}
								getCurPlayer().addCard(removed);
								removeCardFromDeck(removed);
							}
						});
    					pushStateFront(State.CHOOSE_KNIGHT_TO_PROMOTE, null, new UndoAction() {
							
							@Override
							public void undo() {
								popState(); // saa except for this
								for (int kIndex : knights) {
									mBoard.getVertex(kIndex).demoteKnight();
								}
								getCurPlayer().addCard(removed);
								removeCardFromDeck(removed);
							}
						});
    				}
				}
				break;
			}
			case SPY_CARD: {
				// steal a players progress cards
				List<Integer> players = computeSpyPlayers(this, getCurPlayerNum());
				if (players.size() > 0) {
					mOptions = players;
					pushStateFront(State.CHOOSE_PLAYER_TO_SPY_ON);
				}
				break;
			}
			case TRADE_MONOPOLY_CARD: {
				pushStateFront(State.CHOOSE_TRADE_MONOPOLY);
				break;
			}
			case WARLORD_CARD: {
				// Activate all knights
				List<Integer> knights = mBoard.getVertsOfType(getCurPlayerNum(), VertexType.BASIC_KNIGHT_INACTIVE, VertexType.STRONG_KNIGHT_INACTIVE, VertexType.MIGHTY_KNIGHT_INACTIVE);
				if (knights.size() > 0) {
    				for (int vIndex : knights) {
    					mBoard.getVertex(vIndex).activateKnight();
    				}
    				putCardBackInDeck(getCurPlayer().removeCard(ProgressCardType.Warlord));
				}
				break;
			}
			case WEDDING_CARD: {
				boolean done = false;
				for (Player p : getPlayers()) {
					if (p == getCurPlayer())
						continue;
					if (p.getPoints() > getCurPlayer().getPoints()) {
						int numUnused = p.getCardCount(CardType.Commodity);
						numUnused += p.getCardCount(CardType.Resource);
						if (numUnused > 0) {
							if (!done) {
								done = true;
								pushStateFront(State.SET_PLAYER, getCurPlayerNum(), null);
							}
    						if (numUnused <= 2) {
    							// automatically give
    							List<Card> cards = p.getCards(CardType.Commodity);
    							cards.addAll(p.getCards(CardType.Resource));
    							for (Card c : cards) {
    								p.removeCard(c);
    								getCurPlayer().addCard(c);
    								onTakeOpponentCard(getCurPlayer(), p, c);
    							}
    						} else {
    							pushStateFront(State.CHOOSE_GIFT_CARD, getCurPlayer(), null);
    							pushStateFront(State.CHOOSE_GIFT_CARD, getCurPlayer(), null);
    							pushStateFront(State.SET_PLAYER, p.getPlayerNum(), null);
    						}
						}
					}
				}
				if (done) {
					putCardBackInDeck(getCurPlayer().removeCard(ProgressCardType.Wedding));
				}
				break;
			}
		}
	}
	
	public static List<Integer> computeSpyPlayers(SOC soc, int playerNum) {
		List<Integer> players = new ArrayList<Integer>();
		for (Player p : soc.getPlayers()) {
			if (p.getPlayerNum() != playerNum) {
				if (p.getCardCount(CardType.Progress) > 0) {
					players.add(p.getPlayerNum());
				}
			}
		}
		return players;
	}

	public static int computeNumStructuresAdjacentToTileType(int playerNum, Board b, TileType type) {
		int num = 0;
		for (Tile t : b.getTiles()) {
			if (t.getType() != type)
				continue;
			for (Vertex v : b.getTileVertices(t)) {
				if (v.isStructure() && v.getPlayer() == playerNum) {
					num++;
				}
			}
		}
		return num;
	}

	private void putCardBackInDeck(Card card) {
		switch (card.getCardType()) {
			case Development:
				mDevelopmentCards.add(card);
				break;
			case Progress: {
				int index = ProgressCardType.values()[card.getTypeOrdinal()].type.ordinal();
				mProgressCards[index].add(card);
				break;
			}
			case Commodity:
			case Resource: // ignore, these are infinite
				break;
			case SpecialVictory:
				throw new RuntimeException("Should not happen");
		}
	}
	
	private void removeCardFromDeck(Card card) {
		boolean success = false;
		switch (card.getCardType()) {
			case Development:
				success = mDevelopmentCards.remove(card);
				break;
			case Progress: {
				int index = ProgressCardType.values()[card.getTypeOrdinal()].type.ordinal();
				success = mProgressCards[index].remove(card);
				break;
			}
			case Commodity:
			case Resource: // ignore, these are infinite
				success = true;
				break;
			case SpecialVictory:
				throw new RuntimeException("Should not happen");
		}
		assert(success);
	}

	/**
	 * Called when a trade completed as event for the user to handle if they wish
	 * base method does nothing.
	 * @param player
	 * @param trade
	 */
	protected void onTradeCompleted(Player player, Trade trade) {}
	
	/**
	 * Called when a player has discovered a new island for app to add any logic they want.
	 * @param player
	 * @param island
	 */
	protected void onPlayerdiscoveredIsland(Player player, Island island) {}
	
    /**
     * Set the current largest army player
     * @param playerNum
     */
    public void setLargestArmyPlayer(int playerNum) {
        this.mLargestArmyPlayer = playerNum;
    }
    
    /**
     * Set the current longest road player
     * @param playerNum
     */
    public void setLongestRoadPlayer(int playerNum) {
        this.mLongestRoadPlayer = playerNum;
    }

	/**
	 * Return true when it is legal for a player to cancel from their current Move.  
	 * @return
	 */
	public boolean canCancel() {
	    return getState().canCancel;
	}

	/**
	 * Typically this operation causes the game to revert a state.
	 *
	 */
	public void cancel() {
		if (!canCancel()) {
			logError("Calling cancel when cancel not allowed");
			return;
		}
		
		resetOptions();
		UndoAction undoAction = getUndoAction();
		popState();
		if (undoAction != null) {
		    undoAction.undo();
		} 

	}

	protected void onDiscoverTerritory(Player player, Tile tile) {}
	
	private void checkForDiscoveredNewTerritory(int vIndex) {
		int [] die = { 2,3,4,5,6,8,9,10,11,12 };
		for (Tile tile : mBoard.getVertexTiles(vIndex)) {
			if (tile.getType() == TileType.UNDISCOVERED) {
				int [] chances = new int[TileType.values().length];
				for (TileType t : TileType.values()) {
					chances[t.ordinal()] = t.chanceOnUndiscovered;
				}
				int index = Utils.chooseRandomFromSet(chances);
				TileType newType = TileType.values()[index];
				tile.setType(newType);
				assert(newType.chanceOnUndiscovered > 0);
				int resourceBonus = getRules().getNumResourcesForDiscoveredTerritory();
				onDiscoverTerritory(getCurPlayer(), tile);

				// apply bonus of any
				switch (newType) {
					case GOLD: {
						// choose random number
						tile.setDieNum(die[Utils.rand() % die.length]);
						for (int i=0; i<resourceBonus; i++)
							pushStateFront(State.DRAW_RESOURCE_NOCANCEL);
						break;
					}
					case FIELDS:
					case FOREST:
					case HILLS:
					case MOUNTAINS:
					case PASTURE:
						tile.setDieNum(die[Utils.rand() % die.length]);
						getCurPlayer().incrementResource(tile.getResource(), resourceBonus);
						onDistributeResources(getCurPlayer(), tile.getResource(), resourceBonus);
						break;
					default:
						break;
					
				}
				printinfo("Player " + getCurPlayer().getName() + " has discoverd a new territory: " + newType);
				
				for (Route r : mBoard.getTileRoutes(tile)) {
					if (tile.isWater())
						r.setAdjacentToWater(true);
					else
						r.setAdjacentToLand(true);
				}
			}
		}
	}

	private void checkForDiscoveredIsland(int vIndex) {
		for (Tile t : mBoard.getVertexTiles(vIndex)) {
			if (t.getIslandNum() > 0) {
				if (!mBoard.isIslandDiscovered(getCurPlayerNum(), t.getIslandNum())) {
					mBoard.setIslandDiscovered(getCurPlayerNum(), t.getIslandNum(), true);
					printinfo("Player " + getCurPlayer().getName() + " has discovered an island");
					onPlayerdiscoveredIsland(getCurPlayer(), mBoard.getIsland(t.getIslandNum()));
				}
			}
		}
	}
	
	/**
     * Called when a player get the longest road or overtakes another player.
     * default method does nothing
     * @param oldPlayer null if newPlayer is the first to get the longest road
     * @param newPlayer player that has the longest road
     * @param armySize
	 */
	protected void onLongestRoadPlayerUpdated(Player oldPlayer, Player newPlayer, int maxRoadLen) {}
	private void updateLongestRoutePlayer() {
	    int pNum = computeLongestRoadPlayer(this);
	    if (pNum < 1) {
	        if (mLongestRoadPlayer > 0) {
	            printinfo("Player " + getPlayerByPlayerNum(mLongestRoadPlayer).getName() + " is blocked and has lost the longest road!");
	            onLongestRoadPlayerUpdated(getPlayerByPlayerNum(mLongestRoadPlayer), null, 0);
	        }
	        mLongestRoadPlayer = -1;
	        return;
	    }
        if (pNum == mLongestRoadPlayer)
            return;
        final Player currentLongestRoadPlayer = getPlayerByPlayerNum(getLongestRoadPlayerNum());
        final Player maxRoadLenPlayer = getPlayerByPlayerNum(pNum);
        final int maxRoadLen = maxRoadLenPlayer.getRoadLength();
        
        if (mLongestRoadPlayer < 0) {
            printinfo("Player " + maxRoadLenPlayer.getName() + " has gained the Longest Road!");
            onLongestRoadPlayerUpdated(null, maxRoadLenPlayer, maxRoadLen);
        } else if (maxRoadLenPlayer.getRoadLength() > currentLongestRoadPlayer.getRoadLength()) {
            printinfo("Player " + maxRoadLenPlayer.getName() + " has overtaken Player " + getPlayerByPlayerNum(mLongestRoadPlayer).getName() + " with the Longest Road!");
            onLongestRoadPlayerUpdated(currentLongestRoadPlayer, maxRoadLenPlayer, maxRoadLen);
        }
        
        setLongestRoadPlayer(maxRoadLenPlayer.getPlayerNum());

	}
	
	/**
	 * Called when a player road becomes blocked, resulting is loss of road length.  Only used when Config.ENABLE_ROAD_BLOCK enabled.
	 * @param player
	 * @param road
	 */
	protected void onPlayerRoadBlocked(Player player, Route road) {}
	public void updatePlayerRoadsBlocked(Board board, int vertexIndex) {
	    if (mRules.isEnableRoadBlock()) {
		    Vertex vertex = board.getVertex(vertexIndex);
	        for (int i=0; i<vertex.getNumAdjacent(); i++) {
	            final Route edge = board.getRoute(vertexIndex, vertex.getAdjacent()[i]);
	            int playerNum = edge.getPlayer();
	            if (playerNum == 0)
	                continue;
	            if (playerNum == getCurPlayerNum())
	                continue;
	            BlockRoadsVisitor visitor = new BlockRoadsVisitor(vertexIndex, playerNum, BlockRoadsVisitor.MODE_SEARCH);
	            board.walkRouteTree(vertex.getAdjacent()[i], visitor);
	            if (visitor.num > 0) {
	                // if ANY will be removed, then rewalk the tree and remove
	                visitor = new BlockRoadsVisitor(vertexIndex, playerNum, BlockRoadsVisitor.MODE_REMOVE);
	                board.walkRouteTree(vertex.getAdjacent()[i], visitor);
	                Route e = board.getRoute(vertexIndex, vertex.getAdjacent()[i]);
	                board.setPlayerForRoute(e, 0);
	            }
	        }
	    }
	}
	
	private class BlockRoadsVisitor implements IVisitor {

	    final static int MODE_SEARCH = 0;
        final static int MODE_REMOVE = 1;
        
	    BlockRoadsVisitor(int startVertex, int playerNum, int mode) {
            this.playerNum = playerNum;
            this.startVertex = startVertex;
            this.mode = mode;
        }


	    final int startVertex;
        final int playerNum;
        final int mode;

        int num = 1;
        
        @Override
        public boolean visit(Route e, int depth) {
            //System.out.print(getIndent(depth) + "visit: " + e);
            if (num == 0)
                return false;
            if (e.getPlayer() != playerNum) {
                System.out.println(" a:return false (no road)" + num);
                return false;
            }
            if (mBoard.getVertex(e.getFrom()).getPlayer() == playerNum || mBoard.getVertex(e.getTo()).getPlayer() == playerNum) {
                assert(mode == MODE_SEARCH);
                num = 0;
                System.out.println(" b:return false (structureFound) num=" + num);
                return false;
            }
            if (mode == MODE_REMOVE) {
                onPlayerRoadBlocked(getPlayerByPlayerNum(e.getPlayer()), e);
                getBoard().setPlayerForRoute(e, 0);
            }
            num ++;
            if (mBoard.getVertex(e.getFrom()).getPlayer() != 0 || mBoard.getVertex(e.getTo()).getPlayer() != 0) {
                return false;
            }
            System.out.println(" c:return true num=" + num);
            return true;
        }
        
        @Override
        public boolean canRecurse(int vertexIndex) {
            return (vertexIndex != startVertex);            
        }
	    
	    
	}
	
	/**
	 * compute the player num who should have the longest road, or 0 if none exists.
	 * soc is not changed.
	 * @param soc
	 * @return
	 */
	public static int computeLongestRoadPlayer(SOC soc) {
		int maxRoadLen = soc.mRules.getMinLongestLoadLen() - 1;		
		if (soc.getLongestRoadPlayerNum() > 0)
		    maxRoadLen = Math.max(maxRoadLen, soc.getPlayerByPlayerNum(soc.getLongestRoadPlayerNum()).getRoadLength());
		Player maxRoadLenPlayer = soc.getPlayerByPlayerNum(soc.getLongestRoadPlayerNum());
		
		for (int i=0; i<soc.mNumPlayers; i++) {
			Player cur = soc.mPlayers[i];
			int len = cur.getRoadLength();
			if (len > maxRoadLen) {
				maxRoadLen = len;
				maxRoadLenPlayer = cur;
			}
		} 

		if (maxRoadLenPlayer == null)
		    return -1;
			//return soc.getLongestRoadPlayerNum();
		
		if (maxRoadLenPlayer.getRoadLength() >= soc.mRules.getMinLongestLoadLen())
			return maxRoadLenPlayer.getPlayerNum();
		
		return -1;
	}

	/**
	 * Called when a player get the largest army or overtakes another player.
	 * default method does nothing
	 * @param oldPlayer null if newPlayer is the first to get the largest army
	 * @param newPlayer player that has the largest army
	 * @param armySize current largest army size
	 */
	protected void onLargestArmyPlayerUpdated(Player oldPlayer, Player newPlayer, int armySize) {}
	private void updateLargestArmyPlayer() {
	    int pNum = computeLargestArmyPlayer(this);
	    if (pNum < 1) {
	        mLargestArmyPlayer = -1;
	        return;
	    }
	    if (pNum == this.mLargestArmyPlayer)
	        return;

        Player maxArmyPlayer = getPlayerByPlayerNum(pNum);
	    final int maxArmySize = maxArmyPlayer.getArmySize();
        final Player currentLargestArmyPlayer = getPlayerByPlayerNum(mLargestArmyPlayer);
	    if (mLargestArmyPlayer < 0) {
	        printinfo("Player " + maxArmyPlayer.getName() + " has the largest army!");
	        onLargestArmyPlayerUpdated(null, maxArmyPlayer, maxArmySize);
	    } else if (maxArmyPlayer.getArmySize() > currentLargestArmyPlayer.getArmySize()) {
	        printinfo("Player " + maxArmyPlayer.getName() + " takes overtakes Player " + getPlayerByPlayerNum(mLargestArmyPlayer).getName() + " for the largest Army!");
	        onLargestArmyPlayerUpdated(currentLargestArmyPlayer, maxArmyPlayer, maxArmySize);
	    }

	    setLargestArmyPlayer(maxArmyPlayer.getPlayerNum());
	}
	
	/**
	 * compute the player who should have the largest army.
	 * soc is not changed.
	 * @param soc
	 * @return
	 */
	public static int computeLargestArmyPlayer(SOC soc) {
		int maxArmySize = soc.mRules.getMinLargestArmySize() - 1;
		if (soc.getLargestArmyPlayerNum() > 0)
		    maxArmySize = soc.getPlayerByPlayerNum(soc.getLargestArmyPlayerNum()).getArmySize();
		Player maxArmyPlayer = null;
		for (int i=0; i<soc.mNumPlayers; i++) {
			Player cur = soc.mPlayers[i];
			if (cur.getArmySize() > maxArmySize) {
				maxArmySize = cur.getArmySize();
				maxArmyPlayer = cur;
			}
		}

		if (maxArmyPlayer == null)
			return soc.getLargestArmyPlayerNum();
		
		if (maxArmyPlayer.getArmySize() >= soc.mRules.getMinLargestArmySize())
		    return maxArmyPlayer.getPlayerNum();
		
		return -1;
	}

	/**
	 * Return a list of vertices available for a settlement given a player and board instance.
	 * 
	 * @param p
	 * @param b
	 * @return
	 */
	static public List<Integer> computeSettlementVertexIndices(SOC soc, int playerNum, Board b) {

		// build an array of vertices legal for the current player
		// to place a settlement.
		List<Integer> vertices = new ArrayList<Integer>();
		for (int i = 0; i < b.getNumVerts(); i++) {
			Vertex v = b.getVertex(i);
			if (v.getPlayer() != 0)
				continue;
			if (!v.canPlaceStructure())
				continue;
			boolean isOnIsland = false;
			for (Tile cell : b.getVertexTiles(i)) {
				if (cell.getIslandNum() > 0) {
					isOnIsland = true;
					break;
				}
			}
			
			boolean canAdd = true;
			boolean isOnRoute = false;
			for (int ii = 0; ii < v.getNumAdjacent(); ii++) {
				int iv = b.findAdjacentVertex(i, ii);
				if (iv >= 0) {
					Vertex v2 = b.getVertex(iv);
					if (v2.getPlayer() != 0) {
						canAdd = false;
						break;
					}

					int ie = b.getRouteIndex(i, iv);
					if (ie >= 0) {
						Route e = b.getRoute(ie);
						if (e.getPlayer() == playerNum) {
							isOnRoute = true;
						}
					}
				}
			}

			if (!canAdd)
				continue;

			if (b.getNumStructuresForPlayer(playerNum) < (soc == null ? 2 : soc.getRules().getNumStartSettlements())) {
				if (soc.getRules().isEnableIslandSettlementsOnSetup() || !isOnIsland)
					vertices.add(i);
			} else {
				if (isOnRoute)
					vertices.add(i);
			}
		}
		return vertices;
	}

	/**
	 * Return a list of edges available for a road given a player and board instance.  
	 * @param p
	 * @param b
	 * @return
	 */
	static public List<Integer> computeRoadRouteIndices(int playerNum, Board b) {
        //if (Profiler.ENABLED) Profiler.push("SOC::computeRoadOptions");
        try {
    	    
    	    List<Integer> edges = new ArrayList<Integer>();
    		for (int i = 0; i < b.getNumRoutes(); i++) {
    			if (b.isRouteAvailableForRoad(i, playerNum))
    				edges.add(i);
    		}
    		return edges;
        } finally {
            //if (Profiler.ENABLED) Profiler.pop("SOC::computeRoadOptions");
        }
	}

	static public List<Integer> computeShipRouteIndices(int playerNum, Board b) {
		//if (Profiler.ENABLED) Profiler.push("SOC::computeRoadOptions");
        try {
    	    
    	    List<Integer> edges = new ArrayList<Integer>();
    		for (int i = 0; i < b.getNumRoutes(); i++) {
    			if (b.isRouteAvailableForShip(i, playerNum))
    				edges.add(i);
    		}
    		return edges;
        } finally {
            //if (Profiler.ENABLED) Profiler.pop("SOC::computeRoadOptions");
        }
	}
	
	/**
	 * return a list of vertices a new level 1 knight can be placed (basically any vertex that is on the road of a player that is empty)
	 * @param playerNum
	 * @param b
	 * @return
	 */
	static public List<Integer> computeNewKnightVertexIndices(int playerNum, Board b) {
		HashSet<Integer> verts = new HashSet<Integer>();
		// any open vertex on a players road will suffice
		for (Route r : b.getRoutes()) {
			if (r.getPlayer() != playerNum)
				continue;
			if (b.getVertex(r.getFrom()).getType() == VertexType.OPEN)
				verts.add(r.getFrom());
			if (b.getVertex(r.getTo()).getType() == VertexType.OPEN)
				verts.add(r.getTo());
		}
		return new ArrayList<Integer>(verts);
	}
	
	/**
	 * Return list of vertices where a knight can be promoted (this assumes the player has passed the canBuild(Knight) test)
	 * @param playerNum
	 * @param b
	 * @return
	 */
	static public List<Integer> computePromoteKnightVertexIndices(Player p, Board b) {
		List<Integer> verts = new ArrayList<Integer>();
		for (int vIndex=0; vIndex < b.getNumVerts(); vIndex++) {
			Vertex v = b.getVertex(vIndex);
			if (v.getPlayer() == p.getPlayerNum() && !v.isPromotedKnight()) {
				switch (v.getType()) {
					case BASIC_KNIGHT_ACTIVE:
					case BASIC_KNIGHT_INACTIVE:
						verts.add(vIndex);
						break;
					case STRONG_KNIGHT_ACTIVE:
					case STRONG_KNIGHT_INACTIVE:
						if (p.getCityDevelopment(DevelopmentArea.Politics) >= 3) {
							verts.add(vIndex);
						}
						break;
					default:
						break;
				}
			}
		}
		
		return verts;
	}
	
	static public List<Integer> computeDiplomatOpenRouteIndices(SOC soc, Board b) {
		List<Integer> allOpenRoutes = new ArrayList<Integer>();
		for (int i=1; i<=soc.getNumPlayers(); i++) {
			allOpenRoutes.addAll(computeOpenRouteIndices(i, b, true, false));
		}
		return allOpenRoutes;
	}
	
	static public List<Integer> computeOpenRouteIndices(int playerNum, Board b, boolean checkRoads, boolean checkShips) {
		Set<Integer> options = new HashSet<Integer>();
		for (int eIndex=0; eIndex<b.getNumRoutes(); eIndex++) {
			Route e = b.getRoute(eIndex);
			// check the obvious
			if (e.isLocked())
				continue;
			if (e.getPlayer() != playerNum)
				continue;
			if (e.isShip()) {
				if (!checkShips)
					continue;
				if (e.isAttacked())
					continue;
			} else {
				if (!checkRoads)
					continue;
			}
			
			// if either vertex is the players settlement, then not movable
			if (b.getVertex(e.getFrom()).getPlayer() == playerNum)
				continue;
			if (b.getVertex(e.getTo()).getPlayer() == playerNum)
				continue;
			// if there is a route from either end, then not open
			int numConnected = 0;
			for (Route ee : b.getVertexRoutes(e.getFrom())) {
				if (ee != e && ee.getPlayer() == playerNum) {
					numConnected++;
					break;
				}
			}
			for (Route ee : b.getVertexRoutes(e.getTo())) {
				if (ee != e && ee.getPlayer() == playerNum) {
					numConnected++;
					break;
				}
			}
			if (numConnected < 2) {
				options.add(eIndex);
			}
		}
		return new ArrayList<Integer>(options);
	}
	
	/**
	 * Return list of verts a knight can move to including those where they can displace another player knight.  
	 * These are all open verts that lie on the same route as the knight.
	 * @param knightVertex
	 * @param b
	 * @return
	 */
	static public List<Integer> computeKnightMoveVertexIndices(int knightVertex, Board b) {
		List<Integer> verts = new ArrayList<Integer>();
		Vertex knight = b.getVertex(knightVertex);
		assert(knight != null);
		assert(knight.getType().getKnightLevel() > 0);
		boolean [] visitedVerts = new boolean[b.getNumVerts()];
		visitedVerts[knightVertex] = true;
		findReachableVertsR(b, knight, knightVertex, verts, visitedVerts);
		return verts;
	}
	
	static public List<Integer> computeActivateKnightVertexIndices(int playerNum, Board b) {
		return b.getVertsOfType(playerNum, VertexType.BASIC_KNIGHT_INACTIVE, VertexType.STRONG_KNIGHT_INACTIVE, VertexType.MIGHTY_KNIGHT_INACTIVE);		
	}
	
	static public List<Integer> computeDisplacedKnightVertexIndices(int displacedKnightVertex, Board b) {
		return computeKnightMoveVertexIndices(displacedKnightVertex, b);
	}
	
	static public List<Integer> computeMovableKnightVertexIndices(int playerNum, Board b) {
		List<Integer> knights = b.getVertsOfType(playerNum, VertexType.BASIC_KNIGHT_ACTIVE, VertexType.STRONG_KNIGHT_ACTIVE, VertexType.MIGHTY_KNIGHT_ACTIVE);
		List<Integer> movableKnights = new ArrayList<Integer>();
		for (int kIndex: knights) {
			if (computeKnightMoveVertexIndices(kIndex, b).size() > 0) {
				movableKnights.add(kIndex);
			}
		}
		return movableKnights;
	}

	private static void findReachableVertsR(Board b, Vertex knight, int startVertex, List<Integer> verts, boolean [] visitedVerts) {
		Vertex start = b.getVertex(startVertex);
		for (int i=0; i<start.getNumAdjacent(); i++) {
			int vIndex = start.getAdjacent()[i];
			if (visitedVerts[vIndex])
				continue;
			visitedVerts[vIndex] = true;
			int rIndex = b.getRouteIndex(startVertex, vIndex);
			Route r = b.getRoute(rIndex);
			if (r.getPlayer() != knight.getPlayer())
				continue;
			Vertex v = b.getVertex(vIndex);
			if (v.getType() == VertexType.OPEN)
				verts.add(vIndex);
			else if (v.getPlayer() != knight.getPlayer() && knight.getType().isKnightActive()) {
				int kl = v.getType().getKnightLevel();
				if (kl > 0 && kl < knight.getType().getKnightLevel()) {
					verts.add(vIndex); // TODO: we can move to this vertex but not pass it?
				}
				continue;
			}
			findReachableVertsR(b, knight, vIndex, verts, visitedVerts);
		}
	}

	/**
	 * Return a list of vertices available for a city given a player and board intance.
	 * @param p
	 * @param b
	 * @return
	 */
	static public List<Integer> computeCityVertxIndices(int playerNum, Board b) {
		return b.getVertsOfType(playerNum, VertexType.SETTLEMENT);
	}

	/**
	 * Return a list of vertices available for a city given a player and board instance.
	 * @param p
	 * @param b
	 * @return
	 */
	static public List<Integer> computeCityWallVertexIndices(int playerNum, Board b) {
		return b.getVertsOfType(playerNum, VertexType.CITY);
	}
	
	/**
	 * Return a list of vertices available for a metropolis given a player and board instance
	 * @param playerNum
	 * @param b
	 * @return
	 */
	static public List<Integer> computeMetropolisVertexIndices(int playerNum, Board b) {
		return b.getVertsOfType(playerNum, VertexType.CITY, VertexType.WALLED_CITY);
	}

	/**
	 * Return a list of MoveTypes available given a player and board instance.
	 * @param p
	 * @param b
	 * @return
	 */
	static public List<MoveType> computeMoves(Player p, Board b, SOC soc) {
		List<MoveType> types = new ArrayList<MoveType>();
        types.add(MoveType.CONTINUE);

		if (p.canBuild(BuildableType.City) && b.getNumSettlementsForPlayer(p.getPlayerNum()) > 0)
		    types.add(MoveType.BUILD_CITY);

		if (p.canBuild(BuildableType.Development))
		    types.add(MoveType.DRAW_DEVELOPMENT);

		for (DevelopmentCardType t :DevelopmentCardType.values()) {
			if (t.moveType != null && p.getUsableCardCount(t) > 0) {
				types.add(t.moveType);
			}
		}

		if (canPlayerTrade(p, b))
		    types.add(MoveType.TRADE);
		
		if (p.canBuild(BuildableType.Settlement)) {
			for (int i = 0; i < b.getNumVerts(); i++) {
				if (b.isVertexAvailbleForSettlement(i) && b.isVertexAdjacentToPlayerRoute(i, p.getPlayerNum())) {
					types.add(MoveType.BUILD_SETTLEMENT);
					break;
				}
			}
		}

		if (p.canBuild(BuildableType.Road)) {
			for (int i=0; i<b.getNumRoutes(); i++) {
				if (b.isRouteAvailableForRoad(i, p.getPlayerNum())) {
					types.add(MoveType.BUILD_ROAD);
					break;
				}
			}
			
		}
		
		if (soc.mRules.isEnableSeafarersExpansion()) {
			if (p.canBuild(BuildableType.Ship)) {
				for (int i=0; i<b.getNumRoutes(); i++) {
					if (b.isRouteAvailableForShip(i, p.getPlayerNum())) {
						types.add(MoveType.BUILD_SHIP);
						break;
					}
				}
			}
			
			// check for movable ships
			if (computeOpenRouteIndices(p.getPlayerNum(), b, false, true).size() > 0) {
				types.add(MoveType.MOVE_SHIP);
			}
		}
		
		if (soc.mRules.isEnableCitiesAndKnightsExpansion()) {
			
			for (ProgressCardType t: ProgressCardType.values()) {
				if (t.moveType != null && p.getUsableCardCount(t) > 0) {
					types.add(t.moveType);
				}
			}
			
			if (p.canBuild(BuildableType.CityWall)) {
				if (b.getNumVertsOfType(p.getPlayerNum(), VertexType.CITY) > 0) {
					types.add(MoveType.BUILD_CITY_WALL);
				}
			}
			
			if (p.canBuild(BuildableType.Knight)) {
				if (b.getOpenKnightVertsForPlayer(p.getPlayerNum()).size() > 0) {
					types.add(MoveType.HIRE_KNIGHT);
				}
				if (b.getNumVertsOfType(p.getPlayerNum(), VertexType.BASIC_KNIGHT_ACTIVE, VertexType.BASIC_KNIGHT_INACTIVE) > 0) {
					types.add(MoveType.PROMOTE_KNIGHT);
				} else if (p.hasFortress() && 0 < b.getNumVertsOfType(p.getPlayerNum(), VertexType.STRONG_KNIGHT_ACTIVE, VertexType.STRONG_KNIGHT_INACTIVE)) {
					types.add(MoveType.PROMOTE_KNIGHT);
				}
			}
			
			if (p.canBuild(BuildableType.ActivateKnight)) {
				if (computeActivateKnightVertexIndices(p.getPlayerNum(), b).size() > 0)
					types.add(MoveType.ACTIVATE_KNIGHT);
			}

			for (int vIndex : b.getVertsOfType(p.getPlayerNum(), VertexType.BASIC_KNIGHT_ACTIVE, VertexType.STRONG_KNIGHT_ACTIVE, VertexType.MIGHTY_KNIGHT_ACTIVE)) {
				if (computeKnightMoveVertexIndices(vIndex, b).size() > 0) {
					types.add(MoveType.MOVE_KNIGHT);
					break;
				}
			}
			
			if (b.getNumCitiesForPlayer(p.getPlayerNum())> 0) {
				for (DevelopmentArea area : DevelopmentArea.values()) {
					int devel = p.getCityDevelopment(area);
					if (devel < 5 && p.getCardCount(area.commodity) > devel) {
						types.add(area.move);
					}
				}
			}
		}

		return types;
	}

	static public List<Integer> computePirateTileIndices(SOC soc, Board b) {
		if (!soc.getRules().isEnableSeafarersExpansion())
			return Collections.emptyList();
		List<Integer> cellIndices = new ArrayList<Integer>();
		for (int i = 0; i < b.getNumTiles(); i++) {
			Tile cell = b.getTile(i);
			if (cell.isWater())
				cellIndices.add(i);
		}
		return cellIndices;
	}
	
	/**
	 * Return a list of cells available for a robber given a board instance.
	 * @param b
	 * @return
	 */
	static public List<Integer> computeRobberTileIndices(SOC soc, Board b) {
		List<Integer> cellIndices = new ArrayList<Integer>();
		boolean desertIncluded = false;
		for (int i = 0; i < b.getNumTiles(); i++) {
			Tile cell = b.getTile(i);
			if (!desertIncluded) {
				if (cell.getType() == TileType.DESERT) {
					cellIndices.add(i);
					desertIncluded = true;
					continue;
				}
			}
			if (!cell.isDistributionTile())
				continue;
			// only test tiles that has opposing players on them
			boolean hasPlayer = false;
			for (int vIndex=0; vIndex<cell.getNumAdj(); vIndex++) {
				Vertex v = b.getVertex(cell.getAdjVert(vIndex));
				if (v.getPlayer() > 0 && v.getPlayer() != soc.getCurPlayerNum()) {
					hasPlayer = true;
					break;
				}
			}
			if (!desertIncluded) {
				if (cell.getType() == TileType.DESERT) {
					cellIndices.add(i);
					desertIncluded = true;
				}
			}
			if (hasPlayer) {
				cellIndices.add(i);
			}
		}
		return cellIndices;
	}
	
	/**
	 * Return a list of cells available for a merchant given a board instance.
	 * @param b
	 * @return
	 */
	static public List<Integer> computeMerchantTileIndices(SOC soc, int playerNum, Board b) {
		List<Integer> cellIndices = new ArrayList<Integer>();
		for (int i = 0; i < b.getNumTiles(); i++) {
			if (b.getRobberTile() == i)
				continue;
			Tile cell = b.getTile(i);
			if (cell.isDistributionTile() && cell.getResource() != null) {
				for (Vertex v : b.getTileVertices(cell)) {
					if (v.isStructure() && v.getPlayer() == playerNum) {
						cellIndices.add(i);
						break;
					}
				}
			}
		}
		return cellIndices;
	}

	/**
	 * Return the list of unused resource and development cards a player can give up.
	 * @param p
	 * @return
	 */
	static public List<Card> computeGiveUpCards(Player p) {
		List<Card> cards = new ArrayList<Card>();
		cards.addAll(p.getUnusedCards(CardType.Resource));
		cards.addAll(p.getUnusedCards(CardType.Development));
		cards.addAll(p.getUnusedCards(CardType.Commodity));
		return cards;
	}

	/**
	 * Compute all the trade options given a player and board instance.
	 * @param p
	 * @param b
	 * @return
	 */
	static public List<Trade> computeTrades(Player p, Board b) {
		List<Trade> trades = new ArrayList<Trade>();
		computeTrades(p, b, trades, 10);
		return trades;
	}
	
	static public int computeCatanStrength(SOC soc, Board b) {
		return b.getNumVertsOfType(0, VertexType.BASIC_KNIGHT_ACTIVE) +
			   b.getNumVertsOfType(0, VertexType.STRONG_KNIGHT_ACTIVE) * 2 +
			   b.getNumVertsOfType(0, VertexType.MIGHTY_KNIGHT_ACTIVE) * 3;
	}
	
	static public int computeBarbarianStrength(SOC soc, Board b) {
		return b.getNumVertsOfType(0, VertexType.CITY, VertexType.WALLED_CITY);
	}
	
	static private void computeTrades(Player p, Board b, List<Trade> trades, int maxOptions) {
		
        //if (Profiler.ENABLED) Profiler.push("SOC::computeTradeOptions");
	    
        try {
    	    int i;
    		int numOptions = 0;
    
    		boolean[] resourcesFound = new boolean[NUM_RESOURCE_TYPES];
    		boolean[] commoditiesFound = new boolean[NUM_COMMODITY_TYPES];
    		
    		// Check for Merchant Fleet
    		if (p.getMerchantFleetTradable() != null) {
    			if (p.getCardCount(p.getMerchantFleetTradable()) >= 2) {
					trades.add(new Trade(p.getMerchantFleetTradable(), 2));
        			switch (p.getMerchantFleetTradable().getCardType()) {
    					case Commodity:
    						commoditiesFound[p.getMerchantFleetTradable().getTypeOrdinal()] = true;
    						break;
    					case Resource:
    						resourcesFound[p.getMerchantFleetTradable().getTypeOrdinal()] = true;
    						break;
    					default:
    						throw new RuntimeException("Unexpected case");
        				
        			}
    			}
    		}
    		
    		// see if we have a 2:1 trade option
    		for (i = 0; i < b.getNumTiles(); i++) {
    			Tile tile = b.getTile(i);
    			if (b.getMerchantTile() == i && b.getMerchantPlayer() == p.getPlayerNum() && tile.isDistributionTile()) {
    				// player gets the 2:1 trade benefit for this tile resource
    			} else {
    			
        			if (!tile.isPort() || null==tile.getResource()) 
        				continue;
        
        			if (!b.isPlayerAdjacentToTile(p.getPlayerNum(), i))
        				continue;
        
        			if (p.getCardCount(tile.getResource()) < 2)
        				continue;
    			}

    			if (resourcesFound[tile.getResource().ordinal()])
    				continue;

    			trades.add(new Trade(tile.getResource(), 2));
    			resourcesFound[tile.getResource().ordinal()] = true;
    			
    			if (numOptions >= maxOptions)
    				return;
    		}
    
    		// we have a 3:1 trade option when we are adjacent to a PORT_MULTI
    		for (i = 0; i < b.getNumTiles(); i++) {
    			Tile cell = b.getTile(i);
    			if (cell.getType() != TileType.PORT_MULTI)
    				continue;
    
    			if (!b.isPlayerAdjacentToTile(p.getPlayerNum(), i))
    				continue;
    
    			// for (int r=0; r<Helper.NUM_RESOURCE_TYPES; r++) {
    			for (ResourceType r : ResourceType.values()) {
    				if (!resourcesFound[r.ordinal()] && p.getCardCount(r) >= 3) {
    					trades.add(new Trade(r, 3));
    					resourcesFound[r.ordinal()] = true;
    					if (numOptions >= maxOptions)
    						return;
    				}
    			}
    			
    			for (CommodityType c : CommodityType.values()) {
    				if (!commoditiesFound[c.ordinal()] && p.getCardCount(c) >= 3) {
    					trades.add(new Trade(c, 3));
    					commoditiesFound[c.ordinal()] = true;
    					if (numOptions >= maxOptions)
    						return;
    				}
    			}
    		}
    
    		// look for 4:1 trades
    		for (ResourceType r : ResourceType.values()) {
    
    			if (!resourcesFound[r.ordinal()] && p.getCardCount(r) >= 4) {
    				trades.add(new Trade(r, 4));
    
    				if (numOptions >= maxOptions)
    					return;
    			}
    		}
    		
    		for (CommodityType c : CommodityType.values()) {
				if (!commoditiesFound[c.ordinal()] && p.getCardCount(c) >= 4) {
					trades.add(new Trade(c, 4));
					if (numOptions >= maxOptions)
						return;
				}
			}
    		
        } finally {
            //if (Profiler.ENABLED) Profiler.pop("SOC::computeTradeOptions");
        }
	}

	/**
	 * Compute the list of players from which 'p' can take a card 
	 * @param p
	 * @param b
	 * @return
	 */
	public List<Player> computeTakeOpponentCardOptions(Player p, Board b, boolean pirate) {
	    return computeTakeOpponentCardPlayers(getAllPlayers(), p, b, pirate);
	}
	
	/**
	 * 
	 * @param players
	 * @param p
	 * @param b
	 * @return
	 */
	static public List<Player> computeTakeOpponentCardPlayers(Player [] players, Player p, Board b, boolean pirate) {
		
	    List<Player> choices = new ArrayList<Player>();
		boolean [] playerNums = new boolean[players.length];
		
		if (pirate) {
			for (int eIndex : b.getTileRouteIndices(b.getTile(b.getPirateTile()))){
				Route e = b.getRoute(eIndex);
				if (e.getPlayer() == 0)
					continue;
				if (e.getPlayer() == p.getPlayerNum())
					continue;
				if (players[e.getPlayer()].getTotalCardsLeftInHand() <= 0)
					continue;
				playerNums[e.getPlayer()] = true;
			}
		} else {
    		Tile cell = b.getTile(b.getRobberTile());
    		for (int vIndex : cell.getAdjVerts()) {
    			Vertex v = b.getVertex(vIndex);
    			if (v.getPlayer() == 0)
    				continue;
    			if (v.getPlayer() == p.getPlayerNum())
    				continue;
    			if (players[v.getPlayer()].getTotalCardsLeftInHand() <= 0)
    				continue;
    			playerNums[v.getPlayer()] = true;
    		}
		}
		
		for (int i=1; i<playerNums.length; i++)
			if (playerNums[i])
				choices.add(players[i]);
		return choices;
	}
	
	/**
	 * 
	 * @param soc
	 * @param p
	 * @return
	 */
	static public List<Card> computeGiftCards(SOC soc, Player p) {
		List<Card> cards = p.getCards(CardType.Commodity);
		cards.addAll(p.getCards(CardType.Resource));
		return cards;
	}
	
	/**
	 * 
	 * @param p
	 * @param b
	 * @return
	 */
	static public boolean canPlayerTrade(Player p, Board b) {
		List<Trade> options = new ArrayList<Trade>();
		computeTrades(p, b, options, 1);
		return options.size() > 0;
	}

	/**
	 * Get a new array of all players
	 * @return
	 */
	private Player [] getAllPlayers() {
		Player [] players = new Player[mNumPlayers+1];
		for (int i=0; i<mNumPlayers;i++)
			players[i+1] = mPlayers[i];
		return players;
	}
	
	/**
	 * 
	 * @return
	 */
	public final Iterable<Player> getPlayers() {
		return Arrays.asList(Arrays.copyOf(mPlayers, mNumPlayers));
	}
	
	private void processCityImprovement(Player p, DevelopmentArea area) {
		printinfo("Player " + p.getName() + " is improving their " + area);
		int devel = p.getCityDevelopment(area);
		assert(devel <= 5);
		p.removeCards(area.commodity, devel+1);
		p.setCityDevelopment(area, devel+1);
		checkMetropolis(devel, getCurPlayerNum(), area);
	}
	
	private void processDice() {
		// roll the dice
		if (getDiceNum() == 7) {
			popState();
			printinfo("Uh Oh, Player " + getCurPlayerNum() + " rolled a 7.");
			pushStateFront(State.NEXT_PLAYER);
			pushStateFront(State.SET_PLAYER, getCurPlayerNum(), null);
			for (int ii=0; ii<mNumPlayers; ii++) {
				Player cur = mPlayers[(ii + mCurrentPlayer) % mNumPlayers];
				int numCards = cur.getTotalCardsLeftInHand();
				if (numCards > getMinHandCardsForRobberDiscard(cur.getPlayerNum())) {
					int numCardsToSurrender = numCards / 2;
					printinfo("Player " + cur.getPlayerNum() + " must give up " + numCardsToSurrender + " of " + numCards + " cards");
					while (numCardsToSurrender > 0)
						pushStateFront(State.GIVE_UP_CARD, numCardsToSurrender--, null);
					pushStateFront(State.SET_PLAYER, cur.getPlayerNum(), null);
				}
			} 
			if (mRules.isEnableSeafarersExpansion())
				pushStateFront(State.POSITION_ROBBER_OR_PIRATE_NOCANCEL);
			else
				pushStateFront(State.POSITION_ROBBER_NOCANCEL);
		} else {

			for (int i = 0; i < mNumPlayers-1; i++) {
				if (i > 0)
					pushStateFront(State.PLAYER_TURN_NOCANCEL);
				pushStateFront(State.NEXT_PLAYER);
			}
			// reset the players ships/development cards usability etc.
			pushStateFront(State.INIT_PLAYER_TURN);

			// do this last so that any states that get pushed are on top
			distributeResources(getDiceNum());
		}
		
		if (mRules.isEnableCitiesAndKnightsExpansion()) {
			DiceEvent ev = DiceEvent.fromDieNum(mDice[2]);
			switch (ev) {
				case AdvanceBarbarianShip:
					processBarbarianShip();
					break;
				case PoliticsCard:
					distributeProgressCard(DevelopmentArea.Politics);
					break;
				case ScienceCard:
					distributeProgressCard(DevelopmentArea.Science);
					break;
				case TradeCard:
					distributeProgressCard(DevelopmentArea.Trade);
					break;        				
			}
		}		
	}
	
	/**
	 * Called when a player loses their metropolis to another player
	 * @param loser
	 * @param stealer
	 * @param area
	 */
	protected void onMetropolisStolen(int loser, int stealer, DevelopmentArea area) {}
	
	private void checkMetropolis(int devel, int playerNum, DevelopmentArea area) {
		if (devel >= 4) {
			List<Integer> metropolis = mBoard.getVertsOfType(0, area.vertexType);
			if (metropolis.size() == 0) {
				pushStateFront(State.CHOOSE_METROPOLIS, area, null);
			} else {
				assert(metropolis.size() == 1);
				Vertex v = mBoard.getVertex(metropolis.get(0));
				if (v.getPlayer() != getCurPlayerNum()) {
					Player other = getPlayerByPlayerNum(v.getPlayer());
					int otherDevel = getPlayerByPlayerNum(v.getPlayer()).getCityDevelopment(area);
					assert(otherDevel>=4);
					if (otherDevel < devel) {
						printinfo("Player " + other.getName() + " loses Metropolis " + area + " to " + getCurPlayer().getName());
						v.setType(VertexType.CITY);
						onMetropolisStolen(v.getPlayer(), playerNum, area);
						pushStateFront(State.CHOOSE_METROPOLIS, area, null);
					}
				}
			}
		}		
	}

	protected void onProgressCardDistributed(int playerNum, ProgressCardType type) {}
	protected void onSpecialVictoryCard(int playerNum, SpecialVictoryType type) {}
	
	private void distributeProgressCard(DevelopmentArea area) {
		for (Player p : getPlayers()) {
			if (mProgressCards[area.ordinal()].size() > 0 && p.getCardCount(CardType.Progress) < mRules.getMaxProgressCards() && p.getCityDevelopment(area) >= mDice[1]) {
				Card card = mProgressCards[area.ordinal()].remove(0);
				printinfo("Player " + p.getName() + " draws a " + area.name() + " Progress Card");
				if (card.equals(ProgressCardType.Constitution)) {
					p.addCard(SpecialVictoryType.Constitution);
					onSpecialVictoryCard(p.getPlayerNum(), SpecialVictoryType.Constitution);
				} else if (card.equals(ProgressCardType.Printer)) {
					p.addCard(SpecialVictoryType.Printer);
					onSpecialVictoryCard(p.getPlayerNum(), SpecialVictoryType.Printer);
				} else {
    				p.addCard(card);
    				onProgressCardDistributed(p.getPlayerNum(), ProgressCardType.values()[card.getTypeOrdinal()]);
				}
			}
		}
	}

	/**
	 * Called when the barbarians advance toward catan
	 * @param distanceAway
	 */
	protected void onBarbariansAdvanced(int distanceAway) {}
	
	/**
	 * Called when barbarians start their attack
	 * @param barbarianStrength
	 * @param catanStrength
	 */
	protected void onBarbariansAttack(int barbarianStrength, int catanStrength) {}
	
	/**
	 * Called when city is defended by a wall, but the wall is destroyed
	 * @param playerNum
	 * @param vertexIndex
	 */
	protected void onCityWallDestroyed(int playerNum, int vertexIndex) {}
	
	/**
	 * Called when city is reduced to a settlement
	 * @param playerNum
	 * @param vertexIndex
	 */
	protected void onCityDestroyed(int playerNum, int vertexIndex) {}
	
	private void processBarbarianShip() {
		mBarbarianDistance -= 1;
		if (mBarbarianDistance <= 0) {
			printinfo("The Barbarians are attacking!");
			int [] playerStrength = new int[mNumPlayers+1];
			int minStrength = Integer.MAX_VALUE;
			int maxStrength = 0;
			for (int i=1; i<=getNumPlayers(); i++) {
				playerStrength[i] = mBoard.getNumVertsOfType(i, VertexType.BASIC_KNIGHT_ACTIVE) * VertexType.BASIC_KNIGHT_ACTIVE.getKnightLevel()
								  + mBoard.getNumVertsOfType(i, VertexType.STRONG_KNIGHT_ACTIVE) * VertexType.STRONG_KNIGHT_ACTIVE.getKnightLevel()
								  + mBoard.getNumVertsOfType(i, VertexType.MIGHTY_KNIGHT_ACTIVE) * VertexType.MIGHTY_KNIGHT_ACTIVE.getKnightLevel();
				minStrength = Math.min(minStrength, playerStrength[i]);
				maxStrength = Math.max(maxStrength, playerStrength[i]);
			}

			int catanStrength = CMath.sum(playerStrength);
			int barbarianStrength = computeBarbarianStrength(this, mBoard);
			
			if (catanStrength >= barbarianStrength) {
				// find defender
				printinfo("Catan defended itself from the Barbarians!");
				List<Integer> defenders = new ArrayList<Integer>();
				for (int i=1; i<playerStrength.length; i++) {
					if (playerStrength[i] == maxStrength) {
						defenders.add(i);
					}
				}
				assert(defenders.size() > 0);
				if (defenders.size() == 1) {
					Player defender = getPlayerByPlayerNum(defenders.get(0));
					defender.addSpecialVictoryCard(SpecialVictoryType.DefenderOfCatan);
					printinfo("Player " + defender.getName() + " receives the Defender of Catan card!");
				} else {
					pushStateFront(State.SET_PLAYER, getCurPlayerNum(), null);
					for (int playerNum : defenders) {
						if (getPlayerByPlayerNum(playerNum).getCardCount(CardType.Progress) < mRules.getMaxProgressCards()) {
    						pushStateFront(State.CHOOSE_PROGRESS_CARD_TYPE);
    						pushStateFront(State.SET_PLAYER, playerNum, null);
						}
					}
				}
					
			} else {
				
				printinfo("Catan failed to defend against the Barbarians!");
				List<Integer> pilledged = new ArrayList<Integer>();
				for (int i=1; i<playerStrength.length; i++) {
					if (playerStrength[i] == minStrength) {
						pilledged.add(i);
					}
				}
				assert(pilledged.size() > 0);
				for (int playerNum : pilledged) {
					List<Integer> cities = mBoard.getVertsOfType(playerNum, VertexType.WALLED_CITY);
					if (cities.size() > 0) {
						printinfo("Player " + getPlayerByPlayerNum(playerNum).getName() + " has defended their city with a wall");
						int cityIndex = cities.get(0);
						onCityWallDestroyed(playerNum, cityIndex);
						Vertex v = mBoard.getVertex(cityIndex);
						v.setType(VertexType.CITY);
					} else {
						cities = mBoard.getVertsOfType(playerNum, VertexType.CITY);
						if (cities.size() > 0) {
							printinfo("Player " + getPlayerByPlayerNum(playerNum).getName() + " has their city pilledged");
							int cityIndex = cities.get(0);
							onCityDestroyed(playerNum, cityIndex);
							Vertex v = mBoard.getVertex(cityIndex);
							v.setType(VertexType.SETTLEMENT);
						}
					}
				}
				
			}
			mBarbarianDistance = mRules.getBarbarianStepsToAttack();
		} else {
			onBarbariansAdvanced(mBarbarianDistance);
		}
	}

	/**
	 * Called when a player takes some resource from another player.  can be called multiple times
	 * in a turn.  default method does nothing.
	 * @param taker
	 * @param giver
	 * @param type
	 * @param amount
	 */
	protected void onMonopolyCardApplied(Player taker, Player giver, ICardType applied, int amount) {}
	
	private void processMonopoly(ICardType type) {
		// take the specified resource from all other players
		for (int i=1; i<mNumPlayers; i++) {
			Player cur = mPlayers[(mCurrentPlayer+i) % mNumPlayers];
			int num = cur.getCardCount(type);
			if (num > 0) {
				if (mRules.isEnableCitiesAndKnightsExpansion() && num > 2) {
					num = 2;
				}
				printinfo("Player " + getCurPlayer().getName() + " takes " + num + " " + type + " card from player " + cur.getName());
				onMonopolyCardApplied(getCurPlayer(), cur, type, num);
				cur.incrementResource(type, -num);
				getCurPlayer().incrementResource(type, num);
			}
		}
	}
	
	/**
	 * Called when game over is detected
	 */
	protected void onGameOver(Player winner) {
	    
	}

    public final boolean save(String fileName) {
    	try {
    		saveToFile(new File(fileName));
    		return true;
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	return false;
    }
    
    public final boolean load(String fileName) {
    	reset();
    	File file = new File(fileName);
    	if (file.exists()) {
    		try {
    			loadFromFile(file);
    			return true;
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
    	}
    	return false;
    }

	public final boolean isRoadBlockEnabled() {
		return mRules.isEnableRoadBlock();
	}

	public final int getPointsLargestArmy() {
		return mRules.getPointsLargestArmy();
	}
	
	public final int getPointsLongestRoad() {
		return mRules.getPointsLongestRoad();
	}
	
	public int getBarbarianDistance() {
		return this.mBarbarianDistance;
	}

	public int getMinHandCardsForRobberDiscard(int playerNum) {
		return mRules.getMinHandSizeForGiveup() + 2 * mBoard.getNumVertsOfType(playerNum, VertexType.WALLED_CITY);
	}
	
	public String getHelpText() {
		if (mStateStack.peek() == null) {
			return "Game not running";
		}
		
		return mStateStack.peek().state.helpText;
	}
}
