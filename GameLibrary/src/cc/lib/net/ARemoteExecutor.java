package cc.lib.net;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.lib.game.Utils;
import cc.lib.utils.NoDupesMap;
import cc.lib.utils.Reflector;

/**
 * Created by chriscaron on 3/12/18.
 *
 * Execute methods remotely and wait for the return value if it has one. This should have effect of
 * making the socket connection appear transparent the the caller.
 *
 * TODO: Add retries?
 *
 * Be sure to not obfuscate those methods involved in this scheme so different versions
 * of application can remain compatible.
 *
 * Example:
 *
 * you have some object that exists on 2 systems connected by I/O stream.
 *
 * MyObjectType myObject;
 *
 * the client and server endpoints both derive from ARemoteExecutor
 *
 * On system A:
 * server = new ARemoteExecutor() { ... }
 *
 * On system B:
 * client = new ARemoteExecutor() {... }
 *
 * System register objects to be executed upon
 *
 * client.register(myObject.getClass().getSimpleName(), myObject);
 *
 * ...
 *
 * @Keep
 * class MyObjectType {
 *
 *     // make sure to prevent obfuscation
 *     @Keep
 *     public Integer add(int a, int b) {
 *         try {
 *             // executeOnRemote will determine method name and class from Exception stack
 *             // and bundle everything up to the client then wait for a response and return
 *             // it once it arrives
 *             return server.executeOnRemote(true, a, b);
 *         } catch (IOException e) {
 *             ...
 *         }
 *         return null; // good practice to return non-primitives
 *     }
 * }
 *
 */

public abstract class ARemoteExecutor {

    public static final GameCommandType EXECUTE_REMOTE = new GameCommandType("EXEC_REMOTE");
    public static final GameCommandType REMOTE_RETURNS = new GameCommandType("REMOTE_RETURNS");

    private Map<String, Object> executorObjects = new NoDupesMap<>(new HashMap<String, Object>());

    private final class ResponseListener<T> implements GameCommandType.Listener {
        private T response;
        private final String id;

        ResponseListener(String id) {
            this.id = id;
        }

        public void setResponse(T response) {
            this.response = response;
            synchronized (this) {
                notify();
            }
        }

