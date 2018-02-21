package cc.game.kaiser.swing;

import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cc.game.kaiser.ai.PlayerBot;
import cc.game.kaiser.core.*;
import cc.lib.game.GColor;
import cc.lib.game.AGraphics;
import cc.lib.game.AImage;
import cc.lib.game.Justify;
import cc.lib.game.Utils;
import cc.lib.swing.AWTGraphics;
import cc.lib.swing.EZFrame;
import cc.lib.swing.KeyboardAnimationApplet;
import cc.lib.utils.Reflector;

public class KaiserApplet extends KeyboardAnimationApplet {

    public static void main(String [] args) {
        //Utils.DEBUG_ENABLED = true;
        //Golf.DEBUG_ENABLED = true;
        //PlayerBot.DEBUG_ENABLED = true;
        EZFrame frame = new EZFrame("Kaiser");
        KeyboardAnimationApplet app = new KaiserApplet();
        frame.add(app);
        app.init();
        frame.centerToScreen(800, 600);
        app.start();
        app.setMillisecondsPerFrame(20);
    }   

    private int tableImageId = -1;
    private int [] cardDownImage = new int[4];
    private Kaiser kaiser;
    private boolean running = false;

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
    
    class CardImage {
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
      new CardImage(Rank.FIVE,  Suit.HEARTS,      "39.png"),      
      new CardImage(Rank.THREE, Suit.CLUBS,       "45.png"),      
    };
    
    private int cardWidth = 0;
    private int cardHeight = 0;
    
    final File SAVE_FILE = new File("../savegame.txt");
    
