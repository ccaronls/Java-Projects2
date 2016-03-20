package cc.game.soc.core;

import java.io.PrintStream;
import java.util.*;

import cc.lib.game.IVector2D;
import cc.lib.math.Vector2D;
import cc.lib.utils.Reflector;

public abstract class BotNode extends Reflector<BotNode> implements Comparable<BotNode> {

	static {
		addAllFields(BotNode.class);
	}
	
    private boolean optimal = false;
    private double value = 0;
    private int numProperties = 0;
    float chance = 1.0f;
    BotNode next;
    final Map<String, Double> properties = new TreeMap<String, Double>();
    
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
    	assert(value != Double.NEGATIVE_INFINITY);
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
    
    public IVector2D getBoardPosition(Board b) {
    	return Vector2D.ZERO;
    }

    public final double getValue() {
    	if (numProperties != properties.size()) {
    		value = 0;
    		for (Map.Entry<String, Double> e : properties.entrySet()) {
    			value += AITuning.getInstance().getScalingFactor(e.getKey()) * e.getValue();
    		}
    		numProperties = properties.size();
    	}
        return value;
    }
    
    public final void resetCache() {
    	numProperties = 0;
    }
    
    public final List<BotNode> getChildren() {
    	return children;
    }

    public final BotNode getParent() {
        return parent;
    }

    @Override
    public final int compareTo(BotNode arg) {
    	double value = getValue();
    	double argValue = arg.getValue();
    	
    	// descending order
    	if (value < argValue)
    		return 1;
    	
    	if (value > argValue)
    		return -1;
    	
    	return 0;
    }
    
    public final void addValue(String name, double value) {
    	name = name.replace(' ', '_').trim();
    	if (value != 0) {
    		properties.put(name, value*chance);
    	} else {
    		properties.remove(name);
    	}
    	numProperties = 0;
    }
    
    public final Set<String> getKeys() {
    	return properties.keySet();
    }
    
    public final Double getValue(String key) {
    	return properties.get(key);
    }
    
    public String debugDump() {
    	StringBuffer buf = new StringBuffer();
    	for (String key : properties.keySet()) {
    		buf.append(String.format("%-30s : %s\n", key, properties.get(key)));
    	}
    	buf.append("Value=" + getValue());
    	return buf.toString();
    }

	public void clear() {
		properties.clear();
		numProperties = 0;
		value = Double.NEGATIVE_INFINITY;
	}
}
