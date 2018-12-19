package cc.game.soc.core;

import java.util.*;

import cc.lib.game.Utils;
import cc.lib.utils.Reflector;

/**
 * 
 * @author Chris Caron
 *
 */
public abstract class Player extends Reflector<Player> {

	static {
		addAllFields(Player.class);
	}
	
	private final Vector<Card>		cards = new Vector<Card>();
	private int						playerNum	= 0; // player numbering starts at 1 
	private int						roadLength	= 0;	
	private int						points = 0;
	private int						harborPoints = 0;
	private int						numDiscoveredTerritories = 0;
	private final int []	        cityDevelopment = new int[DevelopmentArea.values().length];
	private Card					merchantFleetTradable;

	protected Player(Player other) {
	    cards.addAll(other.cards);
	    playerNum = other.playerNum;
	    roadLength = other.roadLength;
	    points = other.points;
	    harborPoints = other.harborPoints;
	    numDiscoveredTerritories = other.numDiscoveredTerritories;
	    System.arraycopy(other.cityDevelopment, 0, cityDevelopment, 0, cityDevelopment.length);
    }

	/**
	 * 
	 */
	protected Player() {
	}
	
	Player(int playerNum) {
	    setPlayerNum(playerNum);
	}
	/*
	@Override
    public final String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(getName());
        synchronized (cards) {
            buf.append(" cards:").append(cards);
        }
        buf.append(" roadLen:").append(roadLength);
        buf.append(" points:").append(points);
        buf.append(" harbor pts:").append(harborPoints);
        buf.append(" num discovered territories:").append(numDiscoveredTerritories);
        for (DevelopmentArea area : DevelopmentArea.values()) {
        	if (cityDevelopment[area.ordinal()] > 0) {
        		buf.append(" " + area.name() + ":").append(cityDevelopment[area.ordinal()]);
        	}
        }
        if (merchantFleetTradable != null) {
        	buf.append(" merchantFleet:" + merchantFleetTradable);
        }
        return buf.toString();
    }*/
    
	/**
	 * set all un-usable cards to usable or usabel cards to unusable 
	 */
	public final List<Card> setCardsUsable(CardType type, boolean usable) {
		List<Card> modified = new ArrayList<Card>();
		synchronized (cards) {
            for (Card card : cards) {
                if (card.isUsed())
                    continue;
                if (card.getCardType() != type)
                    continue;
                if (usable && !card.isUsable()) {
                    card.setUsable();
                    modified.add(card);
                } else if (!usable && card.isUsable()) {
                    card.setUsable();
                    modified.add(card);
                }
            }
        }
		return modified;
	}

	/**
	 * 
	 * @param card
	 */
	public final void addCard(Card card) {
	    synchronized (cards) {
            cards.add(card);
            Collections.sort(cards);
        }
	}
	
	/**
	 * 
	 * @param type
	 */
	public final void addCard(ICardType<?> type) {
	    synchronized (cards) {
            cards.add(new Card(type, type.defaultStatus()));//CardStatus.USABLE));
            Collections.sort(cards);
        }
	}
	
	/**
	 * 
	 * @param type
	 * @param num
	 */
	public final void addCards(ICardType<?> type, int num) {
		for (int i=0; i<num; i++)
			addCard(new Card(type, type.defaultStatus()));
	}

	/**
	 * 
	 * @param type
	 * @param num
	 */
	public final void removeCards(ICardType<?> type, int num) {
		for (int i=0; i<num; i++)
			removeCard(type);
	}

	/**
	 * 
	 * @return
	 */
	public final List<Card> getUnusedProgressCards() {
		return getUnusedCards(CardType.Progress);
	}
	
	/**
	 * Return list of unused resource and commodity cards
	 * @return
	 */
	public final List<Card> getUnusedBuildingCards() {
		List<Card> cards = getUnusedCards(CardType.Resource);
		cards.addAll(getUnusedCards(CardType.Commodity));
		return cards;
	}
	
	/**
	 * 
	 * @param buildable
	 * @param multiple
	 */
	public final void adjustResourcesForBuildable(BuildableType buildable, int multiple) {

		for (ResourceType t : ResourceType.values()) {
			incrementResource(t, buildable.getCost(t) * multiple);
		} 
	}
	
	/**
	 * 
	 */
	public void reset() {
	    synchronized (cards) {
            cards.clear();
        }
		roadLength=0;
		points = 0;
		harborPoints = 0;
		numDiscoveredTerritories = 0;
		merchantFleetTradable = null;
		Utils.fillArray(cityDevelopment, 0);
	}
	
