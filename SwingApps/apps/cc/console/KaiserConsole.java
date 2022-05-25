package cc.console;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import cc.game.kaiser.ai.PlayerBot;
import cc.game.kaiser.core.Bid;
import cc.game.kaiser.core.Card;
import cc.game.kaiser.core.Hand;
import cc.game.kaiser.core.Kaiser;
import cc.game.kaiser.core.Player;
import cc.lib.game.Utils;

public class KaiserConsole { 

    public static void main(String [] args)
    {
        KaiserConsole game = new KaiserConsole();

        game.chooseGameType();
    }

    void chooseGameType() {
        String option = null;
        String name = null;
        while (true) {
            while (name == null || name.length() <= 0) {
                printf("Enter your name >");
                name = readLine().trim();
            }
            
            do {
                printf("\n\n");
                printf("Welcome %s\n", name);
                printf("S>   Single Player\n");
                printf("H>   Host Multi Player\n");
                printf("J>   Join Multi Player\n");
                printf("Q>   Quit");
                option = readLine().trim().toUpperCase();
            } while (option.length() <= 0);
            switch (option.charAt(0)) {
                case 'S': singlePlayerGame(name); break;
                case 'Q': System.exit(0); break;
                default:
                    printf("Unknown command: %c", option.charAt(0));
                    break;
            }
        }
        
    }

    void singlePlayerGame(String name) {
        
        Kaiser kaiser = new Kaiser();
        int nm = 0;
        String [] names = { "Simon", "Beth", "Max", "Joan" };
        int random = Utils.rand() % 4;
        int i=0;
        for ( ; i<random; i++) {
            kaiser.setPlayer(i, new PlayerBot(names[nm++]));
        }
        kaiser.setPlayer(i++, new ConsolePlayer(name));
        for ( ; i<4; i++)
            kaiser.setPlayer(i, new PlayerBot(names[nm++]));
        
        while (true) {
            drawGame(kaiser);
            kaiser.runGame();
            if (kaiser.isGameOver()) {
                kaiser.newGame();
            }
        }
    }

    static final int PORT = 32323;
    static final String VERSION = "KaiserConsole";
    
    ConsolePlayer thisPlayer = null;
    
    /*
    class Client extends GameClient implements KaiserCommand.Listener {

        int playerNum;
        Kaiser kaiser = new Kaiser();
        
        public Client(String userName, String host) {
            super(userName, host, PORT, VERSION);
        }

        @Override
        protected void onMessage(String message) {
            printf("\nMESSAGE:\n   " + message + "\n\n");
        }

        @Override
        protected void onDisconnected(String message) {
            printf("DISCONNECTED");
        }

        @Override
        protected void onConnected() {
            printf("CONNECTED");
        }

        @Override
        protected void onCommand(final GameCommand cmd) {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        KaiserCommand.clientDecode(Client.this, cmd);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

        @Override
        public void onSetPlayer(int num, String name) {
            if (name.equals(getName()))
                kaiser.setPlayer(num, thisPlayer = new ConsolePlayer(name));
            else
                kaiser.setPlayer(num, new TempPlayer(name));
        }

        @Override
        public void onPlayTrick(final Card[] options) {
            new Thread(new Runnable() {
                public void run() {
                    Card card = thisPlayer.playTrick(kaiser, options);
                    if (card  != null)
                        Client.this.send(KaiserCommand.clientPlayTrick(card));
                }
            }).start();
        }

        @Override
        public void onMakeBid(Bid[] bids) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void onDealtCard(Card card) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void onGameUpdate(InputStream in) {
            // TODO Auto-generated method stub
            
        }
    }
    

    class TempPlayer extends Player {

        public TempPlayer(String name) {
            super(name);
        }

        @Override
        public Card playTrick(Kaiser kaiser, Card [] card) {
            throw new RuntimeException("Should never get called");
        }

        @Override
        public Bid makeBid(Kaiser kaiser, Bid [] options) {
            throw new RuntimeException("Should never get called");
        }
        
    }*/


    
    
