package cc.game.soc.netx;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import cc.game.soc.core.*;

class Command {

    final static String VERSION = "0.0.1";
    final static String MARKER  = "--SOCCMD--";

	enum Type {

		// commands that originate from the server
	    SRVR_PING_RESPONSE(false),
		SRVR_CONNECTED(false),
		SRVR_RECONNECTED(false),
		SRVR_DISCONNECTED(false),
		SRVR_ERROR(false),
		
		SRVR_NEWGAME(false),
		SRVR_ENDGAME(false),
		SRVR_FORM(true),
		SRVR_BOARD(false),
		
		SRVR_CHOOSE_SETTLEMENT_VERTEX(true),
		SRVR_CHOOSE_ROAD_EDGE(true),
		SRVR_CHOOSE_CITY_VERTEX(true), 
		SRVR_CHOOSE_ROBBER_CELL(true), 
		SRVR_CHOOSE_MOVE(true), 
		SRVR_ROLL_DICE(true), 
		SRVR_CHOOSE_PLAYER_TO_TAKE_CARD_FROM(true), 
		SRVR_CHOOSE_CARD_TO_GIVE_UP(true), 
		SRVR_CHOOSE_RESOURCE(true),
		SRVR_CHOOSE_TRADE_OPTIONS(true),
		
		// commands that originate from the client
		CL_PING(false),
		CL_CONNECT(false),
		CL_RECONNECT(false),
		CL_DISCONNECT(false),
		CL_FORMSUBMIT(false),
		
		CL_SET_SETTLEMENT_VERTEX(false),
		CL_SET_ROAD_EDGE(false),
		CL_SET_CITY_VERTEX(false),
		CL_SET_ROBBER_CELL(false),
		CL_SET_MOVE(false),
		CL_SPIN_DICE(false),
		CL_SET_PLAYER_TO_TAKE_CARD_FROM(false),
		CL_SET_CARD_TO_GIVE_UP(false),
		CL_SET_RESOURCE(false),
		CL_SET_TRADE(false)
		
		;

	    private final boolean blocking;

	    Type(boolean blocking) {
	        this.blocking = blocking;
	    }
		
        public boolean isBlocking() {
            return blocking;
        }
	}
	
	private Type type;
	private Map<String, String> params;
	
	private Command(Type type) {
		this(type, new HashMap<String, String>());
	}

    private Command(Type type, Map<String, String> params) {
        this.type = type;
        this.params = params;
    }

    Type getType() {
        return type;
    }
    
	private Command addString(String key, Object value) {
	    if (value == null)
	        params.remove(key);
	    else
	        params.put(key, value.toString());
		return this;
	}

	private Integer getInt(String key) {
		return Integer.parseInt(params.get(key));
	}
	
	private String getString(String key) {
		return params.get(key);
	}
	
    void write(DataOutputStream out) throws Exception {
        out.writeUTF(MARKER);
        out.writeUTF(type.name());
        out.writeShort(params.size());
        for (Entry<String, String> e: params.entrySet()) {
            out.writeUTF(e.getKey());
            out.writeUTF(e.getValue());
        }
    }
    
    public String toString() {
        return type.name() + ":" + params;
    }

    private List<Integer> getIntList(String string) {
		ArrayList<Integer> l = new ArrayList<Integer>();
		String [] parts = string.substring(1, string.length()-1).split("[, ]");
		for (String s : parts) {
			l.add(Integer.parseInt(s));
		}
		return l;
	}

    private List<SOCTrade> getTradeList(String string) {
        List<SOCTrade> l = new ArrayList<SOCTrade>();
        String [] parts = string.substring(1, string.length()-1).split("[, ]");
        for (int i=0; i<parts.length; i++) {
            l.add(new SOCTrade(parts[0]));
        }
        return l;
    }

