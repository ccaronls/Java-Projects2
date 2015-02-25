package cc.lib.main;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import cc.lib.net.ClientConnection;
import cc.lib.net.GameCommand;
import cc.lib.net.GameServer;
import cc.lib.net.GameServer.Listener;
import cc.lib.utils.CommandLineParser;

/**
 * This class allows for management of a large number of connected clients and their associated
 * games.  Features like leader boards are automatic.
 * 
 * This system itself does not host any live games, instead it connects clients who want to play
 * in a client hosted game.  This system will be a communication relay  for those games where low
 * latency is not an issue.  For low latency games the clients will need their own custom connection.
 *  
 * @author ccaron
 *
 */
public class GameServerManagementConsole extends GameServer {

    GameServerManagementConsole(Listener serverListener, int listenPort, int clientReadTimeout, String serverVersion) throws Exception {
        super(serverListener, listenPort, clientReadTimeout, serverVersion);
    }
    
    Map<String, Game> games = Collections.synchronizedMap(new TreeMap<String, Game>());

    static void println(String msg, Object ... args) {
        System.out.println(String.format(msg, args));
    }
    
    static void print(String msg) {
        System.out.print(msg);
    }
    
    static GameServer.Listener gameServerListener = new GameServer.Listener() {

        @Override
        public void onConnected(ClientConnection conn) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void onReconnection(ClientConnection conn) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void onClientDisconnected(ClientConnection conn) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void onClientCommand(ClientConnection conn, GameCommand command) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void onFormSubmited(ClientConnection conn, int id, Map<String, String> params) {
            // TODO Auto-generated method stub
            
        }
        
    };
    
    // this is here as an example.  This needent be the only
    // way to use GameServer
    public static void main(String [] argv) {
        int clientReadTimeout = 5000;
        String version = "0";
        CommandLineParser parser = new CommandLineParser(
                "h\"help\"" +
                "t$i\"client timeout (default : " + clientReadTimeout + ")\"" +
                "v$s\"version (default : " + version + ")\""
                );
        try {
            String options = "i\"listen port\"";
            
            parser.parse(argv, options);
            if (parser.getParamSpecified('h')) {
                println(parser.getUsage(GameServer.class));
                System.exit(0);
            }

            //if (parser.getParamSpecified(c))
            
            final int listenPort = Integer.parseInt(parser.getArg(0));
            if (parser.getParamSpecified('t'))
                clientReadTimeout = Integer.parseInt(parser.getParamValue('t'));
            
            
            println("Instantiating GameServerConsole to listen on port " + listenPort);
            
            GameServerManagementConsole server = new GameServerManagementConsole( gameServerListener, listenPort, clientReadTimeout, version);
                    
                    //(GameServer)GameServerConsole.class.getClassLoader().loadClass(clazz).getConstructor(new Class []{ Integer.class }).newInstance(listenPort);
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            
            while (true) {
                print("> ");
                String line = reader.readLine();
                if (line == null)
                    continue;
                
                line = line.toLowerCase();
                String [] parts = line.split("[ ]+");
                
                if (line.startsWith("help")) {
                    println(
                       "COMMANDS:\n"
                       + "   help                 this message\n"
                       + "   quit                 shutdown and exit\n"
                       + "   listclients [filter] list all connected clients with an optional filter\n"
                       + "   listgames [filter]   list all games with an optional filter\n"
                       + "   viewclient <id>      view details on a specific client (id required)\n" 
                       + "   viewgame <id>        view details on a specific game (id required)\n"
                       );
                }
                
                else if (line.startsWith("quit")) {
                    server.stop();
                    System.exit(0);
                }
                
                else if (line.startsWith("listclients")) {
                    int i = 0;
                    println("CLIENTS:");
                    println("   %-20s  %-20s  %s", "Name", "Status", "Game");
                    println("   %-20s  %-20s  %s", "---------", "----------", "---------");
                    for (ClientConnection conn: server.getConnectionValues()) {
                        if (i++ > 50) {
                            println("   ... " + (server.getNumClients() - i) + " more");
                            break;
                        }
                        /*
                        Game game = (Game)conn.getAttribute("GAME");
                        println("   %-20s  %-20s  %s", 
                                conn.getName(), conn.getStatus(), game == null ? "" : game.getId()
                                );
                          */      
                    }
                }
                
                else if (line.startsWith("listgames")) {
                    int i = 0;
                    println("GAMES:");
                    println("   %-20s  %-20s  %s", "Name", "Status", "Host");
                    println("   %-20s  %-20s  %s", "---------", "----------", "---------");
                    for (Game game: server.games.values()) {
                        if (i++ > 50) {
                            println("   ... " + (server.games.size() - i) + " more");
                            break;
                        }
                        println("   %-20s  %-20s  %s", 
                                game.getId(), game.getStatus(), game.getHostClient()
                                );
                    }
                }
                
                else if (line.startsWith("viewgame")) {
                    
                }
                
                else if (line.startsWith("viewgame")) {
                    try {
                        String id = parts[1];
                        Game game = server.games.get(id);
                        if (game == null)
                            throw new Exception("Unknown game '" + id + "'");

                        int spacing = 10;
                        println(
                                "GAME\n"
                                + "   id        :" + game.getId()
                                );
                        /*
                        for (String key: game.details.keySet()) {
                            println("   %-" + spacing + "s  %s\n", key, game.details.get(key));
                        }*/
                    } catch (Exception e) {
                        String msg = e.getMessage();
                        if (msg == null || msg.length() == 0)
                            msg = e.getClass().getSimpleName();
                        println("ERROR: " + msg + " usage 'viewgame <id>'");
                    }
                } 
                
                else {
                    System.err.println("ERROR: Unknown command: " + line + "\n\n");
                }
            }
            
        } catch (Exception e) {
            //System.err.println(parser.getUsage(GameServer.class));
            System.err.println(e.getClass().getSimpleName() + " " + e.getMessage());
            System.exit(1);
        }
    }

}
