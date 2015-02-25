package cc.lib.xml.descrip;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

public class BasicType extends AValueType {

	private static LinkedHashMap<String, BasicType> instances = new LinkedHashMap<String, BasicType>();

	private Pattern pattern;
	
	private BasicType(String id, String pattern) {    	
    	super(id);
    	this.pattern = Pattern.compile(pattern);
    	if (instances.containsKey(id))
    		throw new RuntimeException("Duplicate BasicType [" + id + "]");
    	instances.put(id, this);
    } // static access only

	public BasicType() {
		// needed for serializable
	}
	
	/*
	 *  (non-Javadoc)
	 * @see cc.lib.xml.descrip.AValueType#validate(java.lang.String)
	 */
	public void validate(String value) throws Exception {
		if (!pattern.matcher(value).matches())
			throw new Exception("REG EX MISMATCH [" + value + "] too " + this);
	}
	
	/*
	 *  (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return super.toString() + pattern == null ? "" : ": REGEX { " + pattern.pattern() + " }";
	}
	
	/**
	 * 
	 * @param id
	 * @return
	 */
    public static AValueType getInstance(String id) {
    	return (AValueType)instances.get(id);
    }
    
    /**
     * Iterates over the keys
     * @return
     */
    public static Iterator<String> iterator() {
    	return instances.keySet().iterator();
    }
    
    // NOTE: Order is important here, these must be ordered in such a way
    // that the least restrictive type are constructed first.  In other words
    // entries that occur earlier should accept all entries that follow it.
    // when searching for a best fit type, we should choose the earliest.

    public final static AValueType NO_CHECK_TYPE 
    		= new BasicType("NO_CHECK_TYPE", ".*") ;

    public final static AValueType NON_EMPTY_STRING 
    		= new BasicType("NON_EMPTY_STRING", ".+") ;

    public final static AValueType FLOAT_VALUE 
    		= new BasicType("FLOAT_VALUE", 
    				"(0)|(-?(([1-9][0-9]*(\\.?[0-9]+)?)|(0\\.[0-9]+)))");

    public final static AValueType INT_VALUE 
    		= new BasicType("INT_VALUE", "(0)|(-?[1-9][0-9]*)") ;

    public final static AValueType EMPTY_VALUE
    		= new BasicType("EMPTY", "") {
    		public void validate(String value) throws Exception {
    			if (value != null && value.length()>0)
    				throw new Exception("Expected [" + this + "] got [" + value + "]");
    		}
    	};
}
