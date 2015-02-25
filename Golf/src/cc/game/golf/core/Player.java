package cc.game.golf.core;

import java.util.*;

import cc.lib.utils.Reflector;

/**
 * Abstract base class for all Golf Players
 * 
 * @author ccaron
 *
 */
public abstract class Player extends Reflector {

    static {
        addAllFields(Player.class);
    }
    
    String name;
    int points;
    int index = -1;
    
    Card [][] cards;

    public Player() {}
    public Player(String name) {
        this.name = name;
    }
    
    final void reset(Golf golf) {
        int numRows = golf.getRules().getGameType().getRows();
        cards = new Card[numRows][];
        for (int i=0; i<numRows; i++) {
            cards[i] = new Card[golf.getRules().getGameType().getCols()];
        }
    }

    final void addPoints(int handPoints) {
        points += handPoints;
    }

    /**
     * Clear the points for this player
     */
    final void clearPoints() {
        points = 0;
    }

    /**
     * Return the index of this player in the game or -1 if not in game
     * @return
     */
    public final int getPlayerNum() {
        return index;
    }
    
    /**
     * Count the number of cards actually dealt
     * @return
     */
    public final int getNumCards() {
        int num = 0;
        for (int i=0; i<cards.length; i++) {
            for (int ii=0; ii<cards[i].length; ii++)
                if (cards[i][ii] != null)
                    num++;
        }
        return num;
    }

    /*
     * Set all cards in the hand to showing
     */
    final void showAllCards() {
        for (int i=0; i<cards.length; i++) {
            for (int ii=0; ii<cards[i].length; ii++)
                cards[i][ii].setShowing(true);
        }
    }

    /**
     * Get a deep copy of the card in the top row
     * @return
     */
    public final List<Card> getRow(int row) {
        List<Card> copy = new ArrayList<Card>(cards[row].length);
        for (int i=0; i<cards[row].length; i++) {
            //copy.add((Card)cards[row][i].deepCopy());
            copy.add(cards[row][i]);
        }
        return copy;
    }

    /**
     * Return a deep copy of this players cards
     * @return
     */
    public final Card [][] getCards() {
        Card [][] copy = new Card[cards.length][cards[0].length];
        for (int i=0; i<copy.length; i++) {
            for (int ii=0; ii<copy[i].length; ii++) {
                copy[i][ii] = cards[i][ii];
            }
        }
        return copy;
    }

    /**
     * Return the player's current score not counting the current round
     * @return
     */
    public final int getPoints() {
        return points;
    }
    
    /**
     * Return number of cards face up
     * @return
     */
    public final int getNumCardsShowing() {
        int  numShowing = 0;
        for (int i=0; i<cards.length; i++) {
            for (int ii=0; ii<cards[i].length; ii++)
                if (cards[i][ii] != null && cards[i][ii].isShowing())
                    numShowing++;
        }
        return numShowing;
    }

    /**
     * Return number of cards face up
     * @return
     */
    public final int getNumCardsShowing(int row) {
        int  numShowing = 0;
        for (int i=0; i<cards[row].length; i++) {
            if (cards[row][i] != null && cards[row][i].isShowing())
                numShowing++;
        }
        return numShowing;
    }

    /**
     * Return number of cards dealt
     * @return
     */
    public final int getNumCardsDealt() {
        int numDealt = 0;
        for (int i=0; i<cards.length; i++) {
            for (int ii=0; ii<cards[i].length; ii++)
                if (cards[i][ii] != null)
                    numDealt ++;
        }
        return numDealt;
    }

    /**
     * Get a deep copy of all cards in the hand.
     * @return
     */
    public final List<Card> getAllCards() {
        List<Card> all = new ArrayList<Card>();
        for (int i=0; i<cards.length; i++)
            all.addAll(Arrays.asList(cards[i]));
        return all;
    }

    /**
     * 
     * @param card
     *
    final void dealCard(Card card, int row, int col) {
        assert(cards[row][col] == null);
        cards[row][col] = card;
    }

    /**
     * Get a card.  Row 0 is top row.  All other values return secondary row.
     * @param row
     * @param column
     * @return
     */
    public final Card getCard(int row, int column) {
        try {
            return cards[row][column];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ArrayIndexOutOfBoundsException("row " + row + " col " + column + " out of range");
        }
    }
    
    /**
     * Return a card first from the top row, then from the bottom row.
     * @param num
     * @return
     */
    public final Card getCard(int num) {
        int row = num / cards[0].length;
        int col = num % cards[0].length;
        return getCard(row, col);
    }
    
    /*
     * 
     */
    final void setCard(int index, Card card) {
        int row = index / cards[0].length;
        int col = index % cards[1].length;
        cards[row][col] = card;
    }
    
    final void setCard(int row, int col, Card card) {
        cards[row][col] = card;
    }
    
