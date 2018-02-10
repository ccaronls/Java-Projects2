package cc.game.golf.swing;

import java.awt.Font;
import java.util.*;

import cc.game.golf.ai.PlayerBot;
import cc.game.golf.core.*;
import cc.game.golf.core.Rules.GameType;
import cc.game.golf.core.Rules.KnockerBonusType;
import cc.game.golf.core.Rules.KnockerPenaltyType;
import cc.game.golf.core.Rules.WildCard;
import cc.lib.game.*;
import cc.lib.swing.AWTGraphics;
import cc.lib.swing.EZFrame;
import cc.lib.swing.KeyboardAnimationApplet;

/**
 * GolfSwing (get it?)
 * @author ccaron
 *
 */
public class GolfSwing extends KeyboardAnimationApplet {

    public static void main(String [] args) {
        AGraphics.DEBUG_ENABLED = true;
        Utils.DEBUG_ENABLED = true;
        Golf.DEBUG_ENABLED = true;
        PlayerBot.DEBUG_ENABLED = true;
        EZFrame frame = new EZFrame("Golf Card Game");
        KeyboardAnimationApplet app = new GolfSwing();
        frame.add(app);
        frame.centerToScreen(800, 600);
        app.init();
        app.start();
        app.setTargetFPS(30);
    }   
    
    private int tableImageId = -1;
    private final int [] cardDownImage = new int[4];
    private final List<Card> pickableCards = new ArrayList<Card>();
    private int pickedCard = -1;
    private int highlightedCard = -1;
    private int screenWidth, screenHeight;
    private int cardWidth = 0;
    private int cardHeight = 0;
    private String message = "";
    private final List<AAnimation> animations = new LinkedList<AAnimation>();
    
    private IGolfGame game;
    
    public GolfSwing() {
        
    }
    
    enum Angle {
        ANG_0(0), 
        ANG_90(90), 
        ANG_180(180), 
        ANG_270(270);
        
        final int degrees;
        
        Angle(int degrees) {
            this.degrees = degrees;
        }
    }
    
    @SuppressWarnings("rawtypes")
    class CardImage implements Comparable{ //<CardImage> { - This is temping, but we need to be able to compare against integer
        final Rank rank;
        final Suit suit;
        final String fileName;
        int [] imageId = new int[4];
        
        public CardImage(Rank rank, Suit suit, String fileName) {
            super();
            this.rank = rank;
            this.suit = suit;
            this.fileName = fileName;
        }

        @Override
        public int compareTo(Object obj) {
            int i0 = rank.ordinal() << 16 | suit.ordinal();
            int i1;
            if (obj instanceof CardImage) {
                CardImage c = (CardImage)obj;
                i1 = c.rank.ordinal() << 16 | c.suit.ordinal();
            } else {
                i1 = (Integer)obj;
            }
            return i0-i1;
        }

/*        @Override
        public int compareTo(CardImage card) {
            if (rank == card.rank)
                return suit.ordinal() - card.suit.ordinal();
            return rank.ordinal() - card.rank.ordinal(); 
        }*/
    }
    
