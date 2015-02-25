package cc.lib.xml;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * This class can be used to execute 'java code' from XML using Reflection
 * @author ccaron
 * 
 * <pre>
 * <h4>Summary</h4>
 * This class can be used to execute java code defined in a xml docuemnt.
 * <h4>Example</h4>
 * Below is some XML with the code equivelent below.  
 * The following classes are assumed to be defined:
 * 
 * package cc.lib.xml;
 * 
 * class MyOtherClass {
 *    MyOtherClass(Integer i) {}
 * }
 * 
 * class MyClass {
 *   MyClass(Integer i, MyOtherClass o) {}
 *   public void func1() {}
 *   public void func2(Object arg1, MyOtherClass arg2) {}
 * } 
 * 
 * XML:
 * 
 * &lt;start&gt; -- this can be anything, it is totally ignored
 *    &lt;obj class="mypackage cc.lib.xml;
 *       &lt;arg class="java.lang.Integer" value="1"/&gt;
 *       &lt;arg class="mypackage cc.lib.xml;
 *          &lt;arg class="java.lang.Integer" value="10"/&gt;
 *       &lt;/arg&gt;
 *       &lt;call method="func1"/&gt; -- call a func that takes no params
 *       &lt;call method="func2"&gt; -- call a func that takes 2 params, 1 easy, 1 complex
 *          &lt;param class="java.lang.String" value="hello" baseclass="java.lang.Object"/&gt;
 *          &lt;param class="mypackage cc.lib.xml;
 *             &lt;arg class="java.lang.Integer" value="100"/&gt;
 *          &lt;/param&gt;
 *       &lt;/call&gt;
 *    &lt;/obj&gt;
 * &lt;/start&gt;
 * 
 * CODE:
 * 
 * MyClass x(new Integer(1), new MyOtherClass(new Integer(10));
 * x.func1();
 * x.func2(new String("hello"), new MyOtherClass(new Integer(100)));
 * 
 * <h4>Limitations</h4>
 * tags of this form:
 * &lt;arg class="java.lang.Integer" value="100"/&gt;
 * must refer too classes that accept a String as a constructor param
 * 
 * Return values from method calls are ignored.
 * 
 * Finally, an annoying circumstance of reflection requires that the exact
 * class be provided too constructors and functions.  For instance, say we have a
 * method:
 * 
 * void method(Number n) {}
 * 
 * And we want to provide an Integer, to this function.  Reflection will not be able
 * too match Integer against Number even though an Integer 'isa' Number.  So we 
 * have to provide an additional attribute to the xml 'baseclass' that provides the
 * Class (or Interface) too use when the constucted class differs from the param type
 * expected.
 * </pre>  
 */
public class XmlReflection {

    //private static Logger log = Logger.getLogger(XmlReflection.class);
    
    /**
     * Test this class
     * @param args
     */
    public static void main(String [] args) {
        if (args.length < 1) {
            System.err.println("USAGE: XmlReflection <xmlFile>");
            System.exit(1);
        }
        
        try {
            XmlElement root = XmlParser.parseXml(args[0]);
            parseDoc(root);            
            
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /* Class for keeping the object and its baseclass type in a single unit */
    private static class ParseObjectResult {
        Object theObj;
        Class<?>  theClass;
    }
    
    /**
     * 
     * @param root the root of the document
     * @return an array of created objects.  There is 1 object for each 'obj' tag
     * @throws XmlReflectionException
     */
    public static List parseDoc(XmlElement root) throws XmlReflectionException {
        List objects = new LinkedList();
        try {
            for (XmlElement elem = root.getFirst(); elem != null; elem = elem.getNextSibling()) {
                if (elem.getName().equals("obj")) {
                    ParseObjectResult result = parseObject(elem, true);
                    objects.add(result.theObj);
                } else {
                    throw new XmlReflectionException("Unknown tag [" + elem.getName() + "] in XML:" + root);
                }
            }
        } catch (NoSuchMethodException e) {
            throw new XmlReflectionException(e.getClass().getSimpleName() + ":" + e.getMessage());
        } catch (IllegalAccessException e) {
            throw new XmlReflectionException(e.getClass().getSimpleName() + ":" + e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new XmlReflectionException(e.getClass().getSimpleName() + ":" + e.getMessage());
        } catch (InvocationTargetException e) {
            throw new XmlReflectionException(e.getClass().getSimpleName() + ":" + e.getCause());
        } catch (InstantiationException e) {
            throw new XmlReflectionException(e.getClass().getSimpleName() + ":" + e.getMessage());
        }
        return objects;
    }
    
    /* parse and instantiate an object defined from some xml */
    private static ParseObjectResult parseObject(XmlElement elem, boolean checkMethods) 
    throws XmlReflectionException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException, ClassNotFoundException {
        String name = elem.getAttribute("class");
        if (name == null)
            throw new XmlReflectionException("Cant find class attribute in xml: " + elem);
        ParseObjectResult result = new ParseObjectResult();
        // if this object has a "value" attrib
        String value = elem.getAttribute("value");
        XmlElement cur = null;
        if (value != null) {
            if (elem.getElementsByName("arg").size() > 0)
                throw new XmlReflectionException("Invalid xml, elem [" + name + "] has value attrib and args");
            Class [] params = new Class [] { Class.forName("java.lang.String") };
            Object [] values = new Object [] { value };
            result.theObj = instantiate(name, params, values);
        } else {
            cur = elem.getFirst();
            if (cur == null || !cur.getName().equals("arg")) {
                // no arguments, simple class instantiation
                result.theObj = Class.forName(name).newInstance();
            } else {
                // more complex instatiation
                List children = new LinkedList();
                for ( ; cur != null && cur.getName().equals("arg"); cur = cur.getNextSibling()) {
                    ParseObjectResult obj = parseObject(cur, false);
                    children.add(obj);
                }
                Object [] values = new Object[children.size()];
                Class  [] params = new Class[children.size()];
                Iterator it = children.iterator();
                int i = 0;
                while (it.hasNext()) {
                    ParseObjectResult p = (ParseObjectResult)it.next();
                    values [i] = p.theObj;
                    params [i] = p.theClass;
                    i++;
                }
                result.theObj = instantiate(name, params, values);
            }
        }
        
        // check for the 'baseclass' attribute
        String baseClass = elem.getAttribute("baseclass");
        if (baseClass != null) {
            result.theClass = Class.forName(baseClass);
        } else {
            result.theClass = result.theObj.getClass();
        }
        // check for methods on this object
        if (checkMethods) {
            for ( ; cur != null ; cur = cur.getNextSibling()) {
                if (!cur.getName().equals("call"))
                    throw new XmlReflectionException("Unknown xml tag: " + cur);
                executeMethod(result.theObj, cur);
            }
        } else if (cur != null) {
            throw new XmlReflectionException("Cant execute xml: " + cur);
        }
        return result;
    }
    
    /* parse and execute the xml defining an method call */
    private static Object executeMethod(Object parent, XmlElement call) 
    throws XmlReflectionException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException, ClassNotFoundException {
        String methodName = call.getAttribute("method");
        if (methodName == null)
            throw new XmlReflectionException("Cant find method attribute in xml: " + call);
        List children = new LinkedList();
        for (XmlElement cur = call.getFirst(); cur != null; cur = cur.getNextSibling()) {
            ParseObjectResult o = parseObject(cur, false);
            children.add(o);
        }
        Object [] values = new Object[children.size()];
        Class  [] params = new Class[children.size()];
        Iterator it = children.iterator();
        int i = 0;
        while (it.hasNext()) {
            ParseObjectResult p = (ParseObjectResult)it.next();
            values [i] = p.theObj;
            params [i] = p.theClass;
            i++;
        }
        return callMethod(parent, methodName, params, values);
    }
    
    /* Instantiate an object given its classname, the paramtypes and the params */
    private static Object instantiate(String className, Class [] params, Object [] args) 
    throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException, ClassNotFoundException {
        return Class.forName(className).getConstructor(params).newInstance(args);
    }

    /* Call a method given and object, the mthod name the param types and the params */
    private static Object callMethod(Object parent, String name, Class [] params, Object [] args)
    throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException, ClassNotFoundException {
        return parent.getClass().getMethod(name, params).invoke(parent, args);
    }
    
}
