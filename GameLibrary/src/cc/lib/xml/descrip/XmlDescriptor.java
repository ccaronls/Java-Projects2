package cc.lib.xml.descrip;

import java.io.*;
import java.util.Iterator;
import java.util.Set;

import cc.lib.xml.XmlElement;



/**
 * Class for defining an xml description (like a DTD)
 * Only we dont need licences or whatever!
 * 
 * @author ccaron
 *
 */
public class XmlDescriptor {

    private ReqElem root;
    
    /**
     * Create a description from a ReqElem.  This is how we migth construct
     * a description in code.
     *
     * @param root
     */
    public XmlDescriptor(ReqElem root) {
        this.root = root;
    }
    
    /**
     * Initialize from a descriptor file.
     * 
     * @param descriptorFile
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public XmlDescriptor(File descriptorFile) throws IOException, DescriptorException {
    	load(descriptorFile);
    }
    
    /**
     * Generate a description from an XmlElement.  This is how we might generate 
     * a description from an existing xml document to validate other docs.
     * 
     * script flag used to specify ReqAttrib/Elem whenever possible.
     * Also affects use of cc.lib.xml.descrip.ChoiceString when
     * cc.lib.xml.descrip.BasicType.NON_EMPTY_STRING is found.
     * 
     * @param elem
     */
    public XmlDescriptor(XmlElement elem, boolean strict) {
    	XmlInfo info = new XmlInfo();
    	info.evaluate(elem);
    	Iterator<String> it = info.iterator();
    	String elemId = it.next();
    	root = new ReqElem(elemId);
		XmlInfo.ElemInfo elemInfo = info.getInfo(elemId);
		elemInfo.setElem(root);
		addAttributes(root, elemInfo, strict);
		addContent(root, elemInfo);
		while (it.hasNext()) {
			elemId = it.next();
			elemInfo = info.getInfo(elemId);
			String elemName = elemId.substring(elemId.lastIndexOf(XmlInfo.DELIM)+XmlInfo.DELIM.length());
			//OptElem newElem = null;
			//XmlInfo.ElemInfo parentInfo = getParentInfo(elemId, info);
			// if parent always has at least 1 occurance
			OptElem newElem = strict ? new ReqElem(elemName) : new OptElem(elemName);
			//newElem.addChildDescription(parentInfo.getElem());
			elemInfo.setElem(newElem);
			addAttributes(newElem, elemInfo, strict);
			addContent(newElem, elemInfo);
			addToParent(elemId, info, newElem);
		}
    }
    
    private XmlInfo.ElemInfo getParentInfo(String id, XmlInfo info) {
    	int index = id.lastIndexOf(XmlInfo.DELIM);
    	if (index < 0)
    		throw new RuntimeException("Illgal child elem id [" + id + "]");
    	String parentId = id.substring(0, index);
    	return info.getInfo(parentId);    	
    }
    
    /*
     * 
     */
    private void addToParent(String id, XmlInfo info, OptElem elem) {
    	XmlInfo.ElemInfo elemInfo = getParentInfo(id, info);
    	elemInfo.getElem().addChildDescription(elem);
    }
    
    /*
     * 
     */
    private void addContent(OptElem elem, XmlInfo.ElemInfo info) {
    	AValueType type = getValueTypeFromValuesCounter(info.content.getKeySet());
    	elem.setContentType(type);
    }
    
    /*
     * 
     */
    private void addAttributes(OptElem elem, XmlInfo.ElemInfo info, boolean strict) {
    	Iterator<?> it = info.attribs.keySet().iterator();
    	while (it.hasNext()) {
    		String attribName = (String)it.next();
    		XmlInfo.ValueCounter counts = (XmlInfo.ValueCounter)info.attribs.get(attribName);
    		AValueType value = getValueTypeFromValuesCounter(counts.getKeySet());
    		// if attibName always occurs in the elem, and strict, then use ReqAttrib
    		OptAttrib attribDesc = null;
    		if (strict && (info.occurances == counts.getTotalOccurances()))
    			attribDesc = new ReqAttrib(attribName, value);
    		else
    			attribDesc = new OptAttrib(attribName, value);
    		elem.addAttributeDescription(attribDesc);
    	}
    }
    
    /**
     * Get the best fit ValueType from a string.
     * Will be one of cc.lib.xml.desc.BasicType
     * 
     * @param items
     * @return
     */
    public static AValueType getValueTypeFromValuesCounter(Set<?> items) {
    	Iterator<?> it = items.iterator();
    	int bestPriority = Integer.MAX_VALUE;
    	AValueType result = BasicType.EMPTY_VALUE;
    	while (it.hasNext()) {
    		String value = (String)it.next();
    		Iterator<?> bit = BasicType.iterator();
    		int priority = 0;
    		AValueType prevType = BasicType.NO_CHECK_TYPE;
    		while (bit.hasNext()) {
    			String key = (String)bit.next();
    			AValueType type = BasicType.getInstance(key);
    			//if (priority++ > bestPriority)
    			//	break;
    			try {
    				type.validate(value);
    			} catch (Exception e) {
    				// if we cannot validate this then we quit
    				if (priority < bestPriority) {
    					bestPriority = priority;
    					result = prevType;
    				}
    				break;
    			}
    			priority++;
    			prevType = type;
    		}
    	}
    	if (result == null)
    		throw new RuntimeException("Failed to determine value from: " + items);
    	//System.out.println("Returning [" + result + "] for: " + items);
    	return result;
    }
    
    /**
     * Validate an xml document.
     * 
     * @param elem
     * @throws DescriptorException when the document is invalid
     */
    public void validate(XmlElement elem) throws DescriptorException {
    	try {
    		root.validate(elem);
    	} catch (DescriptorException e) {
    		throw e;
    	} catch (Exception e) {
    		e.printStackTrace();
    		throw new DescriptorException("Validation failed:" + e.getMessage());
    	}
    }

    /*
     *  (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return root.toString();
    }
    
    /**
     * Load a description from an InputStream
     * @param fileName
     */
    public void load(File file) throws IOException, DescriptorException {
    	ObjectInputStream in = null;
    	try {
	    	in = new ObjectInputStream(new FileInputStream(file));
	    	root = (ReqElem)in.readObject();
    	} catch (IOException e) {
    		throw e;
    	} catch (Throwable e) {
    		throw new DescriptorException("Cannot load description: " + e.getMessage());
    	} finally {
    		try {
    			in.close();
    		} catch (Exception e) {}
    	}
    }
    
    /**
     * 
     * @param fileName
     * @throws IOException
     */
    public void save(File file) throws IOException {
        ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(file));
        output.writeObject(root);
        output.close();
    }
    
}
