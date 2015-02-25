package cc.game.soc.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import cc.game.soc.core.*;

public class AINode implements Comparable <AINode> {
	
    final NodeType type;
    int index = -1;
    SOCTrade trade = null;
    float value = 0;
    boolean optimal = false;
    
    AINode left = null;
    AINode right = null;
    AINode next = null;
    AINode prev = null;
    AINode parent = null;
    
    //public Object userData;
    
    public String toString() {
        return type.name() + " " + (trade == null ? index : trade) + " " + value + " " + optimal;
    }

    AINode() {
        this(NodeType.ROOT, -1);
    }
    
    private AINode(NodeType t, int index) {
    	type = t;
    	this.index=index;
    }

    private AINode(SOCTrade t) {
    	type = NodeType.TRADE_CHOICE;
    	trade=t; 
    }

    public boolean isLeaf() {
        return left == null;
    }
    
    public boolean isOptimal() {
        return optimal;
    }
    
    public NodeType getType() {
        return this.type;
    }
    /*
    // TODO: Move to a Helper
    static String [] indent = null; 
    
    static String getIndent(int d) {
    	if (indent == null) {
    	    indent = new String[32];
    		String s = "";
    		for (int i=0; i<32; i++) {
    			indent[i] = s;
    			s += "  ";
    		}
    	}
    	return indent[d];
    }*/

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
        }
        
        if (value != 0) {
            buf.append(" ").append(value);
            //assert(isLeaf());
        }
        if (optimal)
            buf.append(" -- [OPTIMAL] --");
        //log.debug(buf);
        soc.logDebug(buf.toString());
    }

    void debugDumpTree(SOC soc) {
        if (parent != null)
            parent.debugDumpTree(soc);
        else
            debugDumpTreeR(soc, "");
    }
    
    private void debugDumpTreeR(SOC soc, String indent) {
        printNode(soc, indent);
        if (left != null)
        	left.debugDumpTreeR(soc, indent + "+-");
        if (next != null)
        	next.debugDumpTreeR(soc, indent);
    }

    AINode attach(SOCTrade trade) {
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
        if (left == null) {
            left = right = child;
        } else {
            right.next = child;
            child.prev = right;
            right = child;
        }
        child.parent = this;
    }

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
    }
    
    /*
     * Sort the children 
     */
    void sortChildren() { 
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
        }
    }
    
    /*
    void sortChildrenOptimized() {
        if (left == right)
            return;
        ChildList list = new ChildList(this);
        Collections.sort(list);
    }
    
    // possible optimization:
    private class ChildList extends AbstractSequentialList<AINode> {

        final AINode root;
        final int numChildren;
        ChildList(AINode root) {
            this.root = root;
            int num = 0;
            for (AINode c=root; c!=null; c=c.next) {
                num++;
            }
            numChildren = num;
        }
        
        class MyListIterator implements ListIterator<AINode>
        {

            AINode current;
            int index;
            MyListIterator(AINode current, int index) {
                this.current = current;
                this.index = index;
            }
            
            @Override
            public void add(AINode n) {
                assert(current != null);
                n.next = current;
                n.prev = current.prev;
                current.prev = n;
                if (n.prev == null) {
                    // insert at front
                    n.parent.left = n;
                } else {
                    n.prev.next = n;
                }
            }

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public boolean hasPrevious() {
                return current.prev != null;
            }

            @Override
            public AINode next() {
                AINode n = current;
                current = current.next;
                return n;
            }

            @Override
            public int nextIndex() {
                return index+1;
            }

            @Override
            public AINode previous() {
                return current.prev;
            }

            @Override
            public int previousIndex() {
                return index-1;
            }

            @Override
            public void remove() {
                assert(current != null);
                assert(current.prev != null);
                current.prev.detatch();
            }

            @Override
            public void set(AINode n) {
                assert(current != null);
                if (current.prev == null) {
                    
                }
                assert(current.prev != null);
                n.prev = current.prev.prev;
                n.next = current;
                current.prev = n;
                if (n.prev == null)
                    n.parent.left = n;
            }
        }
        
        
        
        @Override
        public int size() {
            return numChildren;
        }



        @Override
        public ListIterator<AINode> listIterator(int index) {
            AINode c = root.left;
            while (c != null && index-->0)
                c = c.next;
            assert(c != null);
            return new MyListIterator(c, index);
        }
        
    }*/

    AINode findOptimalMoves() {
        float [] max = { -1 };
        AINode node = findOptimalNodeR(max);
        node.optimal = true;
        return node;
    }
    
    private AINode findOptimalNodeR(float [] max) {
        optimal = false;
        AINode t0 = null, t1 = null;
        if (left != null) {
            t0 = left.findOptimalNodeR(max);
        }
        
        if (next != null) {
            t1 = next.findOptimalNodeR(max);
        }
        
        if (isLeaf() && value > max[0]) {
            max[0] = value;
            return this;
        }
        
        // not sure about this:
        // shouldn't it be:
        
        //if (t0 != null && t1 != null && t0.value >= t1.value)
        //    return t0;
        //
        //return t1;

        if (t0 != null && t0.value == max[0])
            return t0; // hmmmm
        return t1;
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
    public void walkTreePreOrderR(AINodeVisitor visitor, int depth) {
        visitor.onVisit(this, depth);
        depth++;
        if (left != null)
            left.walkTreePreOrderR(visitor, depth);
        if (right != null)
            right.walkTreePreOrderR(visitor, depth);
        depth--;
    }
    public void walkTreePostOrderR(AINodeVisitor visitor, int depth) {
        depth++;
        if (left != null)
            left.walkTreePostOrderR(visitor, depth);
        if (right != null)
            right.walkTreePostOrderR(visitor, depth);
        depth--;
        visitor.onVisit(this, depth);
    }
    public void walkTreeInOrderR(AINodeVisitor visitor, int depth) {
        depth++;
        if (left != null)
            left.walkTreeInOrderR(visitor, depth);
        --depth;
        visitor.onVisit(this, depth);
        depth++;
        if (right != null)
            right.walkTreePreOrderR(visitor, depth);
        --depth;
    }
    
    public void walkTreePreOrder(AINodeVisitor visitor) {
        walkTreePreOrderR(visitor, 1);
    }
    public void walkTreePostOrder(AINodeVisitor visitor) {
        walkTreePostOrderR(visitor, 1);
    }
    public void walkTreeInOrder(AINodeVisitor visitor) {
        walkTreeInOrderR(visitor, 1);
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

    public SOCTrade getTrade() {
        return trade;
    }

    public float getValue() {
        return value;
    }

    public AINode getLeft() {
        return left;
    }

    public AINode getRight() {
        return right;
    }

    public AINode getNext() {
        return next;
    }

    public AINode getPrev() {
        return prev;
    }

    public AINode getParent() {
        return parent;
    }

    @Override
    public int compareTo(AINode arg0) {
        return arg0.value - value >= 0 ? 1 : -1; 
    }
    
    
}
