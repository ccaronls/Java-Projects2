package cc.game.soc.ai;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import cc.game.soc.core.*;

public class AINode implements Comparable <AINode> {
	
    final NodeType type;
    int index = -1;
    Trade trade = null;
    float value = 0;
    boolean optimal = false;
    StringWriter debugString = new StringWriter();
    final PrintWriter debug = new PrintWriter(debugString);
    
    List<AINode> children = new LinkedList<AINode>();
    AINode parent = null;
    
    //public Object userData;
    
    public String toString() {
        if (type == NodeType.MOVE_CHOICE)
            return MoveType.values()[index].name();
        
        String str = type.name();// + " " + (trade == null ? index : trade);
        switch (type) {
			case GIVEUP_CARD:
				//str += " " + GiveUpCardOption.values()[index].getType().name();
				break;
			case RESOURCE_CHOICE:
				break;
			case MOVE_CHOICE:
				str += " " + MoveType.values()[index].name();
				break;
			case SHIP_MOVE_CHOICE:
			case SHIP_CHOICE:
			case ROAD_CHOICE:
				str += " edge=" + index;
				break;
			case ROBBER_CHOICE:
				str += " tile=" + index;
				break;
			case ROOT:
				break;
			case CITY_CHOICE:
			case SETTLEMENT_CHOICE:
				str += " vertex=" + index;
				break;
			case TAKE_PLAYER_CARD:
				str += " player=" + index;
				break;
			case TRADE_CHOICE:
				assert(trade != null);
				str += " " + trade;
				break;
        }
        if (value != 0) {
            str += " " + value;
        }
        if (optimal)
            str += " OPT";
        str += debug.toString();
        return str;
    }

    AINode() {
        this(NodeType.ROOT, -1);
    }
    
    private AINode(NodeType t, int index) {
    	type = t;
    	this.index=index;
    }

    private AINode(Trade t) {
    	type = NodeType.TRADE_CHOICE;
    	trade=t; 
    }

    public boolean isLeaf() {
        return children.size() == 0;
    }
    
    public boolean isOptimal() {
        return optimal;
    }
    
    public NodeType getType() {
        return this.type;
    }
    
    void printNode(SOC soc, String indent)
    {
        StringBuffer buf = new StringBuffer();
        buf.append(indent).append(type);
        switch (type) {
            case ROOT: 
                break;
            case MOVE_CHOICE: 
                buf.append(" ").append(MoveType.values()[index]);
                break;
			case SHIP_CHOICE:
			case SHIP_MOVE_CHOICE:
            case SETTLEMENT_CHOICE:
            case ROAD_CHOICE:
            case CITY_CHOICE:
            case ROBBER_CHOICE:
                buf.append(" index(").append(index).append(")");
                break;
            case TRADE_CHOICE:
                //log.debug(" %s X %d", getResourceTypeString(tree.trade.resource), tree.trade.amount);
            	buf.append(" ").append(trade.getType()).append(" X ").append(trade.getAmount());
                break;
            case RESOURCE_CHOICE:
                buf.append(" ").append(ResourceType.values()[index]);
                break;
			case GIVEUP_CARD:
				buf.append(" ").append(GiveUpCardOption.values()[index].getType().name());//DevelopmentCardType.values()[index]);
				break;
			case TAKE_PLAYER_CARD:
				buf.append(" player(").append(index).append(")");
				break;
        }
        
        if (value != 0) {
            buf.append(" ").append(value);
        }
        if (optimal)
            buf.append(" -- [OPTIMAL] --");
        //log.debug(buf);
        soc.logDebug(buf.toString());
        //if (debugString.getBuffer().length() > 0)
        	//soc.logDebug(debugString.getBuffer().toString());
    }

    void debugDumpTree(SOC soc) {
        if (parent != null)
            parent.debugDumpTree(soc);
        else
            debugDumpTreeR(soc, "");
    }
    
    private void debugDumpTreeR(SOC soc, String indent) {
        printNode(soc, indent);
        for (AINode n : children)
        	n.debugDumpTreeR(soc, indent + "+-");
        /*
        if (left != null)
        	left.debugDumpTreeR(soc, indent + "+-");
        if (next != null)
        	next.debugDumpTreeR(soc, indent);
    */
    }

