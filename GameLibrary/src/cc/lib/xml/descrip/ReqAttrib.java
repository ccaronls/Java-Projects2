package cc.lib.xml.descrip;

public class ReqAttrib extends OptAttrib {

    public ReqAttrib(String name) {
        super(name);
    }
    
    public ReqAttrib(String name, AValueType type) {
        super(name, type);
    }
    
    public boolean isRequired() {
        return true;
    }
    
}
