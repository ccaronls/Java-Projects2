package cc.lib.net;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.utils.NoDupesMap;
import cc.lib.utils.Reflector;

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

    private final static Logger log = LoggerFactory.getLogger(GameCommand.class);

    private final GameCommandType type;
    private final Map<String, Object> arguments = new NoDupesMap<>(new LinkedHashMap());

    /**
     * @param commandType
     */
    public GameCommand(GameCommandType commandType) {
        this.type = commandType;
    }

    /**
     * @return
     */
    public Map<String, Object> getArguments() {
        return arguments;
    }

    /**
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
    public final String getString(String key) {
        return (String)arguments.get(key);
    }

    public final boolean getBoolean(String key, boolean defaultValue) {
        Object o = arguments.get(key);
        if (o != null) {
            return (Boolean) o;
        }
        return defaultValue;
    }

    public final boolean getBoolean(String key) {
        return (Boolean) arguments.get(key);
    }

    /**
     * @param key
     * @return
     */
    public final int getInt(String key) {
        return (Integer) arguments.get(key);
    }

    public final long getLong(String key) {
        return (Long)arguments.get(key);
    }

    public final float getFloat(String key) {
        return (Float) arguments.get(key);
    }

    public final double getDouble(String key) {
        return (Double) arguments.get(key);
    }

    public final <T extends Reflector> T getReflector(String key, T object) throws Exception {
        object.merge((String) arguments.get(key));
        return object;
    }

    /**
     * @return
     */
    String getVersion() {
        return getString("version");
    }

    /**
     *
     * @return
     */
    public final String getName() {
        return getString("name");
    }
    
    /**
     * 
     * @return
     */
    public final String getMessage() {
        return getString("message");
    }
    
    /**
     * 
     * @return
     */
    public final String getFilter() {
        return getString("filter");
    }

    /**
     * 
     * @param nm
     * @param val
     * @return
     */
    public final GameCommand setArg(String nm, Object val) {
        if (nm == null)
            throw new NullPointerException();
        if (val == null)
            arguments.remove(nm);
        else
            arguments.put(nm, val);
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
     * @param version
     * @return
     */
    public final GameCommand setVersion(String version) {
        return setArg("version", version);
    }

    /**
     * @param args
     * @return
     */
    public final GameCommand setArgs(Map<String, Object> args) {
        for (Map.Entry<String, Object> e : args.entrySet()) {
            setArg(e.getKey(), e.getValue());
        }
        return this;
    }

    /**
     * @param arg
     * @param items
     * @return
     */
    public final <T extends Enum<T>> GameCommand setEnumList(String arg, Collection<T> items) {
        Integer[] list = new Integer[items.size()];
        int index = 0;
        for (T item : items) {
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
        String s = getString(arg);
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
        String s = getString(arg);
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

    public static GameCommand parse(DataInputStream din) throws Exception {
        String cmd = din.readUTF();
        GameCommandType type = GameCommandType.valueOf(cmd);
        GameCommand command = new GameCommand(type);
        int numArgs = din.readInt();
        for (int i=0; i<numArgs; i++) {
            String key = din.readUTF();
            int itype = din.readUnsignedByte();
            switch (itype) {
                case TYPE_NULL:
                    break;
                case TYPE_BOOL:
                    command.arguments.put(key, din.readBoolean());
                    break;
                case TYPE_INT:
                    command.arguments.put(key, din.readInt());
                    break;
                case TYPE_LONG:
                    command.arguments.put(key, din.readLong());
                    break;
                case TYPE_FLOAT:
                    command.arguments.put(key, din.readFloat());
                    break;
                case TYPE_DOUBLE:
                    command.arguments.put(key, din.readDouble());
                    break;
                case TYPE_STRING:
                    command.arguments.put(key, din.readUTF());
                    break;
                case TYPE_REFLECTOR: {
                    int len = din.readInt();
                    byte[] data = new byte[len];
                    din.readFully(data);
                    command.arguments.put(key, new String(data, "UTF-8"));
                    break;
                }
                default:
                    throw new cc.lib.utils.GException("Unhandled type " + itype);
            }
        }
        return command;
    }

    /**
     *
     * @param dout
     * @throws Exception
     */
    public final void write(DataOutputStream dout) throws IOException {
        dout.writeUTF(type.name());
        dout.writeInt(arguments.size());
        for (String key : arguments.keySet()) {
            dout.writeUTF(key);
            Object o = arguments.get(key);
            if (o == null) {
                dout.writeByte(TYPE_NULL);
            } else if (o instanceof Boolean) {
                dout.writeByte(TYPE_BOOL);
                dout.writeBoolean((Boolean) o);
            } else if (o instanceof Integer) {
                dout.writeByte(TYPE_INT);
                dout.writeInt((Integer)o);
            } else if (o instanceof Long) {
                dout.writeByte(TYPE_LONG);
                dout.writeLong((Long)o);
            } else if (o instanceof Float) {
                dout.writeByte(TYPE_FLOAT);
                dout.writeFloat((Float)o);
            } else if (o instanceof Double) {
                dout.writeByte(TYPE_DOUBLE);
                dout.writeDouble((Double)o);
            } else if (o instanceof String) {
                dout.writeByte(TYPE_STRING);
                dout.writeUTF((String)o);
            } else if (o instanceof Reflector) {
                dout.writeByte(TYPE_REFLECTOR);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ((Reflector) o).serialize(new Reflector.MyPrintWriter(out));
                byte[] bytes = out.toByteArray();
                dout.writeInt(bytes.length);
                dout.write(bytes);
            } else {
                dout.writeByte(TYPE_STRING);
                dout.writeUTF(o.toString());
            }
        }
        dout.flush();
    }

    private final static int TYPE_NULL = 0;
    private final static int TYPE_BOOL = 1;
    private final static int TYPE_INT = 2;
    private final static int TYPE_LONG = 3;
    private final static int TYPE_FLOAT = 4;
    private final static int TYPE_DOUBLE = 5;
    private final static int TYPE_STRING = 6;
    private final static int TYPE_REFLECTOR = 7;

    public String toString() {
        return type + ": " + arguments;
    }


}
