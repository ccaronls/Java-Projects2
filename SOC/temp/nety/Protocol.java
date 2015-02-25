package cc.game.soc.nety;

import java.awt.Color;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cc.game.soc.core.*;

public class Protocol {

	static String VERSION = "1.0"; // changes in protocol should result in increase of version here
	
	/////////////////////////////////////////////////

	static int decodeInt(String data) throws ProtocolException {
		try {
			return Integer.parseInt(data);
		} catch (Exception e) {
			throw new ProtocolException("Failed to decode Integer from '" + data + "'", e);
		}
	}
	
	/////////////////////////////////////////////////

	private static String encodeList(List<?> list) {
        StringBuffer buf = new StringBuffer();
        Iterator<?> it = list.iterator();
        while (it.hasNext()) {
            buf.append(it.next().toString()).append(",");
        }
        return buf.toString();
	}

	@SuppressWarnings("unchecked")
    private static <T extends Enum> List<T> decodeEnumList(Class<T> clazz, String data) throws ProtocolException {
		try {
	        String [] parts = data.split(",");
	        List list = new ArrayList(); 
	        for (int i=0; i<parts.length; i++) {
	            list.add(Enum.valueOf(clazz, parts[i]));
	        }
	        return list;
		} catch (Exception e) {
			throw new ProtocolException("Failed to parse move list from '" + data + "'", e);
		}

	}
	
	/////////////////////////////////////////////////

	static String encodeMoveList(List<MoveType> moves) {
		return encodeList(moves);
	}
	
	static List<MoveType> decodeMoveList(String data) throws ProtocolException {
		return decodeEnumList(MoveType.class, data);
    }

	/////////////////////////////////////////////////

	static String encodeStringList(List<String> strings) {
		return encodeList(strings);
	}
	
	static List<String> decodeStringList(String data) throws ProtocolException {
		try {
			String [] parts = data.split("[,]");
			return Arrays.asList(parts);
		} catch (Exception e) {
			throw new ProtocolException("Failed to decode string list '" + data + "'", e);
		}
	}
	
	/////////////////////////////////////////////////

	static String encodeGiveUpCardInfo(int numToGiveUp, List<GiveUpCardOption> cards) {
		return String.valueOf(numToGiveUp) + ":" + encodeList(cards);
	}
	
    static int decodeGiveUpCardAmount(String data) throws ProtocolException {
        try {
            int colon = data.indexOf(':');
            return Integer.parseInt(data.substring(0, colon));
        } catch (Exception e) {
            throw new ProtocolException("Failed to parse GiveUpCardAmount from '" + data + "'", e);
        }
        
    }
    
	static List<GiveUpCardOption> decodeGiveUpCardList(String data) throws ProtocolException {
        try {
            int colon = data.indexOf(':');
            return decodeEnumList(GiveUpCardOption.class, data.substring(colon+1));
        } catch (ProtocolException e) {
            throw e;
        } catch (Exception e) {
            throw new ProtocolException("Failed to parse GiveUpCardList from '" + data + "'", e);
        }
		
	}
	
	/////////////////////////////////////////////////

    static String encodeColors(Map<String, Color> colors) {
    	StringBuffer buf = new StringBuffer();
    	
    	Iterator<String> it = colors.keySet().iterator();
    	String sep = "";
    	while (it.hasNext()) {
    		String name = it.next();
    		Color  color = colors.get(name);
    		buf.append(sep).append(name).append("[").append(color.getRed()).append(".")
    			.append(color.getGreen()).append(".").append(color.getBlue()).append("]");
    		sep = ",";
    	}
    	
    	return buf.toString();
    }

	static Map<String, Color> decodeColors(String data) throws ProtocolException {
		try {
			Map<String, Color> colors = new LinkedHashMap<String, Color>();
			String [] parts = data.split(",");
			for (int i=0; i<parts.length; i++) {
				int lbrack = parts[i].indexOf('[');
				int rbrack = parts[i].indexOf(']');
				String [] rgb = parts[i].substring(lbrack+1, rbrack).split("\\.");
				Color c = new Color(Integer.parseInt(rgb[0]), Integer.parseInt(rgb[1]), Integer.parseInt(rgb[2]));
				String txt = parts[i].substring(0, lbrack);
				colors.put(txt, c);
			}
			return colors;
		} catch (Exception e) {
			throw new ProtocolException("Failed to parse colro map from '" + data + "'", e);
		}
	}
	
	/////////////////////////////////////////////////
	