	@SuppressWarnings({ "unchecked" })
	private <T extends Enum<T>> List getEnumList(Class<T> enumCls, String string) {
		ArrayList l = new ArrayList();
		String [] parts = string.substring(1, string.length()-1).split("[, ]");
		for (String s : parts) {
			l.add(Enum.valueOf(enumCls, s));
		}		
		return l;
	}

	static Command read(DataInputStream in) throws IOException {
        String s = in.readUTF();
        while (!s.equals(MARKER))
            s = in.readUTF();
        Type type = Type.valueOf(in.readUTF());
        int num = in.readShort();
        Map<String, String> params = new HashMap<String, String>();
        for (int i=0; i<num; i++) {
            params.put(in.readUTF(), in.readUTF());
        }
        return new Command(type, params);
	}

	// PROTOCOL ///////////////////////////////////////////////////////////////////
	
	static Command newSrvrChooseSettlementVertex(List<Integer> vertexIndices) {
		return new Command(Type.SRVR_CHOOSE_SETTLEMENT_VERTEX).addString("indices", vertexIndices);
	}
	static List<Integer> parseSrvrChooseSettlementVertex(Command cmd) {
	    return cmd.getIntList("indices");
	}	
	// -------------------------------------
	static Command newSrvrChooseRoadEdge(List<Integer> edgeIndices) {
		return new Command(Type.SRVR_CHOOSE_ROAD_EDGE).addString("indices", edgeIndices);
	}
	static List<Integer> parseSrvrChooseRoadEdge(Command cmd) {
	    return cmd.getIntList("indices");
	}
    // -------------------------------------
	static Command newSrvrChooseCityVertex(List<Integer> vertexIndices) {
		return new Command(Type.SRVR_CHOOSE_CITY_VERTEX).addString("indices", vertexIndices);
	}
	static List<Integer> parseSrvrChooseCityVertex(Command cmd) {
	    return cmd.getIntList("indices");
	}
    // -------------------------------------
	static Command newSrvrChooseRobberCell(List<Integer> cellIndices) {
		return new Command(Type.SRVR_CHOOSE_ROBBER_CELL).addString("indices", cellIndices);
	}
	static List<Integer> parseSrvrChooseRobberCell(Command cmd) {
	    return cmd.getIntList("indices");
	}
    // -------------------------------------
	static Command newSrvrChooseMove(List<MoveType> moves) {
		return new Command(Type.SRVR_CHOOSE_MOVE).addString("moves", moves);
	}
	@SuppressWarnings("unchecked")
    static List<MoveType> parseSrvrChooseMove(Command cmd) {
	    return cmd.getEnumList(MoveType.class, "moves");
	}
    // -------------------------------------
	static Command newSrvrRollDice() {
		return new Command(Type.SRVR_ROLL_DICE);
	}

    // -------------------------------------
	static Command newSrvrChooseTradeOption(List<SOCTrade> trades) {
		return new Command(Type.SRVR_CHOOSE_TRADE_OPTIONS).addString("options", trades);
	}
	static List<SOCTrade> parseSrvrChooseTradeOption(Command cmd) {
	    return cmd.getTradeList("options");
	}
    // -------------------------------------
	static Command newSrvrChoosePlayerToTakeCardFrom(List<SOCPlayer> playerOptions) {
		List<Integer> playerIndices = new ArrayList<Integer>();
		for (SOCPlayer p : playerOptions)
			playerIndices.add(p.getPlayerNum());
		return new Command(Type.SRVR_CHOOSE_PLAYER_TO_TAKE_CARD_FROM).addString("indices", playerIndices);
	}
	static List<SOCPlayer> parseSrvrChoosePlayerToTakeCardFrom(SOC soc, Command cmd) {
	    List<Integer> playerNumList = cmd.getIntList("indices");
	    List<SOCPlayer> players = new ArrayList<SOCPlayer>();
	    for (int num : playerNumList) {
	        players.add(soc.getPlayerByPlayerNum(num));
	    }
	    return players;
	}
    // -------------------------------------
	static Command newSrvrChooseCardToGiveUp(List<GiveUpCardOption> options) {
		return new Command(Type.SRVR_CHOOSE_CARD_TO_GIVE_UP).addString("options", options);
	}
	@SuppressWarnings("unchecked")
    static List<GiveUpCardOption> parseSrvrChooseCardToGiveUp(Command cmd) {
	    return cmd.getEnumList(GiveUpCardOption.class, cmd.getString("options"));
	}
    // -------------------------------------
	static Command newSrvrChooseResource() {
		return new Command(Type.SRVR_CHOOSE_RESOURCE);
	}

