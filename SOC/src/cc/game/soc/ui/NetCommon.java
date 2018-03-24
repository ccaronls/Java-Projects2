package cc.game.soc.ui;

import cc.lib.crypt.Cypher;
import cc.lib.crypt.HuffmanEncoding;

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
            int[] counts = {979, 0, 0, 0, 0, 0, 0, 0, 0, 474, 543, 0, 0, 13, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 305, 670, 21, 670, 775, 1012, 248, 182, 618, 408, 597, 215, 761, 774, 177, 325, 1020, 584, 742, 351, 994, 303, 298, 378, 910, 864, 2147483647, 575, 907, 27, 12, 257, 223, 541, 82, 397, 747, 225, 78, 352, 256, 1003, 384, 483, 811, 369, 359, 406, 600, 712, 96, 157, 723, 321, 374, 774, 112, 245, 997, 787, 11, 861, 101, 85, 148, 691, 693, 312, 482, 141, 628, 272, 335, 171, 289, 51, 85, 205, 621, 67, 570, 11, 1002, 746, 242, 81, 898, 170, 946, 403, 100, 274, 267, 251};
            HuffmanEncoding enc = new HuffmanEncoding(counts);
            enc.keepAllOccurances();
            enc.generate();
            return enc;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
