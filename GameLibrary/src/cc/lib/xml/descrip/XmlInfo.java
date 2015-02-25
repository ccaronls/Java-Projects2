package cc.lib.xml.descrip;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

import cc.lib.xml.XmlElement;

/**
 * Used by XmlDecriptor to build up tables of info.
 * the resulting ionfo is used to generate a description.
 * 
 * @author ccaron
 *
 * package access only
 */
public class XmlInfo {

	private final static Integer ONE = new Integer(1); 
	
	class ValueCounter {
		// map strings to Integers to track occurance count
		private HashMap<String, Integer> values = new HashMap<String, Integer>();
		private int occurances = 0;
		
		// add a string to the set or increment is count
		void addValue(String value) {
			if (values.containsKey(value)) {
				Integer count = (Integer)values.get(value);
				values.put(value, new Integer(count.intValue()+1));
			} else {
				values.put(value, ONE);
			}
			occurances++;
		}
		
		// the the number of occurances of a string
		int getOccurances(String value) {
			return ((Integer)values.get(value)).intValue();
		}
		
		int getTotalOccurances() {
			return this.occurances;
		}
		
		// return string keys
		Set<String> getKeySet() {
			return values.keySet();
		}
		
		public String toString() {
			//return values.toString();
			StringBuffer buf = new StringBuffer();
			Iterator<String> it = values.keySet().iterator();
			while (it.hasNext()) {
				String key = (String)it.next();
				String val = values.get(key).toString();
				buf.append("value [" + key + "] X " + val + "\n");
			}
			return buf.toString();
		}
	}
	
	class ElemInfo {
		int occurances = 0;
		ValueCounter content = new ValueCounter();
		HashMap<String, ValueCounter> attribs = new HashMap<String, ValueCounter>();
		HashMap<String, Integer> children = new HashMap<String, Integer>();
		OptElem elem = null;
		
		void addAttrib(String name, String value) {
			if (attribs.containsKey(name)) {
				((ValueCounter)attribs.get(name)).addValue(value);
			} else {
				ValueCounter counter = new ValueCounter();
				counter.addValue(value);
				attribs.put(name, counter);
			}
		}
		
		int getOccurances(String childName) {
			return ((Integer)children.get(childName)).intValue();
		}
		
		void addChild(String name) {
			if (children.containsKey(name)) {
				Integer count = (Integer)children.get(name);
				children.put(name, new Integer(count.intValue()+1));
			} else {
				children.put(name, ONE);
			}
		}
		
		public String toString() {
			return "ElemInfo X " + occurances + "\ncontent : " + content + "\nattribs : " + attribs + "\nchildren : " + children;
		}
		
		public void setElem(OptElem elem) {
			this.elem = elem;
		}
		
		public OptElem getElem() {
			return this.elem;
		}
	}
	
	// map strings too elemInfo objects
	private HashMap<String, ElemInfo> elemMap = new LinkedHashMap<String, ElemInfo>();
	
	public Iterator<String> iterator() {
		return elemMap.keySet().iterator();
	}
	
	public ElemInfo getInfo(String key) {
		return (ElemInfo)elemMap.get(key);
	}
	
	public void evaluate(XmlElement elem) {
		evaluateR(elem, "");
	}
	
	public String toString() {
		StringBuffer buf = new StringBuffer();
		Iterator<String> it = elemMap.keySet().iterator();
		while (it.hasNext()) {
			String key = (String)it.next();
			String value = elemMap.get(key).toString();
			buf.append(key + " : " + value + "\n");
		}
		return buf.toString();
	}
	
	private void evaluateR(XmlElement elem, String prefix) {
		String elemId = prefix + elem.getName();
		ElemInfo info = null;
		if (elemMap.containsKey(elemId)) {
			info = (ElemInfo)elemMap.get(elemId);
		} else {
			info = new ElemInfo();
			elemMap.put(elemId, info);
		}
		
		info.occurances ++;
		
		if (elem.getContentText() != null) {
			info.content.addValue(elem.getContentText());
		}
		
		String [] attribs = elem.getAttributes();
		for (int i=0; i<attribs.length; i++) {
			info.addAttrib(attribs[i], elem.getAttribute(attribs[i]));
		}
		
		XmlElement cur = elem.getFirst();
		while (cur != null) {
			String newPrefix = prefix + elem.getName() + DELIM;
			info.addChild(newPrefix + cur.getName());
			evaluateR(cur, newPrefix);			
			cur = cur.getNextSibling();
		}
	}
	
	public static final String DELIM = "$$";
	
}
