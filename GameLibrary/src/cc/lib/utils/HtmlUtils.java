package cc.lib.utils;

import java.util.ArrayList;
import java.util.List;

public class HtmlUtils {

	/**
	 * Wrap text on newline boundaries as well as keep the length below a maximum.
	 * Newlines will be stripped and spaces at the ends are stripeed.
	 * 
	 * @param txt
	 * @param maxChars
	 * @throws IllegalArgumentException of maxChars < 1
	 * @return
	 */
	public static String [] wrapText(String txt, int maxChars) {
		ArrayList<String> list = new ArrayList<String>();
		wrapTextR(txt.trim(), maxChars, list);
		return list.toArray(new String[list.size()]);
	}
	
	private static void wrapTextR(String txt, int maxChars, List<String> lines) {
		if (maxChars < 1)
			throw new IllegalArgumentException("Invalid value for maxChars '" + maxChars + "', value must be greater than 0");
		if (txt == null || txt.length() == 0)
			return;
		int endl = txt.indexOf('\n');
		if (endl > 0 && endl < maxChars) {
			lines.add(txt.substring(0, endl));
			txt = txt.substring(endl+1).trim();
		} else if (txt.length() > maxChars) {
			int spc = txt.indexOf(' ');
			while (spc > 0 && spc < maxChars) {
				int nxt = txt.indexOf(' ', spc+1);
				if (nxt < 0 || nxt > maxChars) {
					break;
				}
				spc = nxt;
			}
			if (spc > 0 && spc < maxChars) {
				lines.add(txt.substring(0, spc).trim());
				txt = txt.substring(spc+1).trim();
			} else {
				lines.add(txt.substring(0, maxChars).trim());
				txt = txt.substring(maxChars).trim();
			}
		} else {
			lines.add(txt);
			txt = null;
		}
		wrapTextR(txt, maxChars, lines);
	}
	
	public static String wrapTextForTD(String txt, int maxChars) {
	    if (txt == null)
	        return null;
		StringBuffer buf = new StringBuffer();
		String [] lines = HtmlUtils.wrapText(txt, maxChars);
		for (int ii=0; ii<lines.length; ii++) {
			buf.append(lines[ii]);
			if (ii < lines.length-1)
				buf.append("<br/>");
		}
		return buf.toString();
	}

}
