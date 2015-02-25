package cc.game.soc.core;

import java.io.*;
import java.util.*;

import cc.game.soc.core.IConfig.Key;
import cc.game.soc.core.SOCBoard.IVisitor;

/**
 * SOC Core business logic
 * 
 * Add player instances then call runGame() until isGameOver()
 * Make sure to initialize the board otherwise a compltly random one will get generated
 * 
 * @author Chris Caron
 * 
 */
public class SOC {
    
    private interface UndoAction {
        void undo();
    }
    
    private class StackItem {
        
        public StackItem(SOCState state, UndoAction action) {
            super();
            this.state = state;
            this.action = action;
        }

        final SOCState state;
        final UndoAction action;
        
        public String toString() {
            if (action != null) {
                return "(" + state + ", " + action + ")";
            }
            return state.name(); 
        }
    };
    
	private SOCPlayer			mPlayers;
	private SOCPlayer			mCurrentPlayer;
	private int					mNumPlayers;
	private int					mDie1, mDie2;
	private int				    mLongestRoadPlayer;
	private int				    mLargestArmyPlayer;
	private ArrayList<StackItem> mStateStack	= new ArrayList<StackItem>();	
	private int []				mDeck = new int [Helper.NUM_DEVELOPMENT_CARD_TYPES];
	private SOCBoard			mBoard;
	
    @SuppressWarnings("rawtypes")
    private List				mOptions;	
	private Random              mRandom;
	private IConfig             mConfig;
	private SOCTrade            mSaveTrade;
	
	private SOCState getState() {
		return mStateStack.isEmpty() ? SOCState.INIT_GAME : mStateStack.get(0).state;
	}
	
	/*
	@SuppressWarnings("unchecked")
    private <T> T getData() {
	    return (T)(mStateStack.isEmpty() ? null : mStateStack.get(0).data);
	}*/

	private UndoAction getUndoAction() {
	    return mStateStack.isEmpty() ? null : mStateStack.get(0).action;
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
		if (mCurrentPlayer == null)
			return 0;
		return mCurrentPlayer.getPlayerNum();
	}
	
	/**
	 * 
	 * @param playerNum
	 */
	public void setCurrentPlayer(int playerNum) {
	    mCurrentPlayer = getPlayerByPlayerNum(playerNum);
	}

	/**
	 * Get value of first die
	 * @return
	 */
	public int getDie1() {
		return mDie1;
	}

	/**
	 * Get value of second die
	 * @return
	 */
	public int getDie2() {
		return mDie2;
	}

	/**
	 * Get sum of both 6 sided die 
	 * @return
	 */
	public int getDiceNum() {
		return mDie1 + mDie2;
	}

	/**
	 * 
	 * @return
	 */
	public SOCBoard getBoard() {
		return mBoard;
	}

	/**
	 * 
	 * @return
	 */
	public SOCPlayer getCurPlayer() {
		return mCurrentPlayer;
	}

	/**
	 * 
	 * @param className
	 * @return
	 * @throws Exception
	 */
	protected SOCPlayer instantiatePlayer(String className) throws Exception {
	    return (SOCPlayer)getClass().getClassLoader().loadClass(className).newInstance();
	}

	/**
	 * 
	 * @return
	 */
	public SOCCell getRobberCell() {
		return getBoard().getCell(getBoard().getRobberCell());
	}
	
	/**
	 * 
	 */
	public SOC() {
	    this(new SOCBoard());
	}

	/**
	 * 
	 * @param board
	 */
	public SOC(SOCBoard board) {
		this(board, IConfig.DEFAULT_CONFIG);
	}
	
	public SOC(SOCBoard board, IConfig config) {
	    this.mBoard = board;
	    this.mConfig = config;
	}
	
	
	/**
	 * 
	 * @return
	 */
	public Random getRandom() {
	    if (mRandom == null)
	        mRandom = new Random();
	    return mRandom;
	}
	
	/**
	 * 
	 * @param random
	 */
	public void setRandom(Random random) {
	    this.mRandom = random;
	}

	/**
	 * Get the interface for getting game settings.  Never returns null  
	 * @return
	 */
	public IConfig getConfig() {
        return this.mConfig;
    }
    
	/**
	 * Set the interfaces for getting game settings.
	 * setting to null reverts to default settings
	 * @param config
	 */
    public void setConfig(IConfig config) {
        if (config == null)
            this.mConfig = IConfig.DEFAULT_CONFIG;
        else
            this.mConfig = config;
    }
    
    /**
     * Convenience method to use a properties file for values
     * @param props
     */
    public void setConfigProperties(final Properties props) {
        setConfig(new IConfig() {

            @Override
            public boolean getBoolean(Key key) {
                try {
                    if (props.contains(key.name()))
                        return Boolean.parseBoolean(props.getProperty(key.name()));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return IConfig.DEFAULT_CONFIG.getBoolean(key);
            }

            @Override
            public int getInt(Key key) {
                try {
                    if (props.contains(key.name()))
                        return Integer.parseInt(props.getProperty(key.name()));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return IConfig.DEFAULT_CONFIG.getInt(key);
            }

            @Override
            public String getString(Key key) {
                try {
                    if (props.contains(key.name()))
                        return props.getProperty(key.name());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return IConfig.DEFAULT_CONFIG.getString(key);
            }
            
        });
    }
	
	/**
	 * 
	 */
	public void reset(boolean clearPlayers) {
		mLongestRoadPlayer = -1;
		mLargestArmyPlayer = -1;
		mDie1 = mDie2 = 0;
		mBoard.reset();
		mStateStack.clear();
		if (clearPlayers) {
		    mPlayers = mCurrentPlayer = null;
		    mNumPlayers = 0;
	        mCurrentPlayer = null;
		}

		resetOptions();
	}
	
	private void resetOptions() {
		mOptions = null;
	}

	private void initDeck() {
		for (int i=0; i<Helper.NUM_DEVELOPMENT_CARD_TYPES; i++)
			mDeck[i] = DevelopmentCardType.values()[i].getNumOccurancesInDeck(mConfig);
		
	}
	
	/**
	 * 
	 * @param board
	 */
	public void setBoard(SOCBoard board) {
		mBoard = board;
	}

	/**
	 * 
	 * @param out
	 * @throws IOException
	 */
	public void write(BufferedWriter out) throws IOException {
		out.write("NUMPLAYERS:" + getNumPlayers() + "\n");
		out.write("CURPLAYER:" + getCurPlayerNum() + "\n");
		out.write("LONGEST_ROAD_PLAYER:" + mLongestRoadPlayer + "\n");
		out.write("LARGEST_ARMY_PLAYER:" + mLargestArmyPlayer + "\n");
		for (DevelopmentCardType d : DevelopmentCardType.values()) {
			out.write(d.name() + ":" + mDeck[d.ordinal()] + "\n");
		}
		SOCPlayer cur = mPlayers;
		for (int i = 0; i < mNumPlayers; i++) {
			cur.write(out);
			cur = cur.getNext();
		}
		mBoard.saveBoard(out);
	}
	
	public final void write2(BufferedWriter out) throws IOException {
	    out.write("SOC.NUMPLAYER=" + getNumPlayers() + "\n");
        out.write("SOC.CURPLAYER=" + getCurPlayerNum() + "\n");
        out.write("SOC.LONGEST_ROAD_PLAYER=" + mLongestRoadPlayer + "\n");
        out.write("SOC.LARGEST_ARMY_PLAYER=" + mLargestArmyPlayer + "\n");
        for (DevelopmentCardType d : DevelopmentCardType.values()) {
            out.write("SOC.DECK[" + d.name() + "]=" + mDeck[d.ordinal()] + "\n");
        }
        SOCPlayer cur = mPlayers;
        for (int i = 0; i < mNumPlayers; i++) {
            cur.write2(out);
            cur = cur.getNext();
        }
        mBoard.saveBoard2(out);
	}

	static <T extends Enum<T>> int parseEnumIndex(String line, Class<T> clazz) {
        int l = line.indexOf('[');
        int r = line.indexOf(']', l);
        String v = line.substring(l+1,r);
        return Enum.valueOf(clazz, v).ordinal();
	}
	
	static int parseIndex(String line) {
        int l = line.indexOf('[');
        int r = line.indexOf(']', l);
        return Integer.parseInt(line.substring(l+1,r).trim());
    }
    
    static String parseField(String line) {
        // everything between first '.' and '='
        int dot = line.indexOf('.');
        int eq = line.indexOf('=', dot);
        return line.substring(dot+1, eq);
    }
    
    static String parseValue(String line) {
        // everything to right of '='
        return line.substring(line.indexOf('=')+1);
    }
	/**
     * 
     * @param in
     * @throws Exception
     */
    public final void read2(BufferedReader in) throws Exception {
        String line = "";
        try {
            while (true) {
                line = in.readLine();
                if (line == null)
                    break;
                if (line.length() == 0)
                    continue;
                if (line.startsWith("#"))
                    continue;
                if (line.startsWith("SOC")) {
                    String field = parseField(line);
                    String value = parseValue(line);
                    if (field.equals("NUMPLAYER")) {
                        int num = Integer.parseInt(value);
                        if (getNumPlayers() != num) {
                            //this.sesetNumPlayers(num);
                        }
                    } else if (field.equals("CURPLAYER")) {
                        int num = Integer.parseInt(value);
                        this.setCurrentPlayer(num);
                    } else if (field.equals("DECK")) {
                        int index = parseEnumIndex(line, DevelopmentCardType.class);
                        mDeck[index] = Integer.parseInt(value);
                    } else if (field.equals("LONGEST_ROAD_PLAYER")) {
                        this.setLongestRoadPlayer(Integer.parseInt(value));
                    } else if (field.equals("LARGEST_ARMY_PLAYER")) {
                        this.setLargestArmyPlayer(Integer.parseInt(value));
                    }
                } else if (line.startsWith("PLAYER")) {
                    int num = parseIndex(line);
                    SOCPlayer p = getPlayerByPlayerNum(num);
                    String field = parseField(line);
                    String value = parseValue(line);
                    if (field.equals("class")) {
                        if (p == null || !p.getClass().equals(value)) {
                            p = this.instantiatePlayer(value);
                            p.setPlayerNum(num);
                            //this.addPlayer(player)
                            this.addPlayer(p);
                        }
                    } else {
                        p.read(field, value);
                    }
                } else if (line.startsWith("BOARD")) {
                    
                } else {
                    throw new IOException("Unknown game element: " + line);
                }
            }
        } catch (Exception e) {
            throw new Exception("Error paring line: " + line + "\n", e);
        } finally {
            in.close();
        }
    }   	
	/**
	 * 
	 * @param fileName
	 */
	public void save(String fileName) {
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(fileName));
			write(out);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				out.close();
			} catch (Exception e) {}
		}
	}

