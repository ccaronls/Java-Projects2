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
	private int []					cityDevelopment = new int[DevelopmentArea.values().length];
	private Card					merchantFleetTradable;
	
	/**
	 * 
	 * @param num
	 */
	protected Player() {
	}
	
	Player(int playerNum) {
	    setPlayerNum(playerNum);
	}
	
	@Override
    public final String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(getName());
        buf.append(" cards:").append(cards);
        buf.append(" roadLen:").append(roadLength);
        buf.append(" points:").append(points);
        buf.append(" harbor pts:").append(harborPoints);
        for (DevelopmentArea area : DevelopmentArea.values()) {
        	if (cityDevelopment[area.ordinal()] > 0) {
        		buf.append(" " + area.name() + ":").append(cityDevelopment[area.ordinal()]);
        	}
        }
        if (merchantFleetTradable != null) {
        	buf.append(" merchantFleet:" + merchantFleetTradable);
        }
        return buf.toString();
    }
    
	/**
	 * set all un-usable cards to usable or usabel cards to unusable 
	 */
	public final List<Card> setCardsUsable(CardType type, boolean usable) {
		List<Card> modified = new ArrayList<Card>();
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
		return modified;
	}

	/**
	 * 
	 * @param type
	 * @param flag
	 */
	public final void addCard(Card card) {
		cards.add(card);
		Collections.sort(cards);
	}
	
	/**
	 * 
	 * @param type
	 */
	public final void addCard(ICardType<?> type) {
		cards.add(new Card(type, type.defaultStatus()));//CardStatus.USABLE));
		Collections.sort(cards);
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
	public final void reset() {
		cards.clear();
		roadLength=0;
		points = 0;
		harborPoints = 0;
	}
	
	/**
	 * 
	 * @return
	 */
	public final Iterable<Card> getCards() {
		return cards;
	}
	
	/**
	 * 
	 * @param type
	 * @return
	 */
	public final List<Card> getCards(CardType type) {
		List<Card> list = new ArrayList<Card>();
		for (Card card : cards) {
			if (card.getCardType() == type) {
				list.add(card);
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
		for (Card card : cards) {
			if (card.isUsable() && card.equals(type)) {
				list.add(card);
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
		for (Card card : cards) {
			if (card.isUsable() && card.equals(type)) {
				list.add(card);
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
		for (Card card : cards) {
			if (card.getCardType() == type && !card.isUsed()) {
				list.add(card);
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
		for (Card card : cards) {
			if (!card.isUsed()) {
				list.add(card);
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
		for (Card card : cards) {
			if (card.getCardType() == type)
				num++;
		}
		return num;
	}

	/**
	 * 
	 * @param type
	 * @param status
	 * @return
	 */
	public final int getCardCount(CardType type, CardStatus status) {
		int num = 0;
		for (Card card : cards) {
			if (card.getCardType() == type && card.getCardStatus() == status)
				num++;
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
		for (Card card : cards) {
			if (card.getCardType() == type && !card.isUsable());
				num++;
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
		for (Card card : cards) {
			if (card.equals(type))
				num++;
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
		for (Card card : cards) {
			if (card.equals(type))
				num++;
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
		for (Card card : cards) {
			if (card.equals(type) && card.isUsed())
				num++;
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
		for (Card card : cards) {
			if (card.equals(type) && card.isUsable())
				num++;
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
		for (Card card : cards) {
			if (card.getCardType() == type && card.isUsable())
				num++;
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
		for (Card card : cards) {
			if (card.equals(type) && !card.isUsed())
				num++;
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
		for (Card card : cards) {
			if (card.getCardType() == (type) && !card.isUsed())
				num++;
		}
		return num;
	}
	
	/**
	 * 
	 * @return
	 */
	public final int getUnusedCardCount() {
		int num = 0;
		for (Card card : cards) {
			if (!card.isUsed())
				num++;
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
		for (Card card : cards) {
			if (!card.isUsed() && r-- == 0) {
				cards.remove(card);
				return card;
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
		for (Card card : cards) {
			if (card.equals(type))
				return card;
		}
		return null;
	}
	
	/**
	 * 
	 * @param type
	 * @return
	 */
	public final Card removeCard(ICardType<?> type) {
		for (Card card : cards) {
			if (card.equals(type)) {
				cards.remove(card);
				return card;
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
		if (cards.remove(card))
			return card;
		return null;
	}
	
	/**
	 * 
	 * @param type
	 * @return
	 */
	public final Card removeUsableCard(ICardType<?> type) {
		for (Card card : cards) {
			if (card.equals(type) && card.isUsable()) {
				cards.remove(card);
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
	public final Card getUsableCard(ICardType<?> type) {
		for (Card card : cards) {
			if (card.equals(type) && card.isUsable()) {
				return card;
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
	 * @param status
	 * @return
	 */
	public final int getTotalDevelopment(int flag) {
		return getCardCount(CardType.Development);
	}

	/**
	 * 
	 * @return
	 */
	public final int getArmySize() {
		return getUsedCardCount(DevelopmentCardType.Soldier);
	}

	/**
	 * 
	 * @return
	 */
	public final int getTotalCardsLeftInHand() {
		int num=0;
		for (Card card : cards) {
			if (!card.isUsed())
				num++;
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
	 * @param rules
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
	 * Return a move or null for not yet chosen.
	 * Method will get called continuously until non-null value returned.
	 * Cannot be cancelled.
	 * 
	 * @param soc
	 * @param moves
	 * @param numMoves
	 * @return
	 */
	public abstract MoveType chooseMove(SOC soc, Collection<MoveType> moves);
	
	public static enum VertexChoice {
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
	 * @return
	 */
	public abstract Vertex chooseVertex(SOC soc, Collection<Integer> vertexIndices, VertexChoice mode);

	public static enum RouteChoice {
		ROAD,
		SHIP,
		UPGRADE_SHIP,
		SHIP_TO_MOVE,
		ROUTE_DIPLOMAT, // player chooses from any open route.  If theirs, then they can move it, otherwise opponents are removed from the baord.
		OPPONENT_ROAD_TO_ATTACK,
	}
	
	public abstract Route chooseRoute(SOC soc, Collection<Integer> routeIndices, RouteChoice mode);

	public static enum RouteChoiceType {
		ROAD_CHOICE,
		SHIP_CHOICE
	}

	/**
	 * 
	 * @param soc
	 * @return
	 */
	public abstract RouteChoiceType chooseRouteType(SOC soc);
	
	public static enum TileChoice {
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
	public abstract Tile chooseTile(SOC soc, Collection<Integer> tileIndices, TileChoice mode);
	
	
	/**
	 * Return trade to make or null for not yet chosen.
	 * Method will get called continuously until non-null value returned.
	 * This method can be canceled.
	 * 
	 * @param soc
	 * @param trades
	 * @param numTrades
	 * @return
	 */
	public abstract Trade chooseTradeOption(SOC soc, Collection<Trade> trades);

	public static enum PlayerChoice {
		PLAYER_TO_TAKE_CARD_FROM,
		PLAYER_TO_SPY_ON,
		PLAYER_FOR_DESERTION,
		PLAYER_TO_GIFT_CARD,
		PLAYER_TO_FORCE_HARBOR_TRADE,
	};
	
	/**
	 * Return player to take card from or null for not yet chosen.
	 * Method will get called continuously until non-null value returned.
	 * Cannot be cancelled.
	 * 
	 * @param soc
	 * @param playerOptions
	 * @param numPlayerOptions
	 * @return
	 */
	public abstract Player choosePlayer(SOC soc, Collection<Integer> playerOptions, PlayerChoice mode);
	
	public static enum CardChoice {
		RESOURCE_OR_COMMODITY,
		GIVEUP_CARD, // cards are lost 
		OPPONENT_CARD, // choose from an opponents cards
		PROGRESS_CARD, // choose a progress card
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
	
	public static enum EnumChoice {
		MONOPOLY,
		DRAW_DEVELOPMENT_CARD, 
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
	 * @param die array of dice to set
	 * @param num length of array to set
	 * @return true when player completed
	 */
	public abstract boolean setDice(Dice [] dice, int num);

}
