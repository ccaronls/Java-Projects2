package cc.lib.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.lib.utils.NoDupesMap;

/**
 * <pre>
 * GameCommand is the protocol used for clients and servers to talk to each other.
 * 
 * Only logic to encode and decode commands and access the data is here.
 * 
 * Recommended example of extending this class to keep protocol logic abstracted:
 * 
 *  // all encoding/decoding logic is isolated to this class
 * class MyCommands {
 *    final static GameCommand CMD1 = new GameCommand("CMD1");
 *    final static GameCommand CMD2 = new GameCommand("CMD2");
 *    
 *    interface Listener {
 *       void onCMD1(String arg1);
 *       void onCMD2(int arg1, int arg2);
 *    }
 *    
 *    // server commands.  server gets an instance of the command it wishes to send.
 *    GameCommand getCMD1(String arg) {
 *       return new GameCommand(CMD1).setArg("arg", arg);
 *    }
 *    
 *    GameCommand getCMD2(int a, int b) {
 *       return new GameCommand(CMD2).setArg("a", a).setArg("b", b);
 *    }
 * 
 *    // this is typically called from GameClient.onCommand(cmd)
 *    public static boolean clientDecode(Listener listener, GameCommand cmd) {
 *       if (cmd.getType() == CMD1) {
 *          listener.onCMD1(cmd.getArg("arg"));
 *       } else if (cmd.getType() == CMD2) {
 *          listener.onCMD2(Integer.parseInt(cmd.getArg("a")), Integer.parseInt(cmd.getArg("b")));
 *       } else {
 *          return false; // tell caller we did not handle the command
 *       }
 *       
 *       return true;
 *    }
 * }
 *  </pre>
 * @author ccaron
 *
 */
public class GameCommand {

    private final GameCommandType type;
    private final Map<String, String> arguments = new NoDupesMap<>(new HashMap<String,String>());
    
    /**
     * 
     * @param commandType
     */
    public GameCommand(GameCommandType commandType) {
        this.type = commandType;
    }

    /**
     * 
     * @return
     */
    public final GameCommandType getType() {
        return this.type;
    }

    /**
     * 
     * @param key
     * @return
     */
    public final String getArg(String key) {
        return arguments.get(key);
    }
    
    /**
     * 
     * @param key
     * @return
     */
    public final int getInt(String key) {
        return Integer.parseInt(getArg(key));
    }
    
    /**
     * 
     * @return
     */
    public final Iterable<String> getArguments() {
        return arguments.values();
    }
    
    Map<String,String> getProperties() {
        return this.arguments;
    }

    static GameCommand parse(DataInputStream din) throws Exception {
        String cmd = din.readUTF();
        GameCommandType type = GameCommandType.valueOf(cmd);
        GameCommand command = new GameCommand(type);
        int numArgs = din.readInt();
        for (int i=0; i<numArgs; i++) {
            command.arguments.put(din.readUTF(), din.readUTF());
        }
        return command;
    }
    
    /**
     * 
     * @return
     */
    String getVersion() {
        return arguments.get("version");
    }

    /**
     * 
     * @return
     */
    public final String getName() {
        return arguments.get("name");
    }
    
    /**
     * 
     * @return
     */
    public final String getMessage() {
        return arguments.get("message");
    }
    
    /**
     * 
     * @return
     */
    public final String getFilter() {
        return arguments.get("filter");
    }

    /**
     * 
     * @param nm
     * @param val
     * @return
     */
    public final GameCommand setArg(String nm, Object val) {
        if (val == null)
            arguments.remove(nm);
        else
            arguments.put(nm,  val.toString());
        return this;
    }
    
    /**
     * 
     * @param name
     * @return
     */
    public final GameCommand setName(String name) {
        return setArg("name", name);
    }
    
    /**
     * 
     * @param msg
     * @return
     */
    public final GameCommand setMessage(String msg) {
        return setArg("message", msg);
    }
    
    /**
     * 
     * @param filter
     * @return
     */
    public final GameCommand setFilter(String filter) {
        return setArg("filter", filter);
    }

    /**
     * 
     * @param version
     * @return
     */
    public final GameCommand setVersion(String version) {
        return setArg("version", version);
    }
    
    /**
     * 
     * @param arg
     * @param items
     * @return
     */
    public final <T extends Enum<T>> GameCommand setEnumList(String arg, Collection<T> items) {
        Integer [] list = new Integer[items.size()];
        int index=0;
        for (T item:items) {
            list[index++] = item.ordinal();
        }
        return setIntList(arg, list);
    }
    
    /**
     * 
     * @param arg
     * @param enumType
     * @return
     */
    public final <T extends Enum<T>> List<T> getEnumList(String arg, Class<T> enumType) {
        String s = getArg(arg);
        if (s == null)
            return null;
        String [] ss = s.split("[, ]+");
        List<T> list = new ArrayList<T>();
        for (int i=0; i<ss.length; i++) {
            ss[i] = ss[i].trim();
            if (ss[i].length() > 0)
                list.add(Enum.valueOf(enumType, ss[i]));
        }
        return list;
    }
    
    /**
     * 
     * @param arg
     * @param items
     * @return
     */
    public final GameCommand setIntList(String arg, Collection<Integer> items) {
        return setIntList(arg, items.toArray(new Integer[items.size()]));
    }
    
    /**
     * 
     * @param arg
     * @param items
     * @return
     */
    public final GameCommand setIntList(String arg, Integer [] items) {
        return setArg(arg, Arrays.toString(items));
    }
    
    /**
     * 
     * @param arg
     * @return
     */
    public final List<Integer> getIntList(String arg) {
        String s = getArg(arg);
        if (s == null)
            return null;
        String [] ss = s.split("[, ]+");
        ArrayList<Integer> list = new ArrayList<Integer>();
        for (int i=0; i<ss.length; i++) {
            ss[i] = ss[i].trim();
            if (ss[i].length() > 0)
                list.add(Integer.parseInt(ss[i].trim()));
        }
        return list;
    }

    /**
     * 
     * @param dout
     * @throws Exception
     */
    public final void write(DataOutputStream dout) throws Exception {
        dout.writeUTF(type.name());
        dout.writeInt(arguments.size());
        for (String key : arguments.keySet()) {
            dout.writeUTF(key);
            dout.writeUTF(arguments.get(key));
        }
        dout.flush();
    }
    
    public String toString() {
        return type + ": " + arguments;
    }


}
