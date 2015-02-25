package cc.game.soc.nety;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import cc.game.soc.core.SOCBoard;
import cc.lib.xml.XmlElement;
import cc.lib.xml.XmlParser;
import cc.lib.xml.descrip.XmlDescriptor;

/**
 * Simple server example that accepts client connections and starts a game once at
 * least 1 player has joined and a timeout has expired for other players to join.
 * 
 * Players should eb able to rejoin if they are dropped.
 * 
 * @author ccaron
 *
 * <pre>
 * TODO: Class Description
 * </pre>
 */
public class StandaloneServer implements Runnable {

    private final static Logger log = Logger.getLogger(StandaloneServer.class);

    public static void main(String [] args) {
        String xmlFile = "server.xml";
        if (args.length > 0)
            xmlFile = args[0];

        
        XmlElement root = null;
        try {
            root = XmlParser.parseXml(new File(xmlFile));
            log.debug(root.toStringDeep());
            XmlDescriptor descriptor = null;
            File descFile = new File("xmlserverdesc.bin");
            try {
            	descriptor = new XmlDescriptor(descFile);
            	descriptor.validate(root);
            } catch (FileNotFoundException e) {
            	log.warn("Description file not found, regenerating ...");
            	// create the descriptor file if not present
            	descriptor = new XmlDescriptor(root, true);
                log.debug("Created descriptor:\n" + descriptor.toString());
                descriptor.save(new File("xmlserverdesc.bin"));
            }
            new Thread(new StandaloneServer(root)).start();    
        } catch (Exception e) {
            log.error("Failed to load xml from '" + xmlFile + "'\n"
            		+ "ERROR: " + e.getClass() + ":" + e.getMessage() + "\n"
                    + "Usage: StandaloneServer [propsFile (default:server.xml)]\n");
            e.printStackTrace();
        } 
        
    }
	
	SOCServer server;
	int numPlayers = 0;
	SOCBoard board = new SOCBoard();
	
    @SuppressWarnings("unchecked")
    StandaloneServer(XmlElement root) throws Exception {
        List<XmlElement> elems = root.getElementsByName("server");
        XmlElement game = elems.get(0);
        numPlayers = Integer.parseInt(game.getAttribute("numPlayers"));
        int inactivityTimeoutSeconds = Integer.parseInt(game.getAttribute("inactivityTimeout"));
        int joinTimeout = Integer.parseInt(game.getAttribute("joinTimeout"));
        String boardName = game.getAttribute("board");
        board.load(boardName);
        // parse the colors
        elems = root.getElementsByName("colors");
        List<XmlElement> colors = elems.get(0).getElementsByName("color");
        if (colors.size() < numPlayers) {
            throw new Exception("numPlayers cannot excede number of specified colors");
        }
        elems = root.getElementsByName("client");
        XmlElement client = elems.get(0);
        int pingFreq = Integer.parseInt(client.getAttribute("pingFreq"));        
        server  = new SOCServer(numPlayers, inactivityTimeoutSeconds, joinTimeout, pingFreq);
        server.setBoard(board);
        Iterator <XmlElement> it = colors.iterator();
        while (it.hasNext()) {
            XmlElement color = it.next();
            String name = color.getAttribute("name");
            int r = Integer.parseInt(color.getAttribute("r"));
            int g = Integer.parseInt(color.getAttribute("g"));
            int b = Integer.parseInt(color.getAttribute("b"));
            server.addAvailableColor(name, new Color(r,g,b));                    
        }
        new Thread(server).start();
    }
    
	StandaloneServer(int numPlayers, int inactivityTimeoutSeconds, int joinTimeout, int pingFreq) throws Exception {
		board.load("soc_def_board.txt");
		this.numPlayers = numPlayers;
		server  = new SOCServer(numPlayers, inactivityTimeoutSeconds, joinTimeout, pingFreq);
		server.addAvailableColor("White", Color.WHITE);
		server.addAvailableColor("Gray", Color.GRAY);
		server.addAvailableColor("Green", Color.GREEN);
		server.addAvailableColor("Red", Color.RED);
		server.addAvailableColor("Purple", Color.MAGENTA);
		server.addAvailableColor("Pink", Color.PINK);
		new Thread(server).start();
	}
	
	public void run()
	{
        try {
            while (true) {
                log.info("Starting new game");
                server.reset();
                while (server.isRunning() && server.isWaitingForPlayersToJoin()) {
                    server.waitForConection();
                    log.debug("Waking up");
                }
    
                log.info("all Players present, Initializing game ...");
                server.initGame();

                log.debug("Waiting on player confirmation ...");
                while (server.isRunning() && server.isWaitingForPlayerConfirmation()) {
                    server.waitForConfirmation();
                }
                log.info("All players confirmed");

                log.info("Running game ...");
        		while (server.isGameRunning()) {
        			server.runGame();
                    Thread.sleep(500);
        		}
                
                server.reset();
            }
        } catch (Exception e) {  
            e.printStackTrace();
            log.error(e.getMessage());
            log.info("Server no longer running, exiting");
            System.exit(1);
        }
            
	}
	
}
