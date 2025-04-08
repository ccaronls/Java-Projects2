package cc.game.golf.console;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import cc.game.golf.ai.PlayerBot;
import cc.game.golf.core.*;

public class GolfConsole {

    public static void main(String [] args) {
        Golf.DEBUG_ENABLED = true;
        new GolfConsole(); 
    }

    BufferedReader reader = null;
    String readLine() {
        try {

            if (reader == null)
                reader = new BufferedReader(new InputStreamReader(System.in));
            printf("> ");
            return reader.readLine();
             
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        } 
        
        return null;
    }
    
    void println(String fmt, Object ... args) {
        System.out.println(String.format(fmt, args));
    }
    
    void printf(String fmt, Object ...args) {
        System.out.print(String.format(fmt, args));
    }
    
    class ConsolePlayer extends Player {

        @Override
        protected int turnOverCard(Golf golf, int row) {
            while (true) {
                int max = golf.getRules().getGameType().getCols()-1;
                println("Choose card from row " + (row+1) + " to turn over (0-" + max +")");
                try {
                    int num = Integer.parseInt(readLine().trim());
                    if (num >= 0 && num <= max) {
                        return num;
                    }
                } catch (Exception e) {}
                println("Invalid entry");
                
            }
        }

        @Override
        protected DrawType chooseDrawPile(Golf golf) {
            while (true) {
                println("draw from (s)tack or (p)ile?");
                try {
                    int s = readLine().trim().charAt(0);
                    if (s == 's') {
                        return DrawType.DTStack;
                    } else if (s == 'p') {
                        return DrawType.DTDiscardPile;
                    }
                } catch (Exception e) {}
                println("Invalid entry");
            }
        }

        @Override
        protected Card chooseDiscardOrPlay(Golf golf, Card drawCard) {
            while (true) {
                println("Pick card to swap (0-8) or (d)iscard");
                try {
                    int s = readLine().trim().charAt(0);
                    if (s == 'd') {
                        return drawCard;
                    }
                    int n = s-'0';
                    if (n >= 0 && n < 8) {
                        return this.getCard(n);
                    }
                } catch (Exception e) {}
                println("Invalid entry");
            }
        }

        @Override
        protected Card chooseCardToSwap(Golf golf, Card discardPileTop) {
            while (true) {
                println("Pick card to swap (0-8) or (d)iscard");
                try {
                    int s = readLine().trim().charAt(0);
                    int n = s-'0';
                    if (n >= 0 && n < 8) {
                        return this.getCard(n);
                    }
                } catch (Exception e) {}
                println("Invalid entry");
            }
        }
        
    }
    
    Golf g = new Golf();
    
 // heart diamond club spade
    String [][] cardSymbols = {
            { "  ^ ^ ",  "  /\\  ",  "   O  ", "   ^  " },
            { "  \\ / ",  " /  \\ ",  "  O O ", "  / \\ " },
            { "   v  ",  " \\  / ",  "   ^  ", "   ^  " },
            { "      ",  "  \\/  ",  "      ", "      " }
            
    };
    
    void drawCards2(List<Card> cards) {
        for (Card c : cards) {
            if (c == null)
                break;
            printf("  +------+");
        }
        printf("\n");
        for (Card c : cards) {
            if (c == null)
                break;
            if (c.isShowing())
                printf("  |%s    |", c.getRank().getRankString());
            else
                printf("  |      |");
        }
        printf("\n");
        for (int iii=0; iii<4; iii++) {
            for (Card c : cards) {
                if (c == null)
                    break;
                if (c.isShowing())
                    printf("  |%s|", cardSymbols[iii][c.getSuit().ordinal()]);
                else
                    printf("  |      |");
            }
            printf("\n");
        }
        for (Card c : cards) {
            if (c == null)
                break;
            printf("  +------+");
        }
        printf("\n");        
    }
    
    void drawCards(List<Card> cards) {
        if (cards.size() < 6)
            drawCardsWide(cards);
        else
            drawCardsCollapsed(cards);
    }
    