    KaiserConsole() {

        
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

    class ConsolePlayer extends  Player
    {

        ConsolePlayer(String nm) {
            super(nm);
        }

        @Override
        public Card playTrick(Kaiser kaiser, Card [] cards) {
            Card card = null;

            drawHeader(kaiser);
            drawTrick(kaiser, 0);//getPlayerNum());
            drawCards(new Hand[] { getHand() }, 1, true);

            for (int i=0; i<getNumCards(); i++) {
                boolean inList = isInArray(getCard(i), cards);
                printf("%c%d%c", inList ? '[' : ' ', i+1, inList ? ']' : ' ');
            }
            printf("\n\n");

            while (true) {
                printf("Choose card to play [1-%d]:", getNumCards());

                String line = readLine();
                int num = 0;
                try {
                    num = Integer.parseInt(line.trim());
                } catch (Exception e) {
                    printf("\n\nInvalid entry.\n\n");
                    continue;
                }
                if (num < 1 || num > getNumCards()) {
                    printf("\n\nInvalid Entry.\n\n");
                    continue;
                }

                card = getCard(num - 1);

                if (isInArray(card, cards))
                    break;

                printf("\n\n%s is not a valid card to play.\n\n", card.toPrettyString());
                
            }

            printf("\n\n");
            return card;

        }

        @Override
        public Bid makeBid(Kaiser kaiser, Bid [] options) {
            drawHeader(kaiser);
            drawCards(new Hand[] { getHand() }, 1, true);
            for (int i=0; i<options.length; i++) {
                printf("%-2d - %-20s ", i+1, bidToString(options[i]));
                if ((i+1) % 2 == 1)
                    printf("\n");
            }

            int op = 0;
            while (true)
            {

                printf("\nChoose bid option:\n");
                String line = readLine();
                try {
                    op = Integer.parseInt(line.trim());
                } catch (Exception e) {
                    printf("\n\nInvalid Entry\n\n");
                    continue;
                }

                if (op < 0 || op > options.length) {
                    printf("\n\nNot an option\n\n");
                    continue;
                }

                break;
            }

            if (op == 0)
                return null;

            return options[op-1];            
        }

    };

    void drawHeader(Kaiser kaiser)
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
            , kaiser.getNumRounds()
            , kaiser.getPlayer(kaiser.getDealer()).getName()
            , kaiser.getTrump().getSuitString()
            , kaiser.getPlayer(kaiser.getTeam(0).getPlayerA()).getName()
            , kaiser.getPlayer(kaiser.getTeam(1).getPlayerA()).getName()
            , kaiser.getPlayer(kaiser.getTeam(0).getPlayerB()).getName()
            , kaiser.getPlayer(kaiser.getTeam(1).getPlayerB()).getName()
            , kaiser.getTeam(0).getBid()
            , kaiser.getTeam(1).getBid()
            , kaiser.getTeam(0).getTotalPoints()
            , kaiser.getTeam(1).getTotalPoints()
            , kaiser.getTeam(0).getRoundPoints()
            , kaiser.getTeam(1).getRoundPoints()
            );


    }

    void drawRoundResult(Kaiser kaiser)
    {
        printf("\n\nResults of Round %d\n\n", kaiser.getNumRounds());
        for (int i=0; i<4; i++) {
            Player p = kaiser.getPlayer(i);
            printf("\n%s (Team %s)\n", p.getName(), kaiser.getTeam(p.getTeam()).getName());
            Hand [] tricks = p.getTricks();
            drawCards(tricks, p.getNumTricks(), true);
        }
    }
    
    void getch() {
        readLine();
    }

    void drawGame(Kaiser kaiser)
    {
        switch (kaiser.getState())
        {
        case NEW_GAME:
            printf("Press any key to start a new game\n");
            getch();
            break;

        case NEW_ROUND:
            break;

        case DEAL:
            break;

        case BID:
            break;

        case TRICK:
            break;

        case PROCESS_TRICK:
            drawTrick(kaiser, 0);
            break;

        case RESET_TRICK:
            printf("\n\nPress any key to continue\n\n");
            getch();
            break;

        case PROCESS_ROUND:
            drawHeader(kaiser);
            drawRoundResult(kaiser);
            break;

        case GAME_OVER:
            printf("\n\n  G A M E   O V E R    \n\n");
            break;

        }
    }

    static <T> boolean isInArray(T value, T [] array) {
        for (int i=0; i<array.length; i++)
            if (value.equals(array[i]))
                return true;
        return false;
    }

    String getPlayerHeader(Kaiser kaiser, Player p)
    {
        Player tw = kaiser.getTrickWinner();
        String s = p.getName();
        if (kaiser.getDealer() == p.getPlayerNum() ||
                kaiser.getStartPlayer() == p.getPlayerNum() ||
            tw == p) {
            s += " [";
            if (kaiser.getDealer() == p.getPlayerNum())
                s += "D";
            if (kaiser.getStartPlayer() == p.getPlayerNum())
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

    void drawTrick(Kaiser kaiser, final int frontPlayer)
    {
        // arrange the played cards as follows:
        /*
                     Name(fp+2)
                    +----+
                    |    |
                    |    |
                    +----+
             Name(fp+1)         Name(fp+3)
            +----+          +----+
            |    |          |    |
            |    |          |    |
            +----+          +----+
                     you(fp)
                    +----+
                    |    |
                    |    |
                    +----+

        */

        final int backPlayer = (frontPlayer+2)%4;
        final int leftPlayer = (frontPlayer+1)%4;
        final int rightPlayer = (frontPlayer+3)%4;
        
        
        Card card = kaiser.getTrick(backPlayer);
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
            ,indent, getPlayerHeader(kaiser, kaiser.getPlayer(backPlayer))
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
        Card card0 = kaiser.getTrick(leftPlayer);
        Card card1 = kaiser.getTrick(rightPlayer);
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
            ,indent, getPlayerHeader(kaiser, kaiser.getPlayer(leftPlayer)), 
                spacing, getPlayerHeader(kaiser, kaiser.getPlayer(rightPlayer))
            ,indent, spacing
            ,indent, card0 == null ? "  " : card0.rank.getRankString(), spacing, card1 == null ? "  " : card1.rank.getRankString()
            //,indent, card0 == null ? ' ' : card0.suit.getSuitChar(), spacing, card1 == null ? ' ' : card1.suit.getSuitChar()
            ,indent, card0 == null ? "      " : cardSymbols[0][card0.suit.ordinal()], spacing, card1 == null ? "      " : cardSymbols[0][card1.suit.ordinal()]
            ,indent, card0 == null ? "      " : cardSymbols[1][card0.suit.ordinal()], spacing, card1 == null ? "      " : cardSymbols[1][card1.suit.ordinal()]
            ,indent, card0 == null ? "      " : cardSymbols[2][card0.suit.ordinal()], spacing, card1 == null ? "      " : cardSymbols[2][card1.suit.ordinal()]
            ,indent, card0 == null ? "      " : cardSymbols[3][card0.suit.ordinal()], spacing, card1 == null ? "      " : cardSymbols[3][card1.suit.ordinal()]
            ,indent, spacing
            );

        card = kaiser.getTrick(frontPlayer);
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
            ,indent, getPlayerHeader(kaiser, kaiser.getPlayer(frontPlayer))
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

}
