package cc.game.kaiser.net;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import cc.game.kaiser.core.*;
import cc.lib.net.*;

public class KaiserClient extends GameClient {

    class TempPlayer extends Player {

        public TempPlayer(String name) {
            super(name);
        }

        @Override
        public int playTrick(Kaiser kaiser, int[] options, int numOptions) {
            throw new RuntimeException("Should never get called");
        }

        @Override
        public Bid makeBid(Kaiser kaiser, int numOptions) {
            throw new RuntimeException("Should never get called");
        }
        
    }
    
    Team [] team = new Team[2];
    Hand hand = new Hand();
    TempPlayer [] players = new TempPlayer[4];
    Card [] trick = new Card[4];
    int trickWinnerPlayerNum;
    int dealerPlayerNum;
    int startPlayerNum;
    
    public KaiserClient(String userName, String host) {
        super(userName, host, Common.PORT, Common.VERSION);
        
        players[0] = new TempPlayer("");
        players[1] = new TempPlayer("");
        players[2] = new TempPlayer("");
        players[3] = new TempPlayer("");
        
        team[0] = new Team("A", players[0], players[2]);
        team[1] = new Team("B", players[1], players[3]);

    }

    @Override
    protected void onMessage(String message) {
        System.out.println(message);
    }

    @Override
    protected void onDisconnected(String message) {
        System.out.println("You have been dropped: " + message);
        synchronized (this) {
            notify();
        }
    }

    @Override
    protected void onConnected() {
        System.out.println("Client " + getName() + " is connected");
        
    }

    @Override
    protected void onCommand(GameCommand cmd) {
        if (cmd.getType() == Common.MAKE_BID) {
            
        } else if (cmd.getType() == Common.PLAY_TRICK) {
            int numOptions = Integer.parseInt(cmd.getArg("numOptions"));
            int [] options = new int[numOptions];
            String [] ops = cmd.getArg("options").split("[^0-9]+");
            int i = 0;
            for (String op : ops) {
                options[i++] = Integer.parseInt(op);
            }
            
        } else if (cmd.getType() == Common.DEALT_CARD) {
            Suit suit = Suit.valueOf(cmd.getArg("suit"));
            Rank rank = Rank.valueOf(cmd.getArg("rank"));
            Card card = Kaiser.getCard(rank, suit);
            this.hand.addCard(card);
            printf("You were dealt a %s", card.toPrettyString());
        } else if (cmd.getType() == Common.SET_PLAYER) {
            int num = Integer.parseInt(cmd.getArg("num"));
            String name = cmd.getArg("name");
            players[num].setName(name);
        } else if (cmd.getType() == Common.CARD_PLAYED) {
            int num = Integer.parseInt(cmd.getArg("num"));
            Suit suit = Suit.valueOf(cmd.getArg("suit"));
            Rank rank = Rank.valueOf(cmd.getArg("rank"));
            Card card = Kaiser.getCard(rank, suit);
            trick[num] = card;
        }
    }

    void startGame() {
        team[0] = new Team("A", players[0], players[2]);
        team[1] = new Team("B", players[1], players[3]);
    }
    
    BufferedReader reader = null;
    String readLine() {
        try {

            if (reader == null)
                reader = new BufferedReader(new InputStreamReader(System.in));
            return reader.readLine();
             
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        } 
        
        return null;
    }
    
    void printf(String fmt, Object ...args) {
        System.out.print(String.format(fmt, args));
    }
    