    private CardImage [] cardImages = {
      new CardImage(Rank.ACE,   Suit.CLUBS,       "1.png"),      
      new CardImage(Rank.ACE,   Suit.SPADES,      "2.png"),      
      new CardImage(Rank.ACE,   Suit.HEARTS,      "3.png"),      
      new CardImage(Rank.ACE,   Suit.DIAMONDS,    "4.png"),      
      new CardImage(Rank.KING,  Suit.CLUBS,       "5.png"),      
      new CardImage(Rank.KING,  Suit.SPADES,      "6.png"),      
      new CardImage(Rank.KING,  Suit.HEARTS,      "7.png"),      
      new CardImage(Rank.KING,  Suit.DIAMONDS,    "8.png"),      
      new CardImage(Rank.QUEEN, Suit.CLUBS,       "9.png"),      
      new CardImage(Rank.QUEEN, Suit.SPADES,      "10.png"),      
      new CardImage(Rank.QUEEN, Suit.HEARTS,      "11.png"),      
      new CardImage(Rank.QUEEN, Suit.DIAMONDS,    "12.png"),      
      new CardImage(Rank.JACK,  Suit.CLUBS,       "13.png"),      
      new CardImage(Rank.JACK,  Suit.SPADES,      "14.png"),      
      new CardImage(Rank.JACK,  Suit.HEARTS,      "15.png"),      
      new CardImage(Rank.JACK,  Suit.DIAMONDS,    "16.png"),      
      new CardImage(Rank.TEN,   Suit.CLUBS,       "17.png"),      
      new CardImage(Rank.TEN,   Suit.SPADES,      "18.png"),      
      new CardImage(Rank.TEN,   Suit.HEARTS,      "19.png"),      
      new CardImage(Rank.TEN,   Suit.DIAMONDS,    "20.png"),      
      new CardImage(Rank.NINE,  Suit.CLUBS,       "21.png"),      
      new CardImage(Rank.NINE,  Suit.SPADES,      "22.png"),      
      new CardImage(Rank.NINE,  Suit.HEARTS,      "23.png"),      
      new CardImage(Rank.NINE,  Suit.DIAMONDS,    "24.png"),      
      new CardImage(Rank.EIGHT, Suit.CLUBS,       "25.png"),      
      new CardImage(Rank.EIGHT, Suit.SPADES,      "26.png"),      
      new CardImage(Rank.EIGHT, Suit.HEARTS,      "27.png"),      
      new CardImage(Rank.EIGHT, Suit.DIAMONDS,    "28.png"),      
      new CardImage(Rank.SEVEN, Suit.CLUBS,       "29.png"),      
      new CardImage(Rank.SEVEN, Suit.SPADES,      "30.png"),      
      new CardImage(Rank.SEVEN, Suit.HEARTS,      "31.png"),      
      new CardImage(Rank.SEVEN, Suit.DIAMONDS,    "32.png"),      
      new CardImage(Rank.SIX,   Suit.CLUBS,       "33.png"),      
      new CardImage(Rank.SIX,   Suit.SPADES,      "34.png"),      
      new CardImage(Rank.SIX,   Suit.HEARTS,      "35.png"),      
      new CardImage(Rank.SIX,   Suit.DIAMONDS,    "36.png"),      
      new CardImage(Rank.FIVE,  Suit.CLUBS,       "37.png"),      
      new CardImage(Rank.FIVE,  Suit.SPADES,      "38.png"),      
      new CardImage(Rank.FIVE,  Suit.HEARTS,      "39.png"),      
      new CardImage(Rank.FIVE,  Suit.DIAMONDS,    "40.png"),      
      new CardImage(Rank.FOUR,  Suit.CLUBS,       "41.png"),      
      new CardImage(Rank.FOUR,  Suit.SPADES,      "42.png"),      
      new CardImage(Rank.FOUR,  Suit.HEARTS,      "43.png"),      
      new CardImage(Rank.FOUR,  Suit.DIAMONDS,    "44.png"),      
      new CardImage(Rank.THREE, Suit.CLUBS,       "45.png"),      
      new CardImage(Rank.THREE, Suit.SPADES,      "46.png"),      
      new CardImage(Rank.THREE, Suit.HEARTS,      "47.png"),      
      new CardImage(Rank.THREE, Suit.DIAMONDS,    "48.png"),      
      new CardImage(Rank.TWO,   Suit.CLUBS,       "49.png"),      
      new CardImage(Rank.TWO,   Suit.SPADES,      "50.png"),      
      new CardImage(Rank.TWO,   Suit.HEARTS,      "51.png"),      
      new CardImage(Rank.TWO,   Suit.DIAMONDS,    "52.png"),    
      new CardImage(Rank.JOKER, Suit.BLACK,       "53.png"),
      new CardImage(Rank.JOKER, Suit.RED,         "54.png"),
    };