	static String encodeColor(Color color) {
    	return String.valueOf(color.getRed()) + "," + color.getGreen() + "," + color.getBlue();
	}
	
	static Color decodeColor(String data) throws ProtocolException {
		try {
			String [] rgb = data.split("[,]");
			Color color = new Color(Integer.parseInt(rgb[0]), Integer.parseInt(rgb[1]), Integer.parseInt(rgb[2]));
			return color;
		} catch (Exception e) {
			throw new ProtocolException("Failed to parse color from '" + data + "'", e);
		}
	}

	/////////////////////////////////////////////////

	public static String encodeBoard(SOCBoard board) throws IOException {
		StringWriter buf = new StringWriter();
		BufferedWriter out = new BufferedWriter(buf);
		//board.saveBoard(out);
		out.close();
		return buf.toString();
	}
	
	public static void decodeBoard(SOCBoard board, String data) throws ProtocolException {
		try {
			StringReader buf = new StringReader(data);
			BufferedReader in = new BufferedReader(buf);
			//board.loadBoard(in);
			in.close();
		} catch (Exception e) {
			throw new ProtocolException("Failed to decode board", e);
		}
	}
	
	/////////////////////////////////////////////////

	static String encodeResourceCount(ResourceType type, int count) {
		return type.name() + "," + count;
	}
	
	static void decodeResourceCount(SOCPlayer player, String data) throws ProtocolException {
        try {
			String [] parts = data.split(",");
			player.setResourceCount(ResourceType.valueOf(parts[0]), Integer.parseInt(parts[1]));
		} catch (Exception e) {
			throw new ProtocolException ("Failed to decode resource count form  '" + data + "'", e);
		}
	}
	
	/////////////////////////////////////////////////

	static String encodeDice(int die1, int die2) {
		return "" + die1 + "," + die2;
	}
	
	static void decodeDice(SOC soc, String data) throws ProtocolException {
		try {
			String [] parts = data.split(",");
			soc.setDice(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
		} catch (Exception e) {
			throw new ProtocolException("Failed to parse dice from '" + data + "'", e);
		}
	}
	
	/////////////////////////////////////////////////

	static String encodeDevelCard(DevelopmentCardType card, int flag, boolean add) {
		return card.name() + "," + flag + "," + add;
	}
	
	static void decodeDevelCard(SOCPlayer p, String data) throws ProtocolException {
		try {
			String [] parts = data.split("[,]");
			boolean add = Boolean.parseBoolean(parts[2]);
			DevelopmentCardType type = DevelopmentCardType.valueOf(parts[0]);
			int flag = Integer.parseInt(parts[1]);
			if (add) {
				p.addDevelopmentCard(type, flag);
			} else {
				p.removeDevelopmentCard(type, flag);
				
			}
		} catch (Exception e) {
			throw new ProtocolException("Failed to decoide development card from '" + data + "'", e);
		}
	}
	
	/////////////////////////////////////////////////

	static String encodeIntList(List<Integer> list) {
		return encodeList(list);
	}
	
	static List<Integer> decodeIntList(String data) throws ProtocolException {
		try {
			String [] parts = data.split("[,]");
			List<Integer> list = new ArrayList<Integer>();
			for (int i=0; i<parts.length; i++) {
				list.add(Integer.parseInt(parts[i]));
			}
			return list;
		} catch (Exception e) {
			throw new ProtocolException("Failed to parse int list from '" + data + "'", e);
		}
	}
	
	/////////////////////////////////////////////////

	static String encodeTrade(SOCTrade t, ResourceType r) {
		return t.getType().name() + "," + t.getAmount() + "," + r.name();
	}
	
	static SOCTrade decodeTrade(String data) throws ProtocolException {
		try {
			String [] parts = data.split("[,]");
			return new SOCTrade(ResourceType.valueOf(parts[0]), Integer.parseInt(parts[1]));
		} catch (Exception e) {
			throw new ProtocolException("Failed to parse Trade from '" + data + "'", e);
		}
	}

	static ResourceType decodeTradeResource(String data) throws ProtocolException {
		try {
			String [] parts = data.split("[,]");
			return ResourceType.valueOf(parts[2]);
		} catch (Exception e) {
			throw new ProtocolException("Failed to parse TradeResource from '" + data + "'", e);
		}
	}

	/////////////////////////////////////////////////

	/////////////////////////////////////////////////

	/////////////////////////////////////////////////

	/////////////////////////////////////////////////

	/////////////////////////////////////////////////

}
