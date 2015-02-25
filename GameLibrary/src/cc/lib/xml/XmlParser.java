package cc.lib.xml;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * XmlParser
 * 
 * Class to load a xml files into a heirarcal tree
 */
public final class XmlParser {
    // precompile some patterns
    private static Pattern header       = Pattern.compile("<[?].*[?]>");
    private static Pattern docInfo      = Pattern.compile("<!.*>");
    private static Pattern simpleTag    = Pattern.compile("<.*/>");
    private static Pattern complexTag   = Pattern.compile("<.*>");
    private static Pattern ident        = Pattern.compile("<[ ]*[a-zA-Z_\\-][a-zA-Z0-9_\\-]*[ ]*");
    private static Pattern attribute    = Pattern.compile("[a-zA-Z_\\-][a-zA-Z0-9_\\-]*[ ]*=[ ]*[\"][^\"]*[\"]");
    private static Pattern endOfTag     = Pattern.compile("[ ]*[/]?>"); 
    
    // where to read lines of text from
    private BufferedReader reader;
    
    // where too store lines read from
    private StringBuffer buffer = new StringBuffer(); // input buffer
    
    // current line being parsed
    private int lineNum = 0; // last line read from file
    private String curLine = "";
    
    // document root, this is what gets returned
    private XmlElement root = new XmlElement();
    
