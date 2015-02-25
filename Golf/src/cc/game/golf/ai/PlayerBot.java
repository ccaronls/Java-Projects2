package cc.game.golf.ai;

import java.util.*;

import cc.game.golf.core.*;

/**
 * Simplistic AI robot.  Always chooses play that results in lowest points.
 * @author ccaron
 *
 */
public class PlayerBot extends Player {

    
    public PlayerBot() {
        super();
    }
    
    public PlayerBot(String name) {
        super(name);
    }
    
    public static boolean DEBUG_ENABLED = false;
    
    private static int maxPoints = 100;
    private static double maxSetPotential = 1;
    
    static class Hand {
        Card [][] cards;
        Card replaced;
        int pts;
        double ptsScore;
        double setPotentialScore;
        double faceDownScore;
        double totalScore;
        String label = "";
        Hand(Card [][]cards) {
            this.cards = cards;
        }
    }
    
    private LinkedList<Hand> hands = new LinkedList<Hand>();
    
    private void debugWriteHands() {
        debugWriteHands(hands, getNumRows(), getNumCols());
    }
    
    static void debugWriteHands(List<Hand> hands, int rows, int cols) {
        String hand = "";
        final String handSpacing = "  ";
        for (int i=0; i<rows; i++) {
            for (int iii=0; iii<hands.size(); iii++) {
                for (int ii=0; ii<cols; ii++) {
                    hand += "+--+";
                }
                hand += handSpacing;
            }
            hand += "\n";
            for (int iii=0; iii<hands.size(); iii++) {
                for (int ii=0; ii<cols; ii++) {
                    Card c = hands.get(iii).cards[i][ii];
                    if (c.isShowing())
                        hand += String.format("|%c%c|", c.getRank().getRankString().charAt(0), c.getSuit().getSuitChar());
                    else
                        hand += "|  |";
                }
                hand += handSpacing;
            }
            hand += "\n";
            for (int iii=0; iii<hands.size(); iii++) {
                for (int ii=0; ii<cols; ii++) {
                    hand += "|  |";
                }
                hand += handSpacing;
            }
            hand += "\n";
                
            for (int iii=0; iii<hands.size(); iii++) {
                for (int ii=0; ii<cols; ii++) {
                    hand += "+--+";
                }
                hand += handSpacing;
            }
            hand += "\n";
        }
        for (int i=0; i<hands.size(); i++) {                
            hand += String.format("Pts   :%5d  ", hands.get(i).pts);
        }
        hand += "\n";
        for (int i=0; i<hands.size(); i++) {                
            hand += String.format("Pscore:%1.3f  ", hands.get(i).ptsScore);
        }
        hand += "\n";
        for (int i=0; i<hands.size(); i++) {                
            hand += String.format("Fscore:%1.3f  ", hands.get(i).faceDownScore);
        }
        hand += "\n";
        for (int i=0; i<hands.size(); i++) {                
            hand += String.format("Sscore:%1.3f  ", hands.get(i).setPotentialScore);
        }
        hand += "\n";
        for (int i=0; i<hands.size(); i++) {                
            hand += String.format("Total :%1.3f  ", hands.get(i).totalScore);
        }
        hand += "\n";
        for (int i=0; i<hands.size(); i++) {                
            Card c = hands.get(i).replaced;
            if (c != null)
                hand += String.format("Replaced:%c%c   ", c.getRank().getRankString().charAt(0), c.getSuit().getSuitChar());
            else
                hand += String.format("%-14s", "");
        }
        hand += "\n";
        for (int i=0; i<hands.size(); i++) {                
            hand += String.format("%-14s", hands.get(i).label);
        }
        hand += "\n";
        //String msg = "Score:" + score + "\n" + hand + "pts=" + pts + " ptsValue=" + ptsValue + " faceDown=" + facedownValue + " setPotential=" + setPotentialValue + "\n";
        System.out.println(hand);        
    }
    
    private void generateHands(Golf golf, Card drawCard) {
        if (DEBUG_ENABLED) {
            System.out.println("-----------------------------------------------------\n" +
                               "Player " + getPlayerNum() + " generating hands from card: " + drawCard.toPrettyString());
        }
        hands.clear();
        final int cols = getNumCols();
        final int numRows = getNumRows();
        for (int i=0; i<numRows * cols; i++) {
            Hand hand = new Hand(new Card[numRows][]);
            for (int ii=0; ii<numRows; ii++) {
                hand.cards[ii] = getRow(ii).toArray(new Card[cols]);
            }
            hands.add(hand);
        }
        
        for (int i=0; i<hands.size(); i++) {
            int row = i / cols;
            int col = i % cols;
            Hand hand = hands.get(i);
            hand.replaced = hand.cards[row][col];
            hand.cards[row][col] = drawCard;

            computeHandScore(golf, hand);
        }
    }

    private Hand getBestHand() {
        Hand best = null;
        for (Hand hand : hands) {
            if (best == null || best.totalScore < hand.totalScore) {
                best = hand;
            }
        }
        best.label += " (B)";
        return best;
    }
    
