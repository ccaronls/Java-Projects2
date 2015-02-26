package cc.fantasy.struts.util;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import cc.fantasy.model.Franchise;
import cc.fantasy.model.LeagueStat;
import cc.fantasy.model.Player;
import cc.fantasy.model.Position;
import cc.fantasy.model.Stat;

public class Common {

    public static String makeStatsString(List<Stat> stats) {
        if (stats.size() == 0)
            return "";
        StringBuffer buf = new StringBuffer(stats.get(0).getName());
        int i = 1;
        for ( ; i<stats.size() && i<4; i++)
            buf.append(",").append(stats.get(i).getName());
        if (i<stats.size())
        	buf.append(",...");        	
        return buf.toString();
    }

    public static String makeLeagueStatsString(List<LeagueStat> stats) {
        if (stats.size() == 0)
            return "";
        StringBuffer buf = new StringBuffer();
        for (int i=0; i<stats.size(); i++)
        	if (stats.get(i).getMultiplier() != 0) {
        		if (buf.length() == 0)
        			buf.append(stats.get(i).getName());
        		else
        			buf.append(",").append(stats.get(i).getName());
        	}
        if (buf.length() > 20) {
        	return buf.substring(0, 20) + "...";
        }
        return buf.toString();
    }

    public static String makePlayerNameString(Player p) {
        String nm = "";
        if (p.getFirstName() != null && p.getFirstName().length() > 0)
            nm += p.getFirstName().charAt(0) + ". ";
        if (p.getLastName() != null)
            nm += p.getLastName();
        return nm;
    }
    
    public static String makePositionsString(Franchise f, List<String> positions) {
        if (positions.size() == 0)
            return "";
        Position p = f.getPosition(positions.get(0));
        StringBuffer buf = new StringBuffer(p.getName());
        int i = 1;
        for ( ; i<positions.size(); i++) {
            p = f.getPosition(positions.get(i));
            buf.append(",").append(p.getName());
        }     
        if (i<positions.size())
        	buf.append(",...");        	
        return buf.toString();
    }

    public static void safeClose(InputStream input) {
        try {
            input.close();
        } catch (Exception e) {}
    }
    
    public static Date stringToDate(String date, Locale locale) throws Exception {
    	if (date == null)
    		return null;
        return new SimpleDateFormat("MM-dd-yyyy", locale).parse(date);
    }

    public static String dateToString(Date date, Locale locale) {
    	if (date == null)
    		return null;
        SimpleDateFormat df = new SimpleDateFormat("MM-dd-yyyy", locale);
        return df.format(date);
    }

    public static boolean parseBoolean(String s) {
    	if (s == null)
    		throw new NullPointerException();
    	if (s.equalsIgnoreCase("true"))
    		return true;
    	if (s.equalsIgnoreCase("false"))
    		return false;
    	throw new NumberFormatException("Cannot convert '" + s + "' to boolean, must be [true/false]");
    }
}