	/**
	 * 
	 * @param playerNum range is [1-numPlayers] inclusive
	 * @return null if player num out of range, the player with num otherwise
	 */
	public SOCPlayer getPlayerByPlayerNum(int playerNum) {
		if (mPlayers == null)
			return null;
		//assert (playerNum > 0);
		SOCPlayer cur = mPlayers;
		do {
			if (cur.getPlayerNum() == playerNum)
				return cur;
			cur = cur.getNext();
		} while (cur != mPlayers);
		return null;
	}

	/**
	 * 
	 * @param in
	 * @throws Exception
	 */
	public void read(BufferedReader in) throws Exception {
		reset(true);
		int curPlayerNum = 0;
		int numPlayers = 0;
		numPlayers = Helper.parseInt(in.readLine(), "NUMPLAYERS:");
		curPlayerNum = Helper.parseInt(in.readLine(), "CURPLAYER:");
		mLongestRoadPlayer = Helper.parseInt(in.readLine(), "LONGEST_ROAD_PLAYER:");
		mLargestArmyPlayer = Helper.parseInt(in.readLine(), "LARGEST_ARMY_PLAYER:");
		for (DevelopmentCardType d : DevelopmentCardType.values()) {
			mDeck[d.ordinal()] = Helper.parseInt(in.readLine(), d.name() + ":");
		}
		mStateStack.clear();
		if (numPlayers < 2) {
			throw new IOException("Wrong number of players " + numPlayers + " expected >= 2");
		}
		for (int i = 0; i < numPlayers; i++) {
			String line = in.readLine();
			String[] parts = line.split("[ ]");
			String className = parts[0];
			int playerNum = Integer.parseInt(parts[1]);
			SOCPlayer newPlayer = getPlayerByPlayerNum(playerNum);
			if (newPlayer == null || !newPlayer.getClass().getName().equals(className)) {
			    newPlayer = instantiatePlayer(className);
	            newPlayer.setPlayerNum(playerNum);
	            addPlayer(newPlayer);
			}
			newPlayer.read(in);
            assert(playerNum == newPlayer.getPlayerNum());
		}
		mCurrentPlayer = getPlayerByPlayerNum(curPlayerNum);
		if (mCurrentPlayer == null) {
			throw new IOException("Failed to get the current player");
		}
		mBoard.loadBoard(in);
		updateLongestRoadPlayer();
		updateLargestArmyPlayer();
		updatePlayerPoints();

		mStateStack.clear();
		pushState(SOCState.ROLL_DICE);
	}
	
    private void pushStateFront(SOCState state) {
        pushStateFront(state, null);
    }

    private void pushStateFront(SOCState state, UndoAction action) {
	    mStateStack.add(0, new StackItem(state, action));
	}
	
	private void pushState(SOCState state, UndoAction action) {
	    mStateStack.add(new StackItem(state, action));
	}

    private void pushState(SOCState state) {
        pushState(state, null);
    }

