package cc.game.dominos.core;

import cc.lib.crypt.Cypher;
import cc.lib.crypt.HuffmanEncoding;
import cc.lib.net.GameCommandType;

/**
 * Created by chriscaron on 3/20/18.
 */

public class MPConstants {

    public final static String VERSION = "DOMINOS.1.0";
    public final static int PORT = 16342;
    public final static int CLIENT_READ_TIMEOUT = 15000;
    public final static int MAX_CONNECTIONS = 8;

    public final static String DOMINOS_ID = "Dominos";
    public final static String USER_ID    = "User";

    public final static String DNS_TAG = "_dom._tcp.local.";

    public static Cypher getCypher() {
        if (true)
            return null;
        try {
            int [] counts = {1006,0,0,0,0,0,0,0,0,258,996,0,0,936,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,236,821,536,382,748,66,35,966,170,539,365,189,261,125,183,869,226,108,132,427,317,641,286,842,325,247,132,927,608,447,817,14,927,481,1004,453,583,278,27,311,190,540,2147483647,986,544,703,394,388,627,412,1015,922,585,567,290,32,215,450,846,26,1021,324,110,488,896,74,78,216,231,372,917,81,537,959,522,968,433,515,115,318,879,455,984,82,962,675,760,961,565,128,736,287,143,607};
            return new HuffmanEncoding(counts);
        } catch (Exception e) {
            throw new AssertionError();
        }
    }

    /** include:
     *  numPlayers(int)
     *  playerNum(int) of the client
     *  dominos(serialixed)
     */
    public final static GameCommandType SVR_TO_CL_INIT_GAME   = new GameCommandType("SVR_INIT_GAME");

    public final static GameCommandType CL_TO_SVR_FORFEIT      = new GameCommandType("CL_FORFEIT");

    /**
     * Just pass the serialized dominos
     */
    public final static GameCommandType SVR_TO_CL_INIT_ROUND  = new GameCommandType("SVR_INIT_ROUND");
}