    int pickCard(List<Card> options) {
        pickedCard = -1;
        pickableCards.clear();
        pickableCards.addAll(options);
        try {
            synchronized (this) {
                wait(10000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        pickableCards.clear();
        return pickedCard;        
    }
    
    @Override
    protected void doInitialization() {
        newSinglePlayerGame();
    }
    
    private void loadImages(AGraphics g) {
        for (CardImage c: cardImages) {            
            c.imageId[Angle.ANG_0.ordinal()] = g.loadImage(c.fileName);
            c.imageId[Angle.ANG_90.ordinal()] = g.newRotatedImage(c.imageId[0], Angle.ANG_90.degrees);
            c.imageId[Angle.ANG_180.ordinal()] = g.newRotatedImage(c.imageId[0], Angle.ANG_180.degrees);
            c.imageId[Angle.ANG_270.ordinal()] = g.newRotatedImage(c.imageId[0], Angle.ANG_270.degrees);
        }
        Arrays.sort(cardImages);
        tableImageId = g.loadImage("table.png");
        cardDownImage[0] = cardDownImage[2] = g.loadImage("b1fv.png");
        cardDownImage[1] = cardDownImage[3] = g.loadImage("b1fh.png");
        cardWidth = g.getImage(cardImages[0].imageId[0]).getWidth();
        cardHeight = g.getImage(cardImages[0].imageId[0]).getHeight();
        this.initTurnOverCardAnimations(g);
    }

    void message(String msg) {
        System.out.println(msg);
    }
    
    private int getCardWidth(Angle ang) {
        switch (ang) {
            case ANG_0:
            case ANG_180:
                return cardWidth;
            case ANG_90:
            case ANG_270:
                return cardHeight;
        }
        throw new RuntimeException("Unhandled case");
    }

    private int getCardHeight(Angle ang) {
        switch (ang) {
            case ANG_0:
            case ANG_180:
                return cardHeight;
            case ANG_90:
            case ANG_270:
                return cardWidth;
        }
        throw new RuntimeException("Unhandled case");
    }

    @Override
    protected void drawFrame(AWTGraphics g) {
        try {
            clearScreen();
            g.drawImage(tableImageId, 0, 0, screenHeight-5, screenHeight-5);
            if (!game.isRunning()) {
                drawGameReady(g);
            } else {
                drawGame(g);
                drawPlayersStatus(g);
            }
            getMouseButtonClicked(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private <T> T incrementRule(T currentValue, T ... options) {
        int index = 0;
        for ( ; index < options.length; index++) {
            if (currentValue == options[index])
                break;
        }
        index = (index+1) % options.length;
        return options[index];
    }
    
    private void drawGameReady(AGraphics g) {
        List<Card> deck = game.getDeck();
        Card [] cards = deck.toArray(new Card[deck.size()]);
        int start = 0;
        int x = 10;
        int y = 10;
        int width = screenHeight - 25;
        int cardsPerRow = cards.length / 4;
        while (start < cards.length) {
            drawCards_old(g, cards, start, cardsPerRow, x, y, Angle.ANG_0, width);
            start += cardsPerRow;
            y += cardHeight+5;
        }
        
        final int buttonX = screenHeight + 10;
        int buttonY = 20;
        final int buttonDY = g.getTextHeight() + 10;
        final int buttonWidth = (screenWidth - 20) - buttonX; 

        if (game.canResume()) {
            if (drawPickButton(g, buttonX, buttonY, buttonWidth, "RESUME")) {
                try {
                    game.resume();
                } catch (Exception e) {
                    //SAVE_FILE.delete();
                    message("Failed to resume: " + e.getMessage());
                }
            }
        }
        buttonY += buttonDY;
        if (drawPickButton(g, buttonX, buttonY, buttonWidth, "NEW GAME")) {
            game.startNewGame();
        }

        buttonY += buttonDY;
        if (drawPickButton(g, buttonX, buttonY, buttonWidth, "JOIN MULTIPLAYER")) {
            try {
                IGolfGame multiplayerGame = new MultiPlayerGolfGame(this, "Chris");
                multiplayerGame.startNewGame();
                game = multiplayerGame;
            } catch (Exception e) {
                setMessage("Failed to connect to server");
            }
        }

        final int buttonWidth2 = buttonWidth / 3;
        final int buttonX2 = buttonX + buttonWidth - buttonWidth2;
        
        Rules rules = game.getRules();
        
        buttonY += buttonDY;
        g.setColor(g.WHITE);
        buttonY += buttonDY;
        g.setColor(g.WHITE);
        g.drawJustifiedString(buttonX, buttonY+5, Justify.LEFT, Justify.TOP, "Game Type");
        if (drawPickButton(g, buttonX2, buttonY, buttonWidth2, rules.getGameType().name())) {
            rules.setGameType(incrementRule(rules.getGameType(), GameType.values()));
            game.updateRules();
        }
        buttonY += buttonDY;
        g.setColor(g.WHITE);
        g.drawJustifiedString(buttonX, buttonY+5, Justify.LEFT, Justify.TOP, "Num Holes");
        if (drawPickButton(g, buttonX2, buttonY, buttonWidth2, " " + rules.getNumHoles() + " ")) {
            rules.setNumHoles(incrementRule(rules.getNumHoles(), 18, 9));
            game.updateRules();
        }
        if (rules.getGameType() != GameType.NineCard && rules.getGameType() != GameType.FourCard) {
            buttonY += buttonDY;
            g.setColor(g.WHITE);
            g.drawJustifiedString(buttonX, buttonY+5, Justify.LEFT, Justify.TOP, "Num Decks");
            if (drawPickButton(g, buttonX2, buttonY, buttonWidth2, " " + rules.getNumDecks() + " ")) {
                rules.setNumDecks(incrementRule(rules.getNumDecks(), 1, 2));
                game.updateRules();
            }
        }
        buttonY += buttonDY;
        g.setColor(g.WHITE);
        g.drawJustifiedString(buttonX, buttonY+5, Justify.LEFT, Justify.TOP, "Wild Card");
        if (drawPickButton(g, buttonX2, buttonY, buttonWidth2, rules.getWildcard().name())) {
            rules.setWildcard(incrementRule(rules.getWildcard(), WildCard.values()));
            game.updateRules();
        }
        if (rules.getGameType() != GameType.NineCard) {
            buttonY += buttonDY;
            g.setColor(g.WHITE);
            g.drawJustifiedString(buttonX, buttonY+5, Justify.LEFT, Justify.TOP, "Num Jokers");
            if (drawPickButton(g, buttonX2, buttonY, buttonWidth2, " " + rules.getNumJokers() + " ")) {
                rules.setNumJokers(incrementRule(rules.getNumJokers(), 0, 2, 4, 8));
                game.updateRules();
            }
            buttonY += buttonDY;
            g.setColor(g.WHITE);
            g.drawJustifiedString(buttonX, buttonY+5, Justify.LEFT, Justify.TOP, "Jokers Paired");
            if (drawPickButton(g, buttonX2, buttonY, buttonWidth2, " " + rules.getJokerValuePaired()+ " ")) {
                rules.setJokerValuePaired(incrementRule(rules.getJokerValuePaired(), -5, -4));
                game.updateRules();
            }
            buttonY += buttonDY;
            g.setColor(g.WHITE);
            g.drawJustifiedString(buttonX, buttonY+5, Justify.LEFT, Justify.TOP, "Jokers Unpaired");
            if (drawPickButton(g, buttonX2, buttonY, buttonWidth2, " " + rules.getJokerValueUnpaired() + " ")) {
                rules.setJokerValueUnpaired(incrementRule(rules.getJokerValueUnpaired(), 15, 0));
                game.updateRules();
            }
        }
        buttonY += buttonDY;
        g.setColor(g.WHITE);
        g.drawJustifiedString(buttonX, buttonY+5, Justify.LEFT, Justify.TOP, "Knocker penalty");
        if (drawPickButton(g, buttonX2, buttonY, buttonWidth2, rules.getKnockerPenaltyType().name())) {
            rules.setKnockerPenaltyType(incrementRule(rules.getKnockerPenaltyType(), KnockerPenaltyType.values()));
            game.updateRules();
        }
        buttonY += buttonDY;
        g.setColor(g.WHITE);
        g.drawJustifiedString(buttonX, buttonY+5, Justify.LEFT, Justify.TOP, "Knocker bonus");
        if (drawPickButton(g, buttonX2, buttonY, buttonWidth2, rules.getKnockerBonusType().name())) {
            rules.setKnockerBonusType(incrementRule(rules.getKnockerBonusType(), KnockerBonusType.values()));
            game.updateRules();
        }
        buttonY += buttonDY;
        g.setColor(g.WHITE);
        g.drawJustifiedString(buttonX, buttonY+5, Justify.LEFT, Justify.TOP, "Four of a Kind Bonus");
        if (drawPickButton(g, buttonX2, buttonY, buttonWidth2, " " + rules.getFourOfAKindBonus() + " ")) {
            rules.setFourOfAKindBonus(incrementRule(rules.getFourOfAKindBonus(), -50, -40, -30, -20, -10, 0));
            game.updateRules();
        }
    }
    
    private int drawMessage(AGraphics g, int x, int y, int width, String msg) {
        final int ty = g.getTextHeight();
        y += ty;
        String [] lines = g.generateWrappedLines(msg, width);
        g.setColor(g.GRAY);
        for (String l : lines) {
            y += ty;
            g.drawJustifiedString(x, y, Justify.LEFT, Justify.TOP, l);
        }
        return y;
    }
    
    private void drawPlayersStatus(AGraphics g) {
        g.setColor(g.WHITE);
        final int statusWidth = screenWidth - screenHeight - 20;
        final int lx = screenHeight + 10;
        final int rx = screenWidth -  20;
        final int cx = (lx+rx) / 2;
        int y = 10;
        final int ty = g.getTextHeight();
        final int by = ty+10;//3;
        
        g.drawJustifiedString(cx, y, Justify.CENTER, Justify.TOP, "ROUND " + (game.getNumRounds()));
        y += ty;
        
        for (int i=0; i<game.getNumPlayers(); i++) {
            y += ty;
            String lText = game.getPlayerName(i) + (game.getDealer() == i ? "(Dealer)" : "");
            g.drawJustifiedString(lx, y, Justify.LEFT, Justify.TOP, lText);
            String rText = "" + game.getPlayerPoints(i) + " Points";
            g.drawJustifiedString(rx, y, Justify.RIGHT, Justify.TOP, rText);
        }
        
        y += by;
        if (drawPickButton(g, cx-statusWidth/2, y, statusWidth, "QUIT")) {
            game.quit();
            newSinglePlayerGame();
            synchronized (this) {
                notify();
            }
            return;
        }
        y += by;

        String playerName = game.getPlayerName(game.getCurrentPlayer());
        g.setColor(g.WHITE);
        switch (game.getState()) {
        
            case INIT:
            case SHUFFLE:
                break;
            case DEAL:
                break;
            case TURN_OVER_CARDS: {
                y = drawMessage(g, lx, y, statusWidth, "Waiting for " + playerName + " to turn over a card");
                break;
            }
            case TURN: {
                y = drawMessage(g, lx, y, statusWidth, "Waiting for " + playerName + " to choose from stack or discard pile");
                break;
            }
            case PLAY: {
                y = drawMessage(g, lx, y, statusWidth, "Waiting for " + playerName + " to choose card from their hand to swap");
                break;
            }
            case DISCARD_OR_PLAY: {
                y = drawMessage(g, lx, y, statusWidth, "Waiting for " + playerName + " to discard or choose a card from their hand to swap");
                break;
            }
            case SETUP_DISCARD_PILE:
                break;
            case GAME_OVER: {
                //y += by;
                g.drawJustifiedString(cx, y, Justify.CENTER, Justify.TOP, "G A M E   O V E R");
                y += ty;
                //SwingPlayer winner = (SwingPlayer)golf.getPlayer(golf.getWinner());
                int winner = game.getWinner();
                g.drawJustifiedString(cx, y, Justify.CENTER, Justify.TOP, game.getPlayerName(winner) + " Wins!");
                y += by;
                if (this.drawPickButton(g, cx-statusWidth/2, y, statusWidth, "Play Agian?")) {
                    synchronized (game) {
                        game.notify();
                    }
                }
                break;
            }
        
            case PROCESS_ROUND:
                break;
            case END_ROUND:
                y += by;
                if (this.drawPickButton(g, cx-statusWidth/2, y, statusWidth, "CONTINUE")) {
                    synchronized (game) {
                        game.notify();
                    }
                }
                break;
        }

        if (message != null) {
            y = drawMessage(g, lx, y, statusWidth, message);
        }

    }
    
    static class CardLayout {
        final int [][] x;
        final int [][] y;
        final int width;
        final int height;
        final Angle angle;
        public CardLayout(Angle angle, int rows, int cols, int width, int height) {
            super();
            this.angle = angle;
            this.x = new int[rows][cols];
            this.y = new int[rows][cols];
            switch (angle) {
                case ANG_0: case ANG_180: this.width=width; this.height=height; break;
                default: this.width = height; this.height = width; break;
            }
        }        
    }
    
    CardLayout [] cardsLayout = new CardLayout[4];
    
    private void drawGame(AGraphics g) {
        final int padding = 3;
        final int z0 = cardHeight*2 + padding*3;
        final int z1 = screenHeight - cardHeight*2 - padding*3;
        //final int txtHeight = g.getTextHeight();
        
        int [] xTop = { z0+padding, z0/2 + padding, z0+padding, z1+padding};
        int [] yTop = { z1+padding, z0+padding, z0/2+padding, z0+padding };
        int [] xBottom = { xTop[0], padding, xTop[2], z1+cardHeight+2*padding };
        int [] yBottom = { z1+cardHeight+2*padding, yTop[3], padding, yTop[1] };

        // draw piles
        final int cz = screenHeight/2+padding;

        int xTxt[] = {
              cz, z0/2, cz, z1 + z0/2
        };
        
        int [] yTxt = {
                yTop[0]-padding*2, yTop[3]-padding, yBottom[3]+padding, yTop[1]-padding
        };
        
        Justify [] vTxt = { Justify.BOTTOM, Justify.BOTTOM, Justify.TOP, Justify.BOTTOM };
        
        final int numRows = game.getRules().getGameType().getRows();
        assert(numRows > 1);
        final int numCols = game.getRules().getGameType().getCols();
        highlightedCard = -1;
        for (int i=0; i<game.getNumPlayers(); i++) {
            int playerIndex = (game.getFrontPlayer() + i) % game.getNumPlayers();  
            cardsLayout[i] = new CardLayout(Angle.values()[i], numRows, numCols, z1-z0, getCardHeight(Angle.ANG_0));
            CardLayout layout = cardsLayout[i];
            //SwingPlayer p = (SwingPlayer)golf.getPlayer(i);
            for (int row=0; row<numRows; row++) {
                float xscale = (float)(xBottom[i] - xTop[i]) / (numRows - 1);
                float yscale = (float)(yBottom[i] - yTop[i]) / (numRows - 1);
                int x = xTop[i] + Math.round(xscale * row);
                int y = yTop[i] + Math.round(yscale * row);
//                drawCards(g, p.getRow(row).toArray(new Card[numInRow]), 0, numInRow, x, y, Angle.values()[i], z1 - z0);
                layoutCards(layout.x[row], layout.y[row], numCols, x, y, Angle.values()[i], z1 - z0);
            }
            drawCards(g, layout, game.getPlayerCards(playerIndex));
            String text = game.getPlayerName(playerIndex) + " Showing:" + game.getHandPoints(playerIndex);
            if (playerIndex == game.getKnocker())
                text += " KNOCKED";
            g.setColor(g.WHITE);
            g.drawJustifiedString(xTxt[i], yTxt[i], Justify.CENTER, vTxt[i], text);
        }
        
        
        Card [] piles = { 
                game.getTopOfDeck(),
                game.getTopOfDiscardPile(),
        };
        Angle stackAngle = cardsLayout[game.getFrontPlayer()].angle;
        stackLayout = new CardLayout(stackAngle, 1, 2, getCardWidth(Angle.ANG_0)*2 + padding, getCardHeight(Angle.ANG_0));
        layoutCards(stackLayout.x[0], stackLayout.y[0], 2, cz-cardWidth, cz-cardHeight/2, stackAngle, cardWidth*2 + padding);
        drawCards(g, stackLayout, new Card[][] { piles } );
        
        processAnimations(g);
        
        if (AGraphics.DEBUG_ENABLED) {
            g.setColor(g.WHITE);
            g.drawCircle(getMouseX(), getMouseY(), 5);
        }
    }
    
    CardLayout stackLayout;
    
    @Override
    protected void onDimensionsChanged(AGraphics g, int width, int height) {
        Utils.println("Dimensions changed to " + width + " X " + height);
        this.screenWidth = width;
        this.screenHeight = height;
        g.ortho(0, screenWidth, 0, screenHeight);
        if (this.tableImageId < 0) {
            loadImages(g);
        }
        //float aspectRatio = (float)cardWidth/cardHeight;
        float aspectRatio = (float)cardHeight/cardWidth;
        cardWidth = (screenHeight/3) / 3 - 3;
        cardHeight= Math.round((float)cardWidth*aspectRatio);
    }

    private int getPickableIndex(Card card) {
        return pickableCards.indexOf(card);
    }
    
    // complex function  
    // draw an array of cards spaced and centered within the specified rectangle 
    // with a specified orientation.
    // handles picking of cards as well.
    private void layoutCards(int [] outX, int [] outY, int numCards, int x, int y, Angle angle, int maxLenPixels) {
        
        int dx = 0;
        int dy = 0;
        int len = numCards;
        final int maxDx = cardWidth + cardWidth / 6; 
        if (len > 1) {
            switch (angle) {
                case ANG_0: 
                case ANG_180:
                    dx = (maxLenPixels - cardWidth)/(len-1);
                    if (dx > maxDx) {
                        dx = maxDx;
                        final int spacing = dx - cardWidth;
                        x = x+maxLenPixels/2 - (cardWidth*len/2) - (spacing*(len-1)/2);
                    }
                    break;
                case ANG_90:  
                case ANG_270:
                    dy = (maxLenPixels - cardWidth)/(len-1);
                    if (dy > maxDx) {
                        dy = maxDx;
                        final int spacing = dy-cardWidth;
                        y = y+maxLenPixels/2 - (cardWidth*len/2) - (spacing*(len-1)/2);
                    }
                    break;
            }
        }            

        //System.out.println("card picked1 = " + cardPicked);
        for (int i=0; i<len; i++) {
            outX[i] = x;
            outY[i] = y;
            x += dx;
            y += dy;
        }
    }   
    
    private void drawCards(AGraphics g, CardLayout layout, Card [][] cards) {
        
        if (AGraphics.DEBUG_ENABLED) {
            g.setColor(g.RED);
            for (int i=0; i<layout.x.length; i++)
                g.drawRect(layout.x[i][0], layout.y[i][0], layout.width, layout.height);
        }
        
        
        Card highlighted = null;
        final int cardWidth = getCardWidth(layout.angle);
        final int cardHeight = getCardHeight(layout.angle);
        
        for (int r=0; r<cards.length; r++) {
            for (int c=0; c<cards[r].length; c++) {
                Card card = cards[r][c];
                if (getPickableIndex(card) >= 0 && Utils.isPointInsideRect(getMouseX(), getMouseY(), layout.x[r][c], layout.y[r][c], cardWidth, cardHeight)) {
                    highlighted = card;
                }
            }
        }
        
        // now draw the cards
        for (int r=0; r<cards.length; r++) {
            for (int c=0; c<cards[r].length; c++) {
                Card card = cards[r][c];
                int dy = 0;
                //if (highlighted != null && card.equals(highlighted))
                //    dy = 20;
                this.drawCard(g, card, layout.x[r][c], layout.y[r][c]-dy, layout.angle);
                if (highlighted != null && card.equals(highlighted)) {
                    g.setColor(g.RED);
                    g.drawRect(layout.x[r][c]-2, layout.y[r][c]-2, cardWidth+4, cardHeight+4, 3);
                }
            }
        }
        
        synchronized (this) {
            if (highlighted != null && this.getMouseButtonPressed(0)) {
                this.pickedCard = this.getPickableIndex(highlighted);
                System.out.println("Picked the " + pickableCards.get(pickedCard));
                notify();
            }
        }
    }

    
    // complex function  
    // draw an array of cards spaced and centered within the specified rectangle 
    // with a specified orientation.
    // handles picking of cards as well.
    private void drawCards_old(AGraphics g, Card [] cards, int offset, int len, int x, int y, Angle angle, int maxLenPixels) {
        if (AGraphics.DEBUG_ENABLED) {
            g.setColor(g.RED);
            if (angle == Angle.ANG_0 || angle == Angle.ANG_180)
                g.drawRect(x, y, maxLenPixels, cardHeight);
            else
                g.drawRect(x, y, cardHeight, maxLenPixels);
        }
        synchronized (this) {
            if (len <= 0)
                return;
            if (offset + len >= cards.length)
                len = cards.length - offset;
            int dx = 0;
            int dy = 0;
            final int maxDx = cardWidth + cardWidth / 6; 
            if (len > 1) {
                switch (angle) {
                    case ANG_0: 
                    case ANG_180:
                        dx = (maxLenPixels - cardWidth)/(len-1);
                        if (dx > maxDx) {
                            dx = maxDx;
                            final int spacing = dx - cardWidth;
                            x = x+maxLenPixels/2 - (cardWidth*len/2) - (spacing*(len-1)/2);
                        }
                        break;
                    case ANG_90:  
                    case ANG_270:
                        dy = (maxLenPixels - cardWidth)/(len-1);
                        if (dy > maxDx) {
                            dy = maxDx;
                            final int spacing = dy-cardWidth;
                            y = y+maxLenPixels/2 - (cardWidth*len/2) - (spacing*(len-1)/2);
                        }
                        break;
                }
            }            

            // search backwards to see if a card is picked
            int picked = -1;
            int sx = x + dx*(len-1);
            int sy = y + dy*(len-1);
            if (highlightedCard < 0) {
                for (int i=len-1; i>=0; i--) {
                    picked = getPickableIndex(cards[i]);
                    if (picked >= 0 && Utils.isPointInsideRect(getMouseX(), getMouseY(), sx, sy, cardWidth, cardHeight)) {
                        //picked = i;
                        //System.out.println("picked = " + picked);
                        break;
                    }
                    picked = -1;
                    sx -= dx;
                    sy -= dy;
                }
            }
            
            //System.out.println("card picked1 = " + cardPicked);
            for (int i=0; i<len; i++) {
                sy = y;
                if (highlightedCard < 0 && picked >= 0 && cards[i].equals(pickableCards.get(picked))) {
                    highlightedCard = picked;
                    sy -= 20;
                }
                drawCard(g, cards[i+offset], x, sy, angle);
                x += dx;
                y += dy;
            }
            if (this.getMouseButtonPressed(0)) {
                if (picked >= 0) {
                    this.pickedCard = picked;
                    System.out.println("Picked the " + pickableCards.get(pickedCard));
                }
                notify();
            }
        }
    }   

    private void drawCard(AGraphics g, Card card, int x, int y, Angle angle) {
        if (card == null)
            return;
        int cw = getCardWidth(angle);
        int ch = getCardHeight(angle);

        g.drawImage(getCardImage(card, angle), x, y, cw, ch);
        if (card.isShowing() && game.getRules().getWildcard().isWild(card)) {
            g.setColor(g.makeColor(0,0,0,64));
            g.drawFilledRect(x, y, cw, ch);
            int cx = x + cw/2;
            int cy = y + ch/2;
            g.setColor(g.BLACK);
            g.drawJustifiedString(cx+2, cy+2, Justify.CENTER, Justify.CENTER, "WILD");
            g.setColor(g.YELLOW);            
            g.drawJustifiedString(cx, cy, Justify.CENTER, Justify.CENTER, "WILD");
        }
    }
    
    private int getCardImage(Card card, Angle angle) {
        if (!card.isShowing())
            return cardDownImage[angle.ordinal()];
        return getCardImage(card.getRank(), card.getSuit(), angle);
    }
    
    private int getCardImage(Rank rank, Suit suit, Angle angle) {
        if (rank == null || suit == null)
            return cardDownImage[angle.ordinal()];
        int key = rank.ordinal() << 16 | suit.ordinal();
        int index = Arrays.binarySearch(cardImages, key);
        CardImage card = cardImages[index];
        if (card.rank != rank || card.suit != suit)
            throw new RuntimeException("No image for card " + rank + " " + suit);
        return card.imageId[angle.ordinal()];//index;
//        for (CardImage c : cardImages) {
//            if (c.rank == rank && c.suit == suit)
//                return c.imageId[angle.ordinal()];
//        }
//        throw new RuntimeException("No image for card " + rank + " " + suit);
    }
    
    private boolean drawPickButton(AGraphics g, int x, int y, int minWidth, String text) {
        final int padding = 2;
        int wid = Math.round(g.getTextWidth(text));
        if (wid < minWidth)
            wid = minWidth;
        int hgt = g.getTextHeight();
        AColor fontColor = g.WHITE;
        AColor rectColor = g.CYAN;
        boolean highlighted = false;
        if (Utils.isPointInsideRect(getMouseX(), getMouseY(), x, y, wid+padding*2, hgt+padding*2)) {
            rectColor = g.YELLOW;
            highlighted = true;
        }
        g.setColor(fontColor);
        g.drawJustifiedString(x+padding, y+padding, Justify.LEFT, Justify.TOP, text);
        g.setColor(rectColor);
        g.drawRect(x, y, wid+padding*2, hgt+padding*2, 1);
        
        boolean pressed = this.getMouseButtonPressed(0);
        if (pressed) {
            System.out.println("PRESSED");
        }
        
        return highlighted && pressed;
    }

    @Override
    protected Font getDefaultFont() {
        return new Font("Arial", Font.BOLD, 12);
    }

    void processAnimations(AGraphics g) {
        if (animations.size() <= 0)
            return;
        AAnimation [] anims;
        synchronized (animations) {
            anims = animations.toArray(new AAnimation[animations.size()]);
        }
        
        for (AAnimation a : anims) {
            a.update(g);
            if (a.isDone()) {
                synchronized (animations) {
                    animations.remove(a);
                }
            }
        }
    }

    class MoveCardAnimation extends AAnimation {

        final float sx, sy, ex, ey;
        final Angle angle;
        final Card card;
        
        public MoveCardAnimation(Card card, float sx, float sy, float ex, float ey, Angle angle, long duration) {
            super(duration, 0);
            this.card = card;
            this.sx = sx;
            this.sy = sy;
            this.ex = ex;
            this.ey = ey;
            this.angle = angle;
            start();
        }

        @Override
        public void draw(AGraphics g, float position, float dt) {
            float x = sx + (ex-sx) * position;
            float y = sy + (ey-sy) * position;
            float cw = cardWidth;
            float ch = cardHeight;
            int xi = Math.round(x - cw/2);
            int yi = Math.round(y - ch/2);
            switch (angle) {
                case ANG_90:
                case ANG_270:
                    cw = cardHeight;
                    ch = cardWidth;
                    break;
                case ANG_0:
                case ANG_180:
                    break;
            }
            drawCard(g, card, xi, yi, angle);
        }
        
    }
    
    int [][] turnOverCardImages = new int[4][10];
    void initTurnOverCardAnimations(AGraphics g) {
        for (int i=0; i<10; i++) {
            int id = g.loadImage("turnoverblue" + i + ".png");
            int idRot = g.newRotatedImage(id, 90);
            turnOverCardImages[Angle.ANG_0.ordinal()][i] = turnOverCardImages[Angle.ANG_180.ordinal()][i] = id;
            turnOverCardImages[Angle.ANG_90.ordinal()][i] = turnOverCardImages[Angle.ANG_270.ordinal()][i] = idRot;
        }
    }
    
    class TurnOverCardAnimation extends AAnimation {
        final int x, y, w, h;
        final Card card;
        final Angle angle;
        final int [] images;
        
        TurnOverCardAnimation(long duration, int x, int y, Card card, Angle angle) {
            super (duration, 0);
            this.x = x;
            this.y = y;
            this.card = card;
            this.angle = angle;
            w = getCardWidth(angle);
            h = getCardHeight(angle);
            images = turnOverCardImages[angle.ordinal()];
            start();
        }

        @Override
        public void draw(AGraphics g, float position, float dt) {
            int index = Math.round(position * images.length);
            //System.out.println("index=" + index);
            if (index < 0)
                index = 0;
            if (index >= images.length)
                index = images.length-1;
            g.drawImage(images[index], x, y, w, h);
        }
    }

    void startSwapCardAnimation(int player, DrawType type, Card drawn, int row, int col) {
        CardLayout layout = cardsLayout[(player-game.getFrontPlayer()+game.getNumPlayers()) % game.getNumPlayers()];
        AAnimation a;
        if (type == DrawType.DTStack) {
            a = newMoveCardAnimation(drawn, stackLayout, 0, 0, layout, row, col, 1000);
        }
        else {
            a = newMoveCardAnimation(drawn, stackLayout, 0, 1, layout, row, col, 1000);
        }
        AAnimation b = newMoveCardAnimation(game.getPlayerCard(player, row, col), layout, row, col, stackLayout, 0, 1, 1000);
        synchronized (animations) {
            animations.add(a);
            animations.add(b);
            try {
                repaint();
                animations.wait(1000);
            } catch (Exception e) {}
        }
    }
    
    void startDiscardDrawnCardAnimation(Card card) {
        synchronized (animations) {
            animations.add(newMoveCardAnimation(card, stackLayout, 0, 0, stackLayout, 0, 1, 1000));
            try {
                animations.wait(1000);
            } catch (Exception e) {}
        }
    }
    
    private AAnimation newMoveCardAnimation(Card c, CardLayout src, int srcRow, int srcCol, CardLayout dst, int dstRow, int dstCol, long time) {
        int sx = src.x[srcRow][srcCol] + getCardWidth(src.angle)/2;
        int sy = src.y[srcRow][srcCol] + getCardHeight(src.angle)/2;
        int ex = dst.x[dstRow][dstCol] + getCardWidth(dst.angle)/2;
        int ey = dst.y[dstRow][dstCol] + getCardHeight(dst.angle)/2;
        return new MoveCardAnimation(c, sx, sy, ex, ey, src.angle, time);
    }
    
    // called from seperated thread
    void startDealCardAnimation(int player, Card c, int row, int col) {
        CardLayout layout = cardsLayout[(player-game.getFrontPlayer()+game.getNumPlayers()) % game.getNumPlayers()];
        synchronized (animations) {
            animations.add(newMoveCardAnimation(c, stackLayout, 0, 0, layout, row, col, 500));
            try {
                repaint();
                animations.wait(200);
            } catch (Exception e) {}
        }
    }
    
    final int TIME_TO_TURN_OVER_CARD = 1500;
    
    void startTurnOverCardAnimationStack() {
        synchronized (animations) {
            animations.add(new TurnOverCardAnimation(TIME_TO_TURN_OVER_CARD, stackLayout.x[0][0], stackLayout.y[0][0], game.getTopOfDeck(), stackLayout.angle));
            try {
                repaint();
                animations.wait(TIME_TO_TURN_OVER_CARD);
            } catch (Exception e) {}
        }
    }
    
    void startTurnOverCardAnimation(int player, Card card, int row, int col) {
        CardLayout layout = cardsLayout[(player-game.getFrontPlayer()+game.getNumPlayers()) % game.getNumPlayers()];
        synchronized (animations) {
            animations.add(new TurnOverCardAnimation(TIME_TO_TURN_OVER_CARD, layout.x[row][col], layout.y[row][col], card, layout.angle));
            try {
                repaint();
                animations.wait(TIME_TO_TURN_OVER_CARD);
            } catch (Exception e) {}
        }
    }

    public void setMessage(String msg) {
        this.message = msg;
    }
    
    public void newSinglePlayerGame() {
        game = new SinglePlayerGolfGame(this);
    }
}
