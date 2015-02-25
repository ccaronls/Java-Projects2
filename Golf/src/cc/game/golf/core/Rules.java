package cc.game.golf.core;

import cc.lib.utils.Reflector;

/**
 * Class to hold rules of the game tunable to achieve many variations.  
 * Rule changes made here will not take effect until Golf.initGame() is called.
 * 
 * @author ccaron
 *
 */
public class Rules extends Reflector {

    static {
        addAllFields(Rules.class);
    }
    
    public enum GameType {
        FourCard(2,2), // 2x2
        SixCard(2,3), // 2x3
        EightCard(2,4),  // 2x4
        NineCard(3,3);   // 3x3
        
        private final int rows;
        private final int cols;
        
        private GameType(int rows, int cols) {
            this.rows = rows;
            this.cols = cols;
        }
        
        public final int getRows() {
            return rows;
        }
        
        public final int getCols() {
            return cols;
        }
    }
    
    /**
     * 
     * @author ccaron
     *
     */
    public enum KnockerBonusType {
        /**
         * Knocker get zero points if hand value is greater than zero
         */
        ZeroOrLess,
        /**
         * Knocker get -number of players from his score
         */
        MinusNumberOfPlayers,
        /**
         * No bonus
         */
        None
    }
    
    public enum KnockerPenaltyType {
        Plus10,
        HandScoreDoubledPlus5,
        EqualToHighestHand,
        TwiceNumberOfPlayers,
        None
    }
    
    public enum WildCard {
        OneEyedJacks,
        SuiceKings,
        Dueces,
        None;
        
        public boolean isWild(Card card) {
            switch (this) {
                case OneEyedJacks:
                    return card.isOneEyedJack();
                case SuiceKings:
                	return card.isSuicideKing();
                case Dueces:
                    return card.getRank() == Rank.TWO;
                case None:
                    return false;
            }
            throw new RuntimeException("Unhanlded case '" + this + "'");
        }
    }
    
    private GameType gameType = GameType.SixCard;
    private int numHoles = 9;
    private int numDecks = 2;
    private int numJokers = 4;
    private int jokerValueUnpaired = 15;
    private int jokerValuePaired = -5;
    private KnockerPenaltyType knockerPenaltyType = KnockerPenaltyType.Plus10;
    private KnockerBonusType knockerBonusType = KnockerBonusType.ZeroOrLess;
    private int fourOfAKindBonus = -40;
    private WildCard wildcard = WildCard.None;
    
    /**
     * Default rules
     */
    public Rules() {
    }

    /**
     * Create custom rules
     * @param numHoles
     * @param numCardsPerRow
     * @param numDecks
     * @param numJokers
     * @param jokerValueUnpaired
     * @param jokerValuePaired
     * @param knockerPenaltyType
     * @param knockerBonusType
     */
    public Rules(GameType gameType, int numHoles, int numDecks, int numJokers, int jokerValueUnpaired,
            int jokerValuePaired, KnockerPenaltyType knockerPenaltyType, KnockerBonusType knockerBonusType,
            int fourOfAKindBonus, WildCard wildCard) {
        this.gameType = gameType;
        this.numHoles = numHoles;
        this.numDecks = numDecks;
        this.numJokers = numJokers;
        this.jokerValueUnpaired = jokerValueUnpaired;
        this.jokerValuePaired = jokerValuePaired;
        this.knockerPenaltyType = knockerPenaltyType;
        this.knockerBonusType = knockerBonusType;
        this.fourOfAKindBonus = fourOfAKindBonus;
        this.wildcard = wildCard;
    }

    public final GameType getGameType() {
        return gameType;
    }

    public final void setGameType(GameType gameType) {
        this.gameType = gameType;
    }

    public final int getNumHoles() {
        return numHoles;
    }

    public final void setNumHoles(int numHoles) {
        this.numHoles = numHoles;
    }

    public final int getNumDecks() {
        if (gameType == GameType.FourCard)
            return 1;
        else if (gameType == GameType.NineCard)
            return 2;
        return numDecks;
    }

    public final void setNumDecks(int numDecks) {
        this.numDecks = numDecks;
    }

    public final int getNumJokers() {
        if (gameType == GameType.NineCard)
            return 0;
        return numJokers;
    }

    public final void setNumJokers(int numJokers) {
        this.numJokers = numJokers;
    }

    public final int getJokerValueUnpaired() {
        return jokerValueUnpaired;
    }

    public final void setJokerValueUnpaired(int jokerValueUnpaired) {
        this.jokerValueUnpaired = jokerValueUnpaired;
    }

    public final int getJokerValuePaired() {
        return jokerValuePaired;
    }

    public final void setJokerValuePaired(int jokerValuePaired) {
        this.jokerValuePaired = jokerValuePaired;
    }

    public final KnockerPenaltyType getKnockerPenaltyType() {
        return knockerPenaltyType;
    }

    public final void setKnockerPenaltyType(KnockerPenaltyType knockerPenaltyType) {
        this.knockerPenaltyType = knockerPenaltyType;
    }

    public final KnockerBonusType getKnockerBonusType() {
        return knockerBonusType;
    }

    public final void setKnockerBonusType(KnockerBonusType knockerBonusType) {
        this.knockerBonusType = knockerBonusType;
    }

    public final int getFourOfAKindBonus() {
        return fourOfAKindBonus;
    }

    public final void setFourOfAKindBonus(int fourOfAKindBonus) {
        this.fourOfAKindBonus = fourOfAKindBonus;
    }

    public final WildCard getWildcard() {
        return wildcard;
    }

    public final void setWildcard(WildCard wildcard) {
        this.wildcard = wildcard;
    }

    /**
     * Use current rule set to get the value of a card. 
     * @param card
     * @param paired
     * @return
     */
    public int getCardValue(Card card, boolean paired) {
        switch (card.getRank()) {
            case JOKER:
                if (paired)
                    return getJokerValuePaired();
                return getJokerValueUnpaired();
            default:
                return paired ? 0 : card.getRank().value;
                        
        }
    }
}
