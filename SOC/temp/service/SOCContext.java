package cc.game.soc.service;

/**
 * 
 * @author ccaron
 *
 * Singleton
 */
public class SOCContext {

	private SOCContext() {}
	
	// TODO: Move this to a beans definition scheme
	private static SOCService service = new SOCServiceXml("db/xml");
	
	public static SOCService getService() {
		return service;
	}
	
}
