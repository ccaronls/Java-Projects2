package cc.lib.net;

import java.util.*;

/**
 * <pre>
 * An Extendible Enum 
 * We go with this scheme for the sake of extensibility since we cannot extend an enum.
 * 
 * It still has the things we like about Enum like ordinal and allowing == for comparisons.
 * However we do lose the ability to use switch-case statements,
 * but this is acceptable since if-else work the same if not quite as pretty.
 *   
 * Example:
 * 
 *   class MyGameCommandType extends GameCommandType {
 *      MyGameCommandType(String name) {
 *          super(name);
 *      }
 *      
 *      static final MyGameCommandType CMD1 = new MyGameCommandType("CMD1");
 *   }
 *   
 *   later ...
 *   
 *   GameCommand cmd = new GameCommand(MyGameCommandType.CMD1).setArg("x", "y");
 *   cmd.write(out);
 *   
 *   and will arrive at destination:
 *   
 *   server.onClientCommand(client, cmd);
 *   
 *   or
 *   
 *   GameClient.onCommand(cmd);
 * </pre>  
 *   
 * @author ccaron
 *
 */
public final class GameCommandType implements Comparable<GameCommandType> {
    
    private final static Map<String, GameCommandType> instances = new LinkedHashMap<String, GameCommandType>() {
        // we want to preserve ordering (linked hash map) and prevent misuse by overriding put
        @Override
        public GameCommandType put(String key, GameCommandType value) {
            if (containsKey(key))
                throw new RuntimeException("Cannot put duplicate key '" + key + "'");
            return super.put(key, value);
        }
        
    };

    // These commands are all package access only and are handled internally.  
    
    // additional info is name and version    
    static final GameCommandType CL_CONNECT = new GameCommandType("CL_CONNECT");
    // additional info is name and version
    static final GameCommandType CL_RECONNECT = new GameCommandType("CL_RECONNECT");
    // no additional info
    static final GameCommandType CL_DISCONNECT = new GameCommandType("CL_DISCONNECT");
    static final GameCommandType CL_KEEPALIVE = new GameCommandType("CL_KEEPALIVE");
    // command to submit a form
    static final GameCommandType CL_FORM_SUBMIT = new GameCommandType("CL_FORM_SUBMIT");
    static final GameCommandType CL_ERROR       = new GameCommandType("CL_ERROR");

    // commands that originate from the server
    
    // confirmation command from the server that a client has connected    
    static final GameCommandType SVR_CONNECTED = new GameCommandType("SVR_CONNECTED");

    // send an error message
    static final GameCommandType SVR_MESSAGE = new GameCommandType("SVR_MESSAGE");
    // tell the client that they have been disconnected
    static final GameCommandType SVR_DISCONNECTED = new GameCommandType("SVR_DISCONNECTED");
    // server send a form to be filled out by client
    static final GameCommandType SVR_FORM = new GameCommandType("SVR_FORM");

    static final GameCommandType SVR_EXECUTE_METHOD = new GameCommandType("SVR_EXECUTE_METHOD");
    
    /**
     * 
     * @param name
     */
    public GameCommandType(String name) {
        this.mName = name;
        mOrdinal = instances.size();
        instances.put(name, this);
    }
    
    private final int mOrdinal;
    private final String mName;
    
    /**
     * Just like enum
     * @return
     */
    public final int ordinal() {
        return this.mOrdinal;
    }

    /**
     * Just like enum
     * @return
     */
    public static Iterable<GameCommandType> values() {
        return instances.values();
    }

    @Override
    public final int compareTo(GameCommandType arg0) {
        return mOrdinal - arg0.mOrdinal;
    }
    
    /**
     * 
     * @param id
     * @return
     * @throws IllegalArgumentException
     */
    public static GameCommandType valueOf(String id) throws IllegalArgumentException {
        if (!instances.containsKey(id))
            throw new IllegalArgumentException("Unknown GameComamndType '" + id + "'");
        return instances.get(id);
    }
    
    /**
     * 
     * @return
     */
    public final String name() {
        return mName;
    }
    
    @Override
    public final String toString() {
        return mName;
    }
}