    /**
     * Return a double value that ranks the hand.  
     * Should be value between 0-1 where 0 is the worst and 1 is the best
     * @param golf
     * @param cards
     * @return
     */
    protected void computeHandScore(Golf golf, Hand hand) {
        internalComputeHandScore(golf.getRules(), hand, golf.rand() % 1000);
    }
    
    /*
     * Package access for unit tests
     */
    static void internalComputeHandScore(Rules rules, Hand hand, double randFactor) {
        // get the actual showing score of this hand
        Card[][]cards = hand.cards;
        int pts = getHandPoints(rules, cards);
        if (Math.abs(pts) > maxPoints) {
            maxPoints = Math.abs(pts);
        }
        // get the number of face down cards and return an value whose graph is 2^x 
        double facedownValue = 1.0 - (Math.pow(2, countFaceDownCards(cards)) / 100);
        double setPotentialValue = 0;
        if (rules.getGameType() == Rules.GameType.NineCard) {
            // visit each cards and see check all adjacent cards to see if we make a set
            // potential sets are +1.  Max possible is 99 if all squares are same rank
            int [] xStep = { -1, 0, 1,  0, -2,  0, 2, 0 };
            int [] yStep = {  0, 1, 0, -1,  0, -2, 0, 2 };
            assert(xStep.length == yStep.length);
            for (int i=0; i<cards.length; i++) {
                for (int ii=0; ii<cards[i].length; ii++) {
                    if (!cards[i][ii].isShowing())
                        continue;
                    for (int iii=0; iii<xStep.length; iii++) {
                        int r = i+xStep[iii];
                        int c = ii+yStep[iii];
                        if (r>=0 && r<cards.length && c>=0 & c<cards[r].length) {
                            if (cards[r][c].isShowing() && isSet(rules, cards[i][ii], cards[r][c])) {
                                setPotentialValue += 1;
                            }
                        }
                    }
                }
            }
            if (setPotentialValue > maxSetPotential)
                maxSetPotential = setPotentialValue;
                
            setPotentialValue /= maxSetPotential;
        }
        
        final double randomFactor = 0.01;
        final double ptsFactor = 0.45;
        final double faceDownFactor = 0.2;
        final double setPotentialFactor = 0.34;
        
        final double ptsValue = (double)(maxPoints - pts) / (maxPoints * 2);
        final double randValue = randFactor / 1000;//(double)(golf.rand() % 1000) / 1000;

        final double ptsScore = ptsFactor*ptsValue;
        final double fdScore = faceDownFactor * facedownValue;
        final double spScore = setPotentialFactor * setPotentialValue;
        final double randScore = randValue * randomFactor; 
        
        final double score = ptsScore + fdScore + spScore + randScore;
        
        hand.pts = pts;
        hand.ptsScore = ptsScore;
        hand.faceDownScore = fdScore;
        hand.setPotentialScore = spScore;
        hand.totalScore = score;
        
        if (score < 0 || score > 1)
            System.err.println("Score should be between [0-1] but is: " + score);
        
        //return score;
    }
    
    public static int countFaceDownCards(Card [][] cards) {
        int numDown = 0;
        for (int i=0; i<cards.length; i++) {
            for (int ii=0; ii<cards[i].length; ii++) {
                if (!cards[i][ii].isShowing())
                    numDown++;
            }
        }
        return numDown;
    }
    
    @Override
    protected DrawType chooseDrawPile(Golf golf) {
        // see if we have an empty slot matching drawCard
        generateHands(golf, golf.getTopOfDiscardPile());
        Hand hand = new Hand(getCards());
        hand.label += "Current";
        computeHandScore(golf, hand);
        hands.addFirst(hand);
        
        Hand best = getBestHand();
        if (DEBUG_ENABLED) {
            debugWriteHands();
        }
        
        if (best.replaced == null)
            return DrawType.DTStack;
        
        return DrawType.DTDiscardPile;
    }

    @Override
    protected Card chooseDiscardOrPlay(Golf golf, Card drawCard) {
        
        generateHands(golf, drawCard);
        Hand hand = new Hand(getCards());
        hand.label = "Current";
        hand.replaced = drawCard;
        computeHandScore(golf, hand);
        hands.addFirst(hand);
        Hand best = getBestHand();
        
        if (DEBUG_ENABLED) {
            debugWriteHands();
        }

        return best.replaced;
    }

    @Override
    protected Card chooseCardToSwap(Golf golf, Card drawCard) {
        assert(drawCard != null);
        if (hands.size() == 0) {
            generateHands(golf, drawCard);
        }
        Card swapped = getBestHand().replaced;
        if (swapped == null) {
            debugWriteHands();
        }
        assert(swapped != null);
        return swapped;
    }

    /**
     * for debugging load/save.  Just any ole single line statement (no newline)
     * @return
     */
    public String getMessage() {
        return "I am a robot";
    }

    @Override
    protected int turnOverCard(Golf golf, int row) {
        return golf.rand() % golf.getRules().getGameType().getCols();
    }
    
}