    void drawCards(Hand [] hands, int numHands, boolean faceUp)
    {
        final int CARD_HEIGHT = 3;
        int i, ii;
        if (numHands <= 0) {
            printf("\n<NONE>\n\n");
            return;
        }

        String cardsSpacing = "     ";

        final int MAX_HANDS_PER_ROW = 3;

        //int handsShown = 0;
        int start = 0;
        while (start < numHands) {
            int end = numHands;
            if (end - start > MAX_HANDS_PER_ROW) {
                end = start + MAX_HANDS_PER_ROW;
            }
            for (ii=start; ii<end; ii++) {
                for (i=0; i<hands[ii].getNumCards()-1; i++) {
                    printf("+--");
                }
                printf("+------+%s", cardsSpacing);
            }
            printf("\n");
            for (ii=start; ii<end; ii++) {
                for (i=0; i<hands[ii].getNumCards()-1; i++) {
                    printf("|%s", faceUp ? hands[ii].getCard(i).rank.getRankString() : "  ");
                }
                printf("|%s    |%s", faceUp ? hands[ii].getCard(i).rank.getRankString() : "  ", cardsSpacing);
            }
            printf("\n");
            for (ii=start; ii<end; ii++) {
                for (i=0; i<hands[ii].getNumCards()-1; i++) {
                    printf("|%c ", faceUp ? hands[ii].getCard(i).suit.getSuitChar() : ' ');
                }
                printf("|%c     |%s", faceUp ? hands[ii].getCard(i).suit.getSuitChar() : ' ', cardsSpacing);
            }
            printf("\n");
            for (int iii=0; iii<CARD_HEIGHT; iii++) {
                for (ii=0; ii<(end-start); ii++) {
                    for (i=0; i<hands[ii].getNumCards()-1; i++) {
                        printf("|  ");
                    }
                    printf("|      |%s", cardsSpacing);
                }    
                printf("\n");
            }
            for (ii=start; ii<end; ii++) {
                for (i=0; i<hands[ii].getNumCards()-1; i++) {
                    printf("+--");
                }
                printf("+------+%s", cardsSpacing);
            }
            printf("\n");
            start = end;
        }
    }
    
    void drawHeader(int numRounds, String dealer, Suit trump, Team teamA, Team teamB)
    {
        printf(
            "\n"
            + "---------------------------------------------------\n"
            + "Round: %d      Dealer: %s    Trump: %s\n"
            + "\n"
            + "           TEAM A           TEAM B\n"
            + "Player 1   %-10s       %s\n"
            + "Player 2   %-10s       %s\n"
            + "Bid        %-10s       %s\n"
            + "Total      %-10d       %d\n"
            + "Round      %-10d       %d\n"
            + "\n"
            , numRounds
            , dealer
            , trump.getSuitString()
            , teamA.getPlayerA().getName()
            , teamB.getPlayerA().getName()
            , teamA.getPlayerB().getName()
            , teamB.getPlayerB().getName()
            , teamA.getBid()
            , teamB.getBid()
            , teamA.getTotalPoints()
            , teamB.getTotalPoints()
            , teamA.getRoundPoints()
            , teamB.getRoundPoints()
            );


    }

    void drawRoundResult(int numRounds)
    {
        printf("\n\nResults of Round %d\n\n", numRounds);
        for (int i=0; i<4; i++) {
            Player p = players[i];
            printf("\n%s (Team %s)\n", p.getName(), p.getTeam().getName());
            //for (int ii=0; ii<p.getNumTricks(); ii++) {
            //    drawCards(p.getTrick(ii), true);
            //}
            drawCards(p.getTricks(), p.getNumTricks(), true);
        }
    }    
    
    String getPlayerHeader(Player p)
    {
        Player tw = getPlayer(trickWinnerPlayerNum);
        String s = p.getName();
        if (dealerPlayerNum == p.getPlayerNum() ||
            startPlayerNum == p.getPlayerNum() ||
            tw == p) {
            s += " [";
            if (dealerPlayerNum == p.getPlayerNum())
                s += "D";
            if (startPlayerNum == p.getPlayerNum())
                s += "S";
            if (p == tw)
                s += "W";
            s += "]";
        }
        return s;
    }
    
    // heart diamond club spade
    String [][] cardSymbols = {
            { "  ^ ^ ",  "  /\\  ",  "   O  ", "   ^  " },
            { "  \\ / ",  " /  \\ ",  "  O O ", "  / \\ " },
            { "   v  ",  " \\  / ",  "   ^  ", "   ^  " },
            { "      ",  "  \\/  ",  "      ", "      " }
            
    };
    
    Card getTrick(int index) {
        return trick[index];
    }

    Player getPlayer(int num) {
        return players[num];
    }
    
