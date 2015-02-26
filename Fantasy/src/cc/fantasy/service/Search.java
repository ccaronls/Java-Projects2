package cc.fantasy.service;

import java.io.Serializable;

/**
 * 
 * @author ccaron
 *
 * <pre>
 * Structure to hold search params
 * </pre>
 */
public class Search <T> implements Serializable {

    int offset = 0;
    int max = Integer.MAX_VALUE;
    boolean descending = false;
    T key;
    int total = Integer.MAX_VALUE; // set during a search with the total list size
    
    public Search() {
    	
    }
    
	public boolean isDescending() {
		return descending;
	}
	public void setDescending(boolean descending) {
		this.descending = descending;
	}
	public int getMax() {
		return max;
	}
	public void setMax(int max) {
		this.max = max;
	}
	public int getOffset() {
		return offset;
	}
	public void setOffset(int offset) {
		this.offset = offset;
	}
    public T getKey() {
        return key;
    }
    public void setKey(T key) {
        this.key = key;
    }
    public int getTotal() {
        return total;
    }
    public void setTotal(int total) {
        this.total = total;
    }   
	
}
