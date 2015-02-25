package cc.lib.xml.descrip;

public class UnknownAttributeException extends DescriptorException {
    
    UnknownAttributeException(String name, OptElem parent) {
        super("Unknown attribute [" + name + "] for parent [" + parent + "]");
    }

}
