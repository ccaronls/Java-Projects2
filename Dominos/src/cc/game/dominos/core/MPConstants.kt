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
        try {
            int [] counts = {12182,876,36,18,422,457,2425,436,25,557,35303,1,574,2,107,556,136,338,14,0,2,0,4,0,0,0,0,0,0,0,330,20,220703,1,28,0,0,0,0,0,0,0,0,7,0,114,27637,1,8055,8475,7269,3320,2972,2646,1912,868,827,1188,0,0,0,22957,0,0,674,1585,759,2123,1061,5440,873,730,740,1887,762,1259,2423,5056,1150,1813,5331,777,2924,1671,5605,1365,1890,0,555,0,0,0,1,0,0,2298,0,19941,807,30044,11287,46596,795,8763,1636,27990,1524,761,11761,22888,25259,32291,34477,692,11573,13304,13971,1846,4817,749,855,1189,740,7222,0,7222,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,62,0,0,0,22,0,0,0,0,1,0,0,0,0,0,0,0,0,0,28,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,14,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,28,0,0,0,18,0,0,0,17,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,1,1,1,1};
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