	/**
	 * 
	 * @return
	 */
	public final Iterable<Card> getCards() {
	    synchronized (cards) {
            return Collections.unmodifiableList(cards);
        }
	}
	
	/**
	 * 
	 * @param type
	 * @return
	 */
	public final List<Card> getCards(CardType type) {
		List<Card> list = new ArrayList<Card>();
		synchronized (cards) {
            for (Card card : cards) {
                if (card.getCardType() == type) {
                    list.add(card);
                }
            }
        }
		return list;
	}
	
	public final List<Card> getUniqueCards(CardType type) {
		boolean [] found = new boolean[type.getCount()];
		List<Card> list = new ArrayList<Card>();
		for (Card card : cards) {
			if (card.getCardType() == type && !found[card.getTypeOrdinal()]) {
				list.add(card);
				found[card.getTypeOrdinal()] = true;
			}
		}
		return list;
	}
	
	/**
	 * 
	 * @param type
	 * @return
	 */
	public final List<Card> getUsableCards(ICardType<?> type) {
		List<Card> list = new ArrayList<Card>();
		synchronized (cards) {
            for (Card card : cards) {
                if (card.isUsable() && card.equals(type)) {
                    list.add(card);
                }
            }
        }
		return list;
	}

	/**
	 * 
	 * @param type
	 * @return
	 */
	public final List<Card> getUsableCards(CardType type) {
		List<Card> list = new ArrayList<Card>();
		synchronized (cards) {
            for (Card card : cards) {
                if (card.isUsable() && card.equals(type)) {
                    list.add(card);
                }
            }
        }
		return list;
	}

	/**
	 * 
	 * @param type
	 * @return
	 */
	public final List<Card> getUnusedCards(CardType type) {
		List<Card> list = new ArrayList<Card>();
		synchronized (cards) {
            for (Card card : cards) {
                if (card.getCardType() == type && !card.isUsed()) {
                    list.add(card);
                }
            }
        }
		return list;
	}
	
	/**
	 * 
	 * @return
	 */
	public final List<Card> getUnusedCards() {
		List<Card> list = new ArrayList<Card>();
		synchronized (cards) {
            for (Card card : cards) {
                if (!card.isUsed()) {
                    list.add(card);
                }
            }
        }
		return list;
	}

	/**
	 * 
	 * @return
	 */
	public final Card getMerchantFleetTradable() {
		return merchantFleetTradable;
	}

	/**
	 * 
	 * @param merchantFleetTradable
	 */
	public final void setMerchantFleetTradable(Card merchantFleetTradable) {
		this.merchantFleetTradable = merchantFleetTradable;
	}

	/**
	 * 
	 * @param type
	 * @return
	 */
	public final int getCardCount(CardType type) {
		int num = 0;
		synchronized (cards) {
            for (Card card : cards) {
                if (card.getCardType() == type)
                    num++;
            }
        }
		return num;
	}

	public List<Card> getGiftableCards() {
	    List<Card> giftable = new ArrayList<>();
	    giftable.addAll(getCards(CardType.Commodity));
        giftable.addAll(getCards(CardType.Progress));
        giftable.addAll(getCards(CardType.Resource));
        giftable.addAll(getCards(CardType.Development));
        return giftable;
    }

	/**
	 * 
	 * @param type
	 * @param status
	 * @return
	 */
	public final int getCardCount(CardType type, CardStatus status) {
		int num = 0;
		synchronized (cards) {
            for (Card card : cards) {
                if (card.getCardType() == type && card.getCardStatus() == status)
                    num++;
            }
        }
		return num;
	}

	/**
	 * 
	 * @param type
	 * @return
	 */
	public final int getUnusableCardCount(CardType type) {
		int num = 0;
		synchronized (cards) {
            for (Card card : cards) {
                if (card.getCardType() == type && !card.isUsable()) ;
                num++;
            }
        }
		return num;
	}

	/**
	 * 
	 * @param type
	 * @return
	 */
	public final int getCardCount(Card type) {
		int num = 0;
		synchronized (cards) {
            for (Card card : cards) {
                if (card.equals(type))
                    num++;
            }
        }
		return num;
	}
	
	/**
	 * 
	 * @param type
	 * @return
	 */
	public final int getCardCount(ICardType<?> type) {
		int num = 0;
		synchronized (cards) {
            for (Card card : cards) {
                if (card.equals(type))
                    num++;
            }
        }
		return num;
	}

