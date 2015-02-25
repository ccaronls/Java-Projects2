package cc.lib.xml.descrip;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Base class for value types.  These define attribute values and content values
 * @see cc.lib.xml.descrip.BasicValues for common value types.
 * 
 * @author ccaron
 *
 */
public abstract class AValueType implements Serializable {

    private String id;
    
    private void writeObject(ObjectOutputStream out) throws IOException {
    	out.writeObject(id);
    }
    
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    	id = (String)in.readObject();
    }
    
    protected AValueType() { 
        this.id = getClass().getName(); 
    }
    
    protected AValueType(String id) { 
        this.id = id;
    }
    
    public String toString() {
        return id;
    }
    
    public abstract void validate(String value) throws Exception;
    
}
