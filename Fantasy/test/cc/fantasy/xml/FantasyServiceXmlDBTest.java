package cc.fantasy.xml;

import cc.fantasy.service.FantasyServiceTest;
import cc.fantasy.service.IFantasyService;
import cc.fantasy.xml.FantasyServiceXmlDB;

public class FantasyServiceXmlDBTest extends FantasyServiceTest {

    static IFantasyService service = new FantasyServiceXmlDB("testdata");

	@Override
	protected IFantasyService getService() {
		return service;
	}
}
