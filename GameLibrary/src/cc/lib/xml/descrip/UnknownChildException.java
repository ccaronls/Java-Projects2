package cc.lib.xml.descrip;

public class UnknownChildException extends DescriptorException {

    UnknownChildException(String name) {
        super("Unknown child element [" + name + "]");
    }
    
}
