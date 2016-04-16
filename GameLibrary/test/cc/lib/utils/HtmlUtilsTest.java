package cc.lib.utils;

import junit.framework.TestCase;

public class HtmlUtilsTest extends TestCase {

	public void testWrapString() {
		String [] strings = {
				"This is a test of wrapping strings",
				"    This\n\n\n     is another    test of\nwrapping a very lonng string with lots of text\n\n\nand that is all       "
		};
		
		int [] maxLen = { 1, 5, 15, 30, 100 };
		for (String s : strings) {
			for (int m : maxLen) {
				String [] lines = HtmlUtils.wrapText(s, m);
				System.out.println("Wrapping string '" + s + "' of maxLength=" + m);
				for (int ii=0; ii<lines.length; ii++) {
					System.out.println("'" + lines[ii] + "'");
				}
			}
		}
	}
	
}
