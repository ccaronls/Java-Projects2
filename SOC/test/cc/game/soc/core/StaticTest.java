package cc.game.soc.core;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.lib.game.Utils;

public class StaticTest extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}
	
	public void testDiceEvent() {
		assertEquals(DiceEvent.fromDieNum(1), DiceEvent.AdvanceBarbarianShip);
		assertEquals(DiceEvent.fromDieNum(2), DiceEvent.AdvanceBarbarianShip);
		assertEquals(DiceEvent.fromDieNum(3), DiceEvent.AdvanceBarbarianShip);
		assertEquals(DiceEvent.fromDieNum(4), DiceEvent.ScienceCard);
		assertEquals(DiceEvent.fromDieNum(5), DiceEvent.TradeCard);
		assertEquals(DiceEvent.fromDieNum(6), DiceEvent.PoliticsCard);
	}
	
	public void test1() {
		
		List<Card> cards = new ArrayList<Card>();
		for (DevelopmentCardType d : DevelopmentCardType.values())
			cards.add(new Card(d.getCardType(), 0, CardStatus.UNUSABLE));
		
		for (DevelopmentCardType d : DevelopmentCardType.values())
			cards.add(new Card(d.getCardType(), 0, CardStatus.USABLE));
		
		for (DevelopmentCardType d : DevelopmentCardType.values())
			cards.add(new Card(d.getCardType(), 0, CardStatus.USED));
		
		Utils.shuffle(cards);
		
		Collections.sort(cards);
		System.out.println(cards.toString().replace(", ", "\n"));
	}
	
	
	public void test() {
		Pattern splitter = Pattern.compile("[A-Z][a-z0-9]*");
		
		String [] words = {
				"A",
				"Abc",
				"AbcDef Ghi "
		};
		
		for (String word : words) {
		
        	Matcher matcher = splitter.matcher(word);
        	String txt = "";
        	while (matcher.find()) {
        		if (txt.length() > 0) {
        			txt += "-";
        		}
        		txt += matcher.group();
        	}
        	System.out.println(word + "->" + txt);
		}
	}

}
