package cc.game.kaiser.ai;

import cc.game.kaiser.core.*;

public class PlayerBot extends Player
{
    /**
     * Setting this to TRUE will cause debug spam to stdout.
     */
    public static boolean ENABLE_AIDEBUG = false;
    
    // these are all set prior to calling evaluate
    int trickValue; // one of -2, 1, or 6
    boolean partnerWinning;
    boolean opponentWinning;
    Suit trump;
    Suit lead;
    Rank winningRank;

    public final int getTrickValue() { return trickValue; }
    public final boolean isPartnerWinning() { return partnerWinning; }
    public final boolean isOpponentWinning() { return opponentWinning; }
    public final Suit getTrump() { return trump; }
    public final Suit getLead() { return lead; }
    public final Rank getWinningRank() { return winningRank; }
    
    /**
     *  method to return a value for a card that can be
     *  redefined for different AI implementations.
     * @param card
     * @return
     */
    protected float evaluate(Card card) {
        // if an opponent is winning the hand and we have the three, play it.
        // if an opponent is winning the hand and the three has been played,
        // then play our lowest card.
        // If our partner is winning the hand, and we have the five, then play it.
        // otherwise play our lowest card.

        float cardValue = 0;

        if (trickValue > 0) {
            if (partnerWinning) {
                // our highest value card should be the five, otherwise the 7 non-trumps,
                // and the worse card would be the three
                if (card.rank == Rank.FIVE)
                    cardValue = 100;
                else if (card.rank == Rank.THREE)
                    cardValue = -100;
                else
                    cardValue = 10 - card.rank.ordinal() - (card.suit == trump ? 10 : 0);
            } else if (opponentWinning) {
                // out highest value card is the 3, then a non-trump that wins the trick,
                // then a low trump, followed by our lowest card in the required suit.
                if (card.rank == Rank.THREE)
                    cardValue = 100;
                else if (card.rank == Rank.FIVE)
                    cardValue = -100;
                else {
                    if (card.suit == lead) {
                        if (card.rank.ordinal() > winningRank.ordinal())
                            cardValue = 10 * card.rank.ordinal();
                        else
                            cardValue = 20 - card.rank.ordinal();
                    } else if (card.suit == trump) {
                        cardValue = 10;
                    } else {
                        cardValue = 0;
                    }
                }
            } else {
                // we want to play out highest valued card.
                cardValue = card.rank.ordinal();
                if (card.suit == trump)
                    cardValue *= 10;
            }

        } else {
            if (partnerWinning) {
                // play our lowest valued card
            } else if (opponentWinning) {
                // play out lowest valued card
            } else {
                // this situation is only possible if the first player
                // has played the 3 and we are the second player.
                // We still play the lowest valued card (most likly to lose the trick)
            }

            cardValue = 20 - card.rank.ordinal() - (card.suit == trump ? 10 : 0);
        }

        if (ENABLE_AIDEBUG)
            System.out.println("Card " + getCardString(card) + " value of " + cardValue);

        return cardValue;        
    }

    public PlayerBot() {
        super("");
    }
    
    /**
     * 
     * @param nm
     */
    public PlayerBot(String nm) { 
        super(nm);
        trickValue = 0;
        partnerWinning = (false);
        opponentWinning = (false);
        trump = (Suit.NOTRUMP);
        lead = (Suit.NOTRUMP);
    }

    @Override
    public Card playTrick(Kaiser kaiser, Card [] options) {
        lead = Suit.NOTRUMP;
        Card leadCard = kaiser.getTrickLead();
        if (leadCard != null)
            lead = leadCard.suit;
        partnerWinning = false;
        opponentWinning = false;
        trickValue = 1;
        //Player winningPlayer = null;
        Card winningCard = null;

        for (int i=0; i<Kaiser.NUM_PLAYERS; i++) {
            Card card = kaiser.getTrick(i);
            if (card == null)
                continue;
            if (card.rank == Rank.FIVE)
                trickValue += 5;
            else if (card.rank == Rank.THREE)
                trickValue -= 3;
            if (winningCard == null)
                winningCard = card;
            else
                winningCard = kaiser.getWinningCard(card, winningCard);

            if (winningCard == card) {
                if (i == kaiser.getTeammate(getPlayerNum())) {
                    partnerWinning = true;
                    opponentWinning = false;
                }
                else {
                    opponentWinning = true;
                    partnerWinning = false;
                }
            }
        }

        winningRank = Rank.SEVEN;
        if (winningCard != null)
            winningRank = winningCard.rank;

        if (ENABLE_AIDEBUG) {
            System.out.println(String.format("AI: cards[%s, %s, %s, %s]\n"
                + "   leadSuit        = %s\n"
                + "   winningCard     = %s\n"
                + "   winningRank     = %s\n"
                + "   opponentWinning = %s\n"
                + "   partnerWinning  = %s\n"
                + "   trickValue      = %d\n"
                , getCardString(kaiser.getTrick(0))
                , getCardString(kaiser.getTrick(1))
                , getCardString(kaiser.getTrick(2))
                , getCardString(kaiser.getTrick(3))
                , getCardString(kaiser.getTrickLead())
                , getCardString(winningCard)
                , winningCard == null ? "null" : winningRank.getRankString()
                , opponentWinning ? "TRUE" : "FALSE"
                , partnerWinning ? "TRUE" : "FALSE"
                , trickValue));
        }
        
        Card bestOption = null;
        float bestValue = -99999;
        for (Card c: options) {
            float value = evaluate(c);
            if (value > bestValue) {
                bestValue = value;
                bestOption = c;
            }
        }
        
        return bestOption;
    }    

    @Override
    public Bid makeBid(Kaiser kaiser, Bid [] options) {
        return options.length > 0 ? options[0] : Bid.NO_BID;
    }

    private static String getCardString(Card card)
    {
        return card == null ? "null" : card.toPrettyString();
    }        
};

