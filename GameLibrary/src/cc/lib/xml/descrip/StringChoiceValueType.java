package cc.lib.xml.descrip;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class StringChoiceValueType extends AValueType {

    private List<String> list = new LinkedList<String>();
    
    /**
     * 
     * @param list
     */
    public StringChoiceValueType(List<String> list) {
        this.list = list;
    }
    
    /**
     * 
     * @param a
     * @param b
     */
    public StringChoiceValueType(String a, String b) {
        list.add(a);
        list.add(b);
    }

    /**
     * 
     * @param a
     * @param b
     * @param c
     */
    public StringChoiceValueType(String a, String b, String c) {
        list.add(a);
        list.add(b);
        list.add(c);
    }
    
    /**
     * 
     * @param a
     * @param b
     * @param c
     * @param d
     */
    public StringChoiceValueType(String a, String b, String c, String d) {
        list.add(a);
        list.add(b);
        list.add(c);
        list.add(d);
    }
    
    /**
     * 
     * @param options
     */
    public StringChoiceValueType(String [] options) {
        for (int i=0; i<options.length; i++) {
            list.add(options[i]);
        }
    }

    /*
     *  (non-Javadoc)
     * @see cc.lib.xml.descrip.AValueType#validate(java.lang.String)
     */
    public void validate(String value) throws Exception {
        boolean found = false;
        //for (int i=0; i<list.length; i++) {
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            if (value.equals((String)it.next())) {
                found = true;
                break;
            }
        }
        if (!found)
            throw new Exception("Invalid Value [" + value +"], choices are: " + list);
    }
    
}
