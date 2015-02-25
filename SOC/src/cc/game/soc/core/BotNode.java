package cc.game.soc.core;

import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import cc.lib.utils.Reflector;

public abstract class BotNode extends Reflector<BotNode> implements Comparable<BotNode> {

	static {
		addAllFields(BotNode.class);
	}
	
    private float value = -Float.MAX_VALUE;
    private boolean optimal = false;
    BotNode next;
    HashMap<String, Double> properties = new HashMap<String, Double>();
    
    List<BotNode> children = new LinkedList<BotNode>();
    BotNode parent = null;
    
    BotNode() {}

	public boolean isLeaf() {
        return children.size() == 0;
    }
    
    public boolean isOptimal() {
        return optimal;
    }
    
    public void setOptimal(boolean optimal) {
    	this.optimal = optimal;
    }
    
	public void printTree(PrintStream out) {
		printTreeR(out, "");
	}

    public String toString() {
    	String s = getDescription();
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

    public BotNode attach(BotNode child)
    {
    	children.add(child);
        child.parent = this;
        return child;
    }
    
    /*
     * Sort the children 
     */
    void sortChildren() { 
    	Collections.sort(children);
    }
/*
    BotNode findOptimalMoves() {
        BotNode node = findOptimalNodeR();
        node.optimal = true;
        return node;
    }

    private BotNode findOptimalNodeR() {
    	if (isLeaf())
    		return this;
    	
    	BotNode opt = null;
    	float max = -Float.MAX_VALUE;
    	
    	for (BotNode n : children) {
    		BotNode n2 = n.findOptimalNodeR();
    		if (opt == null || n2.value > max) {
    			max = n2.value;
    			opt = n2;
    		}
    	}
    	
    	return opt;
    }*/

    public abstract Object getData();
    
    public abstract String getDescription();

    public float getValue() {
        return value;
    }
    
    void setValue(float value) {
    	this.value = value;
    }

    public List<BotNode> getChildren() {
    	return children;
    }

    public BotNode getParent() {
        return parent;
    }

    @Override
    public int compareTo(BotNode arg0) {
        return arg0.value - value >= 0 ? 1 : -1; 
    }

}