    @Override
    protected void doInitialization() {
        Kaiser.DEBUG_ENABLED = true;
        Utils.DEBUG_ENABLED = false;
        PlayerBot.ENABLE_AIDEBUG = true;
        Reflector.ENABLE_THROW_ON_UNKNOWN = true;
        kaiser = new Kaiser();
        try {
            kaiser.loadFromFile(SAVE_FILE);
            for (Player p : kaiser.getPlayers()) {
                if (p instanceof SwingPlayerUser) {
                    ((SwingPlayerUser)p).applet = this;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            kaiser.newGame();
            kaiser.setPlayer(0, new SwingPlayerUser("Chris", this));
            kaiser.setPlayer(1, new SwingPlayer("AI 1"));
            kaiser.setPlayer(2, new SwingPlayer("AI 2"));
            kaiser.setPlayer(3, new SwingPlayer("AI 3"));
        }
        
    }
    
    void loadImages(AGraphics g) {
        if (this.tableImageId >= 0) {
            return;
        }
            
        for (CardImage c: cardImages) {
            
            c.imageId[Angle.ANG_0.ordinal()] = g.loadImage(c.fileName);
            c.imageId[Angle.ANG_90.ordinal()] = g.newRotatedImage(c.imageId[0], Angle.ANG_90.degrees);
            c.imageId[Angle.ANG_180.ordinal()] = g.newRotatedImage(c.imageId[0], Angle.ANG_180.degrees);
            c.imageId[Angle.ANG_270.ordinal()] = g.newRotatedImage(c.imageId[0], Angle.ANG_270.degrees);
        }
        tableImageId = g.loadImage("table.png");
        cardDownImage[0] = cardDownImage[2] = g.loadImage("b1fv.png");
        cardDownImage[1] = cardDownImage[3] = g.loadImage("b1fh.png");
        AImage image = g.getImage(cardImages[0].imageId[0]);
        cardWidth = image.getWidth();
        cardHeight = image.getHeight();        
    }

    synchronized void startThread() {
        if (running)
            return;
        running = true;
        new Thread(new Runnable() {
            public void run() {
                try {
                    State prevState = State.GAME_OVER;
                    while (running) {
                        synchronized (kaiser) {
                            kaiser.runGame();
                            if (prevState != kaiser.getState()) {
                                System.out.println("went from state: " + prevState + " -> " + kaiser.getState());
                                prevState = kaiser.getState();
                                kaiser.saveToFile(SAVE_FILE);
                            }
                            switch (kaiser.getState()) {
                                /** initial state */
                                case NEW_GAME:               
                                /** reset hands, bids increment start player */
                                case NEW_ROUND:
                                    break;
                                /** deal the cards */
                                case DEAL:
                                    kaiser.wait(100);
                                    break;
                                /** take bids */
                                case BID:                    
                                /** each player play's their card */
                                    kaiser.wait(1000);
                                    break;
                                case TRICK:
                                    kaiser.wait(1000);
                                    break;
                                /** determine trick winner */
                                case PROCESS_TRICK:
                                    kaiser.wait(3000);
                                    break;
                                /** reset before back to TRICK */
                                case RESET_TRICK:      
                                    break;
                                /** all cards have been played, check for kaiser, assign points */
                                case PROCESS_ROUND:        
                                    //kaiser.wait(30000);
                                    break;
                                /** one of the teams has won */
                                case GAME_OVER:
                                    break;
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                running = false;
            }
        }).start();
    }
    
    @Override
    protected void drawFrame(AGraphics g) {
        this.clearScreen();
        g.drawImage(tableImageId, 0, 0, screenHeight-5, screenHeight-5);
        if (!running) {
            drawGameReady(g);
            if (this.getMouseButtonClicked(0)) {
                startThread();
            }
        } else {
            switch (kaiser.getState()) {
                /** initial state */
                case NEW_GAME:               
                /** reset hands, bids increment start player */
                case NEW_ROUND:              
                /** deal the cards */
                case DEAL:
                /** take bids */
                case BID: 
                /** each player play's their card */
                case TRICK:                  
                /** determine trick winner */
                case PROCESS_TRICK:          
                /** reset before back to TRICK */
                case RESET_TRICK:            
                    drawGame(g);
                    break;
                /** all cards have been played, check for kaiser, assign points */
                case PROCESS_ROUND:        
                    drawTricksTaken(g);
                    break;
                /** one of the teams has won */
                case GAME_OVER:
                    if (getMouseButtonClicked(0)) {
                        kaiser.newGame();
                    }
                    break;

            }
        }
    }
    
    private SwingPlayer getPlayer(int index) {
        return (SwingPlayer)kaiser.getPlayer(index);
    }
    
    private void drawTricksTaken(AGraphics g) {
        int x = 15;
        int y = 15;
        for (int i=0; i<kaiser.getNumPlayers(); i++) {
            Player p = kaiser.getPlayer(i);
            g.drawJustifiedString(x, y, Justify.LEFT, Justify.TOP, p.getName() + " " + p.getNumTricks() + " tricks");
            y += g.getTextHeight();
            if (p.getNumTricks() == 0)
                continue;
            Hand []tricks = p.getTricks();
            final int sx = x;
            for (int ii=0; ii<p.getNumTricks(); ii++) {
                Card [] cards = tricks[ii].getCards();
                final int maxLenPixels = cardWidth*2;
                this.drawCards(g, cards, 0, cards.length, x, y, Angle.ANG_0, maxLenPixels, true);
                x += maxLenPixels+5;
                if (x + maxLenPixels > screenHeight) {
                    x = sx;
                    y += cardHeight + 2;
                }
            }
            x = sx;
            y += cardHeight + 5;
        }
    }
    
    private void drawGame(AGraphics g) {
        synchronized (this) {
            final int padding = 5;
            final int sh = screenHeight;
            final int fh = g.getTextHeight() + 3;
            
            int [] x = { 
                    padding*2+cardHeight, 
                    sh-cardHeight-padding, 
                    sh-cardHeight*2-padding, 
                    padding 
            };
            int [] y = { 
                    sh-cardHeight-padding*2, 
                    padding+cardHeight, 
                    padding, 
                    sh-cardHeight*2-padding 
            };
            // draw the trick cards that have been played
            final int c = sh/2; // x/y center of board
            final int d = cardWidth/2;
    
            final int [] tdx = { -d, d, -d, -(d+cardHeight) };
            int [] tdy = { d, -d, -(d+cardHeight), -d };
    
            final int maxCardsWidth = screenHeight/2;
            
            final int txtPadding = 15;
            final int [] nx = { screenHeight-txtPadding, screenHeight-txtPadding, txtPadding, txtPadding };
            final int [] ny = { y[0], txtPadding, txtPadding, y[0] };
            final Justify [] nhj = { Justify.RIGHT, Justify.RIGHT, Justify.LEFT, Justify.LEFT };
            final Justify [] nvj = { Justify.TOP, Justify.TOP, Justify.TOP, Justify.TOP };
            
            for (int i=0; i<4; i++) {
                drawPlayerHand(g, getPlayer(i), x[i], y[i], Angle.values()[i], maxCardsWidth);
                String txt = getPlayer(i).getName();
                if (kaiser.getDealer() == i) {
                    txt += "\nDealer";
                }
                if (kaiser.getStartPlayer() == i) {
                    txt += "\nStart";
                }
                if (kaiser.getState() == State.PROCESS_TRICK && kaiser.getTrickWinnerIndex() == i) {
                    txt += "\nTrick Winner";
                }
                g.drawJustifiedString(nx[i], ny[i], nhj[i], nvj[i], txt);
                Card trick = kaiser.getTrick(i);
                if (trick != null) {
                    this.drawCard(g, trick, c+tdx[i], c+tdy[i], Angle.values()[i], true);
                }
            }
            
            Team t0 = kaiser.getTeam(0);
            Team t1 = kaiser.getTeam(1);
            
            String text = "Team " + t0.getName() + " " + t0.getBid() + "\n" +
                          kaiser.getPlayer(t0.getPlayerA()).getName() + " " + kaiser.getPlayer(t0.getPlayerB()).getName() + "\n" +
                          "\n" +
                          "Points " + t0.getTotalPoints() + "\n" +
                          "Round  " + t0.getRoundPoints() + "\n" +
                          "\n" +
                          "Team " + t1.getName() + " " + t1.getBid() + "\n" +
                          kaiser.getPlayer(t1.getPlayerA()).getName() + " " + kaiser.getPlayer(t1.getPlayerB()).getName() + "\n" +
                          "\n" +
                          "Points " + t1.getTotalPoints() + "\n" +
                          "Round  " + t1.getRoundPoints() + "\n";
            
            g.setColor(GColor.WHITE);
            int maxWidth = screenWidth - screenHeight - 10;
            final int sx = screenHeight + 5;
            float sy = 10;
            sy = g.drawWrapString(sx, sy, maxWidth, text);
    
            final int dy = fh+2;
            sy += dy;
    
            if (bidOptions.size() > 0) {
                
                // draw the bid numbers
                int xx = sx;
                g.drawJustifiedString(sx, sy, Justify.LEFT, Justify.TOP, "Choose your bid");
                sy += dy;
    
                boolean [] used = new boolean[52];
                for (Bid bid: bidOptions) {
                    if (bid.numTricks <= 0)
                        continue;
                    if (used[bid.numTricks])
                        continue;
                    used[bid.numTricks] = true;
                    xx += this.drawPickBidNumButton(g, xx, sy, bid.numTricks);
                    if (xx >= this.screenWidth - 10) {
                        xx = sx;
                        sy += dy;
                    }
                }
                
                sy += dy;
                if (selectedBidNum > 0) {
                    for (Bid bid: bidOptions) {
                        if (bid.numTricks == selectedBidNum) {
                            if (drawPickBidSuitButton(g, sx, sy, bid.trump)) {
                                pickedBid = bid;
                            }
                            sy += dy;
                        }
                    }
                }
    
                sy += dy;
                if (this.selectedBidNum > 0 && this.selectedBidSuit != null) {
                    if (drawPickButton(g, sx, sy, "BID " + selectedBidNum + " " + selectedBidSuit)) {
                        for (Bid bid : bidOptions) {
                            if (bid.numTricks == selectedBidNum && bid.trump == selectedBidSuit) {
                                pickedBid = bid;
                            }
                        }
                        notify();
                    }
                    sy += dy;
                }
                
                if (this.drawPickButton(g, sx, sy, "NO BID")) {
                    this.pickedBid = Bid.NO_BID;
                    notify();
                }
            }
        }            
    }

    private boolean drawPickButton(AGraphics g, float x, float y, String text) {
        final int padding = 2;
        int wid = Math.round(g.getTextWidth(text));
        int hgt = g.getTextHeight();
        GColor fontColor = GColor.WHITE;
        GColor rectColor = GColor.CYAN;
        boolean highlighted = false;
        if (Utils.isPointInsideRect(getMouseX(), getMouseY(), x, y, wid+padding*2, hgt+padding*2)) {
            rectColor = GColor.YELLOW;
            highlighted = true;
        }
        g.setColor(fontColor);
        g.drawJustifiedString(x+padding, y+padding, Justify.LEFT, Justify.TOP, text);
        g.setColor(rectColor);
        g.drawRect(x, y, wid+padding*2, hgt+padding*2, 1);
        
        return highlighted && this.getMouseButtonClicked(0);
    }
    
    private int selectedBidNum = -1;
    private int drawPickBidNumButton(AGraphics g, float x, float y, int num) {
        String text = "" + num;
        final int padding = 2;
        int wid = Math.round(g.getTextWidth("00"));
        int hgt = g.getTextHeight();
        GColor fontColor = GColor.WHITE;
        GColor rectColor = GColor.CYAN;
        boolean highlighted = false;
        if (num == selectedBidNum) {
            rectColor = GColor.RED;
        }
        else if (Utils.isPointInsideRect(getMouseX(), getMouseY(), x, y, wid+padding*2, hgt+padding*2)) {
            highlighted = true;
            rectColor = GColor.YELLOW;
        }
        g.setColor(fontColor);
        g.drawJustifiedString(x+padding, y+padding, Justify.LEFT, Justify.TOP, text);
        g.setColor(rectColor);
        g.drawRect(x, y, wid+padding*2, hgt+padding*2, 1);
        
        if (highlighted && this.getMouseButtonClicked(0)) {
            selectedBidNum = num;
        }
        
        return wid + padding * 2 + 2;
    }

    private Suit selectedBidSuit = null;
    
    private boolean drawPickBidSuitButton(AGraphics g, float x, float y, Suit suit) {
        String text = suit.name();
        final int padding = 2;
        int wid = 0;
        for (Suit s: Suit.values()) {
            int w = Math.round(g.getTextWidth(s.name()));
            if (w > wid)
                wid = w;
        }
        //g.getTextWidth("WW");
        int hgt = g.getTextHeight();
        GColor fontColor = GColor.WHITE;
        GColor rectColor = GColor.CYAN;
        boolean highlighted = false;
        if (suit == selectedBidSuit) {
            rectColor = GColor.RED;
        }
        else if (Utils.isPointInsideRect(getMouseX(), getMouseY(), x, y, wid+padding*2, hgt+padding*2)) {
            highlighted = true;
            rectColor = GColor.YELLOW;
        }
        g.setColor(fontColor);
        g.drawJustifiedString(x+padding, y+padding, Justify.LEFT, Justify.TOP, text);
        g.setColor(rectColor);
        g.drawRect(x, y, wid+padding*2, hgt+padding*2, 1);
        
        if (highlighted && this.getMouseButtonClicked(0)) {
            selectedBidSuit = suit;
            return true;
        }
        
        return false;
    }

    @Override
    public void mouseClicked(MouseEvent evt) {
        switch (kaiser.getState()) {
            case NEW_GAME:
            case NEW_ROUND:
            //case DEAL:
            //case BID:
            //case TRICK:
            case PROCESS_TRICK:
            case RESET_TRICK:
            case PROCESS_ROUND:
            case GAME_OVER:
                synchronized (this) {
                    notifyAll();
                }
                break;
            default:
                break;
        }
        super.mouseClicked(evt);
    }

    private void drawPlayerHand(AGraphics g, SwingPlayer player, int x, int y, Angle angle, int maxLenPixels) {
        Card [] cards = player.getHand().getCards();
        this.drawCards(g, cards, 0, cards.length, x, y, angle, maxLenPixels, player.isCardsShowing());
    }
    
    private void drawGameReady(AGraphics g) {
        Card [] cards = kaiser.getAllCards();
        int x = 20;
        int y = 10;
        int width = screenWidth - 20;
        drawCards(g, cards, 0, 8, x, y, Angle.ANG_0, width, true);
        y += cardHeight + 5;
        drawCards(g, cards, 8, 8, x, y, Angle.ANG_0, width, true);
        y += cardHeight + 5;
        drawCards(g, cards, 16, 8, x, y, Angle.ANG_0, width, true);
        y += cardHeight + 5;
        drawCards(g, cards, 24, 8, x, y, Angle.ANG_0, width, true);
        g.setColor(GColor.BLUE);
        g.drawJustifiedString(screenWidth/2, screenHeight/2, Justify.CENTER, Justify.CENTER, 
                "Click Mouse button to start");
    }
    
    private List<Card> pickableCards = new ArrayList<Card>();
    private int pickedCard = -1; //this is the index in pickableCards
    
    private List<Bid> bidOptions = new ArrayList<Bid>();
    private Bid pickedBid = null;
    
    private int getPickableIndex(Card card) {
        return pickableCards.indexOf(card);
    }
    
    private void drawCards(AGraphics g, Card [] cards, int offset, int len, int x, int y, Angle angle, int maxLenPixels, boolean showing) {
        synchronized (this) {
            if (len <= 0)
                return;
            if (showing)
                pickedCard = -1;
            int dx = 0;
            int dy = 0;
            switch (angle) {
                case ANG_0: 
                    dx = (maxLenPixels - cardWidth)/len;
                    if (dx > cardWidth/4)
                        dx = cardWidth/4;
                    break;
                case ANG_180:
                    dx = (maxLenPixels - cardWidth)/len;
                    if (dx > cardWidth/4)
                        dx = cardWidth/4;
                    dx = -dx;
                    break;
                case ANG_90:  
                    dy = (maxLenPixels - cardWidth)/len;
                    if (dy > cardWidth/4)
                        dy = cardWidth/4;
                    break;
                case ANG_270:
                    dy = (maxLenPixels - cardWidth)/len;
                    if (dy > cardWidth/4)
                        dy = cardWidth/4;
                    dy = -dy;
                    break;
            }
            
            int picked = -1;
            if (showing) {
                // search backwards to see if a card is picked
                int sx = x + dx*(len-1);
                int sy = y + dy*(len-1);
                for (int i=len-1; i>=0; i--) {
                    pickedCard = getPickableIndex(cards[i]);
                    if (pickedCard >= 0 && Utils.isPointInsideRect(getMouseX(), getMouseY(), sx, sy, cardWidth, cardHeight)) {
                        picked = i;
                        break;
                    }
                    pickedCard = -1;
                    sx -= dx;
                    sy -= dy;
                }
            }
            
            //System.out.println("card picked1 = " + cardPicked);
            for (int i=0; i<len; i++) {
                int sy = y;
                if (i == picked)
                    sy -= 20;
                drawCard(g, cards[i+offset], x, sy, angle, showing);
                x += dx;
                y += dy;
            }
            if (pickedCard >= 0 && this.getMouseButtonClicked(0)) {
                notify();
            }
        }
        
    }
    
    private void drawCard(AGraphics g, Card card, int x, int y, Angle angle, boolean showing) {
        int cw = cardWidth;
        int ch = cardHeight;

        switch (angle) {
            case ANG_90: 
            case ANG_270: 
                cw = cardHeight;
                ch = cardWidth;
                break;
            default: break;
        }
        if (showing) {
            g.drawImage(getCardImage(card.rank, card.suit, angle), x, y, cw, ch);
        }
        else
            g.drawImage(this.cardDownImage[angle.ordinal()], x, y, cw, ch);
    }
    
    private int getCardImage(Rank rank, Suit suit, Angle angle) {
        for (CardImage c : cardImages) {
            if (c.rank == rank && c.suit == suit)
                return c.imageId[angle.ordinal()];
        }
        throw new RuntimeException("No image for card " + rank + " " + suit);
    }

    private int screenWidth, screenHeight;
    
    @Override
    protected void onDimensionsChanged(AGraphics g, int width, int height) {

        Utils.println("Dimensions changed to " + width + " X " + height);
        this.screenWidth = width;
        this.screenHeight = height;
        g.ortho();
        loadImages(g);
        //float aspectRatio = (float)cardWidth/cardHeight;
        float aspectRatio = (float)cardHeight/cardWidth;
        cardWidth = (screenHeight/3) / 3 - 3;
        cardHeight= Math.round((float)cardWidth*aspectRatio);        
    }

    public Card pickCard(Card[] options) {
        synchronized (this) {
            this.pickableCards.clear();
            this.pickableCards.addAll(Arrays.asList(options));
            try {
                wait();
            } catch (Exception e) {
                e.printStackTrace();
            }
            pickableCards.clear();
        }
        if (pickedCard >= 0) {
            return options[pickedCard];
        }
        return null;
    }

    public Bid pickBid(Bid[] options) {
        synchronized (this) {
            this.selectedBidNum = -1;
            this.selectedBidSuit = null;
            this.bidOptions.clear();
            this.bidOptions.addAll(Arrays.asList(options));
            try {
                wait();
            } catch (Exception e) {
                e.printStackTrace();
            }
            this.bidOptions.clear();
        }
        return pickedBid;
    }

    /*
    // called form run thread
    public Card chooseCard(Card[] options) {
        synchronized (this) {
            System.out.println("Card picked2 = " + cardPicked);
            Card card = null;
            if (pickableCards.size() == 0)
                pickableCards.addAll(Arrays.asList(options));
            if (getMouseButton(0, true) && cardPicked >= 0) {
                pickableCards.clear();
                card = options[cardPicked];
                cardPicked = -1;
            }
            return card;
        }
    }
    
    // called from run thread
    public Bid chooseBid(Bid [] options) {
        return options[0];
    }*/

}