    /**
     * Use the rules to evaluate the number of points a hand is worth
     * @param golf
     * @param top
     * @param bottom
     * @return
     */
    public static int getHandPoints(Rules rules, Card [][] cards) {
        int points = 0;
        switch (rules.getGameType()) {
            case NineCard: {
                int bonusPts = 0;
                int [][] pts = new int[cards.length][];
                
                // fill the pts array with pts of each card
                assert(cards.length == 3);
                for (int i=0; i<cards.length; i++) {
                    pts[i] = new int[cards[i].length];
                    assert(pts[i].length == 3);
                    for (int ii=0; ii<cards[i].length; ii++) {
                        if (cards[i][ii].isShowing())
                            pts[i][ii] = rules.getCardValue(cards[i][ii], false);
                    }
                }
                
                // check rows, cols
                for (int i=0; i<3; i++) {
                    if (isSet(rules, cards[i][0], cards[i][1], cards[i][2])) {
                        pts[i][0] = pts[i][1] = pts[i][2] = 0;
                    }
                    if (isSet(rules, cards[0][i], cards[1][i], cards[2][i])) {
                        pts[0][i] = pts[1][i] = pts[2][i] = 0;
                    }
                }

                // check diagonals
                if (isSet(rules, cards[0][0], cards[1][1], cards[2][2])) {
                    pts[0][0] = pts[1][1] = pts[2][2] = 0;
                }
                if (isSet(rules, cards[0][2], cards[1][1], cards[2][0])) {
                    pts[0][2] = pts[1][1] = pts[2][0] = 0;
                }
                
                // check quads
                if (isSet(rules, cards[0][0], cards[0][1], cards[1][0], cards[1][1])) {
                    pts[0][0] = pts[0][1] = pts[1][0] = pts[1][1] = 0;
                    bonusPts -= 25;
                }
                if (isSet(rules, cards[1][0], cards[1][1], cards[2][0], cards[2][1])) {
                    pts[1][0] = pts[1][1] = pts[2][0] = pts[2][1] = 0;
                    bonusPts -= 25;
                }
                if (isSet(rules, cards[0][1], cards[1][1], cards[0][2], cards[1][2])) {
                    pts[0][1] = pts[1][1] = pts[0][2] = pts[1][2] = 0;
                    bonusPts -= 25;
                }
                if (isSet(rules, cards[1][1], cards[2][1], cards[1][2], cards[2][2])) {
                    pts[1][1] = pts[2][1] = pts[1][2] = pts[2][2] = 0;
                    bonusPts -= 25;
                }
                for (int i=0; i<3; i++) {
                    for (int ii=0; ii<3; ii++) {
                        points += pts[i][ii];
                    }
                }
                points += bonusPts;
                break;
            }
            default: {
                for (int i=0; i<rules.getGameType().getCols(); i++) {
                    if (isSet(rules, cards[0][i], cards[1][i])) {
                        points += rules.getCardValue(cards[0][i], true);
                    } else {
                        if (cards[0][i].isShowing())
                            points += rules.getCardValue(cards[0][i], false);
                        if (cards[1][i].isShowing())
                            points += rules.getCardValue(cards[1][i], false);
                    }
                }
                break;
            }
        }
        return points;
    }
    
    /**
     * Return true if the list of cards comprise a set.
     * @param rules
     * @param cards
     * @return
     */
    public static boolean isSet(Rules rules, Card ... cards) {
        assert(cards.length > 1);
        Rank r = null;
        for (Card card : cards) {
            if (!card.isShowing())
                return false;
            if (rules.getWildcard().isWild(card))
                continue;
            if (r == null)
                r = card.getRank();
            else if (!r.equals(card.getRank()))
                return false;
        }
        //Golf.debugMsg("Found set from: " + Arrays.asList(cards));
        return true;
    }
    
    /**
     * 
     * @return
     */
    public final int getHandPoints(Golf golf) {
        if (this.getNumCards() < golf.getNumHandCards())
            return 0;
        return getHandPoints(golf.getRules(), cards);
    }
    
    /**
     * Return true if all cards are showing
     * @return
     */
    public final boolean isAllCardsShowing() {
        for (int i=0; i<cards.length; i++) {
            for (int ii=0; ii<cards[0].length; ii++)
                if (!cards[i][ii].isShowing())
                    return false;
        }
        return true;
    }
    
    /**
     * 
     * @return
     */
    public final int getNumRows() {
        return cards != null ? cards.length : 0;
    }
    
    /**
     * 
     * @return
     */
    public final int getNumCols() {
        return cards!=null && cards[0] != null ? cards[0].length : 0;
    }
    
    /**
     * Set rowcol array to the row/col position of the card in this players hand.
     * Return true if found, false otherwise/
     * 
     * @param c the card to find
     * @param rowCol must be non-null array of length >= 2
     * @return true if card found, false otherwise
     */
    public final boolean getPositionOfCard(Card c, int [] rowCol) {
        for (int i=0; i<getNumRows(); i++) {
            for (int ii=0; ii<getNumCols(); ii++) {
                if ((c==null && cards[i][ii] == null) || (c != null && c.equals(cards[i][ii]))) {
                    rowCol[0]=i; rowCol[1]=ii;
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 
     * @return
     */
    public final String getName() {
        if (name == null)
            name = "Player " + getPlayerNum();
        return name;
    }

    /**
     * 
     * @param name
     */
    public final void setName(String name) {
        this.name = name;
    }

    /**
     * 
     * @param golf
     * @param row
     * @return
     */
    protected abstract int turnOverCard(Golf golf, int row);
    
    /**
     * Choose from enum value to draw from stack, discard pile or wait
     * @param golf
     * @return
     */
    protected abstract DrawType chooseDrawPile(Golf golf);

    /**
     * Choose any card from your hand to swap out, or the drawn card itself or null to indicate waiting
     * @param golf
     * @param drawCard
     * @return
     */
    protected abstract Card chooseDiscardOrPlay(Golf golf, Card drawCard);

    /**
     * Choose any card from your hand to swap out, or null to indicate waiting.
     * Returning discardPileFromTop will not be accepted
     * @param golf
     * @return
     */
    protected abstract Card chooseCardToSwap(Golf golf, Card discardPileTop);


}
