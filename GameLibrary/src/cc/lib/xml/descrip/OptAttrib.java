package cc.lib.xml.descrip;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Base class for Descriptive Attributes
 * @author ccaron
 *
 */
public class OptAttrib implements Serializable {

    private String name;
    private AValueType type = BasicType.NO_CHECK_TYPE;
    
    private void writeObject(ObjectOutputStream out) throws IOException {
    	out.writeObject(name);
    	out.writeObject(type);
    }
    
    private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
    	name = (String)input.readObject();
    	type = (AValueType)input.readObject();
    }
    
    public OptAttrib() {
    }
    
    public OptAttrib(String name) {
        this.name = name;
    }

    public OptAttrib(String name, AValueType type) {
        this(name);
        this.type = type;
    }
    
    public String getName() {
        return name;
    }

    public AValueType getType() {
        return type;
    }
    
    public boolean isRequired() {
        return false;
    }
    
    public String toString() {
        return getClass().getSimpleName() + ": [" + name + 
                "], type [" + type + "]";
    }
    
}
