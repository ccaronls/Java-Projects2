package cc.game.kaiser.net;

import java.io.*;
import java.util.*;

import cc.game.kaiser.core.*;
import cc.lib.net.*;

/**
 * This class meant to hold all protocol encode/decode between client and server
 * 
 * Server makes calls directly to the package hidden methods
 * Client implement Listener and make calls to the public methods
 * 
 * 
 * @author ccaron
 *
 */
public class KaiserCommand extends GameCommandType {

    /**
     * Client implement this @see clientProcess
     * @author ccaron
     *
     */
    public interface Listener {

        void onSetPlayer(int num, String name);

        void onPlayTrick(Card[] options);

        void onMakeBid(Bid[] bids);

        void onDealtCard(Card card);

        void onGameUpdate(InputStream in);
    }
    
    private KaiserCommand(String name) {
        super(name);
    }

    // declare all the commands so the GameCommand parser knows about them
    private static final GameCommandType SET_PLAYER  = new GameCommandType("SET_PLAYER");
    private static final GameCommandType MAKE_BID    = new GameCommandType("MAKE_BID");
    private static final GameCommandType PLAY_TRICK  = new GameCommandType("PLAY_TRICK");
    private static final GameCommandType DEALT_CARD  = new GameCommandType("DEALT_CARD");
    private static final GameCommandType UPDATE_GAME = new GameCommandType("UPDATE_GAME");
    //private static final GameCommandType UPDATE_PLAYER = new GameCommandType("UPDATE_PLAYER");
    
    // make a string out of an array
    private static <T> String toString(T [] arr) {
        StringBuffer buf = new StringBuffer();
        for (int i=0; i<arr.length; i++) {
            String s = String.valueOf(arr[i]).trim();
            if (s.length() > 0) {
                if (buf.length() > 0)
                    buf.append(",");
                buf.append(s); 
            }
        }
        return buf.toString();
    }
    
    private interface IParser<T> {
        T parse(String s) throws IllegalArgumentException;
    }
    
    // convert a string to a list
    private static <T> List<T> fromString(String s, IParser<T> parser) {
        String [] parts = s.split("[,]");
        ArrayList<T> list = new ArrayList<T>();
        for (int i=0; i<parts.length; i++) {
            list.add(parser.parse(parts[i]));
        }
        return list;
    }
    
    // server commands
    static GameCommand getSetPlayerCommand(String name, int i) {
        return new GameCommand(SET_PLAYER).setArg("name", name).setArg("num", i);
    }
    static GameCommand getPlayTrickCommand(Card [] options) {
        return new GameCommand(PLAY_TRICK).setArg("options", toString(options));
    }
    static GameCommand getMakeBidCommand(Bid [] bids) {
        return new GameCommand(MAKE_BID).setArg("options", toString(bids));
    }
    static GameCommand getDealtCardCommand(Card c) {
        return new GameCommand(DEALT_CARD).setArg("card", c.toString());
    }
    static GameCommand getUpdateGameCommand(Kaiser kaiser) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        kaiser.serialize(out);
        out.close();
        return new GameCommand(UPDATE_GAME).setArg("game", new String(out.toByteArray()));
    }

    /**
     * Should be used by clients to decode the commands into associated callbacks in @see Listener.
     * Returns false if command not handled.  This func is typically called from GameClient.onCommand 
     * 
     * @param listener
     * @param cmd
     * @return
     * @throws Exception
     */
    public static boolean clientDecode(Listener listener, GameCommand cmd) throws Exception {
        if (cmd.getType() == SET_PLAYER) {
            String name = cmd.getString("name");
            int num = Integer.parseInt(cmd.getString("num"));
            listener.onSetPlayer(num, name);
        } else if (cmd.getType() == PLAY_TRICK) {
            List<Card> cards = fromString(cmd.getString("options"), new IParser<Card>() {
                public Card parse(String s) {
                    return Card.parseCard(s);
                }
            });
            Card [] options = cards.toArray(new Card[cards.size()]);
            listener.onPlayTrick(options);
        } else if (cmd.getType() == MAKE_BID) {
            List<Bid> bids = fromString(cmd.getString("options"), new IParser<Bid>() {
                public Bid parse(String s) {
                    return Bid.parseBid(s);
                }
            });
            Bid [] options = bids.toArray(new Bid[bids.size()]);
            listener.onMakeBid(options);
        } else if (cmd.getType() == DEALT_CARD) {
            Card card = Card.parseCard(cmd.getString("card"));
            listener.onDealtCard(card);
        } else if (cmd.getType() == UPDATE_GAME) {
            ByteArrayInputStream in = new ByteArrayInputStream(cmd.getString("game").getBytes());
            try {
                listener.onGameUpdate(in);
            } finally {
                try {
                    in.close();
                } catch (Exception ex) {}
            }
        }
        
        else {
            return false;
        }
        
        return true;
    }

    // -----------------------------------------------------------------------------------------
    // -----------------------------------------------------------------------------------------
    // -----------------------------------------------------------------------------------------
    // -----------------------------------------------------------------------------------------
    // -----------------------------------------------------------------------------------------

    // CLIENT
    
    // Define commands that originate from the client.
    
    // respond to a playTrick command
    private static final GameCommandType CL_PLAY_TRICK = new GameCommandType("CL_PLAY_TRICK");
    private static final GameCommandType CL_MAKE_BID = new GameCommandType("CL_MAKE_BID");
    
    public static GameCommand clientPlayTrick(Card card) {
        return new GameCommand(CL_PLAY_TRICK).setArg("card", card);
    }
    public static GameCommand clientMakeBid(Bid bid) {
        return new GameCommand(CL_MAKE_BID).setArg("bid", bid);
    }

    // Server uses this to decode
    
    static boolean serverDecode(RemotePlayer player, KaiserServer server, GameCommand cmd) throws Exception {
        if (cmd.getType() == CL_PLAY_TRICK) {
            Card card = Card.parseCard(cmd.getString("card"));
            if (player != null)
                player.setResponse(card);
        } else if (cmd.getType() == CL_MAKE_BID) {
            Bid bid = Bid.parseBid(cmd.getString("bid"));
            if (player != null)
                player.setResponse(bid);
        }
        
        else {
            return false;
        }
        
        return true;
    }
    
}
