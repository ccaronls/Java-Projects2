package cc.game.soc.core;

import java.io.PrintStream;
import java.util.*;

import cc.lib.game.IVector2D;
import cc.lib.game.Utils;
import cc.lib.math.Vector2D;
import cc.lib.utils.Reflector;

public abstract class BotNode implements Comparable<BotNode> {

    private boolean optimal = false;
    private double value = 0;
    private int numProperties = 0;
    float chance = 1.0f;
    BotNode next;
    final Map<String, Double> properties = new TreeMap<String, Double>();
    MoveType strategy = MoveType.CONTINUE; // TODO

    List<BotNode> children = new LinkedList<BotNode>();
    BotNode parent = null;
    
    BotNode() {}

    /**
     *
     * @return
     */
	public final boolean isLeaf() {
        return children.size() == 0;
    }

    /**
     *
     * @return
     */
    public final boolean isOptimal() {
        return optimal;
    }

    /**
     *
     * @param optimal
     */
    public final void setOptimal(boolean optimal) {
    	this.optimal = optimal;
    }

    /**
     *
     * @param out
     */
	public final void printTree(PrintStream out) {
		printTreeR(out, "");
	}

	@Override
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

    /**
     *
     * @param child
     * @return
     */
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

    /**
     *
     * @return
     */
    public abstract Object getData();

    /**
     *
     * @return
     */
    public abstract String getDescription();

    /**
     *
     * @param b
     * @return
     */
    public IVector2D getBoardPosition(Board b) {
    	return Vector2D.ZERO;
    }

    /**
     *
     * @return
     */
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

    /**
     *
     */
    public final void resetCache() {
    	numProperties = 0;
    }

    /**
     *
     * @return
     */
    public final List<BotNode> getChildren() {
    	return children;
    }

    /**
     *
     * @return
     */
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

    /**
     *
     * @param name
     * @param value
     */
    public final void addValue(String name, double value) {
    	name = name.replace(' ', '_').trim();
    	if (value != 0) {
    		properties.put(name, value*Utils.clamp(chance, 0, 1));
    	} else {
    		properties.remove(name);
    	}
    	numProperties = 0;
    }

    /**
     *
     * @return
     */
    public final Set<String> getKeys() {
    	return properties.keySet();
    }

    /**
     *
     * @param key
     * @return
     */
    public final Double getValue(String key) {
    	return properties.get(key);
    }

    /**
     *
     * @return
     */
    public final String debugDump() {
    	StringBuffer buf = new StringBuffer();
    	for (String key : properties.keySet()) {
    		buf.append(String.format("%-30s : %s\n", key, properties.get(key)));
    	}
    	buf.append("Value=" + getValue());
    	return buf.toString();
    }

    /**
     *
     */
	public final void clear() {
		properties.clear();
		numProperties = 0;
		value = Double.NEGATIVE_INFINITY;
	}

}