    /**
     * Return the root of xml document (typically the 'device-capabilities' tag)
     * @param fileName - file to load
     * @return - the root of document
     * @throws Exception - on any IO or parsing exception
     */
    public static XmlElement parseXml(File file) throws XmlParserException {
        try {
            return new XmlParser().parse(new BufferedReader(new FileReader(file)));
        } catch (IOException e) {
            throw new XmlParserException(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
    
    /**
     * 
     * @param xml
     * @return
     * @throws Exception
     */
    public static XmlElement parseXml(String xml) throws XmlParserException {
        try {
            return new XmlParser().parse(new BufferedReader(new StringReader(xml)));
        } catch (EOFException e) {
            throw new XmlParserException(e.getClass().getSimpleName() + ": " + e.getMessage());
        } catch (IOException e) {
            throw new XmlParserException(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Write to file starting at the provided root
     * @param root - root element of the file
     * @param fileName - file to write too
     * @throws Exception - on any IO error
     */
    public static String toXML(XmlElement root) {
        StringBuffer buf = new StringBuffer(XmlUtils.xmlHeader());
        buf.append(root.toStringDeep());
        return buf.toString();
    }
    
    /* return content text up to next tag or null if none found 
     * WARNING : Assumption is that content text tags are always
     * on a single line! (This is the case with devices.xml)
     * This will parse:
     *    <tag>Content Text</tag>
     * This will not parse:
     *    <tag>Content Text
     *    </tag>
     * nor this:
     *    <tag>
     *    Content Text
     *    </tag>
     * */
    private String readContentText() throws IOException {
        String result = null;
        while (true) {
            int right = buffer.indexOf("<");
            if (right == 0)
                break; // no content text
            if (right > 0)
            {
                result = buffer.substring(0, right);
                buffer.delete(0,right);
                break;
            }
            String line = reader.readLine();
            if (line == null)
                break;
            lineNum++;
            buffer.append(line.trim());
        }
        return result;
    }
    
    /* return next tag in file of form '<...>' */
    private String readNextTag() throws IOException {
        // see if there is a tag in the buffer
        String result = null;
        int left, right;
        
        while (true)
        {
            left = buffer.indexOf("<!--");
            if (left>=0)
            {
                // found a comment, extract it
                right = buffer.indexOf("-->", left+1);
                if (right>left)
                {
                    buffer.delete(left,right+4);
                }
            }
            else
            {       
                left = buffer.indexOf("<");
                if (left>=0)
                {
                    right = buffer.indexOf(">", left+1);
                    if (right>left)
                    {
                        result = buffer.substring(left,right+1);
                        buffer.delete(0,right+1);
                        break;
                    }
                }
            }
            // append the next line to the buffer
            String line = reader.readLine();
            if (line == null)
            {
                if (left >= 0)
                    throw new EOFException ("Unexpected end of file");
                break;
            }
            curLine = line;
            lineNum++;
            buffer.append(line.trim());
        }
        return result;
    }
    
    /* load a file */
    private XmlElement parse(BufferedReader reader) throws IOException, EOFException, XmlParserException {
        this.reader = reader;
        while (true) {
            String tag = readNextTag();
            if (tag == null)
                break;
                    
            if (header.matcher(tag).matches())
                continue;
            if (docInfo.matcher(tag).matches())
                continue;
            try {
                if (complexTag.matcher(tag).matches())
                {
                    XmlElement elem = parseComplexTag(tag);
                    root.addChild(elem);
                }
                else
                    throw new XmlParserException("Dont know how to parse line: " + curLine);
            } catch (Exception e) {
                reader.close();
                throw new XmlParserException("ParseError, line#(" + lineNum + ") " + e + "\nline:" + curLine);
            }
        }
        reader.close();
        return root.getFirst();
    }
    
    /* Check for a tag of form <name>ContentText</name> */
    private XmlElement checkForTextTag(String start) throws Exception {
        String startTag = start.substring(1, start.length()-1).trim();
        String content = readContentText();
        if (content == null)
            return null;
        String close = readNextTag();
        String closeTag = close.substring(2, close.length()-1).trim();
        if (!closeTag.equals(startTag))
            throw new Exception("Expected closing tag </" + startTag + "> found " + closeTag);
        XmlElement elem = new XmlElement(startTag, content);
        return elem;
    }
    
    /* recursive parse a tag of form:
     *  <name attrib1="xyz">
     *     <name2 ...>
     *     ...
     *  </name>      
     */
    private XmlElement parseComplexTag(String curTag) throws Exception {
        XmlElement elem;
        try {
            elem = checkForTextTag(curTag);
            if (elem != null)
                return elem;
            elem = parseSimpleTag(curTag);
            String endTag = "</" + elem.getName() + ">";
            while (true) {
                String tag = readNextTag();
                if (tag == null)
                    throw new Exception("Unexpected EOF");
                if (endTag.equals(tag))
                    break;
                if (simpleTag.matcher(tag).matches())
                    elem.addChild(parseSimpleTag(tag));
                else if (complexTag.matcher(tag).matches())
                    elem.addChild(parseComplexTag(tag));
                else
                    throw new Exception("Dont know how to parse line");
                
            }
        } catch (Exception e) {
            throw new Exception("Malformed xml:" + curTag + "\n caused by:" + e.getMessage());
        }
        return elem;
    }
   
    /* parse the name and attributes from a line of text */
    private XmlElement parseSimpleTag(String line) throws XmlParserException {
        XmlElement elem;
        
        // parse the name of the elem
        Matcher m = ident.matcher(line);
        if (!m.find())
            throw new XmlParserException("Failed to extract tag name from line: " + line);
        
        String name = m.group().substring(1).trim();
        elem = new XmlElement(name);
        int next = m.end();
        
        // parse all the attributes
        m = attribute.matcher(line);
        while (m.find(next)) {
            parseAttribute(elem, m.group());
            next = m.end();
        }
        
        if (!endOfTag.matcher(line).find(next))
            throw new XmlParserException("Malformed XML: " + line);

        return elem;
    }
    
    /* extract the name/value pair from text and add the parent */
    private void parseAttribute(XmlElement parent, String text) {
        int eq = text.indexOf('=');
        String name  = text.substring(0, eq).trim();
        String value = text.substring(eq+1).trim();
        String valueSansQuotes = value.substring(1, value.length()-1);
        parent.addAttribute(name, valueSansQuotes);
    }
}