	/**
	 * 
	 * @param fileName
	 * @return
	 */
	public boolean load(String fileName) {
		boolean success = true;
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(fileName));
			read(in);
		} catch (IOException e) {
			success = false;
			logError(e.getMessage());
		} catch (Exception e) {
			success = false;
			e.printStackTrace();
		} finally {
			try {
				in.close();
			} catch (Exception e) {
			}
		}
		return success;
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
	public void addPlayer(SOCPlayer player) {
		//assert (player.getPlayerNum() == 0);
		if (mPlayers == null) {
			mPlayers = player;
			mPlayers.setNext(mPlayers);
			mPlayers.setPrev(mPlayers);
		} else {
			SOCPlayer last = mPlayers.getPrev();
			last.setNext(player);
			player.setPrev(last);
			player.setNext(mPlayers);
			mPlayers.setPrev(player);
		}
		++mNumPlayers;
		if (player.getPlayerNum() == 0)
			player.setPlayerNum(mNumPlayers);
        logDebug("AddPlayer num = " + player.getPlayerNum() + " " + player.getClass().getSimpleName());
	}

	private void incrementCurPlayer(int num) {
		int nextPlayerNum = getCurPlayerNum() + num;
		if (nextPlayerNum > mNumPlayers)
			nextPlayerNum = 1;
		else if (nextPlayerNum < 1)
			nextPlayerNum = mNumPlayers;
		//((mCurrentPlayer.getPlayerNum() + num) % getNumPlayers()) + 1;
		logDebug("Increment player [" + num + "] positions, was " + mCurrentPlayer.getPlayerNum() + ", now " + nextPlayerNum);
		mCurrentPlayer = getPlayerByPlayerNum(nextPlayerNum);
	}

	/**
	 * 
	 *
	 */
	public void dumpStack() {
		if (isDebugEnabled()) {
			logDebug("State Stack: " + mStateStack);
		}
	}

	private void popState() {
		assert (mStateStack.size() > 0);
		mStateStack.remove(0);
		logDebug("Setting state to " + (getState()));
		resetOptions();
	}

	/**
	 * 
	 * @param die1
	 * @param die2
	 */
	public void setDice(int die1, int die2) {
		mDie1 = die1;
		mDie2 = die2;
	}
	
	/**
	 * 
	 * @param r
	 */
	public void rollDice(Random r) {
		setDice(r.nextInt(6) + 1, r.nextInt(6) + 1);
		printinfo("Die roll: " + mDie1 + ", " + mDie2);
	}

	private void dealCards() {
		printinfo("Dealing cards ...");
		for (int i = 0; i < mBoard.getNumCells(); i++) {
			SOCCell cell = mBoard.getCell(i);
			if (cell.getType() != SOCCellType.RESOURCE)
				continue;

			SOCPlayer cur = mPlayers;
			do {

				if (mBoard.isPlayerAdjacentToCell(cur, i)) {
					cur.incrementResource(cell.getResource(), 1);
				}
				cur = cur.getNext();
			} while (cur != mPlayers);
		}

		// print out what each player got.
		SOCPlayer cur = mPlayers;
		do {

			String msg = "Player " + cur.getPlayerNum() + " gets";
			for (ResourceType r : ResourceType.values()) {
				int num = cur.getResourceCount(r);
				if (num > 0) {
					msg += " " + num + " X " + r;
					this.onDistributeResources(cur, r, num);
				}
			}
			printinfo(msg);
			cur = cur.getNext();

		} while (cur != mPlayers);
	}

	/**
	 * Called for Every resource bundle a player recieves.  
	 * Called once for each player, for each resource.  
	 * default method does nothing.
	 * @param player
	 * @param type
	 * @param amount
	 */
	protected void onDistributeResources(SOCPlayer player, ResourceType type, int amount) {}
	private void distributeResources() {
		// collect info to be displayed at the end
		int [][] info = new int[Helper.NUM_RESOURCE_TYPES][];

		for (int i = 0; i < info.length; i++) {
			info[i] = new int[getNumPlayers()];
		}

		printinfo("Distributing resources for num " + getDiceNum());

		// visit all the cells with dice as their num
		for (int i = 0; i < mBoard.getNumCells(); i++) {

			SOCCell cell = mBoard.getCell(i);
			if (cell.getType() != SOCCellType.RESOURCE)
				continue;

			if (mBoard.getRobberCell() == i)
				continue; // apply the robber

			if (cell.getDieNum()!= getDiceNum())
				continue;

			// visit each of the adjacent verts to this cell and
			// add to any player at the vertex, some resource of
			// type cell.resource
			for (int vIndex: cell.getAdjVerts()) {
				SOCVertex vertex = mBoard.getVertex(vIndex);
				if (vertex.player > 0) {
					int num = vertex.isCity ? mConfig.getInt(Key.NUM_RESOURCES_FOR_CITY) : mConfig.getInt(Key.NUM_RESOURCES_FOR_SETTLEMENT);
					info[cell.getResource().ordinal()][vertex.player - 1] += num;
					getPlayerByPlayerNum(vertex.player).incrementResource(cell.getResource(), num);
				}
			}
		}

		// display how much each player recieved
		SOCPlayer cur = mPlayers;

		for (int i = 0; i < mNumPlayers; i++) {
	        String msg = "";

			//for (int j = 0; j < Helper.NUM_RESOURCE_TYPES; j++) {
	        for (ResourceType r : ResourceType.values()) {
			    int amount = info[r.ordinal()][i];
				if (amount > 0) {
					msg += (msg.length() == 0 ? "" : ", ") + info[r.ordinal()][i] + " X " + r;
					this.onDistributeResources(cur, r, amount);
				}
			}

			if (msg.length() > 0) {
				printinfo("Player " + cur.getPlayerNum() + " gets " + msg);
			}
			cur = cur.getNext();
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

	private void computePointsForPlayer(SOCPlayer p) {
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
	static public int computePointsForPlayer(SOCPlayer player, SOCBoard board, SOC soc) {
	    int longestRoadPlayer = soc.getLongestRoadPlayerNum();
	    int largestArmyPlayer = soc.getLargestArmyPlayerNum();
		int numPts = 0;
		// count cities and settlements
		for (int i = 0; i < board.getNumVerts(); i++) {
			SOCVertex vertex = board.getVertex(i);
			if (vertex.player == player.getPlayerNum()) {
				numPts += vertex.isCity ? soc.mConfig.getInt(Key.POINTS_CITY) : soc.mConfig.getInt(Key.POINTS_SETTLEMENT);
			}
		}

		numPts += player.getNumDevelopment(DevelopmentCardType.Victory, DevelopmentCard.USED);
        
		if (player.getPlayerNum() == longestRoadPlayer)
			numPts += soc.mConfig.getInt(Key.POINTS_LONGEST_ROAD);

		if (player.getPlayerNum() == largestArmyPlayer)
			numPts += soc.mConfig.getInt(Key.POINTS_LARGEST_ARMY);

		//soc.logDebug("Points = " + numPts);
		
		return numPts;
	}
	
	/**
	 * Called when a players point change (for better or worse).  default method does nothing.
	 * @param player
	 * @param changeAmount
	 */
	protected void onPlayerPointsChanged(SOCPlayer player, int changeAmount) {}
	private void updatePlayerPoints() {
		SOCPlayer cur = mPlayers;
		do {
			computePointsForPlayer(cur);
			cur = cur.getNext();
		} while (cur != mPlayers);
	}

	/**
	 * Called when a player picks a development card from the deck.
	 * default method does nothing.
	 * @param player
	 * @param card
	 */
	protected void onDevelopmentCardPicked(SOCPlayer player, DevelopmentCardType card) {}
	private void pickDevelopmentFromDeck() {
		// add up the total chance
		int index = Helper.chooseRandomFromSet(mDeck, getRandom());
		if (index < 0) {
		    initDeck();
		    index = Helper.chooseRandomFromSet(mDeck, getRandom());
		}
		mDeck[index] --;
		assert(mDeck[index]) >= 0;
		DevelopmentCardType card = DevelopmentCardType.values()[index];
		printinfo("Player " + getCurPlayerNum() + " picked a " + card + " card");
		getCurPlayer().addDevelopmentCard(card, DevelopmentCard.NOT_USABLE);
		this.onDevelopmentCardPicked(getCurPlayer(), card);
	}

	/**
	 * Called when a player takes a card from another due to soldier.  default method does nothing.
	 * @param taker
	 * @param giver
	 * @param card
	 */
	protected void onTakeOpponentCard(SOCPlayer taker, SOCPlayer giver, Object card) {}
	
	private void takeOpponentCard(SOCPlayer taker, SOCPlayer giver) {
		assert (giver != taker);

		int[] develOrResource = { giver.getTotalDevelopment(DevelopmentCard.UNUSED), giver.getTotalResourceCount() };

		int cardType = Helper.chooseRandomFromSet(develOrResource, getRandom());
		if (cardType == 0) {
			// devel
			int[] develCards = new int[Helper.NUM_DEVELOPMENT_CARD_TYPES];
			for (DevelopmentCardType t : DevelopmentCardType.values()) {
				develCards[t.ordinal()] = giver.getNumDevelopment(t, DevelopmentCard.UNUSED);
			}

			int type = Helper.chooseRandomFromSet(develCards, getRandom());

			DevelopmentCardType card = DevelopmentCardType.values()[type];
			taker.addDevelopmentCard(card, DevelopmentCard.USABLE);
			giver.removeDevelopmentCard(card, DevelopmentCard.UNUSED);

			printinfo("Player " + taker.getPlayerNum() + " taking a " + card + " card from Player " + giver.getPlayerNum());
			onTakeOpponentCard(taker, giver, card);
		} else {
			// resource
			int[] resourceCards = new int[Helper.NUM_RESOURCE_TYPES];
			for (ResourceType t : ResourceType.values()) {
				resourceCards[t.ordinal()] = giver.getResourceCount(t);
			}

			int type = Helper.chooseRandomFromSet(resourceCards, getRandom());
			ResourceType card = ResourceType.values()[type];

			taker.incrementResource(card, 1);
			giver.incrementResource(card, -1);

			printinfo("Player " + taker.getPlayerNum() + " taking a " + card + " card from Player " + giver.getPlayerNum());
	        onTakeOpponentCard(taker, giver, card);
		}
	}
    
    public void initGame() {
        int i = 0;
        // setup
        assert (mNumPlayers > 1);
        assert (mPlayers != null);
        assert (mBoard != null);

        mLongestRoadPlayer = -1;
        mLargestArmyPlayer = -1;
        mStateStack.clear();
        mBoard.reset();
        mCurrentPlayer = getPlayerByPlayerNum(1);
        for (i=1; i<=getNumPlayers(); i++) {
            getPlayerByPlayerNum(i).reset();
        }
        resetOptions();
        initDeck();

        // Each player gets some randomly picked resources
        // According to the rules: each player is allowed to place 2
        // settlements
        // and 2 roads. To be the most fair, we cycle through the players
        // twice
        // in a pendumlum like manner (e.g 0-numP, then numP-0).
        // after the initial settlements and roads are placed, each player
        // gets
        // a 1 card for each resource their settlements are touching.
        //
        // push a bunch of states ...

        // TODO : Make this so the first player to choose is picked at
        // random
        // also need to consider the AI
        for (i = 0; i < mNumPlayers - 1; i++) {
            pushState(SOCState.POSITION_SETTLEMENT_NOCANCEL);
            pushState(SOCState.POSITION_ROAD_NOCANCEL);
            pushState(SOCState.NEXT_PLAYER);
        }

        // the last player picks again
        pushState(SOCState.POSITION_SETTLEMENT_NOCANCEL);
        pushState(SOCState.POSITION_ROAD_NOCANCEL);

        for (; i > 0; i--) {
            // the last player picks twice
            pushState(SOCState.POSITION_SETTLEMENT_NOCANCEL);
            pushState(SOCState.POSITION_ROAD_NOCANCEL);
            pushState(SOCState.PREV_PLAYER);
        }

        pushState(SOCState.POSITION_SETTLEMENT_NOCANCEL);
        pushState(SOCState.POSITION_ROAD_NOCANCEL);
        pushState(SOCState.DEAL_CARDS);
        pushState(SOCState.ROLL_DICE);
        
    }

    /**
     * 
     * @return
     */
    public boolean isGameOver() {
        for (int i=1; i<=getNumPlayers(); i++) {
            SOCPlayer player = getPlayerByPlayerNum(i);
            if (player.getPoints() >= mConfig.getInt(Key.WINNING_POINTS)) {
                onGameOver(player);
                return true;
            }
        }
        return false;
    }
    
    /**
     * Return true when it is valid to call run()
     * @return
     */
    public boolean canRun() {
        return this.mStateStack.size() > 0;
    }
    
    /**
     * Clear all states from the game.  Use caution when using as this invalidates the game.
     */
    protected void clearState() {
        mStateStack.clear();
    }
    
    /**
     * A game processing step.  Typically this method is called from a unique thread.  
     *   
     * 
     * @return true if run is valid, false otherwise
     */
	@SuppressWarnings("unchecked")
    public void runGame() {
    	    
		if (mBoard == null) {
			throw new RuntimeException("No board, cannot run game");
		}

		if (!mBoard.isInitialized()) {
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
		
		final SOCState state = getState();
		//if (Profiler.ENABLED) Profiler.push("SOC::runGame");
        //if (Profiler.ENABLED) Profiler.push("SOC::runGame[" + state + "]");
        try {
    		if (isGameOver()) {
    		    return;
    		}
    		
    		switch (state) {
    
    		case INIT_GAME: // transition state
                initGame();
    			break;
    			
    		case DEAL_CARDS: // transition state
    			dealCards();
    			popState();
    			break;
    
            case POSITION_SETTLEMENT_CANCEL: // wait state
    		case POSITION_SETTLEMENT_NOCANCEL: // wait state
    			if (mOptions == null) {
    				printinfo("Player " + getCurPlayerNum() + " place settlement");
    				setupSettlementOptions();
    			}
    
    			assert (mOptions != null);
    			SOCVertex v = mCurrentPlayer.chooseSettlementVertex(this, mOptions);
    
    			if (v != null) {
    				int vIndex = mBoard.getVertexIndex(v);
    				printinfo("Player " + getCurPlayerNum() + " placed a settlement on vertex " + vIndex);
    				assert (v.player == 0);
    				v.player = getCurPlayerNum();
                    updatePlayerRoadsBlocked(getBoard(), vIndex);
    
    				// need to re-eval the road lengths for all players that the new settlement
    				// may have affected.  Get the players to update first to avoid dups.
    				boolean [] playerNumsToCompute = new boolean[getNumPlayers()+1];
    				for (int ii=0; ii<v.getNumAdjacent(); ii++) {
    					int eIndex = mBoard.getEdgeIndex(vIndex, v.getAdjacent()[ii]);
    					SOCEdge e = mBoard.getEdge(eIndex);
    					if (e.getPlayer() > 0)
    						playerNumsToCompute[e.getPlayer()] = true;
    				}
    				
    				for (int ii=1; ii<playerNumsToCompute.length; ii++) {
    					if (playerNumsToCompute[ii]) {
    					    SOCPlayer p = getPlayerByPlayerNum(ii);
    						int len = mBoard.computeMaxRoadLengthForPlayer(ii);
    						p.setRoadLength(len);
    					}
    				}
    				
    				updateLongestRoadPlayer();
    				updatePlayerPoints();
    
    				popState();
    			}
    			break;
    
    		case POSITION_ROAD_NOCANCEL: // wait state
            case POSITION_ROAD_CANCEL: // wait state
    
    			if (mOptions == null) {
    				printinfo("Player " + getCurPlayerNum() + " place road");
    				setupRoadOptions();
    			}
    
    			assert (mOptions != null && mOptions.size() > 0);
    			SOCEdge edge = mCurrentPlayer.chooseRoadEdge(this, mOptions);
    
    			if (edge != null) {
    			    if (edge.player > 0) {
    			        throw new AssertionError();
    			    }
    				//assert (edge.player <= 0);
    				int eIndex = mBoard.getEdgeIndex(edge);
    				printinfo("Player " + getCurPlayerNum() + " placing a road on edge " + eIndex);
    				edge.player = getCurPlayerNum();
    				SOCPlayer p = getCurPlayer();
    				int len = mBoard.computeMaxRoadLengthForPlayer(p.getPlayerNum());
    				p.setRoadLength(len);
    				updateLongestRoadPlayer();
    				updatePlayerPoints();
    				popState();
    			}
    			break;
    
    		case POSITION_CITY: // wait state
    			if (mOptions == null) {
    				printinfo("Player " + getCurPlayerNum() + " place city");
    				setupCityOptions();
    			}
    
    			assert (mOptions != null);
    			v = mCurrentPlayer.chooseCityVertex(this, mOptions);
    
    			if (v != null) {
    				assert (v.player == getCurPlayerNum());
    				int vIndex = mBoard.getVertexIndex(v);
    				printinfo("Player getCurPlayerNum() placing a city at vertex " + vIndex);
    				assert (v.isCity == false);
    				v.isCity = true;
    				computePointsForPlayer(getCurPlayer());
    				popState();
    			}
    			break;
    
    		case NEXT_PLAYER: // transition state
    			incrementCurPlayer(1);
    			popState();
    			break;
    
    		case PREV_PLAYER: // transition state
    			incrementCurPlayer(-1);
    			popState();
    			break;
    
    		case ROLL_DICE: // wait state
    			// sanity check, each time we get here our state stack should be
    			// empty
    			if (mStateStack.size() > 1) {
    				logError("State stack not empty on roll dice");
    				dumpStack();
    			}
    
    			if (mCurrentPlayer.rollDice()) {
    				printinfo("Player " + getCurPlayerNum() + " is rolling the dice ...");
    				setState(SOCState.START_ROUND);
    			}
    			break;
    
    		case START_ROUND: // transition state
    		// init normal game action from here
    		// according to the rules each player rolls the die in order.
    		// after a die roll, resources are distributed and each player
    		// is allowed to take a turn. Unless the roll is a 7, in which case
    		// the current player gets to place the robber, take a card from another
    		// player of their choice, then all the players must give up half of
    		// their
    		// cards if they have more than 7 UNUSED cards.
    		{
    			// roll the dice
    			rollDice(getRandom());
    			if (getDiceNum() == 7) {
    				printinfo("Uh Oh, Player " + getCurPlayerNum() + " rolled a 7.");
    				// current player gets to position the robber
    				pushState(SOCState.POSITION_ROBBER_NOCANCEL);
    				pushState(SOCState.TAKE_OPPONENT_CARD);
    				pushState(SOCState.GIVE_UP_CARD_SETUP);
    			} else {
    				distributeResources();
    				// each player can build
    				for (i = 0; i < mNumPlayers; i++) {
    					pushState(SOCState.PLAYER_TURN);
    					pushState(SOCState.NEXT_PLAYER);
    				}
    				pushState(SOCState.NEXT_PLAYER);
    				pushState(SOCState.MAKE_DEVEL_USABLE);
    				pushState(SOCState.ROLL_DICE);
    			}
    			dumpStack();
    			popState();
                break;
    		}
    
    		case MAKE_DEVEL_USABLE: // transition state
    			getCurPlayer().setDevelopmentCardsUsable();
    			popState();
    			break;
    
    		case PLAYER_TURN: // wait state
    			if (mOptions == null) {
    				printinfo("Player " + getCurPlayerNum() + " choose move");
    				setupPlayerMoves();
    			}
    			assert (mOptions != null);
    			MoveType move = mCurrentPlayer.chooseMove(this, mOptions);
    			if (move != null) {
    				processMove(move);
    				resetOptions();
    			}
    			break;
    
    		case SHOW_TRADE_OPTIONS: // wait state
    			if (mOptions == null) {
    				printinfo("Player " + getCurPlayerNum() + " select trade option");
    				setupTradeOptions();
    			}
    			assert (mOptions != null);
    			final SOCTrade trade = mCurrentPlayer.chooseTradeOption(this, mOptions);
    			if (trade != null) {
    				printinfo("Player " + getCurPlayerNum() + " trades " + trade.getType() + " X " + trade.getAmount());
    				mCurrentPlayer.incrementResource(trade.getType(), -trade.getAmount());
    				popState();
    				this.mSaveTrade = trade;
    				pushStateFront(SOCState.TRADE_COMPLETED);
    				pushStateFront(SOCState.DRAW_RESOURCE_CANCEL, new UndoAction() {
    				    public void undo() {
    		                mCurrentPlayer.incrementResource(trade.getType(), trade.getAmount());
    		                popState();
    				    }
    				});
    			}
    			break;
    
            case POSITION_ROBBER_CANCEL: // wait state
    		case POSITION_ROBBER_NOCANCEL: // wait state
    			if (mOptions == null) {
    				printinfo("Player " + getCurPlayerNum() + " place robber");
    				setupRobberOptions();
    			}
    			SOCCell cell = mCurrentPlayer.chooseRobberCell(this, mOptions);
    
    			if (cell != null) {
    				int cellIndex = mBoard.getCellIndex(cell);
    				printinfo("Player " + getCurPlayerNum() + " placing robber on cell " + cellIndex);
    				mBoard.setRobber(cellIndex);
    				popState();
    			}
    			break;
    
    		case TAKE_OPPONENT_CARD: // wait state
    			if (mOptions == null) {
    				setupTakeOpponentCardOptions();
    			}
    			assert (mOptions != null);
    			if (mOptions.size() == 0) {
    				popState();
    			} else {
    				SOCPlayer player = mCurrentPlayer.choosePlayerNumToTakeCardFrom(this, mOptions);
    				if (player != null) {
    					assert (player != mCurrentPlayer);
    					assert (player.getPlayerNum() > 0);
    					takeOpponentCard(mCurrentPlayer, player);
    					popState();
    				}
    			}
    			break;
    
    		case DO_MONOPOLY: // wait state
    			ResourceType r = mCurrentPlayer.chooseResource(this);
    			if (r != null) {
    				processMonopoly(r);
    				popState();
    			}
    			break;
    
    		case DRAW_DEVELOPMENT_CARD: // transition state
    			pickDevelopmentFromDeck();
    			popState();
    			break;
    
    		case GIVE_UP_CARD_SETUP: // transition state
    		{
    			// on a 7, all players with >= MIN_PLAYER_CARDS_FOR_SURRENDER_ON_7 cards must give up half
    			SOCPlayer cur = mCurrentPlayer;
    			do {
    				int numCards = cur.getTotalCardsLeftInHand();
    				if (numCards > mConfig.getInt(Key.MIN_PLAYER_CARDS_FOR_SURRENDER_ON_7)) {
    					int numCardsToSurrender = numCards / 2;
    					printinfo("Player " + cur.getPlayerNum() + " must give up " + numCardsToSurrender + " of " + numCards + " cards");
    					cur.setNumCardsToSurrender(numCardsToSurrender);
    					for (int c = 0; c < numCardsToSurrender; c++)
    						pushState(SOCState.GIVE_UP_CARD);
    				}
    				pushState(SOCState.NEXT_PLAYER);
    				cur = cur.getNext();
    
    			} while (cur != mCurrentPlayer);
    			pushState(SOCState.NEXT_PLAYER);
    			pushState(SOCState.MAKE_DEVEL_USABLE);
    			pushState(SOCState.ROLL_DICE);
    			popState();
                break;
    		}
    
    		case GIVE_UP_CARD: // wait state
    			if (mOptions == null) {
    				setupGiveUpCardOptions();
    			}
    			assert (mOptions != null);
    			GiveUpCardOption card = mCurrentPlayer.chooseCardToGiveUp(this, mOptions, mCurrentPlayer.getNumCardsToSurrender());
    			if (card != null) {
    			    mCurrentPlayer.setNumCardsToSurrender(mCurrentPlayer.getNumCardsToSurrender()-1);
    			    processGiveUpCard(card);
    				popState();
    			}
    			break;
    
    		case DRAW_RESOURCE_NOCANCEL:
    		case DRAW_RESOURCE_CANCEL: // wait state
    			r = mCurrentPlayer.chooseResource(this);
    			if (r != null) {
    				printinfo("Player " + getCurPlayerNum() + " draws a " + r + " resource card");
    				mCurrentPlayer.incrementResource(r, 1);
    				popState();
    			}
    			break;
    			
    		case TRADE_COMPLETED:
    		    onTradeCompleted(getCurPlayer(), mSaveTrade);
    		    popState();
    		    break;
    		}
            dumpStack();
	    } finally {
	        //if (Profiler.ENABLED) Profiler.pop("SOC::runGame[" + state + "]");
            //if (Profiler.ENABLED) Profiler.pop("SOC::runGame");
	    }
	}

	/**
	 * Called when a trade completed as event for the user to handle if they wish
	 * base method does nothing.
	 * @param player
	 * @param trade
	 */
	protected void onTradeCompleted(SOCPlayer player, SOCTrade trade) {}
	
	private void setState(SOCState State) {
		if (!mStateStack.isEmpty())
			mStateStack.remove(0);
		pushStateFront(State);
	}
    
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
		
		UndoAction undoAction = getUndoAction();
		if (undoAction != null) {
		    undoAction.undo();
		} 
		
		popState();
	}

	/**
     * Called when a player get the longest road or overtakes another player.
     * default method does nothing
     * @param oldPlayer null if newPlayer is the first to get the longest road
     * @param newPlayer player that has the longest road
     * @param armySize
	 */
	protected void onLongestRoadPlayerUpdated(SOCPlayer oldPlayer, SOCPlayer newPlayer, int maxRoadLen) {}
	private void updateLongestRoadPlayer() {
	    int pNum = computeLongestRoadPlayer(this);
	    if (pNum < 1) {
	        if (mLongestRoadPlayer > 0) {
	            printinfo("Player " + mLongestRoadPlayer + " is blocked and has lost the longest road!");
	            onLongestRoadPlayerUpdated(getPlayerByPlayerNum(mLongestRoadPlayer), null, 0);
	        }
	        mLongestRoadPlayer = -1;
	        return;
	    }
        if (pNum == mLongestRoadPlayer)
            return;
        final SOCPlayer currentLongestRoadPlayer = getPlayerByPlayerNum(getLongestRoadPlayerNum());
        final SOCPlayer maxRoadLenPlayer = getPlayerByPlayerNum(pNum);
        final int maxRoadLen = maxRoadLenPlayer.getRoadLength();
        
        if (mLongestRoadPlayer < 0) {
            printinfo("Player " + maxRoadLenPlayer.getPlayerNum() + " has gained the Longest Road!");
            onLongestRoadPlayerUpdated(null, maxRoadLenPlayer, maxRoadLen);
        } else if (maxRoadLenPlayer.getRoadLength() > currentLongestRoadPlayer.getRoadLength()) {
            printinfo("Player " + maxRoadLenPlayer.getPlayerNum() + " has overtaken Player " + mLongestRoadPlayer + " with the Longest Road!");
            onLongestRoadPlayerUpdated(currentLongestRoadPlayer, maxRoadLenPlayer, maxRoadLen);
        }
        
        setLongestRoadPlayer(maxRoadLenPlayer.getPlayerNum());

	}
	
	/**
	 * Called when a player road is blocked and some of their roads will be removed.  Only used when Config.ENABLE_ROAD_BLOCK enabled.
	 * @param player
	 * @param road
	 */
	protected void onPlayerRoadBlocked(SOCPlayer player, SOCEdge road) {}
	public void updatePlayerRoadsBlocked(SOCBoard board, int vertexIndex) {
	    if (mConfig.getInt(Key.ENABLE_ROAD_BLOCK) != 1)
	        return;
	    SOCVertex vertex = board.getVertex(vertexIndex);
        for (int i=0; i<vertex.getNumAdjacent(); i++) {
            final SOCEdge edge = board.getEdge(vertexIndex, vertex.getAdjacent()[i]);
            int playerNum = edge.getPlayer();
            if (playerNum == 0)
                continue;
            if (playerNum == getCurPlayerNum())
                continue;
            BlockRoadsVisitor visitor = new BlockRoadsVisitor(vertexIndex, playerNum, BlockRoadsVisitor.MODE_SEARCH);
            board.walkEdgeTree(vertex.getAdjacent()[i], visitor);
            if (visitor.num > 0) {
                // if ANY will be removed, then rewalk the tree and remove
                visitor = new BlockRoadsVisitor(vertexIndex, playerNum, BlockRoadsVisitor.MODE_REMOVE);
                board.walkEdgeTree(vertex.getAdjacent()[i], visitor);
                board.getEdge(vertexIndex, vertex.getAdjacent()[i]).setPlayer(0);
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
        public boolean visit(SOCEdge e, int depth) {
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
                e.setPlayer(0);
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
		int maxRoadLen = soc.mConfig.getInt(Key.MIN_LONGEST_ROAD_LEN) - 1;		
		if (soc.getLongestRoadPlayerNum() > 0)
		    maxRoadLen = Math.max(maxRoadLen, soc.getPlayerByPlayerNum(soc.getLongestRoadPlayerNum()).getRoadLength());
		SOCPlayer maxRoadLenPlayer = soc.getPlayerByPlayerNum(soc.getLongestRoadPlayerNum());
		
		SOCPlayer cur = soc.mPlayers;
		do {
			int len = cur.getRoadLength();
			if (len > maxRoadLen) {
				maxRoadLen = len;
				maxRoadLenPlayer = cur;
			}
			cur = cur.getNext();
		} while (cur != soc.mPlayers);

		if (maxRoadLenPlayer == null)
		    return -1;
			//return soc.getLongestRoadPlayerNum();
		
		if (maxRoadLenPlayer.getRoadLength() >= soc.mConfig.getInt(Key.MIN_LONGEST_ROAD_LEN))
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
	protected void onLargestArmyPlayerUpdated(SOCPlayer oldPlayer, SOCPlayer newPlayer, int armySize) {}
	private void updateLargestArmyPlayer() {
	    int pNum = computeLargestArmyPlayer(this);
	    if (pNum < 1) {
	        mLargestArmyPlayer = -1;
	        return;
	    }
	    if (pNum == this.mLargestArmyPlayer)
	        return;

        SOCPlayer maxArmyPlayer = getPlayerByPlayerNum(pNum);
	    final int maxArmySize = maxArmyPlayer.getArmySize();
        final SOCPlayer currentLargestArmyPlayer = getPlayerByPlayerNum(mLargestArmyPlayer);
	    if (mLargestArmyPlayer < 0) {
	        printinfo("Player " + maxArmyPlayer.getPlayerNum() + " has the largest army!");
	        onLargestArmyPlayerUpdated(null, maxArmyPlayer, maxArmySize);
	    } else if (maxArmyPlayer.getArmySize() > currentLargestArmyPlayer.getArmySize()) {
	        printinfo("Player " + maxArmyPlayer.getPlayerNum() + " takes overtakes Player " + mLargestArmyPlayer + " for the largest Army!");
	        onLargestArmyPlayerUpdated(currentLargestArmyPlayer, maxArmyPlayer, maxArmySize);
	    }

	    setLargestArmyPlayer(maxArmyPlayer.getPlayerNum());
	    updatePlayerPoints();
	}
	
	/**
	 * compute the player who should have the largest army.
	 * soc is not changed.
	 * @param soc
	 * @return
	 */
	public static int computeLargestArmyPlayer(SOC soc) {
		int maxArmySize = soc.mConfig.getInt(Key.MIN_LARGEST_ARMY_SIZE) - 1;
		if (soc.getLargestArmyPlayerNum() > 0)
		    maxArmySize = soc.getPlayerByPlayerNum(soc.getLargestArmyPlayerNum()).getArmySize();
		SOCPlayer maxArmyPlayer = null;
		SOCPlayer cur = soc.mCurrentPlayer;
		do {
			if (cur.getArmySize() > maxArmySize) {
				maxArmySize = cur.getArmySize();
				maxArmyPlayer = cur;
			}
			cur = cur.getNext();
		} while (cur != soc.mCurrentPlayer);

		if (maxArmyPlayer == null)
			return soc.getLargestArmyPlayerNum();
		
		if (maxArmyPlayer.getArmySize() >= soc.mConfig.getInt(Key.MIN_LARGEST_ARMY_SIZE))
		    return maxArmyPlayer.getPlayerNum();
		
		return -1;
	}

	private void setupSettlementOptions() {
		assert (mOptions == null);
		mOptions = computeSettlementOptions(getCurPlayer(), mBoard);
	}
	
	/**
	 * Return a list of vertices available for a settlement given a player and board instance.
	 * 
	 * @param p
	 * @param b
	 * @return
	 */
	static public List<Integer> computeSettlementOptions(SOCPlayer p, SOCBoard b) {

		// build an array of vertices legal for the current player
		// to place a settlement.
		List<Integer> vertices = new ArrayList<Integer>();
		for (int i = 0; i < b.getNumVerts(); i++) {
			SOCVertex v = b.getVertex(i);
			if (v.player != 0)
				continue;
			if (!v.canPlaceStructure)
				continue;
			boolean canAdd = true;
			boolean isOnRoad = false;
			for (int ii = 0; ii < v.numAdj; ii++) {
				int iv = b.findAdjacentVertex(i, ii);
				if (iv >= 0) {
					SOCVertex v2 = b.getVertex(iv);
					if (v2.player != 0) {
						canAdd = false;
						break;
					}

					int ie = b.getEdgeIndex(i, iv);
					if (ie >= 0) {
						SOCEdge e = b.getEdge(ie);
						if (e.player == p.getPlayerNum()) {
							isOnRoad = true;
						}
					}
				}
			}

			if (!canAdd)
				continue;

			if (isOnRoad || b.getNumStructuresForPlayer(p) < 2) {
				vertices.add(i);
			}
		}
		return vertices;
	}

	private void setupRoadOptions() {
		assert (mOptions == null);
		mOptions = computeRoadOptions(getCurPlayer(), mBoard);
	}
	
	/**
	 * Return a list of edges available for a road given a player and board instance.  
	 * @param p
	 * @param b
	 * @return
	 */
	static public List<Integer> computeRoadOptions(SOCPlayer p, SOCBoard b) {
        //if (Profiler.ENABLED) Profiler.push("SOC::computeRoadOptions");
        try {
    	    
    	    List<Integer> edges = new ArrayList<Integer>();
    		for (int i = 0; i < b.getNumEdges(); i++) {
    			if (b.isEdgeAvailableForRoad(i, p))
    				edges.add(i);
    		}
    		return edges;
        } finally {
            //if (Profiler.ENABLED) Profiler.pop("SOC::computeRoadOptions");
        }
	}

	private void setupCityOptions() {
		assert (mOptions == null);
		mOptions = computeCityOptions(getCurPlayer(), mBoard);
	}
	
	/**
	 * Return a list of vertices available for a city given a player and board intance.
	 * @param p
	 * @param b
	 * @return
	 */
	static public List<Integer> computeCityOptions(SOCPlayer p, SOCBoard b) {
		List<Integer> vertices = new ArrayList<Integer>();
		for (int i = 0; i < b.getNumVerts(); i++) {
			SOCVertex v = b.getVertex(i);
			if (v.player == p.getPlayerNum() && !v.isCity) {
				vertices.add(i);
			}
		}
		return vertices;
	}

	private void setupPlayerMoves() {
		assert (mOptions == null);
		SOCPlayer player = getCurPlayer();
		mOptions = computeMoveOptions(player, mBoard);
	}
	
	/**
	 * Return a list of MoveTypes available given a player and board instance.
	 * @param p
	 * @param b
	 * @return
	 */
	static public List<MoveType> computeMoveOptions(SOCPlayer p, SOCBoard b) {
		List<MoveType> types = new ArrayList<MoveType>();
		
		if (p.canBuild(BuildableType.City) && b.getNumSettlementsForPlayer(p) > 0)
		    types.add(MoveType.BUILD_CITY);

		if (p.canBuild(BuildableType.Development))
		    types.add(MoveType.DRAW_DEVELOPMENT);

		if (p.getNumDevelopment(DevelopmentCardType.Soldier, DevelopmentCard.USABLE) > 0)
		    types.add(MoveType.SOLDIER_CARD);

		if (p.getNumDevelopment(DevelopmentCardType.YearOfPlenty, DevelopmentCard.USABLE) > 0)
		    types.add(MoveType.YEAR_OF_PLENTY_CARD);

		if (p.getNumDevelopment(DevelopmentCardType.RoadBuilding, DevelopmentCard.USABLE) > 0)
		    types.add(MoveType.ROAD_BUILDING_CARD);

		if (p.getNumDevelopment(DevelopmentCardType.Monopoly, DevelopmentCard.USABLE) > 0)
		    types.add(MoveType.MONOPOLY_CARD);

		if (p.getNumDevelopment(DevelopmentCardType.Victory, DevelopmentCard.USABLE) > 0)
		    types.add(MoveType.VICTORY_CARD);

		if (canPlayerTrade(p, b))
		    types.add(MoveType.TRADE);
		
		if (p.canBuild(BuildableType.Settlement)) {
			for (int i = 0; i < b.getNumVerts(); i++) {
				if (b.isVertexAvailbleForSettlement(i) && b.isVertexAdjacentToRoad(i, p)) {
					types.add(MoveType.BUILD_SETTLEMENT);
					break;
				}
			}
		}

		if (p.canBuild(BuildableType.Road)) {
			for (int i=0; i<b.getNumEdges(); i++) {
				if (b.isEdgeAvailableForRoad(i, p)) {
					types.add(MoveType.BUILD_ROAD);
					break;
				}
			}
			
		}

        types.add(MoveType.CONTINUE);

		return types;
	}

	private void setupRobberOptions() {
		assert (mOptions == null);
		mOptions = computeRobberOptions(mBoard);
	}
	
	/**
	 * Return a list of cells available for a robber given a board instance.
	 * @param b
	 * @return
	 */
	static public List<Integer> computeRobberOptions(SOCBoard b) {
		List<Integer> cellIndices = new ArrayList<Integer>();
		boolean desertIncluded = false;
		for (int i = 0; i < b.getNumCells(); i++) {
			SOCCell cell = b.getCell(i);
			switch (cell.getType()) {
			case DESERT:
			    if (desertIncluded)
			        break; // optimization: only add 1 
			    desertIncluded = true;
			case RESOURCE:
				cellIndices.add(i);
				break;
			}
		}
		return cellIndices;
	}

	private void setupGiveUpCardOptions() {
		assert (mOptions == null);
		mOptions = computeGiveUpCardOptions(getCurPlayer());
	}
	
	/**
	 * Return the list of unused resource and development cards a player can give up.
	 * @param p
	 * @return
	 */
	static public List<GiveUpCardOption> computeGiveUpCardOptions(SOCPlayer p) {
		List<GiveUpCardOption> options = new ArrayList<GiveUpCardOption>();
		for (ResourceType r : ResourceType.values()) {
			if (p.getResourceCount(r) > 0) {
				options.add(r.getGiveUpCardOption());
			}
		}
		for (DevelopmentCardType t : DevelopmentCardType.values()) {
			if (p.getNumDevelopment(t, DevelopmentCard.UNUSED) > 0) {
				options.add(t.getGiveUpCardOption());
			}
		}
		return options;
	}

	/**
	 * Compute all the trade options given a player and board instance.
	 * @param p
	 * @param b
	 * @return
	 */
	static public List<SOCTrade> computeTradeOptions(SOCPlayer p, SOCBoard b) {
		List<SOCTrade> trades = new ArrayList<SOCTrade>();
		computeTradeOptions(p, b, trades, 100);
		return trades;
	}
	
	static private void computeTradeOptions(SOCPlayer p, SOCBoard b, List<SOCTrade> trades, int maxOptions) {
		
        //if (Profiler.ENABLED) Profiler.push("SOC::computeTradeOptions");
	    
        try {
    	    int i;
    		int numOptions = 0;
    
    		boolean[] found = new boolean[Helper.NUM_RESOURCE_TYPES];
    
    		// see if we have a 2:1 trade option
    		for (i = 0; i < b.getNumCells(); i++) {
    			SOCCell cell = b.getCell(i);
    			if (cell.getType() != SOCCellType.PORT_RESOURCE)
    				continue;
    
    			if (found[cell.getResource().ordinal()])
    				continue;
    
    			if (!b.isPlayerAdjacentToCell(p, i))
    				continue;
    
    			if (p.getResourceCount(cell.getResource()) < 2)
    				continue;
    
    			trades.add(new SOCTrade(cell.getResource(), 2));
    			found[cell.getResource().ordinal()] = true;
    
    			if (numOptions >= maxOptions)
    				return;
    		}
    
    		// we have a 3:1 trade option when we are adjacent to a PORT_MULTI
    		for (i = 0; i < b.getNumCells(); i++) {
    			SOCCell cell = b.getCell(i);
    			if (cell.getType() != SOCCellType.PORT_MULTI)
    				continue;
    
    			if (!b.isPlayerAdjacentToCell(p, i))
    				continue;
    
    			// for (int r=0; r<Helper.NUM_RESOURCE_TYPES; r++) {
    			for (ResourceType r : ResourceType.values()) {
    				if (!found[r.ordinal()] && p.getResourceCount(r) >= 3) {
    					trades.add(new SOCTrade(r, 3));
    					found[r.ordinal()] = true;
    					if (numOptions >= maxOptions)
    						return;
    				}
    			}
    		}
    
    		// look for 4:1 trades
    		for (ResourceType r : ResourceType.values()) {
    
    			if (!found[r.ordinal()] && p.getResourceCount(r) >= 4) {
    				trades.add(new SOCTrade(r, 4));
    
    				if (numOptions >= maxOptions)
    					return;
    			}
    		}
        } finally {
            //if (Profiler.ENABLED) Profiler.pop("SOC::computeTradeOptions");
        }
	}

	/**
	 * 
	 * @param p
	 * @param b
	 * @return
	 */
	static public boolean canPlayerTrade(SOCPlayer p, SOCBoard b) {
		List<SOCTrade> options = new ArrayList<SOCTrade>();
		computeTradeOptions(p, b, options, 1);
		return options.size() > 0;
	}

	private void setupTradeOptions() {
		assert (mOptions == null);
		mOptions = computeTradeOptions(getCurPlayer(), mBoard);
	}

	/**
	 * Get a new array of all players
	 * @return
	 */
	public SOCPlayer [] getAllPlayers() {
	    SOCPlayer [] players = new SOCPlayer[getNumPlayers()+1];
	    SOCPlayer p = getCurPlayer();
	    for (int i=0; p!=null && i<getNumPlayers(); i++) {
	        players[p.getPlayerNum()] = p;
	        p = p.getNext();
	    }
	    return players;
	}
	
	private void setupTakeOpponentCardOptions() {
		assert (mOptions == null);
		mOptions = computeTakeOpponentCardOptions(getAllPlayers(), getCurPlayer(), mBoard);
	}
	
	/**
	 * Compute the list of players from which 'p' can take a card 
	 * @param p
	 * @param b
	 * @return
	 */
	public List<SOCPlayer> computeTakeOpponentCardOptions(SOCPlayer p, SOCBoard b) {
	    return computeTakeOpponentCardOptions(getAllPlayers(), p, b);
	}
	
	/**
	 * 
	 * @param players
	 * @param p
	 * @param b
	 * @return
	 */
	static public List<SOCPlayer> computeTakeOpponentCardOptions(SOCPlayer [] players, SOCPlayer p, SOCBoard b) {
		
	    List<SOCPlayer> choices = new ArrayList<SOCPlayer>();
		boolean [] playerNums = new boolean[players.length];
		
		SOCCell cell = b.getCell(b.getRobberCell());
		for (int vIndex : cell.getAdjVerts()) {
			SOCVertex v = b.getVertex(vIndex);
			if (v.player == 0)
				continue;
			if (v.player == p.getPlayerNum())
				continue;
			if (players[v.player].getTotalCardsLeftInHand() <= 0)
				continue;
			playerNums[v.player] = true;
		}
		
		for (int i=1; i<playerNums.length; i++)
			if (playerNums[i])
				choices.add(players[i]);
		return choices;
	}

	/**
	 * Not recommended to use as this function modifies player and this data.
	 * call runGame until returns true to process
	 * @param move
	 */
	protected void processMove(MoveType move) {
	    
        printinfo("Player " + getCurPlayerNum() + " choose move " + move);
		switch (move) {
		case BUILD_ROAD:
			getCurPlayer().adjustResourcesForBuildable(BuildableType.Road, -1);
			pushStateFront(SOCState.POSITION_ROAD_CANCEL, new UndoAction() {
			    public void undo() {
                    getCurPlayer().adjustResourcesForBuildable(BuildableType.Road, 1);
			    }
			});
			break;

		case BUILD_SETTLEMENT:
			getCurPlayer().adjustResourcesForBuildable(BuildableType.Settlement, -1);
			pushStateFront(SOCState.POSITION_SETTLEMENT_CANCEL, new UndoAction() {
                public void undo() {
                    getCurPlayer().adjustResourcesForBuildable(BuildableType.Settlement, 1);
                }
            });
			break;

		case BUILD_CITY:
			getCurPlayer().adjustResourcesForBuildable(BuildableType.City, -1);
			pushStateFront(SOCState.POSITION_CITY, new UndoAction() {
                public void undo() {
                    getCurPlayer().adjustResourcesForBuildable(BuildableType.City, 1);
                }
            });
			break;

		case DRAW_DEVELOPMENT:
			getCurPlayer().adjustResourcesForBuildable(BuildableType.Development, -1);
			pushStateFront(SOCState.DRAW_DEVELOPMENT_CARD, new UndoAction() {
                public void undo() {
                    getCurPlayer().adjustResourcesForBuildable(BuildableType.Development, 1);
                }
            });
			break;

		case MONOPOLY_CARD:
			getCurPlayer().useDevelopmentCard(DevelopmentCardType.Monopoly);
			mDeck[DevelopmentCardType.Monopoly.ordinal()] ++; // put card back in deck
			pushStateFront(SOCState.DO_MONOPOLY, new UndoAction() {
			    public void undo() {
		            getCurPlayer().unUseDevelopmentCard(DevelopmentCardType.Monopoly);
		            mDeck[DevelopmentCardType.Monopoly.ordinal()] --; // put card back in deck
			    }
			});
			break;

		case YEAR_OF_PLENTY_CARD:
			getCurPlayer().useDevelopmentCard(DevelopmentCardType.YearOfPlenty);
			mDeck[DevelopmentCardType.YearOfPlenty.ordinal()] ++; // put card back in deck
			pushStateFront(SOCState.DRAW_RESOURCE_NOCANCEL);
            pushStateFront(SOCState.DRAW_RESOURCE_CANCEL, new UndoAction() {
                public void undo() {
                    getCurPlayer().unUseDevelopmentCard(DevelopmentCardType.YearOfPlenty);
                    mDeck[DevelopmentCardType.YearOfPlenty.ordinal()] --; // take card back out of deck
                    popState();                    
                }
            });
			break;

		case ROAD_BUILDING_CARD:
			getCurPlayer().useDevelopmentCard(DevelopmentCardType.RoadBuilding);
			mDeck[DevelopmentCardType.RoadBuilding.ordinal()] ++; // put card back in deck
			pushStateFront(SOCState.POSITION_ROAD_NOCANCEL);
			// can cancel on the first road option, not the second
			pushStateFront(SOCState.POSITION_ROAD_CANCEL, new UndoAction() {
			    public void undo() {
		            getCurPlayer().unUseDevelopmentCard(DevelopmentCardType.RoadBuilding);
		            mDeck[DevelopmentCardType.RoadBuilding.ordinal()] --; // put card back in deck
		            popState(); // pop an extra state since we push the NOCANCEL
			    }
			});

			break;

		case VICTORY_CARD:
			getCurPlayer().useDevelopmentCard(DevelopmentCardType.Victory);
			computePointsForPlayer(getCurPlayer());
			resetOptions();
			break;

		case SOLDIER_CARD:
			getCurPlayer().useDevelopmentCard(DevelopmentCardType.Soldier);
			updateLargestArmyPlayer();
			pushStateFront(SOCState.TAKE_OPPONENT_CARD);
			pushStateFront(SOCState.POSITION_ROBBER_CANCEL, new UndoAction() {
			    public void undo() {
		            getCurPlayer().unUseDevelopmentCard(DevelopmentCardType.Soldier);
		            updateLargestArmyPlayer();
		            popState();
			    }
			});
			break;

		case CONTINUE:
			popState();
			break;

		case TRADE:
			pushStateFront(SOCState.SHOW_TRADE_OPTIONS);
			break;

		default:
			assert (false);// && "Unhandled case");
		}
		dumpStack();
	}

	/**
	 * Called when a player takes some resource from another player.  can be called multiple times
	 * in a turn.  default method does nothing.
	 * @param taker
	 * @param giver
	 * @param type
	 * @param amount
	 */
	protected void onMonopolyCardApplied(SOCPlayer taker, SOCPlayer giver, ResourceType type, int amount) {}
	private void processMonopoly(ResourceType type) {
		// take the specified resource from all other players
		SOCPlayer cur = mCurrentPlayer.getNext();
		while (cur != mCurrentPlayer) {
			int num = cur.getResourceCount(type);
			if (num > 0) {
				printinfo("Player " + getCurPlayerNum() + " takes " + num + " " + type + " card from player " + cur.getPlayerNum());
				onMonopolyCardApplied(mCurrentPlayer, cur, type, num);
				cur.incrementResource(type, -num);
				mCurrentPlayer.incrementResource(type, num);
			}
			cur = cur.getNext();
		}
	}

	private void processGiveUpCard(GiveUpCardOption option) {
		DevelopmentCardType card = null;
		switch (option) {
		case GIVEUP_WOOD_CARD:
			mCurrentPlayer.incrementResource(ResourceType.Wood, -1);
			break;
		case GIVEUP_SHEEP_CARD:
			mCurrentPlayer.incrementResource(ResourceType.Sheep, -1);
			break;
		case GIVEUP_ORE_CARD:
			mCurrentPlayer.incrementResource(ResourceType.Ore, -1);
			break;
		case GIVEUP_WHEAT_CARD:
			mCurrentPlayer.incrementResource(ResourceType.Wheat, -1);
			break;
		case GIVEUP_BRICK_CARD:
			mCurrentPlayer.incrementResource(ResourceType.Brick, -1);
			break;

		// TODO: smarter way to do this with ENUM
		case GIVEUP_MONOPOLY_CARD:
			card = DevelopmentCardType.Monopoly;
			break;
		case GIVEUP_YEAR_OF_PLENTY_CARD:
			card = DevelopmentCardType.YearOfPlenty;
			break;
		case GIVEUP_ROAD_BUILDING_CARD:
			card = DevelopmentCardType.RoadBuilding;
			break;
		case GIVEUP_VICTORY_CARD:
			card = DevelopmentCardType.Victory;
			break;
		case GIVEUP_SOLDIER_CARD:
			card = DevelopmentCardType.Soldier;
			break;

		default:
			assert (false);// && "Unhandled case");
		}
		
		if (card != null) {
			if (mCurrentPlayer.getNumDevelopment(card, DevelopmentCard.NOT_USABLE) > 0) {
				mCurrentPlayer.removeDevelopmentCard(card, DevelopmentCard.NOT_USABLE);
			} else {
				mCurrentPlayer.removeDevelopmentCard(card, DevelopmentCard.USABLE);
			}
			mDeck[card.ordinal()] ++; // put card back in deck
		}
	}
	
	/**
	 * Called when game over is detected
	 */
	protected void onGameOver(SOCPlayer winner) {
	    
	}

    

}
