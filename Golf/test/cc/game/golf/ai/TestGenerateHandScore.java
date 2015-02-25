package cc.game.golf.ai;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import cc.game.golf.ai.PlayerBot.Hand;
import cc.game.golf.core.Card;
import cc.game.golf.core.Rank;
import cc.game.golf.core.Rules;
import cc.game.golf.core.Suit;
import junit.framework.TestCase;

public class TestGenerateHandScore extends TestCase {
/*
Why is 2nd hand best?  Shouldn't it be 5?

+--++--++--+  +--++--++--+  +--++--++--+  +--++--++--+  +--++--++--+  +--++--++--+  +--++--++--+  +--++--++--+  +--++--++--+  +--++--++--+  
|8H||2C||9D|  |8C||2C||9D|  |8H||8C||9D|  |8H||2C||8C|  |8H||2C||9D|  |8H||2C||9D|  |8H||2C||9D|  |8H||2C||9D|  |8H||2C||9D|  |8H||2C||9D|  
|  ||  ||  |  |  ||  ||  |  |  ||  ||  |  |  ||  ||  |  |  ||  ||  |  |  ||  ||  |  |  ||  ||  |  |  ||  ||  |  |  ||  ||  |  |  ||  ||  |  
+--++--++--+  +--++--++--+  +--++--++--+  +--++--++--+  +--++--++--+  +--++--++--+  +--++--++--+  +--++--++--+  +--++--++--+  +--++--++--+  
+--++--++--+  +--++--++--+  +--++--++--+  +--++--++--+  +--++--++--+  +--++--++--+  +--++--++--+  +--++--++--+  +--++--++--+  +--++--++--+  
|  ||3S||9S|  |  ||3S||9S|  |  ||3S||9S|  |  ||3S||9S|  |8C||3S||9S|  |  ||8C||9S|  |  ||3S||8C|  |  ||3S||9S|  |  ||3S||9S|  |  ||3S||9S|  
|  ||  ||  |  |  ||  ||  |  |  ||  ||  |  |  ||  ||  |  |  ||  ||  |  |  ||  ||  |  |  ||  ||  |  |  ||  ||  |  |  ||  ||  |  |  ||  ||  |  
+--++--++--+  +--++--++--+  +--++--++--+  +--++--++--+  +--++--++--+  +--++--++--+  +--++--++--+  +--++--++--+  +--++--++--+  +--++--++--+  
+--++--++--+  +--++--++--+  +--++--++--+  +--++--++--+  +--++--++--+  +--++--++--+  +--++--++--+  +--++--++--+  +--++--++--+  +--++--++--+  
|  ||4C||  |  |  ||4C||  |  |  ||4C||  |  |  ||4C||  |  |  ||4C||  |  |  ||4C||  |  |  ||4C||  |  |8C||4C||  |  |  ||8C||  |  |  ||4C||8C|  
|  ||  ||  |  |  ||  ||  |  |  ||  ||  |  |  ||  ||  |  |  ||  ||  |  |  ||  ||  |  |  ||  ||  |  |  ||  ||  |  |  ||  ||  |  |  ||  ||  |  
+--++--++--+  +--++--++--+  +--++--++--+  +--++--++--+  +--++--++--+  +--++--++--+  +--++--++--+  +--++--++--+  +--++--++--+  +--++--++--+  
Pts   :   35  Pts   :   35  Pts   :   41  Pts   :   16  Pts   :   43  Pts   :   40  Pts   :   34  Pts   :   43  Pts   :   39  Pts   :   43  
Pscore:0.146  Pscore:0.146  Pscore:0.133  Pscore:0.189  Pscore:0.128  Pscore:0.135  Pscore:0.149  Pscore:0.128  Pscore:0.137  Pscore:0.128  
Fscore:0.184  Fscore:0.184  Fscore:0.184  Fscore:0.184  Fscore:0.192  Fscore:0.184  Fscore:0.184  Fscore:0.192  Fscore:0.184  Fscore:0.192  
Sscore:0.240  Sscore:0.300  Sscore:0.150  Sscore:0.225  Sscore:0.300  Sscore:0.240  Sscore:0.180  Sscore:0.240  Sscore:0.240  Sscore:0.240  
Total :0.572  Total :0.671  Total :0.507  Total :0.602  Total :0.659  Total :0.564  Total :0.549  Total :0.573  Total :0.577  Total :0.581   
               (B)
               
               
               
*/
    
    public void test() throws IOException {
        Card [][] cards = {
                {
                    new Card(Rank.EIGHT, Suit.CLUBS, true),
                    new Card(Rank.TWO, Suit.CLUBS, true),
                    new Card(Rank.NINE, Suit.DIAMONDS, true),
                },
                {
                    new Card(Rank.EIGHT, Suit.HEARTS, false),
                    new Card(Rank.THREE, Suit.SPADES, true),
                    new Card(Rank.NINE, Suit.SPADES, true),
                },
                {
                    new Card(Rank.EIGHT, Suit.HEARTS, false),
                    new Card(Rank.FOUR, Suit.CLUBS, true),
                    new Card(Rank.NINE, Suit.DIAMONDS, false),
                }
        };
        Card [][] cards2 = {
                {
                    new Card(Rank.EIGHT, Suit.HEARTS, true),
                    new Card(Rank.TWO, Suit.CLUBS, true),
                    new Card(Rank.NINE, Suit.DIAMONDS, true),
                },
                {
                    new Card(Rank.EIGHT, Suit.HEARTS, false),
                    new Card(Rank.THREE, Suit.SPADES, true),
                    new Card(Rank.NINE, Suit.SPADES, true),
                },
                {
                    new Card(Rank.EIGHT, Suit.HEARTS, false),
                    new Card(Rank.FOUR, Suit.CLUBS, true),
                    new Card(Rank.NINE, Suit.DIAMONDS, false),
                }
        };
        Card [][] cards3 = {
                {
                    new Card(Rank.QUEEN, Suit.SPADES, true),
                    new Card(Rank.TWO, Suit.CLUBS, true),
                    new Card(Rank.NINE, Suit.DIAMONDS, false),
                },
                {
                    new Card(Rank.EIGHT, Suit.HEARTS, false),
                    new Card(Rank.THREE, Suit.SPADES, false),
                    new Card(Rank.NINE, Suit.SPADES, true),
                },
                {
                    new Card(Rank.EIGHT, Suit.HEARTS, false),
                    new Card(Rank.FOUR, Suit.CLUBS, true),
                    new Card(Rank.NINE, Suit.DIAMONDS, false),
                }
        };
        
        Hand hand1 = new Hand(cards);
        Hand hand2 = new Hand(cards2);
        Hand hand3 = new Hand(cards3);
        Rules rules = new Rules();
        rules.loadFromFile(new File("savedrules.txt"));
        PlayerBot.internalComputeHandScore(rules, hand1, 0);
        PlayerBot.internalComputeHandScore(rules, hand2, 0);
        PlayerBot.internalComputeHandScore(rules, hand3, 0);
        ArrayList<Hand> hands = new ArrayList();
        hands.add(hand3);
        hands.add(hand2);
        hands.add(hand1);
        PlayerBot.debugWriteHands(hands, 3, 3);
    }
}
