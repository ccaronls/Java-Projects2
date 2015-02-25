package cc.game.soc.core;

import cc.lib.utils.Reflector;

/**
 * 
 * @author Chris Caron
 * 
 */
public final class DevelopmentCard extends Reflector<DevelopmentCard> {

	static {
		addAllFields(DevelopmentCard.class);
	}
	
    public final static int NOT_USABLE  = 1;
    public final static int USABLE      = 2;
    public final static int UNUSED      = (NOT_USABLE | USABLE);
    public final static int USED        = 4;
    
    public static String getFlagString(int flag) {
        switch (flag) {
        case NOT_USABLE: return "NOT_USABLE";
        case USABLE: return "USABLE";
        case UNUSED: return "UNUSED";
        case USED: return "USED";
        }
        return "INVALID[" + flag + "]";
    }

    
	private final DevelopmentCardType type;
	private int flag;

	@Override
    public String toString() {
        return type + "(" + getFlagString(flag) + ")";
    }
    
    /**
     * 
     */
    public DevelopmentCard() {
    	type = null;
    }
    
	/**
	 * 
	 * @param type
	 * @param flag
	 */
	DevelopmentCard(DevelopmentCardType type, int flag) {
		this.type = type;
		this.flag = flag;
	}

	/**
	 * @return Returns the flag.
	 */
	public boolean getFlag(int mask) {
		return (flag & mask) != 0;
	}
	
	/**
	 * 
	 * @return
	 */
	public int getFlag() {
	    return flag;
	}
	
	/**
	 * 
	 * @param flag
	 */
	public void setFlag(int flag) {
	    this.flag = flag;
	}

	/**
	 * @return Returns the type.
	 */
	public DevelopmentCardType getType() {
		return type;
	}

}