    void drawCardsCollapsed(List<Card> cards) {
        cards.remove(null);
        int maxCards = (80-6) / 3; // max number of cards we can display in 80 columns
        if (cards.size() > maxCards) {
            drawCardsCollapsed(cards.subList(0, maxCards));
            drawCardsCollapsed(cards.subList(maxCards, cards.size()));
        } else if (cards.size() > 0) {
            for (int i=0; i<cards.size()-1; i++) {
                //Card c = cards.get(i);
                printf("+--");
            }
            printf("+----+\n");
            for (int i=0; i<cards.size()-1; i++) {
                Card c = cards.get(i);
                printf("|%s", c.getRank().getRankString());
            }
            printf("|%s  |\n", cards.get(cards.size()-1).getRank().getRankString());
            for (int i=0; i<cards.size()-1; i++) {
                Card c = cards.get(i);
                printf("|%c ", c.getSuit().getSuitChar());
            }
            printf("|%c   |\n", cards.get(cards.size()-1).getSuit().getSuitChar());
            for (int i=0; i<cards.size()-1; i++) {
                //Card c = cards.get(i);
                printf("|  ");
            }
            printf("|    |\n");
            for (int i=0; i<cards.size()-1; i++) {
                //Card c = cards.get(i);
                printf("+--");
            }
            printf("+----+\n");
        }
    }
    
    void drawCardsWide(List<Card> cards) {
        for (Card c : cards) {
            if (c == null)
                break;
            printf("  +----+");
        }
        printf("\n");
        for (Card c : cards) {
            if (c == null)
                break;
            if (c.isShowing())
                printf("  |%s %c|", c.getRank().getRankString(), c.getSuit().getSuitChar());
            else
                printf("  |    |");
        }
        printf("\n");
        for (int i=0; i<2; i++) {
            for (Card c : cards) {
                if (c == null)
                    break;
                if (c.isShowing() && g.getRules().getWildcard().isWild(c))
                    printf("  |Wild|");
                else                    
                    printf("  |    |");
            }
            printf("\n");
        }
        for (Card c : cards) {
            if (c == null)
                break;
            printf("  +----+");
        }
        printf("\n");                
    }

    List<Card> temp = new ArrayList<Card>(2);

    void drawHeader() {
        printf("------------------------------------------------------------\n");
        printf(g.getState() + " Current Player: " + g.getCurrentPlayer() + " Dealer: " + g.getDealer() + "\n");
    }
    
    void drawGame() {
        final int rows = g.getRules().getGameType().getRows();
        final int cols = g.getRules().getGameType().getCols();
        final int numHandCards = rows * cols;
        for (int i=0; i<g.getNumPlayers(); i++) {
            println("");
            println("  Player " + i + " Points Showing: " + g.getPlayer(i).getHandPoints(g) + " total points: " + g.getPlayer(i).getPoints());
            int row = 0;
            for (int ii=0; ii<numHandCards; ii+=cols) {
                if (g.getPlayer(i).getNumCardsDealt() > ii)
                    drawCards(g.getPlayer(i).getRow(row++));
                    
            }
        }
        println("  Stack   Discard Pile");
        temp.clear();
        temp.add(new Card(0));
        temp.add(g.getTopOfDiscardPile());
        drawCards(temp);
    }
    
    GolfConsole() {
        
        final File SAVE_FILE = new File("savedrules.txt");
        Rules rules = new Rules();
        try  {
            rules.loadFromFile(SAVE_FILE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        while (true) {
            println("Rules:\n" + rules);
            printf("\nChange anything?\n>");
            String line = this.readLine();
            if (line == null || line.length() == 0)
                continue;
            if (line.trim().equals("no"))
                break;
            try {
                rules.deserialize(line);
                rules.saveToFile(SAVE_FILE);
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
        
        g.newGame(rules);
        g.addPlayer(new ConsolePlayer());
        g.addPlayer(new PlayerBot());
        //g.addPlayer(new PlayerBot());
        //g.addPlayer(new PlayerBot());
        g.newGame();
        drawCards(g.getDeck());

        while (g.getState() != State.GAME_OVER) {
            drawHeader();
            g.runGame();
            drawGame();
            //readLine();
        }
        printf("GAME OVER\n");
        for (int i=0; i<g.getNumPlayers(); i++) {
            Player p = g.getPlayer(i);
            printf("Player " + i + " ends with " + p.getPoints() + "\n");
        }
        printf("Player " + g.getWinner() + " is the winner");
    }
    
}