        @Override
        public void onCommand(GameCommand cmd) {
            if (cmd.getArg("target").equals(id)) {
                unregister(id);
                try {
                    setResponse((T)Reflector.deserializeFromString(cmd.getArg("return")));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    /**
     * Send a command to remote listener
     * @param cmd
     * @throws IOException
     */
    public abstract void sendCommand(GameCommand cmd) throws IOException;

    /**
     * return connected status
     *
     * @return
     */
    public abstract boolean isConnected();

    /**
     * Register an object using its classname (make sure it matches string from gettClassName in stacktrace)
     * @param o
     */
    public final void register(Object o) {
        register(o.getClass().getName(), o);
    }

    /**
     * register an object with a specific id
     * @param id
     * @param o
     */
    public final void register(String id, Object o) {
        executorObjects.put(id, o);
    }

    /**
     * Unregister an object by its id
     * @param id
     */
    public final void unregister(String id) {
        executorObjects.remove(id);
    }

    /**
     * Methods that call this method will have the same method signature executed on remote
     *   object of same type, provided it has been registered with its endpoint
     * @param hasReturn
     * @param params
     * @param <T>
     * @return
     * @throws IOException
     */
    public final <T> T executeOnRemote(boolean hasReturn, Object...params) throws IOException {
        StackTraceElement elem = new Exception().getStackTrace()[1];
        return executeOnRemote(elem.getClassName(), elem.getMethodName(), hasReturn, params);
    }

    /**
     *
     * @param targetId
     * @param method
     * @param hasReturn
     * @param params
     * @param <T>
     * @return
     */
    public final <T> T executeOnRemote(String targetId, String method, boolean hasReturn, Object ... params) throws IOException {
        GameCommand cmd = new GameCommand(EXECUTE_REMOTE);
        cmd.setArg("method", method);
        cmd.setArg("target", targetId);
        cmd.setArg("numParams", params.length);
        for (int i=0; i<params.length; i++) {
            cmd.setArg("param"+i, Reflector.serializeObject(params[i]));
        }
        if (hasReturn) {
            String id = Utils.genRandomString(64);
            cmd.setArg("responseId", id);
            ARemoteExecutor.ResponseListener<T> listener = new ARemoteExecutor.ResponseListener<>(id);
            EXECUTE_REMOTE.addListener(listener);
            //register(id, listener);
            try {
                sendCommand(cmd);
                synchronized (listener) {
                    listener.wait();
                }
            } catch (Exception e) {
                throw new IOException(e);
            } finally {
                unregister(id);
            }
            return listener.response;
        }
        sendCommand(cmd);
        return null;
    }

    /**
     * Derived classes can extend this method to handle their own types.
     * Should call this at some point. Retunrs true if the cmd handled.
     *
     * @param cmd
     * @return
     * @throws IOException
     */
    public boolean processCommand(GameCommand cmd) throws IOException {
        if (cmd.getType() == EXECUTE_REMOTE) {
            handleExecuteRemote(cmd);
        } else if (cmd.getType() == REMOTE_RETURNS) {
            if (!executorObjects.containsKey(cmd.getArg("responseId")))
                return false;
            handleReturn(cmd);
        } else {
            return false;
        }

        return true;
    }

    private void handleReturn(GameCommand cmd) throws IOException {
        try {
            ResponseListener l = (ResponseListener) executorObjects.get(cmd.getArg("responseId"));
            l.setResponse(Reflector.deserializeFromString(cmd.getArg("returns")));
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    /* Execute a method locally based on params provided by remote caller.
     *
     * @param cmd
     * @throws IOException
     */
    private void handleExecuteRemote(GameCommand cmd) throws IOException {
        String method = cmd.getArg("method");
        int numParams = cmd.getInt("numParams");
        Class [] paramsTypes = new Class[numParams];
        Object [] params = new Object[numParams];
        for (int i=0; i<numParams; i++) {
            String param = cmd.getArg("param" + i);
            Object o = Reflector.deserializeFromString(param);
            paramsTypes[i] = o.getClass();
            params[i] = o;
        }
        Object obj = executorObjects.get(cmd.getArg("target"));//getExecuteObject();
        try {
            Method m = methodMap.get(method);
            if (m == null) {
                m = obj.getClass().getDeclaredMethod(method, paramsTypes);
                methodMap.put(method, m);
            }
            Object result = m.invoke(obj, params);
            String id = cmd.getArg("responseId");
            if (id != null) {
                GameCommand resp = new GameCommand(REMOTE_RETURNS);
                resp.setArg("target", id);
                if (result != null)
                    resp.setArg("returns", Reflector.serializeObject(result));
                sendCommand(resp);
            }
        } catch (Exception e) {
            methodMap.remove(method);
            // search methods to see if there is a compatible match
            try {
                searchMethods(obj, method, paramsTypes, params);

            } catch (Exception ee) {
                throw new IOException(ee);
            }
        }
    }

    private final Map<String, Method> methodMap = new HashMap<>();

    private void searchMethods(Object execObj, String method, Class [] types, Object [] params) throws Exception {
        for (Method m : execObj.getClass().getDeclaredMethods()) {
            if (!m.getName().equals(method))
                continue;
            Class [] paramTypes = m.getParameterTypes();
            if (paramTypes.length != types.length)
                continue;
            boolean match = true;
            for (int i=0; i<paramTypes.length; i++) {
                if (!isCompatiblePrimitives(paramTypes[i], types[i]) && !Reflector.isSubclassOf(types[i], paramTypes[i])) {
                    match = false;
                    break;
                }
            }
            if (match) {
                m.invoke(execObj, params);
                methodMap.put(method, m);
                return;
            }
        }
        throw new Exception("Failed to match method '" + method + "'");
    }

    private final static Map<Class, List> primitiveCompatibilityMap = new HashMap<>();

    static {
        List c;
        primitiveCompatibilityMap.put(boolean.class, c= Arrays.asList(boolean.class, Boolean.class));
        primitiveCompatibilityMap.put(Boolean.class, c);
        primitiveCompatibilityMap.put(byte.class, c=Arrays.asList(byte.class, Byte.class));
        primitiveCompatibilityMap.put(Byte.class, c);
        primitiveCompatibilityMap.put(int.class, c=Arrays.asList(int.class, Integer.class, byte.class, Byte.class));
        primitiveCompatibilityMap.put(Integer.class, c);
        primitiveCompatibilityMap.put(long.class, c=Arrays.asList(int.class, Integer.class, byte.class, Byte.class, long.class, Long.class));
        primitiveCompatibilityMap.put(long.class, c);
        primitiveCompatibilityMap.put(float.class, c=Arrays.asList(int.class, Integer.class, byte.class, Byte.class, float.class, Float.class));
        primitiveCompatibilityMap.put(Float.class, c);
        primitiveCompatibilityMap.put(double.class, c=Arrays.asList(int.class, Integer.class, byte.class, Byte.class, float.class, Float.class, double.class, Double.class, long.class, Long.class));
        primitiveCompatibilityMap.put(Double.class, c);

    }

    private boolean isCompatiblePrimitives(Class a, Class b) {
        if (primitiveCompatibilityMap.containsKey(a))
            return primitiveCompatibilityMap.get(a).contains(b);
        return false;
    }


}
