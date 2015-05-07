package cc.game.soc.core;

import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import cc.lib.utils.Reflector;

public abstract class BotNode extends Reflector<BotNode> implements Comparable<BotNode> {

	static {
		addAllFields(BotNode.class);
	}
	
    private boolean optimal = false;
    private double value = 0;
    private int numProperties = 0;
    BotNode next;
    final HashMap<String, Double> properties = new HashMap<String, Double>();
    
    List<BotNode> children = new LinkedList<BotNode>();
    BotNode parent = null;
    
    BotNode() {}

	public final boolean isLeaf() {
        return children.size() == 0;
    }
    
    public final boolean isOptimal() {
        return optimal;
    }
    
    public final void setOptimal(boolean optimal) {
    	this.optimal = optimal;
    }
    
	public final void printTree(PrintStream out) {
		printTreeR(out, "");
	}

    public final String toString() {
    	String s = getDescription();
    	double value = getValue();
    	if (getData() != null && value > -Float.MAX_VALUE) {
    		s += " (" + value + ")";
    	}
    	if (optimal)
    		s += " ************* OPTIMAL ***************";
    	return s;
    }
    
    private void printTreeR(PrintStream out, String indent)
    {
    	out.print(indent);
    	out.print(this);
        out.println();
        indent += "+-";
        for (BotNode child : children) {
        	child.printTreeR(out, indent);
        }
    }

    public final BotNode attach(BotNode child)
    {
    	children.add(child);
        child.parent = this;
        child.properties.putAll(properties);
        return child;
    }
    
    /*
     * Sort the children 
     */
    final void sortChildren() { 
    	Collections.sort(children);
    }

    public abstract Object getData();
    
    public abstract String getDescription();

    public final double getValue() {
    	if (numProperties != properties.size()) {
    		value = 0;
    		for (Entry<String, Double> e : properties.entrySet()) {
    			value += e.getValue();
    		}
    		numProperties = properties.size();
    	}
        return value;
    }
    
    public final List<BotNode> getChildren() {
    	return children;
    }

    public final BotNode getParent() {
        return parent;
    }

    @Override
    public final int compareTo(BotNode arg0) {
        return arg0.getValue() - getValue() >= 0 ? 1 : -1; 
    }

    public final void addValue(String name, double value) {
    	if (value != 0) {
    		properties.put(name, value);
    	}
    }
}
