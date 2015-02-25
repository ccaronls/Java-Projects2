package cc.lib.xml;

import java.util.Stack;

public class XmlDoc {

    /**
     * Construct the most basic xml document possible
     */
    public XmlDoc() {
    }
    
    /**
     * Method to add a simple tag with any number of name/value pair attriobutes
     * The size of the 2 arrays is assumed to be equal
     * @param name
     * @param params
     * @param values
     */
    public void simpleTag(String name, String [] params, String [] values) {
        buffer.append(XmlUtils.simpleTag(getDepth(), name, params, values));
    }
    
    /**
     * Method to add a simple tag with no attributes 
     * @param name
     */
    public void simpleTag(String name) {
        String [] params = { };
        String [] values = { };
        simpleTag(name, params, values);
    }
    
    /**
     * Method to add a simple tag with 1 name/value pair attribute
     * @param name    the name of the tag
     * @param param1  the name of attribute 1
     * @param value1  the value of attribute 1
     */
    public void simpleTag(String name, String param, String value) {
        String [] params = { param };
        String [] values = { value };
        simpleTag(name, params, values);
    }
    
    /**
     * Method to add a simple tag with 2 name/value pair attributes
     * @param name    the name of the tag
     * @param param1  the name of attribute 1
     * @param value1  the value of attribute 1
     * @param param2  the name of attribute 2
     * @param value2  the value of attribute 2 
     */
    public void simpleTag(String name, String param1, String value1, String param2, String value2) {
        String [] params = { param1, param2 };
        String [] values = { value1, value2 };
        simpleTag(name, params, values);
    }
    
    /**
     * Method to add a simple tag with 3 name/value pair attributes
     * @param name    the name of the tag
     * @param param1  the name of attribute 1
     * @param value1  the value of attribute 1
     * @param param2  the name of attribute 2
     * @param value2  the value of attribute 2 
     * @param param3  the name of attribute 3
     * @param value3  the value of attribute 3
     */
    public void simpleTag(String name, String param1, String value1, String param2, String value2, String param3, String value3) {
        String [] params = { param1, param2, param3 };
        String [] values = { value1, value2, value3 };
        simpleTag(name, params, values);
    }

    /**
     * Method to add a complex tag with any number of name/value pairs
     * The size of the arrays is assumed to be the same
     * @param name
     * @param names
     * @param values
     */
    public void complexTag(String name, String [] names, String [] values) {
        buffer.append(XmlUtils.complexTag(getDepth(), name, names, values));
        push(name);
    }
    
    /**
     * Convenience method to add a tag with no attributes
     * @param name the name of this tag
     */
    public void complexTag(String name) {
        String [] params = {};
        String [] values = {};
        complexTag(name, params, values);
    }
    
    /**
     * Convenience method to add a complex tag with 1 name/value pair
     * @param name   the name of this tag
     * @param param1 the name of attribute 1
     * @param value1 the value of attribute 1
     */
    public void complexTag(String name, String param, String value) {
        String [] params = { param };
        String [] values = { value };
        complexTag(name, params, values);
    }

    /**
     * Convenience method to add a complex tag with 2 name/value pairs
     * @param name   the name of this tag
     * @param param1 the name of attribute 1
     * @param value1 the value of attribute 1
     * @param param2 the name of attribute 2
     * @param value2 the value of attribute 2
     */
    public void complexTag(String name, String param1, String value1, String param2, String value2) {
        String [] params = { param1, param2 };
        String [] values = { value1, value2 };
        complexTag(name, params, values);
    }

    /**
     * Convenience method to add a complex tag with 3 name/value pairs
     * @param name   the name of this tag
     * @param param1 the name of attribute 1
     * @param value1 the value of attribute 1
     * @param param2 the name of attribute 2
     * @param value2 the value of attribute 2
     * @param param3 the name of attribute 3
     * @param value3 the value of attribute 3
     */
    public void complexTag(String name, String param1, String value1, String param2, String value2, String param3, String value3) {
        String [] params = { param1, param2, param3 };
        String [] values = { value1, value2, value3 };
        complexTag(name, params, values);
    }

    /**
     * 
     * @throws Exception
     */
    public void closeTag() {
        if (complexTagStack.empty()) {
            throw new RuntimeException("Calling closeTag when stack is empty");
        }
        buffer.append(XmlUtils.closeTag(getDepth()-1, pop()));
    }
    
    /**
     * 
     *
     */
    public void closeAll() {
        while (getDepth()>0)
            buffer.append(XmlUtils.closeTag(getDepth()-1, pop()));
    }
    
    /**
     * 
     * @return
     * @throws Exception
     */
    public String getXML() {
        closeAll();
        return buffer.toString();
    }
    
    /**
     * 
     *
     */
    public void reset() {
        complexTagStack.clear();
        buffer.setLength(0);
        buffer.append(XmlUtils.xmlHeader());
    }
    
    // maintain a stack to track the depth of complex tags
    private Stack<String> complexTagStack = new Stack<String>();
    
    // use buffer to maintain the generated xml
    private StringBuffer buffer = new StringBuffer(XmlUtils.xmlHeader());
    
    // push a string onto the stack
    private void push(String tag) {
        complexTagStack.push(tag);
    }
    
    // pop a string off the stack
    private String pop() {
        return complexTagStack.pop();
    }
    
    // get the depth of the complex tag stack
    private int getDepth() {
        return complexTagStack.size();
    }

}