	/**
	 * 
	 * @param type
	 * @return
	 */
	public final int getUsedCardCount(ICardType<?> type) {
		int num = 0;
		synchronized (cards) {
            for (Card card : cards) {
                if (card.equals(type) && card.isUsed())
                    num++;
            }
        }
		return num;
	}
	
	/**
	 * 
	 * @param type
	 * @return
	 */
	public final int getUsableCardCount(ICardType<?> type) {
		int num = 0;
		synchronized (cards) {
            for (Card card : cards) {
                if (card.equals(type) && card.isUsable())
                    num++;
            }
        }
		return num;
	}

	/**
	 * 
	 * @param type
	 * @return
	 */
	public final int getUsableCardCount(CardType type) {
		int num = 0;
		synchronized (cards) {
            for (Card card : cards) {
                if (card.getCardType() == type && card.isUsable())
                    num++;
            }
        }
		return num;
	}

	/**
	 * 
	 * @param type
	 * @return
	 */
	public final int getUnusedCardCount(ICardType<?> type) {
		int num = 0;
		synchronized (cards) {
            for (Card card : cards) {
                if (card.equals(type) && !card.isUsed())
                    num++;
            }
        }
		return num;
	}

	/**
	 * 
	 * @param type
	 * @return
	 */
	public final int getUnusedCardCount(CardType type) {
		int num = 0;
		synchronized (cards) {
            for (Card card : cards) {
                if (card.getCardType() == (type) && !card.isUsed())
                    num++;
            }
        }
		return num;
	}
	
	/**
	 * 
	 * @return
	 */
	public final int getUnusedCardCount() {
		int num = 0;
		synchronized (cards) {
            for (Card card : cards) {
                if (!card.isUsed())
                    num++;
            }
        }
		return num;
	}
	
	/**
	 * 
	 * @return
	 */
	public final Card removeRandomUnusedCard() {
		int r = Utils.rand() % getTotalCardsLeftInHand();
		assert(r >= 0);
		synchronized (cards) {
            for (Card card : cards) {
                if (!card.isUsed() && r-- == 0) {
                    cards.remove(card);
                    return card;
                }
            }
        }
		throw new RuntimeException("Failed to remove random card");
	}

	/**
	 * 
	 * @param type
	 * @return
	 */
	public final Card removeRandomUnusedCard(CardType type) {
		List<Card> cards = getUnusedCards(type);
		int n = Utils.rand() % cards.size();
		return removeCard(cards.get(n));
	}

	/**
	 * Convenience method to get count of all resource cards in hand.
	 * @return
	 */
	public final int getTotalResourceCount() {
		return getCardCount(CardType.Resource);
	}

	/**
	 * 
	 * @param type
	 * @return
	 */
	public final Card getCard(ICardType<?> type) {
	    synchronized (cards) {
            for (Card card : cards) {
                if (card.equals(type))
                    return card;
            }
        }
		return null;
	}
	
	/**
	 * 
	 * @param type
	 * @return
	 */
	public final Card removeCard(ICardType<?> type) {
	    synchronized (cards) {
            for (Card card : cards) {
                if (card.equals(type)) {
                    cards.remove(card);
                    return card;
                }
            }
        }
		throw new RuntimeException("Player has no cards of type " + type + " to remove");
	}
	
	/**
	 * 
	 * @param card
	 * @return
	 */
	public final Card removeCard(Card card) {
	    synchronized (cards) {
            if (cards.remove(card))
                return card;
        }
		return null;
	}
	
	/**
	 * 
	 * @param type
	 * @return
	 */
	public final Card removeUsableCard(ICardType<?> type) {
	    synchronized (cards) {
            for (Card card : cards) {
                if (card.equals(type) && card.isUsable()) {
                    cards.remove(card);
                    return card;
                }
            }
        }
		return null;
	}
	
	/**
	 * 
	 * @param type
	 * @return
	 */
	public final Card getUsableCard(ICardType<?> type) {
		for (Card card : cards) {
		    synchronized (cards) {
                if (card.equals(type) && card.isUsable()) {
                    return card;
                }
            }
		}
		return null;
	}
	
	/**
	 * 
	 * @param type
	 * @param amount
	 */
	public final void incrementResource(ICardType<?> type, int amount) {
		if (amount < 0)
			removeCards(type, -amount);
		else
			addCards(type, amount);
	}

	/**
	 * 
	 * @return
	 */
	public final boolean canBuild() {
		for (BuildableType t : BuildableType.values())
			if (canBuild(t))
				return true;
		return false;
	}

	/**
	 * 
	 * @param type
	 * @return
	 */
	public final boolean canBuild(BuildableType type) {
		for (ResourceType r : ResourceType.values())
			if (getCardCount(r) < type.getCost(r))
				return false;
		return true;
	}