    // -------------------------------------
	static Command newClSetSettlementVertex(int vertexIndex) {
	    return new Command(Type.CL_SET_SETTLEMENT_VERTEX).addString("index", vertexIndex);
	}
    static int parseClSetSettlementVertex(Command cmd) {
        return cmd.getInt("index");
    }
    // -------------------------------------
	static Command newClSetRoadEdge(int edgeIndex) {
	    return new Command(Type.CL_SET_ROAD_EDGE).addString("index", edgeIndex);
	}
    static int parseClSetRoadEdge(Command cmd) {
        return cmd.getInt("index");
    }
    // -------------------------------------
	static Command newClSetCityVertex(int vertexIndex) {
	    return new Command(Type.CL_SET_CITY_VERTEX).addString("index", vertexIndex);
	}
    static int parseClSetCityVertex(Command cmd) {
        return cmd.getInt("index");
    }
    // -------------------------------------
	static Command newClChooseRobberCell(int cellIndex) {
	    return new Command(Type.CL_SET_ROBBER_CELL).addString("index", cellIndex);
	}
	static int parseClChooseRobberCell(Command cmd) {
	    return cmd.getInt("index");
	}
    // -------------------------------------
	static Command newClChooseMove(MoveType chooseMove) {
	    return new Command(Type.CL_SET_MOVE).addString("move", chooseMove.name());
	}
	static MoveType parseClChooseMove(Command cmd) {
	    return MoveType.valueOf(cmd.getString("move"));
	}
    // -------------------------------------
	static Command newClRollDice(boolean rollDice) {
	    return new Command(Type.CL_SPIN_DICE);
	}

    // -------------------------------------
	static Command newClSetPlayertoTakeCardFrom(SOCPlayer player) {
	    return new Command(Type.CL_SET_PLAYER_TO_TAKE_CARD_FROM).addString("playerNum", player.getPlayerNum()); 
	}
	static SOCPlayer parseClSetPlayerToTakeCardFrom(SOC soc, Command cmd) {
	    return soc.getPlayerByPlayerNum(cmd.getInt("playerNum"));
	}
    // -------------------------------------
	static Command newClSetCardToGiveUp(GiveUpCardOption card) {
	    return new Command(Type.CL_SET_CARD_TO_GIVE_UP).addString("card", card.name());
	}
	static GiveUpCardOption parseClSetCardToGiveUp(Command cmd) {
	    return GiveUpCardOption.valueOf(cmd.getString("card"));
	}
    // -------------------------------------
	static Command newClSetResource(ResourceType chooseResource) {
	    return new Command(Type.CL_SET_RESOURCE).addString("resource", chooseResource.name());
	}
	static ResourceType parseClSetResource(Command cmd) {
	    return ResourceType.valueOf(cmd.getString("resource"));
	}
    // -------------------------------------
	static Command newClConnect() {
	    return new Command(Type.CL_CONNECT);
	}
	
    // -------------------------------------
    static Command newClReconnect(String clientName) {
        return new Command(Type.CL_RECONNECT).addString("clientName", clientName);
    }
    static String parseClReconnect(Command cmd) {
        return cmd.getString("clientName");
    }
    // -------------------------------------
    static Command newClDisconnect() {
        return new Command(Type.CL_DISCONNECT);
    }

