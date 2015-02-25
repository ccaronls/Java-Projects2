package cc.lib.xml;

/**
 * General use utilities for writing Xml tags
 * 
 * @author ccaron
 *
 */
public class XmlUtils {
    
    /**
     * What char to use to end lines.  Some parsers choke on \n, so set this too ""
     */
    public static String ENDL = "\n";
    
    /**
     * Whether to enforce indenting.  Some parser choke on leading whitespace, so set too false.
     */
    public static boolean DOINDENT = true; 
    
    /**
     * Create a xml header tag
     * @return Xml String
     */
    public static String xmlHeader() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"; // this tag MUST have an endl
    }
    
    /**
     * Method to constuct a simple xml tag from arbitrary list of names/vales
     * The size od names and values is assumed to be equal 
     *  
     * @param indent
     * @param id
     * @param names
     * @param values
     * @return Xml String
     */
    public static String simpleTag(int indent, String id, String [] names, String [] values)  {
        return baseTag(indent, id, names, values) + "/>" + ENDL;
    }
    
    /**
     * 
     * @param indent
     * @param id
     * @param names
     * @param values
     * @return
     */
    public static String complexTag(int indent, String id, String [] names, String [] values)  {
        return baseTag(indent, id, names, values) + ">" + ENDL;
    }
    
    /**
     * Create a closing tag, this should follow any complex tags
     * @param indent
     * @param id
     * @return Xml String
     */
    public static String closeTag(int indent, String id) {
        return getIndent(indent) + "</" + id + ">" + ENDL;
    }
    
    /**
     * Convenience method
     * 
     * @param indent
     * @param id
     * @param names
     * @param values
     * @return Xml String
     */
    private static String baseTag(int indent, String id, String [] names, String [] values)  {
        StringBuffer buffer = new StringBuffer();
        buffer.append(getIndent(indent) + "<" + id);
        for (int i=0; i<names.length; i++) {
            buffer.append(" " + names[i] + "=\"" + values[i] + "\"");
        }
        return buffer.toString();
    }
    
    /*
     * 
     */
    private final static int MAX_INDENT = 30;
    
    /*
     * 
     */
    private static String [] indents;
    static {
        indents = new String[MAX_INDENT];
        String ind = "";
        for (int i=0; i<MAX_INDENT; i++) {
            indents[i] = new String(ind);
            ind = ind + "   ";
        }
    }
    
    /*
     * 
     */
    public static String getIndent(int depth) {
        if (DOINDENT) {
            return depth >= MAX_INDENT ? indents[MAX_INDENT-1] : indents[depth];
        }
        return indents[0];
    }
    
    // This class is not instatiated, has ststic members only
    private XmlUtils() {}
    
}