	/**
	 * 
	 * @param flag
	 * @return
	 */
	public final int getTotalDevelopment(int flag) {
		return getCardCount(CardType.Development);
	}

	/**
	 * 
	 * @return
	 */
	public final int getArmySize(Board b) {
		return getUsedCardCount(DevelopmentCardType.Soldier) + b.getNumRoutesOfType(playerNum, RouteType.WARSHIP);
	}

	/**
	 * 
	 * @return
	 */
	public final int getTotalCardsLeftInHand() {
		int num=0;
		synchronized (cards) {
            for (Card card : cards) {
                if (!card.isUsed())
                    num++;
            }
        }
		return num;
	}

	/**
	 * 
	 * @param num
	 */
	public final void setPlayerNum(int num) {
		this.playerNum = num;
	}

	/**
	 * 
	 * @return
	 */
	public final int getPlayerNum() {
		return playerNum;
	}

	/**
	 * 
	 * @param len
	 */
	public final void setRoadLength(int len) {
		roadLength = len;
	}
	
	/**
	 * 
	 *
	 */
	public final int getRoadLength() {
		return roadLength;
	}

	/**
	 * 
	 * @return
	 */
	public final int getHarborPoints() {
		return harborPoints;
	}

	/**
	 * 
	 * @param pts
	 */
	public final void setHarborPoints(int pts) {
		this.harborPoints = pts;
	}

	/**
	 * @return Returns the points.
	 */
	public final int getPoints() {
		return points;
	}

	/**
	 * @param points The points to set.
	 */
	public final void setPoints(int points) {
		this.points = points;
	}

	/**
	 * 
	 * @param area
	 * @return
	 */
	public final int getCityDevelopment(DevelopmentArea area) {
		return cityDevelopment[area.ordinal()];
	}
	
	/**
	 * 
	 * @param area
	 * @param num
	 */
	public final void setCityDevelopment(DevelopmentArea area, int num) {
		cityDevelopment[area.ordinal()] = num;
	}
   
	/**
	 * Return the name of this player.  Default implementation just returns the player num.
	 * @return
	 */
	public String getName() {
		return "Player " + String.valueOf(playerNum);
	}

	/**
	 * 
	 * @return
	 */
	public final boolean hasFortress() {
		return cityDevelopment[DevelopmentArea.Politics.ordinal()] >= DevelopmentArea.CITY_IMPROVEMENT_FOR_SPECIAL_ABILITY;
	}
	
	/**
	 * 
	 * @return
	 */
	public final boolean hasTradingHouse() {
		return cityDevelopment[DevelopmentArea.Trade.ordinal()] >= DevelopmentArea.CITY_IMPROVEMENT_FOR_SPECIAL_ABILITY;
	}
	
	/**
	 * 
	 * @return
	 */
	public final boolean hasAqueduct() {
		return cityDevelopment[DevelopmentArea.Science.ordinal()] >= DevelopmentArea.CITY_IMPROVEMENT_FOR_SPECIAL_ABILITY;
	}

	/**
	 * Special victory points are points for longest road, largest army, printer, constitution
	 * @return
	 */
	public final int getSpecialVictoryPoints() {
		int pts = 0;
		for (Card c : getCards()) {
			if (c.getCardType() == CardType.SpecialVictory) {
				SpecialVictoryType type = SpecialVictoryType.values()[c.getTypeOrdinal()];
				pts += type.points;
			}
		}
		return pts;
	}
	
	/**
	 * 
	 * @return
	 */
	public final int getNumDiscoveredTerritories() {
		return numDiscoveredTerritories;
	}
	
	/*
	 * 
	 */
	final void incrementDiscoveredTerritory(int amount) {
		numDiscoveredTerritories+=amount;
	}

    /**
     * Special case player type when Rules.isCatanForTwo enabled
     *
     * Neutral players dont get a turn, instead the other playres take turns for them.
     * When another player places a road, they do so also for the neutral player (for free)
     * When another player places a settlement, they do so also for the neutral player if possible, a road otherwise
     * The non-neutral players roll the dice twice and collect all resources from the 2 rolls. The die rolls must be different values.
     *
     * @return
     */
	public boolean isNeutralPlayer() {
	    return false;
    }
	
	/**
	 * Return a move or null for not yet chosen.
	 * Method will get called continuously until non-null value returned.
	 * Cannot be cancelled.
	 * 
	 * @param soc
	 * @param moves
	 * @return
	 */
	public abstract MoveType chooseMove(SOC soc, Collection<MoveType> moves);
	