    // -------------------------------------
    static Command newSrvrForm(ServerForm form) {
        return new Command(Type.SRVR_FORM).addString("id", form.getId()).addString("xml", form.toXML());
    }
    static ClientForm parseSrvrForm(Command cmd, NetClient client) throws Exception {
        ClientForm form = new ClientForm(cmd.getInt("id"), client);
        form.fromXML(cmd.getString("xml"));
        return form;
    }
    // -------------------------------------
    private static void initClFormR(FormElem elem, Command cmd) {
        switch (elem.type) {
            case VLISTCONTAINER:
            case HLISTCONTAINER:
            case BUTTONOPTION:
            case LABEL:
            case SUBMITBUTTON:
                break;
            case CHOICEBUTTON:
                cmd.addString(elem.id, elem.text);
                break;
            case TOGGLEBUTTON:
                cmd.addString(elem.id, elem.enabled ? "true" : "false");
                break;
            case TEXTINPUT:
                cmd.addString(elem.id, elem.text);
                break;
        }
        for (FormElem e: elem.getChildren()) {
            initClFormR(e, cmd);
        }
    }
    
    // -------------------------------------
    static Command newClFormSubmit(ClientForm form) {
        Command cmd = new Command(Type.CL_FORMSUBMIT);
        initClFormR(form.rootElem, cmd);
        cmd.addString("__formAction", form.action);
        cmd.addString("__formId", form.id);
        return cmd;
    }
    static FormResponse parseClFormSubmit(Command cmd) {
        int id = Integer.parseInt(cmd.getString("__formId"));
        String action = cmd.getString("__formAction");
        cmd.params.remove("__formId");
        cmd.params.remove("__formAction");
        return new FormResponse(id, action, cmd.params);

    }
    // -------------------------------------
    static Command newSrvrConnected(String clientName) {
        return new Command(Type.SRVR_CONNECTED).addString("clientName", clientName);
    }
    static String parseSrvrConnected(Command cmd) {
        return cmd.getString("clientName");
    }
    // -------------------------------------
    static Command newSrvrReconnected() {
        return new Command(Type.SRVR_RECONNECTED);
    }

    // -------------------------------------
    static Command newSrvrError(String string) {
        return new Command(Type.SRVR_ERROR).addString("error", string);
    }
    static String parseSrvrError(Command cmd) {
        return cmd.getString("error");
    }
    // -------------------------------------
    static Command newSrvrDisconnected(String reason) {
        return new Command(Type.SRVR_DISCONNECTED).addString("reason", reason);
    }
    static String parseSrvrDisconnected(Command cmd) {
        return cmd.getString("reason");
    }
    // -------------------------------------
    static Command newClSetTrade(SOCTrade t) {
        return new Command(Type.CL_SET_TRADE).addString("type", t.getType().name()).addString("amount", t.getAmount());
    }
    static SOCTrade parseClSetTrade(Command cmd) {
        return new SOCTrade(ResourceType.valueOf(cmd.getString("type")), cmd.getInt("amount"));
    }
    // -------------------------------------
    static Command newUpdateBoard(SOCBoard board) throws IOException {
        StringWriter string = new StringWriter();
        BufferedWriter writer = new BufferedWriter(string);
        try {
            //board.saveBoard(writer);
        } finally {
            try {
                writer.close();
            } catch (Exception e) {}
        }
        return new Command(Type.SRVR_BOARD).addString("board", string.getBuffer().toString());
    }
    static void parseUpdateBoard(SOCBoard board, Command cmd) throws IOException {
        StringReader string = new StringReader(cmd.getString("board"));
        BufferedReader reader = new BufferedReader(string);
        try {
            //board.loadBoard(reader);
        } finally {
            try {
                reader.close();
            } catch (Exception e) {}
        }
    }
    // -------------------------------------

    static Command newClPing() {
        return new Command(Type.CL_PING);
    }

    static Command newSrvrPingResponse() {
        return new Command(Type.SRVR_PING_RESPONSE);
    }

}