    AINode attach(Trade trade) {
        AINode n = new AINode(trade);
        attach(n);
        return n;
    }
    
    AINode attach(NodeType type, int index) {
        AINode n = new AINode(type, index);
        attach(n);
        return n;
    }
    
    private void attach(AINode child)
    {
    	/*
        if (left == null) {
            left = right = child;
        } else {
            right.next = child;
            child.prev = right;
            right = child;
        }*/
    	children.add(child);
        child.parent = this;
    }
/*
    void detatch()
    {
        assert(parent != null);
        // if we are at the front, special case
        if (prev == null) {
            assert(parent.left == this);
            parent.left = next;
            if (next != null) {
                next.prev = null;
            } else {
                // case when we are only child
                parent.right = null;
            }
        } else if (next == null) {
            assert(parent.right == this);
            assert(parent.left != this); // must be more than one
            assert(prev != null);
            parent.right = prev;
            prev.next = null;
        } else {
            prev.next = next;
            next.prev = prev;
        }
        parent = null;
        next = null;
        prev = null;
        left = null;
        right = null;
    }*/
    
    /*
     * Sort the children 
     */
    void sortChildren() { 
    	/*
        if (left == right)
            return; // list is zero or has 1 element, so no snorting
        List<AINode> children = new ArrayList<AINode>();
        for (AINode n = left; n != null; n=n.next) {
            children.add(n);
        }
        Collections.sort(children);
        left = right = null;
        for (AINode n : children) {
            n.next = n.prev = null;
            attach(n);
        }*/
    	Collections.sort(children);
    }

    AINode findOptimalMoves() {
        AINode node = findOptimalNodeR();
        node.optimal = true;
        return node;
    }

    private AINode findOptimalNodeR() {
    	if (isLeaf())
    		return this;
    	
    	AINode opt = null;
    	float max = -Float.MAX_VALUE;
    	
    	for (AINode n : children) {
    		AINode n2 = n.findOptimalNodeR();
    		if (opt == null || n2.value > max) {
    			max = n2.value;
    			opt = n2;
    		}
    	}
    	
    	return opt;
    }

    @Override
    public boolean equals(Object arg0) {
        AINode o = (AINode)arg0;
        return this.type == o.type &&
               this.index == o.index &&
               isEqual(trade, o.trade);
    }
    
    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null)
            return true;
        if (a == null || b == null)
            return false;
        return a.equals(b);
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    /**
     * Used to walk the tree
     * @see walkTreePreOrder
     * @author ccaron
     *
     */
    public interface AINodeVisitor {
        void onVisit(AINode node, int depth);
    }
    
    public void walkTreeR(AINodeVisitor visitor, int depth) {
        visitor.onVisit(this, depth);
        depth++;
        for (AINode n : children)
        	n.walkTreeR(visitor, depth);
        /*
        if (left != null)
            left.walkTreeR(visitor, depth);
        depth--;
        if (next != null)
            next.walkTreeR(visitor, depth);*/
    }
    
    public void walkTree(AINodeVisitor visitor) {
        walkTreeR(visitor, 1);
    }
    
    public List<AINode> getPath() {
        LinkedList<AINode> list = new LinkedList<AINode>();
        if (type != NodeType.ROOT)
            list.addFirst(this);
        AINode cur = parent;
        while (cur != null && cur.type != NodeType.ROOT) {
            list.addFirst(cur);
            cur = cur.parent;
        }
        return list;
    }

    public int getIndex() {
        return index;
    }

    public Trade getTrade() {
        return trade;
    }

    public float getValue() {
        return value;
    }

    public List<AINode> getChildren() {
/*        List<AINode> l = new LinkedList<AINode>();
        for (AINode n=left; n!=null; n=n.next) {
            l.add(n);
        }
        return l;*/
    	return children;
    }

    public AINode getParent() {
        return parent;
    }

    @Override
    public int compareTo(AINode arg0) {
        return arg0.value - value >= 0 ? 1 : -1; 
    }
    
    
}
