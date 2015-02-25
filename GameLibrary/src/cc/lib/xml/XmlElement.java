package cc.lib.xml;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to manage an xml file as an element heirarchy
 * 
 * @author ccaron
 */
public final class XmlElement implements Comparable<XmlElement> {
    
    /*
     * an attribute is the text inside of a tag of the form name=value
     */
    private class XmlAttribute {
        private String          name;
        private String          value;
        private XmlAttribute    next; // linked list
        
        public XmlAttribute(String name, String value, XmlAttribute next) {
            this.name   = name;
            this.value  = value;
            this.next   = next;
        }
        
        public String getName()         { return name; }
        public String getValue()        { return value; }
        public XmlAttribute getNext()   { return next; }
        public String toString()        { return name + "=\"" + value + "\""; }

    }

    private XmlElement right        = null;
    private XmlElement left         = null;
    private XmlElement next         = null;
    private XmlElement parent       = null; // form an n-way tree
    
    private String name             = "";
    private XmlAttribute attributes = null;
    private String contentText      = "";
    
    public XmlElement() {
    }
    
    public XmlElement(String name) {
        this.name = name;
    }
    
    public XmlElement(String name, String content) {
        this.name           = name;
        this.contentText    = content;
    }

    // allow comparisons
    public int compareTo(XmlElement x) {
        int comp = getName().compareTo(x.getName());
        if (comp == 0)
        {
            XmlAttribute a0 = attributes;
            XmlAttribute a1 = x.attributes;
            
            while (comp == 0)
            {
                if (a0 == null && a1 == null)
                    return 0;
            
                if (a0 == null)
                    return 1;
            
                if (a1 == null)
                    return -1;
            
                comp = a0.getValue().compareTo(a1.getValue());
                a0 = a0.next;
                a1 = a1.next;
            }
            
        }
        return comp;
    }
    
    public boolean equals(XmlElement x) {
        return getName().equals(x.getName());
    }
    
    public String getName() {
        return name;
    }
    
    public XmlElement getFirst() {
        return left;
    }
    
    public XmlElement getFirst(String name) {
        XmlElement cur = getFirst();
        while (cur != null && !cur.getName().equals(name))
            cur = cur.getNextSibling();
        return cur;
    }
    
    public void addAttribute(String name, String value) {
        attributes = new XmlAttribute(name, value, attributes);
    }

    public void assignChildren(XmlElement [] array) {
        left = right = null;
        for (int i=0; i<array.length; i++)
        {
            array[i].next = null;
            this.addChild(array[i]);
        }
    }
    
    /**
     * Return the value of an attribute or null when attribute not found
     * @param name - name of the attribute (lhs of =)
     * @return - the value of the attribute (rhs of =)
     */
    public String getAttribute(String name) {
        XmlAttribute attr = attributes;
        while (attr != null) {
            if (attr.getName().equals(name))
                return attr.getValue();
            attr = attr.next;
        }
        return null;
    }
    
    /**
     * Return an array of 
     * @return
     */
    public String [] getAttributes() {
        List<String> list = new ArrayList<String>();
        XmlAttribute attr = attributes;
        while (attr != null) {
            list.add(attr.getName());
            attr = attr.next;
        }
        return list.toArray(new String[list.size()]);
    }
    
    /**
     * Return the parent of this or null when this is a root
     * @return
     */
    public XmlElement getParent() {
        return parent;
    }
    
    /**
     * @return my next sibling or null when the end of the list
     */
    public XmlElement getNextSibling() {
        return next;
    }
    
    /**
     * 
     * @param name
     * @return
     */
    public XmlElement getNextSibling(String name) {
        XmlElement cur = getNextSibling();
        while (cur != null && !cur.getName().equals(name))
            cur = cur.getNextSibling();
        return cur;
    }
    
    /**
     * Get a list of elements from a given name
     * @param name - the name of the element
     * @return - array of elements
     */
    public List<XmlElement> getElementsByName(String name) {
        ArrayList<XmlElement> list = new ArrayList<XmlElement>();
        XmlElement cur = left;
        while (cur != null) {
            if (cur.name.equals(name))
                list.add(cur);
            cur = cur.next;
        }
        return list;
    }
    
    /**
     * Attach a child
     * @param child - element to attach
     */
    public void addChild(XmlElement child) {
        if (left == null) {
            left = right = child;
        } else {
            right.next = child;
            right = child;
        }
        child.parent = this;
    }
    
    /**
     * Get the content text (if any) of this element, never returns null
     * @return - the contenet text
     */
    public String getContentText() {
        return contentText;
    }
    
    /* helper to write the attributes, last first */
    private String writeAttrib(XmlAttribute attr) {
        if (attr.next != null)
            return writeAttrib(attr.next) + " " + attr.toString();
        return " " + attr.toString();
    }
    
    /**
     * Return a string of just the element header
     * @return
     */
    public String toString() {
        StringBuffer buffer = new StringBuffer("<" + name);
        if (attributes != null)
            buffer.append(writeAttrib(attributes));
        if (contentText.length() > 0)
            buffer.append(contentText+"</" + name + ">");
        else if (left == null)
            buffer.append("/>");
        else
            buffer.append(">");
        return buffer.toString();
    }
    
    /**
     * Recursive generate the xml
     */
    public String toStringDeep() {
        return toStringRecursive(0);
    }
    
    /*
     * Recursive Helper method
     */
    private String toStringRecursive(int depth) {
        StringBuffer buffer = new StringBuffer(XmlUtils.getIndent(depth) + "<" + name);
        if (attributes != null)
            buffer.append(writeAttrib(attributes));
        if (contentText.length() > 0)
            buffer.append(">"+contentText+"</" + name + ">\n");
        else if (left == null)
            buffer.append("/>\n");
        else
        {
            buffer.append(">\n");
            XmlElement elem = left;
            while (elem != null) {
                buffer.append(elem.toStringRecursive(depth+1));
                elem = elem.next;
            }
            buffer.append(XmlUtils.getIndent(depth) + "</" + name + ">\n");
        }
        return buffer.toString();
    }
};