    void drawTrick()
    {
        // arrange the played cards as follows:
        /*
                     Name(1)
                    +----+
                    |    |
                    |    |
                    +----+
             Name(0)         Name(2)
            +----+          +----+
            |    |          |    |
            |    |          |    |
            +----+          +----+
                     you(3)
                    +----+
                    |    |
                    |    |
                    +----+

        */

        Card card = getTrick(1);
        String indentStr = "                    ";
        String indent = indentStr;
        printf(
            "%s  %s\n"
            + "%s+------+\n"
            + "%s|%s    |\n"
            //+ "%s|%c     |\n"
            + "%s|%s|\n"
            + "%s|%s|\n"
            + "%s|%s|\n"
            + "%s|%s|\n"
            + "%s+------+\n"
            ,indent, getPlayerHeader(getPlayer(1))
            ,indent
            ,indent, card == null ? "  " : card.rank.getRankString()
            //,indent, card == null ? ' ' : card.suit.getSuitChar()
            ,indent, card == null ? "      " : cardSymbols[0][card.suit.ordinal()] 
            ,indent, card == null ? "      " : cardSymbols[1][card.suit.ordinal()]
            ,indent, card == null ? "      " : cardSymbols[2][card.suit.ordinal()]
            ,indent, card == null ? "      " : cardSymbols[3][card.suit.ordinal()]
            ,indent
        );

        indentStr = "          ";
        indent = indentStr;
        String spacingStr = "            ";
        String spacing = spacingStr;
        Card card0 = getTrick(0);
        Card card1 = getTrick(2);
        printf(
            "%s %-8s%s %s\n"
            + "%s+------+%s+------+\n"
            + "%s|%s    |%s|%s    |\n"
            //+ "%s|%c     |%s|%c     |\n"
            + "%s|%s|%s|%s|\n"
            + "%s|%s|%s|%s|\n"
            + "%s|%s|%s|%s|\n"
            + "%s|%s|%s|%s|\n"
            + "%s+------+%s+------+\n"
            ,indent, getPlayerHeader(getPlayer(0)), spacing, getPlayerHeader(getPlayer(2))
            ,indent, spacing
            ,indent, card0 == null ? "  " : card0.rank.getRankString(), spacing, card1 == null ? "  " : card1.rank.getRankString()
            //,indent, card0 == null ? ' ' : card0.suit.getSuitChar(), spacing, card1 == null ? ' ' : card1.suit.getSuitChar()
            ,indent, card0 == null ? "      " : cardSymbols[0][card0.suit.ordinal()], spacing, card1 == null ? "      " : cardSymbols[0][card1.suit.ordinal()]
            ,indent, card0 == null ? "      " : cardSymbols[1][card0.suit.ordinal()], spacing, card1 == null ? "      " : cardSymbols[1][card1.suit.ordinal()]
            ,indent, card0 == null ? "      " : cardSymbols[2][card0.suit.ordinal()], spacing, card1 == null ? "      " : cardSymbols[2][card1.suit.ordinal()]
            ,indent, card0 == null ? "      " : cardSymbols[3][card0.suit.ordinal()], spacing, card1 == null ? "      " : cardSymbols[3][card1.suit.ordinal()]
            ,indent, spacing
            );

        card = getTrick(3);
        indentStr = "                    ";
        indent = indentStr;
        printf(
            "%s  %s\n"
            + "%s+------+\n"
            + "%s|%s    |\n"
            //+ "%s|%c     |\n"
            + "%s|%s|\n"
            + "%s|%s|\n"
            + "%s|%s|\n"
            + "%s|%s|\n"
            + "%s+------+\n"
            ,indent, getPlayerHeader(getPlayer(3))
            ,indent
            ,indent, card == null ? "  " : card.rank.getRankString()
            //,indent, card == null ? ' ' : card.suit.getSuitChar()
            ,indent, card == null ? "      " : cardSymbols[0][card.suit.ordinal()] 
            ,indent, card == null ? "      " : cardSymbols[1][card.suit.ordinal()]
            ,indent, card == null ? "      " : cardSymbols[2][card.suit.ordinal()]
            ,indent, card == null ? "      " : cardSymbols[3][card.suit.ordinal()]
            ,indent);

    }

    String bidToString(Bid bid)
    {
        String s = "";
        if (bid.numTricks == 0)
            s = "No Bid";
        else {
            s += String.valueOf(bid.numTricks);
            s += " tricks ";
            s += bid.trump.getSuitString();
        }
        return s;
    }

    
    
    
    
    
    
    
    
    
    
    
    
    
    public static void main(String [] argv) {

        try {
            
            if (argv.length != 2) {
                System.out.println("USAGE: KaiserClient <username> <host>");
                System.exit(1);
            }
                
            KaiserClient client = new KaiserClient(argv[0], argv[1]);
            client.connect();
            
            while (client.isConnected()) {
                synchronized (client) {
                    client.wait();
                }
            }
            
            System.out.println("Client has been disconnected");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
    
}
