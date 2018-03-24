package cc.game.dominos.core;

import cc.lib.crypt.Cypher;
import cc.lib.crypt.HuffmanEncoding;
import cc.lib.net.GameCommand;
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
            int [] counts = {385,25,4,2,12,11,87,35,0,24,757,1,12,2,5,21,1,3,0,0,1,0,0,1,0,0,0,0,0,0,26,2,4268,0,8,0,0,0,0,0,0,0,0,0,0,36,628,0,261,230,169,147,46,71,41,18,24,130,0,0,0,452,0,0,7,22,7,48,36,137,15,8,7,49,6,12,55,102,40,51,106,9,62,45,112,23,48,0,21,0,0,0,1,0,0,61,0,625,34,506,214,828,16,186,45,522,58,18,264,458,495,571,542,5,306,290,347,76,107,14,37,45,8,164,0,164,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2};
            HuffmanEncoding enc = new HuffmanEncoding(counts);
            enc.keepAllOccurances();
            enc.generate();
            return enc;
        } catch (Exception e) {
            throw new AssertionError();
        }
    }

    /** include:
     *  numPlayers(int)
     *  playerNum(int) of the client
     */
    public final static GameCommandType SVR_TO_CL_INIT_GAME   = new GameCommandType("SVR_INIT_GAME");

    public final static GameCommandType CL_TO_SVR_FORFEIT      = new GameCommandType("CL_FORFEIT");

    /**
     * Just pass the serialized dominos
     */
    public final static GameCommandType SVR_TO_CL_INIT_ROUND  = new GameCommandType("SVR_INIT_ROUND");

    public static GameCommand getSvrToClInitGameCmd(Dominos d, Player remote) {
        return new GameCommand(SVR_TO_CL_INIT_GAME)
                .setArg("numPlayers", d.getNumPlayers())
                .setArg("playerNum", remote.getPlayerNum());
    }

}