	public enum VertexChoice {
		SETTLEMENT,
		CITY,
		CITY_WALL,
		NEW_KNIGHT,
		KNIGHT_TO_MOVE,
		KNIGHT_TO_ACTIVATE,
		KNIGHT_TO_PROMOTE,
		KNIGHT_MOVE_POSITION,
		KNIGHT_DISPLACED,
		KNIGHT_DESERTER,
		OPPONENT_KNIGHT_TO_DISPLACE,
		TRADE_METROPOLIS,
		POLITICS_METROPOLIS,
		SCIENCE_METROPOLIS,
		PIRATE_FORTRESS,
		OPPONENT_STRUCTURE_TO_ATTACK,
	};
	
	/**
	 * @param soc
	 * @param vertexIndices
	 * @param mode
	 * @param knightToMove null unless mode is KNIGHT_DISPLACED or KNIGHT_TO_MOVE
	 * @return
	 */
	public abstract Integer chooseVertex(SOC soc, Collection<Integer> vertexIndices, VertexChoice mode, Integer knightToMove);

	public enum RouteChoice {
		ROAD,
		SHIP,
		UPGRADE_SHIP,
		SHIP_TO_MOVE,
		ROUTE_DIPLOMAT, // player chooses from any open route.  If theirs, then they can move it, otherwise opponents are removed from the baord.
		OPPONENT_ROAD_TO_ATTACK, 
		OPPONENT_SHIP_TO_ATTACK, // player chooses an opponent ship adjacent to one of they're warships
	}
	
	public abstract Integer chooseRoute(SOC soc, Collection<Integer> routeIndices, RouteChoice mode, Integer shipToMove);

	public enum RouteChoiceType {
		ROAD_CHOICE,
		SHIP_CHOICE
	}

	/**
	 * 
	 * @param soc
	 * @return
	 */
	public abstract RouteChoiceType chooseRouteType(SOC soc);
	
	public enum TileChoice {
		ROBBER,
		PIRATE,
		INVENTOR,
		MERCHANT,
	}
	
	/**
	 * 
	 * @param soc
	 * @param tileIndices
	 * @param mode
	 * @return
	 */
	public abstract Integer chooseTile(SOC soc, Collection<Integer> tileIndices, TileChoice mode);
	
	
	/**
	 * Return trade to make or null for not yet chosen.
	 * Method will get called continuously until non-null value returned.
	 * This method can be canceled.
	 * 
	 * @param soc
	 * @param trades
	 * @return
	 */
	public abstract Trade chooseTradeOption(SOC soc, Collection<Trade> trades);

	public enum PlayerChoice {
		PLAYER_TO_TAKE_CARD_FROM,
		PLAYER_TO_SPY_ON,
		PLAYER_FOR_DESERTION,
		PLAYER_TO_GIFT_CARD,
		PLAYER_TO_FORCE_HARBOR_TRADE,
	};
	
	/**
	 * Return playerNum to take card from or null for not yet chosen.
	 * Method will get called continuously until non-null value returned.
	 * Cannot be cancelled.
	 * 
	 * @param soc
	 * @param playerOptions
	 * @param mode
	 * @return
	 */
	public abstract Integer choosePlayer(SOC soc, Collection<Integer> playerOptions, PlayerChoice mode);
	
	public enum CardChoice {
		RESOURCE_OR_COMMODITY,
		GIVEUP_CARD, // cards are lost 
		OPPONENT_CARD, // choose from an opponents cards
		EXCHANGE_CARD, // exchange for another players card
		FLEET_TRADE, // any num of 2:1 trades of a single resource or commodity for 1 turn 
	}
	
	/**
	 * Choose a card from a list
	 * @param soc
	 * @param cards
	 * @return
	 */
	public abstract Card chooseCard(SOC soc, Collection<Card> cards, CardChoice mode);
	
	public enum EnumChoice {
		COMMODITY_MONOPOLY,
		RESOURCE_MONOPOLY,
		DRAW_PROGRESS_CARD,
		CRANE_CARD_DEVELOPEMENT,
	}
	
	/**
	 * 
	 * @param soc
	 * @param values
	 * @return
	 */
	public abstract <T extends Enum<T>> T chooseEnum(SOC soc, EnumChoice mode, T [] values);
	
	/**
	 * Players sets the dice to values of their choice each in range [1-6] inclusive
     *
     * @param soc
	 * @param dice array of dice to set
	 * @param num length of array to set
	 * @return true when player completed
	 */
	public abstract boolean setDice(SOC soc, Dice [] dice, int num);


}
