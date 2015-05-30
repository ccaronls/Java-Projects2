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
	public static final int NUM_DEVELOPMENT_AREA_TYPES	= DevelopmentArea.values().length;
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
    	omitField(SOC.class, "mOptions");
    }
    
	private final Player[]   	mPlayers = new Player[MAX_PLAYERS];
	private int					mCurrentPlayer;
	private int					mNumPlayers;
	private int []				mDice;
	private Stack<StackItem>	mStateStack = new Stack<StackItem>();
	private List<Card>			mDevelopmentCards = new ArrayList<Card>();
	private List<Card>[]		mProgressCards;
	private final List<EventCard> mEventCards = new ArrayList<EventCard>();
	private Board				mBoard;
	private Rules				mRules;
	private int					mBarbarianDistance = -1; // CAK
	private int []				mMetropolisPlayer = new int[NUM_DEVELOPMENT_AREA_TYPES];
	
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
	
	private Player getSpecialVictoryPlayer(SpecialVictoryType card) {
		Player player = null;
		for (Player p : getPlayers()) {
			int num = p.getCardCount(card);
			assert(num == 0 || num == 1);
			if (num > 0) {
				assert(player == null);
				player = p;
			}
		}
		return player;
	}
	
	/**
	 * Get the playernum with the longest road
	 * @return
	 */
	public Player getLongestRoadPlayer() {
		return getSpecialVictoryPlayer(SpecialVictoryType.LongestRoad);
	}

	/**
	 * Get the playernum with the largest ary
	 * @return
	 */
	public Player getLargestArmyPlayer() {
		return getSpecialVictoryPlayer(SpecialVictoryType.LargestArmy);
	}
	
	/**
	 * 
	 * @return
	 */
	public Player getHarborMaster() {
		return getSpecialVictoryPlayer(SpecialVictoryType.HarborMaster);
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
		assert(mCurrentPlayer >= 0);
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
	 * Get the dice.  
	 * @return
	 */
	public int [] getDice() {
		return mDice;
	}

	/**
	 * Get sum of both 6 sided die 
	 * @return
	 */
	public int getProductionNum() {
		EventCard card = getTopEventCard();
		if (card != null) {
			return card.getProduction();
		}
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
	 * Return player num who has existing metropolis or 0 if not owned
	 * @param area
	 * @return
	 */
	public int getMetropolisPlayer(DevelopmentArea area) {
		return mMetropolisPlayer[area.ordinal()];
	}
	
	/**
	 * 
	 * @return
	 */
	public Tile getRobberCell() {
		return getBoard().getTile(getBoard().getRobberTileIndex());
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
		for (int i=0; i<getNumPlayers(); i++) {
			mPlayers[i].reset();
		}
		mBoard.reset();
		mStateStack.clear();
		mEventCards.clear();
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
		if (getRules().isEnableCitiesAndKnightsExpansion()) {
			mProgressCards = new List[NUM_DEVELOPMENT_AREA_TYPES];
			for (int i=0; i<NUM_DEVELOPMENT_AREA_TYPES; i++)
				mProgressCards[i] = new ArrayList<Card>();
			for (ProgressCardType p :  ProgressCardType.values()) {
				if (getRules().isEnableEventCards() && p == ProgressCardType.Alchemist)
					continue; // dont add alchemist cards for this kind of expansion
				for (int i=0; i<p.deckOccurances; i++) {
					mProgressCards[p.type.ordinal()].add(new Card(p, CardStatus.USABLE));
				}
			}
			for (int i=0; i<mProgressCards.length; i++)
				Utils.shuffle(mProgressCards[i]);
		} else {
			for (DevelopmentCardType d : DevelopmentCardType.values()) {
				switch (d) {
					case Monopoly:
					case RoadBuilding:
					case YearOfPlenty:
					case Victory: {
						for (int i=0; i<d.deckOccurances; i++)
							mDevelopmentCards.add(new Card(d, CardStatus.USABLE));
						break;
					}
					case Soldier: {
						if (getRules().isEnableRobber()) {
							for (int i=0; i<d.deckOccurances; i++)
								mDevelopmentCards.add(new Card(d, CardStatus.USABLE));
						}
						break;
					}
					case Warship: {
						if (isPirateAttacksEnabled()) {
							for (int i=0; i<d.deckOccurances; i++)
								mDevelopmentCards.add(new Card(d, CardStatus.USABLE));
						}
						break;
					}
					default:
						throw new AssertionError("Unhandled case " + d);
				}
			}
			Utils.shuffle(mDevelopmentCards);
		}
		
	}
	
	private void initEventCards() {
		mEventCards.clear();
		for (EventCardType e : EventCardType.values()) {
			for (int p : e.production) {
				mEventCards.add(new EventCard(e, p));
			}
		}
		Utils.shuffle(mEventCards);
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
	
	public final EventCard getTopEventCard() {
		if (mEventCards.size() > 0) {
			return mEventCards.get(0);
		}
		return null;
	}
	
	private void processEventCard() {
		EventCard next = getTopEventCard();
		switch (next.getType()) {
			case CalmSea: {
				// player with most harbors get to pick a resource card
				int most = 0;
				Player withMost = null;
				for (Player p : getPlayers()) {
					int num = 0;
					boolean [] used = new boolean[getBoard().getNumTiles()]; 
					for (int vIndex : mBoard.getStructuresForPlayer(p.getPlayerNum())) {
						Vertex v = getBoard().getVertex(vIndex);
						for (int i=0; i<v.getNumTiles(); i++) {
							int tIndex = v.getTile(i);
							if (used[tIndex])
								continue;
							used[tIndex] = true;
							Tile t = getBoard().getTile(tIndex);
							switch (t.getType()) {
								case PORT_BRICK:
								case PORT_MULTI:
								case PORT_ORE:
								case PORT_SHEEP:
								case PORT_WHEAT:
								case PORT_WOOD:
									num++;
									break;
								default:
									break;
								
							}
						}
					}
					if (num > most) {
						most = num;
						withMost = p;
					}
				}
				if (withMost != null) {
					printinfo(withMost.getName() + " has most harbors and gets to pick a resource card");
					pushStateFront(State.SET_PLAYER, getCurPlayerNum(), null);
					pushStateFront(State.DRAW_RESOURCE_NOCANCEL);
					pushStateFront(State.SET_PLAYER, withMost.getPlayerNum(), null);
				}
				break;
			}
			case Conflict: {
				Player LAP = getLargestArmyPlayer();
				if (LAP == null) {
					int [] armySize = new int[getNumPlayers()+1];
					int maxSize = 0;
					for (Player p : getPlayers()) {
						int size = 0;
						if (getRules().isEnableCitiesAndKnightsExpansion()) {
							for (int kIndex : getBoard().getKnightsForPlayer(p.getPlayerNum())) {
								size += getBoard().getVertex(kIndex).getType().getKnightLevel();
							}
						} else {
							size = p.getArmySize();
						}
						armySize[p.getPlayerNum()] = size;
						maxSize = Math.max(size, maxSize);
					}
					
					for (int i=1; i<armySize.length; i++) {
						if (armySize[i] == maxSize) {
							if (LAP == null) 
								LAP = getPlayerByPlayerNum(i);
							else {
								// there is a tie, s no event
								LAP = null;
								break;
							}
						}
					}
				}
				if (LAP != null) {
					printinfo(LAP.getName() + " Has largest army and gets to take a resource card from another");
					pushStateFront(State.SET_PLAYER, getCurPlayerNum(), null);
					pushStateFront(State.CHOOSE_OPPONENT_TO_TAKE_RESOURCE_FROM);
					pushStateFront(State.SET_PLAYER, LAP.getPlayerNum(), null);
					mOptions = computeOpponents(this, LAP.getPlayerNum());
				} else {
					printinfo("No single player with largest army so event cancelled");
				}
				break;
			}
			case Earthquake: {
				for (Player p : getPlayers()) {
					if (p.getCardCount(SpecialVictoryType.DamagedRoad) > 0)
						continue;
					List<Integer> routes = getBoard().getRoadsForPlayer(p.getPlayerNum());
					if (routes.size() > 0) {
						Route r = getBoard().getRoute(Utils.randItem(routes));
						p.addCard(SpecialVictoryType.DamagedRoad);
						r.setDamaged(true);
					}
				}
				break;
			}
			case Epidemic: {
				// cities only produce 1 resource.  handled in distribute resources
				break;
			}
			case GoodNeighbor: {
				// each player gives player to their left a resource or commodity
				pushStateFront(State.SET_PLAYER, getCurPlayerNum(), null);
				for (int i=1; i<=mNumPlayers; i++) {
					int left = i+1;
					if (left > mNumPlayers)
						left=1;
					
					pushStateFront(State.CHOOSE_GIFT_CARD, getPlayerByPlayerNum(left), null);
					pushStateFront(State.SET_PLAYER, i, null);
				}
				break;
			}
			case NeighborlyAssistance: {
				// winning player gives up a resource to another
				Player p = computePlayerWithMostVictoryPoints(this);
				if (p != null) {
					printinfo(p.getName() + " must give a resource card to another player of their choice");
					pushStateFront(State.SET_PLAYER, getCurPlayerNum(), null);
					pushStateFront(State.CHOOSE_OPPONENT_FOR_GIFT_CARD, getCurPlayer().getCards(CardType.Resource), null);
					pushStateFront(State.SET_PLAYER, p.getPlayerNum(), null);
					mOptions = computeOpponents(this, p.getPlayerNum());
				}
				break;
			}
			case NoEvent:
				break;
			case PlentifulYear: {
				// each player draws a resource from pile
				for (int i=0; i<getNumPlayers(); i++) {
					pushStateFront(State.NEXT_PLAYER);
					pushStateFront(State.DRAW_RESOURCE_NOCANCEL);
				}
				break;
			}
			case RobberAttack: {
				// same as rolling a seven.  handled by processDice
				break;
			}
			case RobberFlees: {
				List<Integer> tiles = getBoard().getTilesOfType(TileType.DESERT);
				if (tiles.size() > 0) {
					getBoard().setRobber(tiles.get(0));
				} else {
					getBoard().setRobber(-1);
				}
				break;
			}
			case Tournament: { // TODO: verify non-cak rules
				int [] numKnights = new int[getNumPlayers()+1];
				int most = 0;
				for (int i=1; i<=getNumPlayers(); i++) {
					int num = 0;
					if (getRules().isEnableCitiesAndKnightsExpansion())
						num = getBoard().getNumKnightsForPlayer(i);
					else
						num = getPlayerByPlayerNum(i).getArmySize();
					numKnights[i] = num;
					if (num > most) {
						most = num;
					}
				}
				if (most > 0) {
					pushStateFront(State.SET_PLAYER, getCurPlayerNum(), null);
					for (int i=1; i<numKnights.length; i++) {
						if (numKnights[i] == most) {
    						pushStateFront(State.DRAW_RESOURCE_NOCANCEL);
    						pushStateFront(State.SET_PLAYER, i, null);
						}
					}
				}
				break;
			}
			case TradeAdvantage: {
				Player LRP = getLongestRoadPlayer();
				if (LRP == null) {
					int [] roadLength = new int[getNumPlayers()+1];
					int maxSize = 0;
					for (Player p : getPlayers()) {
						int len = roadLength[p.getPlayerNum()] = p.getRoadLength();
						maxSize = Math.max(len, maxSize);
					}
					
					for (int i=1; i<roadLength.length; i++) {
						if (roadLength[i] == maxSize) {
							if (LRP == null) 
								LRP = getPlayerByPlayerNum(i);
							else {
								// there is a tie, s no event
								printinfo("No event when 2 or more players have the same size army");
								LRP = null;
								break;
							}
						}
					}
				}
				if (LRP != null) {
					printinfo(LRP.getName() + " has longest road and gets to take a card form another");
					pushStateFront(State.SET_PLAYER, getCurPlayerNum(), null);
					pushStateFront(State.CHOOSE_OPPONENT_TO_TAKE_RESOURCE_FROM);
					pushStateFront(State.SET_PLAYER, LRP.getPlayerNum(), null);
					mOptions = computeOpponents(this, LRP.getPlayerNum());
				} else {
					printinfo("No single player with the longest road to event cancelled");
				}
				break;
			}
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

		boolean epidemic = false;
		if (getTopEventCard() != null) {
			epidemic = getTopEventCard().getType() == EventCardType.Epidemic;
		}

		// visit all the cells with dice as their num
		for (int i = 0; i < mBoard.getNumTiles(); i++) {

			Tile cell = mBoard.getTile(i);
			if (!cell.isDistributionTile())
				continue;
			assert(cell.getDieNum() != 0);
			if (mBoard.getRobberTileIndex() == i)
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
					if (cell.getType() == TileType.GOLD) {
						// set to original player
						pushStateFront(State.SET_PLAYER, getCurPlayerNum(), null);
						if (getRules().isEnableCitiesAndKnightsExpansion()) {
							pushStateFront(State.DRAW_RESOURCE_OR_COMMODITY_NOCANCEL);
						} else {
							pushStateFront(State.DRAW_RESOURCE_NOCANCEL);
						}
						// set to player that needs choose a resource
						pushStateFront(State.SET_PLAYER, vertex.getPlayer(), null);
					} else if (getRules().isEnableCitiesAndKnightsExpansion()) {
						
						if (vertex.isCity()) {
							
							if (epidemic) {
								resourceInfo[cell.getResource().ordinal()][vertex.getPlayer()] += 1;
								p.incrementResource(cell.getResource(), 1);
							} else {
								if (cell.getCommodity() == null) {
									resourceInfo[cell.getResource().ordinal()][vertex.getPlayer()] += 2;
									p.incrementResource(cell.getResource(), 2);	
								} else {
									resourceInfo[cell.getResource().ordinal()][vertex.getPlayer()] += 1;
    								p.incrementResource(cell.getResource(), 1);
    								commodityInfo[cell.getCommodity().ordinal()][vertex.getPlayer()] += 1;
    								p.incrementResource(cell.getCommodity(), 1);
								}
							}
						} else if (vertex.isStructure()) {
    						resourceInfo[cell.getResource().ordinal()][vertex.getPlayer()] += 1;
    						p.incrementResource(cell.getResource(), 1);
						}
					} else {
						int num = getRules().getNumResourcesForSettlement();
						if (!epidemic && vertex.isCity()) 
							num = getRules().getNumResourcesForCity();
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
				printinfo(p.getName() + " gets " + msg);
			} else if (p.hasAqueduct()) {
				printinfo(p.getName() + " applying Aqueduct ability");
				pushStateFront(State.SET_PLAYER, getCurPlayerNum(), null);
				pushStateFront(State.DRAW_RESOURCE_NOCANCEL);
				pushStateFront(State.SET_PLAYER, p.getPlayerNum(), null);
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
        System.out.println(getPlayerByPlayerNum(playerNum).getName() + ": " + txt);
    }
    
	private void printinfo(String txt) {
		printinfo(getCurPlayerNum(), txt);
	}

	static public int computePointsFromCards(Player player, SOC soc) {
		int numPts = player.getSpecialVictoryPoints();
		int victoryPts = player.getUsableCardCount(DevelopmentCardType.Victory);
		if (numPts + victoryPts >= soc.getRules().getPointsForWinGame()) {
			numPts += victoryPts;
		}
		return numPts;
	}
	
	static public int computePointsFromBoard(Player player, Board board, SOC soc) {
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
		for (boolean b: islands) {
			if (b) {
				numPts += soc.getRules().getPointsIslandDiscovery();
			}
		}

		return numPts;
	}

	/**
	 * Compute the point the player should have based on the board and relevant SOC values.
	 * The player's point field is not changed. 
	 * @param player
	 * @return
	 */
	static public int computePointsForPlayer(Player player, Board board, SOC soc) {
		return computePointsFromCards(player, soc) + computePointsFromBoard(player, board, soc);
	}

	/**
	 * Called when a players point change (for better or worse).  default method does nothing.
	 * @param player
	 * @param changeAmount
	 */
	protected void onPlayerPointsChanged(Player player, int changeAmount) {}
	
	private void updatePlayerPoints() {
		for (int i=0; i<mNumPlayers; i++) {
			Player p = mPlayers[i];
			int newPoints = computePointsForPlayer(p, mBoard, this);
		    if (newPoints != p.getPoints()) {
		        this.onPlayerPointsChanged(p, newPoints-p.getPoints());
	            p.setPoints(newPoints);
		    }
		}
	}

	/**
	 * Called immediately after a die roll.  Base method does nothing.
	 * @param dice
	 */
	protected void onDiceRolled(int ... dice) {}
	
	/**
	 * Called immediately after event card dealt.  Base method does nothing.
	 * @param card
	 */
	protected void onEventCardDealt(EventCard card) {}
	
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
		picked.setUnusable();
		getCurPlayer().addCard(picked);
		printinfo(getCurPlayer().getName() + " picked a " + picked + " card");
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
		printinfo(taker.getName() + " taking a " + taken.getName() + " card from Player " + giver.getName());
		onTakeOpponentCard(taker, giver, taken);
	}
    
    public void initGame() {
        // setup
        assert (mNumPlayers > 1);
        assert (mPlayers != null);
        assert (mBoard != null);
        
        if (getRules().isEnableCitiesAndKnightsExpansion()) {
        	mDice = new int[3];
        	mBarbarianDistance = getRules().getBarbarianStepsToAttack();
        } else {
        	mDice = new int[2];
        	mBarbarianDistance = -1;
        }

        reset();
        initDeck();

        if (isPirateAttacksEnabled()) {
        	getBoard().setPirate(getBoard().getPirateRouteStartTile());
        }
        
        mCurrentPlayer = Utils.randRange(0, getNumPlayers()-1);
        
        pushStateFront(State.START_ROUND);
        pushStateFront(State.DEAL_CARDS);
        
        // first player picks last
        pushStateFront(State.POSITION_ROAD_OR_SHIP_NOCANCEL);
        if (getRules().isEnableCitiesAndKnightsExpansion())
        	pushStateFront(State.POSITION_CITY_NOCANCEL);
        else
        	pushStateFront(State.POSITION_SETTLEMENT_NOCANCEL);

        // player picks in reverse order
        for (int i=mNumPlayers-1; i > 0; i--) {
            pushStateFront(State.PREV_PLAYER);
            pushStateFront(State.POSITION_ROAD_OR_SHIP_NOCANCEL);
            if (getRules().isEnableCitiesAndKnightsExpansion())
            	pushStateFront(State.POSITION_CITY_NOCANCEL);
            else
            	pushStateFront(State.POSITION_SETTLEMENT_NOCANCEL);
        }

        // the last player picks again
        pushStateFront(State.POSITION_ROAD_OR_SHIP_NOCANCEL);
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
            if (player.getPoints() >= getRules().getPointsForWinGame()) {
                onGameOver(player);
                return true;
            }
        }
        return false;
    }
    
    public static List<Card> createCards(ICardType<?> [] values, CardStatus status) {
    	List<Card> cards = new ArrayList<Card>();
    	for (ICardType<?> c : values) {
    		cards.add(new Card(c, status));
    	}
    	return cards;
    }
    
    public static List<Integer> computeHarborTradePlayers(Player trader, SOC soc) {
    	List<Integer> players = new ArrayList<Integer>();
    	int num = trader.getCardCount(CardType.Resource);
    	for (Player p : soc.getPlayers()) {
    		if (num == 0)
    			break;
    		if (p == trader)
    			continue;
    		int numCommodity = p.getCardCount(CardType.Commodity);
    		if (numCommodity > 0) {
    			players.add(p.getPlayerNum());
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

		if (!mBoard.isReady()) {
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
		
		//Vertex knightVertex = null;
		
		if (!runGameCheck())
			return;
		
		try {

			switch (getState()) {

				case DEAL_CARDS: // transition state
					popState();
					distributeResources(0);
					break;

				case POSITION_SETTLEMENT_CANCEL: // wait state
				case POSITION_SETTLEMENT_NOCANCEL: { // wait state
					if (mOptions == null) {
						printinfo(getCurPlayer().getName() + " place settlement");
						mOptions = computeSettlementVertexIndices(this, getCurPlayerNum(), mBoard);
					}

					assert (mOptions != null);
					Vertex v = getCurPlayer().chooseVertex(this, mOptions, VertexChoice.SETTLEMENT);

					if (v != null) {
						int vIndex = mBoard.getVertexIndex(v);
						printinfo(getCurPlayer().getName() + " placed a settlement on vertex " + vIndex);
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
								int len = mBoard.computeMaxRouteLengthForPlayer(ii, getRules().isEnableRoadBlock());
								p.setRoadLength(len);
							}
						}

						updateLongestRoutePlayer();
						checkForDiscoveredIsland(vIndex);
						if (getRules().isEnableHarborMaster()) {
							int newPts = 0;
							for (Tile t : getBoard().getTilesAdjacentToVertex(v)) {
								if (t.isPort()) {
									newPts += 1;
								}
							}
							if (newPts > 0) {
								int pts = getCurPlayer().getHarborPoints() + newPts;
								getCurPlayer().setHarborPoints(pts);
								updateHarborMasterPlayer();
							}
						}
						resetOptions();
						popState();
					}
					break;
				}

				case POSITION_ROAD_OR_SHIP_CANCEL:
				case POSITION_ROAD_OR_SHIP_NOCANCEL: {
					
					if (getRules().isEnableSeafarersExpansion()) {
					
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

				case POSITION_ROAD_NOCANCEL: // wait state
				case POSITION_ROAD_CANCEL: {// wait state

					if (mOptions == null) {
						printinfo(getCurPlayer().getName() + " place road");
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
						printinfo(getCurPlayer().getName() + " placing a road on edge " + eIndex);
						//edge.player = getCurPlayerNum();
						getBoard().setPlayerForRoute(edge, getCurPlayerNum());
						processRouteChange(getCurPlayer(), edge);
//						Player p = getCurPlayer();
//						int len = mBoard.computeMaxRouteLengthForPlayer(p.getPlayerNum(), getRules().isEnableRoadBlock());
//						p.setRoadLength(len);
//						updateLongestRoutePlayer();
//						checkForDiscoveredNewTerritory(edge.getFrom());
//						checkForDiscoveredNewTerritory(edge.getTo());
						resetOptions();
						popState();
					}
					break;
				}

				case POSITION_SHIP_NOCANCEL:
				case POSITION_SHIP_CANCEL: {
					if (mOptions == null) {
						printinfo(getCurPlayer().getName() + " place ship");
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
						printinfo(getCurPlayer().getName() + " placing a ship on edge " + eIndex);
						getBoard().setPlayerForRoute(edge, getCurPlayerNum());
						processRouteChange(getCurPlayer(), edge);
						resetOptions();
						popState();
					}
					break;
				}

				case CHOOSE_SHIP_TO_MOVE:
					if (mOptions == null) {
						printinfo(getCurPlayer().getName() + " choose ship to move");
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
						printinfo(getCurPlayer().getName() + " place city");
						mOptions = computeSettlementVertexIndices(this, getCurPlayerNum(), mBoard);
					}
				case POSITION_CITY_CANCEL: { // wait state
					if (mOptions == null) {
						printinfo(getCurPlayer().getName() + " place city");
						mOptions = computeCityVertxIndices(getCurPlayerNum(), mBoard);
					}

					assert (mOptions != null);
					Vertex v = getCurPlayer().chooseVertex(this, mOptions, VertexChoice.CITY);

					if (v != null) {
						if (!getRules().isEnableCitiesAndKnightsExpansion()) {
							assert (v.getPlayer() == getCurPlayerNum());
						}
						v.setPlayer(getCurPlayerNum());
						int vIndex = mBoard.getVertexIndex(v);
						printinfo(getCurPlayer().getName() + " placing a city at vertex " + vIndex);
						if (getRules().isEnableHarborMaster()) {
							int newPts = 0;
							for (Tile t : getBoard().getTilesAdjacentToVertex(v)) {
								if (t.isPort()) {
									newPts += v.getType() == VertexType.SETTLEMENT ? 1 : 2;
								}
							}
							if (newPts > 0) {
								int pts = getCurPlayer().getHarborPoints() + newPts;
								getCurPlayer().setHarborPoints(pts);
								updateHarborMasterPlayer();
							}
						}
						v.setType(VertexType.CITY);
						resetOptions();
						popState();
					}
					break;
				}
				
				case CHOOSE_CITY_FOR_WALL: {
					if (mOptions == null) {
						printinfo(getCurPlayer().getName() + " choose city to protect with wall");
						mOptions = computeCityWallVertexIndices(getCurPlayerNum(), mBoard);
					}

					assert (mOptions != null);
					Vertex v = getCurPlayer().chooseVertex(this, mOptions, VertexChoice.CITY_WALL);

					if (v != null) {
						assert (v.getPlayer() == getCurPlayerNum());
						int vIndex = mBoard.getVertexIndex(v);
						printinfo(getCurPlayer().getName() + " placing a city at vertex " + vIndex);
						assert (v.getType() == VertexType.CITY);
						v.setType(VertexType.WALLED_CITY);
						resetOptions();
						popState();
					}
					break;

				}

				case CHOOSE_METROPOLIS: {
					if (mOptions == null) {
						printinfo(getCurPlayer().getName() + " choose city to upgrade to Metropolis");
						mOptions = computeMetropolisVertexIndices(getCurPlayerNum(), mBoard);
					}
					
					assert(mOptions != null && mOptions.size() > 0);
					DevelopmentArea area = (DevelopmentArea)getStateData();
					assert(area != null);
					Vertex v = null;
					if (mOptions.size() == 1)
						v = mBoard.getVertex((Integer)mOptions.get(0));
					else
						v = getCurPlayer().chooseVertex(this, mOptions, area.choice);
					if (v != null) {
						assert(v.getPlayer() == getCurPlayerNum());
						assert(v.isCity());
						mMetropolisPlayer[area.ordinal()] = getCurPlayerNum();
						printinfo(getCurPlayer().getName() + " is building a " + area + " Metrololis");
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
					if (getRules().isEnableEventCards()) {
						mOptions = Arrays.asList(MoveType.DEAL_EVENT_CARD);
					} else {
    					if (getCurPlayer().getCardCount(ProgressCardType.Alchemist) > 0) {
    						mOptions = Arrays.asList(MoveType.ALCHEMIST_CARD, MoveType.ROLL_DICE);
    					} else {
    						mOptions = Arrays.asList(MoveType.ROLL_DICE);
    					}
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

				case PLAYER_TURN_NOCANCEL: // wait state
					if (mOptions == null) {
						printinfo(getCurPlayer().getName() + " choose move");
						mOptions = computeMoves(getCurPlayer(), mBoard, this);
					}
					assert (mOptions != null);
					assert (mOptions.size() > 0);
					MoveType move = getCurPlayer().chooseMove(this, mOptions);
					if (move != null) {
						processMove(move);
						//resetOptions();
					}
					break;

				case SHOW_TRADE_OPTIONS: { // wait state
					if (mOptions == null) {
						printinfo(getCurPlayer().getName() + " select trade option");
						mOptions = computeTrades(getCurPlayer(), mBoard);
					}
					assert (mOptions != null);
					final Trade trade = getCurPlayer().chooseTradeOption(this, mOptions);
					if (trade != null) {
						printinfo(getCurPlayer().getName() + " trades " + trade.getType() + " X " + trade.getAmount());
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
						if (getRules().isEnableCitiesAndKnightsExpansion()) {
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
						printinfo(getCurPlayer().getName() + " place robber or pirate");
						mOptions = computeRobberTileIndices(this, mBoard);
						mOptions.addAll(computePirateTileIndices(this, mBoard));
					}
				case POSITION_ROBBER_CANCEL: // wait state
				case POSITION_ROBBER_NOCANCEL: { // wait state
					if (mOptions == null) {
						printinfo(getCurPlayer().getName() + " place robber");
						mOptions = computeRobberTileIndices(this, mBoard);
					}
					if (mOptions.size() == 0) {
						mBoard.setRobber(-1);
						mBoard.setPirate(-1);
						popState();
					} else {
    					Tile cell = getCurPlayer().chooseTile(this, mOptions, TileChoice.ROBBER);
    
    					if (cell != null) {
    						popState();
    						int cellIndex = mBoard.getTileIndex(cell);
    						resetOptions();
    						if (cell.isWater()) {
    							printinfo(getCurPlayer().getName() + " placing pirate on cell " + cellIndex);
    							mBoard.setPirate(cellIndex);
    							pushStateFront(State.CHOOSE_OPPONENT_TO_TAKE_RESOURCE_FROM);
    							mOptions = computeRobberTakeOpponentCardOptions(getCurPlayer(), getBoard(), true);
    						} else {
    							printinfo(getCurPlayer().getName() + " placing robber on cell " + cellIndex);
    							mBoard.setRobber(cellIndex);
    							pushStateFront(State.CHOOSE_OPPONENT_TO_TAKE_RESOURCE_FROM);
    							mOptions = computeRobberTakeOpponentCardOptions(getCurPlayer(), getBoard(), false);
    						}
    					}
					}
					break;
				}
				case POSITION_PIRATE_CANCEL: // wait state
				case POSITION_PIRATE_NOCANCEL: { // wait state
					if (mOptions == null) {
						printinfo(getCurPlayer().getName() + " place robber");
						mOptions = computePirateTileIndices(this, mBoard);
					}
					Tile cell = getCurPlayer().chooseTile(this, mOptions, TileChoice.PIRATE);

					if (cell != null) {
						popState();
						int cellIndex = mBoard.getTileIndex(cell);
						resetOptions();
						if (cell.isWater()) {
							printinfo(getCurPlayer().getName() + " placing pirate on cell " + cellIndex);
							mBoard.setPirate(cellIndex);
							pushStateFront(State.CHOOSE_OPPONENT_TO_TAKE_RESOURCE_FROM);
							mOptions = computeRobberTakeOpponentCardOptions(getCurPlayer(), getBoard(), true);
						} else {
							printinfo(getCurPlayer().getName() + " placing robber on cell " + cellIndex);
							mBoard.setRobber(cellIndex);
							pushStateFront(State.CHOOSE_OPPONENT_TO_TAKE_RESOURCE_FROM);
							mOptions = computeRobberTakeOpponentCardOptions(getCurPlayer(), getBoard(), false);
						}
					}
					break;
				}

				case CHOOSE_OPPONENT_TO_TAKE_RESOURCE_FROM: // wait state
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
					
				case CHOOSE_OPPONENT_FOR_GIFT_CARD:
					assert(mOptions != null);
					Player player = getCurPlayer().choosePlayer(this, mOptions, PlayerChoice.PLAYER_TO_GIFT_CARD);
					if (player != null) {
						mOptions = (List<Card>)getStateData(); // must include the cards to gift
						assert(mOptions != null && mOptions.size() > 0);
						popState();
						pushStateFront(State.CHOOSE_GIFT_CARD, player, null);
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
				
				case SETUP_GIVEUP_CARDS: {
					popState();
					pushStateFront(State.NEXT_PLAYER);
					pushStateFront(State.SET_PLAYER, getCurPlayerNum(), null);
					for (int ii=0; ii<mNumPlayers; ii++) {
						Player cur = mPlayers[(ii + mCurrentPlayer) % mNumPlayers];
						int numCards = cur.getTotalCardsLeftInHand();
						if (numCards > getMinHandCardsForRobberDiscard(cur.getPlayerNum())) {
							int numCardsToSurrender = numCards / 2;
							printinfo(cur.getName() + " must give up " + numCardsToSurrender + " of " + numCards + " cards");
							while (numCardsToSurrender > 0)
								pushStateFront(State.GIVE_UP_CARD, numCardsToSurrender--, null);
							pushStateFront(State.SET_PLAYER, cur.getPlayerNum(), null);
						}
					} 
					break;
				}

				case GIVE_UP_CARD: { // wait state
					if (mOptions == null) {
						mOptions = getCurPlayer().getUnusedCards();
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
					// fallthrough
				case DRAW_RESOURCE_NOCANCEL:
				case DRAW_RESOURCE_CANCEL: { // wait state
					if (mOptions == null) {
						mOptions = new ArrayList<Card>();
						for (ResourceType t : ResourceType.values()) {
							mOptions.add(new Card(t, CardStatus.USABLE));
						}
					}
					Card card = getCurPlayer().chooseCard(this, mOptions, CardChoice.RESOURCE_OR_COMMODITY);
					if (card != null) {
						printinfo(getCurPlayer().getName() + " draws a " + card.getName() + " card");
						onCardPicked(getCurPlayer(), card);
						getCurPlayer().addCard(card);
						popState();
						resetOptions();
					}
					break;
				}

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
						v.activateKnight();
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
						v.setType(v.getType().promotedType());
						v.setPromotedKnight(true);
						resetOptions();
						popState();
					}
					break;
				}
				
				case CHOOSE_KNIGHT_TO_MOVE: {
					assert(mOptions != null);
					assert(mOptions.size() > 0);
					final Vertex v = getCurPlayer().chooseVertex(this, mOptions, VertexChoice.KNIGHT_TO_MOVE);
					final VertexType vt = v.getType();
					if (v != null) {
						popState();
						mOptions = computeKnightMoveVertexIndices(mBoard.getVertexIndex(v), mBoard);
						pushStateFront(State.POSITION_KNIGHT_CANCEL, v.getType(), new UndoAction() {
							@Override
							public void undo() {
								v.setPlayer(getCurPlayerNum());
								v.setType(vt);
							}
						});
						v.removePlayer();
					}
					break;
				}
				
				case POSITION_DISPLACED_KNIGHT: { 
					//knightVertex = (Vertex)getStateData();
					VertexType displacedKnight = (VertexType)getStateData();
					assert(mOptions != null && mOptions.size() > 0);
					Vertex v = getCurPlayer().chooseVertex(this, mOptions, VertexChoice.KNIGHT_DISPLACED);
					if (v != null) {
						assert(v.getType() == VertexType.OPEN);
						v.setType(displacedKnight);
						resetOptions();
					}
					break;
				}

				case POSITION_KNIGHT_CANCEL: {
					assert(mOptions != null && mOptions.size() > 0);
					VertexType knight = (VertexType)getStateData();
					assert(knight.isKnight());
					Vertex v = getCurPlayer().chooseVertex(this, mOptions, VertexChoice.KNIGHT_MOVE_POSITION);
					if (v != null) {
						popState();
						resetOptions();
						if (v.getPlayer() != 0) {
							assert(v.isKnight());
							assert(v.getType().getKnightLevel() < knight.getKnightLevel());
							mOptions = computeDisplacedKnightVertexIndices(mBoard.getVertexIndex(v), mBoard);
							if (mOptions.size() > 0) {
    							pushStateFront(State.SET_PLAYER, getCurPlayerNum(), null);
    							pushStateFront(State.POSITION_DISPLACED_KNIGHT, v.getType(), null);
    							pushStateFront(State.SET_PLAYER, v.getPlayer(), null);
							}
						} else {
							assert(v.getType() == VertexType.OPEN);
						}
					
    					v.setPlayer(getCurPlayerNum());
    					v.setType(knight);
    					if (v.isActiveKnight())
    						v.deactivateKnight();

    					// see if we are chasing away the robber
    					for (int i=0; i<v.getNumTiles(); i++) {
    						int tIndex = v.getTile(i);
    						if (tIndex == mBoard.getRobberTileIndex()) {
    							printinfo(getCurPlayer().getName() + " has chased away the robber!");
    							pushStateFront(State.POSITION_ROBBER_NOCANCEL);
    						} else if (tIndex == mBoard.getPirateTileIndex()) {
    							printinfo(getCurPlayer().getName() + " has chased away the pirate!");
    							pushStateFront(State.POSITION_PIRATE_NOCANCEL);
    						}
    						
    					}
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
						List<Integer> knights = getBoard().getKnightsForPlayer(p.getPlayerNum());
						if (knights.size() > 0) {
							putCardBackInDeck(getCurPlayer().removeCard(ProgressCardType.Deserter));
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
						resetOptions();
						final Card card = getCurPlayer().removeCard(ProgressCardType.Diplomat);
						putCardBackInDeck(card);
						if (r.getPlayer() == getCurPlayerNum()) {
							mBoard.setPlayerForRoute(r, 0);
							mOptions = computeRoadRouteIndices(getCurPlayerNum(), mBoard);
							mOptions.remove((Object)mBoard.getRouteIndex(r));
							pushStateFront(State.POSITION_ROAD_CANCEL, null, new UndoAction() {
								@Override
								public void undo() {
									mBoard.setPlayerForRoute(r, getCurPlayerNum());
									processRouteChange(getCurPlayer(), r);
									removeCardFromDeck(card);
									getCurPlayer().addCard(card);
								}
							});
						} else {
							int playerNum = r.getPlayer();
							mBoard.setPlayerForRoute(r, 0);
							processRouteChange(getPlayerByPlayerNum(playerNum), r);
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
						mOptions.remove((Object)vIndex);
						putCardBackInDeck(getCurPlayer().removeCard(ProgressCardType.Intrigue));
						popState();
						if (mOptions.size() > 0) {
    						pushStateFront(State.SET_PLAYER, getCurPlayerNum(), null);
    						pushStateFront(State.POSITION_DISPLACED_KNIGHT, v.getType(), null);
    						pushStateFront(State.SET_PLAYER, v.getPlayer(), null);
						} else {
							v.removePlayer();
							resetOptions();
						}
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
							mOptions.remove((Object)mBoard.getTileIndex(tile));
							pushStateFront(State.CHOOSE_TILE_INVENTOR, tile, null);
						} else {
							// swap em
							int t = firstTile.getDieNum();
							firstTile.setDieNum(tile.getDieNum());
							tile.setDieNum(t);
							onTilesInvented(getCurPlayer(), firstTile, tile);
							putCardBackInDeck(getCurPlayer().removeCard(ProgressCardType.Inventor));
							resetOptions();
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
						resetOptions();
					}
					break;
				}
				
				case CHOOSE_RESOURCE_FLEET: {
					assert(mOptions.size() > 0);
					Card c = getCurPlayer().chooseCard(this, mOptions, CardChoice.FLEET_TRADE);
					if (c != null) {
						getCurPlayer().getCard(ProgressCardType.MerchantFleet).setUsed();
						getCurPlayer().setMerchantFleetTradable(c);
						popState();
					}
					break;
				}
				
				case CHOOSE_PLAYER_TO_SPY_ON: {
					assert(mOptions.size() > 0);
					Player p = getCurPlayer().choosePlayer(this, mOptions, PlayerChoice.PLAYER_TO_SPY_ON);
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
						resetOptions();
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
					if (mOptions == null) {
						mOptions = computeGiftCards(this, getCurPlayer());
					}
					Card c = getCurPlayer().chooseCard(this, mOptions, CardChoice.GIVEUP_CARD);
					if (c != null) {
						Player taker = (Player)getStateData();
						getCurPlayer().removeCard(c);
						taker.addCard(c);
						printinfo(getCurPlayer().getName() + " gives a " + c.getName() + " card to " + taker.getName());
						onTakeOpponentCard(taker, getCurPlayer(), c);
						popState();
						resetOptions();
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

	private void processRouteChange(Player p, Route edge) {
		int len = mBoard.computeMaxRouteLengthForPlayer(p.getPlayerNum(), getRules().isEnableRoadBlock());
		p.setRoadLength(len);
		updateLongestRoutePlayer();
		if (edge.getPlayer() > 0) {
    		checkForDiscoveredNewTerritory(edge.getFrom());
    		checkForDiscoveredNewTerritory(edge.getTo());
		}		
	}
	
	/**
	 * Not recommended to use as this function modifies player and this data.
	 * call runGame until returns true to process
	 * @param move
	 */
	private final void processMove(MoveType move) {
	    
        printinfo(getCurPlayer().getName() + " choose move " + move);
        resetOptions();
        switch (move) {
        	case ROLL_DICE:
        		rollDice();
        		processDice();
        		break;
        		
        	case DEAL_EVENT_CARD:
        		if (mEventCards.size() > 1) {
        			mEventCards.remove(0);
        		} else {
        			initEventCards();
        		}
        		onEventCardDealt(getTopEventCard());
        		if (getRules().isEnableCitiesAndKnightsExpansion()) {
        			mDice[1] = Utils.rand() % 6 + 1;
        			mDice[2] = Utils.rand() % 6 + 1;
        			onDiceRolled(mDice);
        		}
        		processDice();
        		processEventCard();
        		break;
        		
        	case REPAIR_ROAD: {
        		getCurPlayer().adjustResourcesForBuildable(BuildableType.Road, -1);
        		getCurPlayer().removeCard(SpecialVictoryType.DamagedRoad);
        		for (Route r : getBoard().getRoutesForPlayer(getCurPlayerNum())) {
        			if (r.isDamaged()) {
        				r.setDamaged(false);
        				break;
        			}
        		}
        		break;
        	}
        		
        	case BUILD_ROAD:
        		getCurPlayer().adjustResourcesForBuildable(BuildableType.Road, -1);
        		pushStateFront(State.POSITION_ROAD_CANCEL, null, new UndoAction() {
        			public void undo() {
        				getCurPlayer().adjustResourcesForBuildable(BuildableType.Road, 1);
        			}
        		});
        		resetOptions();
        		break;

        	case BUILD_SHIP:
        		getCurPlayer().adjustResourcesForBuildable(BuildableType.Ship, -1);
        		pushStateFront(State.POSITION_SHIP_CANCEL, null, new UndoAction() {
        			public void undo() {
        				getCurPlayer().adjustResourcesForBuildable(BuildableType.Ship, 1);
        			}
        		});
        		resetOptions();
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
        		resetOptions();
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
        		resetOptions();
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
        		resetOptions();
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
        		resetOptions();
        		break;
        	}

        	case BISHOP_CARD: {
        		final Card removed = getCurPlayer().removeCard(ProgressCardType.Bishop);
        		putCardBackInDeck(removed);
        		pushStateFront(getRules().isEnableSeafarersExpansion() ? State.POSITION_ROBBER_OR_PIRATE_CANCEL : State.POSITION_ROBBER_CANCEL, null, new UndoAction() {
					
					@Override
					public void undo() {
						removeCardFromDeck(removed);
						getCurPlayer().addCard(removed);
					}
				});
        		resetOptions();
        		break;
        	}
        	
        	case WARSHIP_CARD: {
        		final Card used = getCurPlayer().getUsableCard(DevelopmentCardType.Soldier);
    			// set a random of the players route to a warship
    			List<Integer> ships = getBoard().getShipsForPlayer(getCurPlayerNum(), false);
    			if (ships.size() > 0) { // TODO: Allow player to choose ship to upgrade
    				used.setUsed();
    				Route r = getBoard().getRoute(ships.get(0));
    				r.setWarShip(true);
    				onPlayerShipUpgraded(getCurPlayer(), r);
    			}
        		resetOptions();
        		break;
        	}
        	
        	case SOLDIER_CARD: {
        		//final Card removed = getCurPlayer().removeCard(DevelopmentCardType.Soldier);
        		final Card used = getCurPlayer().getUsableCard(DevelopmentCardType.Soldier);
        		used.setUsed();
        		updateLargestArmyPlayer();
        		getCurPlayer().setCardsUsable(CardType.Development, false);
        		pushStateFront(getRules().isEnableSeafarersExpansion() ? State.POSITION_ROBBER_OR_PIRATE_CANCEL : State.POSITION_ROBBER_CANCEL, null, new UndoAction() {
        			public void undo() {
        				used.setUsable();
        				updateLargestArmyPlayer();
        				getCurPlayer().setCardsUsable(CardType.Development, true);
        			}
        		});
        		resetOptions();
        		break;
        	}

        	case ALCHEMIST_CARD: {
        		if (getCurPlayer().setDice(mDice, 2)) {
        			putCardBackInDeck(getCurPlayer().removeCard(ProgressCardType.Alchemist));
        			mDice[0] = Utils.clamp(mDice[0], 1, 6);
        			mDice[1] = Utils.clamp(mDice[1], 1, 6);
        			mDice[2] = Utils.rand() % 6 + 1;
        			onDiceRolled(0, 0, mDice[2]);
        			printinfo(getCurPlayer().getName() + " applied Alchemist card on dice " +  mDice[0] + ", " + mDice[1] + ", " + DiceEvent.fromDieNum(mDice[2]));
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
        		mOptions = computeNewKnightVertexIndices(getCurPlayerNum(), mBoard);
        		if (mOptions.size() > 0) {
            		getCurPlayer().adjustResourcesForBuildable(BuildableType.Knight, -1);
            		pushStateFront(State.POSITION_KNIGHT_CANCEL, VertexType.BASIC_KNIGHT_INACTIVE, new UndoAction() {
    					@Override
    					public void undo() {
    						getCurPlayer().adjustResourcesForBuildable(BuildableType.Knight, 1);
    					}
    				});
        		} else {
        			resetOptions();
        		}
        		break;
        	}
        	case MOVE_KNIGHT: 
        		mOptions = computeMovableKnightVertexIndices(getCurPlayerNum(), mBoard);
        		if (mOptions.size() > 0) {
        			pushStateFront(State.CHOOSE_KNIGHT_TO_MOVE);
        		} else {
        			resetOptions();
        		}
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
        		processCityImprovement(getCurPlayer(), DevelopmentArea.Politics);
        		break;
        	}
        	
        	case IMPROVE_CITY_SCIENCE:{
        		processCityImprovement(getCurPlayer(), DevelopmentArea.Science);
        		break;
        	}
        	
        	case IMPROVE_CITY_TRADE:{
        		processCityImprovement(getCurPlayer(), DevelopmentArea.Trade);
        		break;
        	}

        	case CONTINUE:
        		popState();
        		break;
        		
			case CRANE_CARD: {
				// build a city improvement for 1 commodity less than normal
				List<MoveType> options = computeCraneCardImprovements(getCurPlayer());
				if (options.size() > 0) {
    				mOptions = options;
    				final Card card = getCurPlayer().removeCard(ProgressCardType.Crane);
    				putCardBackInDeck(card);
    				pushStateFront(State.CHOOSE_CITY_IMPROVEMENT, null, new UndoAction() {
						
						@Override
						public void undo() {
							removeCardFromDeck(card);
							getCurPlayer().addCard(card);
						}
					});
				}
				break;
			}
				
			case DESERTER_CARD:{
				// replace an opponents knight with one of your own
				List<Integer> players = computeDeserterPlayers(this, mBoard, getCurPlayer());
				if (players.size() > 0) {
					mOptions = players;
					pushStateFront(State.CHOOSE_PLAYER_FOR_DESERTION);
				}
				break;
			}
			case DIPLOMAT_CARD: {
				List<Integer> allOpenRoutes = computeDiplomatOpenRouteIndices(this, mBoard);
				if (allOpenRoutes.size() > 0) {
					mOptions = allOpenRoutes;
					pushStateFront(State.CHOOSE_DIPLOMAT_ROUTE);
				}
				break;
			}
			case ENGINEER_CARD: {
				// build a city wall for free
				List<Integer> cities = computeCityWallVertexIndices(getCurPlayerNum(), mBoard);
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
				List<Integer> players = computeHarborTradePlayers(getCurPlayer(), this);
				if (players.size() > 0) {
					mOptions = players;
					putCardBackInDeck(getCurPlayer().removeCard(ProgressCardType.Harbor));
    				for (int num  : computeHarborTradePlayers(getCurPlayer(), this)) {
    					pushStateFront(State.CHOOSE_HARBOR_RESOURCE, getPlayerByPlayerNum(num), null);
    				}
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
				mOptions = computeInventorTileIndices(mBoard);
				pushStateFront(State.CHOOSE_TILE_INVENTOR);
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
				List<Integer> players = computeMasterMerchantPlayers(this, getCurPlayer());
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
				List<Card> tradableCards = computeMerchantFleetCards(getCurPlayer());
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
					getCurPlayer().incrementResource(ResourceType.Ore, numGained);
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
				List<Integer> sabotagePlayers = computeSaboteurPlayers(this, getCurPlayerNum());
				boolean done = false;
				pushStateFront(State.SET_PLAYER, getCurPlayerNum(), null);
				for (int pNum : sabotagePlayers) {
					Player p = getPlayerByPlayerNum(pNum);
					int num = p.getUnusedCardCount() / 2;
					if (num > 0) {
						done = true;
    					for (int i=0; i<num; i++) {
    						pushStateFront(State.GIVE_UP_CARD);
    					}
    					pushStateFront(State.SET_PLAYER, pNum, null);
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
					if (knights.size()>1) {
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
				List<Integer> players = computeSpyOpponents(this, getCurPlayerNum());
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
				List<Integer> knights = computeWarlordVertices(mBoard, getCurPlayerNum());
				if (knights.size() > 0) {
    				for (int vIndex : knights) {
    					mBoard.getVertex(vIndex).activateKnight();
    				}
    				putCardBackInDeck(getCurPlayer().removeCard(ProgressCardType.Warlord));
				}
				break;
			}
			case WEDDING_CARD: {
				pushStateFront(State.SET_PLAYER, getCurPlayerNum(), null);
				List<Integer> opponents = computeWeddingOpponents(this, getCurPlayer());
				if (opponents.size() > 0) {
					putCardBackInDeck(getCurPlayer().removeCard(ProgressCardType.Wedding));
    				for (int num : opponents) {
    					Player p = getPlayerByPlayerNum(num);
    					// automatically give
    					List<Card> cards = p.getCards(CardType.Commodity);
    					cards.addAll(p.getCards(CardType.Resource));
    					if (cards.size() <= 2) {
    						// automatic
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
				break;
			}
		}
	}
	
	public static List<Integer> computeSpyOpponents(SOC soc, int playerNum) {
		List<Integer> players = new ArrayList<Integer>();
		for (Player p : soc.getPlayers()) {
			if (p.getPlayerNum() != playerNum) {
				if (p.getUnusedCardCount(CardType.Progress) > 0) {
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
	
	public static List<Integer> computeMasterMerchantPlayers(SOC soc, Player player) {
		List<Integer> players = new ArrayList<Integer>();
		for (Player p : soc.getPlayers()) {
			if (p.getPlayerNum() == player.getPlayerNum())
				continue;
			if (p.getPoints() > player.getPoints()) {
				players.add(p.getPlayerNum());
			}
		}
		return players;
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
			default:
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
			default:
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
	protected void onPlayerDiscoveredIsland(Player player, Island island) {}
	
	/**
	 * 
	 * @param p
	 * @param r
	 */
	protected void onPlayerShipUpgraded(Player p, Route r) {}
	
	private void givePlayerSpecialVictoryCard(Player player, SpecialVictoryType card) {
    	Player current = getSpecialVictoryPlayer(card);
    	if (current != player) {
    		if (current != null)
    			current.removeCard(card);
    		if (player != null)
    			player.addCard(card);
    	}
	}
	
    /**
     * Set the current largest army player
     * @param playerNum
     */
    public void setLargestArmyPlayer(Player player) {
    	givePlayerSpecialVictoryCard(player, SpecialVictoryType.LargestArmy);
    }
    
    /**
     * Set the current longest road player
     * @param playerNum
     */
    public void setLongestRoadPlayer(Player player) {
    	givePlayerSpecialVictoryCard(player, SpecialVictoryType.LongestRoad);
    }

    /**
     * 
     * @param player
     */
    public void setHarborMasterPlayer(Player player) {
    	givePlayerSpecialVictoryCard(player, SpecialVictoryType.HarborMaster);
    }
    
	/**
	 * Return true when it is legal for a player to cancel from their current Move.  
	 * @return
	 */
	public boolean canCancel() {
	    return getState().canCancel;
	}

	/**
	 * 
	 * @return
	 */
	public boolean isGoodForSave() {
		return getState() == State.PLAYER_TURN_NOCANCEL || getState() == State.DEAL_CARDS;//!getState().canCancel && getStateData() == null;
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean isPirateAttacksEnabled() {
		return getRules().isEnableSeafarersExpansion() && getBoard().getPirateRouteStartTile() >= 0;
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
				printinfo(getCurPlayer().getName() + " has discoverd a new territory: " + newType);
				
				for (Route r : mBoard.getTileRoutes(tile)) {
					if (tile.isWater())
						r.setAdjacentToWater(true);
					else if (tile.isLand())
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
					printinfo(getCurPlayer().getName() + " has discovered an island");
					onPlayerDiscoveredIsland(getCurPlayer(), mBoard.getIsland(t.getIslandNum()));
				}
			}
		}
	}
	
	/**
	 * Called when a player road becomes blocked, resulting is loss of road length.  Only used when Config.ENABLE_ROAD_BLOCK enabled.
	 * @param player
	 * @param road
	 */
	protected void onPlayerRoadBlocked(Player player, Route road) {}
	public void updatePlayerRoadsBlocked(Board board, int vertexIndex) {
	    if (getRules().isEnableRoadBlock()) {
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
	public static Player computeLongestRoadPlayer(SOC soc) {
		int maxRoadLen = soc.getRules().getMinLongestLoadLen() - 1;
		Player curLRP = soc.getLongestRoadPlayer();
		if (curLRP != null)
		    maxRoadLen = Math.max(maxRoadLen, curLRP.getRoadLength());

		Player maxRoadLenPlayer = curLRP;
		for (Player cur : soc.getPlayers()) {
			int len = cur.getRoadLength();
			if (len > maxRoadLen) {
				maxRoadLen = len;
				maxRoadLenPlayer = cur;
			}
		} 

		if (maxRoadLenPlayer == null)
		    return null;
		
		if (maxRoadLenPlayer.getRoadLength() >= soc.getRules().getMinLongestLoadLen())
			return maxRoadLenPlayer;
		
		return null;
	}

	/**
	 * compute the player who should have the largest army.
	 * soc is not changed.
	 * @param soc
	 * @return
	 */
	public static Player computeLargestArmyPlayer(SOC soc) {
		int maxArmySize = soc.getRules().getMinLargestArmySize() - 1;
		Player curLAP = soc.getLargestArmyPlayer();
		if (curLAP != null)
		    maxArmySize = Math.max(maxArmySize, curLAP.getArmySize());
		Player maxArmyPlayer = curLAP;
		for (Player cur : soc.getPlayers()) {
			if (cur.getArmySize() > maxArmySize) {
				maxArmySize = cur.getArmySize();
				maxArmyPlayer = cur;
			}
		}

		if (maxArmyPlayer == null)
			return null;
		
		if (maxArmyPlayer.getArmySize() >= soc.getRules().getMinLargestArmySize())
		    return maxArmyPlayer;
		
		return null;
	}	
	
	/**
	 * Compute the player who SHOULD have the harbor master points.  This is the single player with most harbor points >= 3.
	 * @param soc
	 * @return
	 */
	public static Player computeHarborMaster(SOC soc) {
		int minHarborPts = 2;
		Player curHM = soc.getHarborMaster();
		if (curHM != null)
			minHarborPts = curHM.getHarborPoints();
		Player maxHM = curHM;
		for (Player cur : soc.getPlayers()) {
			if (cur.getHarborPoints() > minHarborPts) {
				minHarborPts = cur.getHarborPoints();
				maxHM = cur;
			}
		}
		
		if (maxHM == null)
			return null;
		
		if (maxHM.getHarborPoints() >= 3)
			return maxHM;
		
		return null;
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
	    Player maxRoadLenPlayer = computeLongestRoadPlayer(this);
	    Player curLRP = getLongestRoadPlayer();
	    if (maxRoadLenPlayer == null) {
	        if (curLRP != null) {
	            printinfo(curLRP.getName() + " is blocked and has lost the longest road!");
	            setLongestRoadPlayer(null);
	            onLongestRoadPlayerUpdated(curLRP, null, 0);
	        }
	        return;
	    }
        if (curLRP != null && maxRoadLenPlayer.getPlayerNum() == curLRP.getPlayerNum())
            return;
        final int maxRoadLen = maxRoadLenPlayer.getRoadLength();
        
        if (curLRP == null) {
            printinfo(maxRoadLenPlayer.getName() + " has gained the Longest Road!");
            onLongestRoadPlayerUpdated(null, maxRoadLenPlayer, maxRoadLen);
        } else if (maxRoadLenPlayer.getRoadLength() > curLRP.getRoadLength()) {
            printinfo(maxRoadLenPlayer.getName() + " has overtaken " + curLRP.getName() + " with the Longest Road!");
            onLongestRoadPlayerUpdated(curLRP, maxRoadLenPlayer, maxRoadLen);
        }
        
        setLongestRoadPlayer(maxRoadLenPlayer);

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
		Player largestArmyPlayer = computeLargestArmyPlayer(this);
		Player curLAP = getLargestArmyPlayer();
	    if (largestArmyPlayer == null) {
	    	setLargestArmyPlayer(null);
	        return;
	    }
	    if (curLAP != null && largestArmyPlayer.getPlayerNum() == curLAP.getPlayerNum())
	        return;

	    final int maxArmySize = largestArmyPlayer.getArmySize();
	    if (curLAP == null) {
	        printinfo(largestArmyPlayer.getName() + " has the largest army!");
	        onLargestArmyPlayerUpdated(null, largestArmyPlayer, maxArmySize);
	    } else if (largestArmyPlayer.getArmySize() > curLAP.getArmySize()) {
	        printinfo(largestArmyPlayer.getName() + " overtakes " + curLAP.getName() + " for the largest Army!");
	        onLargestArmyPlayerUpdated(curLAP, largestArmyPlayer, maxArmySize);
	    }

	    setLargestArmyPlayer(largestArmyPlayer);
	}
	
	protected void onHarborMasterPlayerUpdated(Player oldPlayer, Player newPlayer, int harborPts) {}
	
	private void updateHarborMasterPlayer() {
		Player harborMaster = computeHarborMaster(this);
		Player curHM = getHarborMaster();
		if (harborMaster == null) {
			setHarborMasterPlayer(null);
			return;
		}
		
		if (curHM != null && harborMaster.getPlayerNum() == curHM.getPlayerNum())
			return;
		
		final int maxHP = harborMaster.getHarborPoints();
		if (curHM == null) {
			printinfo(harborMaster.getName() + " has is the Harbor Master!");
			onHarborMasterPlayerUpdated(null, harborMaster, maxHP);
		} else {
			printinfo(harborMaster.getName() + " overthrows " + curHM.getName() + " as the new Harbor Master!");
			onHarborMasterPlayerUpdated(curHM, harborMaster, maxHP);
		}
		
		setHarborMasterPlayer(harborMaster);
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
			boolean canAdd = true;
			for (Tile cell : b.getVertexTiles(i)) {
				if (cell.getIslandNum() > 0) {
					isOnIsland = true;
				} else if (cell.getType() == TileType.UNDISCOVERED) {
					canAdd = false;
					break;
				}
			}
			
			if (!canAdd)
				continue;

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
							if (e.isDamaged()) {
								canAdd = false;
								break;
							}
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
			else if (v.isKnight() && v.getPlayer() != knight.getPlayer() && knight.getType().isKnightActive()) {
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
		LinkedHashSet<MoveType> types = new LinkedHashSet<>();
        types.add(MoveType.CONTINUE);

		if (p.canBuild(BuildableType.City) && b.getNumSettlementsForPlayer(p.getPlayerNum()) > 0)
		    types.add(MoveType.BUILD_CITY);

		if (!soc.getRules().isEnableCitiesAndKnightsExpansion()) {
    		if (p.canBuild(BuildableType.Development))
    		    types.add(MoveType.DRAW_DEVELOPMENT);

    		for (DevelopmentCardType t :DevelopmentCardType.values()) {
    			if (t.moveType != null && p.getUsableCardCount(t) > 0) {
    				types.add(t.moveType);
    			}
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
			if (p.getCardCount(SpecialVictoryType.DamagedRoad) > 0) {
				types.add(MoveType.REPAIR_ROAD);
			} else {
    			for (int i=0; i<b.getNumRoutes(); i++) {
    				if (b.isRouteAvailableForRoad(i, p.getPlayerNum())) {
    					types.add(MoveType.BUILD_ROAD);
    					break;
    				}
    			}
			}
		}
		
		if (soc.getRules().isEnableSeafarersExpansion()) {
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
			
			for (int vIndex : b.getVertsOfType(0, VertexType.PIRATE_FORTRESS)) {
				for (Route r : b.getVertexRoutes(vIndex)) {
					if (r.isShip() && r.getPlayer() == p.getPlayerNum()) {
						types.add(MoveType.ATTACK_PIRATE_FORTRESS);
						break;
					}
				}
			}
		}
		
		if (soc.getRules().isEnableCitiesAndKnightsExpansion()) {
			
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
			
			int numCities = b.getNumVertsOfType(p.getPlayerNum(), VertexType.CITY, VertexType.WALLED_CITY);
			int numMetros = b.getNumVertsOfType(p.getPlayerNum(), VertexType.METROPOLIS_POLITICS, VertexType.METROPOLIS_SCIENCE, VertexType.METROPOLIS_TRADE);
			
			// metropolis:
			//   Player must have at least 1 city / metropolis to make any improvements
			//   There are only 3 metropolis in play at any one time.
			//   Must have a non-metropolis city to increase beyond level 3 in any area
			//   First player to reach level 4 in an area, gets to convert one of their cities to a metropolis
			//   When a player reaches level 5 they can take the metropolis from another player with level 4
			//   Once a metropolis is at level 5 it cannot be taken away
			//   Can a player attain level 5 if someone else has a level 5 metro in that area? (no for now)
			
			if (numCities > 0 || numMetros > 0) {
				for (DevelopmentArea area : DevelopmentArea.values()) {
					int devel = p.getCityDevelopment(area);
					assert(devel <= DevelopmentArea.MAX_CITY_IMPROVEMENT);
					if (devel >= DevelopmentArea.MAX_CITY_IMPROVEMENT || p.getCardCount(area.commodity) <= devel) {
						continue;
					}
					
					if (devel < DevelopmentArea.MIN_METROPOLIS_IMPROVEMENT-1) {
						types.add(area.move);
						continue;
					}
					
					if (soc.getMetropolisPlayer(area) != p.getPlayerNum() && numCities <= 0) {
						continue;
					}
					
					if (devel <= DevelopmentArea.MIN_METROPOLIS_IMPROVEMENT) {
						types.add(area.move);
						continue;
					}
					
					if (soc.mMetropolisPlayer[area.ordinal()] > 0) {
						Player o = soc.getPlayerByPlayerNum(soc.mMetropolisPlayer[area.ordinal()]);
						if (o.getCityDevelopment(area) >= DevelopmentArea.MAX_CITY_IMPROVEMENT)
							continue; // cant advance to max if someone else already has (TODO: confirm this rule or make config)
					}
					
					types.add(area.move);
				}
			}
		}

		return new ArrayList<MoveType>(types);
	}

	static public List<Integer> computePirateTileIndices(SOC soc, Board b) {
		if (soc.isPirateAttacksEnabled())
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
		if (!soc.getRules().isEnableRobber())
			return cellIndices;
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
			boolean addTile = false;
			for (int vIndex=0; vIndex<cell.getNumAdj(); vIndex++) {
				Vertex v = b.getVertex(cell.getAdjVert(vIndex));
				if (v.getPlayer() > 0 && v.getPlayer() != soc.getCurPlayerNum()) {
					if (v.isKnight()) {
						addTile = false;
						break;
					}
					if (soc.getPlayerByPlayerNum(v.getPlayer()).getSpecialVictoryPoints() < soc.getRules().getMinVictoryPointsForRobber()) {
						addTile = false;
						break;
					}
					addTile = true;
				}
			}
			
			if (addTile) {
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
			if (b.getRobberTileIndex() == i)
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
	 * Return list of players who can be sabotaged by playerNum
	 * @param soc
	 * @param playerNum
	 * @return
	 */
	static public List<Integer> computeSaboteurPlayers(SOC soc, int playerNum) {
		List<Integer> players = new ArrayList<Integer>();
		int pts = soc.getPlayerByPlayerNum(playerNum).getPoints();
		for (Player player : soc.getPlayers()) {
			if (player.getPlayerNum() == playerNum)
				continue;
			if (player.getPoints() >= pts)
				players.add(player.getPlayerNum());
		}
		return players;
	}

	/**
	 * Compute all the trade options given a player and board instance.
	 * @param p
	 * @param b
	 * @return
	 */
	static public List<Trade> computeTrades(Player p, Board b) {
		List<Trade> trades = new ArrayList<Trade>();
		computeTrades(p, b, trades, 100);
		return trades;
	}
	
	static public int computeCatanStrength(SOC soc, Board b) {
		return b.getNumVertsOfType(0, VertexType.BASIC_KNIGHT_ACTIVE) +
			   b.getNumVertsOfType(0, VertexType.STRONG_KNIGHT_ACTIVE) * 2 +
			   b.getNumVertsOfType(0, VertexType.MIGHTY_KNIGHT_ACTIVE) * 3;
	}
	
	static public int computeBarbarianStrength(SOC soc, Board b) {
		return b.getNumVertsOfType(0, VertexType.CITY, VertexType.WALLED_CITY, VertexType.METROPOLIS_POLITICS, VertexType.METROPOLIS_SCIENCE, VertexType.METROPOLIS_TRADE);
	}
	
	static private void computeTrades(Player p, Board b, List<Trade> trades, int maxOptions) {
		
        //if (Profiler.ENABLED) Profiler.push("SOC::computeTradeOptions");
	    
        try {
    	    int i;
    
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
    		
    		if (trades.size() >= maxOptions)
    			return;
    		
    		// see if we have a 2:1 trade option

    		// check for Trading House ability 
    		
			// we can trade 2:1 commodities if we have level 3 or greater trade improvement (Trading House)
    		if (p.hasTradingHouse()) {
    			for (CommodityType type : CommodityType.values()) {
        			if (!commoditiesFound[type.ordinal()]) {
        				if (p.getCardCount(type) >= 2) {
        					trades.add(new Trade(type, 2));
        					commoditiesFound[type.ordinal()] = true;
        				}
        			}
    			}
			}
    		
    		// check tiles for ports, merchant
    		for (i = 0; i < b.getNumTiles(); i++) {
    			
    			if (b.getPirateTileIndex() == i)
    				continue;
        			
        		if (trades.size() >= maxOptions)
        			return;

    			Tile tile = b.getTile(i);

    			if (tile.getResource() == null)
    				continue;
    			
    			if (b.getMerchantTileIndex() == i && b.getMerchantPlayer() == p.getPlayerNum()) {
    				if (p.getCardCount(tile.getResource()) < 2)
        				continue;
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
    		}
    
    		// we have a 3:1 trade option when we are adjacent to a PORT_MULTI
    		for (i = 0; i < b.getNumTiles(); i++) {
        		if (trades.size() >= maxOptions)
        			return;

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
    				}
    			}
    			
    			for (CommodityType c : CommodityType.values()) {
    				if (!commoditiesFound[c.ordinal()] && p.getCardCount(c) >= 3) {
    					trades.add(new Trade(c, 3));
    					commoditiesFound[c.ordinal()] = true;
    				}
    			}
    		}
    
    		// look for 4:1 trades
    		for (ResourceType r : ResourceType.values()) {
        		if (trades.size() >= maxOptions)
        			return;
    			if (!resourcesFound[r.ordinal()] && p.getCardCount(r) >= 4) {
    				trades.add(new Trade(r, 4));
    			}
    		}
    		
    		for (CommodityType c : CommodityType.values()) {
        		if (trades.size() >= maxOptions)
        			return;
				if (!commoditiesFound[c.ordinal()] && p.getCardCount(c) >= 4) {
					trades.add(new Trade(c, 4));
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
	public List<Integer> computeRobberTakeOpponentCardOptions(Player p, Board b, boolean pirate) {
	    return computeTakeOpponentCardPlayers(getAllPlayers(), p, b, pirate);
	}
	
	/**
	 * 
	 * @param players
	 * @param p
	 * @param b
	 * @return
	 */
	static public List<Integer> computeTakeOpponentCardPlayers(Player [] players, Player p, Board b, boolean pirate) {
		
	    List<Integer> choices = new ArrayList<Integer>();
		boolean [] playerNums = new boolean[players.length];
		
		if (pirate) {
			for (int eIndex : b.getTileRouteIndices(b.getTile(b.getPirateTileIndex()))){
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
    		Tile cell = b.getTile(b.getRobberTileIndex());
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
				choices.add(players[i].getPlayerNum());
		return choices;
	}
	
	/**
	 * Return list of vertices for warlord card
	 * @param b
	 * @param playerNum
	 * @return
	 */
	static public List<Integer> computeWarlordVertices(Board b, int playerNum) {
		return b.getVertsOfType(playerNum, VertexType.BASIC_KNIGHT_INACTIVE, VertexType.STRONG_KNIGHT_INACTIVE, VertexType.MIGHTY_KNIGHT_INACTIVE);
	}
	
	static public List<Card> computeMerchantFleetCards(Player p) {
		List<Card> tradableCards = new ArrayList<Card>();
		for (ResourceType t : ResourceType.values()) {
			List<Card> cards = p.getUsableCards(t);
			if (cards.size() >= 2) {
				tradableCards.addAll(cards);
			}
		}

		for (CommodityType t : CommodityType.values()) {
			List<Card> cards = p.getUsableCards(t);
			if (cards.size() >= 2) {
				tradableCards.addAll(cards);
			}
		}
		return tradableCards;
	}
	
	/**
	 * Return list of players who are not p who have more points than p and who have at least 1 Commodity or Resource card in their hand.
	 * @param soc
	 * @param p
	 * @return
	 */
	static public List<Integer> computeWeddingOpponents(SOC soc, Player p) {
		List<Integer> players = new ArrayList<Integer>();
		for (Player player : soc.getPlayers()) {
			if (player.getPlayerNum() == p.getPlayerNum())
				continue;
			if (player.getPoints() <= p.getPoints())
				continue;
			if (player.getUnusedCardCount(CardType.Commodity) > 0 || player.getUnusedCardCount(CardType.Resource) > 0)
				players.add(player.getPlayerNum());
		}
		return players;
	}
	
	/**
	 * Return a list of all the opponent players of playerNum
	 * @param soc
	 * @param playerNum
	 * @return
	 */
	public static List<Integer> computeOpponents(SOC soc, int playerNum) {
		List<Integer> p = new ArrayList<Integer>();
		for (int i=1; i<=soc.getNumPlayers(); i++) {
			if (i != playerNum) {
				p.add(i);
			}
		}
		return p;
	}
	
	/**
	 * Get the winningest player with most victory points
	 * @param soc
	 * @return
	 */
	static public Player computePlayerWithMostVictoryPoints(SOC soc) {
		Player winning = null;
		int most = 0;
		for (Player p : soc.getPlayers()) {
			int num = p.getSpecialVictoryPoints();
			if (num > most) {
				most = num;
				winning = p;
			}
		}
		return winning;
	}
	
	/**
	 * Return a list of players suitable for the deserter card
	 * @param soc
	 * @param b
	 * @param p
	 * @return
	 */
	static public List<Integer> computeDeserterPlayers(SOC soc, Board b, Player p) {
		List<Integer> players = new ArrayList<Integer>();
		for (int i=1; i<=soc.getNumPlayers(); i++) {
			if (i == p.getPlayerNum())
				continue;
			if (0 < b.getNumKnightsForPlayer(i)) {
				players.add(i);
			}
		}
		return players;
	}
	
	static public List<MoveType> computeCraneCardImprovements(Player p) {
		ArrayList<MoveType> options = new ArrayList<MoveType>();
		for (DevelopmentArea area : DevelopmentArea.values()) {
			int devel = p.getCityDevelopment(area);
			int numCommodity = p.getCardCount(area.commodity);
			if (numCommodity >= devel) {
				options.add(area.move);
			}
		}
		return options;
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
//		popState();
		printinfo(p.getName() + " is improving their " + area);
		int devel = p.getCityDevelopment(area);
		assert(devel < DevelopmentArea.MAX_CITY_IMPROVEMENT);
		devel++;
		p.removeCards(area.commodity, devel);
		p.setCityDevelopment(area, devel);
		checkMetropolis(devel, getCurPlayerNum(), area);
	}
	
	protected void onPirateSailing(int fromTile, int toTile) {}
	
	protected void onCardLost(Player p, Card c) {}
	
	protected void onPirateAttack(Player p, int playerStrength, int pirateStrength) {}
	
	private void processPirateAttack() {
		int min = Math.min(mDice[0], mDice[1]);
		while (min-- > 0) {
			int fromTile = getBoard().getPirateTileIndex();
			Tile t = getBoard().getPirateTile();
			int toTile = t.getPirateRouteNext();
			if (toTile < 0)
				toTile = getBoard().getPirateRouteStartTile();
			getBoard().setPirate(-1);
			onPirateSailing(fromTile, toTile);
			getBoard().setPirate(toTile);
		}
		Tile t = getBoard().getPirateTile();
		boolean [] attacked = new boolean[16];
		pushStateFront(State.SET_PLAYER, getCurPlayerNum(), null);
		for (int vIndex : t.getAdjVerts()) {
			Vertex v = getBoard().getVertex(vIndex);
			if (attacked[v.getPlayer()])
				continue;
			if (v.isStructure()) {
				Player p = getPlayerByPlayerNum(v.getPlayer());
				int playerPts = 0;
				for (int rIndex : getBoard().getRoadsForPlayer(v.getPlayer())) {
					Route r = getBoard().getRoute(rIndex);
					if (r.isWarShip())
						playerPts ++;
				}
				attacked[v.getPlayer()] = true;
				printinfo("Pirate Attack!  " +p.getName() +  " strength " + playerPts + " pirate strength " + min);
				onPirateAttack(p, playerPts, min);
				if (min < playerPts) {
					// player wins the attack
					printinfo(p.getName() + " has defeated the pairates and take a resource card of their choice");
					pushStateFront(State.DRAW_RESOURCE_NOCANCEL);
					pushStateFront(State.SET_PLAYER, v.getPlayer(), null);
				} else if (min > playerPts) {
					printinfo("Pirates have defeated " + p.getName() + " so player loses 2 random resources cards");
					int numResources = p.getCardCount(CardType.Resource);
					for (int i=0; i<2 && numResources-- > 0; i++) {
						Card c = p.removeRandomUnusedCard(CardType.Resource);
						onCardLost(p, c);
					}
				} else {
					printinfo("Pirate and " + p.getName() + " are of equals strength so nothing happens");
				}
			}
		}
	}
	
	private void processDice() {
		// roll the dice
		
		if (isPirateAttacksEnabled()) {
			processPirateAttack();
		}
		
		if (getProductionNum() == 7) {
			popState();
			printinfo("Uh Oh, " + getCurPlayer().getName()  + " rolled a 7.");
			pushStateFront(State.SETUP_GIVEUP_CARDS);
			if (getRules().isEnableSeafarersExpansion())
				pushStateFront(State.POSITION_ROBBER_OR_PIRATE_NOCANCEL);
			else
				pushStateFront(State.POSITION_ROBBER_NOCANCEL);
		} else {

    		popState();
    		pushStateFront(State.NEXT_PLAYER);
    		
    		// after the last player takes a turn for this round need to advance 2 players
    		// so that the player after the player who rolled the dice gets to roll next
    		for (int i = 0; i < mNumPlayers; i++) {
    			if (i > 0)
    				pushStateFront(State.PLAYER_TURN_NOCANCEL);
    			pushStateFront(State.NEXT_PLAYER);
    		}
    		// reset the players ships/development cards usability etc.
    		pushStateFront(State.INIT_PLAYER_TURN);
    
    		// do this last so that any states that get pushed are on top
    		distributeResources(getProductionNum());
    		
    		if (getRules().isEnableCitiesAndKnightsExpansion()) {
    			switch (DiceEvent.fromDieNum(mDice[2])) {
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
	}
	
	/**
	 * Called when a player loses their metropolis to another player
	 * @param loser
	 * @param stealer
	 * @param area
	 */
	protected void onMetropolisStolen(Player loser, Player stealer, DevelopmentArea area) {}
	
	private void checkMetropolis(int devel, int playerNum, DevelopmentArea area) {
		if (devel >= DevelopmentArea.MIN_METROPOLIS_IMPROVEMENT) {
			//List<Integer> metropolis = mBoard.getVertsOfType(0, area.vertexType);
			final int metroPlayer = mMetropolisPlayer[area.ordinal()];
			if (metroPlayer != playerNum) { // if we dont already own this metropolis
    			if (metroPlayer == 0) { // if it is unowned
    				pushStateFront(State.CHOOSE_METROPOLIS, area, null);
    			} else {
    				//assert(metropolis.size() == 1);
    				List<Integer> verts = mBoard.getVertsOfType(metroPlayer, area.vertexType);
    				assert(verts.size() == 1);
    				Vertex v = mBoard.getVertex(verts.get(0));
    				assert(v.getPlayer() == metroPlayer);
    				if (v.getPlayer() != getCurPlayerNum()) {
    					Player other = getPlayerByPlayerNum(metroPlayer);
    					final int otherDevel = other.getCityDevelopment(area);
    					assert(otherDevel>=DevelopmentArea.MIN_METROPOLIS_IMPROVEMENT);
    					if (otherDevel < devel) {
    						printinfo(other.getName() + " loses Metropolis " + area + " to " + getCurPlayer().getName());
    						v.setType(VertexType.CITY);
    						onMetropolisStolen(getPlayerByPlayerNum(v.getPlayer()), getPlayerByPlayerNum(playerNum), area);
    						pushStateFront(State.CHOOSE_METROPOLIS, area, null);
    					}
    				}
    			}
			}
		}		
	}

	protected void onProgressCardDistributed(Player player, ProgressCardType type) {}
	protected void onSpecialVictoryCard(Player player, SpecialVictoryType type) {}
	
	private void distributeProgressCard(DevelopmentArea area) {
		for (Player p : getPlayers()) {
			if (mProgressCards[area.ordinal()].size() > 0 
					&& p.getCardCount(CardType.Progress) < getRules().getMaxProgressCards() 
					&& p.getCityDevelopment(area) > 0 
					&& p.getCityDevelopment(area) >= mDice[1]-1) {
				Card card = mProgressCards[area.ordinal()].remove(0);
				printinfo(p.getName() + " draws a " + area.name() + " Progress Card");
				if (card.equals(ProgressCardType.Constitution)) {
					p.addCard(SpecialVictoryType.Constitution);
					onSpecialVictoryCard(p, SpecialVictoryType.Constitution);
				} else if (card.equals(ProgressCardType.Printer)) {
					card.setUsed();
					p.addCard(SpecialVictoryType.Printer);
					onSpecialVictoryCard(p, SpecialVictoryType.Printer);
				} else {
    				p.addCard(card);
    				onProgressCardDistributed(p, ProgressCardType.values()[card.getTypeOrdinal()]);
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
	 * 
	 * @param catanStrength
	 * @param barbarianStrength
	 * @param playerStatus
	 */
	protected void onBarbariansAttack(int catanStrength, int barbarianStrength, String [] playerStatus) {}
	
	/**
	 * 
	 * @param playerNum
	 * @param tile0
	 * @param tile1
	 */
	protected void onTilesInvented(Player player, Tile tile0, Tile tile1) {}
	
	private void processBarbarianShip() {
		mBarbarianDistance -= 1;
		if (mBarbarianDistance == 0) {
			printinfo("The Barbarians are attacking!");
			int [] playerStrength = new int[mNumPlayers+1];
			int minStrength = Integer.MAX_VALUE;
			int maxStrength = 0;
			for (int i=1; i<=getNumPlayers(); i++) {
				playerStrength[i] = mBoard.getNumVertsOfType(i, VertexType.BASIC_KNIGHT_ACTIVE) * VertexType.BASIC_KNIGHT_ACTIVE.getKnightLevel()
								  + mBoard.getNumVertsOfType(i, VertexType.STRONG_KNIGHT_ACTIVE) * VertexType.STRONG_KNIGHT_ACTIVE.getKnightLevel()
								  + mBoard.getNumVertsOfType(i, VertexType.MIGHTY_KNIGHT_ACTIVE) * VertexType.MIGHTY_KNIGHT_ACTIVE.getKnightLevel();
				int numCities = mBoard.getNumVertsOfType(i, VertexType.CITY, VertexType.WALLED_CITY);//, VertexType.METROPOLIS_POLITICS, VertexType.METROPOLIS_SCIENCE, VertexType.METROPOLIS_TRADE);
				if (numCities == 0)
					minStrength = Integer.MAX_VALUE; // dont count players who have no pillidgable cities
				else
					minStrength = Math.min(minStrength, playerStrength[i]);
				maxStrength = Math.max(maxStrength, playerStrength[i]);
			}

			int catanStrength = CMath.sum(playerStrength);
			int barbarianStrength = computeBarbarianStrength(this, mBoard);
			String [] playerStatus = new String[mNumPlayers+1];

			for (int i=0; i<playerStrength.length; i++) {
				if (playerStrength[i] == Integer.MAX_VALUE)
					playerStrength[i] = 0;
			}
			
			for (Player p : getPlayers()) {
				playerStatus[p.getPlayerNum()] = "Strength " + playerStrength[p.getPlayerNum()];
			}

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
					defender.addCard(SpecialVictoryType.DefenderOfCatan);
					printinfo(defender.getName() + " receives the Defender of Catan card!");
					for (Player p : getPlayers()) {
						playerStatus[p.getPlayerNum()] = "[" + playerStrength[p.getPlayerNum()] + "]";
					}
					playerStatus[defender.getPlayerNum()] += " Defender of Catan";
				} else {
					pushStateFront(State.SET_PLAYER, getCurPlayerNum(), null);
					for (int playerNum : defenders) {
						if (getPlayerByPlayerNum(playerNum).getCardCount(CardType.Progress) < getRules().getMaxProgressCards()) {
							playerStatus[playerNum] += " Choose progress card";
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
						printinfo(getPlayerByPlayerNum(playerNum).getName() + " has defended their city with a wall");
						int cityIndex = Utils.randItem(cities);
						playerStatus[playerNum] += " Defended by wall";
						Vertex v = mBoard.getVertex(cityIndex);
						v.setType(VertexType.CITY);
					} else {
						cities = mBoard.getVertsOfType(playerNum, VertexType.CITY);
						if (cities.size() > 0) {
							printinfo(getPlayerByPlayerNum(playerNum).getName() + " has their city pilledged");
							int cityIndex = Utils.randItem(cities);
							playerStatus[playerNum] +=" City piledged";
							Vertex v = mBoard.getVertex(cityIndex);
							v.setType(VertexType.SETTLEMENT);
						}
					}
				}
				
			}
			onBarbariansAttack(catanStrength, barbarianStrength, playerStatus);
			mBarbarianDistance = getRules().getBarbarianStepsToAttack();
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
	protected void onMonopolyCardApplied(Player taker, Player giver, ICardType<?> applied, int amount) {}
	
	private void processMonopoly(ICardType<?> type) {
		// take the specified resource from all other players
		for (int i=1; i<mNumPlayers; i++) {
			Player cur = mPlayers[(mCurrentPlayer+i) % mNumPlayers];
			int num = cur.getCardCount(type);
			if (num > 0) {
				if (getRules().isEnableCitiesAndKnightsExpansion() && num > 2) {
					num = 2;
				}
				printinfo(getCurPlayer().getName() + " takes " + num + " " + type + " card from player " + cur.getName());
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

	public int getBarbarianDistance() {
		return this.mBarbarianDistance;
	}

	public int getMinHandCardsForRobberDiscard(int playerNum) {
		int num = getRules().getMinHandSizeForGiveup();
		if (getRules().isEnableCitiesAndKnightsExpansion()) {
			num += getRules().getNumSafeCardsPerCityWall() * mBoard.getNumVertsOfType(playerNum, VertexType.WALLED_CITY,
				// From the rule book metros are not included in this computation but I think they should be so I am doing it so there
				VertexType.METROPOLIS_POLITICS, VertexType.METROPOLIS_SCIENCE, VertexType.METROPOLIS_TRADE); 
		}
		return num;
	}
	
	public String getHelpText() {
		if (mStateStack.peek() == null) {
			return "Game not running";
		}
		
		return mStateStack.peek().state.helpText;
	}
}
