package cc.game.golf.swing;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import cc.game.golf.ai.PlayerBot;
import cc.game.golf.core.*;

public class SinglePlayerGolfGame extends Golf implements IGolfGame, Runnable {

    private final File SAVE_FILE = new File("savegame.txt");
    private final File RULES_FILE = new File("savedrules.txt");
    
    final GolfSwing g;
    boolean running = false;
    
    SinglePlayerGolfGame(GolfSwing g) {
        this.g = g;
        loadRules();
    }
    
    @Override
    public int getFrontPlayer() {
        if (getState() == State.DEAL)
            return getDealer();
        if (getPlayer(getCurrentPlayer()) instanceof SwingPlayerUser)
            return this.getCurrentPlayer();
        return 0;
    }

    @Override
    public Card[][] getPlayerCards(int player) {
        return getPlayer(player).getCards();
    }

    @Override
    public String getPlayerName(int player) {
        return getPlayer(player).getName();
    }

    @Override
    public int getHandPoints(int player) {
        return getPlayer(player).getHandPoints(this);
    }

    @Override
    public int getPlayerPoints(int player) {
        return getPlayer(player).getPoints();
    }

    @Override
    public Card getPlayerCard(int player, int row, int col) {
        return getPlayer(player).getCard(row, col);
    }

    @Override
    protected void message(String format, Object... params) {
        super.message(format, params);
        g.setMessage(String.format(format, params));
    }

    @Override
    protected void onKnock(int player) {
        // TODO Auto-generated method stub
        super.onKnock(player);
    }

    @Override
    protected void onCardSwapped(int player, DrawType dtstack, Card drawn, Card swapped, int row, int col) {
        g.startSwapCardAnimation(player, dtstack, drawn, row, col);                
    }

    @Override
    protected void onCardDiscarded(int player, DrawType dtstack, Card swapped) {
        g.startDiscardDrawnCardAnimation(swapped);
    }

    @Override
    protected void onDealCard(int player, Card card, int row, int col) {
        g.startDealCardAnimation(player, card, row, col);
    }

    @Override
    protected void onCardTurnedOver(int player, Card card, int row, int col) {
        g.startTurnOverCardAnimation(player, card, row, col);                    
    }

    @Override
    protected void onDrawPileChoosen(int player, DrawType type) {
        if (type == DrawType.DTStack) {
            g.startTurnOverCardAnimationStack();
        }
    }

    @Override
    protected void onChooseCardToSwap(int player, Card card, int row, int col) {
        if (!card.isShowing())
            g.startTurnOverCardAnimation(player, card, row, col);                    
    }

    public void run() {
        State prevState = State.INIT;
        try {
            while (running) {
                synchronized (this) {
                    if (prevState != getState()) {
                        prevState = getState();
                        saveGame(SAVE_FILE);
                    }
                    if (prevState != getState())
                        System.out.println("Processing state: " + getState());
                    switch (getState()) {
                        case DEAL:
                            //golf.wait(250);
                            break;
                        case TURN_OVER_CARDS:
                        case SETUP_DISCARD_PILE:
                        case PLAY:
                        case DISCARD_OR_PLAY:
                            break;
                        case END_ROUND:
                        case GAME_OVER:
                            wait(20000);
                            break;
                        case INIT:
                            break;
                        case PROCESS_ROUND:
                            break;
                        case SHUFFLE:
                            break;
                        case TURN:
                            break;
                        default:
                            break;
                    }
                    if (running)
                        runGame();
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        System.out.println("Thread exiting");
        clear();
        running = false;
    }
    
    private synchronized void startThread() {
        if (running)
            return;
        running = true;
        new Thread(this).start();
    }

    void loadRules() {
        if (RULES_FILE.exists()) {
            try {
                getRules().loadFromFile(RULES_FILE);
                clear();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    void saveRules() {
        PrintWriter printer = null;
        try {
            printer = new PrintWriter(new FileWriter(RULES_FILE));
            //rules.save(printer);
            getRules().serialize(printer);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (printer != null)
                try {
                    printer.close();
                } catch (Exception e) {}
        }
        clear();
    }

    public boolean canResume() {
        return SAVE_FILE.exists();
    }
    
    public void updateRules()  {
        saveRules();
    }
    
    public void quit() {
        running = false;
    }
    
    public boolean isRunning() {
        return running;
    }

    @Override
    public void resume() throws IOException {
        loadGame(SAVE_FILE);
        for (int i=0; i<getNumPlayers(); i++) {
            Player p = getPlayer(i);
            if (p != null && (p instanceof SwingPlayerUser)) {
                ((SwingPlayerUser)p).setGolfSwing(g);
            }
        }
        startThread();        
    }

    @Override
    public void startNewGame() {
//        addPlayer(new SwingPlayerUser("Chris0", g));
//        addPlayer(new SwingPlayerUser("Chris1", g));
//        addPlayer(new SwingPlayerUser("Chris2", g));
//        addPlayer(new SwingPlayerUser("Chris3", g));
        addPlayer(new SwingPlayerUser("Chris", g));
        addPlayer(new PlayerBot("Harry"));
        addPlayer(new PlayerBot("Phil"));
        addPlayer(new PlayerBot("Tom"));
        startThread();
    }
    
    
}
