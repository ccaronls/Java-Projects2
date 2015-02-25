package cc.lib.xml.descrip;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import cc.lib.xml.XmlElement;

/**
 * Abstract class fro Descriptive Elements
 * @author ccaron
 *
 */
public class OptElem implements Serializable {

    private String name;
    private Map<String, OptAttrib> attribMap;
    private Map<String, OptElem> childMap;
    private AValueType contentType;
    
    private void writeObject(ObjectOutputStream out) throws IOException {
    	out.writeObject(name);
    	out.writeObject(attribMap);
    	out.writeObject(childMap);
    	out.writeObject(contentType);
    }
    
    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
    	name = (String)input.readObject();
    	attribMap = (Map<String, OptAttrib>)input.readObject();
    	childMap = (Map<String, OptElem>)input.readObject();
    	contentType = (AValueType)input.readObject();
    }
    
    public OptElem() {
    	// needed for Serializable
    }
    
    public OptElem(String name) {
        attribMap = new HashMap<String, OptAttrib>();
        childMap = new HashMap<String, OptElem>();
        contentType = BasicType.EMPTY_VALUE;
        this.name = name;
    }
    
    public OptElem(String name, OptAttrib [] attributeDescriptions) {
        this(name);
        for (int i=0; i<attributeDescriptions.length; i++) {
            this.addAttributeDescription(attributeDescriptions[i]);
        }
    }
    
    public OptElem(String name, OptAttrib [] attributeDescriptions, OptElem [] childDescriptions) {
        this(name, attributeDescriptions);
        for  (int i=0; i<childDescriptions.length; i++) {
            this.addChildDescription(childDescriptions[i]);
        }
    }
    
    public void setContentType(AValueType type) {
        this.contentType = type;
    }
    
    public String getName() {
        return name;
    }
    
    public boolean getRequired() {
        return false;
    }
    
    public void addAttributeDescription(OptAttrib attrib) {
        attribMap.put(attrib.getName(), attrib);
    }
    
    public void addChildDescription(OptElem elem) {
        childMap.put(elem.getName(), elem);
    }
    
    public OptAttrib getDescAttrib(String name) throws UnknownAttributeException {
        if (!this.attribMap.containsKey(name))
            throw new UnknownAttributeException(name, this);
        return (OptAttrib)this.attribMap.get(name);
    }
    
    public OptElem getChildDesc(String name) throws UnknownChildException {
        if (!this.childMap.containsKey(name))
            throw new UnknownChildException(name);
        return (OptElem)this.childMap.get(name);
    }
    
    // package access only
    void validate(XmlElement elem) throws DescriptorException {
        // validate the attributes we have
    	if (!elem.getName().equals(getName()))
    		throw new UnknownElementException(elem);
        String [] attribs = elem.getAttributes();
        for (int i=0; i<attribs.length; i++) {
            OptAttrib attrib = getDescAttrib(attribs[i]);
            try {
                attrib.getType().validate(elem.getAttribute(attribs[i]));
            } catch (Exception e) {
                throw new DescriptorException("Invalid attribute: " + attrib + "\n reason: " + e.getMessage(), e);
            }
        }
        // check for any missing attribs that may be required
        Iterator<String> it = this.attribMap.keySet().iterator();
        while (it.hasNext()) {
            String name = (String)it.next();
            OptAttrib attrib = getDescAttrib(name);
            if (elem.getAttribute(name) == null && attrib.isRequired()) {
                throw new AttributeNotFoundException(attrib);
            }
        }
        // validate the specified children
        XmlElement cur = elem.getFirst();
        while (cur != null) {
            String name = cur.getName();
            OptElem descElem = this.getChildDesc(name);
            descElem.validate(cur);
            cur = cur.getNextSibling();
        }
        // check for required children not provided.
        it = this.childMap.keySet().iterator();
        while (it.hasNext()) {
            String name = (String)it.next();
            OptElem e = this.getChildDesc(name);
            if (e.getRequired() && elem.getElementsByName(name).size()==0)
                throw new ChildNotFoundException(name, this);            
        }
    }
    
    private static int depth = 0;
    
    private String getIndent() {
        StringBuffer indent = new StringBuffer();
        for (int i=0; i<depth; i++) {
            indent.append("   ");
        }
        return indent.toString();
    }
    
    /*
     *  (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        String indent = getIndent();
        buffer.append(indent + this.getClass().getSimpleName() + "\n");
        buffer.append(indent + "name       = " + name + "\n");
        buffer.append(indent + "content    = " + contentType + "\n");
        buffer.append(indent + "attributes = " + this.attribMap.values().toString() + "\n");
        buffer.append(indent + "children:");
        depth ++;
        buffer.append(this.childMap.values().toString());
        depth --;
        return buffer.toString();
    }
}
