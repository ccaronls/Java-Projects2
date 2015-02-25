package cc.game.golf.swing;

import java.io.IOException;
import java.util.*;

import cc.game.golf.core.*;
import cc.game.golf.net.*;
import cc.game.golf.net.GolfProtocol.InputState;
import cc.lib.net.*;

public class MultiPlayerGolfGame extends GameClient implements IGolfGame {

    Card topOfDeck = new Card(-1);
    Card topOfDiscardPile = null;
    int knocker = -1;
    int winner = -1;
    int curPlayer = -1;
    int dealer = -1;
    int numPlayers = 0;
    int numRounds = 0;
    int playerNum = -1;
    Rules rules = new Rules();
    PlayerInfo [] players;
    State state = State.INIT;
    List<Card> deck = new ArrayList<Card>();
    final GolfSwing g;
    InputState inputState = InputState.NONE;
    int rowToTurnOver = -1;
    
    Runnable inputThread = new Runnable() {
        public void run() {
            while (isRunning()) {
                try {
                    switch (inputState) {
                        case NONE:
                            break;
                        case CHOOSE_CARD_TO_SWAP: {
                            ArrayList<Card> pickList = new ArrayList<Card>();
                            Card [][] hand = players[playerNum].cards;
                            for (Card [] row : hand) {
                                for (Card c: row) {
                                    pickList.add(c);
                                }
                            }
                            int picked = g.pickCard(pickList);
                            if (picked >= 0) {
                                send(new GameCommand(GolfProtocol.CL_CHOOSE_CARD_TO_SWAP).setArg("card", pickList.get(picked)));
                                inputState = InputState.NONE;
                            }
                            break;
                        }
                        case CHOOSE_DRAW_PILE: {
                            ArrayList<Card> pickList = new ArrayList<Card>();
                            pickList.add(topOfDeck);
                            pickList.add(topOfDiscardPile);
                            switch (g.pickCard(pickList)) {
                                case 0:
                                    send(new GameCommand(GolfProtocol.CL_CHOOSE_DRAW_PILE).setArg("type", DrawType.DTStack));
                                    inputState = InputState.NONE;
                                    break;
                                case 1:
                                    send(new GameCommand(GolfProtocol.CL_CHOOSE_DRAW_PILE).setArg("type", DrawType.DTDiscardPile));
                                    inputState = InputState.NONE;
                                    break;
                            }
                            break;
                        }
                        case CHOOSE_DISCARD_OR_PLAY: { 
                            ArrayList<Card> pickList = new ArrayList<Card>();
                            pickList.add(topOfDiscardPile);
                            Card [][] hand = players[playerNum].cards;
                            for (Card [] row : hand) {
                                for (Card c: row) {
                                    pickList.add(c);
                                }
                            }
                            int picked = g.pickCard(pickList);
                            if (picked >= 0) {
                                send(new GameCommand(GolfProtocol.CL_CHOOSE_DISCARD_OR_PLAY).setArg("card", pickList.get(picked)));
                                inputState = InputState.NONE;
                            }
                            break;
                        }
                        case CHOOSE_CARD_TO_TURN_OVER: {
                            Card [][] hand = players[playerNum].cards;
                            ArrayList<Card> pickList = new ArrayList<Card>();
                            pickList.addAll(Arrays.asList(hand[rowToTurnOver]));
                            int picked = g.pickCard(pickList);
                            if (picked >= 0) {
                                send(new GameCommand(GolfProtocol.CL_CHOOSE_CARD_TO_TURN_OVER).setArg("card", pickList.get(picked)));
                                inputState = InputState.NONE;
                            }
                            break;
                        }
                    }
                    synchronized (this) {
                        wait(2000);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    inputState = InputState.NONE;
                }
            }
        }
    };
    
    public MultiPlayerGolfGame(GolfSwing g, String userName) throws IOException {
        super(userName, GolfProtocol.VERSION);
        this.g = g;
        connect("127.0.0.1", GolfProtocol.PORT);
    }

    @Override
    public Card getTopOfDeck() {
        return topOfDeck;
    }

    @Override
    public Card getTopOfDiscardPile() {
        return topOfDiscardPile;
    }

    @Override
    public int getKnocker() {
        return knocker;
    }

    @Override
    public int getNumPlayers() {
        return numPlayers;
    }

    @Override
    public Rules getRules() {
        return rules;
    }

    @Override
    public Card[][] getPlayerCards(int player) {
        return players[player].cards;
    }

    @Override
    public String getPlayerName(int player) {
        return players[player].name;
    }

    @Override
    public int getHandPoints(int player) {
        return players[player].handPoints;
    }

    @Override
    public int getNumRounds() {
        return numRounds;
    }

    @Override
    public int getDealer() {
        return dealer;
    }

    @Override
    public int getWinner() {
        return winner;
    }

    @Override
    public int getCurrentPlayer() {
        return curPlayer;
    }
    
    @Override
    public int getPlayerPoints(int player) {
        return players[player].points;
    }

    @Override
    public List<Card> getDeck() {
        return deck;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public Card getPlayerCard(int player, int row, int col) {
        return players[player].cards[row][col];
    }

    @Override
    public boolean canResume() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void updateRules() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void quit() {
        disconnect();
    }

    @Override
    public boolean isRunning() {
        return isConnected();
    }

    @Override
    public void resume() throws Exception {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void startNewGame() {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void onMessage(String message) {
        g.setMessage(message);
    }

    @Override
    protected void onDisconnected(String message) {
        g.setMessage("Client Disconnected: " + message);
        g.newSinglePlayerGame();
    }

    @Override
    protected void onConnected() {
        this.logInfo("Client Connected");
    }

    @Override
    protected void onCommand(GameCommand cmd) {
        try {
            if (cmd.getType() == GolfProtocol.SRVR_ON_CARD_TURNEDOVER) {
                int player = Integer.parseInt(cmd.getArg("player"));
                Card card = GolfProtocol.parseCard(cmd.getArg("card"));
                int row = Integer.parseInt(cmd.getArg("row"));
                int col = Integer.parseInt(cmd.getArg("col"));
                g.startTurnOverCardAnimation(player, card, row, col);
                send(new GameCommand(GolfProtocol.CL_CONTINUE));
            } else if (cmd.getType() == GolfProtocol.SRVR_CHOOSE_CARD_TO_SWAP) {
                inputState = InputState.CHOOSE_CARD_TO_SWAP;
                inputThread.notify();
            } else if (cmd.getType() == GolfProtocol.SRVR_CHOOSE_DISCARD_OR_PLAY) {
                inputState = InputState.CHOOSE_DISCARD_OR_PLAY;
                inputThread.notify();
            } else if (cmd.getType() == GolfProtocol.SRVR_CHOOSE_CARD_TO_TURN_OVER) {
                rowToTurnOver = Integer.parseInt(cmd.getArg("row"));
                inputState = InputState.CHOOSE_CARD_TO_TURN_OVER;
                inputThread.notify();
            } else if (cmd.getType() == GolfProtocol.SRVR_CHOOSE_DRAW_PILE) {
                inputState = InputState.CHOOSE_DRAW_PILE;
                inputThread.notify();
            } else if (cmd.getType() == GolfProtocol.SRVR_ON_DRAW_PILE_CHOOSEN) {
                DrawType type = DrawType.valueOf(cmd.getArg("type"));
                if (type == DrawType.DTStack)
                    g.startTurnOverCardAnimationStack();
                topOfDeck = GolfProtocol.parseCard(cmd.getArg("card"));
                send(new GameCommand(GolfProtocol.CL_CONTINUE));
            } else if (cmd.getType() == GolfProtocol.SRVR_ON_CARD_DEALT) {
                int player = Integer.parseInt(cmd.getArg("player"));
                Card card = GolfProtocol.parseCard(cmd.getArg("card"));
                int row = Integer.parseInt(cmd.getArg("row"));
                int col = Integer.parseInt(cmd.getArg("col"));
                g.startDealCardAnimation(player, card, row, col);
                send(new GameCommand(GolfProtocol.CL_CONTINUE));
            } else if (cmd.getType() == GolfProtocol.SRVR_ON_CARD_DISCARDED) {
                Card card = GolfProtocol.parseCard(cmd.getArg("card"));
                g.startDiscardDrawnCardAnimation(card);
                send(new GameCommand(GolfProtocol.CL_CONTINUE));
            } else if (cmd.getType() == GolfProtocol.SRVR_ON_CARD_SWAPPED) {
                int player = Integer.parseInt(cmd.getArg("player"));
                Card card = GolfProtocol.parseCard(cmd.getArg("card"));
                int row = Integer.parseInt(cmd.getArg("row"));
                int col = Integer.parseInt(cmd.getArg("col"));
                DrawType type = DrawType.valueOf(cmd.getArg("type"));
                g.startSwapCardAnimation(player, type, card, row, col);
                send(new GameCommand(GolfProtocol.CL_CONTINUE));
            } else if (cmd.getType() == GolfProtocol.SRVR_ON_KNOCK) {
                knocker = Integer.parseInt(cmd.getArg("player"));
                send(new GameCommand(GolfProtocol.CL_CONTINUE));
            } else if (cmd.getType() == GolfProtocol.SRVR_GAME_STATE) {
                state = State.valueOf(cmd.getArg("state"));
                curPlayer = Integer.parseInt(cmd.getArg("curPlayer"));
                numPlayers = Integer.parseInt(cmd.getArg("numPlayers"));
                winner = Integer.parseInt(cmd.getArg("winner"));
                knocker = Integer.parseInt(cmd.getArg("knocker"));
                dealer = Integer.parseInt(cmd.getArg("dealer"));
                numRounds = Integer.parseInt(cmd.getArg("numRounds"));
                topOfDeck = GolfProtocol.parseCard(cmd.getArg("topOfDeck"));
                topOfDiscardPile = GolfProtocol.parseCard(cmd.getArg("topOfDiscardPile"));
                if (players == null) {
                    players = new PlayerInfo[numPlayers];
                    for (int i=0; i<numPlayers; i++)
                        players[i] = new PlayerInfo();
                } else if (players.length != numPlayers) {
                    PlayerInfo [] newInfo = new PlayerInfo[numPlayers];
                    int i=0;
                    for (i=0; i<Math.min(numPlayers, players.length); i++) {
                        newInfo[i] = players[i];
                    }
                    while (i < numPlayers) {
                        newInfo[i++] = new PlayerInfo();
                    }
                    players = newInfo;
                }
            } else if (cmd.getType() == GolfProtocol.SRVR_PLAYER_STATE) {
                int player = Integer.parseInt(cmd.getArg("player"));
                players[player].deserialize(cmd.getArg("info"));
            } else {
                throw new Exception("Unhandled message: " + cmd.getType());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onForm(ClientForm form) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public int getFrontPlayer() {
        return playerNum;
    }

    
    
    
}
