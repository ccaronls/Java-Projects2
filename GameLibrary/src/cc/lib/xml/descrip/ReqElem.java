package cc.lib.xml.descrip;

public class ReqElem extends OptElem {

    public ReqElem (String name) {
        super(name);
    }

    public ReqElem (String name, OptAttrib [] attributeDescriptions) {
        super(name, attributeDescriptions);
    }

    public ReqElem (String name, OptAttrib [] attributeDescriptions, OptElem [] childDescriptions) {
        super(name, attributeDescriptions, childDescriptions);
    }
    
    public boolean isRequired() {
        return true;
    }

}
