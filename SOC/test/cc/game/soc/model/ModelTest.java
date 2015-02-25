package cc.game.soc.model;

import cc.game.soc.base.SOCTestBase;

public class ModelTest extends SOCTestBase {

	public void testTable() {
	
		String table = AModel.toTable(service.listGames(null, null));
		System.out.println(table);
		
	}
	
	
}
