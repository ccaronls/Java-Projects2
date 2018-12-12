package cc.game.soc.ui;

import cc.lib.crypt.Cypher;
import cc.lib.crypt.HuffmanEncoding;
import cc.lib.net.GameCommandType;

/**
 * Created by chriscaron on 3/12/18.
 */

public class NetCommon {

    public final static String USER_ID = "PlayerUser";
    public final static String SOC_ID = "SOC";

    public final static int PORT = 15551;
    public final static String VERSION = "SOC1.0";
    public final static int CLIENT_READ_TIMEOUT = 20000;
    public final static int MAX_CONNECTIONS = 8;

    public final static String DNS_SERVICE_ID = "_soc._tcp.local.";

    public static Cypher getCypher() {
        try {
            int [] counts = {0,0,0,0,0,0,0,0,0,0,275883,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1257558,0,0,0,0,0,0,0,0,0,0,0,0,94,339046,0,812,9178,10282,9494,8439,7550,9458,13,9,13,0,0,0,21056,0,0,0,7718,1142,48994,1684,11074,57,500,144,80896,0,57,7969,71,8567,35989,12425,0,13645,80100,5183,3768,3282,174,1793,4170,0,0,0,0,0,8671,0,292194,108,241162,19716,258754,13,147110,14,12327,56905,42303,72978,91145,109648,104923,3889,0,128677,59648,163926,16152,56921,0,1536,10810,0,105802,0,105802};
            HuffmanEncoding enc = new HuffmanEncoding(counts);
            enc.keepAllOccurances();
            enc.generate();
            return enc;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public final static GameCommandType SVR_TO_CL_INIT = new GameCommandType("SVR_TO_CL_INIT");

    public final static GameCommandType SVR_TO_CL_UPDATE = new GameCommandType("SVR_TO_CL_UPDATE");
}
