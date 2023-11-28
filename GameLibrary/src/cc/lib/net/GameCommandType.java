package cc.lib.net;

import java.util.LinkedHashMap;
import java.util.Map;

import cc.lib.utils.NoDupesMap;

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
    
    private final static Map<String, GameCommandType> instances = new NoDupesMap<>(new LinkedHashMap<>());

    // These commands are all package access only and are handled internally.

    // --------------------------------------
    // Command sent from the client
    // --------------------------------------

    // response from a waiting execOnRemote
    static final GameCommandType CL_REMOTE_RETURNS = new GameCommandType("CL_REMOTE_RETURNS");
    // additional info is name and version
    static final GameCommandType CL_CONNECT = new GameCommandType("CL_CONNECT");
    // no additional info
    static final GameCommandType PING = new GameCommandType("PING");
    // report an error that occured on the client
    static final GameCommandType CL_ERROR = new GameCommandType("CL_ERROR");
    // set the display name of this user. Includes a 'name' argument.
    static final GameCommandType CL_HANDLE      = new GameCommandType("CL_HANDLE");
    // client signals they are disconnecting
    static final GameCommandType CL_DISCONNECT = new GameCommandType("CL_DISCONNECT");


    // --------------------------------------
    // commands sent from the server
    // --------------------------------------

    // confirmation command from the server that a client has connected
    static final GameCommandType SVR_CONNECTED = new GameCommandType("SVR_CONNECTED");
    static final GameCommandType SVR_EXECUTE_REMOTE = new GameCommandType("SVR_EXEC_REMOTE");
    // server asks client to disconnect from their end
    static final GameCommandType SVR_DISCONNECT = new GameCommandType("SVR_DISCONNECT");

    // --------------------------------------
    // shared command types
    // --------------------------------------
    static final GameCommandType MESSAGE = new GameCommandType("MESSAGE");
    // confirmation from server when client asks to be disconnected
    static final GameCommandType PASSWORD = new GameCommandType("PASSWORD");



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

    public final GameCommand make() {
        return new GameCommand(this);
    };

}

