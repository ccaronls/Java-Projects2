package cc.game.soc.service;

public class Search {

    int offset = 0;
    int max = Integer.MAX_VALUE;
    String orderByField = null; // sort using an object field
    String orderByMethod = null; // sort using a method of the form 'Comparable getSomething(IFantasyService)' 
    boolean descending = false;
    int total; // set by parser and represents the total count for a search
    
    public Search() {
    	
    }
    
	public Search(int offset, int max, boolean descending) {
		this.offset = offset;
		this.max = max;
		this.descending = descending;
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
	public String getOrderByField() {
		return orderByField;
	}
	public void setOrderByField(String orderByField) {
		this.orderByField = orderByField;
	}
	public String getOrderByMethod() {
		return orderByMethod;
	}
	public void setOrderByMethod(String orderByMethod) {
		this.orderByMethod = orderByMethod;
	}
	public int getTotal() {
		return total;
	}
	void setTotal(int total) {
		this.total = total;
	}
    
	

}
